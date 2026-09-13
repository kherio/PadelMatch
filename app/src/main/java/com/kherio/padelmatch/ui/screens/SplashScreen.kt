package com.kherio.padelmatch.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kherio.padelmatch.R
import com.kherio.padelmatch.ui.theme.AlmostBlack
import com.kherio.padelmatch.ui.theme.Gold
import kotlinx.coroutines.delay

/**
 * Pantalla de bienvenida: muestra el logo del club en grande con una
 * animación de entrada (escala + fundido) y pasa sola a Home tras un
 * instante.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Efecto de entrada: aparece haciéndose grande con un ligero rebote
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = LinearOutSlowInEasing)
        )
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(durationMillis = 500))
        delay(900)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlmostBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo_artaza),
                contentDescription = "Artaza Torresolo Pádel Club",
                modifier = Modifier
                    .size(220.dp)
                    .scale(scale.value)
                    .alpha(alpha.value)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "ARTAZA TORRESOLO",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.alpha(alpha.value)
            )
            Text(
                "PÁDEL CLUB",
                color = Color(0xFFBFBFBF),
                fontSize = 13.sp,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}
