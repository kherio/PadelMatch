package com.kherio.padelmatch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kherio.padelmatch.data.TournamentFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(
    onBack: () -> Unit,
    onNext: (name: String, format: TournamentFormat, courts: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(TournamentFormat.AMERICANO) }
    var courts by remember { mutableStateOf(2) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Nuevo torneo") })
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del torneo") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Formato")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = format == TournamentFormat.AMERICANO,
                    onClick = { format = TournamentFormat.AMERICANO },
                    label = { Text("Americano") }
                )
                FilterChip(
                    selected = format == TournamentFormat.MEXICANO,
                    onClick = { format = TournamentFormat.MEXICANO },
                    label = { Text("Mexicano") }
                )
            }

            Text("Número de pistas: $courts")
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Button(onClick = { if (courts > 1) courts-- }) { Text("-") }
                Spacer(Modifier.width(16.dp))
                Button(onClick = { courts++ }) { Text("+") }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { onNext(name.ifBlank { "Torneo de pádel" }, format, courts) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Siguiente: añadir jugadores")
            }
        }
    }
}
