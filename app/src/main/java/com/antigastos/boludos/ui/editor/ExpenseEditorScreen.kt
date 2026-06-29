package com.antigastos.boludos.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.SpendingBudgetEngine
import com.antigastos.boludos.ui.theme.AgColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseEditorScreen(
    viewModel: ExpenseEditorViewModel,
    title: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.amountDigits,
                onValueChange = viewModel::setAmountDigits,
                label = { Text("Monto (pesos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    val v = state.amountDigits.toLongOrNull()
                    Column {
                        if (v != null && v > 0) {
                            Text(
                                "${MoneyFormat.formatPesos(v)} · ~${MoneyFormat.pesosToLucas(v)} lucas",
                            )
                        }
                        val lh = state.lifeHoursLine
                        if (lh != null) {
                            Text(
                                lh,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                },
            )

            state.spendCheck?.let { check ->
                SpendingHint(check)
            }

            Text("Categoría")
            if (state.suggestedSlug != null) {
                Text(
                    "💡 Sugerencia: parece de \"${state.suggestedSlug}\"",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.categories.forEach { cat ->
                    FilterChip(
                        selected = cat.id == state.selectedCategoryId,
                        onClick = { viewModel.selectCategory(cat.id) },
                        label = { Text(cat.name) },
                    )
                }
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Nota (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.partnerEnabled) {
                PartnerSplitSection(
                    partnerName = state.partnerName,
                    sharedSplitPercent = state.sharedSplitPercent,
                    amountDigits = state.amountDigits,
                    onToggle = { viewModel.setShared(it) },
                    onSplitChange = { viewModel.setSharedSplitPercent(it) },
                )
            }

            Button(
                onClick = { viewModel.save(onSaved) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving &&
                    state.amountDigits.toLongOrNull()?.let { it > 0 } == true &&
                    state.selectedCategoryId != null,
            ) {
                Text(
                    when {
                        state.saving -> "Guardando…"
                        state.isEdit -> "Guardar cambios"
                        else -> "Guardar gasto"
                    },
                )
            }
        }

        if (state.askConfirmation) {
            val tip = state.spendCheck?.tip ?: "Estás muy zarpado, ¿seguro?"
            val holiday = state.spendCheck?.holiday
            AlertDialog(
                onDismissRequest = { viewModel.dismissConfirmation() },
                title = { Text("¿Seguro que querés cargarlo?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tip)
                        if (holiday != null) {
                            Text(
                                "${holiday.emoji} Hoy es ${holiday.name}: ${holiday.tagline}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmAndSave(onSaved) }) {
                        Text("Cargarlo igual")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissConfirmation() }) {
                        Text("Mejor no")
                    }
                },
            )
        }

        if (state.askCrushChoice) {
            val catName = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name ?: "esta categoría"
            val v = state.amountDigits.toLongOrNull() ?: 0L
            AlertDialog(
                onDismissRequest = { viewModel.dismissCrushChoice() },
                title = { Text("🧊 ¿Antojo o necesidad?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Son ${MoneyFormat.formatPesos(v)} en $catName. " +
                                "Si fue un impulso, podés mandarlo a la heladera 24 h y te aviso después.",
                        )
                        OutlinedButton(
                            onClick = { viewModel.deferCrush24h(onSaved) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.saving,
                        ) { Text("Heladera 24 h") }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.saveNowAfterCrushChoice(onSaved) },
                        enabled = !state.saving,
                    ) { Text("Guardar ya") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissCrushChoice() }) { Text("Cancelar") }
                },
            )
        }
    }
}

@Composable
private fun PartnerSplitSection(
    partnerName: String,
    sharedSplitPercent: Int?,
    amountDigits: String,
    onToggle: (Boolean) -> Unit,
    onSplitChange: (Int) -> Unit,
) {
    val isShared = sharedSplitPercent != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isShared) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "👥 Compartir con $partnerName",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (isShared) {
                            "$partnerName te debe la diferencia."
                        } else {
                            "Marcalo si lo pagaste vos por los dos."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = isShared, onCheckedChange = onToggle)
            }
            if (isShared) {
                val split = sharedSplitPercent ?: 50
                val amount = amountDigits.toLongOrNull() ?: 0L
                Text(
                    "Vos: $split%   ·   $partnerName: ${100 - split}%",
                    fontWeight = FontWeight.SemiBold,
                )
                Slider(
                    value = split.toFloat(),
                    onValueChange = { onSplitChange(it.toInt()) },
                    valueRange = 1f..99f,
                    steps = 97,
                )
                if (amount > 0) {
                    val partnerOwes = amount * (100 - split) / 100
                    val mine = amount - partnerOwes
                    Text(
                        "Vos ponés ${MoneyFormat.formatPesos(mine)} · " +
                            "$partnerName te debe ${MoneyFormat.formatPesos(partnerOwes)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendingHint(check: SpendingBudgetEngine.SpendCheck) {
    val color = when (check.verdict) {
        SpendingBudgetEngine.Permission.GREEN -> AgColors.PermissionGreen
        SpendingBudgetEngine.Permission.YELLOW -> AgColors.PermissionYellow
        SpendingBudgetEngine.Permission.RED -> AgColors.PermissionRed
        SpendingBudgetEngine.Permission.FORBIDDEN -> AgColors.PermissionForbidden
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                check.tip,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
            check.holiday?.let { h ->
                Text("${h.emoji} ${h.name}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
