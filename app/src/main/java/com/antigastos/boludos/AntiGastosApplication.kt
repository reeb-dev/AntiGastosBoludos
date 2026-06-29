package com.antigastos.boludos

import android.app.Application
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.antigastos.boludos.data.AchievementsRepository
import com.antigastos.boludos.data.ChatRepository
import com.antigastos.boludos.data.ExpenseRepository
import com.antigastos.boludos.data.FirebaseAiClient
import com.antigastos.boludos.data.GeminiApi
import com.antigastos.boludos.data.GeminiCredentials
import com.antigastos.boludos.data.GifRepository
import com.antigastos.boludos.data.MemeApi
import com.antigastos.boludos.data.PactRepository
import com.antigastos.boludos.data.ScoreRepository
import com.antigastos.boludos.data.SettingsRepository
import com.antigastos.boludos.data.local.AppDatabase
import com.antigastos.boludos.domain.AiCopyOrchestrator
import com.antigastos.boludos.domain.PersonaChatEngine
import com.antigastos.boludos.domain.WeeklyChallengeSeeder
import com.antigastos.boludos.notifications.Notifier
import com.antigastos.boludos.notifications.StreakStatusNotifier
import com.antigastos.boludos.voice.TtsSpeaker
import com.antigastos.boludos.work.WorkSchedulers
import com.antigastos.boludos.ads.AdHelper
import com.antigastos.boludos.ads.AdMonetizationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AntiGastosApplication : Application(), ImageLoaderFactory {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.create(this) }
    val scoreRepository by lazy { ScoreRepository(database) }
    val repository by lazy {
        ExpenseRepository(database, scoreRepository = scoreRepository)
    }
    val settingsRepository by lazy { SettingsRepository(database) }
    val achievementsRepository by lazy { AchievementsRepository(database) }
    val pactRepository by lazy { PactRepository(database) }
    val gifRepository by lazy { GifRepository() }
    val memeApi by lazy { MemeApi() }
    val tts by lazy { TtsSpeaker(this) }
    val chatRepository by lazy { ChatRepository(database) }
    val geminiApi by lazy { GeminiApi() }
    val firebaseAiClient by lazy {
        FirebaseAiClient().also { GeminiCredentials.firebaseAiReady = it.isReady }
    }
    val aiQuota by lazy { com.antigastos.boludos.data.AiQuota(this) }
    val personaChatEngine by lazy { PersonaChatEngine(geminiApi, firebaseAiClient) }
    val aiCopyOrchestrator by lazy { AiCopyOrchestrator(personaChatEngine, aiQuota) }

    @Volatile
    var lastInteractionAt: Long = System.currentTimeMillis()
        private set

    fun touchInteraction() {
        lastInteractionAt = System.currentTimeMillis()
    }

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)

        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (t: Throwable) {
            Log.i("AntiGastosApp", "Firebase no inicializado.")
        }

        AdMonetizationTracker.attach(this)

        applicationScope.launch {
            repository.ensureCategoriesSeeded()
            settingsRepository.ensureDefaults()
            settingsRepository.maybeRotatePersonaOnLaunch()
            repository.seedSubscriptionsForCurrentMonth()
            achievementsRepository.refreshAll(repository)
            scoreRepository.snapshot()
            repository.setExpensesChangedListener {
                applicationScope.launch {
                    StreakStatusNotifier.refresh(this@AntiGastosApplication)
                }
            }
            WeeklyChallengeSeeder.maybeSeed(this@AntiGastosApplication)
            val s = settingsRepository.getSnapshot()
            WorkSchedulers.applyFromSettings(this@AntiGastosApplication, s)
            StreakStatusNotifier.refresh(this@AntiGastosApplication)
        }
    }

    /** Inicializa ads tras consentimiento (llamar desde MainActivity). */
    fun initAdsAfterConsent() {
        com.google.android.gms.ads.MobileAds.initialize(this) {}
        AdHelper.preloadAllRewarded(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
}
