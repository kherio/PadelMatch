package com.kherio.padelmatch.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kherio.padelmatch.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentScreen(
    tournamentId: String,
    repository: TournamentRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tournament by remember { mutableStateOf<Tournament?>(null) }
    var tab by remember { mutableStateOf(0) } // 0 = ronda actual, 1 = clasificación

    LaunchedEffect(tournamentId) {
        tournament = repository.getById(tournamentId)
        if (tournament?.rounds?.isEmpty() == true) {
            generateFirstRound(tournament!!)?.let {
                tournament = it
                repository.save(it)
            }
        }
    }

    val t = tournament ?: return

    fun persist(updated: Tournament) {
        tournament = updated
        scope.launch { repository.save(updated) }
    }

    fun updateScore(match: MatchResult, s1: Int?, s2: Int?) {
        val currentRound = t.rounds.last()
        val updatedMatches = currentRound.matches.map {
            if (it.id == match.id) it.copy(score1 = s1, score2 = s2) else it
        }
        val updatedRounds = t.rounds.dropLast(1) + currentRound.copy(matches = updatedMatches)
        persist(t.copy(rounds = updatedRounds))
    }

    fun finishRoundAndGenerateNext() {
        var updated = t
        val currentRound = updated.rounds.last()
        currentRound.matches.filter { it.isFinished }.forEach { m ->
            updated = PairingEngine.applyResult(updated, m)
        }
        val next = PairingEngine.nextRound(updated)
        updated = updated.copy(rounds = updated.rounds.dropLast(1) + currentRound + next)
        persist(updated)
    }

    fun shareStandings() {
        val ranking = t.players.sortedByDescending { it.totalPoints }
            .mapIndexed { i, p -> "${i + 1}. ${p.name} — ${p.totalPoints} pts" }
            .joinToString("\n")
        val text = "🎾 ${t.name}\nRonda ${t.rounds.size}\n\nClasificación:\n$ranking"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir clasificación"))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(t.name) }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Ronda actual") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Clasificación") })
            }

            if (tab == 0) {
                val currentRound = t.rounds.lastOrNull()
                if (currentRound == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Generando ronda...") }
                } else {
                    LazyColumn(
                        Modifier.weight(1f).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Text("Ronda ${currentRound.number}", fontWeight = FontWeight.Bold) }
                        items(currentRound.matches, key = { it.id }) { match ->
                            MatchCard(match, t.players) { s1, s2 -> updateScore(match, s1, s2) }
                        }
                        if (currentRound.sittingOutPlayerIds.isNotEmpty()) {
                            item {
                                val names = currentRound.sittingOutPlayerIds
                                    .mapNotNull { id -> t.players.find { it.id == id }?.name }
                                    .joinToString(", ")
                                Text("Descansan: $names", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { shareStandings() }, modifier = Modifier.weight(1f)) {
                            Text("Compartir")
                        }
                        Button(
                            onClick = { finishRoundAndGenerateNext() },
                            enabled = currentRound.matches.all { it.isFinished },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Siguiente ronda")
                        }
                    }
                }
            } else {
                val ranking = t.players.sortedByDescending { it.totalPoints }
                LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                    itemsIndexed(ranking) { index, p ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${index + 1}. ${p.name}")
                            Text("${p.totalPoints} pts · ${p.gamesPlayed} PJ")
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

private fun generateFirstRound(t: Tournament): Tournament? {
    if (t.rounds.isNotEmpty()) return null
    val round = PairingEngine.nextRound(t)
    return t.copy(rounds = listOf(round))
}

@Composable
private fun MatchCard(match: MatchResult, players: List<Player>, onScoreChange: (Int?, Int?) -> Unit) {
    fun nameOf(id: String) = players.find { it.id == id }?.name ?: "?"

    var s1 by remember(match.id) { mutableStateOf(match.score1?.toString() ?: "") }
    var s2 by remember(match.id) { mutableStateOf(match.score2?.toString() ?: "") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Pista ${match.court}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${nameOf(match.team1.player1Id)} / ${nameOf(match.team1.player2Id)}", Modifier.weight(1f))
                OutlinedTextField(
                    value = s1,
                    onValueChange = { s1 = it.filter { c -> c.isDigit() }; onScoreChange(s1.toIntOrNull(), s2.toIntOrNull()) },
                    modifier = Modifier.width(64.dp),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${nameOf(match.team2.player1Id)} / ${nameOf(match.team2.player2Id)}", Modifier.weight(1f))
                OutlinedTextField(
                    value = s2,
                    onValueChange = { s2 = it.filter { c -> c.isDigit() }; onScoreChange(s1.toIntOrNull(), s2.toIntOrNull()) },
                    modifier = Modifier.width(64.dp),
                    singleLine = true
                )
            }
        }
    }
}


