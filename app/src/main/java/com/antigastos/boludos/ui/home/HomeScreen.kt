package com.antigastos.boludos.ui.home

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.SpanishMonths
import com.antigastos.boludos.domain.model.HomeUiModel
import com.antigastos.boludos.ui.MainViewModel
import com.antigastos.boludos.ui.common.MonthSelector

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    mainViewModel: MainViewModel,
    onOpenList: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenGoals: () -> Unit,
) {
    val ui by viewModel.uiState.collectAsState()
    val month by mainViewModel.selectedMonth.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Cuánto se te va", style = MaterialTheme.typography.headlineSmall)

        MonthSelector(
            yearMonth = month,
            onPrev = { mainViewModel.shiftMonth(-1) },
            onNext = { mainViewModel.shiftMonth(1) },
            modifier = Modifier.fillMaxWidth(),
        )

        ui?.let { state ->
            SummarySection(state)
            GoalSection(state)
            PersonalityCard(state)
            QuickLinks(onOpenList, onOpenStats, onOpenGoals)
        } ?: Text("Cargando…")
    }
}

@Composable
private fun SummarySection(state: HomeUiModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Total en ${SpanishMonths.name(state.yearMonth.monthValue)}", style = MaterialTheme.typography.titleMedium)
            Text(
                MoneyFormat.formatPesos(state.totalPesos),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "(${MoneyFormat.pesosToLucas(state.totalPesos)} lucas aprox.)",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text("Top categorías", style = MaterialTheme.typography.titleSmall)
            state.topCategories.forEach { cat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(cat.name)
                    Text(MoneyFormat.formatPesos(cat.amountPesos))
                }
            }
            if (state.topCategories.isEmpty()) {
                Text("Todavía no cargaste gastos este mes.")
            }
        }
    }
}

@Composable
private fun GoalSection(state: HomeUiModel) {
    val goal = state.globalGoalPesos ?: return
    val progress = state.globalProgress ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Meta global del mes", style = MaterialTheme.typography.titleMedium)
            Text("Tope: ${MoneyFormat.formatPesos(goal)}")
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PersonalityCard(state: HomeUiModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Modo honesto", style = MaterialTheme.typography.titleMedium)
            Text(state.personalityMessage, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun QuickLinks(onList: () -> Unit, onStats: () -> Unit, onGoals: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ir a…", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.FilledTonalButton(onClick = onList, modifier = Modifier.weight(1f)) {
                Text("Historial")
            }
            androidx.compose.material3.FilledTonalButton(onClick = onStats, modifier = Modifier.weight(1f)) {
                Text("Stats")
            }
        }
        androidx.compose.material3.FilledTonalButton(onClick = onGoals, modifier = Modifier.fillMaxWidth()) {
            Text("Metas")
        }
    }
}
