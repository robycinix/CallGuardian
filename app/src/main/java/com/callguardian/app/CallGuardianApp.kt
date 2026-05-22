package com.callguardian.app

import android.app.Application
import com.callguardian.app.data.repository.GuardianRepository
import com.callguardian.app.telephony.NotificationHelper
import com.callguardian.app.worker.LogMaintenanceScheduler
import dagger.hilt.android.HiltAndroidApp
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class CallGuardianApp : Application(), Configuration.Provider {
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var logMaintenanceScheduler: LogMaintenanceScheduler
    @Inject lateinit var repository: GuardianRepository
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        logMaintenanceScheduler.schedule()
        appScope.launch { repository.initialize() }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
