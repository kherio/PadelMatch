package com.kherio.padelmatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kherio.padelmatch.data.Player
import com.kherio.padelmatch.data.Tournament
import com.kherio.padelmatch.data.TournamentFormat
import com.kherio.padelmatch.data.TournamentRepository
import com.kherio.padelmatch.ui.screens.CreateTournamentScreen
import com.kherio.padelmatch.ui.screens.HomeScreen
import com.kherio.padelmatch.ui.screens.PlayersScreen
import com.kherio.padelmatch.ui.screens.TournamentScreen
import com.kherio.padelmatch.ui.theme.PadelMatchTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: TournamentRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = TournamentRepository(this)

        setContent {
            PadelMatchTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(repository)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AppNavHost(repository: TournamentRepository) {
    val navController = rememberNavController()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Estado temporal mientras se crea un torneo nuevo (nombre/formato/pistas -> jugadores)
    var pendingName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var pendingFormat by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(TournamentFormat.AMERICANO) }
    var pendingCourts by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(2) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                repository = repository,
                onOpenTournament = { id -> navController.navigate("tournament/$id") },
                onCreateNew = { navController.navigate("create") }
            )
        }
        composable("create") {
            CreateTournamentScreen(
                onBack = { navController.popBackStack() },
                onNext = { name, format, courts ->
                    pendingName = name
                    pendingFormat = format
                    pendingCourts = courts
                    navController.navigate("players")
                }
            )
        }
        composable("players") {
            PlayersScreen(
                onBack = { navController.popBackStack() },
                onStart = { players: List<Player> ->
                    val tournament = Tournament(
                        name = pendingName,
                        format = pendingFormat,
                        courts = pendingCourts,
                        players = players
                    )
                    scope.launch {
                        repository.save(tournament)
                        navController.navigate("tournament/${tournament.id}") {
                            popUpTo("home")
                        }
                    }
                }
            )
        }
        composable(
            "tournament/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            TournamentScreen(
                tournamentId = id,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
