package com.kherio.padelmatch.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kherio.padelmatch.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentScreen(
    tournamentId: String,
    repository: TournamentRepository,
    onBack: () -> Unit,
    onTournamentFinished: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tournament by remember { mutableStateOf<Tournament?>(null) }
    var tab by remember { mutableStateOf(0) } // 0 = ronda actual, 1 = clasificación
    var showFinishDialog by remember { mutableStateOf(false) }

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

    /** Aplica los resultados pendientes de la ronda actual (por si aún no se
     * ha pasado por "Siguiente ronda") antes de cerrar el torneo, para que
     * ningún resultado puntuado se quede fuera de la clasificación final.
     * Espera a que el guardado termine antes de navegar al podio, para que
     * no se lea el dato antiguo por una carrera con el guardado asíncrono. */
    fun applyPendingResultsAndFinish() {
        var updated = t
        val lastRound = t.rounds.lastOrNull()
        if (lastRound != null) {
            lastRound.matches.filter { it.isFinished }.forEach { m ->
                updated = PairingEngine.applyResult(updated, m)
            }
        }
        scope.launch {
            if (updated != t) {
                tournament = updated
                repository.save(updated)
            }
            onTournamentFinished(t.id)
        }
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

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("¿Finalizar torneo?") },
            text = { Text("Se cerrará la clasificación actual y se mostrará el podio final. Podrás seguir viendo los resultados después.") },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    applyPendingResultsAndFinish()
                }) { Text("Finalizar") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t.name, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showFinishDialog = true }) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = "Finalizar torneo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .imePadding()
        ) {
            SegmentedTabs(
                selected = tab,
                onSelect = { tab = it },
                labels = listOf("Ronda actual", "Clasificación")
            )

            if (tab == 0) {
                val currentRound = t.rounds.lastOrNull()
                val roundRobinDone = currentRound?.let { PairingEngine.isRoundRobinComplete(t, it) } ?: false
                if (currentRound == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyColumn(
                        Modifier.weight(1f).padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                AnimatedContent(
                                    targetState = currentRound.number,
                                    transitionSpec = {
                                        (slideInHorizontally { it } + fadeIn()) togetherWith
                                            (slideOutHorizontally { -it } + fadeOut())
                                    },
                                    label = "roundNumber"
                                ) { number ->
                                    Text(
                                        "Ronda $number",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "A ${t.pointsTarget} puntos",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        itemsIndexed(currentRound.matches, key = { _, m -> m.id }) { index, match ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(300, delayMillis = index * 60)) +
                                    slideInVertically(tween(300, delayMillis = index * 60)) { it / 4 }
                            ) {
                                MatchCard(match, t.players, t.pointsTarget) { s1, s2 -> updateScore(match, s1, s2) }
                            }
                        }
                        if (currentRound.sittingOutPlayerIds.isNotEmpty()) {
                            item {
                                val names = currentRound.sittingOutPlayerIds
                                    .mapNotNull { id -> t.players.find { it.id == id }?.name }
                                    .joinToString(", ")
                                AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            "Descansan: $names",
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                    if (roundRobinDone) {
                        AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "¡Todos han jugado con todos y contra todos! Ya puedes finalizar el torneo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { shareStandings() },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Compartir")
                        }
                        if (roundRobinDone) {
                            Button(
                                onClick = { showFinishDialog = true },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Finalizar torneo", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = { finishRoundAndGenerateNext() },
                                enabled = currentRound.matches.all { it.isFinished },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Text("Siguiente ronda", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.NavigateNext, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            } else {
                val ranking = t.players.sortedByDescending { it.totalPoints }
                LazyColumn(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(ranking, key = { _, p -> p.id }) { index, p ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(250, delayMillis = index * 50)) +
                                slideInHorizontally(tween(250, delayMillis = index * 50)) { it / 6 }
                        ) {
                            RankingRow(position = index + 1, player = p)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedTabs(selected: Int, onSelect: (Int) -> Unit, labels: List<String>) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                label = "tabBg"
            )
            val fgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tabFg"
            )
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .clickableNoRipple { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.SemiBold,
                    color = fgColor
                )
            }
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

private fun generateFirstRound(t: Tournament): Tournament? {
    if (t.rounds.isNotEmpty()) return null
    val round = PairingEngine.nextRound(t)
    return t.copy(rounds = listOf(round))
}

@Composable
private fun MatchCard(match: MatchResult, players: List<Player>, pointsTarget: Int, onScoreChange: (Int?, Int?) -> Unit) {
    fun nameOf(id: String) = players.find { it.id == id }?.name ?: "?"

    var s1 by remember(match.id) { mutableStateOf(match.score1?.toString() ?: "") }
    var s2 by remember(match.id) { mutableStateOf(match.score2?.toString() ?: "") }

    val team1Winning = (s1.toIntOrNull() ?: -1) > (s2.toIntOrNull() ?: -1)
    val team2Winning = (s2.toIntOrNull() ?: -1) > (s1.toIntOrNull() ?: -1)
    val maxDigits = pointsTarget.toString().length

    // Al escribir el marcador de un equipo, se autocompleta el del otro para
    // que la suma siempre sea igual a la puntuación objetivo del torneo.
    fun onTeam1Change(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(maxDigits)
        val value = digits.toIntOrNull()
        s1 = if (value != null && value > pointsTarget) pointsTarget.toString() else digits
        val v = s1.toIntOrNull()
        s2 = if (v != null) (pointsTarget - v).coerceAtLeast(0).toString() else s2
        onScoreChange(s1.toIntOrNull(), s2.toIntOrNull())
    }

    fun onTeam2Change(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(maxDigits)
        val value = digits.toIntOrNull()
        s2 = if (value != null && value > pointsTarget) pointsTarget.toString() else digits
        val v = s2.toIntOrNull()
        s1 = if (v != null) (pointsTarget - v).coerceAtLeast(0).toString() else s1
        onScoreChange(s1.toIntOrNull(), s2.toIntOrNull())
    }

    val cardElevation by animateDpAsState(
        targetValue = if (team1Winning || team2Winning) 6.dp else 2.dp,
        label = "matchCardElevation"
    )

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "Pista ${match.court}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamNames(
                    name1 = nameOf(match.team1.player1Id),
                    name2 = nameOf(match.team1.player2Id),
                    highlighted = team1Winning,
                    modifier = Modifier.weight(1f)
                )
                ScoreField(value = s1, highlighted = team1Winning, maxDigits = maxDigits, onChange = ::onTeam1Change)
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "VS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamNames(
                    name1 = nameOf(match.team2.player1Id),
                    name2 = nameOf(match.team2.player2Id),
                    highlighted = team2Winning,
                    modifier = Modifier.weight(1f)
                )
                ScoreField(value = s2, highlighted = team2Winning, maxDigits = maxDigits, onChange = ::onTeam2Change)
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "Suma: ${(s1.toIntOrNull() ?: 0) + (s2.toIntOrNull() ?: 0)} / $pointsTarget",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun TeamNames(name1: String, name2: String, highlighted: Boolean, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        targetValue = if (highlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
        label = "teamNameColor"
    )
    Column(modifier) {
        Text(
            "$name1 / $name2",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

@Composable
private fun ScoreField(value: String, highlighted: Boolean, maxDigits: Int, onChange: (String) -> Unit) {
    val borderColor by animateColorAsState(
        targetValue = if (highlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
        label = "scoreFieldBorder"
    )
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(maxDigits)) },
        modifier = Modifier.width(68.dp),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = borderColor
        )
    )
}

@Composable
private fun RankingRow(position: Int, player: Player) {
    val medalColor = when (position) {
        1 -> androidx.compose.ui.graphics.Color(0xFFFFD700)
        2 -> androidx.compose.ui.graphics.Color(0xFFC0C0C0)
        3 -> androidx.compose.ui.graphics.Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val medalTextColor = if (position <= 3) androidx.compose.ui.graphics.Color.Black else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (position <= 3) 3.dp else 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(medalColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (position <= 3) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = medalTextColor, modifier = Modifier.size(18.dp))
                    } else {
                        Text("$position", fontWeight = FontWeight.Bold, color = medalTextColor, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(player.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${player.totalPoints} pts", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${player.gamesPlayed} PJ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
