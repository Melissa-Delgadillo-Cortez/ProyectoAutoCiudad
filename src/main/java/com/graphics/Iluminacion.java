package com.graphics; // Mantiene esta lección junto a las clases anteriores.

import static org.lwjgl.glfw.GLFW.*; // Incluye las constantes de las teclas N y F.

/**
 * CLASE 3 — ILUMINACIÓN DE LA CIUDAD.
 *
 * Responsabilidades de esta clase:
 *   • Alternar entre ambiente diurno y nocturno con la tecla N.
 *   • Encender / apagar los faros del automóvil con la tecla F.
 *   • Distribuir 16 farolas por el mapa ampliado (≥ 9 exigidas).
 *   • Enviar al shader las posiciones de todas las farolas,
 *     la posición del auto y su dirección frontal.
 *   • Reemplazar el shader de color plano de CiudadBase por uno que
 *     calcula luz ambiental, difusa direccional (sol), atenuación
 *     de farolas y conos de los faros del vehículo.
 *   • Dibujar los postes y bombillas en las mismas posiciones
 *     usadas para el cálculo de luz, garantizando coherencia visual.
 *
 * Hereda de Conduccion (ciudad + vehículo).
 * Entregas extiende esta clase añadiendo entregas, decoración y minimapa.
 */
public class Iluminacion extends Conduccion {

    // ================================================================
    // 1. ESTADO DE ILUMINACIÓN
    // ================================================================

    /**
     * Modo de iluminación activo.
     * true  → ambiente nocturno (farolas activas, cielo oscuro).
     * false → ambiente diurno (luz solar, farolas apagadas).
     * Valor inicial true para ver la iluminación desde el primer cuadro.
     */
    protected boolean noche = true;

    /**
     * Estado de los faros del automóvil.
     * true  → focos delanteros encendidos y proyectando luz al suelo.
     * false → focos apagados (el vehículo sigue visible pero sin iluminar).
     */
    protected boolean faros = true;

    /**
     * Posiciones XYZ de las 16 farolas distribuidas en el mapa 11×11.
     *
     * Criterios de distribución:
     *   • Al menos una farola por cada bloque de 4 manzanas.
     *   • Situadas en las esquinas de las intersecciones (sobre la acera),
     *     a Y = 4.5 u (altura de la bombilla sobre el suelo).
     *   • Cubren el área completa de −55 a +55 en X y Z.
     *
     * Con LIMITE=55 y CELDA=10, los centros de calle quedan en:
     *   X: −50, −30, −10, +10, +30, +50
     *   Z: −50, −30, −10, +10, +30, +50
     * Las farolas se colocan desplazadas 4 u respecto al cruce para
     * quedar dentro de la acera de la manzana contigua.
     *
     * El shader acepta exactamente NUM_LUCES fuentes puntuales;
     * cambiar este valor requiere actualizar la constante y el GLSL.
     */
    protected static final int NUM_LUCES = 16; // Número de farolas; debe coincidir con el shader.

    protected static final float[][] LUCES = {
        // ── Cuadrante noroeste ─────────────────────────────────
        { -44f, 4.5f, -44f }, // Intersección NO-lejana: borde noroeste.
        { -24f, 4.5f, -44f }, // Intersección N-centro-oeste.
        { -44f, 4.5f, -24f }, // Intersección O-centro-norte.
        { -24f, 4.5f, -24f }, // Intersección central noroeste.
        // ── Cuadrante noreste ──────────────────────────────────
        {  24f, 4.5f, -44f }, // Intersección N-centro-este.
        {  44f, 4.5f, -44f }, // Intersección NE-lejana.
        {  24f, 4.5f, -24f }, // Intersección central noreste.
        {  44f, 4.5f, -24f }, // Intersección O-centro-sur-este.
        // ── Cuadrante suroeste ─────────────────────────────────
        { -44f, 4.5f,  24f }, // Intersección SO-norte.
        { -24f, 4.5f,  24f }, // Intersección central suroeste.
        { -44f, 4.5f,  44f }, // Intersección SO-lejana.
        { -24f, 4.5f,  44f }, // Intersección S-centro-oeste.
        // ── Cuadrante sureste ──────────────────────────────────
        {  24f, 4.5f,  24f }, // Intersección central sureste.
        {  44f, 4.5f,  24f }, // Intersección SE-norte.
        {  24f, 4.5f,  44f }, // Intersección S-centro-este.
        {  44f, 4.5f,  44f }, // Intersección SE-lejana: borde sureste.
    };

    // ================================================================
    // 2. CONTROLES E INDICADORES
    // ================================================================

    /**
     * Añade los interruptores N (día/noche) y F (faros) a los controles
     * heredados de Conduccion (WASD, C, R, ESC).
     *
     * @param key  Código GLFW de la tecla pulsada.
     */
    @Override
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, C y R de CiudadBase/2.

        if (key == GLFW_KEY_N) {
            noche = !noche; // Alterna entre ambiente nocturno y diurno.
        }
        if (key == GLFW_KEY_F) {
            faros = !faros; // Enciende o apaga los focos del vehículo.
        }
    }

    /**
     * Devuelve los indicadores de iluminación para el título de la ventana.
     * Entregas llama super.estadoExtra() y concatena sus propios indicadores.
     *
     * @return  Cadena con el estado de día/noche y faros.
     */
    @Override
    protected String estadoExtra() {
        String ambiente   = noche ? "noche" : "dia";        // Texto del modo de luz ambiental.
        String estadoFaro = faros ? "ON"    : "OFF";        // Texto del estado de los focos.
        return " | N: " + ambiente + " | F: faros " + estadoFaro;
    }

    // ================================================================
    // 3. ENVÍO DE DATOS AL SHADER
    // ================================================================

    /**
     * Envía todos los uniforms de iluminación al shader antes de dibujar la escena.
     * Sobreescribe el método vacío de CiudadBase.
     *
     * Datos enviados:
     *   uNoche   → 1 si es de noche, 0 si es de día.
     *   uFaros   → 1 si los faros están encendidos.
     *   uEmision → 0 (iluminación normal); las bombillas lo ponen a 1 en escena().
     *   uLuces   → array con las posiciones XYZ de las NUM_LUCES farolas.
     *   uAuto    → posición del centro del vehículo a la altura de los faros.
     *   uFrente  → vector unitario que apunta hacia el frente del vehículo.
     */
    @Override
    protected void prepararLuces() {
        entero("uNoche", noche ? 1 : 0); // Comunica el modo de iluminación al shader.
        entero("uFaros", faros ? 1 : 0); // Activa o desactiva el cálculo de los focos.
        entero("uEmision", 0);           // Los objetos normales reciben iluminación calculada.

        // Envía la posición de cada farola al array GLSL uLuces[].
        for (int i = 0; i < LUCES.length; i++) {
            // El nombre del uniform sigue la sintaxis de GLSL para arrays: "uLuces[i]".
            vector("uLuces[" + i + "]", LUCES[i][0], LUCES[i][1], LUCES[i][2]);
        }

        // Dirección frontal del auto: necesaria para calcular el cono de los faros.
        float frenteX = -(float) Math.sin(angulo); // Componente X del frente (a partir del ángulo).
        float frenteZ = -(float) Math.cos(angulo); // Componente Z del frente.

        // Centro del vehículo elevado a la altura de los faros (Y = 0.7).
        vector("uAuto",   autoX, 0.7f, autoZ);
        vector("uFrente", frenteX, 0f, frenteZ); // Vector normalizado (Math.sin²+cos²=1).
    }

    // ================================================================
    // 4. GEOMETRÍA DE LAS FAROLAS
    // ================================================================

    /**
     * Dibuja la ciudad heredada, el vehículo y los postes con bombillas.
     * Las bombillas se marcan con uEmision=1 de noche para que el shader
     * las pinte a color pleno sin oscurecerlas por falta de luz incidente.
     */
    @Override
    protected void escena() {
        super.escena(); // Ciudad (CiudadBase) + vehículo (Conduccion).

        for (float[] luz : LUCES) {
            float x = luz[0]; // Coordenada X de esta farola.
            float y = luz[1]; // Altura de la bombilla.
            float z = luz[2]; // Coordenada Z de esta farola.

            // Poste cilíndrico (caja delgada y alta) de color gris oscuro.
            // Centrado en Y = 2.2 con altura 4.4 → base en Y=0 (asfalto), tope en Y=4.4.
            caja(x, 2.2f, z,  0.18f, 4.4f, 0.18f,  0.20f, 0.24f, 0.28f);

            // Bombilla: de noche se activa uEmision para que luzca encendida
            // sin depender de su propia iluminación (se iluminaría a sí misma → 0).
            if (noche) {
                entero("uEmision", 1); // La bombilla emite su color sin cálculo de luz.
            }
            // Cubo pequeño de color cálido (amarillo-anaranjado) representando la bombilla.
            caja(x, y, z,  0.7f, 0.35f, 0.7f,  1f, 0.83f, 0.42f);
            entero("uEmision", 0); // Devuelve iluminación normal para los objetos siguientes.
        }
    }

    // ================================================================
    // 5. SHADER DE FRAGMENTOS CON ILUMINACIÓN
    // ================================================================

    /**
     * Devuelve el código GLSL del shader de fragmentos que calcula iluminación.
     * Reemplaza el shader de color plano de CiudadBase.
     *
     * Modelo de iluminación implementado:
     *   • Luz ambiental: componente constante que ilumina todas las caras por igual.
     *     De día es blanca y moderada; de noche es azulada y tenue.
     *   • Luz difusa direccional (sol): sigue la ley de Lambert.
     *     El factor cos(θ) = dot(normal, dirSol) pondera cuánto mira la cara al sol.
     *     De noche su intensidad se reduce a 10 % para un ligero "moonlight".
     *   • Farolas (solo de noche): fuentes puntuales con atenuación cuadrática.
     *     La distancia d entra en la fórmula 1 / (1 + k1·d + k2·d²).
     *     El factor difuso de Lambert garantiza que solo las caras orientadas
     *     hacia la farola reciben luz de ella.
     *   • Faros del auto (si están encendidos): dos conos con suavizado en el borde.
     *     smoothstep convierte la alineación entre el vector superficie→faro y el eje
     *     del foco en una máscara de 0 (fuera del cono) a 1 (centro del cono).
     *   • Emisión: objetos con uEmision=1 devuelven su color directo sin cálculo.
     *     Usado para bombillas, ventanas nocturnas y el minimapa.
     *
     * El array uLuces tiene exactamente NUM_LUCES=16 entradas para coincidir
     * con el Java que las envía.  Cambiar NUM_LUCES requiere actualizar ambos.
     */
    @Override
    protected String fragmentShader() {
        return """
            #version 330 core

            // ── Entradas interpoladas desde el shader de vértices ──────────────
            in vec3 vMundo;   // Posición del fragmento en el espacio mundo.
            in vec3 vNormal;  // Normal transformada al espacio mundo.
            in vec2 vUV;      // Coordenada de textura con tiling aplicado.

            // ── Uniforms de material y textura ────────────────────────────────
            uniform vec3      uColor;     // Color base del material.
            uniform sampler2D uTex;       // Textura activa (unidad 0).
            uniform int       uUsaTex;    // 1 = mezclar textura; 0 = solo color.

            // ── Uniforms de iluminación ────────────────────────────────────────
            uniform vec3 uLuces[16];
            uniform vec3 uAuto;
            uniform vec3 uFrente;
            uniform int  uNoche;
            uniform int  uFaros;
            uniform int  uEmision;
            uniform int  uMapa;

            out vec4 color;

            void main() {
                // ── Color base: textura × color del material, o solo color ─────
                vec3 base = uColor;
                if (uUsaTex == 1) {
                    // Multiplicar textura por uColor permite variar el tono de cada
                    // edificio conservando el patrón de ladrillo/concreto/etc.
                    base = uColor * texture(uTex, vUV).rgb;
                }

                // ── Emisión y minimapa: color base directo sin luz ────────────
                if (uEmision == 1 || uMapa == 1) {
                    color = vec4(base, 1.0);
                    return;
                }

                vec3 normal = normalize(vNormal);

                // ── Iluminación ambiental + sol ───────────────────────────────
                vec3  luz;
                float intensidadSol;
                if (uNoche == 1) {
                    luz           = vec3(0.10, 0.13, 0.20);
                    intensidadSol = 0.08;
                } else {
                    luz           = vec3(0.46, 0.46, 0.44);
                    intensidadSol = 0.62;
                }
                vec3  dirSol     = normalize(vec3(0.4, 1.0, 0.3));
                float lambertSol = max(dot(normal, dirSol), 0.0);
                luz += vec3(intensidadSol) * lambertSol;

                // ── Farolas (solo de noche) ───────────────────────────────────
                if (uNoche == 1) {
                    for (int i = 0; i < 16; i++) {
                        vec3  haciaLuz  = uLuces[i] - vMundo;
                        float distancia = length(haciaLuz);
                        float difusa    = max(dot(normal, normalize(haciaLuz)), 0.0);
                        float atenuacion = 1.0 + 0.10 * distancia + 0.04 * distancia * distancia;
                        luz += vec3(1.0, 0.73, 0.34) * difusa * 3.5 / atenuacion;
                    }
                }

                // ── Faros del automóvil ───────────────────────────────────────
                if (uFaros == 1) {
                    for (int i = 0; i < 2; i++) {
                        vec3  lateral = vec3(-uFrente.z, 0.0, uFrente.x);
                        float sep     = (i == 0) ? -0.55 : 0.55;
                        vec3  origen  = uAuto + uFrente * 1.36 + lateral * sep;
                        vec3  hacia   = vMundo - origen;
                        float dist    = length(hacia);
                        vec3  eje     = normalize(uFrente + vec3(0.0, -0.10, 0.0));
                        float alin    = dot(normalize(hacia), eje);
                        float cono    = smoothstep(0.85, 0.97, alin);
                        float difusa  = max(dot(normal, -normalize(hacia)), 0.0);
                        float atenu   = 1.0 + 0.04 * dist * dist;
                        luz += vec3(1.0, 0.94, 0.72) * cono * difusa * 8.0 / atenu;
                    }
                }

                // ── Color final: base iluminada ───────────────────────────────
                color = vec4(base * luz, 1.0);
            }
            """;
    }

    // ================================================================
    // 6. PUNTO DE ENTRADA
    // ================================================================

    /** Ejecuta la tercera lección (ciudad + vehículo + iluminación) directamente. */
    public static void main(String[] args) {
        new Iluminacion().run();
    }
}
