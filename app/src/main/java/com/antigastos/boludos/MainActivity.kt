package com.antigastos.boludos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.antigastos.boludos.notifications.Notifier
import com.antigastos.boludos.notifications.StreakStatusNotifier
import com.antigastos.boludos.ui.AppRoot
import com.antigastos.boludos.ui.MainViewModel
import com.antigastos.boludos.ui.navigation.Routes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * `MainActivity` ahora solo se ocupa del lifecycle de Android:
 * permisos de notificación, deeplinks/intents, auto-lock por inactividad
 * y montar `AppRoot` como contenido Compose.
 *
 * Toda la UI (splash, shell con NavHost, overlays) vive bajo
 * `ui/AppRoot.kt` y `ui/AntiGastosApp.kt`.
 */
class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* el resultado no nos detiene; si no concede, las notis simplemente no salen */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        consumeRouteIntent(intent)
        val app = applicationContext as AntiGastosApplication
        com.antigastos.boludos.ads.AdConsent.requestAndInit(this) {
            app.initAdsAfterConsent()
        }
        setContent {
            AppRoot(mainViewModel = mainViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        val app = applicationContext as? AntiGastosApplication ?: return
        // Auto-lock: si pasaron N minutos, pedir biometría de nuevo (si está activa).
        app.applicationScope.launch {
            val s = app.settingsRepository.flow.first()
            val minutes = s.autoLockMinutes
            val elapsedMin = (System.currentTimeMillis() - app.lastInteractionAt) / 60000L
            if (s.biometricLockEnabled && minutes > 0 && elapsedMin >= minutes) {
                mainViewModel.requestAppLock()
            }
        }
        lifecycleScope.launch {
            StreakStatusNotifier.refresh(app)
        }
    }

    override fun onPause() {
        super.onPause()
        (applicationContext as? AntiGastosApplication)?.touchInteraction()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeRouteIntent(intent)
    }

    private fun consumeRouteIntent(intent: Intent?) {
        intent?.getStringExtra(Routes.EXTRA_OPEN_ROUTE)?.takeIf { it.isNotBlank() }?.let {
            mainViewModel.postPendingRoute(it)
        }
        consumeDeeplinkIntent(intent)
    }

    private fun consumeDeeplinkIntent(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme == "antigastos" && data.host == "crush") {
            val idStr = data.pathSegments.lastOrNull()
            val id = idStr?.toLongOrNull()
            if (id != null) {
                mainViewModel.postPendingRoute(Routes.crushResolve(id))
            }
            return
        }
        val route = when (data.toString()) {
            Notifier.DEEPLINK_HOME -> Routes.HOME
            Notifier.DEEPLINK_LIST -> Routes.LIST
            Notifier.DEEPLINK_RECAP -> Routes.RECAP
            Notifier.DEEPLINK_ACHIEVEMENTS -> Routes.ACHIEVEMENTS
            Notifier.DEEPLINK_LOTERIA -> Routes.LOTERIA
            else -> null
        } ?: return
        mainViewModel.postPendingRoute(route)
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
