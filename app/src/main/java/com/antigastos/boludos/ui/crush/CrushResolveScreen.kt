package com.antigastos.boludos.ui.crush

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrushResolveScreen(
    crushId: Long,
    app: AntiGastosApplication,
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val vm: CrushResolveViewModel = viewModel(
        key = "crush_$crushId",
        factory = CrushResolveViewModel.factory(app, crushId),
    )
    val ui by vm.uiState.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val err by vm.error.collectAsState()

    LaunchedEffect(err) {
        val m = err ?: return@LaunchedEffect
        mainViewModel.postSnack(m)
        vm.consumeError()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Antojo en la heladera") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!loaded) {
                CircularProgressIndicator()
                return@Column
            }
            when (val s = ui) {
                null -> Text(
                    "Este pendiente ya no existe o ya lo resolviste.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                else -> {
                    Text(
                        "Pasaron 24 h. ¿Seguís queriendo cargarlo?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${MoneyFormat.formatPesos(s.amountPesos)} · ${s.categoryName}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    s.note?.takeIf { it.isNotBlank() }?.let { n ->
                        Text(
                            "Nota: $n",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "Si fue un impulso del momento, mandalo al tacho sin drama.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            vm.confirmLoad {
                                mainViewModel.postSnack("Listo, quedó cargado.")
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Sí, cargalo") }
                    OutlinedButton(
                        onClick = {
                            vm.discard {
                                mainViewModel.postSnack("Bien ahí. Quedó en la nada.")
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Me arrepentí, fuera") }
                }
            }
        }
    }
}
