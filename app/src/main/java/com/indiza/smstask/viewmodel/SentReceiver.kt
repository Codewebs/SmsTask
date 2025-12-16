package com.indiza.smstask.viewmodel


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.indiza.smstask.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SentReceiver : BroadcastReceiver() {

    private val TAG = "SentReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val idSms = intent.getLongExtra("idSms", -1L)
        val phoneNumber = intent.getStringExtra("phoneNumber") ?: "inconnu"
        val resultCode = this.resultCode

        Log.d(TAG, "📨 Réception callback pour SMS $idSms (numéro: $phoneNumber)")
        Log.d(TAG, "📊 Code résultat: $resultCode")

        if (idSms <= 0L) {
            Log.e(TAG, "❌ ID SMS invalide")
            return
        }

        // Traiter dans une coroutine pour éviter de bloquer le thread UI
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (resultCode) {
                    // Succès
                    android.app.Activity.RESULT_OK -> {
                        Log.d(TAG, "✅ SMS $idSms envoyé avec succès")
                        // Marquer comme envoyé dans l'API
                        ApiClient.api.markSmsAsSent(idSms)
                    }

                    // Échec - codes d'erreur standards
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                        Log.e(TAG, "❌ SMS $idSms: Erreur générique")
                        ApiClient.api.markSmsAsFailed(idSms)
                    }

                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
                        Log.e(TAG, "❌ SMS $idSms: Pas de service réseau")
                        ApiClient.api.markSmsAsFailed(idSms)
                    }

                    SmsManager.RESULT_ERROR_NULL_PDU -> {
                        Log.e(TAG, "❌ SMS $idSms: PDU nul")
                        ApiClient.api.markSmsAsFailed(idSms)
                    }

                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
                        Log.e(TAG, "❌ SMS $idSms: Radio éteinte")
                        ApiClient.api.markSmsAsFailed(idSms)
                    }

                    // Autres erreurs
                    else -> {
                        Log.e(TAG, "❌ SMS $idSms: Erreur inconnue (code: $resultCode)")
                        ApiClient.api.markSmsAsFailed(idSms)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de la mise à jour de l'API", e)
            }
        }
    }
}