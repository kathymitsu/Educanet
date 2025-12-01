package com.example.educanet.screen

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import kotlinx.coroutines.delay
import com.example.educanet.R

/**
 * Splash de inicio con animación tipo Animista:
 * - logo hace scale (0.6 → 1.0) + fade (0 → 1)
 * - texto aparece suavemente
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Alpha para efecto de fade-in
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 900,
            easing = LinearOutSlowInEasing
        ),
        label = "splashAlpha"
    )

    // Scale para efecto de “zoom in” (como algunas animaciones de animista.cl)
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.6f,
        animationSpec = tween(
            durationMillis = 900,
            easing = LinearOutSlowInEasing
        ),
        label = "splashScale"
    )

    // Pequeño delay antes de ir a login
    LaunchedEffect(Unit) {
        startAnimation = true
        // tiempo total visible del splash (animación + pausa)
        delay(2000) // 2 segundos aprox
        onFinished()
    }

    // Fondo completo
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Círculo central con logo y texto
        Column(
            modifier = Modifier
                .padding(32.dp)
                .scale(scale)      // zoom animado
                .alpha(alpha),     // fade animado
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(140.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Logo de la app (puedes cambiar el recurso si tienes otro)
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_round),
                        contentDescription = "Logo Educanet",
                        modifier = Modifier.size(96.dp)
                    )
                }
            }

            Text(
                text = "Educanet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Conectando profesores, alumnos\n y apoderados en un solo lugar",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                lineHeight = 18.sp
            )
        }
    }
}
