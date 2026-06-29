package com.antigastos.boludos.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.PersonaCatalog
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * Onboarding 3 pasos: bienvenida → personaje → sueldo (gastos fijos opcional
 * en la misma pantalla, "lo cargás después" si no se completa).
 *
 * Reducido de 4 a 3 pasos para que el primer uso no se sienta un trámite
 * y el usuario pueda llegar al Home con datos mínimos.
 */
@Composable
fun OnboardingScreen(
    app: AntiGastosApplication,
    onDone: () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var salary by remember { mutableStateOf("") }
    var fixed by remember { mutableStateOf("") }
    val settings by app.settingsRepository.flow.collectAsState(
        initial = com.antigastos.boludos.data.local.entity.SettingsEntity(),
    )
    val scope = rememberCoroutineScope()

    val totalSteps = 3

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LinearProgressIndicator(
                progress = { (step + 1) / totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )

            when (step) {
                0 -> Step0Welcome()
                1 -> Step1Persona(
                    current = settings.personaKey,
                    onPick = { key ->
                        scope.launch { app.settingsRepository.update { it.copy(personaKey = key) } }
                    },
                )
                2 -> Step2SalaryAndFixed(
                    salary = salary,
                    fixed = fixed,
                    onSalaryChange = { salary = it },
                    onFixedChange = { fixed = it },
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) { Text("Atrás") }
                }
                Button(
                    onClick = {
                        if (step < totalSteps - 1) {
                            step += 1
                        } else {
                            scope.launch {
                                val s = salary.toLongOrNull() ?: 0L
                                val f = fixed.toLongOrNull() ?: 0L
                                app.repository.upsertBudget(
                                    yearMonth = YearMonth.now(),
                                    salaryPesos = s,
                                    otherIncomePesos = 0L,
                                    fixedExpensesPesos = f,
                                )
                                app.settingsRepository.setOnboardingDone()
                                onDone()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(if (step > 0) 2f else 1f)
                        .height(52.dp),
                ) { Text(if (step < totalSteps - 1) "Siguiente" else "Listo, vamos") }
            }
        }
    }
}

@Composable
private fun Step0Welcome() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("¡Bienvenido!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Esto no es una app de finanzas. Es un amigo argentino que te va a romper las pelotas cada vez que gastes en boludeces.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "Te va a juzgar, te va a cargar, te va a felicitar (capaz) y te va a tirar consejos falopa.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text("🧉 Sin nubes, sin chamuyo, todo local.")
    }
}

@Composable
private fun Step1Persona(current: String, onPick: (String) -> Unit) {
    LaunchedEffect(Unit) {
        val pool = PersonaCatalog.all.filter { it.key != current }
        val pick = pool.randomOrNull() ?: PersonaCatalog.all.first()
        onPick(pick.key)
    }
    val persona = PersonaCatalog.byKey(current)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "Te toca tu Pepe",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Sorteamos uno al azar entre todos los personajes. Si no te convence, dale al dado.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(persona.emoji, style = MaterialTheme.typography.displayMedium)
                }
                Text(
                    persona.displayName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    persona.tagline,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Te va a decir cosas como:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                persona.samples.take(2).forEach { sample ->
                    Text("• $sample", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        OutlinedButton(
            onClick = {
                val pool = PersonaCatalog.all.filter { it.key != current }
                val pick = pool.randomOrNull() ?: PersonaCatalog.all.first()
                onPick(pick.key)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("🎲 Sortear otro personaje")
        }
    }
}

@Composable
private fun Step2SalaryAndFixed(
    salary: String,
    fixed: String,
    onSalaryChange: (String) -> Unit,
    onFixedChange: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "¿Cuánto entra al mes?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text("Tu sueldo neto en pesos. Si tenés extras los sumás después en Bolsillo.")
        OutlinedTextField(
            value = salary,
            onValueChange = { onSalaryChange(it.filter(Char::isDigit).take(12)) },
            label = { Text("Sueldo (pesos)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        salary.toLongOrNull()?.takeIf { it > 0 }?.let {
            Text(
                "${MoneyFormat.formatPesos(it)} · ~${MoneyFormat.pesosToLucas(it)} lucas",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Gastos fijos (opcional)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Alquiler + servicios + suscripciones. Lo que sí o sí pagás cada mes. " +
                "Si todavía no los tenés a mano, dejalo vacío y lo cargás después.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = fixed,
            onValueChange = { onFixedChange(it.filter(Char::isDigit).take(12)) },
            label = { Text("Gastos fijos (pesos)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
