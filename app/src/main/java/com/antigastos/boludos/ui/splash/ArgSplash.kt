package com.antigastos.boludos.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.R
import kotlin.math.roundToInt

private val SplashBackground = Color(0xFFEDF1F4)

@Composable
fun ArgSplash(modifier: Modifier = Modifier) {
    var entered by remember { mutableStateOf(false) }
    var textIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
        kotlinx.coroutines.delay(450)
        textIn = true
    }

    val imageScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.55f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "imgScale",
    )
    val imageAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "imgAlpha",
    )
    val imageRotation by animateFloatAsState(
        targetValue = if (entered) 0f else -8f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "imgRotation",
    )

    val infinite = rememberInfiniteTransition(label = "splashPulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (textIn) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "titleAlpha",
    )
    val titleOffset by animateFloatAsState(
        targetValue = if (textIn) 0f else 24f,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "titleOffset",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .safeDrawingPadding()
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .wrapContentHeight()
                .padding(24.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.arg),
                contentDescription = "Argentina",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .alpha(imageAlpha)
                    .scale(imageScale * pulse)
                    .rotate(imageRotation),
            )
            Spacer(Modifier.height(24.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset { IntOffset(0, titleOffset.roundToInt()) },
            ) {
                Text(
                    text = "Anti-gastos boludos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF0F1B2D),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tu plata, tus reglas.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1F4E8A),
                )
            }
        }
    }
}
