package com.antigastos.boludos.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.antigastos.boludos.ads.AdConsent
import androidx.core.content.ContextCompat
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.domain.LifeHoursEngine
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.PersonaCatalog
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onDonate: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenLoteria: () -> Unit = {},
    onOpenQuiz: () -> Unit = {},
    onOpenRecap: () -> Unit = {},
    onOpenPact: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
) {
    val s by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as AntiGastosApplication
    val score by app.scoreRepository.observeScore().collectAsState(initial = null)
    val firebaseAiReady = remember { app.firebaseAiClient.isReady }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val notiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { snackbar.showSnackbar(it) }
    }

    var showWipe by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        SectionCard("📱 Datos en el teléfono") {
            Text(
                "Tus gastos y preferencias viven solo en este dispositivo. " +
                    "No hay cuenta ni sincronización en la nube.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        SectionCard("👋 Tu nombre") {
            Text(
                "Así te saludamos en el Inicio. Queda guardado solo en este teléfono.",
                style = MaterialTheme.typography.bodySmall,
            )
            var nameTxt by remember(s.userDisplayName) { mutableStateOf(s.userDisplayName) }
            OutlinedTextField(
                value = nameTxt,
                onValueChange = { nameTxt = it.take(24) },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.setUserDisplayName(nameTxt) },
                enabled = nameTxt.trim() != s.userDisplayName.trim(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar nombre") }
        }

        SectionCard("🎭 Personaje") {
            Text(
                "Quién te habla en la app. Cada personaje te dice las cosas a su manera.",
                style = MaterialTheme.typography.bodySmall,
            )
            ToggleRow(
                "Cambiar personaje cada vez que abro la app",
                "Si está prendido, te saluda y te habla un personaje distinto en cada apertura. Apagado: queda fijo el que elegís acá abajo.",
                s.personaRotateEnabled,
            ) { viewModel.togglePersonaRotation(it) }
            if (s.personaRotateEnabled) {
                OutlinedButton(
                    onClick = { viewModel.rotatePersonaNow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) { Text("Sortear otro personaje ahora") }
            }
            Text(
                if (s.personaRotateEnabled) "Hoy te habla:" else "Personaje fijo:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
            ) {
                items(PersonaCatalog.all, key = { it.key }) { persona ->
                    val selected = persona.key == s.personaKey
                    Card(
                        onClick = { viewModel.setPersonaKey(persona.key) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                    ) {
                        Column(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(persona.emoji, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                persona.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            val current = PersonaCatalog.byKey(s.personaKey)
            Text(
                "${current.emoji} ${current.tagline}",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = onOpenQuiz,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text("🧠 Volver a hacer el quiz de personaje") }
        }

        SectionCard("✨ Extras") {
            Text(
                "Atajos a las funciones que están escondidas fuera de la barra inferior.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = onOpenLoteria,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text("🎁 Regalo del día (frase / sticker / escudo)") }
            OutlinedButton(
                onClick = onOpenRecap,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text("📊 Recap del mes") }
            OutlinedButton(
                onClick = onOpenPact,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text("🤝 Modo pacto (no gastar en X)") }
            OutlinedButton(
                onClick = onOpenCategories,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text("🏷️ Categorías de gastos") }
            OutlinedButton(
                onClick = { viewModel.resetHomeTips() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text("ℹ️ Volver a mostrar los tips del Inicio") }
        }

        SectionCard("🎨 Emoticones") {
            ToggleRow(
                "Solo emoticones locales",
                "Apaga GIFs y memes de internet. Mostramos un emoji grande estilo sticker (mate 🧉, asado 🥩, pelota ⚽, billetera 💸, fuego 🔥…) con animación suave.",
                s.localStickersOnly,
            ) { viewModel.toggleLocalStickersOnly(it) }
        }

        var aiHeaderTaps by remember { mutableStateOf(0) }
        TappableSectionCard(
            title = "🤖 Frases con IA",
            onTitleTap = {
                aiHeaderTaps += 1
                if (aiHeaderTaps >= 7 && !s.developerMode) {
                    viewModel.enableDeveloperMode()
                    scope.launch { snackbar.showSnackbar("🛠 Modo dev activado") }
                }
            },
        ) {
            val bundled = com.antigastos.boludos.data.GeminiCredentials.isBundled
            val aiOnline = firebaseAiReady || bundled || s.geminiApiKey.isNotBlank()
            val remaining = remember(s.aiPhrasesEnabled) { app.aiCopyOrchestrator.remainingDailyQuota() }

            if (aiOnline) {
                Text(
                    "✅ La IA ya está activa y es gratis. Si un día se pasa del cupo libre, " +
                        "la app sigue funcionando con el catálogo de frases incluido.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    "La IA no está disponible en este dispositivo. La app igual te sirve: " +
                        "cada personaje tiene cientos de frases listas sin internet.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            ToggleRow(
                "Frases generadas con IA",
                "Si está apagado, la app usa el catálogo local (siempre disponible y sin red).",
                s.aiPhrasesEnabled,
            ) { viewModel.toggleAiPhrases(it) }

            if (s.aiPhrasesEnabled && aiOnline) {
                Text(
                    "Frases IA disponibles hoy: $remaining (después caemos al catálogo local).",
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            OutlinedButton(
                onClick = { viewModel.testGemini() },
                enabled = s.aiPhrasesEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Probar IA") }

            if (s.developerMode) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "🛠 Modo desarrollador",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                var apiKey by remember(s.geminiApiKey) { mutableStateOf(s.geminiApiKey) }
                var revealKey by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it.take(120) },
                    label = { Text("API key custom (opcional)") },
                    placeholder = { Text("AIza…") },
                    singleLine = true,
                    visualTransformation = if (revealKey) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        androidx.compose.material3.TextButton(onClick = { revealKey = !revealKey }) {
                            Text(if (revealKey) "ocultar" else "ver")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.setGeminiApiKey(apiKey) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Guardar key") }
                    OutlinedButton(
                        onClick = { viewModel.disableDeveloperMode() },
                        modifier = Modifier.weight(1f),
                    ) { Text("Salir dev") }
                }
            }

            Text(
                "Lo que pegás en el campo de IA va directo a Google. La app no lo guarda en ningún server.",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        SectionCard("🔊 Voz y sonidos") {
            ToggleRow(
                "Leer la frase en voz alta",
                "Botón 'Hablar' en cada frase. Usa la voz del sistema (es-AR si está).",
                s.ttsEnabled,
            ) { viewModel.toggleTts(it) }
            ToggleRow(
                "Sonidos de la app",
                "Pequeños efectos al votar y al cargar gastos.",
                s.soundEnabled,
            ) { viewModel.toggleSound(it) }
            OutlinedButton(
                onClick = { app.tts.speak("Probando uno dos. ¿Me escuchás, hermano?") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Probar voz") }
        }

        SectionCard("⏱ Plata = tiempo") {
            Text(
                "Traducimos pesos a horas de laburo con tu sueldo y tus horas semanales. " +
                    "Sirve para sentir el gasto en la panza, no solo en el bolsillo.",
                style = MaterialTheme.typography.bodySmall,
            )
            ToggleRow(
                "Mostrar “horas de tu vida”",
                "En el Home y al cargar un gasto, si configuraste sueldo y horas.",
                s.lifeHoursEnabled,
            ) { viewModel.toggleLifeHours(it) }
            var monthlyTxt by remember(s.monthlyNetIncomePesos) {
                mutableStateOf(
                    if (s.monthlyNetIncomePesos > 0L) s.monthlyNetIncomePesos.toString() else "",
                )
            }
            OutlinedTextField(
                value = monthlyTxt,
                onValueChange = { raw ->
                    monthlyTxt = raw.filter { it.isDigit() }.take(12)
                    viewModel.setMonthlyNetIncomePesos(monthlyTxt.toLongOrNull() ?: 0L)
                },
                label = { Text("Sueldo neto mensual (ARS)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Horas de laburo por semana: ${s.weeklyWorkHours} h",
                style = MaterialTheme.typography.bodySmall,
            )
            Slider(
                value = s.weeklyWorkHours.toFloat(),
                onValueChange = { viewModel.setWeeklyWorkHours(it.toInt()) },
                valueRange = 5f..60f,
                steps = 54,
            )
            val rate = LifeHoursEngine.hourlyRatePesos(s.monthlyNetIncomePesos, s.weeklyWorkHours)
            if (rate != null && s.monthlyNetIncomePesos > 0L) {
                Text(
                    "Tarifa estimada: ${LifeHoursEngine.formatHourlyRateLabel(rate)} " +
                        "(${LifeHoursEngine.formatLifeHours(10_000L, rate)} por cada diez lucas).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            ToggleRow(
                "Proyectar ritmo en el calendario del Home",
                "Los días que faltan del mes se tiñen con tu promedio diario hasta hoy (más claros).",
                s.heatmapPaceProjectionEnabled,
            ) { viewModel.toggleHeatmapPaceProjection(it) }
            ToggleRow(
                "Sugerir gastos fijos repetidos",
                "Si cargás a mano lo mismo en varios meses, te avisamos para pasarlo a Suscripciones.",
                s.phantomRecurringHintsEnabled,
            ) { viewModel.togglePhantomRecurringHints(it) }
        }

        SectionCard("🧊 Antojo 24 h") {
            Text(
                "En Delivery, Salidas y Boludeces, si el monto supera el mínimo te ofrecemos " +
                    "mandar el gasto a la heladera: te avisamos a las 24 h para confirmar o largarlo.",
                style = MaterialTheme.typography.bodySmall,
            )
            ToggleRow(
                "Activar modo heladera",
                "Solo en gastos nuevos (no al editar uno viejo).",
                s.crushCooldownEnabled,
            ) { enabled ->
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val ok = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!ok) {
                        notiPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                viewModel.toggleCrushCooldown(enabled)
            }
            var minCrushTxt by remember(s.crushMinPesos) {
                mutableStateOf(s.crushMinPesos.toString())
            }
            OutlinedTextField(
                value = minCrushTxt,
                onValueChange = { raw ->
                    minCrushTxt = raw.filter { it.isDigit() }.take(9)
                    val v = minCrushTxt.toLongOrNull() ?: 15_000L
                    viewModel.setCrushMinPesos(v)
                },
                label = { Text("Mínimo en pesos para ofrecer heladera") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Ejemplo: con ${MoneyFormat.formatPesos(s.crushMinPesos)} en delivery te va a frenar un segundo.",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        SectionCard("🔔 Avisos") {
            ToggleRow(
                "Recordatorio diario",
                "Te aviso para que cargues los gastos del día (con guiños si es feriado, viernes o arranque de mes).",
                s.dailyReminderEnabled,
            ) { viewModel.toggleDailyReminder(it) }
            ToggleRow(
                "Aviso de racha (~22:00)",
                "Te recuerda cerrar el día sin perder el hábito ni la racha de no gastar.",
                s.streakNudgeEnabled,
            ) { viewModel.toggleStreakNudge(it) }
            ToggleRow(
                "Notificación fija de racha (+ atajos)",
                "Queda una noti en la barra con la racha y botones +5 mil / +10 mil. En Android 13+ puede pedir permiso.",
                s.persistStreakNotification,
            ) { enabled ->
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val ok = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!ok) {
                        notiPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                viewModel.togglePersistStreakNotification(enabled)
            }
            ToggleRow(
                "Alertas de presupuesto",
                "Te aviso si pasás 80% de la meta o si vas para romperla.",
                s.budgetAlertsEnabled,
            ) { viewModel.toggleBudgetAlerts(it) }
            Text(
                "Hora del recordatorio: ${"%02d:00".format(s.dailyReminderHour)}",
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(9, 13, 18, 21).forEach { h ->
                    OutlinedButton(onClick = { viewModel.setReminderHour(h) }) {
                        Text("${h}:00")
                    }
                }
            }
        }

        SectionCard("🎨 Apariencia") {
            val pts = score?.points ?: 0L
            val unlocked = pts >= 150L
            ToggleRow(
                "Tema campeón (dorado)",
                if (unlocked) {
                    "Paleta dorada para campeones del ahorro. Tenés $pts pts."
                } else {
                    "Se desbloquea al llegar a 150 pts. Vas $pts pts."
                },
                s.championThemeEnabled,
            ) { viewModel.toggleChampionTheme(it) }
        }

        SectionCard("👥 Modo pareja (local)") {
            Text(
                "Cuando lo prendas, en el editor de gastos vas a tener un toggle para marcar un ítem como compartido. " +
                    "El monto se reparte en el % que elijas y en Bolsillo te aparece cuánto te debe.",
                style = MaterialTheme.typography.bodySmall,
            )
            ToggleRow(
                "Activar modo pareja",
                "Es local: no sincroniza nada, solo lleva las cuentas en este teléfono.",
                s.partnerEnabled,
            ) { viewModel.togglePartner(it) }
            if (s.partnerEnabled) {
                var partnerName by remember(s.partnerName) { mutableStateOf(s.partnerName) }
                OutlinedTextField(
                    value = partnerName,
                    onValueChange = { partnerName = it.take(20) },
                    label = { Text("Nombre de tu pareja") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { viewModel.setPartnerName(partnerName) },
                    enabled = partnerName.trim().isNotBlank() && partnerName != s.partnerName,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Guardar nombre")
                }
                Text(
                    "Split por defecto: vos ${s.partnerDefaultSplit}% · pareja ${100 - s.partnerDefaultSplit}%",
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(
                    value = s.partnerDefaultSplit.toFloat(),
                    onValueChange = { viewModel.setPartnerDefaultSplit(it.toInt()) },
                    valueRange = 1f..99f,
                    steps = 97,
                )
            }
        }

        SectionCard("🔒 Seguridad") {
            ToggleRow(
                "Lock biométrico",
                "Pide huella o cara al abrir la app.",
                s.biometricLockEnabled,
            ) { viewModel.toggleBiometric(it) }
            Text("Auto-lock: ${s.autoLockMinutes} min", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 5, 15, 60).forEach { m ->
                    OutlinedButton(onClick = { viewModel.setAutoLockMinutes(m) }) {
                        Text("${m}m")
                    }
                }
            }
        }

        SectionCard("💵 Dólar") {
            var rate by remember(s.usdRate) {
                mutableStateOf(if (s.usdRate > 0) s.usdRate.toString() else "")
            }
            Text(
                "Pegá la cotización del dólar (lo que vale 1 USD en pesos). " +
                    "Va a aparecer al lado del 'te queda' en Bolsillo.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(10) },
                label = { Text("Cotización USD (pesos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val v = rate.replace(',', '.').toDoubleOrNull() ?: 0.0
                    viewModel.setUsdRate(v)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar cotización") }
        }

        SectionCard("☕ Doname un cafecito") {
            Text(
                "Si la app te ayudó, podés invitarme un cafecito por Mercado Pago. " +
                    "Es 100% opcional y no cambia la publicidad.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = onDonate,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Donar un cafecito") }
        }

        SectionCard("📺 Publicidad") {
            Text(
                "Banners discretos y videos opcionales en algunas pantallas.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (s.developerMode) {
                val snap = com.antigastos.boludos.domain.MonetizationEstimator.snapshot(s)
                com.antigastos.boludos.ui.donate.MonetizationEarningsCard(snap)
            }
        }

        SectionCard("🔒 Privacidad") {
            Text(
                "La app guarda tus datos en el teléfono. Los anuncios pueden usar identificadores " +
                    "del dispositivo según tu consentimiento (UMP).",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = onOpenPrivacy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Ver política de privacidad") }
            val activity = context as? FragmentActivity
            if (activity != null && AdConsent.isPrivacyOptionsRequired(activity)) {
                OutlinedButton(
                    onClick = { AdConsent.showPrivacyOptions(activity) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Gestionar consentimiento de anuncios") }
            }
        }

        SectionCard("⚠️ Zona peligrosa") {
            OutlinedButton(
                onClick = { showWipe = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Borrar todos los gastos") }
        }

        SnackbarHost(snackbar)
        Spacer(Modifier.height(48.dp))
    }

    if (showWipe) {
        AlertDialog(
            onDismissRequest = { showWipe = false },
            title = { Text("¿Estás seguro?") },
            text = { Text("Esto borra TODOS los gastos. Las categorías vuelven a las default.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteAllData {}
                    showWipe = false
                }) { Text("Borrar todo") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWipe = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun TappableSectionCard(
    title: String,
    onTitleTap: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTitleTap),
            )
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}
