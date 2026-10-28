package com.tuapp.puzzles.ui.instructions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.ui.emoji

data class InstructionData(
    val title: String,
    val description: String,
    val objective: String,
    val rules: List<String>,
    val strategy: String,
)

fun getInstructions(game: GameType): InstructionData = when (game) {
    GameType.TANGO -> InstructionData(
        title = "Cómo jugar a Tango",
        description = "Tango es un puzle de lógica binaria donde debes llenar una cuadrícula con dos símbolos diferentes, generalmente un sol y una luna, siguiendo un conjunto de reglas estrictas.",
        objective = "Completar toda la cuadrícula con los dos símbolos disponibles.",
        rules = listOf(
            "No puede haber más de dos símbolos iguales consecutivos en ninguna fila o columna.",
            "Cada fila y cada columna debe tener el mismo número de soles y lunas.",
            "No puede haber dos filas o dos columnas idénticas.",
            "Algunas casillas pueden estar unidas por un signo = (deben tener el mismo icono) o × (deben tener iconos diferentes).",
        ),
        strategy = "Busca filas o columnas que estén a punto de completarse o donde colocar un símbolo te obligue a colocar varios más para cumplir las reglas. Presta especial atención a los pares de símbolos para evitar tríos.",
    )
    GameType.ZIP -> InstructionData(
        title = "Cómo jugar a Zip",
        description = "Zip es un rompecabezas numérico en el que debes conectar todos los números en una cuadrícula formando un único camino continuo.",
        objective = "Trazar una sola línea que conecte los números en orden ascendente, pasando por todas las casillas vacías de la cuadrícula.",
        rules = listOf(
            "Comienza en la casilla con el número 1.",
            "Dibuja una línea continua para conectar los números en secuencia (1, 2, 3, etc.).",
            "El camino debe pasar por todas las casillas del tablero una sola vez.",
            "En algunos tableros, puede haber paredes que bloquean el paso entre casillas.",
        ),
        strategy = "Planifica tu ruta con antelación. Fíjate en los números más altos para anticipar por dónde deberá terminar el camino y evita encerrar áreas del tablero que luego no puedas rellenar.",
    )
    GameType.SUDOKU -> InstructionData(
        title = "Cómo jugar a Mini Sudoku",
        description = "Mini Sudoku es una versión compacta y rápida del clásico juego de lógica numérica. Se juega en una cuadrícula más pequeña, lo que lo hace ideal para una partida rápida.",
        objective = "Rellenar una cuadrícula de 6x6 con los números del 1 al 6.",
        rules = listOf(
            "Cada fila debe contener todos los números del 1 al 6, sin repeticiones.",
            "Cada columna debe contener todos los números del 1 al 6, sin repeticiones.",
            "Cada caja de 2x3 (delimitada por una línea más gruesa) debe contener todos los números del 1 al 6, sin repeticiones.",
        ),
        strategy = "Utiliza el método de \"escanear\" filas, columnas y cajas para identificar qué números faltan. Busca las casillas donde solo puede ir un número posible para empezar a resolver el puzle.",
    )
    GameType.QUEENS -> InstructionData(
        title = "Cómo jugar a Queens",
        description = "Queens es un juego de lógica espacial donde debes colocar reinas (coronas) en el tablero respetando las regiones de color y las reglas del ajedrez.",
        objective = "Colocar exactamente una corona en cada fila, cada columna y cada región de color del tablero.",
        rules = listOf(
            "No puede haber más de una corona en la misma fila.",
            "No puede haber más de una corona en la misma columna.",
            "No puede haber más de una corona en la misma región de color.",
            "Dos coronas no pueden tocarse nunca, ni siquiera en diagonal (ninguna casilla colindante).",
            "Toca una casilla para marcarla con \"✕\" donde no pueda ir una corona, o arrastra el dedo para marcar varias casillas seguidas.",
        ),
        strategy = "Empieza por las regiones más pequeñas o estrechas, donde la posición de la corona esté más restringida. Marca con ✕ las casillas adyacentes a cada corona para despejar el tablero. Puedes activar la opción de auto-marcar con X para agilizar la partida.",
    )
    GameType.PATCHES -> InstructionData(
        title = "Cómo jugar a Patches",
        description = "Patches es un puzle geométrico donde debes dividir y cubrir todo el tablero con parches rectangulares según las pistas dadas.",
        objective = "Cubrir todo el tablero con rectángulos que cumplan exactamente las pistas numéricas y de forma.",
        rules = listOf(
            "Cada parche debe contener exactamente una pista en su interior o bordes.",
            "Los parches no pueden solaparse ni dejar casillas vacías en el tablero.",
            "Si la pista indica un número, el parche debe tener exactamente esa área (número de casillas). Un parche de 1x1 se coloca con un solo toque.",
            "Símbolos de forma: ■ debe ser cuadrado; ↔ más ancho que alto; ↕ más alto que ancho; ┅ cualquier proporción rectangular libre.",
            "Pulsando dos veces sobre un parche colocado se elimina. Arrastrando desde cualquiera de sus casillas puedes cambiarle el tamaño.",
        ),
        strategy = "Las casillas de pista ya tienen el color de su parche asignado desde el principio. Empieza dibujando los parches cuyas dimensiones estén forzadas por las esquinas o bordes del tablero.",
    )
}

@Composable
fun GameInstructionsDialog(
    game: GameType,
    onDismiss: () -> Unit,
) {
    val data = getInstructions(game)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(game.emoji, fontSize = 24.sp)
                Text(data.title, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(data.description, style = MaterialTheme.typography.bodyMedium)

                Text("Objetivo:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(data.objective, style = MaterialTheme.typography.bodyMedium)

                Text("Reglas principales:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    data.rules.forEach { rule ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", fontWeight = FontWeight.Bold)
                            Text(rule, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Text("Estrategia:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(data.strategy, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Entendido") }
        },
    )
}
