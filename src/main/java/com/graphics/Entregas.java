package com.graphics; // Reúne la versión final con las otras tres lecciones.

import static org.lwjgl.glfw.GLFW.*;   // Teclas de control y función de título.
import static org.lwjgl.opengl.GL33.*; // Viewport, scissor test y buffers de dibujo.

/**
 * CLASE 4 — CIUDAD TERMINADA: ENTREGAS, DECORACIÓN Y MINIMAPA.
 *
 * Responsabilidades de esta clase:
 *   • Gestionar tres entregas con destinos sobre el mapa ampliado.
 *   • Cronometrar la partida y mostrar el resultado al terminar.
 *   • Decorar parques (árboles + banco), edificios (ventanas) y
 *     calles (pasos peatonales + semáforos con ciclo animado).
 *   • Mostrar / ocultar el minimapa con M y mantener su encuadre
 *     al redimensionar la ventana.
 *   • Reiniciar partida completa con R.
 *
 * Jerarquía de herencia:
 *   Entregas → Iluminacion → Conduccion → CiudadBase
 *
 * Orden de lectura recomendado:
 *   Estado → Controles → Entregas → Escena → Decoración → Minimapa.
 */
public class Entregas extends Iluminacion {

    // ================================================================
    // 1. ESTADO DEL JUEGO
    // ================================================================

    /**
     * Controla si el minimapa se muestra en la esquina superior derecha.
     * Se alterna con la tecla M; inicia visible.
     */
    private boolean mostrarMapa = true;

    /**
     * Número de entregas completadas (0, 1, 2 o 3).
     * También sirve como índice en DESTINOS[]: cuando es 0 apunta
     * al primer destino, cuando llega a DESTINOS.length el juego termina.
     */
    protected int entregas = 0; // protected: CiudadViva lo lee para dibujar el destino activo.

    /**
     * Segundos acumulados desde el inicio o el último reinicio.
     * Se detiene cuando se completan todas las entregas para mostrar
     * el tiempo final en el título.
     */
    protected float tiempo = 0f; // protected: CiudadViva lo lee para animar semáforos y peatones.

    /**
     * Coordenadas XZ de las tres paradas de entrega.
     * Se eligieron sobre intersecciones de calles del mapa 11×11 para
     * garantizar que el auto puede llegar sin atravesar manzanas.
     *
     *   Destino 0 → esquina noreste  (+40, −40)
     *   Destino 1 → esquina noroeste (−40, −40)
     *   Destino 2 → esquina suroeste (−40, +40) — cerca del inicio
     */
    private static final float[][] DESTINOS = {
        {  40f, -40f }, // Primera entrega: noreste del mapa ampliado.
        { -40f, -40f }, // Segunda entrega: noroeste del mapa ampliado.
        { -40f,  40f }, // Tercera entrega: suroeste, cerca del punto de partida.
    };

    // ================================================================
    // 2. CONTROLES Y REINICIO
    // ================================================================

    /**
     * Añade la tecla M (mostrar/ocultar minimapa) a los controles
     * heredados de Iluminacion (N, F) y Conduccion (C, R, WASD, ESC).
     *
     * @param key  Código GLFW de la tecla pulsada.
     */
    @Override
    protected void tecla(int key) {
        super.tecla(key); // Conserva todas las teclas de las clases anteriores.
        if (key == GLFW_KEY_M) {
            mostrarMapa = !mostrarMapa; // Alterna visibilidad del minimapa.
        }
    }

    /**
     * Reinicia el vehículo (posición, velocidad, ángulo) y también el
     * progreso de la partida (entregas y cronómetro).
     * Llamado por la tecla R, heredada de Conduccion.
     */
    @Override
    protected void reiniciar() {
        super.reiniciar(); // Restaura estado del vehículo (Conduccion).
        entregas = 0;      // Vuelve a seleccionar el primer destino.
        tiempo   = 0f;     // Reinicia el cronómetro.
    }

    // ================================================================
    // 3. ACTUALIZACIÓN: CRONÓMETRO Y DETECCIÓN DE LLEGADA
    // ================================================================

    /**
     * Actualiza el vehículo (Iluminacion → Conduccion) y comprueba si el jugador
     * llegó al destino activo con velocidad suficientemente baja.
     *
     * Condiciones de entrega:
     *   • Distancia al destino < 3 unidades (radio de llegada).
     *   • Velocidad absoluta < 1 u/s (el auto debe frenar).
     *
     * @param deltaTime  Segundos transcurridos desde el último cuadro.
     */
    @Override
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime); // Movimiento, colisiones y título heredados.

        if (entregas >= DESTINOS.length) {
            return; // Partida terminada: congela el cronómetro y no comprueba destinos.
        }

        tiempo += deltaTime; // Acumula tiempo solo mientras la partida está activa.

        // ── Distancia al destino activo ────────────────────────────────────
        float dx = autoX - DESTINOS[entregas][0]; // Diferencia en X.
        float dz = autoZ - DESTINOS[entregas][1]; // Diferencia en Z.
        // Usa distancia al cuadrado para evitar sqrt(); compara con 3²=9.
        boolean cerca   = (dx * dx + dz * dz) < 9f;
        boolean frenado = Math.abs(velocidad) < 1f; // Velocidad baja = el auto frenó.

        if (cerca && frenado) {
            entregas++; // Avanza al siguiente destino o termina la partida.
        }
    }

    /**
     * Añade al título los indicadores de mapa, entregas y tiempo.
     * Conduccion construye el título base y concatena lo que devuelve este método.
     *
     * @return  Cadena con el estado de la partida para el título de la ventana.
     */
    @Override
    protected String estadoExtra() {
        String base = super.estadoExtra(); // Indicadores de iluminación de Iluminacion.
        base += " | M: mapa | ";

        if (entregas == DESTINOS.length) {
            // Partida terminada: muestra tiempo final y opción de reinicio.
            base += "GANASTE en " + (int) tiempo + " s!  R: jugar otra vez";
        } else {
            // Durante el recorrido: progreso, instrucción y cronómetro.
            base += "Entregas " + entregas + "/" + DESTINOS.length;
            base += " | Frena en la marca dorada | " + (int) tiempo + " s";
        }
        return base;
    }

    // ================================================================
    // 4. ESCENA FINAL: DESTINO Y DECORACIÓN
    // ================================================================

    /**
     * Dibuja la ciudad iluminada (Iluminacion), la decoración urbana y la
     * marca del destino activo.
     * Omite la decoración pequeña durante el pase del minimapa para
     * no sobrecargar la vista de tamaño reducido.
     */
    @Override
    protected void escena() {
        super.escena(); // Ciudad + vehículo + farolas (clases 1‑3).

        if (!vistaMapa) {
            // La decoración (árboles, ventanas, semáforos…) solo se dibuja
            // en la vista principal; en el minimapa se muestra solo la silueta.
            decorarCiudad();
        }

        if (entregas < DESTINOS.length) {
            dibujarDestino(); // Dibuja la marca dorada del destino activo.
        }
    }

    /**
     * Dibuja la plataforma y la baliza flotante del destino activo.
     *
     * La baliza oscila verticalmente con una función seno para destacar
     * visualmente la parada de entrega incluso a distancia.
     * En el minimapa solo se dibuja la plataforma (sin baliza 3D).
     */
    private void dibujarDestino() {
        float x = DESTINOS[entregas][0]; // Coordenada X del destino actual.
        float z = DESTINOS[entregas][1]; // Coordenada Z del destino actual.

        // uEmision=1: la plataforma y la baliza se ven a pleno color incluso de noche.
        entero("uEmision", 1);

        // Plataforma dorada ancha y muy baja, justo sobre el asfalto.
        // 5 × 0.08 × 5 unidades; centro en Y=0.06 → cara superior en Y=0.10.
        caja(x, 0.06f, z,  5f, 0.08f, 5f,  1f, 0.72f, 0.12f);

        if (!vistaMapa) {
            // Baliza flotante: cubo pequeño que oscila entre Y=3.2 y Y=3.8
            // con frecuencia 2 rad/s y gira continuamente con el tiempo.
            float altOsc = 3.5f + (float) Math.sin(tiempo * 2f) * 0.3f;
            cajaGirada(x, altOsc, z,  0.8f, 0.8f, 0.8f,  1f, 0.8f, 0.15f,  tiempo);
        }

        entero("uEmision", 0); // Restaura iluminación normal para lo siguiente.
    }

    // ================================================================
    // 5. DECORACIÓN URBANA
    // ================================================================

    /**
     * Recorre el mapa y añade decoración según el tipo de cada celda:
     *   tipo 2 → parque:   árboles y banco.
     *   tipo 1 → edificio: ventanas (luminosas de noche).
     *   tipo ≠ 0 → manzana: paso peatonal y semáforo.
     */
    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int col = 0; col < MAPA[fila].length; col++) {
                float x   = centro(col);      // Centro X de la celda.
                float z   = centro(fila);     // Centro Z de la celda.
                int   tipo = MAPA[fila][col]; // 0=calle, 1=edificio, 2=parque.

                if (tipo == 2) {
                    dibujarParque(x, z); // Árboles en las esquinas + banco central.
                }
                if (tipo == 1) {
                    // Recupera la misma altura calculada en CiudadBase para que las
                    // ventanas coincidan exactamente con el cuerpo del edificio.
                    float altura = 5f + (fila * 3 + col * 7) % 9;
                    dibujarVentanas(x, z, altura);
                }
                if (tipo != 0) {
                    // Pasos peatonales y semáforos se colocan en todas las manzanas.
                    dibujarPasoPeatonal(x, z);
                    dibujarSemaforo(x + 4f, z - 4f); // Esquina noreste de la acera.
                }
            }
        }
    }

    // ── Parque ─────────────────────────────────────────────────────────────

    /**
     * Dibuja cuatro árboles (uno en cada esquina de la parcela) y un banco central.
     *
     * Cada árbol tiene:
     *   • Un tronco delgado marrón (caja 0.35 × 2 × 0.35).
     *   • Una copa esférica aproximada verde (caja 2 × 2.3 × 2).
     *
     * El banco tiene:
     *   • Soporte oscuro bajo.
     *   • Tablero de madera más claro.
     *   • Respaldo vertical detrás del asiento.
     *
     * @param x  Centro X del parque en el mundo.
     * @param z  Centro Z del parque en el mundo.
     */
    private void dibujarParque(float x, float z) {
        float[] desps = { -2.5f, 2.5f }; // Desplazamientos ±2.5 para las cuatro esquinas.

        for (float dx : desps) {
            for (float dz : desps) {
                float ax = x + dx; // Posición X del árbol.
                float az = z + dz; // Posición Z del árbol.

                // Tronco: caja marrón delgada de 2 unidades de alto.
                // Centro Y=1.2 → base en Y=0.2 (sobre la hierba), tope en Y=2.2.
                caja(ax, 1.2f, az,  0.35f, 2f, 0.35f,  0.38f, 0.22f, 0.12f);

                // Copa: caja verde más ancha que el tronco.
                // Centro Y=2.7 → cubre de Y=1.55 a Y=3.85.
                caja(ax, 2.7f, az,  2f, 2.3f, 2f,  0.12f, 0.42f, 0.23f);
            }
        }

        // Soporte del banco: caja oscura baja centrada en el parque.
        caja(x, 0.40f, z,  2f, 0.6f, 0.35f,  0.22f, 0.24f, 0.24f);

        // Tablero (asiento): caja de madera encima del soporte.
        // Centro Y=0.65 → cara superior en Y=0.775.
        caja(x, 0.65f, z,  3f, 0.25f, 0.8f,  0.55f, 0.30f, 0.13f);

        // Respaldo: tabla vertical detrás del asiento (desplazada +0.35 en Z).
        caja(x, 1.0f, z + 0.35f,  3f, 0.7f, 0.15f,  0.55f, 0.30f, 0.13f);
    }

    // ── Ventanas ───────────────────────────────────────────────────────────

    /**
     * Distribuye ventanas por pisos en las cuatro fachadas del edificio.
     *
     * De noche, las ventanas se marcan con uEmision=1 para simular
     * habitaciones iluminadas sin depender de las farolas exteriores.
     * De día se iluminan normalmente por reflexión solar.
     *
     * Las ventanas se colocan a 3.51 u del centro del edificio (radio 3.5)
     * para quedar justo en la superficie de la fachada (semiancho = 3.5 u).
     *
     * @param x       Centro X del edificio.
     * @param z       Centro Z del edificio.
     * @param altura  Altura del edificio (misma fórmula que en CiudadBase).
     */
    private void dibujarVentanas(float x, float z, float altura) {
        if (noche) {
            entero("uEmision", 1); // Ventanas auto-iluminadas de noche.
        }

        // Recorre los pisos de 1.7 en 1.7 unidades hasta la cornisa.
        for (float y = 1.7f; y < altura; y += 2f) {
            // Tres ventanas por fachada, separadas 2 unidades.
            for (float d = -2f; d <= 2f; d += 2f) {
                // Fachada norte (Z negativa): ventana alargada en X, muy delgada en Z.
                caja(x + d, y, z - 3.51f,  0.8f, 0.9f, 0.04f,  0.95f, 0.75f, 0.38f);
                // Fachada sur (Z positiva).
                caja(x + d, y, z + 3.51f,  0.8f, 0.9f, 0.04f,  0.95f, 0.75f, 0.38f);
                // Fachada oeste (X negativa): ventana alargada en Z, muy delgada en X.
                caja(x - 3.51f, y, z + d,  0.04f, 0.9f, 0.8f,  0.95f, 0.75f, 0.38f);
                // Fachada este (X positiva).
                caja(x + 3.51f, y, z + d,  0.04f, 0.9f, 0.8f,  0.95f, 0.75f, 0.38f);
            }
        }

        entero("uEmision", 0); // Restaura iluminación normal.
    }

    // ── Paso peatonal ──────────────────────────────────────────────────────

    /**
     * Dibuja siete franjas blancas sobre la calle al norte de la manzana.
     *
     * Las franjas se colocan en Z = centro − 8, que corresponde al tramo
     * de calle justo entre la manzana y el cruce norte.
     * Cada franja está ligeramente elevada (Y=0.045) para no hundirse en el asfalto.
     *
     * @param x  Centro X de la manzana.
     * @param z  Centro Z de la manzana.
     */
    private void dibujarPasoPeatonal(float x, float z) {
        for (int d = -3; d <= 3; d++) { // Siete franjas, índice −3..+3.
            // Franja 0.45 u de ancho, 0.04 de alto, 2 u de profundidad.
            // Espaciadas 1 u en X para formar el paso de cebra.
            caja(x + d, 0.045f, z - 8f,  0.45f, 0.04f, 2f,  0.85f, 0.87f, 0.83f);
        }
    }

    // ── Semáforo ───────────────────────────────────────────────────────────

    /**
     * Construye un semáforo animado con ciclo de 12 segundos.
     *
     * Estructura geométrica:
     *   • Poste gris delgado (0.18 × 2.8 × 0.18).
     *   • Carcasa oscura que aloja las tres bombillas (0.55 × 1.2 × 0.45).
     *   • Tres bombillas (rojo, amarillo, verde) de arriba a abajo.
     *
     * Ciclo temporal (basado en {@code tiempo} heredado de Entregas):
     *   0‑4 s  → rojo encendido (5 s)
     *   5‑9 s  → verde encendido (5 s)
     *   10‑11 s→ amarillo encendido (2 s)
     *   12 s   → reinicia
     *
     * Las bombillas encendidas reciben uEmision=1 para verse brillantes.
     * Las apagadas se dibujan con brillo tenue (0.15) sin emisión.
     *
     * @param x  Posición X del poste del semáforo.
     * @param z  Posición Z del poste del semáforo.
     */
    private void dibujarSemaforo(float x, float z) {
        // Poste: caja gris delgada de 2.8 u de alto.
        // Centro Y=1.7 → base en Y=0.3 (sobre la acera), tope en Y=3.1.
        caja(x, 1.7f, z,  0.18f, 2.8f, 0.18f,  0.18f, 0.20f, 0.22f);

        // Carcasa: caja oscura que contiene las tres bombillas.
        // Centro Y=3.1 → aloja las bombillas entre Y≈2.5 y Y≈3.7.
        caja(x, 3.1f, z,  0.55f, 1.2f, 0.45f,  0.08f, 0.10f, 0.12f);

        // Fase del ciclo (0‑11 s) basada en el tiempo acumulado de la partida.
        // El operador % produce un número entre 0 y 11 que repite indefinidamente.
        int fase = (int) (tiempo % 12f);

        // Itera sobre las tres bombillas: 0=rojo (arriba), 1=amarillo (centro), 2=verde (abajo).
        for (int idx = 0; idx < 3; idx++) {
            // Determina si esta bombilla está activa en la fase actual.
            boolean encendida;
            if      (idx == 0) encendida = (fase <  5);              // Rojo: primeros 5 s.
            else if (idx == 1) encendida = (fase >= 10);             // Amarillo: últimos 2 s.
            else               encendida = (fase >= 5 && fase < 10); // Verde: 5 s intermedios.

            float brillo = encendida ? 1.0f : 0.15f; // Bombilla encendida vs apagada (tenue).
            entero("uEmision", encendida ? 1 : 0);    // Emisión completa si está encendida.

            // Componentes de color:
            //   idx=0 rojo    → R=1, G=0
            //   idx=1 amarillo→ R=1, G=1  (rojo+verde=amarillo)
            //   idx=2 verde   → R=0, G=1
            float r = (idx < 2) ? brillo : 0f; // Rojo y amarillo tienen componente roja.
            float g = (idx > 0) ? brillo : 0f; // Amarillo y verde tienen componente verde.

            // Altura de cada bombilla: separadas 0.35 u verticalmente.
            // Rojo (idx=0) queda más arriba; verde (idx=2) más abajo.
            float altBombilla = 3.45f - idx * 0.35f;

            // Cara de la bombilla orientada hacia la calle (−Z = norte = frente).
            // Caja 0.28 de ancho, 0.25 de alto y 0.06 de grosor (muy delgada).
            caja(x, altBombilla, z - 0.24f,  0.28f, 0.25f, 0.06f,  r, g, 0.02f);
        }

        entero("uEmision", 0); // Restaura iluminación normal después del semáforo.
    }

    // ================================================================
    // 6. MINIMAPA (segundo pase de dibujo)
    // ================================================================

    /**
     * Produce el cuadro completo: primero la escena principal a pantalla completa
     * y después, si está activado, el minimapa en la esquina superior derecha.
     *
     * Técnica del minimapa:
     *   1. Se dibuja la escena principal normalmente.
     *   2. Se activa GL_SCISSOR_TEST para delimitar el área del minimapa.
     *   3. Se borran color y profundidad SOLO dentro de ese recuadro.
     *   4. Se ajusta glViewport al recuadro y se cambia uMapa a 1 (ortográfico).
     *   5. Se vuelve a llamar a escena() — reutiliza todos los mismos objetos
     *      pero proyectados desde arriba.
     *   6. Se dibuja el indicador del auto encima de todo.
     *   7. El bloque finally restaura viewport, scissor y uMapa originales
     *      para que el próximo cuadro empiece con el estado correcto.
     *
     * Tamaño adaptable:
     *   El lado del minimapa se calcula como el mínimo entre 280 px y
     *   un tercio de la dimensión menor de la ventana, para que siga
     *   siendo legible en ventanas pequeñas y no ocupe demasiado en grandes.
     */
    @Override
    protected void dibujarFrame() {
        super.dibujarFrame(); // Dibuja la vista principal a pantalla completa.

        if (!mostrarMapa) return; // Si el usuario ocultó el mapa, no se dibuja.

        // ── Cálculo del tamaño y posición del minimapa ─────────────────────
        int dimMenor = Math.min(ancho, alto);         // Dimensión más pequeña de la ventana.
        int lado     = Math.min(280, dimMenor / 3);   // Lado del cuadrado; máx 280 px.
        int margen   = Math.min(18,  dimMenor / 20);  // Margen respecto a los bordes.

        // Esquina inferior izquierda del recuadro (OpenGL mide Y desde abajo).
        int mx = ancho - lado - margen; // Borde izquierdo del minimapa.
        int my = alto  - lado - margen; // Borde inferior del minimapa.

        // ── Marco exterior (borde de 3 px) ────────────────────────────────
        glEnable(GL_SCISSOR_TEST);                         // Limita las operaciones al recuadro.
        glScissor(mx - 3, my - 3, lado + 6, lado + 6);    // Incluye el borde de 3 px.
        glClearColor(0.80f, 0.87f, 0.94f, 1f);            // Color claro del borde.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // ── Interior del minimapa ──────────────────────────────────────────
        glScissor(mx, my, lado, lado);                     // Solo el interior.
        glClearColor(0.06f, 0.10f, 0.15f, 1f);            // Fondo oscuro (asfalto visto desde arriba).
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glViewport(mx, my, lado, lado); // Redirige el dibujo al recuadro del minimapa.
        entero("uMapa", 1);             // Activa la proyección ortográfica del shader.
        vistaMapa = true;               // Indica a escena() que omita baliza y decoración pequeña.

        try {
            escena();               // Dibuja la ciudad desde arriba (misma geometría, nueva proyección).
            dibujarIndicadorAuto(); // Superpone el marcador de posición del jugador.
        } finally {
            // ── Restauración del estado de dibujo ────────────────────────────
            // Se ejecuta siempre (incluso ante excepción) para que el siguiente
            // cuadro empiece con viewport y estado correctos.
            vistaMapa = false;
            entero("uMapa", 0);
            glDisable(GL_SCISSOR_TEST);
            glViewport(0, 0, ancho, alto); // Recupera la pantalla completa.
        }

        if (glGetError() != GL_NO_ERROR) {
            throw new IllegalStateException("Error OpenGL durante el minimapa");
        }
    }

    /**
     * Dibuja el indicador de posición del auto en el minimapa.
     *
     * Está formado por:
     *   • Un rectángulo cian orientado según el ángulo del auto (base del indicador).
     *   • Un cuadrado blanco pequeño desplazado hacia el frente, que actúa como
     *     "punta de flecha" para indicar la dirección de marcha.
     *
     * Ambas piezas se sitúan en Y=25 para quedar encima de todos los edificios
     * (el más alto tiene ~13 u) en la proyección ortográfica del minimapa.
     */
    private void dibujarIndicadorAuto() {
        // Rectángulo base cian, orientado con el mismo ángulo que el vehículo.
        // 2.2 × 0.1 × 3.2 unidades para distinguir frente de trasera.
        cajaGirada(autoX, 25f, autoZ,  2.2f, 0.1f, 3.2f,  0.1f, 1f, 1f,  angulo);

        // Dirección frontal del auto para calcular la posición de la punta.
        float fx = -(float) Math.sin(angulo); // Componente X del frente.
        float fz = -(float) Math.cos(angulo); // Componente Z del frente.

        // La punta se coloca 2 u por delante del centro del auto.
        caja(autoX + fx * 2f, 26f, autoZ + fz * 2f,  0.9f, 0.1f, 0.9f,  1f, 1f, 1f);
    }

    // ================================================================
    // 7. PUNTO DE ENTRADA
    // ================================================================

    /** Ejecuta la versión final del proyecto (ciudad + conducción + iluminación + entregas). */
    public static void main(String[] args) {
        new Entregas().run();
    }
}
