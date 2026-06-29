package com.antigastos.boludos.ui.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.ui.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel,
    prefill: MainViewModel.SubscriptionFormPrefill? = null,
    onConsumePrefill: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("1") }
    var selectedCat by remember { mutableStateOf<Long?>(null) }
    var installmentsMode by remember { mutableStateOf(false) }
    var installments by remember { mutableStateOf("12") }

    LaunchedEffect(prefill) {
        val p = prefill ?: return@LaunchedEffect
        name = p.name
        amount = p.amountPesos.toString()
        day = p.dayOfMonth.coerceIn(1, 28).toString()
        selectedCat = p.categoryId
        installmentsMode = false
        onConsumePrefill()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Suscripciones / gastos fijos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Cargá lo que pagás todos los meses (Netflix, alquiler, internet…). Cada mes se anota como gasto solo el día que vos definas.",
            style = MaterialTheme.typography.bodySmall,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (installmentsMode) "Nueva compra en cuotas" else "Nueva suscripción",
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Modo cuotas",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Switch(
                        checked = installmentsMode,
                        onCheckedChange = { installmentsMode = it },
                    )
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(40) },
                    label = {
                        Text(if (installmentsMode) "Qué compraste (ej. Tele)" else "Nombre (ej. Netflix)")
                    },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.filter(Char::isDigit).take(11) },
                    label = {
                        Text(
                            if (installmentsMode) "Total a pagar (pesos)"
                            else "Monto mensual (pesos)",
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                if (installmentsMode) {
                    OutlinedTextField(
                        value = installments,
                        onValueChange = { installments = it.filter(Char::isDigit).take(3) },
                        label = { Text("Cuotas (ej. 12)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val total = amount.toLongOrNull() ?: 0L
                    val n = installments.toIntOrNull() ?: 0
                    if (total > 0 && n > 0) {
                        Text(
                            "→ Cada mes te cargo ${MoneyFormat.formatPesos(total / n)}.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                OutlinedTextField(
                    value = day, onValueChange = { day = it.filter(Char::isDigit).take(2) },
                    label = { Text("Día del mes (1–28)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Text("Categoría", style = MaterialTheme.typography.bodySmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.categories.forEach { c ->
                        FilterChip(
                            selected = c.id == selectedCat,
                            onClick = { selectedCat = c.id },
                            label = { Text(c.name) },
                        )
                    }
                }
                Button(
                    enabled = name.isNotBlank() &&
                        amount.toLongOrNull()?.let { it > 0 } == true &&
                        selectedCat != null &&
                        (!installmentsMode || (installments.toIntOrNull() ?: 0) > 0),
                    onClick = {
                        if (installmentsMode) {
                            viewModel.addInstallments(
                                name = name,
                                totalPesos = amount.toLong(),
                                categoryId = selectedCat!!,
                                installments = installments.toIntOrNull() ?: 1,
                                dayOfMonth = day.toIntOrNull() ?: 1,
                            )
                        } else {
                            viewModel.add(
                                name = name,
                                amountPesos = amount.toLong(),
                                categoryId = selectedCat!!,
                                dayOfMonth = day.toIntOrNull() ?: 1,
                            )
                        }
                        name = ""; amount = ""; day = "1"; selectedCat = null
                        installments = "12"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (installmentsMode) "Cargar en cuotas" else "Agregar")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Tus suscripciones", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(state.items, key = { it.id }) { sub ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(sub.name, style = MaterialTheme.typography.titleMedium)
                            val installmentsLine = sub.installmentsTotal?.let { total ->
                                val paid = sub.installmentsPaid.coerceAtMost(total)
                                "Cuota $paid/$total"
                            }
                            Text(
                                buildString {
                                    append(MoneyFormat.formatPesos(sub.amountPesos))
                                    append(" · cada día ${sub.dayOfMonth}")
                                    if (installmentsLine != null) append(" · $installmentsLine")
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(checked = sub.active, onCheckedChange = { viewModel.toggle(sub) })
                        IconButton(onClick = { viewModel.delete(sub) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar")
                        }
                    }
                }
            }
        }
    }
}
