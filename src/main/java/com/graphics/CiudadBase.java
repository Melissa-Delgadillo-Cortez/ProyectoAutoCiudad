package com.graphics; // Coloca la aplicación en el paquete usado por los ejemplos originales.

import java.util.HashMap; // Permite guardar las ubicaciones de las variables del shader.
import java.util.Map;     // Define el tipo del diccionario de ubicaciones.
import org.lwjgl.glfw.Callbacks;       // Permite liberar los callbacks de la ventana al cerrar.
import org.lwjgl.glfw.GLFWErrorCallback; // Muestra los errores de GLFW en la consola.
import org.lwjgl.opengl.GL;            // Carga las funciones OpenGL disponibles en el equipo.
import static org.lwjgl.glfw.GLFW.*;   // Importa las funciones y constantes de ventanas y teclado.
import static org.lwjgl.opengl.GL33.*; // Importa las funciones OpenGL hasta la versión 3.3.

/**
 * CLASE 1 — BASE DE LA CIUDAD (mapa 11 × 11).
 *
 * Responsabilidades de esta clase:
 *   • Crear la ventana OpenGL 3.3 y configurar GLFW.
 *   • Compilar y enlazar los shaders de vértices y fragmentos.
 *   • Subir a la GPU la geometría del cubo unitario (VAO + VBO).
 *   • Definir el mapa 11 × 11 con calles, edificios y parques.
 *   • Recorrer el mapa y emitir llamadas de dibujo por celda.
 *   • Gestionar el ciclo de vida: iniciar → loop → limpiar.
 *
 * Coordenadas del mundo:
 *   X  →  izquierda (−) / derecha (+)
 *   Y  →  abajo (−) / arriba (+)
 *   Z  →  lejos (−) / cerca (+)
 *
 * Las clases 2‑4 extienden esta con "extends CiudadBase" y reutilizan
 * todo lo definido aquí llamando a "super.*".
 */
public class CiudadBase {

    // ================================================================
    // 1. VARIABLES DE LA APLICACIÓN
    // ================================================================

    /** Identificador numérico que GLFW asigna a la ventana al crearla. */
    protected long ventana;

    /** Identificador del programa GPU que une el shader de vértices y el de fragmentos. */
    protected int programa;

    /** Vertex Array Object: recuerda cómo interpretar los datos del VBO. */
    protected int vao;

    /** Vertex Buffer Object: contiene los 36 vértices del cubo unitario en la GPU. */
    protected int vbo;

    /** Ancho de la ventana en píxeles (actualizado cada cuadro con el framebuffer real). */
    protected int ancho = 1200;

    /** Alto de la ventana en píxeles (actualizado cada cuadro con el framebuffer real). */
    protected int alto  = 800;

    /** Ángulo de la cámara orbital de CiudadBase, en radianes. Usado cuando no hay auto. */
    protected float orbita = 0.6f;

    /**
     * Indica que se está dibujando la proyección del minimapa (Entregas).
     * Cuando vale true, escena() omite decoraciones pequeñas para no sobrecargar
     * la vista superior de tamaño reducido.
     */
    protected boolean vistaMapa = false;

    // ── Constantes del mundo ──────────────────────────────────────────

    /**
     * La mitad del lado total del mapa, en unidades de mundo.
     * El mapa tiene 11 celdas × 10 unidades/celda = 110 unidades de lado,
     * así que LIMITE = 55. El suelo va de −55 a +55 en X y Z.
     */
    protected static final float LIMITE = 55f;

    /**
     * Lado de cada celda del mapa en unidades de mundo.
     * Calles, edificios y parques ocupan exactamente un cuadrado de 10 × 10.
     */
    protected static final float CELDA = 10f;

    // ── Caché de ubicaciones de uniforms ─────────────────────────────

    /**
     * Almacena la ubicación GPU de cada uniform la primera vez que se consulta.
     * Evitar llamar glGetUniformLocation() en cada cuadro ahorra tiempo de CPU.
     */
    private final Map<String, Integer> uniforms = new HashMap<>();

    // ── Mapa de la ciudad ─────────────────────────────────────────────

    /**
     * Matriz 11 × 11 que describe la ciudad celda a celda.
     *
     * Codificación:
     *   0  →  calle (transitable)
     *   1  →  manzana con edificio
     *   2  →  manzana con parque
     *
     * Distribución:
     *   • Las filas y columnas pares (0, 2, 4, 6, 8, 10) son calles.
     *   • Las filas y columnas impares (1, 3, 5, 7, 9) son manzanas.
     *   • El resultado es una cuadrícula urbana con avenidas que cruzan
     *     toda la ciudad y manzanas accesibles desde cualquier calle.
     *
     * Cuenta: 25 celdas de manzana → 13 edificios + 12 parques
     *   (cumple el mínimo de 12 edificios y 4 parques del enunciado).
     */
    protected static final int[][] MAPA = {
        // col: 0  1  2  3  4  5  6  7  8  9 10
        //      Esquema urbano del mapa 11×11:
        //        0 = calle transitable
        //        1 = manzana con edificio
        //        2 = manzana con parque / jardín
        //      Filas y columnas pares → calles; impares → manzanas.
        //      Cuenta: 14 edificios (tipo 1) + 11 parques (tipo 2) = 25 manzanas.
              { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // fila  0: calle norte
              { 0, 1, 0, 2, 0, 1, 0, 2, 0, 2, 0 }, // fila  1: primera banda
              { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // fila  2: avenida
              { 0, 2, 0, 1, 0, 1, 0, 1, 0, 2, 0 }, // fila  3: segunda banda
              { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // fila  4: avenida central norte
              { 0, 1, 0, 2, 0, 2, 0, 2, 0, 1, 0 }, // fila  5: banda central (más jardines)
              { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // fila  6: avenida central
              { 0, 2, 0, 1, 0, 1, 0, 1, 0, 2, 0 }, // fila  7: cuarta banda
              { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // fila  8: avenida central sur
              { 0, 1, 0, 2, 0, 1, 0, 2, 0, 1, 0 }, // fila  9: quinta banda
              { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // fila 10: calle sur
    };

    // ================================================================
    // 2. CICLO DE VIDA: iniciar → loop → limpiar
    // ================================================================

    /**
     * Punto de arranque público que orquesta las tres fases.
     * El bloque finally garantiza que los recursos GPU y de GLFW
     * se liberen incluso si iniciar() o loop() lanzan una excepción.
     */
    public void run() {
        // Prepara el canal de errores de GLFW hacia System.err.
        GLFWErrorCallback errores = GLFWErrorCallback.createPrint(System.err);
        errores.set(); // Instala el manejador antes de cualquier llamada a GLFW.
        try {
            iniciar(); // Crea ventana, contexto, shaders y geometría.
            loop();    // Procesa eventos y dibuja hasta que se cierre la ventana.
        } finally {
            limpiar();                       // Libera GPU, callbacks y ventana.
            glfwSetErrorCallback(null);      // Desconecta el manejador de errores.
            errores.free();                  // Libera la memoria del callback de errores.
        }
    }

    /**
     * Configura GLFW y OpenGL; crea la ventana, el contexto y los recursos GPU.
     * Se llama una sola vez al arrancar la aplicación.
     */
    private void iniciar() {
        if (!glfwInit()) { // Intenta inicializar la biblioteca de gestión de ventanas.
            throw new IllegalStateException("No se pudo iniciar GLFW");
        }

        glfwDefaultWindowHints(); // Restablece las pistas antes de configurarlas.

        // Solicita exactamente OpenGL 3.3 Core Profile.
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);          // Versión mayor: 3
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);          // Versión menor: 3
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE); // Sin funciones obsoletas.
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);  // Requerido en macOS.

        // Crea la ventana con su título y sin monitor de pantalla completa.
        ventana = glfwCreateWindow(ancho, alto,
                "Ciudad Conducción | Cargando…", 0, 0);
        if (ventana == 0) { // GLFW devuelve 0 si no pudo crear la ventana.
            throw new IllegalStateException("No se pudo crear la ventana OpenGL");
        }

        glfwMakeContextCurrent(ventana); // Vincula el contexto OpenGL a este hilo.
        glfwSwapInterval(1);             // VSync: espera el refresco de pantalla entre cuadros.
        GL.createCapabilities();         // Carga todas las funciones OpenGL disponibles.
        glEnable(GL_DEPTH_TEST);         // Activa el test de profundidad para ocultamiento correcto.

        // Registra el callback de teclado; se ejecuta cada vez que el usuario presiona una tecla.
        glfwSetKeyCallback(ventana, (vent, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) { // Solo responde al primer evento de pulsación, no a repetición.
                tecla(key);             // Delega la tecla al método que cada clase puede sobreescribir.
            }
        });

        crearPrograma(); // Compila y enlaza los shaders GLSL.
        crearCubo();     // Sube el cubo unitario a la GPU.
    }

    /**
     * Bucle principal: procesa eventos, actualiza estado y dibuja cuadros
     * hasta que el usuario cierre la ventana o se alcance el límite de demo.
     */
    private void loop() {
        double tiempoAnterior = glfwGetTime(); // Marca de tiempo del cuadro anterior.
        int cuadros    = 0;  // Cuadros dibujados acumulados.
        // demo.frames > 0 limita la ejecución a N cuadros (útil para pruebas automatizadas).
        int maxCuadros = Integer.getInteger("demo.frames", 0);

        // Arreglos reutilizables para consultar el tamaño del framebuffer.
        int[] anchoReal = new int[1];
        int[] altoReal  = new int[1];

        while (!glfwWindowShouldClose(ventana)) { // Repite hasta recibir la señal de cierre.
            glfwPollEvents(); // Despacha todos los eventos pendientes (teclado, ratón, resize…).

            double tiempoActual = glfwGetTime();
            // deltaTime en segundos; máximo 50 ms para evitar saltos grandes al minimizar.
            float deltaTime = (float) Math.min(tiempoActual - tiempoAnterior, 0.05);
            tiempoAnterior = tiempoActual;

            actualizar(deltaTime); // Mueve la cámara o el vehículo según la clase activa.

            // Obtiene el tamaño real del framebuffer (puede diferir en pantallas HiDPI/Retina).
            glfwGetFramebufferSize(ventana, anchoReal, altoReal);
            ancho = anchoReal[0];
            alto  = altoReal[0];

            if (ancho > 0 && alto > 0) { // Evita dibujar si la ventana está minimizada (0×0).
                dibujarFrame(); // Produce la imagen del cuadro actual.
            }

            glfwSwapBuffers(ventana); // Presenta el buffer trasero en pantalla.

            cuadros++;
            if (maxCuadros > 0 && cuadros >= maxCuadros) { // Salida automática para demos.
                glfwSetWindowShouldClose(ventana, true);
            }
        }
    }

    /**
     * Libera únicamente los recursos que llegaron a crearse.
     * El orden inverso al de creación evita referencias huérfanas en la GPU.
     */
    private void limpiar() {
        if (programa != 0) glDeleteProgram(programa);       // Elimina el programa de shaders.
        if (vbo      != 0) glDeleteBuffers(vbo);            // Elimina el buffer de vértices.
        if (vao      != 0) glDeleteVertexArrays(vao);       // Elimina el descriptor de atributos.
        if (ventana  != 0) {
            Callbacks.glfwFreeCallbacks(ventana); // Libera el callback de teclado.
            glfwDestroyWindow(ventana);           // Destruye la ventana y su contexto.
        }
        glfwTerminate(); // Finaliza GLFW y sus recursos globales (debe ser la última llamada).
    }

    // ================================================================
    // 3. TECLADO Y CÁMARA ORBITAL
    // ================================================================

    /**
     * Maneja una pulsación de tecla.  CiudadBase solo atiende ESC.
     * Las subclases llaman super.tecla(key) y agregan sus propios casos.
     *
     * @param key  Código GLFW de la tecla pulsada (p. ej. GLFW_KEY_ESCAPE).
     */
    protected void tecla(int key) {
        if (key == GLFW_KEY_ESCAPE) { // ESC solicita cerrar la ventana.
            glfwSetWindowShouldClose(ventana, true);
        }
    }

    /**
     * Consulta si una tecla sigue presionada durante este cuadro.
     * Devuelve true mientras el usuario la mantiene, no solo en el primer evento.
     *
     * @param key  Código GLFW de la tecla a consultar.
     */
    protected boolean pulsada(int key) {
        return glfwGetKey(ventana, key) == GLFW_PRESS;
    }

    /**
     * Actualiza el estado de la aplicación entre cuadros.
     * CiudadBase mueve la cámara orbital con las flechas horizontales.
     * Las subclases reemplazarán este método para controlar el vehículo.
     *
     * @param deltaTime  Segundos transcurridos desde el cuadro anterior.
     */
    protected void actualizar(float deltaTime) {
        if (pulsada(GLFW_KEY_LEFT))  orbita -= deltaTime; // Gira la cámara hacia la izquierda.
        if (pulsada(GLFW_KEY_RIGHT)) orbita += deltaTime; // Gira la cámara hacia la derecha.
    }

    /**
     * Posiciona la cámara orbital elevada y apuntando al centro de la ciudad.
     * El radio 100 asegura que el mapa 11×11 (110 × 110 unidades) cabe completo.
     * Conduccion sobreescribe este método con la cámara de seguimiento del auto.
     */
    protected void configurarCamara() {
        float camaraX = (float) Math.sin(orbita) * 100f; // Posición X sobre la órbita circular.
        float camaraZ = (float) Math.cos(orbita) * 100f; // Posición Z sobre la órbita circular.
        vector("uOjo",      camaraX, 80f, camaraZ); // Cámara a 80 unidades de altura.
        vector("uObjetivo", 0f, 0f, 0f);            // Apunta al origen del mundo.
        decimal("uAspecto", (float) ancho / alto);   // Proporción para evitar deformaciones.
    }

    /**
     * Gancho para que Iluminacion envíe datos de iluminación al shader antes de dibujar.
     * En CiudadBase está vacío porque el shader inicial usa colores planos.
     */
    protected void prepararLuces() {
        // Sin implementación en CiudadBase: el shader solo usa uColor.
    }

    // ================================================================
    // 4. DIBUJO DE LA CIUDAD
    // ================================================================

    /**
     * Convierte el índice de una fila o columna al centro geométrico de esa celda.
     * La ciudad va de −LIMITE a +LIMITE; la celda 0 empieza en −LIMITE y su
     * centro queda en −LIMITE + CELDA × 0.5.
     *
     * @param indice  Fila o columna de la celda (0 … MAPA.length−1).
     * @return        Coordenada del centro en unidades de mundo.
     */
    protected static float centro(int indice) {
        return -LIMITE + CELDA * (indice + 0.5f);
    }

    /**
     * Produce una imagen completa: limpia los buffers, configura OpenGL
     * y llama a escena() para dibujar todos los objetos.
     * Entregas sobreescribe este método para añadir el pase del minimapa.
     */
    protected void dibujarFrame() {
        glViewport(0, 0, ancho, alto);               // Redirige el dibujo a toda la ventana.
        glClearColor(0.12f, 0.20f, 0.30f, 1f);      // Fondo azul oscuro (cielo nocturno inicial).
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra color y profundidad del cuadro anterior.

        glUseProgram(programa);    // Activa los shaders compilados.
        glBindVertexArray(vao);    // Selecciona los atributos del cubo.
        entero("uMapa", 0);        // Proyección en perspectiva (no el minimapa ortográfico).

        configurarCamara(); // Envía posición y orientación de la cámara al shader.
        prepararLuces();    // Envía datos de iluminación (Iluminacion) o nada (CiudadBase/2).
        escena();           // Dibuja todos los objetos de la ciudad y sus extensiones.

        if (glGetError() != GL_NO_ERROR) { // Detecta cualquier error OpenGL acumulado.
            throw new IllegalStateException("Error OpenGL al dibujar la escena principal");
        }
    }

    /**
     * Dibuja la ciudad completa recorriendo el mapa celda a celda.
     * Las subclases llaman super.escena() y agregan sus propios objetos
     * (vehículo, farolas, decoración…) encima de la ciudad base.
     */
    protected void escena() {
        // ── Suelo ──────────────────────────────────────────────────
        // El mapa mide 110 × 110 unidades (11 celdas × 10 u/celda).
        // La caja de suelo se centra en el origen con esas dimensiones.
        // Altura Y=−0.25 y grosor 0.5 hacen que su cara superior quede en Y=0.
        caja(0f, -0.25f, 0f,
             CELDA * MAPA[0].length,   // Ancho total = número de columnas × CELDA.
             0.5f,
             CELDA * MAPA.length,      // Profundidad total = número de filas × CELDA.
             0.16f, 0.19f, 0.23f);     // Color gris asfalto oscuro.

        // ── Manzanas ──────────────────────────────────────────────
        for (int fila = 0; fila < MAPA.length; fila++) {         // Recorre norte → sur.
            for (int col = 0; col < MAPA[fila].length; col++) {  // Recorre oeste → este.
                float x   = centro(col);          // Centro X de esta celda.
                float z   = centro(fila);         // Centro Z de esta celda.
                int   tipo = MAPA[fila][col];     // 0=calle, 1=edificio, 2=parque.

                if (tipo == 0) {
                    // Las celdas de calle reciben marcas discontinuas en los tramos entre cruces.
                    dibujarMarcasCalle(fila, col, x, z);
                } else {
                    // Todas las manzanas (tipo 1 y 2) tienen una acera elevada sobre el asfalto.
                    // Dimensiones 10 × 0.3 × 10, centrada en Y=0.15 → cara superior en Y=0.3.
                    caja(x, 0.15f, z, 10f, 0.3f, 10f, 0.60f, 0.64f, 0.66f);

                    if (tipo == 1) {
                        // ── Edificio ──────────────────────────────────────────
                        // La altura varía de forma determinista entre 5 y 13 unidades
                        // usando la posición en el mapa: garantiza la misma ciudad en
                        // cada ejecución sin necesidad de un generador de números aleatorios.
                        float altura = 5f + (fila * 3 + col * 7) % 9;

                        // Colores que varían levemente con la posición para diferenciar edificios.
                        float rojo  = 0.28f + col  * 0.04f;
                        float azul  = 0.48f + fila * 0.02f;

                        // Cuerpo del edificio: base en Y=0.3 (acera), altura variable.
                        // Centro en Y = altura/2 + 0.3 coloca la base exactamente sobre la acera.
                        caja(x, altura / 2f + 0.3f, z,
                             7f, altura, 7f,
                             rojo, 0.40f, azul);

                        // Cornisa ancha y oscura en el remate del edificio.
                        // Ligeramente más ancha que el cuerpo (7.3 vs 7) para dar relieve.
                        caja(x, altura + 0.45f, z,
                             7.3f, 0.3f, 7.3f,
                             0.20f, 0.26f, 0.32f);

                    } else {
                        // ── Parque ────────────────────────────────────────────
                        // Capa de césped verde justo encima de la acera (Y=0.32, grosor 0.1).
                        caja(x, 0.32f, z, 9f, 0.1f, 9f, 0.20f, 0.45f, 0.28f);
                    }
                }
            }
        }
    }

    /**
     * Dibuja las líneas discontinuas amarillas en los tramos de calle.
     * Solo se pintan en tramos entre cruces (no en las intersecciones mismas)
     * para simular la señalización vial real.
     *
     * @param fila    Fila de la celda en el mapa.
     * @param columna Columna de la celda en el mapa.
     * @param x       Centro X de la celda en unidades de mundo.
     * @param z       Centro Z de la celda en unidades de mundo.
     */
    private void dibujarMarcasCalle(int fila, int columna, float x, float z) {
        // Tramo horizontal (fila par, columna impar): las marcas se alargan en X.
        if (fila % 2 == 0 && columna % 2 == 1) {
            for (int d = -3; d <= 3; d += 3) { // Tres marcas separadas 3 unidades en X.
                caja(x + d, 0.025f, z,
                     1.6f, 0.03f, 0.13f,  // Marca alargada en X, fina y baja.
                     1f, 0.84f, 0.35f);   // Amarillo vial.
            }
        }
        // Tramo vertical (fila impar, columna par): las marcas se alargan en Z.
        if (columna % 2 == 0 && fila % 2 == 1) {
            for (int d = -3; d <= 3; d += 3) { // Tres marcas separadas 3 unidades en Z.
                caja(x, 0.025f, z + d,
                     0.13f, 0.03f, 1.6f, // Marca alargada en Z.
                     1f, 0.84f, 0.35f);  // Amarillo vial.
            }
        }
    }

    // ================================================================
    // 5. PRIMITIVAS DE DIBUJO
    // ================================================================

    /**
     * Dibuja una caja alineada con los ejes (sin rotación).
     *
     * @param x, y, z    Centro del objeto en el mundo.
     * @param sx, sy, sz Tamaño en cada eje (ancho, alto, profundidad).
     * @param r, g, b    Color RGB en el rango [0, 1].
     */
    protected void caja(float x, float y, float z,
                        float sx, float sy, float sz,
                        float r, float g, float b) {
        cajaGirada(x, y, z, sx, sy, sz, r, g, b, 0f); // Delega con ángulo 0.
    }

    /**
     * Dibuja el cubo unitario escalado, girado alrededor de Y, y trasladado.
     * Todos los objetos de la escena pasan por este método para enviar sus
     * parámetros al shader antes de emitir la llamada de dibujo.
     *
     * @param x, y, z    Centro del objeto en el mundo.
     * @param sx, sy, sz Escala en cada eje.
     * @param r, g, b    Color RGB [0, 1].
     * @param angulo     Giro en radianes alrededor del eje Y.
     */
    protected void cajaGirada(float x, float y, float z,
                               float sx, float sy, float sz,
                               float r, float g, float b,
                               float angulo) {
        vector("uPos",    x,  y,  z);    // Posición del centro en el mundo.
        vector("uEscala", sx, sy, sz);   // Tamaño de la caja.
        vector("uColor",  r,  g,  b);    // Color del material.
        decimal("uGiro",  angulo);       // Rotación alrededor de Y.
        glDrawArrays(GL_TRIANGLES, 0, 36); // Dibuja 36 vértices = 12 triángulos = 6 caras.
    }

    // ================================================================
    // 6. ENVÍO DE UNIFORMS AL SHADER
    // ================================================================

    /**
     * Devuelve la ubicación GPU de un uniform, consultando OpenGL solo la
     * primera vez y devolviendo el valor en caché las siguientes.
     * Si el shader no usa la variable, OpenGL devuelve −1 e ignora los envíos.
     *
     * @param nombre  Nombre exacto de la variable en el código GLSL.
     * @return        Ubicación entera que identifica el uniform en el programa activo.
     */
    protected int uniform(String nombre) {
        if (!uniforms.containsKey(nombre)) {
            uniforms.put(nombre, glGetUniformLocation(programa, nombre));
        }
        return uniforms.get(nombre);
    }

    /** Envía tres flotantes a un uniform de tipo vec3. */
    protected void vector(String nombre, float x, float y, float z) {
        glUniform3f(uniform(nombre), x, y, z);
    }

    /** Envía un flotante a un uniform de tipo float. */
    protected void decimal(String nombre, float valor) {
        glUniform1f(uniform(nombre), valor);
    }

    /** Envía un entero a un uniform de tipo int (también usado como booleano 0/1). */
    protected void entero(String nombre, int valor) {
        glUniform1i(uniform(nombre), valor);
    }

    // ================================================================
    // 7. SHADERS GLSL
    // ================================================================

    /**
     * Código GLSL del shader de vértices.
     *
     * Responsabilidades:
     *   • Escalar el vértice local del cubo unitario con uEscala.
     *   • Girar el resultado alrededor del eje Y con uGiro.
     *   • Trasladar al origen del mundo con uPos.
     *   • Transformar la normal para iluminación correcta (Iluminacion).
     *   • Proyectar con perspectiva (escena principal) u ortografía (minimapa).
     *
     * El shader calcula su propia proyección manualmente en lugar de depender
     * de matrices externas, siguiendo el estilo del proyecto original.
     */
    private String vertexShader() {
        return """
            #version 330 core

            // ── Atributos del VBO (por vértice) ────────────────────────────────
            layout (location = 0) in vec3 aPos;    // Posición local del vértice en el cubo unitario.
            layout (location = 1) in vec3 aNormal; // Normal de la cara en espacio local.
            layout (location = 2) in vec2 aUV;     // Coordenada de textura [0,1]×[0,1] por cara.

            // ── Uniforms enviados desde Java ────────────────────────────────────
            uniform vec3  uPos;      // Centro del objeto en el mundo.
            uniform vec3  uEscala;   // Dimensiones de la caja (ancho, alto, profundidad).
            uniform vec3  uOjo;      // Posición de la cámara en el mundo.
            uniform vec3  uObjetivo; // Punto hacia el que mira la cámara.
            uniform float uGiro;     // Rotación del objeto alrededor de Y, en radianes.
            uniform float uAspecto;  // Proporción ancho/alto de la ventana.
            uniform int   uMapa;     // 1 = proyección ortográfica del minimapa; 0 = perspectiva.
            uniform float uTileUV;   // Factor de repetición de la textura (tiling). 1=una vez, 4=cuatro veces.

            // ── Salidas hacia el shader de fragmentos ────────────────────────────
            out vec3 vMundo;  // Posición del vértice en espacio mundo (para iluminación).
            out vec3 vNormal; // Normal transformada al espacio mundo (para difusa).
            out vec2 vUV;     // Coordenada de textura interpolada para el fragment shader.

            void main() {
                // ── 1. Rotación del objeto alrededor de Y ──────────────────────
                float coseno = cos(uGiro);
                float seno   = sin(uGiro);
                // Columnas de la matriz de rotación Y (GLSL usa orden columna‑mayor).
                mat3 giro = mat3(
                     coseno, 0.0, -seno,
                     0.0,    1.0,  0.0,
                     seno,   0.0,  coseno
                );

                // ── 2. Transformar posición al espacio mundo ───────────────────
                // Escala en espacio local → gira → traslada al centro del objeto.
                vMundo = giro * (aPos * uEscala) + uPos;

                // ── 3. Transformar normal al espacio mundo ─────────────────────
                // Para escala no uniforme se usa la inversa transpuesta de (giro * escala).
                // Como giro es ortogonal, su inversa es su transpuesta; dividir por uEscala
                // equivale a multiplicar por la inversa de la escala diagonal.
                vNormal = normalize(giro * (aNormal / uEscala));

                // ── 4. Coordenada de textura con tiling ───────────────────────
                // Multiplicar aUV por uTileUV hace que la textura se repita N veces
                // en lugar de estirarse sobre toda la cara.
                vUV = aUV * uTileUV;

                // ── 4. Proyección ──────────────────────────────────────────────
                if (uMapa == 1) {
                    // Proyección ortográfica para el minimapa.
                    // El factor 37 encuadra exactamente los 55 u de radio del nuevo mapa
                    // (LIMITE = 55, usamos 58 para dejar un pequeño margen).
                    float pantallaX =  vMundo.x / 58.0;
                    float pantallaY = -vMundo.z / 58.0; // −Z → norte arriba.
                    float prof      = -vMundo.y / 100.0; // Objetos más altos encima.
                    gl_Position = vec4(pantallaX, pantallaY, prof, 1.0);
                } else {
                    // Proyección perspectiva manual (misma técnica que el proyecto original).
                    vec3 frente   = normalize(uObjetivo - uOjo);
                    vec3 derecha  = normalize(cross(frente, vec3(0.0, 1.0, 0.0)));
                    vec3 arriba   = cross(derecha, frente);
                    vec3 delta    = vMundo - uOjo;

                    float vistaX =  dot(delta, derecha);
                    float vistaY =  dot(delta, arriba);
                    float vistaZ = -dot(delta, frente); // Z positiva detrás de la cámara.

                    float fov    = 1.0 / tan(radians(55.0) * 0.5); // Factor de campo visual 55°.
                    float cerca  = 0.1;
                    float lejos  = 350.0; // Aumentado para ver el mapa más grande.

                    float clipX = vistaX * fov / uAspecto;
                    float clipY = vistaY * fov;
                    // Transformación lineal de [cerca, lejos] al rango NDC [−1, +1].
                    float clipZ = (lejos + cerca) / (cerca - lejos) * vistaZ
                                + 2.0 * lejos * cerca / (cerca - lejos);

                    // W = −vistaZ hace que la división de perspectiva (X/W, Y/W) encoja
                    // los objetos lejanos, produciendo el efecto de profundidad real.
                    gl_Position = vec4(clipX, clipY, clipZ, -vistaZ);
                }
            }
            """;
    }

    /**
     * Código GLSL del shader de fragmentos de CiudadBase.
     * Aplica el color uniforme directamente sin ningún cálculo de iluminación.
     * Iluminacion sobreescribe fragmentShader() para añadir luz difusa y de farolas.
     */
    protected String fragmentShader() {
        return """
            #version 330 core

            in vec3 vMundo;          // Posición del fragmento (no usada en CiudadBase, sí en Iluminacion).
            in vec3 vNormal;         // Normal transformada (no usada en CiudadBase, sí en Iluminacion).
            in vec2 vUV;             // Coordenada de textura interpolada.

            uniform vec3      uColor;    // Color base del material enviado por caja().
            uniform sampler2D uTex;      // Textura activa ligada a la unidad 0.
            uniform int       uUsaTex;   // 1 = mezclar textura; 0 = solo color plano.

            out vec4 color; // Color RGBA final que se escribe en el framebuffer.

            void main() {
                vec3 base = uColor; // Parte con el color del material.
                if (uUsaTex == 1) {
                    // Muestrea la textura y la MULTIPLICA por el color del material.
                    // Multiplicar (no reemplazar) preserva las variaciones de color
                    // por edificio y permite que la textura oscurezca/aclare la fachada.
                    vec3 texel = texture(uTex, vUV).rgb;
                    base = uColor * texel;
                }
                color = vec4(base, 1.0);
            }
            """;
    }

    /**
     * Compila un shader individual (vértices o fragmentos).
     * Si el compilador GLSL reporta un error, recupera el mensaje diagnóstico
     * antes de liberar el objeto y lo incluye en la excepción.
     *
     * @param tipo   GL_VERTEX_SHADER o GL_FRAGMENT_SHADER.
     * @param fuente Código GLSL como cadena Java.
     * @return       Identificador del shader compilado.
     */
    private int compilar(int tipo, String fuente) {
        int shader = glCreateShader(tipo); // Reserva el objeto shader en la GPU.
        glShaderSource(shader, fuente);    // Carga el código fuente GLSL.
        glCompileShader(shader);           // Solicita la compilación.
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(shader); // Mensaje del compilador GLSL.
            glDeleteShader(shader);                    // Libera el objeto fallido.
            throw new IllegalStateException("Error compilando shader:\n" + error);
        }
        return shader;
    }

    /**
     * Enlaza los dos shaders en un programa ejecutable.
     * Los objetos shader intermedios se liberan inmediatamente después del enlace,
     * ya que el programa los contiene y no los necesita más como objetos separados.
     */
    private void crearPrograma() {
        int vs = compilar(GL_VERTEX_SHADER,   vertexShader());   // Shader de vértices.
        int fs = 0;
        try {
            fs       = compilar(GL_FRAGMENT_SHADER, fragmentShader()); // Shader de fragmentos.
            programa = glCreateProgram(); // Crea el contenedor del programa.
            glAttachShader(programa, vs); // Adjunta el shader de vértices.
            glAttachShader(programa, fs); // Adjunta el shader de fragmentos.
            glLinkProgram(programa);      // Enlaza entradas/salidas entre shaders.
            if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) {
                throw new IllegalStateException(
                        "Error enlazando programa:\n" + glGetProgramInfoLog(programa));
            }
        } finally {
            glDeleteShader(vs);           // Libera el objeto shader de vértices.
            if (fs != 0) glDeleteShader(fs); // Libera el shader de fragmentos si se creó.
        }
    }

    // ================================================================
    // 8. GEOMETRÍA DEL CUBO (VAO + VBO)
    // ================================================================

    /**
     * Sube a la GPU un cubo unitario centrado en el origen con normales por cara.
     *
     * Cada vértice tiene 6 flotantes: X Y Z NX NY NZ.
     * Se definen explícitamente 36 vértices (6 caras × 2 triángulos × 3 vértices)
     * para mantener normales constantes por cara sin usar un EBO.
     *
     * El VAO recuerda:
     *   atributo 0 → posición (3 floats, offset 0)
     *   atributo 1 → normal   (3 floats, offset 12 bytes)
     */
    private void crearCubo() {
        // Cada vértice: X Y Z  NX NY NZ  U V  → 8 floats = 32 bytes.
        // Las UV van de (0,0) esquina inferior-izquierda a (1,1) superior-derecha.
        // El tiling se aplica en el vertex shader multiplicando por uTileUV.
        float[] v = {
            // Cara +Z (frontal) — normal (0,0,+1)
            -0.5f,-0.5f, 0.5f,  0,0,1,  0,0,
             0.5f,-0.5f, 0.5f,  0,0,1,  1,0,
             0.5f, 0.5f, 0.5f,  0,0,1,  1,1,
             0.5f, 0.5f, 0.5f,  0,0,1,  1,1,
            -0.5f, 0.5f, 0.5f,  0,0,1,  0,1,
            -0.5f,-0.5f, 0.5f,  0,0,1,  0,0,
            // Cara -Z (trasera) — normal (0,0,-1)
            -0.5f,-0.5f,-0.5f,  0,0,-1, 1,0,
            -0.5f, 0.5f,-0.5f,  0,0,-1, 1,1,
             0.5f, 0.5f,-0.5f,  0,0,-1, 0,1,
             0.5f, 0.5f,-0.5f,  0,0,-1, 0,1,
             0.5f,-0.5f,-0.5f,  0,0,-1, 0,0,
            -0.5f,-0.5f,-0.5f,  0,0,-1, 1,0,
            // Cara -X (izquierda) — normal (-1,0,0)
            -0.5f, 0.5f, 0.5f, -1,0,0,  1,1,
            -0.5f, 0.5f,-0.5f, -1,0,0,  0,1,
            -0.5f,-0.5f,-0.5f, -1,0,0,  0,0,
            -0.5f,-0.5f,-0.5f, -1,0,0,  0,0,
            -0.5f,-0.5f, 0.5f, -1,0,0,  1,0,
            -0.5f, 0.5f, 0.5f, -1,0,0,  1,1,
            // Cara +X (derecha) — normal (+1,0,0)
             0.5f, 0.5f, 0.5f,  1,0,0,  0,1,
             0.5f,-0.5f, 0.5f,  1,0,0,  0,0,
             0.5f,-0.5f,-0.5f,  1,0,0,  1,0,
             0.5f,-0.5f,-0.5f,  1,0,0,  1,0,
             0.5f, 0.5f,-0.5f,  1,0,0,  1,1,
             0.5f, 0.5f, 0.5f,  1,0,0,  0,1,
            // Cara +Y (techo) — normal (0,+1,0)
            -0.5f, 0.5f,-0.5f,  0,1,0,  0,1,
            -0.5f, 0.5f, 0.5f,  0,1,0,  0,0,
             0.5f, 0.5f, 0.5f,  0,1,0,  1,0,
             0.5f, 0.5f, 0.5f,  0,1,0,  1,0,
             0.5f, 0.5f,-0.5f,  0,1,0,  1,1,
            -0.5f, 0.5f,-0.5f,  0,1,0,  0,1,
            // Cara -Y (suelo) — normal (0,-1,0)
            -0.5f,-0.5f,-0.5f,  0,-1,0, 0,0,
             0.5f,-0.5f,-0.5f,  0,-1,0, 1,0,
             0.5f,-0.5f, 0.5f,  0,-1,0, 1,1,
             0.5f,-0.5f, 0.5f,  0,-1,0, 1,1,
            -0.5f,-0.5f, 0.5f,  0,-1,0, 0,1,
            -0.5f,-0.5f,-0.5f,  0,-1,0, 0,0,
        };

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, v, GL_STATIC_DRAW);

        // Cada vértice tiene 8 floats × 4 bytes = 32 bytes.
        int stride = 8 * Float.BYTES;

        // Atributo 0 → posición XYZ: 3 floats, offset 0.
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);

        // Atributo 1 → normal XYZ: 3 floats, offset 12 bytes (3 floats × 4).
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Atributo 2 → UV: 2 floats, offset 24 bytes (6 floats × 4).
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    // ================================================================
    // 9. PUNTO DE ENTRADA (solo CiudadBase directa)
    // ================================================================

    /** Ejecuta únicamente la ciudad base sin vehículo ni iluminación. */
    public static void main(String[] args) {
        new CiudadBase().run();
    }
}
