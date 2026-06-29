package com.antigastos.boludos.ui.recap

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.ui.MainViewModel
import com.antigastos.boludos.ui.share.ShareCardRenderer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecapScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as AntiGastosApplication
    val vm: RecapViewModel = viewModel(factory = RecapViewModel.factory(app, mainViewModel))
    val ui by vm.ui.collectAsState()

    val palette = palette(ui?.mood)

    Scaffold(
        containerColor = palette.bgBottom,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Recap del mes",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.bgTop,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(palette.bgTop, palette.bgBottom),
                    ),
                )
                .padding(padding),
        ) {
            when (val state = ui) {
                null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = Color.White) }

                else -> RecapContent(
                    state = state,
                    palette = palette,
                    onShare = {
                        ShareCardRenderer.shareMonthlyRecap(
                            ctx,
                            title = state.title,
                            lines = state.shareLines,
                            mood = state.mood,
                        )
                    },
                    onShareStory = {
                        val persona = com.antigastos.boludos.domain.PersonaCatalog.byKey(state.personaKey)
                        ShareCardRenderer.shareMonthlyRecapStory(
                            context = ctx,
                            monthLabel = state.title.removePrefix("Tu ").removeSuffix(", en corto"),
                            totalPesos = state.totalPesos,
                            personaEmoji = state.personaEmoji,
                            personaName = persona.displayName,
                            highlights = state.shareLines,
                            closingQuote = state.punchline,
                            mood = state.mood,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecapContent(
    state: RecapUi,
    palette: RecapPalette,
    onShare: () -> Unit,
    onShareStory: () -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(state.yearMonth) {
        entered = false
        kotlinx.coroutines.delay(40)
        entered = true
    }
    val animProgress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "recapEnter",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            state.title.uppercase(),
            color = palette.accent.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )

        HeroCard(state = state, palette = palette, animProgress = animProgress)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.tiles.forEachIndexed { index, tile ->
                TileCard(
                    tile = tile,
                    palette = palette,
                    delayMs = 60L * index,
                )
            }
        }

        PunchlineCard(text = state.punchline, palette = palette)

        Button(
            onClick = onShareStory,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = palette.bgBottom,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Compartir story 9:16",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        androidx.compose.material3.OutlinedButton(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Compartir wrapped (cuadrado)")
        }

        Text(
            "Anti-gastos boludos · 🇦🇷",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HeroCard(
    state: RecapUi,
    palette: RecapPalette,
    animProgress: Float,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.10f),
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            palette.heroTop.copy(alpha = 0.95f),
                            palette.heroBottom.copy(alpha = 0.95f),
                        ),
                    ),
                )
                .padding(horizontal = 22.dp, vertical = 28.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    state.heroLabel,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    state.heroValue,
                    color = Color.White,
                    fontSize = (44 * animProgress.coerceAtLeast(0.55f)).sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    state.heroSub,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.20f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "${moodEmoji(state.mood)} ${moodLabel(state.mood)}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TileCard(
    tile: RecapTile,
    palette: RecapPalette,
    delayMs: Long,
) {
    var visible by remember(tile) { mutableStateOf(false) }
    LaunchedEffect(tile) {
        kotlinx.coroutines.delay(delayMs)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "tileAlpha",
    )
    Card(
        modifier = Modifier
            .widthIn(min = 150.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(20.dp),
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.13f * alpha),
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .widthIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                tile.emoji,
                fontSize = 22.sp,
            )
            Text(
                tile.label.uppercase(),
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                tile.value,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            tile.sub?.let {
                Text(
                    it,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PunchlineCard(text: String, palette: RecapPalette) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.25f),
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "💬",
                fontSize = 26.sp,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "“$text”",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private data class RecapPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val heroTop: Color,
    val heroBottom: Color,
    val accent: Color,
)

private fun palette(mood: CopyMood?): RecapPalette = when (mood) {
    CopyMood.CHEER -> RecapPalette(
        bgTop = Color(0xFF0F4C2A),
        bgBottom = Color(0xFF1B1B2A),
        heroTop = Color(0xFF2E7D32),
        heroBottom = Color(0xFF1B5E20),
        accent = Color(0xFFB7E1C0),
    )
    CopyMood.NEUTRAL, null -> RecapPalette(
        bgTop = Color(0xFF103163),
        bgBottom = Color(0xFF0E1B2E),
        heroTop = Color(0xFF1565C0),
        heroBottom = Color(0xFF0D47A1),
        accent = Color(0xFF74ACDF),
    )
    CopyMood.BURN -> RecapPalette(
        bgTop = Color(0xFF6A3F00),
        bgBottom = Color(0xFF1F1404),
        heroTop = Color(0xFFEF6C00),
        heroBottom = Color(0xFFC25500),
        accent = Color(0xFFFFD68A),
    )
    CopyMood.ALARM -> RecapPalette(
        bgTop = Color(0xFF7A0E1E),
        bgBottom = Color(0xFF1F0408),
        heroTop = Color(0xFFD7263D),
        heroBottom = Color(0xFF8C0E1E),
        accent = Color(0xFFF4A0AB),
    )
}

private fun moodEmoji(mood: CopyMood): String = when (mood) {
    CopyMood.CHEER -> "🥳"
    CopyMood.NEUTRAL -> "🧉"
    CopyMood.BURN -> "🔥"
    CopyMood.ALARM -> "🚨"
}

private fun moodLabel(mood: CopyMood): String = when (mood) {
    CopyMood.CHEER -> "Mes copado"
    CopyMood.NEUTRAL -> "Mes tranqui"
    CopyMood.BURN -> "Mes ardiente"
    CopyMood.ALARM -> "Mes en rojo"
}
