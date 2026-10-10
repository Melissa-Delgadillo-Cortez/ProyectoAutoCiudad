package com.graphics; // Permite probar los métodos protegidos del juego desde el mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba las reglas de conducción sin abrir una ventana OpenGL. */
public class ConduccionTest extends TestCase {

    /** Comprueba que las calles del recorrido conectan el inicio y las entregas. */
    public void testCallesYDestinosTransitables() {
        Conduccion juego = new Conduccion(); // Crea el estado del juego sin ejecutar run().
        for (float z = -30; z <= 30; z += 0.25f) { // Recorre posiciones a lo largo de la calle oeste.
            assertTrue(juego.puedeCircular(-30, z)); // Exige que cada punto del tramo sea transitable.
        }
        for (float x = -30; x <= 30; x += 0.25f) { // Recorre la avenida del borde norte.
            assertTrue(juego.puedeCircular(x, -30)); // Comprueba la conexión entre las dos primeras entregas.
        }
        assertTrue(juego.puedeCircular(-10, -10)); // Verifica que un cruce interior permite circular.
        assertFalse(juego.puedeCircular(0, 0)); // Verifica que la manzana del origen bloquea al vehículo.
    }

    /** Comprueba edificios, parques, aceras y bordes del mapa. */
    public void testObstaculosYLimites() {
        Conduccion juego = new Conduccion(); // Crea un vehículo con su radio de colisión inicial.
        assertFalse(juego.puedeCircular(-20, -20)); // Rechaza el centro de un edificio.
        assertFalse(juego.puedeCircular(20, -20)); // Rechaza el centro de un parque.
        assertFalse(juego.puedeCircular(-25.5f, -20)); // Rechaza una posición cuyo círculo invade la acera.
        assertFalse(juego.puedeCircular(34, 0)); // Rechaza una posición demasiado cercana al borde derecho.
        assertFalse(juego.puedeCircular(0, -36)); // Rechaza una posición exterior al borde norte.
        assertTrue(juego.puedeCircular(-27, -20)); // Acepta una posición con separación suficiente de la acera.
    }

    /** Comprueba que el reinicio restaura la posición y detiene el vehículo. */
    public void testReinicio() {
        Entregas juego = new Entregas(); // Crea la versión final sin inicializar OpenGL.
        juego.autoX = 12; // Simula que el auto se desplazó horizontalmente.
        juego.autoZ = 4; // Simula un desplazamiento en profundidad.
        juego.velocidad = 10; // Simula que el auto está avanzando.
        juego.angulo = 2; // Simula que el vehículo cambió de orientación.
        juego.reiniciar(); // Ejecuta el mismo reinicio que se activa con R.
        assertEquals(-30f, juego.autoX, 0f); // Exige recuperar la coordenada X inicial.
        assertEquals(45f, juego.autoZ, 0f); // Exige recuperar la coordenada Z inicial.
        assertEquals(0f, juego.velocidad, 0f); // Exige que el vehículo quede detenido.
        assertEquals(0f, juego.angulo, 0f); // Exige recuperar la dirección frontal inicial.
    }
}
