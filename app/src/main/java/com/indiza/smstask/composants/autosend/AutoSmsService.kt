package com.indiza.smstask.composants.autosend

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.indiza.smstask.ApiClient
import com.indiza.smstask.R
import com.indiza.smstask.tools.DataStoreManager
import com.indiza.smstask.tools.SmsApi
import com.indiza.smstask.viewmodel.SmsSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class AutoSmsService : LifecycleService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        startAutomaticSender()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        isRunning = false
    }

    private fun startForegroundServiceNotification() {
        val channelId = "sms_auto_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Envoi automatique des SMS",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Envoi automatique actif")
            .setContentText("L'app envoie les SMS en arrière-plan")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(1, notification)
    }

    private fun startAutomaticSender() {
        if (isRunning) return
        isRunning = true

        scope.launch {
            val api = getApiClient() ?: return@launch

            while (isRunning) {
                try {
                    val enabled = AutoSendDataStore(applicationContext).autoSendEnabled.first()
                    if (!enabled) {
                        delay(15000) // juste attendre si désactivé
                        continue
                    }

                    val pending = api.getPendingSms(50)
                    pending.forEach { sms ->
                        SmsSender.sendSmsHybrid(
                            context = applicationContext,
                            idSms = sms.idSms,
                            phoneNumber = sms.numeroDestinataire,
                            message = sms.contenuSMS,
                            simSlot = SmsSender.selectedSimSlot
                        )
                        delay(5500)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(15000)
            }
        }

    }
    private suspend fun getApiClient(): SmsApi? {
        val ds = DataStoreManager(applicationContext)
        val url = ds.baseUrl.first() // récupère l'URL actuelle
        if (url.isEmpty()) return null
        ApiClient.init(url)
        return ApiClient.api
    }

}

