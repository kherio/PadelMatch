package com.kherio.padelmatch.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kherio.padelmatch.R
import com.kherio.padelmatch.data.Tournament
import com.kherio.padelmatch.data.TournamentRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: TournamentRepository,
    onOpenTournament: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var tournaments by remember { mutableStateOf<List<Tournament>>(emptyList()) }

    LaunchedEffect(Unit) {
        tournaments = repository.getAll()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_artaza),
                        contentDescription = "Artaza Torresolo Pádel Club",
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Artaza Torresolo")
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNew) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo torneo")
            }
        }
    ) { padding ->
        if (tournaments.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aún no tienes torneos. Pulsa + para crear el primero.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tournaments, key = { it.id }) { t ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenTournament(t.id) }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(t.name, fontWeight = FontWeight.Bold)
                                Text("${t.format.name.lowercase().replaceFirstChar { it.uppercase() }} · ${t.players.size} jugadores · Ronda ${t.rounds.size}")
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    repository.delete(t.id)
                                    tournaments = repository.getAll()
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                            }
                        }
                    }
                }
            }
        }
    }
}
