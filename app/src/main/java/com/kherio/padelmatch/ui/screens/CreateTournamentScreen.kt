package com.kherio.padelmatch.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kherio.padelmatch.data.TournamentFormat

private val POINT_OPTIONS = listOf(14, 21, 24, 31)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(
    onBack: () -> Unit,
    onNext: (name: String, format: TournamentFormat, courts: Int, pointsTarget: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(TournamentFormat.AMERICANO) }
    var courts by remember { mutableStateOf(2) }
    var pointsTarget by remember { mutableStateOf(21) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Nuevo torneo", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del torneo") },
                placeholder = { Text("Ej: Torneo de verano") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text("Formato", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))

                FormatOptionCard(
                    title = "Americano",
                    subtitle = "Parejas rotativas cada ronda",
                    selected = format == TournamentFormat.AMERICANO,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { format = TournamentFormat.AMERICANO }
                )
                Spacer(Modifier.height(10.dp))
                FormatOptionCard(
                    title = "Americano competitivo",
                    subtitle = "Desde la ronda 2, las parejas se generan según la clasificación",
                    selected = format == TournamentFormat.AMERICANO_COMPETITIVO,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { format = TournamentFormat.AMERICANO_COMPETITIVO }
                )
                Spacer(Modifier.height(10.dp))
                FormatOptionCard(
                    title = "Mexicano",
                    subtitle = "Parejas por clasificación",
                    selected = format == TournamentFormat.MEXICANO,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { format = TournamentFormat.MEXICANO }
                )
            }

            Column {
                Text("¿A cuántos puntos se juegan los partidos?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    POINT_OPTIONS.forEach { points ->
                        PointsChip(
                            points = points,
                            selected = pointsTarget == points,
                            modifier = Modifier.weight(1f),
                            onClick = { pointsTarget = points }
                        )
                    }
                }
            }

            Column {
                Text("Número de pistas", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoundIconButton(icon = Icons.Default.Remove, enabled = courts > 1) {
                            if (courts > 1) courts--
                        }
                        Text(
                            "$courts",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        RoundIconButton(icon = Icons.Default.Add, enabled = true) { courts++ }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Hasta ${courts * 4} jugadores jugando a la vez",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { onNext(name.ifBlank { "Torneo de pádel" }, format, courts, pointsTarget) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Siguiente: añadir jugadores", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun PointsChip(points: Int, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "pointsChipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pointsChipFg"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 0.dp,
        animationSpec = spring(),
        label = "pointsChipBorder"
    )

    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(borderWidth, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("$points", fontWeight = FontWeight.Bold, color = contentColor, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun FormatOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "formatCardBg"
    )
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val borderWidth by animateDpAsState(targetValue = if (selected) 2.dp else 1.dp, label = "formatCardBorder")

    Card(
        modifier = modifier.border(borderWidth, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SportsTennis, contentDescription = null, tint = contentColor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = contentColor)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = contentColor)
            }
        }
    }
}

@Composable
private fun RoundIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val fg = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, contentDescription = null, tint = fg)
        }
    }
}
