# 🧩 Puzzles — Colección de Juegos de Lógica para Android

Aplicación nativa para Android que reúne **5 rompecabezas de lógica** clásicos y modernos, con modo campaña precargado, generación procedural infinita con **solución única garantizada**, sistema de rachas diarias y pistas inteligentes. Funciona **100 % offline**, sin anuncios ni permisos de red.

---

## 🎮 Los 5 Juegos Incluidos

| Juego | Descripción | Tablero / Reglas clave |
|---|---|---|
| ☀️ **Tango** | Lógica binaria con soles y lunas | Mismo número de soles y lunas por fila/columna, máximo 2 iguales consecutivos, sin filas/columnas idénticas y restricciones `=` / `×` entre celdas adyacentes. |
| 👑 **Queens** | Reinas por regiones de color | Exactamente 1 corona por fila, columna y región de color, sin que dos coronas se toquen ni siquiera en diagonal. Soporte para arrastrar marcas `✕` y auto-marcado. |
| 🔢 **Mini Sudoku** | Sudoku rápido de formato compacto | Cuadrícula `6×6` dividida en bloques de `2×3` con números del `1` al `6` y modo de notas (candidatos). |
| 🔗 **Zip** | Camino hamiltoniano secuencial | Conecta los números en orden ascendente (`1 → 2 → 3…`) visitando **todas** las celdas del tablero una sola vez y respetando los muros entre casillas. |
| 🟦 **Patches** | Partición geométrica (estilo Shikaku) | Cubre todo el tablero con rectángulos sin solaparse según las pistas de área y proporción (`■` cuadrado, `↔` horizontal, `↕` vertical, `┅` libre). |

---

## ✨ Características Principales

- **Dos modos de juego:**
  - **Modo Campaña:** 15 niveles progresivos por juego (5 Fácil, 5 Medio, 5 Difícil) precargados en `app/src/main/assets/campaign/*.json`.
  - **Partida Libre (Generador procedural):** niveles infinitos generados al vuelo con semilla determinista y **verificación de unicidad de solución** mediante *solvers* de *backtracking*.
- **Ayudas y controles de partida:**
  - Deshacer (`Undo`) con historial de instantáneas (`Snapshotted`), reiniciar tablero, pistas contextuales (`Hint`), temporizador y diálogo de instrucciones integrado en cada juego.
- **Progreso y racha diaria:**
  - Persistencia local reactiva mediante **Jetpack DataStore** (`CampaignProgress` y `Streak`).
- **Interfaz moderna:**
  - Desarrollada íntegramente en **Jetpack Compose** con **Material 3** (soporte para tema claro y oscuro) y gráficos vectoriales/Canvas a medida para cada tablero.

---

## 🏗️ Arquitectura y Estructura del Proyecto

El proyecto está dividido en dos módulos Gradle para separar el motor lógico puro de la capa Android:

```text
Puzzles/
├── engine/                          # Módulo Kotlin JVM puro (sin dependencias de Android)
│   └── src/
│       ├── main/kotlin/com/tuapp/puzzles/
│       │   ├── core/                # GameType, Difficulty, CampaignProgress, Streak, codec JSON
│       │   ├── engine/              # Reglas, Solvers, Generadores, Sesiones y LevelCodec
│       │   │   ├── tango/
│       │   │   ├── queens/
│       │   │   ├── sudoku/
│       │   │   ├── zip/
│       │   │   ├── patches/
│       │   │   └── session/         # Máquinas de estado inmutables con soporte de Undo
│       │   └── tools/               # GenerateCampaign.kt (CLI para regenerar los JSON de campaña)
│       └── test/                    # Tests unitarios JUnit 5 de reglas, solvers, codecs y sesiones
│
└── app/                             # Aplicación Android (Jetpack Compose + Hilt + DataStore)
    └── src/
        ├── main/
        │   ├── assets/campaign/     # Niveles precargados (tango, queens, sudoku, zip, patches)
        │   └── kotlin/com/tuapp/puzzles/
        │       ├── data/            # Repositorios (Campaña, Generador de niveles, DataStore)
        │       ├── di/              # Módulos de inyección de dependencias con Dagger Hilt
        │       └── ui/              # Navegación, pantallas (Home, Campaign, Game), tableros e instrucciones
        ├── test/                    # Tests unitarios del módulo Android
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

### Comandos principales (desde la raíz del repositorio)

```bash
# 1. Ejecutar los tests unitarios del motor lógico (:engine) y de la app (:app)
./gradlew :engine:test :app:testDebugUnitTest

# 2. Compilar el APK de depuración
./gradlew :app:assembleDebug

# 3. Compilar el APK de release (firmado con configuración debug para instalación directa)
./gradlew :app:assembleRelease

# 4. Regenerar los niveles JSON de campaña desde el motor
./gradlew :engine:generateCampaign
```

*(En Windows PowerShell usa `.\gradlew.bat` en lugar de `./gradlew`).*

Los APK generados se encuentran en:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`
