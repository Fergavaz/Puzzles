# PROMPT PARA EL AGENTE: terminar la app Android de puzzles (Zip, Patches, Mini Sudoku, Tango, Queens)

Actúa como ingeniero Android senior (Kotlin, Jetpack Compose, Hilt, DataStore). Tienes un proyecto Gradle casi completo, escrito sin poder compilar la parte Android. Tu trabajo es **dejarlo compilando, probado, jugable en un emulador y pulido**, sin romper las garantías del motor. Trabaja de forma autónoma, verifica todo ejecutándolo y no des nada por bueno sin evidencia (salida de tests, build, capturas o logs).

---

## 1. Contexto del proyecto

App Android nativa **100 % offline** que replica los juegos de LinkedIn: **Zip, Patches, Mini Sudoku, Tango y Queens**. Cada juego tiene reglas, solver, generador procedural con **solución única garantizada**, niveles de campaña precargados y UI propia.

Stack: Kotlin 2.0.21, Jetpack Compose + Material 3, MVVM + Clean Architecture, Hilt (KSP), Jetpack DataStore (Preferences), Navigation Compose, corrutinas/Flow. Sin backend y sin librerías de terceros para la lógica de los puzzles.

### Estructura

```
puzzles/
├── settings.gradle.kts, build.gradle.kts      (AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Hilt 2.52)
├── engine/                                    MÓDULO JVM PURO (prohibido importar android.*)
│   └── src/main/kotlin/com/tuapp/puzzles/
│       ├── core/        Difficulty, GameType, PuzzleLevel, CampaignLevel, CampaignProgress, Json, Streak
│       ├── engine/
│       │   ├── tango|queens|sudoku|zip|patches/   Models, Rules (validador), Solver, Generator
│       │   ├── session/   TangoSession, QueensSession, SudokuSession, ZipSession, PatchesSession, Snapshotted
│       │   ├── LevelCodec.kt (nivel<->JSON, CampaignCodec), Generators.kt (punto de entrada único)
│       └── tools/GenerateCampaign.kt   (genera assets/campaign/*.json)
│   └── src/test/...     tests JUnit/kotlin.test del motor
└── app/                                       MÓDULO ANDROID
    ├── src/main/assets/campaign/{zip,patches,sudoku,tango,queens}.json   (145 niveles ya generados)
    ├── src/main/kotlin/com/tuapp/puzzles/
    │   ├── PuzzlesApp.kt, MainActivity.kt
    │   ├── data/   ProgressRepository (+DataStore), CampaignRepository, LevelGeneratorRepository
    │   ├── di/     DataModule, RepositoryModule, Qualifiers
    │   └── ui/     theme, nav (Routes/AppNav), home, campaign, game (ViewModel+Screen), boards/*
    ├── src/test/       DataStoreProgressRepositoryTest
    └── src/androidTest/ BoardsSmokeTest
```

### Qué está verificado y qué NO

- **Verificado (compilado y ejecutado fuera de Gradle):** todo el módulo `engine` (61 tests pasando: reglas, solvers, unicidad, determinismo por semilla, codecs JSON, sesiones, y un test que revalida que los 145 niveles de campaña tienen solución única). Se ejecutó con un lanzador propio, **no con Gradle**: confirma que `./gradlew :engine:test` funciona con `kotlin("test")` + `useJUnitPlatform()`.
- **NO verificado:** el módulo `:app` completo (Gradle Android, Hilt/KSP, DataStore, Compose, navegación, tests de app y androidTest). Se escribió sin Android SDK. **Espera errores de compilación y de ejecución** y corrígelos.

---

## 2. Reglas de oro (no negociables)

1. **El motor sigue siendo JVM puro**: nada de `android.*` en `engine/`. Los generadores deben seguir siendo **deterministas** (`kotlin.random.Random(seed)`; misma semilla + dificultad = mismo puzzle).
2. **Unicidad de solución**: ningún puzzle (generado o de campaña) puede tener varias soluciones o ninguna. Si cambias un generador, solver o regla, los tests de unicidad deben seguir pasando y debes volver a ejecutar `./gradlew :engine:generateCampaign` si afecta a los niveles precargados (y confirmar que el test de campaña pasa).
3. **No cambies el comportamiento del motor para "arreglar" la UI** sin añadir/ajustar tests. Si hay un bug de motor, escribe primero un test que falle.
4. Sin dependencias de terceros para la lógica de puzzles. Se permiten las librerías AndroidX/Compose/Hilt ya declaradas y las que necesites razonablemente para tooling (no para lógica).
5. Todo funciona **sin Internet** y sin permisos en el manifest.
6. Las versiones declaradas son estables pero no las últimas. Puedes subirlas (AGP, Kotlin, Compose BOM, Hilt, etc.) **solo si hace falta o es claramente seguro**, manteniendo Kotlin 2.x y compatibilidad KSP↔Kotlin. Documenta cualquier cambio.

---

## 3. Plan de trabajo (en este orden)

### Fase 0 — Entorno
- JDK 17, Android SDK (compileSdk 35), Android Studio reciente o línea de comandos. Genera el **Gradle wrapper** (`gradle wrapper`) si falta y comítealo.
- `./gradlew :engine:test` → debe pasar (61 tests). Si `kotlin("test")` no resuelve JUnit 5, ajusta las dependencias de test (`kotlin-test-junit5`/`junit-jupiter`) sin cambiar los tests.

### Fase 1 — Que compile `:app`
- `./gradlew :app:assembleDebug`. Corrige errores hasta compilar limpio. Puntos de riesgo conocidos:
  - Plugins/versiones: KSP ↔ Kotlin ↔ Hilt; `kotlinOptions { jvmTarget }` puede estar deprecado → migrar a `kotlin { compilerOptions { ... } }` o `jvmToolchain(17)`.
  - `FAIL_ON_PROJECT_REPOS` en `settings.gradle.kts`: ningún módulo debe declarar `repositories {}`.
  - Compose: `drawText(TextMeasurer, …)`, `Float.toSp()` dentro de `DrawScope`, `LifecycleEventEffect`, `FilterChip`/`Card(onClick)`/`TopAppBar` (`@OptIn(ExperimentalMaterial3Api)`), `LinearProgressIndicator(progress = { })`, iconos `Icons.AutoMirrored.Filled.*` (`material-icons-extended`).
  - Navigation: rutas con argumentos opcionales (`play/{game}?levelId={levelId}&seed={seed}&difficulty={difficulty}`), `popUpTo(Routes.PLAY)`; `SavedStateHandle` debe recibir `game`, `levelId`, `seed`, `difficulty`.
  - Hilt: `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, módulos `@Binds`/`@Provides`, qualifiers de dispatchers, `DayClock` como `fun interface`.
  - DataStore: una sola instancia (delegado `preferencesDataStore` a nivel de archivo).
  - `GameViewModel`: `when` sobre sesiones (cada rama con `else -> Unit`), referencias `vm::perform`, `vm::undo`, etc.
- Fija warnings relevantes (no es necesario cero warnings, sí cero errores).

### Fase 2 — Tests automáticos
- `./gradlew :app:testDebugUnitTest` (DataStoreProgressRepositoryTest con `runTest` y `backgroundScope`; si el archivo `.preferences_pb` o el scope dan problemas, arréglalo).
- `./gradlew :app:connectedDebugAndroidTest` en emulador (BoardsSmokeTest: Tango y Queens). Añade tests de UI básicos con Compose Test para al menos: Mini Sudoku (seleccionar celda + paleta), Home (aparecen los 5 juegos) y Campaña (todos los niveles son clicables: **juego libre**).
- Añade un test de integración con Hilt si es razonable (`@HiltAndroidTest`) o, como mínimo, prueba `DefaultLevelGenerator` (incluido el **respaldo** a campaña cuando el generador devuelve `null`).

### Fase 3 — Verificación funcional en emulador (obligatoria)
Juega cada juego de principio a fin (usa el botón Pista para acelerar) y confirma:

| Juego | Comprobaciones |
|---|---|
| **Tango** | Toque cíclico vacío→☀️→🌙→vacío; celdas dadas no editables; badges `=`/`×` bien centrados entre celdas; conflictos en rojo; detecta resuelto. |
| **Queens** | Toque cíclico vacío→✕→♛→vacío; bordes gruesos entre regiones; conflictos (fila/columna/región/diagonal) resaltados; detecta resuelto. |
| **Mini Sudoku** | Selección + paleta 1–6 + borrar; regiones 2×3 con borde grueso correcto; conflictos en rojo; pistas fijas no editables. |
| **Zip** | Arrastre continuo desde el 1; no salta celdas en gestos rápidos; volver sobre los pasos retrocede; muros infranqueables; números en orden; Deshacer retrocede un trazo; detecta resuelto (el camino debe terminar en el número más alto). |
| **Patches** | Arrastre entre dos esquinas; vista previa con nº de celdas; borde rojo si inválido; al soltar válido se coloca, si no se descarta; tocar un parche colocado lo quita; la pista puede estar dentro del rectángulo; detecta resuelto. |

Para todos: barra superior (volver, título/nivel, temporizador, Deshacer, Borrar todo, Pista, Reiniciar), diálogo de felicitación con tiempo y botón "Siguiente", el temporizador se pausa al pasar a segundo plano, modo claro/oscuro, rotación/cambio de tamaño de pantalla sin perder la partida, y **persistencia del progreso al cerrar y reabrir la app**.

Navegación esperada: Home → "Jugar campaña" (lista en **juego libre**: ningún nivel bloqueado, ✓ en completados, mejor tiempo, filtro por dificultad, botón "Continuar") y Home → "Generar nuevo" (diálogo de dificultad 1–5, indicador de carga, nivel generado en `Dispatchers.Default`).

Corrige todo bug que encuentres. Si un fallo viene de la lógica de un tablero, reprodúcelo antes con un test de sesión en `engine/`.

### Fase 4 — Rendimiento (criterio de aceptación)
- "Generar nuevo" debe tardar **< 3 s en un dispositivo de gama media** por juego y dificultad. En el PC los generadores tardan ≤ ~250 ms; **mídelo en emulador/dispositivo real** (especialmente **Queens 9×9/10×10** y **Patches 10×10 dificultad 4–5**). Si algún caso se acerca al límite, optimiza (p. ej. reducir `maxAttempts`, topes de nodos del solver, o reparar regiones antes) **sin perder unicidad** y con tests.
- Si la generación falla o tarda de más, el usuario nunca debe quedarse bloqueado: ya existe un **nivel de respaldo de campaña**; comprueba que funciona y que la UI lo indica con claridad (`fromFallback`).
- Gestos de Zip y Patches fluidos (~60 fps): perfila con Layout Inspector/Profile GPU rendering; evita asignaciones en el hilo de dibujo si ves jank (p. ej. en `ZipBoard`, el `Path` y las listas se recalculan en cada frame).

### Fase 5 — Pulido y huecos conocidos
Implementa o decide con criterio (documenta lo que descartes):
1. **Icono de la app** (adaptive icon), nombre y tema; `themes.xml` ya tiene un tema mínimo.
2. **Strings a recursos** (`strings.xml` en español, preparado para añadir otros idiomas); ahora los textos están embebidos en el código.
3. **Pista con feedback visual**: resaltar brevemente la celda/parche/tramo revelado (hoy la pista solo aplica el movimiento).
4. **Accesibilidad**: revisar `contentDescription` de celdas y botones, tamaños táctiles ≥ 48 dp, contraste en modo oscuro (colores de regiones de Queens/Patches), y descripciones útiles para los canvas de Zip y Patches.
5. **Feedback de victoria** más claro (animación sencilla) y mostrar **récord** si se mejora el mejor tiempo.
6. Pantalla de **historial de generados** usando `ProgressRepository.generatedHistory` (se pueden regenerar con su semilla): opcional pero deseable.
7. **Reanudar partida** tras matar el proceso (guardar el estado de la sesión en `SavedStateHandle` o DataStore): opcional.
8. Tablets/pantallas grandes: el tablero debe adaptarse (limitar ancho máximo y centrar).
9. Revisar la **regla de Zip**: se implementó que el camino empieza en el 1, pasa por los números en orden, cubre todas las celdas y **termina en el número más alto**. Si decides cambiarla, hazlo en `ZipRules`, `ZipSolver` y `ZipGenerator` y regenera la campaña.

### Fase 6 — Entrega
- `./gradlew clean :engine:test :app:testDebugUnitTest :app:assembleDebug` en verde, y `:app:assembleRelease` compilando (puedes activar R8 si lo validas).
- APK debug generado y probado en emulador.
- README breve: cómo compilar, ejecutar tests, regenerar la campaña (`./gradlew :engine:generateCampaign`), estructura y decisiones.
- Informe final con: cambios realizados, versiones modificadas, resultados de tests, tiempos de generación medidos y **cualquier punto no resuelto**.

---

## 4. Criterios de aceptación (checklist final)

- [ ] Compila sin errores en Android Studio reciente (Kotlin 2.x, Compose BOM vigente).
- [ ] `engine` sin dependencias de Android; todos sus tests pasan; mínimo 5 tests por juego (existentes).
- [ ] Los cinco juegos se pueden jugar de principio a fin y la app detecta correctamente cuándo están resueltos.
- [ ] Los 145 niveles precargados (Zip 20, Patches 30, Mini Sudoku 40, Tango 30, Queens 25) son jugables con solución única.
- [ ] "Generar nuevo" produce puzzles válidos de solución única en < 3 s en gama media, con respaldo si falla.
- [ ] Progreso (completados, mejores tiempos, racha, generados) persistente tras cerrar y reabrir.
- [ ] Campaña en **juego libre** (sin bloqueos) con progreso marcado visualmente.
- [ ] Funciona 100 % offline y sin permisos.
- [ ] Modo claro/oscuro, accesibilidad básica y UI fluida en distintos tamaños de pantalla.

## 5. Cómo reportar

Al terminar cada fase, resume en pocas líneas: qué corregiste, qué comandos ejecutaste y su resultado. Si algo no se puede arreglar sin tomar una decisión de diseño, **pregunta** en vez de improvisar. No marques una casilla del checklist sin haberla comprobado.
