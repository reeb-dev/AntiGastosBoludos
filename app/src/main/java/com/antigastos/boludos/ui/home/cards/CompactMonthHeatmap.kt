package com.antigastos.boludos.ui.home.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.ui.theme.AgColors
import java.time.YearMonth
import kotlin.math.min

/**
 * Heatmap compacto del mes en curso (misma grilla que Stats, más chica).
 *
 * @param showPaceProjection si true, los días **posteriores a hoy** se pintan
 * con el ritmo diario promedio (suma hasta hoy / día del mes).
 */
@Composable
internal fun CompactMonthHeatmap(
    yearMonth: YearMonth,
    dailySpendByDay: Map<Int, Long>,
    todayDayOfMonth: Int?,
    showPaceProjection: Boolean,
    modifier: Modifier = Modifier,
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val cols = 7
    val rows = (daysInMonth + cols - 1) / cols
    val today = todayDayOfMonth?.coerceIn(1, daysInMonth)
    val spentThroughToday = if (today != null) {
        (1..today).sumOf { d -> dailySpendByDay[d] ?: 0L }
    } else {
        0L
    }
    val daysElapsed = today?.coerceAtLeast(1) ?: 1
    val perDayPace = if (daysElapsed > 0) spentThroughToday / daysElapsed else 0L
    val projectionOn = showPaceProjection && today != null && today < daysInMonth
    val maxV = buildList {
        addAll(dailySpendByDay.values)
        if (projectionOn && perDayPace > 0L) add(perDayPace)
    }.maxOrNull() ?: 0L

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Tu mes en un vistazo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (projectionOn) {
                    "Cada cuadradito = un día. Más intenso = más gastaste. " +
                        "Los días que vienen (más claros) = si seguís al ritmo de hasta hoy."
                } else {
                    "Cada cuadradito = un día. Más intenso = más gastaste."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(cols.toFloat() / rows.toFloat())
                    .padding(top = 10.dp),
            ) {
                val tDay = today
                val padding = 3f
                val cellW = (size.width - (cols + 1) * padding) / cols
                val cellH = (size.height - (rows + 1) * padding) / rows
                val empty = Color(0xFFE8EDF3)
                val full = AgColors.AlarmRed
                for (day in 1..daysInMonth) {
                    val idx = day - 1
                    val r = idx / cols
                    val c = idx % cols
                    val left = padding + c * (cellW + padding)
                    val top = padding + r * (cellH + padding)
                    val anchorDay = tDay ?: 0
                    val isFuturePace = projectionOn && anchorDay > 0 && day > anchorDay
                    val v = when {
                        isFuturePace -> perDayPace
                        else -> dailySpendByDay[day] ?: 0L
                    }
                    val ratio = if (maxV <= 0L) 0.0 else v.toDouble() / maxV.toDouble()
                    var base = lerpColorHome(empty, full, ratio.toFloat())
                    if (isFuturePace) {
                        base = base.copy(alpha = (base.alpha * 0.52f).coerceIn(0.12f, 1f))
                    }
                    val highlight = tDay != null && day == min(tDay, daysInMonth)
                    val color = if (highlight) {
                        base.copy(
                            red = (base.red + 0.12f).coerceIn(0f, 1f),
                            green = (base.green + 0.12f).coerceIn(0f, 1f),
                            blue = (base.blue + 0.08f).coerceIn(0f, 1f),
                        )
                    } else {
                        base
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, top),
                        size = Size(cellW, cellH),
                        cornerRadius = CornerRadius(5f, 5f),
                    )
                }
            }
        }
    }
}

private fun lerpColorHome(a: Color, b: Color, t: Float): Color {
    val tt = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * tt,
        green = a.green + (b.green - a.green) * tt,
        blue = a.blue + (b.blue - a.blue) * tt,
        alpha = 1f,
    )
}
