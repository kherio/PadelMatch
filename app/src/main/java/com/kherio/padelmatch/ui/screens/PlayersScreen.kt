package com.kherio.padelmatch.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kherio.padelmatch.data.Player
import com.kherio.padelmatch.data.PlayerRosterRepository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PlayersScreen(
    onBack: () -> Unit,
    onStart: (List<Player>) -> Unit,
    rosterRepository: PlayerRosterRepository
) {
    var players by remember { mutableStateOf<List<Player>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var roster by remember { mutableStateOf(rosterRepository.getAll()) }
    var editRoster by remember { mutableStateOf(false) }

    fun addPlayer(name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty() && players.none { it.name.equals(clean, ignoreCase = true) }) {
            players = players + Player(name = clean)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Jugadores (${players.size})", fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Nombre del jugador") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            addPlayer(input)
                            roster = rosterRepository.add(input)
                            input = ""
                        }
                    },
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Añadir jugador")
                }
            }

            Spacer(Modifier.height(10.dp))
            val ready = players.size >= 4
            Text(
                if (ready) "¡Listo! Puedes generar la primera ronda cuando quieras."
                else "Añade al menos 4 jugadores (idealmente múltiplos de 4).",
                style = MaterialTheme.typography.bodySmall,
                color = if (ready) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (roster.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Jugadores guardados",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row {
                        if (!editRoster) {
                            TextButton(onClick = { roster.forEach { addPlayer(it) } }) {
                                Text("Añadir todos")
                            }
                        }
                        TextButton(onClick = { editRoster = !editRoster }) {
                            Text(if (editRoster) "Listo" else "Editar")
                        }
                    }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        roster.forEach { name ->
                            val inTournament = players.any { it.name.equals(name, ignoreCase = true) }
                            if (editRoster) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        roster = rosterRepository.remove(name)
                                        if (roster.isEmpty()) editRoster = false
                                    },
                                    label = { Text(name) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Quitar de la lista",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            } else {
                                FilterChip(
                                    selected = inTournament,
                                    onClick = {
                                        if (inTournament) {
                                            players = players.filterNot { it.name.equals(name, ignoreCase = true) }
                                        } else {
                                            addPlayer(name)
                                        }
                                    },
                                    label = { Text(name) },
                                    leadingIcon = if (inTournament) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(players, key = { _, p -> p.id }) { index, p ->
                    PlayerRow(
                        number = index + 1,
                        player = p,
                        onRemove = { players = players - p },
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }

            Button(
                onClick = { onStart(players) },
                enabled = players.size >= 4,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Generar primera ronda", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PlayerRow(number: Int, player: Player, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$number",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(player.name, style = MaterialTheme.typography.bodyLarge)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
