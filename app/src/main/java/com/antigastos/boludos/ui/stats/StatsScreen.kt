package com.antigastos.boludos.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.SpanishMonths
import com.antigastos.boludos.domain.model.CategorySlice
import com.antigastos.boludos.domain.model.StatsSnapshot
import com.antigastos.boludos.ui.MainViewModel
import com.antigastos.boludos.ui.common.MonthSelector
import kotlin.math.abs

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    mainViewModel: MainViewModel,
) {
    val snap by viewModel.snapshot.collectAsState()
    val month by mainViewModel.selectedMonth.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Estadísticas", style = MaterialTheme.typography.headlineSmall)

        MonthSelector(
            yearMonth = month,
            onPrev = { mainViewModel.shiftMonth(-1) },
            onNext = { mainViewModel.shiftMonth(1) },
            modifier = Modifier.fillMaxWidth(),
        )

        snap?.let { s ->
            SummaryVsPrev(s)
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
        } ?: Text("Cargando…")
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
            Text(
                "Mes anterior: ${MoneyFormat.formatPesos(prev)} (" +
                    "${if (diff >= 0) "+" else "-"}${MoneyFormat.formatPesos(abs(diff))})",
                style = MaterialTheme.typography.bodyMedium,
            )
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
        val barW = (size.width - gap * (n + 1)) / n
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
