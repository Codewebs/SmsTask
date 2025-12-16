package com.indiza.smstask.viewmodel


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.indiza.smstask.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeliveredReceiver : BroadcastReceiver() {

    private val TAG = "DeliveredReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val idSms = intent.getLongExtra("idSms", -1L)
        val resultCode = this.resultCode

        when (resultCode) {
            android.app.Activity.RESULT_OK -> {
                Log.d(TAG, "📬 SMS $idSms délivré au destinataire")
                // Optionnel: marquer comme délivré dans l'API
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ApiClient.api.markSmsAsDelivered(idSms)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur API pour marquer comme délivré", e)
                    }
                }
            }
            else -> {
                Log.d(TAG, "📭 SMS $idSms non délivré (code: $resultCode)")
            }
        }
    }
}