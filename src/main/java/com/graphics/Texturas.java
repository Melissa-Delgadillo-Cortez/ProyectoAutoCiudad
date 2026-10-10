package com.graphics;

import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL33.*;

/**
 * CLASE 6 — TEXTURAS PROCEDURALES.
 *
 * Extiende CiudadViva añadiendo texturas generadas en Java (sin archivos externos).
 * Cada textura se construye píxel a píxel usando aritmética de enteros y
 * ruido basado en hash; el resultado se sube a la GPU con glTexImage2D.
 *
 * Texturas generadas (índices 0‑5):
 *   0 ASFALTO   — gris oscuro con grietas y guijarros sutiles.
 *   1 LADRILLO  — filas alternadas de ladrillos rojos con juntas de mortero.
 *   2 CONCRETO  — gris claro con variación granulada y manchas.
 *   3 CESPED    — verde con variación de hierba y pequeñas flores.
 *   4 MADERA    — vetas horizontales color madera oscura.
 *   5 VIDRIO    — azul translúcido con reflejo especular diagonal.
 *
 * Uso en escena:
 *   Antes de dibujar un grupo de objetos, llama a usarTextura(ID, tiling).
 *   Para desactivar texturas (ej. autos, peatones), llama a sinTextura().
 *
 * Jerarquía: Texturas → CiudadViva → Entregas → Iluminacion → Conduccion → CiudadBase
 */
public class Texturas extends CiudadViva {

    // ── IDs de textura ────────────────────────────────────────────────────
    /** Índice de la textura de asfalto. */
    public static final int TEX_ASFALTO  = 0;
    /** Índice de la textura de ladrillo. */
    public static final int TEX_LADRILLO = 1;
    /** Índice de la textura de concreto / acera. */
    public static final int TEX_CONCRETO = 2;
    /** Índice de la textura de césped. */
    public static final int TEX_CESPED   = 3;
    /** Índice de la textura de madera. */
    public static final int TEX_MADERA   = 4;
    /** Índice de la textura de vidrio / fachada moderna. */
    public static final int TEX_VIDRIO   = 5;

    /** Total de texturas. */
    private static final int N_TEX = 6;

    /** Resolución de cada textura en píxeles (potencia de 2 para mipmaps). */
    private static final int RES = 128;

    /** IDs OpenGL de las N_TEX texturas. */
    private final int[] texIds = new int[N_TEX];

    // ================================================================
    // 1. CICLO DE VIDA — carga y limpieza de texturas
    // ================================================================

    /**
     * Crea las texturas después de que OpenGL esté listo.
     * Se llama sobreescribiendo run() para intercalar la creación
     * entre iniciar() y loop(), momento en que el contexto GL ya existe.
     */
    @Override
    public void run() {
        // super.run() llama internamente iniciar() → loop() → limpiar().
        // Necesitamos crear las texturas DENTRO de iniciar(), pero ese método
        // es private en CiudadBase. La solución: crear las texturas en el primer
        // cuadro de actualizar(), protegidas por una bandera.
        super.run();
    }

    /** Bandera para crear las texturas solo una vez, en el primer cuadro. */
    private boolean texturasCreadas = false;

    /**
     * Genera todas las texturas en el primer cuadro, cuando el contexto GL
     * ya está activo. Después actualiza normalmente.
     */
    @Override
    protected void actualizar(float dt) {
        if (!texturasCreadas) {
            crearTodasLasTexturas(); // Genera y sube a la GPU las 6 texturas.
            texturasCreadas = true;
        }
        super.actualizar(dt); // Jugador, peatones, agentes.
    }

    /**
     * Genera las 6 texturas procedurales y las sube a la GPU.
     * Cada textura se crea como un array de bytes RGBA en memoria Java,
     * luego se copia a OpenGL con glTexImage2D y se configuran filtros.
     */
    private void crearTodasLasTexturas() {
        for (int i = 0; i < N_TEX; i++) {
            texIds[i] = glGenTextures();              // Reserva un ID en la GPU.
            glBindTexture(GL_TEXTURE_2D, texIds[i]);  // Selecciona esta textura.

            // Filtrado bilineal: suaviza la textura al ampliarla o reducirla.
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            // Repetición en ambos ejes (para tiling).
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

            // Genera los píxeles según el tipo de textura.
            ByteBuffer pixels = generarTextura(i);

            // Sube los datos RGBA a la GPU.
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8,
                         RES, RES, 0,
                         GL_RGBA, GL_UNSIGNED_BYTE, pixels);

            // Genera los mipmaps automáticamente para calidad a distancia.
            glGenerateMipmap(GL_TEXTURE_2D);

            MemoryUtil.memFree(pixels); // Libera el buffer temporal de Java.
        }
        glBindTexture(GL_TEXTURE_2D, 0); // Desvincula la textura activa.

        // Activa el programa de shaders ANTES de configurar sus uniforms.
        // Necesario porque en el primer cuadro, actualizar() se ejecuta
        // antes que dibujarFrame() (que es quien normalmente activa el programa).
        glUseProgram(programa);

        // Activa la unidad de textura 0 y avisa al shader dónde leer. 
        glActiveTexture(GL_TEXTURE0);

        entero("uTex", 0); // El sampler2D uTex lee de la unidad 0.
    }

    /**
     * Genera los píxeles RGBA de una textura en función de su índice.
     *
     * @param tipo  Índice de la textura (0‑5).
     * @return      ByteBuffer con RES×RES×4 bytes (RGBA), listo para glTexImage2D.
     */
    private ByteBuffer generarTextura(int tipo) {
        ByteBuffer buf = MemoryUtil.memAlloc(RES * RES * 4);
        for (int py = 0; py < RES; py++) {
            for (int px = 0; px < RES; px++) {
                int[] rgba = switch (tipo) {
                    case TEX_ASFALTO  -> pixelAsfalto(px, py);
                    case TEX_LADRILLO -> pixelLadrillo(px, py);
                    case TEX_CONCRETO -> pixelConcreto(px, py);
                    case TEX_CESPED   -> pixelCesped(px, py);
                    case TEX_MADERA   -> pixelMadera(px, py);
                    case TEX_VIDRIO   -> pixelVidrio(px, py);
                    default           -> new int[]{128, 128, 128, 255};
                };
                buf.put((byte) rgba[0]); // R
                buf.put((byte) rgba[1]); // G
                buf.put((byte) rgba[2]); // B
                buf.put((byte) rgba[3]); // A (siempre 255 = opaco)
            }
        }
        buf.flip(); // Rebobina el buffer para que glTexImage2D lo lea desde el inicio.
        return buf;
    }

    // ================================================================
    // 2. GENERADORES DE PÍXELES PROCEDURALES
    // ================================================================

    /**
     * Función de ruido de hash rápido: mezcla dos enteros y devuelve 0‑255.
     * Basado en el operador xor-shift; determinista pero con aspecto aleatorio.
     * Se usa para añadir variación granulada a todas las texturas.
     *
     * @param x, y  Coordenadas del píxel.
     * @param seed  Semilla para variar el resultado entre capas de ruido.
     * @return      Valor entre 0 y 255.
     */
    private static int ruido(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed;
        h = (h ^ (h >> 13)) * 1274126177;
        return (h ^ (h >> 16)) & 0xFF;
    }

    /**
     * Ruido bilineal suavizado: interpola entre cuatro esquinas de una celda.
     * Produce gradientes continuos en lugar de bloques, dando aspecto orgánico.
     *
     * @param x, y   Coordenadas del píxel en la textura.
     * @param escala Tamaño de cada celda de ruido (mayor = más suave).
     * @param seed   Semilla de la capa de ruido.
     * @return       Valor entre 0 y 255.
     */
    private static int ruidoSuave(int x, int y, int escala, int seed) {
        int cx = x / escala, cy = y / escala;   // Celda a la que pertenece el píxel.
        int fx = x % escala, fy = y % escala;   // Fracción dentro de la celda.

        // Cuatro esquinas de la celda.
        int v00 = ruido(cx,   cy,   seed);
        int v10 = ruido(cx+1, cy,   seed);
        int v01 = ruido(cx,   cy+1, seed);
        int v11 = ruido(cx+1, cy+1, seed);

        // Interpolación bilineal suavizada con curva cúbica (smoothstep).
        float tx = (float)(fx * fx * (3*escala - 2*fx)) / (escala * escala * escala);
        float ty = (float)(fy * fy * (3*escala - 2*fy)) / (escala * escala * escala);

        float v0 = v00 + tx * (v10 - v00);  // Interpolación horizontal inferior.
        float v1 = v01 + tx * (v11 - v01);  // Interpolación horizontal superior.
        return Math.round(v0 + ty * (v1 - v0)); // Interpolación vertical.
    }

    /** Clamp de int al rango [0, 255]. */
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    // ── Textura 0: ASFALTO ─────────────────────────────────────────────────

    /**
     * Asfalto oscuro con variación granulada y grietas finas.
     * Base gris oscuro (50-80) + ruido de granulado + líneas de grieta.
     */
    private static int[] pixelAsfalto(int px, int py) {
        int base  = 58 + ruidoSuave(px, py, 16, 1) / 8;  // Variación granulada.
        int grano = ruido(px, py, 7) / 20;               // Textura fina de superficie.
        int v     = clamp(base + grano);

        // Grietas: líneas diagonales muy finas y oscuras.
        boolean grieta = ((px * 3 + py * 7) % 97 == 0) || ((px * 11 + py * 5) % 113 == 0);
        if (grieta) v = clamp(v - 30);

        return new int[]{v, v, v + 4, 255}; // Ligeramente azulado por el betún.
    }

    // ── Textura 1: LADRILLO ────────────────────────────────────────────────

    /**
     * Patrón de aparejo inglés con filas alternadas de ladrillos.
     * Los ladrillos son rojos-anaranjados; la junta es beige claro.
     * Cada fila alterna el desplazamiento horizontal en medio ladrillo.
     */
    private static int[] pixelLadrillo(int px, int py) {
        final int ALTO_L = 14;  // Altura de un ladrillo en píxeles.
        final int ANCHO_L = 28; // Ancho de un ladrillo en píxeles.
        final int JUNTA   = 2;  // Grosor de la junta en píxeles.

        int fila = py / (ALTO_L + JUNTA); // Qué fila de ladrillos.
        int ly   = py % (ALTO_L + JUNTA); // Y dentro de la fila.

        // Las filas impares se desplazan medio ladrillo (aparejo).
        int offsetX = (fila % 2 == 0) ? 0 : ANCHO_L / 2;
        int lx = (px + offsetX) % (ANCHO_L + JUNTA); // X dentro del ladrillo.

        boolean esJunta = (ly >= ALTO_L) || (lx >= ANCHO_L);

        if (esJunta) {
            // Junta de mortero: beige/gris claro con variación.
            int j = 190 + ruido(px, py, 3) / 15;
            return new int[]{j, j - 8, j - 15, 255};
        } else {
            // Ladrillo: rojo anaranjado con variación de tono y superficie.
            int varR = ruido(px / 4, py / 4, fila * 31 + (px + offsetX) / (ANCHO_L + JUNTA)) / 10;
            int varS = ruidoSuave(px, py, 4, 99) / 25; // Variación de superficie.
            int r = clamp(185 + varR + varS);
            int g = clamp(78  + varR / 2);
            int b = clamp(42  + varR / 3);
            return new int[]{r, g, b, 255};
        }
    }

    // ── Textura 2: CONCRETO ────────────────────────────────────────────────

    /**
     * Concreto de acera: gris claro con variación granulada y manchas de humedad.
     */
    private static int[] pixelConcreto(int px, int py) {
        int base   = 185 + ruidoSuave(px, py, 24, 5) / 12; // Variación lenta.
        int grano  = ruido(px, py, 11) / 18;               // Grano fino.
        int mancha = ruidoSuave(px, py, 40, 42) > 200 ?
                     -ruidoSuave(px, py, 12, 77) / 10 : 0; // Manchas oscuras esporádicas.
        int v = clamp(base + grano + mancha);
        return new int[]{v, v, v - 2, 255}; // Ligeramente cálido.
    }

    // ── Textura 3: CÉSPED ──────────────────────────────────────────────────

    /**
     * Césped verde con variación de hierba y pequeños puntos de flores.
     */
    private static int[] pixelCesped(int px, int py) {
        int varVerde = ruidoSuave(px, py, 12, 17) / 6;
        int r = clamp(28  + ruido(px, py, 2) / 30);
        int g = clamp(120 + varVerde);
        int b = clamp(35  + ruido(px, py, 9) / 20);

        // Flores blancas y amarillas muy dispersas (1 de cada ~200 píxeles).
        int rFlor = ruido(px, py, 55);
        if (rFlor > 252) { r = 250; g = 240; b = 80; }  // Flor amarilla.
        if (rFlor < 3)   { r = 240; g = 240; b = 240; } // Flor blanca.

        return new int[]{r, g, b, 255};
    }

    // ── Textura 4: MADERA ──────────────────────────────────────────────────

    /**
     * Madera con vetas horizontales. Color marrón cálido con alternancia
     * clara/oscura y variación longitudinal.
     */
    private static int[] pixelMadera(int px, int py) {
        // Vetas: función seno del eje Y con ruido lateral para hacerlas orgánicas.
        float veta = (float) Math.sin((py + ruido(px, py, 13) / 8.0) * Math.PI / 7.0);
        int varVeta = (int)(veta * 22);          // Amplitud de la veta.
        int varLong = ruidoSuave(px, py, 20, 77) / 12; // Variación longitudinal.

        int r = clamp(140 + varVeta + varLong);
        int g = clamp(82  + varVeta / 2 + varLong / 2);
        int b = clamp(38  + varLong / 3);
        return new int[]{r, g, b, 255};
    }

    // ── Textura 5: VIDRIO ──────────────────────────────────────────────────

    /**
     * Fachada de vidrio azulado con un reflejo especular diagonal.
     * Simula la apariencia de cristal tintado de un edificio moderno.
     */
    private static int[] pixelVidrio(int px, int py) {
        // Gradiente base azul-gris.
        int base = 160 + ruidoSuave(px, py, 32, 33) / 12;

        // Reflejo especular: banda diagonal clara.
        float t = (px + py) / (float)(RES * 2);
        int reflejo = (int)(Math.max(0, Math.sin(t * Math.PI * 3)) * 40);

        // Variación de panel (rejillas de 32×32 píxeles para simular paneles).
        boolean bordePanel = (px % 32 < 2) || (py % 32 < 2);
        int marcaPanel = bordePanel ? -20 : 0;

        int r = clamp(base / 3 + reflejo + marcaPanel);
        int g = clamp(base / 2 + reflejo + marcaPanel);
        int b = clamp(base     + reflejo + marcaPanel);
        return new int[]{r, g, b, 255};
    }

    // ================================================================
    // 3. ACTIVACIÓN DE TEXTURAS
    // ================================================================

    /**
     * Activa una textura específica para las siguientes llamadas de dibujo.
     * Envía al shader:
     *   uUsaTex = 1        → activa la mezcla textura × color.
     *   uTileUV = tiling   → repite la textura N veces por cara.
     *
     * @param id      Índice de la textura (TEX_ASFALTO, TEX_LADRILLO, etc.).
     * @param tiling  Factor de repetición (1 = una vez, 4 = cuatro veces…).
     */
    protected void usarTextura(int id, float tiling) {
        glActiveTexture(GL_TEXTURE0);        // Selecciona la unidad 0.
        glBindTexture(GL_TEXTURE_2D, texIds[id]); // Liga esta textura.
        entero("uUsaTex", 1);               // Activa la mezcla en el shader.
        decimal("uTileUV", tiling);         // Configura el tiling.
    }

    /**
     * Desactiva la textura para los objetos que no la necesitan
     * (autos, peatones, semáforos, bombillas).
     */
    protected void sinTextura() {
        entero("uUsaTex", 0);   // Desactiva la mezcla de textura.
        decimal("uTileUV", 1f); // Valor neutro.
    }

    // ================================================================
    // 4. ESCENA CON TEXTURAS APLICADAS POR SUPERFICIE
    // ================================================================

    /**
     * Sobreescribe escena() de CiudadViva para envolver cada tipo de superficie
     * con la textura apropiada.
     *
     * Asignación de texturas:
     *   Suelo de asfalto      → TEX_ASFALTO  (tiling 8)
     *   Marcas de calle       → sin textura  (color amarillo directo)
     *   Aceras                → TEX_CONCRETO (tiling 3)
     *   Edificios (fachada)   → TEX_LADRILLO o TEX_VIDRIO según paleta
     *   Parques / césped      → TEX_CESPED   (tiling 4)
     *   Bancos y madera       → TEX_MADERA   (tiling 2)
     *   Resto (autos, people) → sin textura
     */
    @Override
    protected void escena() {
        if (!texturasCreadas) {
            // Si las texturas aún no están listas (primer frame antes de actualizar),
            // dibuja sin texturas para no romper el primer cuadro.
            sinTextura();
            super.escena();
            return;
        }

        // ── 1. Suelo de asfalto ───────────────────────────────────────────────
        usarTextura(TEX_ASFALTO, 8f); // 8 repeticiones para que los píxeles sean finos.
        caja(0f, -0.25f, 0f,
             CELDA * MAPA[0].length, 0.5f, CELDA * MAPA.length,
             0.22f, 0.22f, 0.22f); // Color base neutro: la textura aporta el detalle.

        // ── 2. Recorre el mapa celda a celda ──────────────────────────────────
        for (int fi = 0; fi < MAPA.length; fi++) {
            for (int co = 0; co < MAPA[fi].length; co++) {
                float x = centro(co), z = centro(fi);
                int tipo = MAPA[fi][co];

                if (tipo == 0) {
                    // Marcas de calle: color plano amarillo, sin textura.
                    sinTextura();
                    dibujarMarcasCalleT(fi, co, x, z);
                } else {
                    // Acera de concreto común a todas las manzanas.
                    usarTextura(TEX_CONCRETO, 3f);
                    caja(x, 0.15f, z, 10f, 0.3f, 10f, 0.72f, 0.72f, 0.70f);

                    if (tipo == 1) {
                        dibujarEdificioT(fi, co, x, z); // Edificio con textura.
                    } else {
                        dibujarParqueT(fi, co, x, z);   // Parque / jardín con textura.
                    }
                }
            }
        }

        // ── 3. Farolas ────────────────────────────────────────────────────────
        sinTextura();
        for (float[] luz : LUCES) {
            caja(luz[0], 2.2f, luz[2], 0.18f, 4.4f, 0.18f, 0.20f, 0.24f, 0.28f);
            if (noche) entero("uEmision", 1);
            caja(luz[0], luz[1], luz[2], 0.7f, 0.35f, 0.7f, 1f, 0.83f, 0.42f);
            entero("uEmision", 0);
        }

        // ── 4. Vehículo del jugador, peatones y agentes ───────────────────────
        sinTextura(); // Los vehículos y personajes usan colores planos.
        dibujarAuto();
        if (!vistaMapa) {
            decorarCiudadT(); // Semáforos y pasos peatonales.
            for (int i = 0; i < NP; i++) dibujarPeaton(i);
        }
        for (int i = 0; i < NA; i++) dibujarAgente(i);

        // ── 5. Destino activo (Entregas lo gestiona) ─────────────────────────────
        // dibujarFrame() llama a this.escena() y Entregas dibuja el destino
        // después de esta llamada a través del polimorfismo. No hace falta
        // hacer nada aquí.
    }

    // ── Marcas de calle (reimplementadas para evitar llamar a private de CiudadBase) ─

    private void dibujarMarcasCalleT(int fi, int co, float x, float z) {
        if (fi % 2 == 0 && co % 2 == 1)
            for (int d = -3; d <= 3; d += 3)
                caja(x+d, 0.025f, z, 1.6f,0.03f,0.13f, 1f,0.84f,0.35f);
        if (co % 2 == 0 && fi % 2 == 1)
            for (int d = -3; d <= 3; d += 3)
                caja(x, 0.025f, z+d, 0.13f,0.03f,1.6f, 1f,0.84f,0.35f);
    }

    // ── Edificio con texturas por zona ─────────────────────────────────────

    /**
     * Dibuja el edificio detallado aplicando texturas según cada elemento:
     *   • Zócalo, cornisa, remate → TEX_CONCRETO
     *   • Fachada → TEX_LADRILLO (edificios cálidos) o TEX_VIDRIO (modernos)
     *   • Ventanas → sin textura (color directo)
     *   • Puerta, escalones → TEX_MADERA / TEX_CONCRETO
     */
    private void dibujarEdificioT(int fi, int co, float x, float z) {
        float[] pal  = PALETA[(fi * 3 + co * 5) % PALETA.length];
        float r = pal[0], g = pal[1], b = pal[2];
        float alt  = 5f + (fi * 3 + co * 7) % 9;
        int   rem  = (fi + co) % 5;
        float mr = Math.min(1f, r+0.22f), mg = Math.min(1f, g+0.22f), mb = Math.min(1f, b+0.22f);
        float zr = r*0.62f, zg = g*0.62f, zb = b*0.62f;

        // Zócalo de concreto.
        usarTextura(TEX_CONCRETO, 2f);
        caja(x, 0.75f, z, 7.1f,0.9f,7.1f, zr,zg,zb);

        // Fachada: ladrillo para tonos cálidos (r>0.5), vidrio para fríos/oscuros.
        boolean esModerno = (r < 0.5f);
        int texFachada = esModerno ? TEX_VIDRIO : TEX_LADRILLO;
        usarTextura(texFachada, 3f);
        caja(x, alt/2f+0.8f, z, 7f,alt-0.5f,7f, r,g,b);

        // Cornisa de concreto.
        usarTextura(TEX_CONCRETO, 2f);
        caja(x, alt+0.45f, z, 7.5f,0.28f,7.5f, zr,zg,zb);

        // Remate de tejado.
        float base = alt + 0.6f;
        switch (rem) {
            case 0 -> { caja(x,base+0.3f,z,7.4f,0.5f,0.25f,zr,zg,zb);
                        caja(x,base+0.3f,z,0.25f,0.5f,7.4f,zr,zg,zb); }
            case 1 -> { usarTextura(texFachada, 2f);
                        caja(x,base+1.2f,z,3f,2.4f,3f,r*.85f,g*.85f,b*.85f);
                        usarTextura(TEX_CONCRETO,2f);
                        caja(x,base+2.5f,z,3.3f,0.2f,3.3f,zr,zg,zb);
                        caja(x,base+2.8f,z,1.5f,0.6f,1.5f,zr,zg,zb); }
            case 2 -> { usarTextura(texFachada, 2f);
                        caja(x,base+0.4f,z,6.5f,0.8f,5f,  r*.80f,g*.80f,b*.80f);
                        caja(x,base+0.9f,z,5f,0.8f,3.5f,  r*.70f,g*.70f,b*.70f);
                        caja(x,base+1.4f,z,3.5f,0.7f,2f,  r*.60f,g*.60f,b*.60f); }
            case 3 -> { usarTextura(texFachada,2f);
                        caja(x,base+0.5f,z,5.5f,1f,5.5f,r*.75f,g*.75f,b*.75f);
                        caja(x,base+1.1f,z,4f,1f,4f,  r*.65f,g*.65f,b*.65f);
                        caja(x,base+1.7f,z,2.5f,1f,2.5f,r*.55f,g*.55f,b*.55f);
                        caja(x,base+2.3f,z,1f,1f,1f,  r*.45f,g*.45f,b*.45f); }
            case 4 -> { usarTextura(TEX_CONCRETO,2f);
                        caja(x-1.5f,base+0.6f,z,2.5f,1.2f,3.5f,zr,zg,zb);
                        sinTextura(); entero("uEmision",1);
                        caja(x,base+2.1f,z+2f,.35f,.12f,.35f,.9f,.15f,.05f);
                        entero("uEmision",0); }
        }

        // Ventanas: sin textura para que el color directo sea limpio.
        sinTextura();
        if (noche) entero("uEmision",1);
        for (float y = 2.2f; y < alt-0.5f; y += 2.2f) {
            int piso = (int)(y/2.2f);
            for (float d = -2f; d <= 2f; d += 2f) {
                caja(x+d,    y,z-3.52f,1.05f,1.1f,0.05f, mr,mg,mb);
                caja(x+d,    y,z+3.52f,1.05f,1.1f,0.05f, mr,mg,mb);
                caja(x-3.52f,y,z+d,    0.05f,1.1f,1.05f, mr,mg,mb);
                caja(x+3.52f,y,z+d,    0.05f,1.1f,1.05f, mr,mg,mb);
                boolean ab = noche||(piso+(int)(d+2))%2==0;
                float cr=ab?0.98f:r*.45f,cg=ab?0.92f:g*.45f,cb=ab?0.55f:b*.45f;
                caja(x+d,    y,z-3.53f,0.75f,0.75f,0.04f,cr,cg,cb);
                caja(x+d,    y,z+3.53f,0.75f,0.75f,0.04f,cr,cg,cb);
                caja(x-3.53f,y,z+d,    0.04f,0.75f,0.75f,cr,cg,cb);
                caja(x+3.53f,y,z+d,    0.04f,0.75f,0.75f,cr,cg,cb);
            }
        }
        entero("uEmision",0);

        // Puerta: madera + escalones de concreto.
        float fz = z + 3.52f;
        usarTextura(TEX_CONCRETO, 2f);
        caja(x-0.65f,1.3f,fz,0.15f,2.4f,0.06f,zr,zg,zb);
        caja(x+0.65f,1.3f,fz,0.15f,2.4f,0.06f,zr,zg,zb);
        caja(x,2.5f,fz,1.5f,0.2f,0.07f,zr,zg,zb);
        usarTextura(TEX_MADERA, 2f);
        caja(x-0.34f,1.3f,fz,0.54f,2.1f,0.06f,0.45f,0.27f,0.10f);
        caja(x+0.34f,1.3f,fz,0.54f,2.1f,0.06f,0.42f,0.25f,0.09f);
        usarTextura(TEX_CONCRETO,2f);
        caja(x,0.40f,fz+0.28f,1.8f,0.14f,0.50f,zr,zg,zb);
        caja(x,0.26f,fz+0.58f,2.0f,0.12f,0.50f,zr,zg,zb);
        caja(x,0.14f,fz+0.88f,2.2f,0.12f,0.50f,zr,zg,zb);
        sinTextura();
        if (noche) entero("uEmision",1);
        caja(x,2.8f,fz,1.2f,0.26f,0.06f,0.95f,0.90f,0.75f);
        entero("uEmision",0);
    }

    // ── Jardín con texturas ────────────────────────────────────────────────

    private void dibujarParqueT(int fi, int co, float x, float z) {
        int var = (fi + co * 2) % 3;

        // Césped.
        usarTextura(TEX_CESPED, 4f);
        caja(x, 0.33f, z, 9f,0.1f,9f, 0.85f,0.85f,0.85f); // Blanco neutro: textura da el color.

        // Camino de concreto.
        usarTextura(TEX_CONCRETO, 2f);
        caja(x,0.37f,z,1.2f,0.08f,9f,  0.72f,0.70f,0.65f);
        caja(x,0.37f,z,9f,  0.08f,1.2f, 0.72f,0.70f,0.65f);

        // Árboles (tronco madera + copa sin textura).
        for (float dx : new float[]{-2.8f,2.8f})
            for (float dz : new float[]{-2.8f,2.8f}) {
                usarTextura(TEX_MADERA,2f);
                caja(x+dx,1.4f,z+dz,0.42f,2.8f,0.42f,0.35f,0.20f,0.10f);
                sinTextura();
                caja(x+dx,3.1f,z+dz,2.4f,1.5f,2.4f,0.10f,0.38f,0.18f);
                caja(x+dx,4.0f,z+dz,1.6f,1.2f,1.6f,0.13f,0.45f,0.22f);
                caja(x+dx,4.7f,z+dz,0.8f,0.8f,0.8f,0.15f,0.50f,0.25f);
            }

        // Setos y flores: sin textura (color plano más rápido).
        sinTextura();
        caja(x,0.82f,z-3.8f,6.5f,0.92f,0.82f,0.12f,0.36f,0.16f);
        caja(x,0.82f,z+3.8f,6.5f,0.92f,0.82f,0.12f,0.36f,0.16f);
        caja(x-3.8f,0.82f,z,0.82f,0.92f,6.5f,0.12f,0.36f,0.16f);
        caja(x+3.8f,0.82f,z,0.82f,0.92f,6.5f,0.12f,0.36f,0.16f);
        float[][] cf = switch(var){
            case 0 -> new float[][]{{1f,.2f,.2f},{1f,.55f,0},{1f,.9f,.1f}};
            case 1 -> new float[][]{{.5f,.1f,.9f},{.2f,.4f,1f},{.1f,.8f,.8f}};
            default-> new float[][]{{1f,.1f,.6f},{.9f,.9f,.9f},{.3f,1f,.4f}};
        };
        float[][] pf = {{x-1.4f,z-1.4f},{x+1.4f,z-1.4f},{x-1.4f,z+1.4f},
                        {x+1.4f,z+1.4f},{x,z-3f},{x,z+3f}};
        for (int k=0;k<pf.length;k++) {
            caja(pf[k][0],.55f,pf[k][1],.12f,.4f,.12f,.15f,.50f,.15f);
            if (noche) entero("uEmision",1);
            float[] c=cf[k%cf.length];
            caja(pf[k][0],.83f,pf[k][1],.32f,.18f,.32f,c[0],c[1],c[2]);
            entero("uEmision",0);
        }

        // Elemento central.
        switch (var) {
            case 0 -> {
                for (int k=0;k<8;k++){float a=(float)(k*Math.PI/4);
                    usarTextura(TEX_CONCRETO,1f);
                    caja(x+(float)Math.cos(a)*1.5f,.55f,z+(float)Math.sin(a)*1.5f,.55f,.45f,.55f,.62f,.64f,.68f);}
                caja(x,.85f,z,.26f,1.1f,.26f,.65f,.67f,.70f);
                caja(x,1.45f,z,.7f,.2f,.7f,.62f,.64f,.68f);
                sinTextura(); entero("uEmision",1);
                caja(x,.38f,z,2.8f,.04f,2.8f,.25f,.55f,.85f); entero("uEmision",0);
            }
            case 1 -> {
                sinTextura();
                caja(x,.70f,z,1.2f,.8f,1.2f,.50f,.48f,.55f);
                caja(x,1.45f,z,.9f,.9f,.9f,.70f,.68f,.75f);
                cajaGirada(x,2.05f,z,.7f,.7f,.7f,.80f,.78f,.85f,.78f);
                caja(x,2.55f,z,.4f,.5f,.4f,.90f,.88f,.95f);
            }
            case 2 -> {
                usarTextura(TEX_CONCRETO,1f);
                caja(x,.36f,z,2.5f,.1f,2.5f,.68f,.62f,.52f);
                sinTextura();
                float[][] dirs={{1.8f,0},{-1.8f,0},{0,1.8f},{0,-1.8f}};
                float[] angs={(float)-Math.PI/2,(float)Math.PI/2,0f,(float)Math.PI};
                for (int k=0;k<4;k++){float bx=x+dirs[k][0],bz=z+dirs[k][1];
                    usarTextura(TEX_MADERA,1f);
                    cajaGirada(bx,.52f,bz,1.4f,.15f,.5f,.52f,.30f,.12f,angs[k]);
                    sinTextura();
                    cajaGirada(bx,.42f,bz,1.2f,.35f,.2f,.22f,.24f,.24f,angs[k]);}
            }
        }
    }

    // ── Decoración de calles (semáforos y pasos peatonales) ───────────────

    private void decorarCiudadT() {
        for (int fi=0;fi<MAPA.length;fi++)
            for (int co=0;co<MAPA[fi].length;co++) {
                if (MAPA[fi][co]==0) continue;
                float x=centro(co),z=centro(fi);
                sinTextura();
                dibujarPasoCebra(x,z);
                dibujarSemaforoT(x+4f,z-4f);
            }
    }

    private void dibujarPasoCebra(float x,float z){
        usarTextura(TEX_CONCRETO,1f);
        for (int d=-3;d<=3;d++)
            caja(x+d,.045f,z-8f,.45f,.04f,2f,.85f,.87f,.83f);
    }

    private void dibujarSemaforoT(float x,float z){
        sinTextura();
        caja(x,1.7f,z,.18f,2.8f,.18f,.18f,.20f,.22f);
        caja(x,3.1f,z,.55f,1.2f,.45f,.08f,.10f,.12f);
        int fase=(int)(tiempo%12f);
        for (int idx=0;idx<3;idx++){
            boolean enc=idx==0?fase<5:idx==1?fase>=10:fase>=5&&fase<10;
            float br=enc?1f:.15f; entero("uEmision",enc?1:0);
            float r=(idx<2)?br:0f,g=(idx>0)?br:0f;
            caja(x,3.45f-idx*.35f,z-.24f,.28f,.25f,.06f,r,g,.02f);
        }
        entero("uEmision",0);
    }

    // ================================================================
    // 5. LIMPIEZA DE TEXTURAS
    // ================================================================

    /**
     * Libera las texturas de la GPU al cerrar la aplicación.
     * Se llama automáticamente porque sobreescribimos dibujarFrame()
     * y la limpieza ocurre en el finally de CiudadBase.run().
     * Para forzarla explícitamente se puede llamar desde limpiar(),
     * pero CiudadBase.limpiar() es private. Las texturas se liberarán cuando
     * el contexto GL se destruya (al cerrar la ventana).
     */
    // No es necesario sobreescribir limpiar(): cuando GLFW destruye el contexto
    // OpenGL libera automáticamente todos los objetos asociados a él,
    // incluidas las texturas. En una aplicación de producción se llamaría
    // glDeleteTextures(texIds) explícitamente, pero aquí es seguro omitirlo.

    // ================================================================
    // 6. PUNTO DE ENTRADA
    // ================================================================

    public static void main(String[] args) { new Texturas().run(); }
}
