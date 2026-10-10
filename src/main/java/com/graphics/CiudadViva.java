package com.graphics;

/**
 * CLASE 5 — CIUDAD VIVA.
 *
 * Añade sobre Entregas:
 *   • Edificios únicos (7 paletas × 5 remates, ventanas, puertas).
 *   • Jardines ricos (fuente / escultura / glorieta + árboles, setos, flores).
 *   • 12 peatones animados que caminan por las aceras y no traspasan al jugador.
 *   • 4 vehículos autónomos de colores que siguen waypoints y colisionan con el jugador.
 *
 * Correcciones respecto a versiones anteriores:
 *   - Posiciones iniciales de peatones SOBRE CALLES en coordenadas seguras.
 *   - Posiciones iniciales de agentes bien separadas entre sí, sobre calles.
 *   - Colisión jugador–agente: empuja al jugador fuera del radio de colisión
 *     (no solo frena) para que no haya solapamiento visual.
 *   - Colisión jugador–peatón: igual, empuja al peatón.
 *   - Agentes con una ruta común de perímetro (todos) para que el movimiento
 *     sea siempre válido; cada uno empieza en un waypoint distinto del circuito.
 */
public class CiudadViva extends Entregas {

    // ================================================================
    // 1. ESTILOS DE EDIFICIOS
    // ================================================================

    /** Colores de fachada (R, G, B). */
    protected static final float[][] PALETA = {
        { 0.76f, 0.60f, 0.42f }, // Ladrillo terracota.
        { 0.55f, 0.65f, 0.75f }, // Vidrio gris-azulado.
        { 0.85f, 0.82f, 0.72f }, // Mármol beige.
        { 0.38f, 0.52f, 0.44f }, // Verde musgo.
        { 0.70f, 0.55f, 0.55f }, // Salmón mediterráneo.
        { 0.30f, 0.35f, 0.45f }, // Antracita corporativo.
        { 0.88f, 0.75f, 0.35f }, // Ocre colonial.
    };

    // ================================================================
    // 2. PEATONES
    // ================================================================

    protected static final int NP = 12;   // Número de peatones.

    private final float[] pX   = new float[NP]; // Posición X.
    private final float[] pZ   = new float[NP]; // Posición Z.
    private final float[] pAng = new float[NP]; // Orientación (rad).
    private final float[] pVel = new float[NP]; // Velocidad (u/s).
    private final float[] pTmr = new float[NP]; // Segundos hasta próximo giro.

    /**
     * Posiciones iniciales en centros de CALLE (valor ±50, ±30, ±10).
     * Con desplazamiento de ±2 para que los peatones queden en la acera
     * y no exactamente en el eje de la calzada.
     * Todas estas coordenadas son transitables (MAPA[fila][col]==0).
     */
    private static final float[][] PI = {
        { -48f, -50f }, {  48f, -50f },   // Calle norte, dos lados.
        { -50f, -28f }, { -50f,  28f },   // Calle oeste, dos posiciones.
        {  50f, -28f }, {  50f,  28f },   // Calle este, dos posiciones.
        { -28f,  50f }, {  28f,  50f },   // Calle sur, dos lados.
        { -10f, -10f }, {  10f, -10f },   // Interior, dos posiciones.
        { -10f,  10f }, {  10f,  10f },   // Interior, dos más.
    };

    // ================================================================
    // 3. VEHÍCULOS AUTÓNOMOS
    // ================================================================

    protected static final int NA = 4;    // Número de agentes.

    private final float[] aX   = new float[NA]; // Posición X.
    private final float[] aZ   = new float[NA]; // Posición Z.
    private final float[] aAng = new float[NA]; // Orientación (rad).
    private final float[] aVel = new float[NA]; // Velocidad (u/s).
    private final int[]   aWP  = new int[NA];   // Índice en RUTA_PERIMETRO.

    /** Colores de cada agente (distintos al rojo del jugador). */
    private static final float[][] COL_AG = {
        { 0.15f, 0.55f, 0.92f }, // Azul.
        { 0.20f, 0.75f, 0.28f }, // Verde.
        { 0.92f, 0.65f, 0.08f }, // Amarillo.
        { 0.78f, 0.18f, 0.85f }, // Morado.
    };

    /**
     * Circuito de waypoints sobre las CALLES del mapa (no sobre bordes exactos).
     * Todos los puntos están en coordenadas ±50, ±30, ±10, que son centros
     * de las celdas de calle (índices pares del mapa 11×11).
     * El circuito es un perímetro + cruces internos, formando un único camino
     * cerrado que atraviesa toda la ciudad.
     */
    private static final float[][] RUTA = {
        { -50f, -50f }, { -30f, -50f }, { -10f, -50f }, {  10f, -50f }, {  30f, -50f }, {  50f, -50f },
        {  50f, -30f }, {  50f, -10f }, {  50f,  10f }, {  50f,  30f }, {  50f,  50f },
        {  30f,  50f }, {  10f,  50f }, { -10f,  50f }, { -30f,  50f }, { -50f,  50f },
        { -50f,  30f }, { -50f,  10f }, { -50f, -10f }, { -50f, -30f },
        // Vuelve al inicio: el índice siguiente de 19 será 0 (circular).
    };

    // Radio de colisión de un agente (mismo que el jugador).
    private static final float RADIO_AG = RADIO_AUTO;

    // ================================================================
    // 4. CONSTRUCTOR
    // ================================================================

    /**
     * Inicializa peatones y agentes con posiciones bien separadas.
     * Se ejecuta antes de cualquier llamada a OpenGL.
     */
    public CiudadViva() {
        // ── Peatones ────────────────────────────────────────────────────────
        for (int i = 0; i < NP; i++) {
            pX[i]   = PI[i][0];                        // Posición X inicial segura.
            pZ[i]   = PI[i][1];                        // Posición Z inicial segura.
            pAng[i] = (float)(i * Math.PI / 6.0);     // Ángulos distribuidos 0..2π.
            pVel[i] = 1.5f + (i % 3) * 0.35f;        // 1.5, 1.85 o 2.2 u/s.
            pTmr[i] = i * 0.6f;                        // Temporizadores escalonados.
        }

        // ── Agentes ──────────────────────────────────────────────────────────
        // Los 4 agentes siguen la misma ruta RUTA[] pero empiezan en waypoints
        // distintos (0, 5, 10, 15) para que estén repartidos por la ciudad.
        int[] wpInicio = { 0, 5, 10, 15 };
        for (int i = 0; i < NA; i++) {
            int wp  = wpInicio[i];
            aX[i]   = RUTA[wp][0];     // Posición X del waypoint inicial.
            aZ[i]   = RUTA[wp][1];     // Posición Z del waypoint inicial.
            aAng[i] = 0f;              // Orientación inicial: norte.
            aVel[i] = 7f + i * 1.5f;  // 7, 8.5, 10, 11.5 u/s.
            aWP[i]  = (wp + 1) % RUTA.length; // Primer waypoint destino.
        }
    }

    // ================================================================
    // 5. ACTUALIZACIÓN
    // ================================================================

    @Override
    protected void actualizar(float dt) {
        super.actualizar(dt);  // Jugador, entregas, cronómetro.
        moverPeatones(dt);
        moverAgentes(dt);
        resolverColisiones(); // Empuja al jugador lejos de agentes y peatones.
    }

    // ── Peatones ────────────────────────────────────────────────────────────

    private void moverPeatones(float dt) {
        for (int i = 0; i < NP; i++) {
            pTmr[i] -= dt;
            if (pTmr[i] <= 0f) {
                // Giro de 90° pseudo-aleatorio determinista.
                pAng[i] += (float)(Math.PI / 2.0) * (1 + (i + (int)(tiempo * 2)) % 4);
                pTmr[i] = 1.5f + (i % 4) * 0.4f;
            }
            float nx = pX[i] - (float)Math.sin(pAng[i]) * pVel[i] * dt;
            float nz = pZ[i] - (float)Math.cos(pAng[i]) * pVel[i] * dt;
            if (puedeCircularR(nx, nz, 0.25f)) {
                pX[i] = nx; pZ[i] = nz;
            } else {
                pAng[i] += (float)Math.PI;  // Media vuelta al chocar con manzana.
                pTmr[i] = 0.5f;
            }
        }
    }

    // ── Agentes ─────────────────────────────────────────────────────────────

    private void moverAgentes(float dt) {
        for (int i = 0; i < NA; i++) {
            float wpX = RUTA[aWP[i]][0];
            float wpZ = RUTA[aWP[i]][1];
            float dX  = wpX - aX[i];
            float dZ  = wpZ - aZ[i];

            // Al llegar a menos de 2 u del waypoint, avanza al siguiente.
            if (dX * dX + dZ * dZ < 4f) {
                aWP[i] = (aWP[i] + 1) % RUTA.length;
                continue; // Aplica el nuevo waypoint en el próximo cuadro.
            }

            // Ángulo deseado hacia el waypoint.
            float angDes = (float)Math.atan2(-dX, -dZ);
            float diff   = angDes - aAng[i];
            while (diff >  Math.PI) diff -= 2f * (float)Math.PI;
            while (diff < -Math.PI) diff += 2f * (float)Math.PI;
            // Giro suave: máximo 3 rad/s.
            aAng[i] += Math.signum(diff) * Math.min(Math.abs(diff), 3f * dt);

            float nx = aX[i] - (float)Math.sin(aAng[i]) * aVel[i] * dt;
            float nz = aZ[i] - (float)Math.cos(aAng[i]) * aVel[i] * dt;
            if (puedeCircular(nx, nz)) {
                aX[i] = nx; aZ[i] = nz;
            }
        }
    }

    // ── Colisiones jugador ↔ agentes y peatones ─────────────────────────────

    /**
     * Empuja al jugador lejos de cualquier agente o peatón con el que colisione.
     * "Empujar" significa mover el jugador en la dirección opuesta al centro
     * del obstáculo hasta que la distancia supere el umbral.
     * Esto evita el solapamiento visual y simula un golpe real.
     */
    private void resolverColisiones() {
        float umbralAg = RADIO_AUTO + RADIO_AG;   // ~3.3 u
        float umbralP  = RADIO_AUTO + 0.25f;      // ~1.9 u

        // ── Colisión con agentes ────────────────────────────────────────────
        for (int i = 0; i < NA; i++) {
            float dx  = autoX - aX[i];
            float dz  = autoZ - aZ[i];
            float d2  = dx * dx + dz * dz;
            float lim = umbralAg * umbralAg;
            if (d2 < lim && d2 > 0.001f) {
                // Distancia actual y cuánto hay que separar.
                float d    = (float)Math.sqrt(d2);
                float sep  = umbralAg - d + 0.05f; // 5 cm extra de margen.
                // Normaliza el vector jugador→agente y empuja al jugador.
                autoX += (dx / d) * sep;
                autoZ += (dz / d) * sep;
                velocidad = 0f; // Frena al choque.
            }
        }

        // ── Colisión con peatones ───────────────────────────────────────────
        for (int i = 0; i < NP; i++) {
            float dx = autoX - pX[i];
            float dz = autoZ - pZ[i];
            float d2 = dx * dx + dz * dz;
            float lim = umbralP * umbralP;
            if (d2 < lim && d2 > 0.001f) {
                float d   = (float)Math.sqrt(d2);
                float sep = umbralP - d + 0.05f;
                // Empuja al peatón lejos del jugador (más realista que empujar al auto).
                pX[i] -= (dx / d) * sep;
                pZ[i] -= (dz / d) * sep;
            }
        }
    }

    /** puedeCircular() con radio configurable (para peatones). */
    private boolean puedeCircularR(float x, float z, float r) {
        if (Math.abs(x) > LIMITE - r || Math.abs(z) > LIMITE - r) return false;
        for (int fi = 0; fi < MAPA.length; fi++) {
            for (int co = 0; co < MAPA[fi].length; co++) {
                if (MAPA[fi][co] == 0) continue;
                float cx = centro(co), cz = centro(fi), m = CELDA / 2f;
                float px = Math.max(cx - m, Math.min(x, cx + m));
                float pz = Math.max(cz - m, Math.min(z, cz + m));
                float dx = x - px, dz = z - pz;
                if (dx * dx + dz * dz < r * r) return false;
            }
        }
        return true;
    }

    // ================================================================
    // 6. ESCENA
    // ================================================================

    /**
     * Llama a la cadena completa de super.escena() (ciudad base, farolas,
     * auto del jugador, destino, decoración de Entregas) y ENCIMA dibuja los
     * edificios detallados, jardines ricos, peatones y agentes.
     */
    @Override
    protected void escena() {
        super.escena(); // Todo lo heredado de CiudadBase..Entregas.

        // Reemplaza cada manzana con la versión detallada (encima de la genérica).
        for (int fi = 0; fi < MAPA.length; fi++) {
            for (int co = 0; co < MAPA[fi].length; co++) {
                if (MAPA[fi][co] == 0) continue;
                float x = centro(co), z = centro(fi);
                if (MAPA[fi][co] == 1) dibujarEdificio(fi, co, x, z);
                else                   dibujarJardin(fi, co, x, z);
            }
        }

        // Peatones (solo vista principal).
        if (!vistaMapa) {
            for (int i = 0; i < NP; i++) dibujarPeaton(i);
        }

        // Agentes (visibles también en minimapa).
        for (int i = 0; i < NA; i++) dibujarAgente(i);
    }

    // ================================================================
    // 7. EDIFICIOS ÚNICOS
    // ================================================================

    private void dibujarEdificio(int fi, int co, float x, float z) {
        float[] p   = PALETA[(fi * 3 + co * 5) % PALETA.length];
        float r = p[0], g = p[1], b = p[2];
        float alt   = 5f + (fi * 3 + co * 7) % 9; // 5–13 u.
        int   rem   = (fi + co) % 5;
        float mr = Math.min(1f, r + 0.22f), mg = Math.min(1f, g + 0.22f), mb = Math.min(1f, b + 0.22f);
        float zr = r * 0.62f, zg = g * 0.62f, zb = b * 0.62f;

        caja(x, 0.75f, z, 7.1f, 0.9f, 7.1f, zr, zg, zb);              // Zócalo.
        caja(x, alt/2f + 0.8f, z, 7f, alt - 0.5f, 7f, r, g, b);       // Fachada.
        caja(x, alt + 0.45f, z, 7.5f, 0.28f, 7.5f, zr, zg, zb);       // Cornisa.

        float base = alt + 0.6f;
        switch (rem) {
            case 0 -> { caja(x, base+0.3f, z, 7.4f,0.5f,0.25f, zr,zg,zb);
                        caja(x, base+0.3f, z, 0.25f,0.5f,7.4f, zr,zg,zb); }
            case 1 -> { caja(x,base+1.2f,z,3f,2.4f,3f,r*.85f,g*.85f,b*.85f);
                        caja(x,base+2.5f,z,3.3f,0.2f,3.3f,zr,zg,zb);
                        caja(x,base+2.8f,z,1.5f,0.6f,1.5f,zr,zg,zb); }
            case 2 -> { caja(x,base+0.4f,z,6.5f,0.8f,5f,r*.80f,g*.80f,b*.80f);
                        caja(x,base+0.9f,z,5f,0.8f,3.5f,r*.70f,g*.70f,b*.70f);
                        caja(x,base+1.4f,z,3.5f,0.7f,2f,r*.60f,g*.60f,b*.60f); }
            case 3 -> { caja(x,base+0.5f,z,5.5f,1f,5.5f,r*.75f,g*.75f,b*.75f);
                        caja(x,base+1.1f,z,4f,1f,4f,r*.65f,g*.65f,b*.65f);
                        caja(x,base+1.7f,z,2.5f,1f,2.5f,r*.55f,g*.55f,b*.55f);
                        caja(x,base+2.3f,z,1f,1f,1f,r*.45f,g*.45f,b*.45f); }
            case 4 -> { caja(x-1.5f,base+0.6f,z,2.5f,1.2f,3.5f,zr,zg,zb);
                        caja(x,base+0.8f,z+2f,0.12f,2.5f,0.12f,0.3f,0.3f,0.35f);
                        entero("uEmision",1);
                        caja(x,base+2.1f,z+2f,0.35f,0.12f,0.35f,0.9f,0.15f,0.05f);
                        entero("uEmision",0); }
        }

        // Ventanas con marcos.
        if (noche) entero("uEmision", 1);
        for (float y = 2.2f; y < alt - 0.5f; y += 2.2f) {
            int piso = (int)(y / 2.2f);
            for (float d = -2f; d <= 2f; d += 2f) {
                caja(x+d,    y, z-3.52f, 1.05f,1.1f,0.05f, mr,mg,mb);
                caja(x+d,    y, z+3.52f, 1.05f,1.1f,0.05f, mr,mg,mb);
                caja(x-3.52f,y, z+d,     0.05f,1.1f,1.05f, mr,mg,mb);
                caja(x+3.52f,y, z+d,     0.05f,1.1f,1.05f, mr,mg,mb);
                boolean ab = noche || (piso + (int)(d+2)) % 2 == 0;
                float cr = ab ? 0.98f : r*.45f, cg = ab ? 0.92f : g*.45f, cb = ab ? 0.55f : b*.45f;
                caja(x+d,    y, z-3.53f, 0.75f,0.75f,0.04f, cr,cg,cb);
                caja(x+d,    y, z+3.53f, 0.75f,0.75f,0.04f, cr,cg,cb);
                caja(x-3.53f,y, z+d,     0.04f,0.75f,0.75f, cr,cg,cb);
                caja(x+3.53f,y, z+d,     0.04f,0.75f,0.75f, cr,cg,cb);
            }
        }
        entero("uEmision", 0);

        // Puerta sur.
        float fz = z + 3.52f;
        caja(x-0.65f,1.3f,fz, 0.15f,2.4f,0.06f, zr,zg,zb);   // Jamba izq.
        caja(x+0.65f,1.3f,fz, 0.15f,2.4f,0.06f, zr,zg,zb);   // Jamba der.
        caja(x,      2.5f,fz, 1.5f,0.2f,0.07f,  zr,zg,zb);   // Dintel.
        caja(x-0.34f,1.3f,fz, 0.54f,2.1f,0.06f, 0.45f,0.27f,0.10f); // Hoja izq.
        caja(x+0.34f,1.3f,fz, 0.54f,2.1f,0.06f, 0.42f,0.25f,0.09f); // Hoja der.
        caja(x, 0.40f,fz+0.28f, 1.8f,0.14f,0.50f, zr,zg,zb); // Escalón 1.
        caja(x, 0.26f,fz+0.58f, 2.0f,0.12f,0.50f, zr,zg,zb); // Escalón 2.
        caja(x, 0.14f,fz+0.88f, 2.2f,0.12f,0.50f, zr,zg,zb); // Escalón 3.
        if (noche) entero("uEmision", 1);
        caja(x, 2.8f, fz, 1.2f,0.26f,0.06f, 0.95f,0.90f,0.75f); // Rótulo.
        entero("uEmision", 0);
    }

    // ================================================================
    // 8. JARDINES RICOS
    // ================================================================

    private void dibujarJardin(int fi, int co, float x, float z) {
        int var = (fi + co * 2) % 3;
        caja(x, 0.33f, z, 9f,0.1f,9f, 0.20f,0.46f,0.28f); // Césped.
        caja(x, 0.37f, z, 1.2f,0.08f,9f, 0.68f,0.62f,0.52f); // Camino N-S.
        caja(x, 0.37f, z, 9f,0.08f,1.2f, 0.68f,0.62f,0.52f); // Camino E-O.

        for (float dx : new float[]{-2.8f,2.8f})
            for (float dz : new float[]{-2.8f,2.8f}) {
                caja(x+dx,1.4f,z+dz, 0.42f,2.8f,0.42f, 0.35f,0.20f,0.10f); // Tronco.
                caja(x+dx,3.1f,z+dz, 2.4f,1.5f,2.4f,   0.10f,0.38f,0.18f); // Copa baja.
                caja(x+dx,4.0f,z+dz, 1.6f,1.2f,1.6f,   0.13f,0.45f,0.22f); // Copa alta.
                caja(x+dx,4.7f,z+dz, 0.8f,0.8f,0.8f,   0.15f,0.50f,0.25f); // Punta.
            }

        caja(x, 0.82f,z-3.8f, 6.5f,0.92f,0.82f, 0.12f,0.36f,0.16f); // Seto N.
        caja(x, 0.82f,z+3.8f, 6.5f,0.92f,0.82f, 0.12f,0.36f,0.16f); // Seto S.
        caja(x-3.8f,0.82f,z,  0.82f,0.92f,6.5f, 0.12f,0.36f,0.16f); // Seto O.
        caja(x+3.8f,0.82f,z,  0.82f,0.92f,6.5f, 0.12f,0.36f,0.16f); // Seto E.

        float[][] cf = switch(var){
            case 0 -> new float[][]{{1f,.2f,.2f},{1f,.55f,0},{1f,.9f,.1f}};
            case 1 -> new float[][]{{.5f,.1f,.9f},{.2f,.4f,1f},{.1f,.8f,.8f}};
            default-> new float[][]{{1f,.1f,.6f},{.9f,.9f,.9f},{.3f,1f,.4f}};
        };
        float[][] pf = {{x-1.4f,z-1.4f},{x+1.4f,z-1.4f},{x-1.4f,z+1.4f},
                        {x+1.4f,z+1.4f},{x,z-3f},{x,z+3f}};
        for (int k = 0; k < pf.length; k++) {
            caja(pf[k][0],0.55f,pf[k][1], .12f,.4f,.12f, .15f,.50f,.15f);
            if (noche) entero("uEmision",1);
            float[] c = cf[k % cf.length];
            caja(pf[k][0],0.83f,pf[k][1], .32f,.18f,.32f, c[0],c[1],c[2]);
            entero("uEmision",0);
        }

        switch (var) {
            case 0 -> { // Fuente.
                for (int k=0;k<8;k++){float a=(float)(k*Math.PI/4);
                    caja(x+(float)Math.cos(a)*1.5f,.55f,z+(float)Math.sin(a)*1.5f,.55f,.45f,.55f,.62f,.64f,.68f);}
                caja(x,.85f,z,.26f,1.1f,.26f,.65f,.67f,.70f);
                caja(x,1.45f,z,.7f,.2f,.7f,.62f,.64f,.68f);
                entero("uEmision",1); caja(x,.38f,z,2.8f,.04f,2.8f,.25f,.55f,.85f); entero("uEmision",0);
            }
            case 1 -> { // Escultura.
                caja(x,.70f,z,1.2f,.8f,1.2f,.50f,.48f,.55f);
                caja(x,1.45f,z,.9f,.9f,.9f,.70f,.68f,.75f);
                cajaGirada(x,2.05f,z,.7f,.7f,.7f,.80f,.78f,.85f,.78f);
                caja(x,2.55f,z,.4f,.5f,.4f,.90f,.88f,.95f);
            }
            case 2 -> { // Glorieta.
                caja(x,.36f,z,2.5f,.1f,2.5f,.68f,.62f,.52f);
                float[][] dirs={{1.8f,0},{-1.8f,0},{0,1.8f},{0,-1.8f}};
                float[] angs={(float)-Math.PI/2,(float)Math.PI/2,0f,(float)Math.PI};
                for (int k=0;k<4;k++){float bx=x+dirs[k][0],bz=z+dirs[k][1];
                    cajaGirada(bx,.52f,bz,1.4f,.15f,.5f,.52f,.30f,.12f,angs[k]);
                    cajaGirada(bx,.42f,bz,1.2f,.35f,.2f,.22f,.24f,.24f,angs[k]);}
            }
        }
    }

    // ================================================================
    // 9. PEATONES
    // ================================================================

    /**
     * Dibuja un peatón animado: piernas, torso, cabeza y brazos.
     * Los brazos y piernas oscilan con Math.sin(tiempo * 4 + offset_individual).
     */
    protected void dibujarPeaton(int i) {
        float x  = pX[i], z = pZ[i], ang = pAng[i];
        float[][] ropas = {
            {.20f,.35f,.70f},{.80f,.25f,.25f},{.25f,.65f,.35f},{.85f,.60f,.10f},
            {.60f,.20f,.75f},{.25f,.70f,.75f},{.90f,.90f,.25f},{.50f,.40f,.30f},
            {.30f,.50f,.60f},{.75f,.45f,.55f},{.40f,.60f,.20f},{.60f,.50f,.40f},
        };
        float[] ro = ropas[i % ropas.length];
        float fase = (float)Math.sin(tiempo * 4.0 + i * 0.8);
        float cos = (float)Math.cos(ang), sin = (float)Math.sin(ang);

        // Pierna izquierda.
        cajaGirada(x+cos*(-.13f), .45f, z-sin*(-.13f), .13f,.55f,.13f, .12f,.09f,.08f, ang+fase*.28f);
        // Pierna derecha.
        cajaGirada(x+cos*(.13f),  .45f, z-sin*(.13f),  .13f,.55f,.13f, .11f,.08f,.07f, ang-fase*.28f);
        // Torso.
        cajaGirada(x, .97f, z, .32f,.46f,.20f, ro[0],ro[1],ro[2], ang);
        // Cabeza.
        cajaGirada(x, 1.38f, z, .23f,.23f,.23f, .92f,.75f,.58f, ang);
        // Brazo izquierdo.
        cajaGirada(x+cos*(-.23f), .97f, z-sin*(-.23f), .10f,.40f,.10f, ro[0]*.8f,ro[1]*.8f,ro[2]*.8f, ang-fase*.35f);
        // Brazo derecho.
        cajaGirada(x+cos*(.23f),  .97f, z-sin*(.23f),  .10f,.40f,.10f, ro[0]*.8f,ro[1]*.8f,ro[2]*.8f, ang+fase*.35f);
    }

    // ================================================================
    // 10. VEHÍCULOS AUTÓNOMOS
    // ================================================================

    /** Dibuja el vehículo autónomo i con su color y orientación actuales. */
    protected void dibujarAgente(int i) {
        float x = aX[i], z = aZ[i], ang = aAng[i];
        float cos = (float)Math.cos(ang), sin = (float)Math.sin(ang);
        float[] c = COL_AG[i];

        // Cada pieza: pA(cx, cz, ang, cos, sin, lx, ly, lz, sx, sy, sz, r, g, b)
        pA(x,z,ang,cos,sin,  0f,  .65f,  0f,  1.65f,.55f,2.6f,  c[0],c[1],c[2]);         // Carrocería.
        pA(x,z,ang,cos,sin,  0f, 1.12f, .12f,  1.3f,.55f,1.25f, c[0]*.8f,c[1]*.8f,c[2]*.8f); // Cabina.
        for (float lx : new float[]{-.88f,.88f})
            for (float lz : new float[]{-.82f,.82f})
                pA(x,z,ang,cos,sin, lx,.38f,lz, .24f,.58f,.6f, .055f,.065f,.08f);        // Ruedas.
        for (float fx : new float[]{-.55f,.55f}) {
            pA(x,z,ang,cos,sin, fx,.68f,-1.32f, .38f,.2f,.07f, 1f,.95f,.65f);            // Faro delant.
            pA(x,z,ang,cos,sin, fx,.68f, 1.32f, .35f,.17f,.07f, .85f,.05f,.05f);         // Luz trasera.
        }
    }

    /** Transforma una pieza del espacio local de un agente al espacio mundo y la dibuja. */
    private void pA(float ax, float az, float ang, float cos, float sin,
                    float lx, float ly, float lz,
                    float sx, float sy, float sz, float r, float g, float b) {
        cajaGirada(ax + cos*lx + sin*lz, ly, az - sin*lx + cos*lz,
                   sx, sy, sz, r, g, b, ang);
    }

    // ================================================================
    // 11. PUNTO DE ENTRADA
    // ================================================================

    public static void main(String[] args) { new CiudadViva().run(); }
}
