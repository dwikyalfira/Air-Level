package com.airwear.aqi

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.airwear.aqi.data.AqiRepository
import com.airwear.aqi.work.AqiRefreshWorker
import java.util.concurrent.TimeUnit

class AqiApp : Application(), Configuration.Provider {

    lateinit var repository: AqiRepository
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        try {
            WorkManager.initialize(this, workManagerConfiguration)
            repository = AqiRepository(this)
            scheduleRefreshAsync()
        } catch (t: Throwable) {
            Log.e(TAG, "App init failed", t)
            if (!::repository.isInitialized) {
                repository = AqiRepository(this)
            }
        }
    }

    private val mainHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

    private fun scheduleRefreshAsync() {
        mainHandler.postDelayed({
            runCatching {
                val request = PeriodicWorkRequestBuilder<AqiRefreshWorker>(
                    30, TimeUnit.MINUTES,
                    10, TimeUnit.MINUTES,
                )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()

                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    AqiRefreshWorker.WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
            }.onFailure { Log.w(TAG, "scheduleRefresh failed", it) }
        }, 1_500)
    }

    private companion object {
        const val TAG = "AirWear"
    }
}
