# 🧩 Puzzles — Colección de Juegos de Lógica para Android

Aplicación nativa para Android que reúne **6 rompecabezas de lógica** clásicos y modernos, con modo campaña precargado, generación procedural infinita con **solución única garantizada**, sistema de rachas diarias y pistas inteligentes en 2 pasos. Funciona **100 % offline**, sin anuncios ni permisos de red.

---

## 📥 Descarga e Instalación

Descarga el archivo APK listo para instalar en cualquier dispositivo Android (Android 8.0 / API 26 o superior):

- 📦 **[Últimas versiones en GitHub Releases](https://github.com/Fergavaz/Puzzles/releases)** — APKs oficiales (`puzzles.fergavaz.apk`), registro de cambios y notas de lanzamiento.
- 🏷️ **[Tags y versiones del repositorio](https://github.com/Fergavaz/Puzzles/tags)**
- 🔄 **Comprobación de actualizaciones integrada:** la propia aplicación avisa cuando hay una nueva versión disponible y permite descargarla directamente.

---

## 🎮 Los 6 Juegos Incluidos

| Juego | Descripción | Tablero / Reglas clave |
|---|---|---|
| ☀️ **Tango** | Lógica binaria con soles y lunas | Mismo número de soles y lunas por fila/columna, máximo 2 iguales consecutivos, sin filas/columnas idénticas y restricciones `=` / `×` entre celdas adyacentes. Pistas avanzadas en 2 pasos (signos, parejas, sándwich, conteo y deducción de línea). |
| 👑 **Queens** | Reinas por regiones de color | Exactamente 1 corona por fila, columna y región de color, sin que dos coronas se toquen ni siquiera en diagonal. Auto-marcado configurable de casillas colindantes con rollback coherente en Deshacer, y sistema de pistas en 2 pasos (errores primero, regalo, nocaut, confinadas, n-en-n e hipótesis). |
| ➕ **Sumbreak** | Fusión y reducción aritmética | Reduce toda la cuadrícula a un único número. Arrastra o toca números iguales adyacentes para sumarlos ($X + X = 2X$) o combínalos con fichas modificadoras (`+`, `-`, `×`, `★`). Detección automática de bloqueos (*dead-ends*) y sugerencia de movimientos óptimos por BFS. |
| 🔢 **Mini Sudoku** | Sudoku rápido de formato compacto | Cuadrícula `6×6` dividida en bloques de `2×3` con números del `1` al `6` y modo de notas (candidatos). |
| 🔗 **Zip** | Camino hamiltoniano secuencial | Conecta los números en orden ascendente (`1 → 2 → 3…`) visitando **todas** las celdas del tablero una sola vez y respetando los muros entre casillas. |
| 🟦 **Patches** | Partición geométrica (estilo Shikaku) | Cubre todo el tablero con rectángulos sin solaparse según las pistas de área y proporción (`■` cuadrado, `↔` horizontal, `↕` vertical, `┅` libre). |

---

## ✨ Características Principales

- **Dos modos de juego con progresión infinita:**
  - **Modo Campaña:** 170 niveles base progresivos precargados en `app/src/main/assets/campaign/*.json` (Zip: 20, Patches: 30, Sudoku: 40, Tango: 30, Queens: 25, Sumbreak: 25).
  - **Auto-expansión de Campaña:** si completas todos los niveles disponibles de un juego, la app genera y guarda automáticamente **10 niveles adicionales** para continuar jugando sin límite.
  - **Partida Libre (Generador procedural):** niveles generados al vuelo con semilla determinista y **verificación de unicidad de solución** mediante *solvers* de *backtracking* y BFS. Las pantallas generadas se almacenan junto a la campaña.
- **Sistema inteligente de pistas en 2 pasos (estilo LinkedIn):**
  - Implementado en **Queens**, **Tango** y **Sumbreak**.
  - **Paso 1 (Visualización):** resalta las celdas implicadas (origen, objetivo, causas y unidades) y muestra una tarjeta explicativa sin alterar el tablero.
  - **Paso 2 (Aplicación):** un segundo toque aplica la deducción como una única entrada en el historial de Deshacer.
  - Prioridad de errores primero: detecta y avisa de discrepancias antes de deducir nuevos pasos.
- **Controles de partida y accesibilidad:**
  - Deshacer (`Undo`) y Rehacer con historial de instantáneas (`Snapshotted`).
  - Reiniciar tablero, temporizador con pausa automática en segundo plano y diálogo de instrucciones detallado con reglas y estrategias para cada juego.
  - Interacción táctil optimizada: gestos continuos de arrastre (*drag & drop*) y modo alternativo de selección por toque.
- **Progreso y racha diaria:**
  - Persistencia local reactiva mediante **Jetpack DataStore Preferences** (`CampaignProgress` y `Streak`).
- **Interfaz moderna:**
  - Desarrollada íntegramente en **Jetpack Compose** con **Material 3** (soporte para temas claro y oscuro) y componentes Canvas vectoriales para renderizado a 60/120 fps.

---

## 🏗️ Arquitectura y Estructura del Proyecto

El proyecto está dividido en dos módulos Gradle para separar el motor lógico puro de la capa Android:

```text
Puzzles/
├── engine/                          # Módulo Kotlin JVM puro (sin dependencias de Android)
│   └── src/
│       ├── main/kotlin/com/tuapp/puzzles/
│       │   ├── core/                # GameType, Difficulty, CampaignProgress, Streak, codec JSON
│       │   ├── engine/              # Reglas, Solvers, Generadores, Sesiones, Hints y LevelCodec
│       │   │   ├── tango/           # Reglas, Solver, Generador y TangoHintEngine
│       │   │   ├── queens/          # Reglas, Solver, Generador y QueensHintEngine
│       │   │   ├── sumbreak/        # Reglas, Solver BFS, Generador y SumbreakHintEngine
│       │   │   ├── sudoku/          # Reglas, Solver, Generador y candidatos
│       │   │   ├── zip/             # Reglas, Solver y Generador hamiltoniano
│       │   │   ├── patches/         # Reglas, Solver y Generador de particiones
│       │   │   └── session/         # Sesiones interactivas con historial de Undo/Redo
│       │   └── tools/               # GenerateCampaign.kt (CLI para generar los JSON de campaña)
│       └── test/                    # Tests unitarios JUnit de reglas, solvers, codecs, hints y sesiones
│
└── app/                             # Aplicación Android (Jetpack Compose + Hilt + DataStore)
    └── src/
        ├── main/
        │   ├── assets/campaign/     # 170 niveles JSON precargados (tango, queens, sumbreak, etc.)
        │   └── kotlin/com/tuapp/puzzles/
        │       ├── data/            # Repositorios (Campaña, Generador de niveles, DataStore)
        │       ├── di/              # Módulos de inyección de dependencias con Dagger Hilt
        │       └── ui/              # Navegación, pantallas (Home, Campaign, Game), tableros Compose
        │           └── boards/      # TangoBoard, QueensBoard, SumbreakBoard, ZipBoard, PatchesBoard, SudokuBoard
        ├── test/                    # Tests unitarios del módulo Android (ViewModel, Repositorios)
        └── androidTest/             # Tests instrumentados de UI (Compose), humo y benchmarks
```

---

## 🛠️ Stack Tecnológico

| Componente | Versión |
|---|---|
| **Lenguaje** | Kotlin `2.0.21` |
| **Android Gradle Plugin (AGP)** | `8.7.3` |
| **UI** | Jetpack Compose BOM `2024.10.01` + Material 3 |
| **Inyección de dependencias** | Dagger Hilt `2.52` (con KSP `2.0.21-1.0.28`) |
| **Navegación** | Navigation Compose `2.8.3` |
| **Persistencia** | Jetpack DataStore Preferences `1.1.1` |
| **SDK** | `minSdk 26` (Android 8.0) · `targetSdk 35` · `compileSdk 35` |
| **Java / JVM Toolchain** | Java `17` |

---

## 🚀 Compilación y Ejecución

### Requisitos previos
- **JDK 17** o superior.
- **Android SDK (API 35)** configurado en `local.properties` o mediante la variable de entorno `ANDROID_HOME`.

### Comandos principales

```bash
# 1. Ejecutar los tests unitarios del motor lógico (:engine) y de la app (:app)
./gradlew :engine:test :app:testDebugUnitTest

# 2. Compilar el APK de depuración
./gradlew :app:assembleDebug

# 3. Compilar el APK de release (optimizado y firmado para instalación directa)
./gradlew :app:assembleRelease

# 4. Regenerar los niveles JSON de campaña desde el motor
./gradlew :engine:generateCampaign
```

*(En Windows PowerShell usa `.\gradlew.bat` en lugar de `./gradlew`).*

Los APK generados se encuentran en:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/puzzles.fergavaz.apk`
