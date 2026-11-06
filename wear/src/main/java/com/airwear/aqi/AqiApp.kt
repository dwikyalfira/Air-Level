package com.airwear.aqi

import android.app.Application
import android.util.Log
import com.airwear.aqi.data.AqiRepository
import com.airwear.aqi.work.RefreshScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AqiApp : Application() {

    lateinit var repository: AqiRepository
        private set

    /** Process-wide scope for fire-and-forget work that must outlive a screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        repository = AqiRepository(this)

        // WorkManager is initialised by androidx.startup — never call
        // WorkManager.initialize() here (it throws and used to skip scheduling).
        appScope.launch {
            runCatching {
                RefreshScheduler.schedule(this@AqiApp, repository.refreshMinutes.first())
            }.onFailure { Log.w(TAG, "scheduleRefresh failed", it) }
        }
    }

    private companion object {
        const val TAG = "AirWear"
    }
}
