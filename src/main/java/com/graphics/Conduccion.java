package com.graphics; // Agrupa las cuatro lecciones dentro del mismo paquete.

import static org.lwjgl.glfw.GLFW.*; // Importa constantes de teclas y la función de título.

/**
 * CLASE 2 — VEHÍCULO Y CONDUCCIÓN.
 *
 * Responsabilidades de esta clase:
 *   • Definir la posición, orientación y velocidad del automóvil.
 *   • Procesar el teclado para acelerar, frenar y girar.
 *   • Calcular la nueva posición propuesta y verificar colisiones
 *     con los bordes del mapa y con cada manzana antes de moverlo.
 *   • Dibujar el modelo del vehículo mediante cajas.
 *   • Ofrecer dos modos de cámara: seguimiento trasero y vista aérea.
 *   • Mostrar velocidad y estado en el título de la ventana.
 *
 * Hereda de CiudadBase; llama a super.*() para reutilizar ciudad, shaders y GPU.
 * Las clases 3 y 4 extienden esta añadiendo iluminación y entregas.
 */
public class Conduccion extends CiudadBase {

    // ================================================================
    // 1. ESTADO DEL VEHÍCULO
    // ================================================================

    /**
     * Posición X del centro del automóvil en el mundo.
     * Valor inicial: −30 → centro de una calle vertical del nuevo mapa.
     * (Con CELDA=10 y LIMITE=55 la primera calle vertical está en X=−50;
     *  la segunda en X=−30; la tercera en X=−10; etc.)
     */
    protected float autoX = -30f;

    /**
     * Posición Z del centro del automóvil en el mundo.
     * Valor inicial: 45 → cerca del borde sur del mapa (Z máxima ≈ +55).
     * Se elige una posición sobre una calle horizontal para que el auto
     * empiece sobre asfalto en el mapa ampliado.
     */
    protected float autoZ = 45f;

    /**
     * Orientación del vehículo en radianes alrededor del eje Y.
     * 0 rad = frente apuntando hacia −Z (norte del mundo).
     * Los valores crecen en sentido antihorario visto desde arriba.
     */
    protected float angulo = 0f;

    /**
     * Velocidad lineal del auto, en unidades de mundo por segundo.
     * Positivo = avance; negativo = reversa.
     * Se limita entre −6 (reversa máx.) y 16 (avance máx.) u/s.
     */
    protected float velocidad = 0f;

    /**
     * Modo de cámara activo.
     * false → cámara de seguimiento trasero (por defecto).
     * true  → cámara aérea orbital heredada de CiudadBase.
     */
    protected boolean camaraAerea = false;

    /**
     * Radio del círculo de colisión del automóvil, en unidades de mundo.
     * Cualquier punto a menos de RADIO_AUTO del centro se considera parte del vehículo.
     * El valor 1.65 cubre cómodamente la semiancho de la carrocería (1.65 u)
     * sin ser tan grande como para bloquear calles de 10 u de ancho.
     */
    protected static final float RADIO_AUTO = 1.65f;

    // ================================================================
    // 2. TECLADO Y REINICIO
    // ================================================================

    /**
     * Extiende el manejo de teclado de CiudadBase con tres nuevas acciones:
     *   C → alterna la cámara entre seguimiento y vista aérea.
     *   R → reinicia posición, orientación y velocidad del vehículo.
     *
     * @param key  Código GLFW de la tecla pulsada.
     */
    @Override
    protected void tecla(int key) {
        super.tecla(key); // Conserva la acción de ESC definida en CiudadBase.

        if (key == GLFW_KEY_C) {
            camaraAerea = !camaraAerea; // Cambia el modo de cámara con cada pulsación.
        }

        if (key == GLFW_KEY_R) {
            reiniciar(); // Restaura el estado inicial del vehículo.
        }
    }

    /**
     * Restablece el automóvil a su posición y estado de inicio.
     * Entregas sobreescribe este método para reiniciar también el contador
     * de entregas y el cronómetro de la partida.
     */
    protected void reiniciar() {
        autoX    = -30f; // Coordenada X de partida: calle vertical oeste-centro.
        autoZ    =  45f; // Coordenada Z de partida: calle horizontal sur.
        angulo   =   0f; // Orienta el frente del auto hacia el norte (−Z).
        velocidad =  0f; // Detiene cualquier movimiento previo.
    }

    // ================================================================
    // 3. ACTUALIZACIÓN POR CUADRO (movimiento y físicas)
    // ================================================================

    /**
     * Calcula el nuevo estado del vehículo para el cuadro actual.
     * Reemplaza la cámara orbital de CiudadBase con la simulación de conducción.
     *
     * Modelo de movimiento:
     *   1. La aceleración se aplica mientras se mantienen W/↑ o S/↓.
     *   2. La resistencia de rodadura (o freno con ESPACIO) reduce la velocidad
     *      de forma exponencial para un comportamiento suave e independiente
     *      de la tasa de cuadros.
     *   3. El giro es proporcional a la velocidad actual: en reversa el volante
     *      invierte el sentido automáticamente.
     *   4. La posición propuesta se valida contra colisiones antes de aceptarla.
     *
     * @param deltaTime  Segundos transcurridos desde el último cuadro.
     */
    @Override
    protected void actualizar(float deltaTime) {
        // ── Aceleración ────────────────────────────────────────────────────
        float acelerador = 0f; // Sin teclas presionadas el motor está en punto muerto.

        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) {
            acelerador += 1f; // W o ↑ aplican aceleración hacia delante.
        }
        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) {
            acelerador -= 1f; // S o ↓ desaceleran; si la velocidad es positiva frenan,
        }                     // si ya es 0 comienzan la marcha atrás.

        // Integra aceleración: 9 u/s² × tiempo → cambio de velocidad en u/s.
        velocidad += acelerador * 9f * deltaTime;

        // ── Resistencia / freno ────────────────────────────────────────────
        float resistencia = 0.7f; // Resistencia normal al rodar sobre asfalto.
        if (pulsada(GLFW_KEY_SPACE)) {
            resistencia = 7f; // Freno de mano: detiene el auto unas 10 veces más rápido.
        }

        // Decaimiento exponencial: v' = v × e^(−r × dt).
        // Garantiza que la velocidad se reduzca de forma suave sin oscilar alrededor de 0.
        float factor = (float) Math.exp(-resistencia * deltaTime);
        velocidad *= factor;

        // Limita el rango de velocidad para evitar que el auto sea demasiado rápido o rápido en reversa.
        velocidad = Math.max(-6f, Math.min(16f, velocidad));

        // ── Dirección (volante) ────────────────────────────────────────────
        float direccion = 0f; // Sin teclas de dirección el volante está recto.

        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) {
            direccion += 1f; // A o ← giran a la izquierda (ángulo creciente = antihorario).
        }
        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) {
            direccion -= 1f; // D o → giran a la derecha (ángulo decreciente = horario).
        }

        // El giro es proporcional a la velocidad: a más velocidad, giro más ágil.
        // Multiplicar por la velocidad invierte el sentido en reversa automáticamente.
        // 0.11 rad/(u/s) es la sensibilidad del volante.
        angulo += direccion * velocidad * 0.11f * deltaTime;

        // ── Movimiento y colisiones ────────────────────────────────────────
        // El frente del auto apunta en la dirección definida por el ángulo.
        // ángulo=0 → frente hacia −Z; ángulo=π/2 → frente hacia −X.
        float frenteX = -(float) Math.sin(angulo); // Componente X del vector frontal.
        float frenteZ = -(float) Math.cos(angulo); // Componente Z del vector frontal.

        // Posición propuesta tras el desplazamiento de este cuadro.
        float sigX = autoX + frenteX * velocidad * deltaTime;
        float sigZ = autoZ + frenteZ * velocidad * deltaTime;

        if (puedeCircular(sigX, sigZ)) {
            autoX = sigX; // Acepta el movimiento si no hay colisión.
            autoZ = sigZ;
        } else {
            velocidad = 0f; // Frena el auto al chocar con una manzana o el borde.
        }

        actualizarTitulo(); // Actualiza la barra de título con velocidad y estado.
    }

    /**
     * Compone y aplica el texto del título de la ventana.
     * Muestra los controles, la velocidad actual y los indicadores adicionales
     * que cada subclase pueda añadir mediante estadoExtra().
     */
    private void actualizarTitulo() {
        // Convierte la velocidad de u/s a km/h para una lectura intuitiva.
        // Factor de conversión: 1 u/s ≈ 3.6 km/h (igual que metros/segundo → km/h).
        int kmh = Math.round(Math.abs(velocidad) * 3.6f);

        String titulo = getClass().getSimpleName();           // Nombre de la clase activa.
        titulo += " | WASD/flechas: conducir | Espacio: freno"; // Controles principales.
        titulo += " | C: camara | R: reiniciar | ";             // Controles secundarios.
        titulo += kmh + " km/h";                               // Velocímetro.
        titulo += estadoExtra();                               // Indicadores de clases 3/4.
        glfwSetWindowTitle(ventana, titulo);                   // Aplica el título.
    }

    /**
     * Reserva espacio para que Iluminacion y Entregas añadan sus propios indicadores
     * (luces, entregas, cronómetro…) al texto del título sin modificar este método.
     *
     * @return  Cadena vacía en Conduccion; sobreescrita en Iluminacion/4.
     */
    protected String estadoExtra() {
        return ""; // Sin indicadores adicionales en esta etapa.
    }

    // ================================================================
    // 4. DETECCIÓN DE COLISIONES
    // ================================================================

    /**
     * Comprueba si el automóvil puede ocupar la posición (x, z) propuesta.
     *
     * Algoritmo:
     *   1. Verifica que el círculo del auto (radio RADIO_AUTO) no salga de los límites del mapa.
     *   2. Para cada celda no transitables del mapa, calcula el punto más cercano
     *      del rectángulo de la manzana al centro del auto (clamping).
     *   3. Si la distancia al cuadrado entre ese punto y el centro es menor que
     *      RADIO_AUTO², hay colisión y la posición se rechaza.
     *
     * Usar la distancia al cuadrado evita la costosa operación sqrt en cada comprobación.
     *
     * @param x  Coordenada X propuesta del centro del auto.
     * @param z  Coordenada Z propuesta del centro del auto.
     * @return   true si la posición es válida; false si hay colisión.
     */
    protected boolean puedeCircular(float x, float z) {
        // ── Borde del mapa ─────────────────────────────────────────────────
        // El auto completo (radio RADIO_AUTO) debe caber dentro del área de juego.
        float limite = LIMITE - RADIO_AUTO;
        if (Math.abs(x) > limite || Math.abs(z) > limite) {
            return false; // El auto saldría fuera del mapa.
        }

        // ── Manzanas ────────────────────────────────────────────────────────
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int col = 0; col < MAPA[fila].length; col++) {
                if (MAPA[fila][col] == 0) continue; // Las celdas de calle no son obstáculos.

                // Centro de la manzana en coordenadas de mundo.
                float cx = centro(col);
                float cz = centro(fila);

                // Media dimensión de la manzana: la acera ocupa toda la celda (10 × 10),
                // su mitad es 5 u en cada dirección.
                float mitad = CELDA / 2f; // = 5

                // Punto más cercano del rectángulo [cx−5, cx+5] × [cz−5, cz+5] al auto.
                float cercaX = Math.max(cx - mitad, Math.min(x, cx + mitad));
                float cercaZ = Math.max(cz - mitad, Math.min(z, cz + mitad));

                // Distancia al cuadrado entre el auto y ese punto más cercano.
                float dx = x - cercaX;
                float dz = z - cercaZ;

                if (dx * dx + dz * dz < RADIO_AUTO * RADIO_AUTO) {
                    return false; // El círculo del auto invade la manzana.
                }
            }
        }

        return true; // Ningún obstáculo encontrado; posición válida.
    }

    // ================================================================
    // 5. CÁMARA
    // ================================================================

    /**
     * Selecciona la cámara activa según el modo elegido por el usuario.
     *
     * Modo seguimiento (por defecto):
     *   La cámara se sitúa 12 unidades detrás del auto y 9 unidades de altura,
     *   apuntando hacia el centro del capó.  Al girar el auto la cámara lo sigue.
     *
     * Modo aéreo (tecla C):
     *   Reutiliza la cámara orbital de CiudadBase que orbitó la ciudad antes de que
     *   hubiera vehículo.  Permite ver toda la ciudad desde arriba.
     */
    @Override
    protected void configurarCamara() {
        if (camaraAerea) {
            super.configurarCamara(); // Cámara orbital de CiudadBase.
            return;
        }

        // ── Cámara de seguimiento trasero ─────────────────────────────────
        // El vector "atrás" es el opuesto al vector "frente" del auto.
        // frente = (−sin(a), 0, −cos(a)), así que atrás = (+sin(a), 0, +cos(a)).
        float camaraX = autoX + (float) Math.sin(angulo) * 12f; // 12 u detrás en X.
        float camaraZ = autoZ + (float) Math.cos(angulo) * 12f; // 12 u detrás en Z.

        vector("uOjo",      camaraX, 9f, camaraZ);     // Cámara a 9 u de altura.
        vector("uObjetivo", autoX,  0.8f, autoZ);      // Apunta al centro de la carrocería.
        decimal("uAspecto", (float) ancho / alto);     // Proporción correcta de la ventana.
    }

    // ================================================================
    // 6. DIBUJO DEL VEHÍCULO
    // ================================================================

    /**
     * Dibuja la ciudad heredada de CiudadBase y sobre ella el automóvil del jugador.
     * Las subclases llaman super.escena() y añaden sus propios elementos encima.
     */
    @Override
    protected void escena() {
        super.escena(); // Dibuja asfalto, calles, aceras, edificios y parques.
        dibujarAuto();  // Añade el modelo del vehículo en su posición actual.
    }

    /**
     * Construye el modelo del automóvil con cajas orientadas en el espacio local
     * del vehículo y después trasladadas al espacio mundo mediante pieza().
     *
     * Espacio local del auto:
     *   X positivo → lado derecho del conductor.
     *   Z negativo → frente del auto.
     *   Z positivo → trasera del auto.
     *   Y positivo → arriba.
     */
    protected void dibujarAuto() {
        // ── Carrocería ────────────────────────────────────────────────────
        // Rectángulo principal rojo, centro en Y=0.65 (sobre las ruedas).
        pieza(0f, 0.65f, 0f,  1.65f, 0.55f, 2.6f,  0.95f, 0.24f, 0.12f);

        // ── Cabina / techo ────────────────────────────────────────────────
        // Más estrecha y desplazada hacia delante respecto a la carrocería.
        pieza(0f, 1.12f, 0.12f,  1.3f, 0.55f, 1.25f,  0.22f, 0.65f, 0.78f);

        // ── Ruedas ────────────────────────────────────────────────────────
        // Cuatro ruedas: combinación de cada lado (izq/der) y cada eje (del/tras).
        float[] lados = { -0.88f,  0.88f }; // Izquierdo y derecho.
        float[] ejes  = { -0.82f,  0.82f }; // Delantero y trasero.
        for (float lx : lados) {
            for (float lz : ejes) {
                // Cubo aplanado en Z para simular el neumático oscuro.
                pieza(lx, 0.38f, lz,  0.24f, 0.58f, 0.6f,  0.055f, 0.065f, 0.08f);
            }
        }

        // ── Faros delanteros ──────────────────────────────────────────────
        // Dos rectángulos claros en la parte frontal (Z negativa = frente).
        float[] sepFaros = { -0.55f, 0.55f }; // Faro izquierdo y faro derecho.
        for (float fx : sepFaros) {
            // Faro delantero: color marfil para indicar luz halógena.
            pieza(fx, 0.68f, -1.32f,  0.38f, 0.2f, 0.07f,  1f, 0.95f, 0.65f);
            // Luz trasera: color rojo.
            pieza(fx, 0.68f,  1.32f,  0.35f, 0.17f, 0.07f,  0.85f, 0.05f, 0.05f);
        }
    }

    /**
     * Transforma una pieza del espacio local del auto al espacio mundo y la dibuja.
     *
     * El proceso es:
     *   1. Rota el desplazamiento local (x, z) por el ángulo actual del auto.
     *   2. Suma la posición mundial del auto para obtener las coordenadas finales.
     *   3. Llama a cajaGirada() con el mismo ángulo para que la caja
     *      quede orientada igual que el vehículo.
     *
     * @param x, y, z    Posición de la pieza en el espacio local del auto.
     * @param sx, sy, sz Tamaño de la pieza.
     * @param r, g, b    Color RGB [0, 1].
     */
    protected void pieza(float x,  float y,  float z,
                         float sx, float sy, float sz,
                         float r,  float g,  float b) {
        float cos = (float) Math.cos(angulo); // Coseno del ángulo actual del auto.
        float sin = (float) Math.sin(angulo); // Seno del ángulo actual del auto.

        // Rotación 2D en el plano XZ: aplica la misma rotación que el shader
        // haría para el auto completo, pero en Java para posicionar el centro.
        float mundoX = autoX + cos * x + sin * z;
        float mundoZ = autoZ - sin * x + cos * z;

        // Dibuja la caja con la orientación del vehículo para que quede alineada.
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, angulo);
    }

    // ================================================================
    // 7. PUNTO DE ENTRADA
    // ================================================================

    /** Ejecuta la segunda lección (ciudad + vehículo) directamente. */
    public static void main(String[] args) {
        new Conduccion().run();
    }
}
