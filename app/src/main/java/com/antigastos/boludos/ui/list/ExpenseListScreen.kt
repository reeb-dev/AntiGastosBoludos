package com.antigastos.boludos.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.ads.AdHelper
import com.antigastos.boludos.domain.AdFreePolicy
import com.antigastos.boludos.ads.BannerAd
import com.antigastos.boludos.ads.NativeAdCard
import com.antigastos.boludos.data.local.dao.ExpenseListRow
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.PersonaCatalog
import com.antigastos.boludos.ui.MainViewModel
import com.antigastos.boludos.ui.common.EmptyStateWithPersona
import com.antigastos.boludos.ui.common.MonthSelector
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    viewModel: ExpenseListViewModel,
    mainViewModel: MainViewModel,
    onEdit: (Long) -> Unit,
    onAdd: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val month by mainViewModel.selectedMonth.collectAsState()
    val ctx = LocalContext.current
    val app = ctx.applicationContext as AntiGastosApplication
    val settings by app.settingsRepository.flow.collectAsState(initial = SettingsEntity())
    val timeFmt = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())
    var sheetRow by remember { mutableStateOf<ExpenseListRow?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val slugsInMonth = state.rows.map { it.categorySlug to it.categoryName }.distinct()
    val nativeAdPosition = 5

    val showAds = AdFreePolicy.shouldShowAds(settings)
    LaunchedEffect(showAds, state.rows.size) {
        if (showAds && state.rows.size > nativeAdPosition) {
            AdHelper.preloadNative(ctx)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MonthSelector(
            yearMonth = month,
            onPrev = { mainViewModel.shiftMonth(-1) },
            onNext = { mainViewModel.shiftMonth(1) },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            label = { Text("Buscar por categoría, nota o monto") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (slugsInMonth.size > 1) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                slugsInMonth.forEach { (slug, name) ->
                    FilterChip(
                        selected = slug == state.activeCategorySlug,
                        onClick = { viewModel.setCategoryFilter(slug) },
                        label = { Text(name) },
                    )
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(state.rows, key = { it.id }) { row ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { sheetRow = row },
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
                        }
                    }
                }
            }

            // Ad nativo intercalado después del 5° gasto
            if (showAds && state.rows.size > nativeAdPosition) {
                item(key = "native_ad_in_list") {
                    NativeAdCard()
                }
            }

            if (state.rows.isEmpty()) {
                item {
                    val hasFilter = state.query.isNotBlank() || state.activeCategorySlug != null
                    if (hasFilter) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                        ) {
                            Column(
                                Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    "🧐 Sin resultados",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "No encontré gastos con ese filtro. Probá otro filtro o limpiá la búsqueda.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    } else {
                        val persona = remember(settings.personaKey) {
                            PersonaCatalog.byKey(settings.personaKey)
                        }
                        EmptyStateWithPersona(
                            personaEmoji = persona.emoji,
                            personaName = persona.displayName,
                            headline = "Tu lista está más limpia que un caño nuevo",
                            body = "No hay gastos cargados en este mes. Cargá el primero y vamos viendo en qué se te va la guita.",
                            primaryActionLabel = "➕ Cargá tu primer gasto",
                            onPrimaryAction = onAdd,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }
            }
        }

        // Banner inferior para listas cortas (menos de 6 ítems, el nativo no se muestra)
        if (showAds && state.rows.size <= nativeAdPosition) {
            BannerAd(modifier = Modifier.padding(top = 8.dp))
        }
    }

    sheetRow?.let { row ->
        ModalBottomSheet(
            onDismissRequest = { sheetRow = null },
            sheetState = sheetState,
        ) {
            ExpenseDetailSheet(
                row = row,
                timeFmt = timeFmt,
                onEdit = {
                    sheetRow = null
                    onEdit(row.id)
                },
                onDuplicate = {
                    viewModel.duplicate(row.id)
                    sheetRow = null
                },
                onDelete = {
                    viewModel.delete(row.id)
                    sheetRow = null
                },
            )
        }
    }
}

@Composable
private fun ExpenseDetailSheet(
    row: ExpenseListRow,
    timeFmt: DateTimeFormatter,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            row.categoryName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            MoneyFormat.formatPesos(row.amountPesos),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            timeFmt.format(Instant.ofEpochMilli(row.occurredAt)),
            style = MaterialTheme.typography.bodySmall,
        )
        row.note?.takeIf { it.isNotBlank() }?.let {
            Text(
                "📝 $it",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Text("  Editar este gasto")
        }
        TextButton(
            onClick = onDuplicate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Text("  Duplicar (con la fecha de hoy)")
        }
        TextButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                "  Borrar este gasto",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
