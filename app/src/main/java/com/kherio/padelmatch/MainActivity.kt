package com.kherio.padelmatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kherio.padelmatch.data.Player
import com.kherio.padelmatch.data.PlayerRosterRepository
import com.kherio.padelmatch.data.Tournament
import com.kherio.padelmatch.data.TournamentFormat
import com.kherio.padelmatch.data.TournamentRepository
import com.kherio.padelmatch.ui.screens.CreateTournamentScreen
import com.kherio.padelmatch.ui.screens.HomeScreen
import com.kherio.padelmatch.ui.screens.PlayersScreen
import com.kherio.padelmatch.ui.screens.PodiumScreen
import com.kherio.padelmatch.ui.screens.SplashScreen
import com.kherio.padelmatch.ui.screens.TournamentScreen
import com.kherio.padelmatch.ui.screens.UpdatesScreen
import com.kherio.padelmatch.ui.theme.PadelMatchTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: TournamentRepository
    private lateinit var rosterRepository: PlayerRosterRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = TournamentRepository(this)
        rosterRepository = PlayerRosterRepository(this)

        setContent {
            PadelMatchTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(repository, rosterRepository)
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(repository: TournamentRepository, rosterRepository: PlayerRosterRepository) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var pendingName by remember { mutableStateOf("") }
    var pendingFormat by remember { mutableStateOf(TournamentFormat.AMERICANO) }
    var pendingCourts by remember { mutableStateOf(2) }
    var pendingPointsTarget by remember { mutableStateOf(21) }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onFinished = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                repository = repository,
                onOpenTournament = { id -> navController.navigate("tournament/$id") },
                onCreateNew = { navController.navigate("create") },
                onOpenUpdates = { navController.navigate("updates") }
            )
        }

        composable("updates") {
            UpdatesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("create") {
            CreateTournamentScreen(
                onBack = { navController.popBackStack() },
                onNext = { name, format, courts, pointsTarget ->
                    pendingName = name
                    pendingFormat = format
                    pendingCourts = courts
                    pendingPointsTarget = pointsTarget
                    navController.navigate("players")
                }
            )
        }

        composable("players") {
            PlayersScreen(
                rosterRepository = rosterRepository,
                onBack = { navController.popBackStack() },
                onStart = { players: List<Player> ->
                    val tournament = Tournament(
                        name = pendingName,
                        format = pendingFormat,
                        courts = pendingCourts,
                        pointsTarget = pendingPointsTarget,
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
                onBack = { navController.popBackStack() },
                onTournamentFinished = { finishedId ->
                    navController.navigate("podium/$finishedId") {
                        popUpTo("home")
                    }
                }
            )
        }

        composable(
            "podium/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            PodiumScreen(
                tournamentId = id,
                repository = repository,
                onBackToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
