package com.tuapp.puzzles.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.ui.campaign.CampaignScreen
import com.tuapp.puzzles.ui.game.GameScreen
import com.tuapp.puzzles.ui.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val CAMPAIGN = "campaign/{game}"
    /** levelId -> nivel de campaña; seed+difficulty -> regenerar; solo difficulty -> generar uno nuevo. */
    const val PLAY = "play/{game}?levelId={levelId}&seed={seed}&difficulty={difficulty}"

    fun campaign(g: GameType) = "campaign/${g.id}"
    fun playLevel(g: GameType, id: String) = "play/${g.id}?levelId=$id"
    fun playNew(g: GameType, difficulty: Int) = "play/${g.id}?difficulty=$difficulty"
    fun playSeed(g: GameType, seed: Long, difficulty: Int) = "play/${g.id}?seed=$seed&difficulty=$difficulty"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onCampaign = { nav.navigate(Routes.campaign(it)) },
                onNew = { game, difficulty -> nav.navigate(Routes.playNew(game, difficulty)) },
            )
        }
        composable(
            Routes.CAMPAIGN,
            arguments = listOf(navArgument("game") { type = NavType.StringType }),
        ) {
            CampaignScreen(
                onBack = { nav.popBackStack() },
                onPlay = { game, id -> nav.navigate(Routes.playLevel(game, id)) },
            )
        }
        composable(
            Routes.PLAY,
            arguments = listOf(
                navArgument("game") { type = NavType.StringType },
                navArgument("levelId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("seed") { type = NavType.LongType; defaultValue = -1L },
                navArgument("difficulty") { type = NavType.IntType; defaultValue = 3 },
            ),
        ) {
            GameScreen(
                onBack = { nav.popBackStack() },
                // "Siguiente": reemplaza la pantalla de juego actual
                onNavigate = { route -> nav.navigate(route) { popUpTo(Routes.PLAY) { inclusive = true } } },
            )
        }
    }
}
