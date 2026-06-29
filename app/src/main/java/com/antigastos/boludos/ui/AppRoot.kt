package com.antigastos.boludos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.ui.auth.BiometricGate
import com.antigastos.boludos.ui.onboarding.OnboardingScreen
import com.antigastos.boludos.ui.quiz.PersonaQuizScreen
import com.antigastos.boludos.ui.splash.ArgSplash
import com.antigastos.boludos.ui.theme.AntiGastosTheme
import com.antigastos.boludos.ui.welcome.PersonaWelcomeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Punto de entrada Compose: splash → biometría opcional → onboarding → app.
 * Sin login: todo vive en Room en el teléfono.
 */
@Composable
fun AppRoot(
    mainViewModel: MainViewModel,
) {
    val app = LocalContext.current.applicationContext as AntiGastosApplication
    val settings by app.settingsRepository.flow.collectAsState(initial = SettingsEntity())
    val score by app.scoreRepository.observeScore().collectAsState(initial = null)
    val championOn = settings.championThemeEnabled && (score?.points ?: 0L) >= 150L
    val displayName = settings.userDisplayName.ifBlank { "Vecino" }

    AntiGastosTheme(championTheme = championOn) {
        AppRootContent(
            mainViewModel = mainViewModel,
            settings = settings,
            displayName = displayName,
        )
    }
}

@Composable
private fun AppRootContent(
    mainViewModel: MainViewModel,
    settings: SettingsEntity,
    displayName: String,
) {
    val app = LocalContext.current.applicationContext as AntiGastosApplication
    val scope = rememberCoroutineScope()
    val appLockRequested by mainViewModel.appLockRequested.collectAsState()

    var splashDone by remember { mutableStateOf(false) }
    var biometricPassed by remember { mutableStateOf(false) }
    var personaWelcomeShown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!app.skipSplashForTests) {
            delay(1600)
        }
        splashDone = true
    }

    LaunchedEffect(appLockRequested) {
        if (appLockRequested && settings.biometricLockEnabled) {
            biometricPassed = false
            mainViewModel.clearAppLockRequest()
        }
    }

    if (!splashDone) {
        ArgSplash()
        return
    }

    if (settings.biometricLockEnabled && !biometricPassed) {
        BiometricGate(onSuccess = { biometricPassed = true })
        return
    }

    if (!settings.onboardingDone) {
        OnboardingScreen(
            app = app,
            onDone = { scope.launch { app.settingsRepository.setOnboardingDone() } },
        )
        return
    }

    if (!settings.personaQuizDone) {
        PersonaQuizScreen(
            isFirstRun = true,
            onFirstRunComplete = { personaKey ->
                scope.launch { app.settingsRepository.markPersonaQuizDone(personaKey) }
            },
        )
        return
    }

    if (!settings.personaWelcomeShown && !personaWelcomeShown) {
        PersonaWelcomeScreen(
            personaKey = settings.personaKey,
            onContinue = {
                personaWelcomeShown = true
                scope.launch { app.settingsRepository.markPersonaWelcomeShown() }
            },
        )
        return
    }

    AntiGastosApp(
        mainViewModel = mainViewModel,
        displayName = displayName,
    )
}
