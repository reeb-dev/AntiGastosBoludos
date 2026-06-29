package com.antigastos.boludos.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Gate biométrico con reintento si el usuario cancela el prompt.
 */
@Composable
fun BiometricGate(onSuccess: () -> Unit) {
    val ctx = LocalContext.current
    var attempt by remember { mutableIntStateOf(0) }
    var cancelled by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(attempt) {
        cancelled = false
        errorText = null
        val activity = ctx as? FragmentActivity ?: return@LaunchedEffect
        val canUse = BiometricManager.from(ctx)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canUse != BiometricManager.BIOMETRIC_SUCCESS) {
            onSuccess()
            return@LaunchedEffect
        }
        val executor = ContextCompat.getMainExecutor(ctx)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    cancelled = true
                    errorText = errString.toString()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Anti-gastos boludos")
            .setSubtitle("Confirmá con huella o cara")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!cancelled) {
                CircularProgressIndicator()
                Text("🔒 Desbloqueá con biometría", style = MaterialTheme.typography.titleMedium)
            } else {
                Text("🔒 App bloqueada", style = MaterialTheme.typography.titleMedium)
                errorText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                Button(onClick = { attempt += 1 }) { Text("Reintentar") }
                OutlinedButton(onClick = { (ctx as? FragmentActivity)?.finish() }) {
                    Text("Salir de la app")
                }
                Text(
                    "Para desactivar el bloqueo: Ajustes → Seguridad (cuando vuelvas a entrar).",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
