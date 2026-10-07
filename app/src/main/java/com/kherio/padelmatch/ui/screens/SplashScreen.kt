package com.kherio.padelmatch.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kherio.padelmatch.R
import kotlinx.coroutines.delay

/**
 * Pantalla de bienvenida con el logo de Torresolo:
 *  1. el logo se revela desde el centro con un círculo que se expande,
 *     mientras hace un suave zoom hacia dentro;
 *  2. un destello de luz lo recorre en diagonal una vez;
 *  3. pasa sola a Home.
 * El fondo (textura oscura) lo pone la raíz de la app.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val reveal = remember { Animatable(0f) }
    val zoom = remember { Animatable(1.12f) }
    val sheen = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        zoom.animateTo(1f, animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing))
        sheen.animateTo(1f, animationSpec = tween(durationMillis = 1000, easing = LinearEasing))
        delay(500)
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_torresolo),
            contentDescription = "Torresolo Pádel Club",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(zoom.value)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    // 1) revelado circular desde el centro
                    val radius = size.maxDimension * 0.75f * reveal.value
                    val circle = Path().apply {
                        addOval(Rect(center = center, radius = radius))
                    }
                    clipPath(circle) {
                        this@drawWithContent.drawContent()
                    }
                    // 2) destello diagonal sobre lo ya dibujado
                    val w = size.width
                    val x = -w * 0.4f + w * 1.8f * sheen.value
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            start = Offset(x, 0f),
                            end = Offset(x + w * 0.35f, size.height * 0.35f)
                        ),
                        blendMode = BlendMode.SrcAtop
                    )
                }
        )
    }
}
