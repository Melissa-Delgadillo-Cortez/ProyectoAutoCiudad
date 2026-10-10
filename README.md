# Ciudad y conducción con OpenGL: cuatro clases

Proyecto de enseñanza basado en `AppCamara.java` y `AppLaberinto.java` de tus clases: Java 17, Maven, LWJGL 3.3.3, GLFW, OpenGL 3.3 Core, shaders GLSL, VAO y VBO. Toda la geometría se genera con código; no requiere imágenes ni modelos externos.

## Ejecutar

Abre una terminal **en esta carpeta**, donde está `pom.xml`. Necesitas un JDK 17 o superior y Maven (`java -version`, `mvn -version`). La primera compilación descarga las dependencias.

```sh
mvn compile exec:exec -DmainClass=com.graphics.CiudadBase
mvn compile exec:exec -DmainClass=com.graphics.Conduccion
mvn compile exec:exec -DmainClass=com.graphics.Iluminacion
mvn compile exec:exec -DmainClass=com.graphics.Entregas
```

Ejecuta un comando por vez; ESC cierra la ventana. `mvn compile exec:exec` abre la versión final por defecto. En macOS, el perfil Maven agrega `-XstartOnFirstThread`. Si ejecutas desde un IDE en Mac, agrega ese argumento a las opciones de la JVM. Usa `exec:exec`, porque `exec:java` no inicia el proceso de esa forma.

Incluye bibliotecas nativas para macOS Intel/Apple Silicon, Windows x64 y Linux x64/ARM64. Se necesita una sesión gráfica y un controlador compatible con OpenGL 3.3. No está diseñado para ejecutarse en un servidor sin pantalla.

## Cómo se acumulan los avances

```text
CiudadBase: ciudad, mapa y renderizador
  └── Conduccion extends CiudadBase: auto, conducción y colisiones
        └── Iluminacion extends Conduccion: iluminación
              └── Entregas extends Iluminacion: detalles urbanos, entregas y minimapa
```

Cada archivo tiene su propio `main` y puede ejecutarse como una etapa distinta dentro del proyecto. Las etapas posteriores necesitan los archivos anteriores: `extends` reutiliza el avance y `super.escena()` lo dibuja antes de añadir contenido. No son cuatro copias independientes. Así, en Conduccion se explica el auto sin repetir la configuración de OpenGL. Se conservaron los nombres en minúscula solicitados; en proyectos Java es habitual usar `CiudadBase`.

## Cómo leer el código comentado

Los cuatro archivos están divididos en secciones numeradas. Cada instrucción tiene un comentario en español; cada método indica su propósito. Las llaves y líneas vacías solo separan bloques. Los shaders también están explicados línea por línea dentro de sus cadenas GLSL.

- **CiudadBase:** variables → inicio/ciclo/limpieza → teclado/cámara → ciudad → cajas/uniforms → shaders → vértices del cubo.
- **Conduccion:** variables del auto → teclado → movimiento → colisiones → cámara → piezas del vehículo.
- **Iluminacion:** luces → controles → envío de datos → farolas → shader de iluminación.
- **Entregas:** partida → controles → entregas → destino → decoración → minimapa.

Se usa una instrucción por línea, condiciones con llaves y cálculos intermedios con nombres descriptivos. El cubo contiene sus 36 vértices explícitos, como en los ejemplos originales. En `caja()` y `pieza()`, los argumentos mantienen este orden: posición X/Y/Z, tamaño X/Y/Z y color rojo/verde/azul; `cajaGirada()` agrega el ángulo al final.

## Guion de enseñanza

### Clase 1 — Crear primero la ciudad

**Resultado:** una ciudad 3D con calles conectadas, marcas viales, edificios, aceras y parques. Las flechas izquierda/derecha orbitan la cámara.

1. Leer `MAPA`: 0 es calle, 1 edificio y 2 parque. Las filas corresponden a Z y las columnas a X; Y es altura.
2. Explicar `centro()`: convierte índices de matriz a coordenadas del mundo. Cada celda mide 10 unidades y el mapa ocupa 70 × 70.
3. Revisar `crearCubo()`: posiciones y normales en VBO, atributos en VAO y 36 vértices.
4. Seguir `caja()` y `cajaGirada()`: un cubo unitario se transforma en asfalto, edificio o línea.
5. Leer `vertexShader()`: modelo, cámara y perspectiva. `uMapa` deja preparado el segundo tipo de proyección que se usa en Entregas.
6. Recorrer `run()`, `iniciar()`, `loop()`, `dibujarFrame()` y `limpiar()`: eventos, actualización, limpieza, dibujo y presentación.

**Ejercicio:** cambiar una manzana de edificio a parque y variar las alturas. Mantener calles conectadas. Las normales se preparan aquí, pero la iluminación se introduce en Iluminacion.

### Clase 2 — Crear y mover el auto

**Resultado:** auto rojo con cabina, ruedas y faros, cámara de seguimiento y colisiones con manzanas y borde.

1. Leer `dibujarAuto()` y `pieza()`: las piezas usan coordenadas locales que giran y se trasladan juntas.
2. Estudiar `actualizar(deltaTime)`: aceleración, resistencia, freno y límites de velocidad.
3. Relacionar seno/coseno con la dirección del auto. Su frente local es -Z; el giro está en radianes.
4. Explicar el giro proporcional a la velocidad y el cambio de dirección al retroceder.
5. Revisar `puedeCircular()`: círculo contra rectángulo, punto más cercano y distancia al cuadrado.
6. Alternar cámaras con C para observar la conducción en el mapa.

**Ejercicio:** modificar aceleración y resistencia y comparar el manejo. El círculo de colisión es conservador para contener todas las piezas. El choque detiene al auto; no hay rebotes. El paso temporal se limita para evitar atravesar una manzana tras una pausa larga.

### Clase 3 — Focos e iluminación

**Resultado:** ambiente nocturno, nueve farolas que iluminan superficies, faros del vehículo y cambio día/noche.

1. Comparar el shader de color plano de CiudadBase con `fragmentShader()` de Iluminacion.
2. Estudiar normales y Lambert: `max(dot(normal, direccionLuz), 0)`.
3. Revisar ambiente y luz direccional del sol.
4. Relacionar las coordenadas de `LUCES` con los postes dibujados en `escena()`.
5. Analizar cómo disminuye la luz con la distancia.
6. Explicar el cono de los faros con producto escalar y `smoothstep`.
7. Comparar emisión de la bombilla con la luz calculada sobre el suelo. Son fenómenos distintos.

**Ejercicio:** variar el color y la atenuación de una farola. La iluminación es local, sin sombras ni oclusión: una luz puede atravesar un edificio. Implementar shadow maps queda como ampliación. El cielo conserva el mismo fondo para concentrar la comparación día/noche en las superficies.

### Clase 4 — Ciudad final y minimapa

**Resultado:** parques con árboles y bancos, ventanas iluminadas, pasos peatonales, semáforos animados, minimapa y juego de tres entregas.

1. Estudiar `decorarCiudad()` y sus métodos `dibujarParque()`, `dibujarVentanas()`, `dibujarPasoPeatonal()` y `dibujarSemaforo()`: composición de objetos reutilizando cajas y la matriz.
2. Leer `DESTINOS` y `actualizar()`: acercarse a menos de 3 unidades y frenar a menos de 1 unidad/segundo completa una entrega.
3. Seguir `dibujarFrame()`: primero la cámara principal, después una vista ortográfica en otro viewport.
4. Explicar `glScissor`: permite borrar solo el recuadro del minimapa y su profundidad.
5. Mostrar por qué se restauran viewport, scissor y `uMapa` al terminar.
6. Seguir el indicador cian del auto y su punta blanca; la marca dorada indica el destino activo.

**Ejercicio:** agregar una cuarta entrega sobre una calle o modificar el tamaño del minimapa. El minimapa mantiene el norte (-Z) arriba. El destino se muestra también como baliza dorada en el mundo. Al completar tres entregas aparece GANASTE en el título; R reinicia.

## Controles

| Tecla | Acción | Desde |
|---|---|---|
| Izquierda / derecha | Orbitar la ciudad | Clase 1 únicamente |
| W / S o arriba / abajo | Acelerar / frenar y retroceder | Clase 2 |
| A / D o izquierda / derecha | Girar mientras el auto se mueve | Clase 2 |
| Espacio | Freno | Clase 2 |
| C | Cámara de seguimiento / aérea oblicua | Clase 2 |
| R | Reiniciar auto y, en Entregas, las entregas | Clase 2 |
| N | Día / noche | Clase 3 |
| F | Encender / apagar faros | Clase 3 |
| M | Mostrar / ocultar minimapa | Clase 4 |
| ESC | Salir | Todas |

El título de la ventana muestra velocidad en km/h (se supone una unidad = un metro), luces, entregas y tiempo. Puede truncarse si la ventana es pequeña. La velocidad máxima real es algo menor que el límite por la resistencia aplicada. Los semáforos son decorativos: cambian de color pero no bloquean al vehículo. No hay tráfico, peatones, audio, sombras, modelos importados ni ruedas animadas; este es el alcance del ejemplo didáctico finalizado.

## Verificación

```sh
mvn test
```

Las pruebas de lógica comprueban rutas transitables, manzanas, márgenes de colisión y reinicio sin abrir ventanas. Para un arranque gráfico breve puede pasarse `-Ddemo.frames=6` a la **JVM del juego**; al llegar a ese número de cuadros la ventana se cierra. Esta prueba necesita pantalla y comprueba también compilación/enlace de shaders y errores OpenGL.

Práctica manual: ejecutar cada etapa; conducir y chocar con una acera; retroceder; cambiar cámara; alternar N/F; redimensionar la ventana; alternar M; completar las tres entregas y reiniciar con R.

Verificado en este equipo: las cuatro etapas abrieron un contexto gráfico, dibujaron seis cuadros y cerraron sin errores OpenGL. Con Java 25 y LWJGL 3.3.3 aparecen advertencias de acceso nativo, Unsafe y versión JNI; para impartir las clases se recomienda usar JDK 17, la versión objetivo de los ejemplos originales. El arranque breve no sustituye completar manualmente el recorrido.
