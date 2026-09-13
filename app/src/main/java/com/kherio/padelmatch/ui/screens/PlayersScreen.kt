package com.kherio.padelmatch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kherio.padelmatch.data.Player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    onBack: () -> Unit,
    onStart: (List<Player>) -> Unit
) {
    var players by remember { mutableStateOf<List<Player>>(emptyList()) }
    var input by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Jugadores (${players.size})") })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Nombre del jugador") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (input.isNotBlank()) {
                        players = players + Player(name = input.trim())
                        input = ""
                    }
                }) { Text("Añadir") }
            }

            Spacer(Modifier.height(8.dp))
            Text("Se necesitan al menos 4 jugadores (múltiplos de 4 ideal).", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))

            LazyColumn(Modifier.weight(1f)) {
                items(players, key = { it.id }) { p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(p.name)
                        IconButton(onClick = { players = players - p }) {
                            Icon(Icons.Default.Close, contentDescription = "Quitar")
                        }
                    }
                    Divider()
                }
            }

            Button(
                onClick = { onStart(players) },
                enabled = players.size >= 4,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generar primera ronda")
            }
        }
    }
}
