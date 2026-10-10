# Documento de Entrega — Ciudad y Conducción con OpenGL

**Estudiante:** Melissa Delgadillo Cortez        
**Registro:** 218016778       
**Asignatura:** Programación Gráfica  
**Proyecto:** Ciudad 3D con conducción, iluminación, entregas y texturas  
**Tecnologías:** Java 21 · Maven · LWJGL 3.3.3 · GLFW · OpenGL 3.3 Core Profile · GLSL 330  

---

## 1. Descripción del diseño de la ciudad

La ciudad se construye proceduralmente sobre un **mapa de cuadrícula 11 × 11** cuyos valores determinan el tipo de cada celda:

| Valor | Tipo de celda |
|-------|--------------|
| `0`   | Calle transitable |
| `1`   | Manzana con edificio |
| `2`   | Manzana con parque / jardín |

Las filas y columnas de índice **par** (0, 2, 4, 6, 8, 10) forman las avenidas que cruzan toda la ciudad. Las de índice **impar** (1, 3, 5, 7, 9) son manzanas. Cada celda mide 10 × 10 unidades de mundo; el mapa total abarca de −55 a +55 en X y Z (110 × 110 unidades, ~110 × 110 m).

### Distribución de manzanas

El mapa contiene **14 edificios** y **11 parques** (25 manzanas en total), alternados para crear variedad visual:

```
fila 0:  calle norte (todo 0)
fila 1:  edificio · parque · edificio · parque · parque
fila 3:  parque · edificio · edificio · edificio · parque
fila 5:  edificio · parque · parque · parque · edificio   ← banda central (más jardines)
fila 7:  parque · edificio · edificio · edificio · parque
fila 9:  edificio · parque · edificio · parque · edificio
fila 10: calle sur (todo 0)
```

### Elementos geométricos de la ciudad

Todos los objetos de la escena se construyen exclusivamente con **cajas transformadas** (cubo unitario escalado, girado y trasladado). No se usa ningún modelo 3D externo ni imagen de textura en disco.

**Calles:**
- Suelo de asfalto con textura procedural gris oscura con grietas y grano (tiling × 8).
- Marcas viales discontinuas amarillas en los tramos entre intersecciones.
- Pasos de cebra (7 franjas grises) al norte de cada manzana.
- Semáforos animados en la esquina de cada manzana.

**Edificios** (14 unidades):
- Altura variable entre 5 y 13 unidades (determinista por posición en el mapa).
- Siete paletas de color de fachada: terracota, vidrio gris-azulado, mármol beige, verde musgo, salmón mediterráneo, antracita corporativo, ocre colonial.
- Cinco tipos de remate de tejado: parapetos cruzados, torre con pináculo, escalonado piramidal, pirámide en 4 niveles, penthouse con antena.
- Ventanas en las cuatro fachadas por cada piso (iluminadas de noche con emisión).
- Puerta de entrada al sur con jambas, dintel, dos hojas, tres peldaños y rótulo.
- Texturas: zócalo y cornisa en concreto, fachada en ladrillo (tonos cálidos) o vidrio azulado (tonos fríos/oscuros).

**Parques** (11 unidades):
- Capa de césped con textura procedural verde con flores dispersas (tiling × 4).
- Cuatro árboles en las esquinas: tronco con textura de madera + tres niveles de copa.
- Setos verdes en los cuatro bordes + flores de colores en 6 posiciones.
- Caminos de concreto en cruz.
- Elemento central variable: **fuente** (8 pilares + lámina de agua emisiva), **escultura** (cubos apilados con uno girado), o **glorieta** (base + 4 bancas).

**Farolas** (16 unidades):
- Distribuidas en los cuatro cuadrantes del mapa a Y = 4.5 u.
- Poste gris + bombilla amarillo-naranja con emisión propia de noche.

**Peatones** (12):
- Modelo humanoide completo: piernas, torso, cabeza, brazos animados con seno.
- 12 paletas de ropa distintas.
- Caminan por las aceras con giros pseudo-aleatorios cada 1.5–3 s.

**Vehículos autónomos** (4):
- Siguen un circuito de 20 waypoints sobre las calles (perímetro + cruces).
- Mismo modelo que el vehículo del jugador, en azul, verde, amarillo y morado.
- Velocidades: 7, 8.5, 10 y 11.5 u/s.

---

## 2. Controles

| Tecla | Acción | Disponible desde |
|-------|--------|-----------------|
| **W** / ↑ | Acelerar hacia delante | Clase 2 |
| **S** / ↓ | Frenar / retroceder | Clase 2 |
| **A** / ← | Girar a la izquierda (mientras se mueve) | Clase 2 |
| **D** / → | Girar a la derecha (mientras se mueve) | Clase 2 |
| **Espacio** | Freno de mano (detención rápida) | Clase 2 |
| **C** | Alternar cámara: seguimiento trasero ↔ aérea orbital | Clase 2 |
| **R** | Reiniciar auto, entregas y cronómetro | Clase 2 |
| **N** | Alternar modo día ↔ noche | Clase 3 |
| **F** | Encender / apagar faros del vehículo | Clase 3 |
| **M** | Mostrar / ocultar minimapa | Clase 4 |
| ← / → (solo Clase 1) | Orbitar la cámara alrededor de la ciudad | Clase 1 |
| **ESC** | Salir de la aplicación | Todas |

**Indicadores en el título de la ventana:**  
`NombreClase | WASD/flechas: conducir | Espacio: freno | C: camara | R: reiniciar | XXX km/h | N: dia/noche | F: faros ON/OFF | M: mapa | Entregas X/3 | Frena en la marca dorada | XX s`

---

## 3. Funciones implementadas

El proyecto se desarrolló como una cadena de seis clases que se extienden sucesivamente:

```
CiudadBase → Conduccion → Iluminacion → Entregas → CiudadViva → Texturas
   (1)           (2)          (3)           (4)         (5)          (6)
```

### Clase 1 — `CiudadBase` (infraestructura y ciudad)
- Inicialización de GLFW y OpenGL 3.3 Core Profile.
- Compilación y enlace de shaders GLSL (vértices + fragmentos).
- Subida del cubo unitario a GPU (VAO + VBO, 36 vértices explícitos con posición, normal y UV).
- Definición del mapa 11 × 11 y dibujo procedural de la ciudad.
- Proyección en perspectiva manual (55° FOV) y ortográfica (para minimapa), sin matrices externas.
- Cámara orbital controlable con flechas.
- Caché de uniforms para evitar `glGetUniformLocation` por cada cuadro.
- Sistema de dibujo mediante `caja()` / `cajaGirada()`: posición, escala, color, giro.

### Clase 2 — `Conduccion` (vehículo y física)
- Modelo del vehículo con carrocería, cabina, 4 ruedas y faros (piezas en espacio local giradas con el ángulo del auto).
- Física de conducción basada en `deltaTime`:
  - Aceleración: 9 u/s².
  - Resistencia por decaimiento exponencial (`e^−0.7t`).
  - Freno de mano (factor × 7).
  - Giro proporcional a la velocidad (se invierte en reversa automáticamente).
  - Límites: −6 u/s (reversa) a +16 u/s (avance).
- Detección de colisiones círculo–rectángulo contra cada manzana y contra los bordes del mapa.
- Dos modos de cámara: seguimiento trasero (12 u detrás, 9 u de altura) y aérea orbital.
- Indicador de velocidad en km/h en el título de la ventana.

### Clase 3 — `Iluminacion` (iluminación dinámica)
- Shader de fragmentos con modelo de iluminación Phong local:
  - **Luz ambiental**: blanca moderada (día) o azulada tenue (noche).
  - **Luz difusa del sol** (Lambert): `dot(normal, dirSol)` × intensidad (0.62 día / 0.08 noche).
  - **16 farolas** (solo de noche): fuentes puntuales con atenuación cuadrática `1/(1 + 0.10d + 0.04d²)`.
  - **Faros del vehículo** (si encendidos): dos conos con `smoothstep(0.85, 0.97, alineación)`.
  - **Modo emisión** (`uEmision=1`): omite el cálculo de luz (bombillas, ventanas, semáforos encendidos).
- Alternancia día/noche (tecla N): cambia ambiente, intensidad solar y activa/desactiva farolas.
- 16 farolas distribuidas en los cuatro cuadrantes del mapa.
- Geometría de farolas (postes + bombillas) coherente con las posiciones del shader.

### Clase 4 — `Entregas` (juego, minimapa y decoración urbana)
- **Juego de 3 entregas**: destinos en (40,−40), (−40,−40) y (−40,40). Condición: distancia < 3 u y velocidad < 1 u/s.
- **Cronómetro** que se detiene al completar las 3 entregas.
- **Minimapa** en la esquina superior derecha:
  - Proyección ortográfica del mismo mundo.
  - `glScissor` para limpiar solo esa región sin afectar la vista principal.
  - Indicador cian de la posición del jugador con flecha de orientación.
  - Marca dorada del destino activo.
- **Baliza 3D** del destino: plataforma dorada + cubo flotante y giratorio animado con seno.
- **Decoración urbana**:
  - Parques: 4 árboles (tronco + copa) + banco (soporte + tablero + respaldo).
  - Edificios: ventanas en las 4 fachadas por cada piso.
  - Calles: pasos de cebra (7 franjas).
  - Semáforos animados: ciclo de 12 s (rojo 5 s, verde 5 s, amarillo 2 s), con emisión propia.
- Reinicio completo (R): posición, velocidad, ángulo, entregas y cronómetro.

### Clase 5 — `CiudadViva` (ciudad animada)
- **12 peatones** con posiciones iniciales seguras sobre calles:
  - Modelo humanoide con animación de brazos y piernas basada en `sin(tiempo × 4 + offset)`.
  - Lógica de movimiento con `deltaTime`, giros pseudo-aleatorios y detección de colisión con manzanas.
  - Colisión jugador–peatón: el peatón es empujado al contacto.
- **4 vehículos autónomos**:
  - Circuito de 20 waypoints sobre las calles, cada uno empieza en un waypoint distinto.
  - Giro suave hacia el waypoint destino (máx. 3 rad/s).
  - Colisión jugador–agente: frena al jugador y lo empuja fuera del radio.
- **Edificios detallados**: 7 paletas × 5 remates, ventanas con marcos + cristales, puertas con jambas, escalones y rótulo.
- **Jardines ricos**: 3 variantes de elemento central (fuente / escultura / glorieta), setos, flores, árboles en 3 niveles de copa.

### Clase 6 — `Texturas` (texturas procedurales)
- **6 texturas procedurales** de 128 × 128 px RGBA generadas en Java al primer cuadro y subidas a la GPU con `glTexImage2D`:

| ID | Nombre    | Algoritmo |
|----|-----------|-----------|
| 0  | Asfalto   | Base gris + ruido granular + grietas diagonales deterministas |
| 1  | Ladrillo  | Aparejo inglés con filas alternadas, tono variable por ladrillo |
| 2  | Concreto  | Gris claro + grano fino + manchas de humedad esporádicas |
| 3  | Césped    | Verde con variación + flores blancas y amarillas dispersas |
| 4  | Madera    | Vetas horizontales con `sin(y + ruido)` + variación longitudinal |
| 5  | Vidrio    | Gradiente azul-gris + reflejo especular diagonal + rejilla de paneles |

- Funciones de ruido: hash xor-shift (`ruido()`) + interpolación bilineal con smoothstep cúbico (`ruidoSuave()`).
- Filtrado trilineal (`GL_LINEAR_MIPMAP_LINEAR`) + mipmaps generados automáticamente.
- Uniform `uTileUV` para controlar la repetición por superficie (asfalto × 8, cesped × 4, etc.).
- Asignación por tipo de superficie: fachadas cálidas → ladrillo; fachadas frías → vidrio; aceras → concreto; etc.

---

## 4. Recursos y fragmentos externos utilizados

| Recurso | Origen | Uso |
|---------|--------|-----|
| LWJGL 3.3.3 | [lwjgl.org](https://www.lwjgl.org/) (licencia BSD 3) | Bindings Java para OpenGL, GLFW y utilidades de memoria |
| Técnica de proyección perspectiva manual en GLSL | Adaptada de los ejemplos del curso (`AppCamara.java`, `AppLaberinto.java`) | Cálculo de la cámara en el vertex shader sin matrices externas |
| Algoritmo de colisión círculo–rectángulo (clamp + dist²) | Técnica estándar de computación gráfica / game dev | `puedeCircular()` en `Conduccion` |
| Hash xor-shift para ruido procedural | Técnica estándar (Knuth / hash mixing) | `ruido()` en `Texturas` |
| Suavizado smoothstep cúbico | Técnica estándar (Ken Perlin) | `ruidoSuave()` en `Texturas` y conos de faros en el shader |

Toda la geometría, los shaders, la lógica de juego y las texturas fueron escritos de cero para este proyecto. No se usan imágenes externas, modelos 3D importados, motores de juego ni bibliotecas de matemáticas.

---

## 5. Limitaciones conocidas

- **Sin sombras**: la iluminación es puramente local (Phong). La luz de las farolas puede atravesar edificios; no hay shadow maps.
- **Sin oclusión ambiental**: las caras en ángulos grazing reciben algo de luz irreal.
- **Semáforos decorativos**: cambian de color pero no bloquean ni frenan al vehículo ni a los agentes.
- **Peatones en modo minimapa**: se omiten para reducir carga de dibujo; no aparecen en la vista superior.
- **Físicas simplificadas**: el modelo de conducción no simula transferencia de peso, derrape ni neumáticos; el giro es proporcional a la velocidad de forma lineal.
- **Colisión de agentes**: los vehículos autónomos no colisionan entre sí, solo con el jugador.
- **Sin audio**: no hay efectos de sonido ni música.
- **Sin ruedas animadas**: las ruedas no giran con la velocidad; son cajas estáticas.
- **Resolución de texturas fija**: 128 × 128 px para todas las texturas; puede verse pixelado en superficies muy amplias sin suficiente tiling.
- **Compatibilidad**: requiere OpenGL 3.3 Core Profile y un controlador de gráficos compatible. No funciona en servidores sin pantalla (excepto modo headless con `demo.frames`).
- **Advertencias con JDK 25+**: el proyecto objetivo es Java 21; con versiones superiores aparecen advertencias de acceso nativo (Unsafe, JNI). Se recomienda JDK 17–21 para una ejecución limpia.

---

## 6. Instrucciones de ejecución

### Requisitos previos

- **JDK 17 o superior** (recomendado JDK 21): `java -version`
- **Apache Maven 3.6+**: `mvn -version`
- Sesión gráfica activa (no funciona en servidores sin pantalla)
- Controlador de GPU con soporte OpenGL 3.3 Core Profile

### Estructura de archivos a entregar

```
CiudadConduccion/
├── pom.xml                                      ← configuración Maven y dependencias
├── README.md                                    ← guion de enseñanza
├── ENTREGA.md                                   ← este documento
└── src/
    ├── main/java/com/graphics/
    │   ├── CiudadBase.java                      ← Clase 1
    │   ├── Conduccion.java                      ← Clase 2
    │   ├── Iluminacion.java                     ← Clase 3
    │   ├── Entregas.java                        ← Clase 4
    │   ├── CiudadViva.java                      ← Clase 5
    │   └── Texturas.java                        ← Clase 6 (versión final)
    └── test/java/com/graphics/
        └── ConduccionTest.java                  ← pruebas de lógica sin ventana
```

> **Nota:** las dependencias de LWJGL (JARs y nativos) las descarga Maven automáticamente desde Maven Central. No se incluyen en la entrega. El directorio `target/` está excluido por `.gitignore`.

### Comando para ejecutar la versión final

Abrir una terminal en la carpeta donde está `pom.xml` y ejecutar:

```sh
mvn compile exec:java
```

Esto compila y ejecuta `com.graphics.Texturas` (la versión final con texturas procedurales). La primera ejecución descarga las dependencias automáticamente.

### Ejecutar una etapa específica

```sh
mvn compile exec:java -Dexec.mainClass=com.graphics.CiudadBase     # Clase 1: solo ciudad
mvn compile exec:java -Dexec.mainClass=com.graphics.Conduccion      # Clase 2: + vehículo
mvn compile exec:java -Dexec.mainClass=com.graphics.Iluminacion     # Clase 3: + iluminación
mvn compile exec:java -Dexec.mainClass=com.graphics.Entregas        # Clase 4: + minimapa y entregas
mvn compile exec:java -Dexec.mainClass=com.graphics.CiudadViva      # Clase 5: + ciudad viva
mvn compile exec:java -Dexec.mainClass=com.graphics.Texturas        # Clase 6: versión final
```

### Ejecutar en macOS

El perfil `macos` se activa automáticamente por Maven al detectar el SO. Agrega `-XstartOnFirstThread` al proceso de la JVM, requerido por GLFW en macOS. No se necesita ningún cambio manual.

### Ejecutar las pruebas automáticas (sin ventana gráfica)

```sh
mvn test
```

Las 3 pruebas verifican:
1. Rutas transitables sobre las calles del mapa.
2. Colisiones con manzanas y límites del mapa.
3. Reinicio correcto del estado de la partida.

### Verificación rápida con arranque gráfico breve

Para confirmar que los shaders compilan y la ventana se abre correctamente:

```sh
mvn compile exec:java -Ddemo.frames=6
```

La ventana dibuja 6 cuadros y se cierra sola. Requiere pantalla y GPU.

### Notas de plataforma

| SO | Comando | Observación |
|----|---------|-------------|
| Windows | `mvn compile exec:java` | Funciona directamente |
| macOS Intel / Apple Silicon | `mvn compile exec:java` | Perfil `macos` agrega `-XstartOnFirstThread` automáticamente |
| Linux x64 / ARM64 | `mvn compile exec:java` | Requiere display X11 o Wayland activo |

---

## 7. Práctica manual de verificación

Para comprobar todas las funcionalidades implementadas:

1. **Ejecutar la versión final** (`mvn compile exec:java`) y verificar que la ciudad carga con texturas y modo nocturno.
2. **Conducir** con WASD; verificar aceleración, giro y freno. Chocar contra una manzana para confirmar la colisión.
3. **Reversa** con S desde parado; verificar que el volante se invierte.
4. **Freno de mano** con Espacio; verificar detención rápida.
5. **Alternar cámara** con C; observar la ciudad completa desde arriba.
6. **Tecla N**: verificar cambio día ↔ noche (ambiente, farolas, ventanas).
7. **Tecla F**: verificar encendido/apagado de los conos de los faros.
8. **Tecla M**: mostrar/ocultar minimapa; redimensionar la ventana para comprobar que se reescala.
9. **Completar las 3 entregas**: navegar a las marcas doradas, frenar hasta `< 1 km/h` sobre la plataforma.
10. **Reiniciar** con R tras ganar; verificar que el cronómetro y el contador se reinician.
11. **Observar peatones y agentes** circulando; provocar colisión con un agente.

---


