package com.antigastos.boludos.ui.home.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.MoneySlang
import com.antigastos.boludos.domain.ShameLevel
import com.antigastos.boludos.domain.SpanishMonths
import com.antigastos.boludos.domain.SpendingBudgetEngine
import com.antigastos.boludos.domain.model.HomeUiModel
import com.antigastos.boludos.domain.model.TopCategorySpend
import java.time.LocalDate
import kotlinx.coroutines.delay

/**
 * Card hero con el total del mes, badge de presupuesto del día y top
 * categorías. El gradiente cambia según el `ShameLevel` y `CopyMood`
 * para reflejar el "humor" económico del usuario.
 */
@Composable
internal fun SummarySection(state: HomeUiModel, budget: SpendingBudgetEngine.DailyBudget?) {
    val palette = heroPaletteFor(state.shameLevel, state.personalityMood)
    val gradient = Brush.linearGradient(
        colors = listOf(palette.start, palette.end),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient),
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Total en ${SpanishMonths.name(state.yearMonth.monthValue)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onColor.copy(alpha = 0.85f),
                )
                HeroTotal(
                    totalPesos = state.totalPesos,
                    onColor = palette.onColor,
                    personaKey = state.personaKey,
                )
                if (budget != null) {
                    BudgetBadge(budget = budget, onColor = palette.onColor)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Top categorías",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.onColor,
                    fontWeight = FontWeight.SemiBold,
                )
                state.topCategories.forEachIndexed { idx, cat ->
                    AnimatedCategoryRow(
                        category = cat,
                        indexFromTop = idx,
                        textColor = palette.onColor,
                    )
                }
                if (state.topCategories.isEmpty()) {
                    Text(
                        "Todavía no cargaste gastos este mes.",
                        color = palette.onColor.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

private data class HeroPalette(
    val start: Color,
    val end: Color,
    val onColor: Color,
)

@Composable
private fun heroPaletteFor(level: ShameLevel, mood: CopyMood): HeroPalette {
    return when {
        mood == CopyMood.ALARM || level == ShameLevel.LUDOPATA -> HeroPalette(
            start = Color(0xFFD7263D),
            end = Color(0xFF7B0F1A),
            onColor = Color.White,
        )
        mood == CopyMood.BURN || level == ShameLevel.DELIVERY -> HeroPalette(
            start = Color(0xFFFFA726),
            end = Color(0xFFEF6C00),
            onColor = Color.White,
        )
        level == ShameLevel.REY_FIADO -> HeroPalette(
            start = Color(0xFFAB47BC),
            end = Color(0xFF6A1B9A),
            onColor = Color.White,
        )
        mood == CopyMood.CHEER || level == ShameLevel.EJEMPLAR -> HeroPalette(
            start = Color(0xFF66BB6A),
            end = Color(0xFF1B5E20),
            onColor = Color.White,
        )
        else -> HeroPalette(
            start = MaterialTheme.colorScheme.primary,
            end = MaterialTheme.colorScheme.tertiary,
            onColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun HeroTotal(totalPesos: Long, onColor: Color, personaKey: String?) {
    val animated by animateFloatAsState(
        targetValue = totalPesos.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "heroCounter",
    )
    val displayed = animated.toLong().coerceAtLeast(0L)
    val today = remember(personaKey) {
        LocalDate.now().toEpochDay() xor (personaKey?.hashCode()?.toLong() ?: 0L)
    }
    val noun = remember(personaKey, today) {
        MoneySlang.nounForPersona(personaKey, seed = today)
    }
    Column {
        Text(
            MoneyFormat.formatPesos(displayed),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = onColor,
        )
        Text(
            "${MoneyFormat.pesosToLucas(displayed)} $noun",
            style = MaterialTheme.typography.bodyMedium,
            color = onColor.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun BudgetBadge(budget: SpendingBudgetEngine.DailyBudget, onColor: Color) {
    val emoji = when (budget.permission) {
        SpendingBudgetEngine.Permission.GREEN -> "🟢"
        SpendingBudgetEngine.Permission.YELLOW -> "🟡"
        SpendingBudgetEngine.Permission.RED -> "🔴"
        SpendingBudgetEngine.Permission.FORBIDDEN -> "⛔"
    }
    Surface(
        color = onColor.copy(alpha = 0.18f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            "$emoji Hoy: ${MoneyFormat.formatPesos(budget.spentTodayPesos)} / ${MoneyFormat.formatPesos(budget.allowanceTodayPesos)}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = onColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AnimatedCategoryRow(
    category: TopCategorySpend,
    indexFromTop: Int,
    textColor: Color = Color.Unspecified,
) {
    var entered by remember(category.slug) { mutableStateOf(false) }
    LaunchedEffect(category.slug) {
        delay(80L * indexFromTop)
        entered = true
    }
    val a by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "catAlpha",
    )
    val offsetX by animateFloatAsState(
        targetValue = if (entered) 0f else 24f,
        animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
        label = "catOffset",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX.dp)
            .alpha(a),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(category.name, color = textColor)
        AnimatedContent(
            targetState = category.amountPesos,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically { it / 4 })
                    .togetherWith(fadeOut(tween(120)))
            },
            label = "catAmount",
        ) { value ->
            Text(MoneyFormat.formatPesos(value), color = textColor)
        }
    }
}
