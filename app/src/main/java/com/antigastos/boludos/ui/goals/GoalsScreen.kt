package com.antigastos.boludos.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.ui.MainViewModel
import com.antigastos.boludos.ui.common.MonthSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    mainViewModel: MainViewModel,
) {
    val goals by viewModel.goals.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val month by mainViewModel.selectedMonth.collectAsState()

    var globalDigits by remember { mutableStateOf("") }
    var categoryDigits by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(value = false) }
    var selectedCategory by remember(categories) {
        mutableStateOf(categories.firstOrNull())
    }

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

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Meta global", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = globalDigits,
                    onValueChange = { globalDigits = it.filter { ch -> ch.isDigit() }.take(9) },
                    label = { Text("Tope en pesos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        val v = globalDigits.toLongOrNull() ?: return@Button
                        viewModel.saveGoal(0L, v)
                        globalDigits = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = globalDigits.toLongOrNull()?.let { it > 0 } == true,
                ) {
                    Text("Guardar meta global")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Meta por categoría", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = categoryDigits,
                    onValueChange = { categoryDigits = it.filter { ch -> ch.isDigit() }.take(9) },
                    label = { Text("Tope en pesos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        val cat = selectedCategory ?: return@Button
                        val v = categoryDigits.toLongOrNull() ?: return@Button
                        viewModel.saveGoal(cat.id, v)
                        categoryDigits = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = (selectedCategory != null) && (categoryDigits.toLongOrNull()?.let { it > 0 } == true),
                ) {
                    Text("Guardar meta de categoría")
                }
            }
        }

        Text("Metas activas", style = MaterialTheme.typography.titleMedium)
        goals.forEach { g ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            if (g.categoryId == 0L) {
                                "Global"
                            } else {
                                categories.find { it.id == g.categoryId }?.name ?: "Cat ${g.categoryId}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text("Tope: ${MoneyFormat.formatPesos(g.limitPesos)}")
                    }
                }
            }
        }
        if (goals.isEmpty()) {
            Text("Todavía no definiste metas para este mes.")
        }
        Spacer(Modifier.height(24.dp))
    }
}
