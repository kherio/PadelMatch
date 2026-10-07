package com.kherio.padelmatch.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kherio.padelmatch.R
import com.kherio.padelmatch.data.Player
import com.kherio.padelmatch.data.Tournament
import com.kherio.padelmatch.data.TournamentRepository
import com.kherio.padelmatch.ui.theme.AlmostBlack
import com.kherio.padelmatch.ui.theme.Gold
import com.kherio.padelmatch.ui.theme.GoldLight
import com.kherio.padelmatch.ui.theme.CourtGreen
import kotlin.random.Random

@Composable
fun PodiumScreen(
    tournamentId: String,
    repository: TournamentRepository,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    var tournament by remember { mutableStateOf<Tournament?>(null) }

    LaunchedEffect(tournamentId) {
        val t = repository.getById(tournamentId) ?: return@LaunchedEffect
        if (!t.isFinished) {
            val finished = t.copy(isFinished = true)
            repository.save(finished)
            tournament = finished
        } else {
            tournament = t
        }
    }

    val t = tournament ?: return
    val ranking = t.players.sortedByDescending { it.totalPoints }

    fun shareResult() {
        val lines = ranking.mapIndexed { i, p ->
            val medal = when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${i + 1}." }
            "$medal ${p.name} — ${p.totalPoints} pts"
        }.joinToString("\n")
        val text = "🏆 ¡Torneo finalizado! ${t.name}\n\n$lines\n\n🎾 Artaza Torresolo Pádel Club"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir resultado final"))
    }

    Box(Modifier.fillMaxSize().background(AlmostBlack)) {
        ConfettiOverlay(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_emblem),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "¡Torneo finalizado!",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                t.name,
                color = Gold,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(28.dp))

            // Podio: 2º - 1º - 3º (orden visual clásico)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                ranking.getOrNull(1)?.let { PodiumStand(player = it, place = 2, barHeight = 100.dp, delayMillis = 450, modifier = Modifier.weight(1f)) }
                    ?: Spacer(Modifier.weight(1f))
                ranking.getOrNull(0)?.let { PodiumStand(player = it, place = 1, barHeight = 140.dp, delayMillis = 0, modifier = Modifier.weight(1f)) }
                    ?: Spacer(Modifier.weight(1f))
                ranking.getOrNull(2)?.let { PodiumStand(player = it, place = 3, barHeight = 72.dp, delayMillis = 650, modifier = Modifier.weight(1f)) }
                    ?: Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            if (ranking.size > 3) {
                AnimatedVisibility(visible = true, enter = fadeIn(tween(400, delayMillis = 900)) + expandVertically(tween(400, delayMillis = 900))) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "Resto de la clasificación",
                            color = Color(0xFFBFBFBF),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 220.dp)) {
                            itemsIndexed(ranking.drop(3), key = { _, p -> p.id }) { index, p ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${index + 4}. ${p.name}", color = Color.White)
                                    Text("${p.totalPoints} pts", color = Color(0xFFBFBFBF))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onBackToHome,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Inicio")
                }
                Button(
                    onClick = { shareResult() },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Compartir", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PodiumStand(player: Player, place: Int, barHeight: androidx.compose.ui.unit.Dp, delayMillis: Int, modifier: Modifier = Modifier) {
    val medalColor = when (place) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32)
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(350, delayMillis = delayMillis)) + scaleIn(tween(350, delayMillis = delayMillis), initialScale = 0.4f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(medalColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    player.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(90.dp)
                )
                Text("${player.totalPoints} pts", color = Color(0xFFBFBFBF), style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .width(84.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(medalColor),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                "$place",
                modifier = Modifier.padding(top = 10.dp),
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

private data class ConfettiParticle(val x: Float, val phase: Float, val speed: Float, val size: Float, val color: Color, val drift: Float)

@Composable
private fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val colors = listOf(Gold, GoldLight, CourtGreen, Color.White)
    val particles = remember {
        List(45) {
            ConfettiParticle(
                x = Random.nextFloat(),
                phase = Random.nextFloat(),
                speed = 0.5f + Random.nextFloat() * 0.9f,
                size = 3f + Random.nextFloat() * 5f,
                color = colors[Random.nextInt(colors.size)],
                drift = (Random.nextFloat() - 0.5f) * 0.25f
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 7000, easing = LinearEasing)),
        label = "confettiTime"
    )
    Canvas(modifier = modifier) {
        particles.forEach { p ->
            val progress = (time * p.speed + p.phase) % 1f
            val y = progress * size.height
            val x = ((p.x + p.drift * progress).mod(1f)) * size.width
            drawCircle(color = p.color, radius = p.size, center = Offset(x, y), alpha = 0.8f)
        }
    }
}
