package com.antigastos.boludos.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.ui.MainViewModel
import com.antigastos.boludos.ui.common.MonthSelector
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ExpenseListScreen(
    viewModel: ExpenseListViewModel,
    mainViewModel: MainViewModel,
    onEdit: (Long) -> Unit,
) {
    val rows by viewModel.rows.collectAsState()
    val month by mainViewModel.selectedMonth.collectAsState()
    val timeFmt = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Historial", style = MaterialTheme.typography.headlineSmall)
        MonthSelector(
            yearMonth = month,
            onPrev = { mainViewModel.shiftMonth(-1) },
            onNext = { mainViewModel.shiftMonth(1) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            items(rows, key = { it.id }) { row ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(row.id) },
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(row.categoryName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                timeFmt.format(Instant.ofEpochMilli(row.occurredAt)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            row.note?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(MoneyFormat.formatPesos(row.amountPesos), style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { viewModel.delete(row.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar")
                            }
                        }
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            Text("No hay gastos en este mes.", modifier = Modifier.padding(top = 24.dp))
        }
    }
}
