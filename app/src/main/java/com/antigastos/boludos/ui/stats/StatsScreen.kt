package com.antigastos.boludos.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.ads.BannerAd
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.PersonaCatalog
import com.antigastos.boludos.domain.SpanishMonths
import com.antigastos.boludos.domain.model.CategorySlice
import com.antigastos.boludos.domain.model.StatsSnapshot
import com.antigastos.boludos.ui.MainViewModel
import com.antigastos.boludos.ui.common.EmptyStateWithPersona
import com.antigastos.boludos.ui.common.MonthSelector
import com.antigastos.boludos.ui.theme.AgColors
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    mainViewModel: MainViewModel,
    onAdd: () -> Unit = {},
) {
    val snap by viewModel.snapshot.collectAsState()
    val month by mainViewModel.selectedMonth.collectAsState()
    val ctx = LocalContext.current
    val app = ctx.applicationContext as AntiGastosApplication
    val settings by app.settingsRepository.flow.collectAsState(initial = SettingsEntity())

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MonthSelector(
            yearMonth = month,
            onPrev = { mainViewModel.shiftMonth(-1) },
            onNext = { mainViewModel.shiftMonth(1) },
            modifier = Modifier.fillMaxWidth(),
        )

        snap?.let { s ->
            if (s.totalPesos == 0L && s.categorySlices.isEmpty()) {
                val persona = remember(settings.personaKey) {
                    PersonaCatalog.byKey(settings.personaKey)
                }
                EmptyStateWithPersona(
                    personaEmoji = persona.emoji,
                    personaName = persona.displayName,
                    headline = "Acá hay 0 stats",
                    body = "Sin gastos no hay heatmap, ni top, ni nada que graficar. Cargá uno y arrancan los números.",
                    primaryActionLabel = "➕ Cargá tu primer gasto",
                    onPrimaryAction = onAdd,
                )
            } else {
                SummaryVsPrev(s)

                val pastLabel = s.pastSelfLabel
                val pastTotal = s.pastSelfTotalPesos
                val pastDiff = s.pastSelfDiffPesos
                if (pastLabel != null && pastTotal != null && pastDiff != null) {
                    PastSelfCard(
                        label = pastLabel,
                        pastTotalPesos = pastTotal,
                        diffPesos = pastDiff,
                    )
                }

                HeatmapCard(s)

                if (s.topDays.isNotEmpty()) {
                    TopDaysCard(s)
                }

                if (s.categorySlices.isNotEmpty()) {
                    Text("Por categoría", style = MaterialTheme.typography.titleMedium)
                    SimpleBarChart(s.categorySlices)
                    s.categorySlices.forEach { slice ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(slice.label)
                            Text(MoneyFormat.formatPesos(slice.amountPesos))
                        }
                    }
                } else {
                    Text("No hay datos para graficar este mes.")
                }
            }
        } ?: Text("Cargando…")

        // Banner al pie de la pantalla de estadísticas
        if (com.antigastos.boludos.domain.AdFreePolicy.shouldShowAds(settings)) {
            Spacer(Modifier.height(12.dp))
            BannerAd(
                modifier = Modifier.fillMaxWidth(),
                adUnitId = com.antigastos.boludos.ads.AdMobIds.bannerStatsPie,
            )
        }
    }
}

@Composable
private fun SummaryVsPrev(s: StatsSnapshot) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Total ${SpanishMonths.name(s.yearMonth.monthValue)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(MoneyFormat.formatPesos(s.totalPesos), style = MaterialTheme.typography.headlineSmall)
            val prev = s.previousMonthTotalPesos
            val diff = s.totalPesos - prev
            val arrow = when {
                prev == 0L -> ""
                diff > 0 -> "📈"
                diff < 0 -> "📉"
                else -> "≈"
            }
            Text(
                "$arrow Mes anterior: ${MoneyFormat.formatPesos(prev)} (" +
                    "${if (diff >= 0) "+" else "-"}${MoneyFormat.formatPesos(abs(diff))})",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PastSelfCard(
    label: String,
    pastTotalPesos: Long,
    diffPesos: Long,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Tu yo de hace 3 meses ($label)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Ese mes gastaste ${MoneyFormat.formatPesos(pastTotalPesos)}.",
                style = MaterialTheme.typography.bodyMedium,
            )
            val cmp = when {
                diffPesos == 0L -> "Este mes vas igual que ese."
                diffPesos < 0L -> "Vas ${MoneyFormat.formatPesos(-diffPesos)} por debajo de ese mes. Bien ahí."
                else -> "Vas ${MoneyFormat.formatPesos(diffPesos)} por encima de ese mes."
            }
            Text(cmp, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HeatmapCard(s: StatsSnapshot) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Heatmap del mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Cada cuadradito es un día. Más oscuro = más gastaste.",
                style = MaterialTheme.typography.bodySmall,
            )
            Heatmap(s)
        }
    }
}

@Composable
private fun Heatmap(s: StatsSnapshot) {
    val daysInMonth = s.yearMonth.lengthOfMonth()
    val cols = 7
    val rows = (daysInMonth + cols - 1) / cols
    val maxV = s.dailyTotals.values.maxOrNull() ?: 1L

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(cols.toFloat() / rows.toFloat()),
    ) {
        val padding = 4f
        val cellW = (size.width - (cols + 1) * padding) / cols
        val cellH = (size.height - (rows + 1) * padding) / rows
        val empty = Color(0xFFE0E6EE)
        val full = AgColors.AlarmRed
        for (day in 1..daysInMonth) {
            val idx = day - 1
            val r = idx / cols
            val c = idx % cols
            val left = padding + c * (cellW + padding)
            val top = padding + r * (cellH + padding)
            val v = s.dailyTotals[day] ?: 0L
            val ratio = if (maxV == 0L) 0.0 else v.toDouble() / maxV.toDouble()
            val color = lerpColor(empty, full, ratio.toFloat())
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(cellW, cellH),
                cornerRadius = CornerRadius(6f, 6f),
            )
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val tt = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * tt,
        green = a.green + (b.green - a.green) * tt,
        blue = a.blue + (b.blue - a.blue) * tt,
        alpha = 1f,
    )
}

@Composable
private fun TopDaysCard(s: StatsSnapshot) {
    val fmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale("es", "AR"))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Top 5 días caros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            s.topDays.forEachIndexed { i, d ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        Text("${i + 1}. " + fmt.format(d.date).replaceFirstChar { it.titlecase() })
                    }
                    Text(MoneyFormat.formatPesos(d.amountPesos), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SimpleBarChart(slices: List<CategorySlice>) {
    val max = slices.maxOfOrNull { it.amountPesos }?.coerceAtLeast(1L) ?: 1L
    val colors = listOf(
        Color(0xFF2E7D32),
        Color(0xFFF9A825),
        Color(0xFF00695C),
        Color(0xFF5C6BC0),
        Color(0xFFEF5350),
        Color(0xFF8D6E63),
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        val n = slices.size.coerceAtLeast(1)
        val gap = size.width * 0.02f
        val barW = (size.width - (gap * (n + 1))) / n
        slices.forEachIndexed { i, slice ->
            val h = (slice.amountPesos.toFloat() / max.toFloat()) * size.height * 0.85f
            val left = gap + i * (barW + gap)
            val top = size.height - h
            drawRoundRect(
                color = colors[i % colors.size],
                topLeft = Offset(left, top),
                size = Size(barW, h),
                cornerRadius = CornerRadius(8f, 8f),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}
