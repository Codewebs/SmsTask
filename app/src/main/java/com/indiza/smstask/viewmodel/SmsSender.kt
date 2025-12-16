package com.indiza.smstask.viewmodel

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import android.annotation.SuppressLint
import android.content.ContentValues
import android.net.Uri

import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.indiza.smstask.tools.SimPreference
import com.indiza.smstask.viewmodel.SmsSender.selectedSimSlot
import java.lang.IllegalArgumentException

object SmsSender {

    private const val TAG = "SmsSender"
    var selectedSimSlot = -1
        internal set

    /**
     * Détecte le nombre de SIM disponibles
     */
    fun getAvailableSimSlots(context: Context): List<Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            getAvailableSimSlotsModern(context)
        } else {
            // Pour les anciennes versions, retourner juste la SIM 0
            listOf(0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @SuppressLint("MissingPermission")
    private fun getAvailableSimSlotsModern(context: Context): List<Int> {
        val subscriptionManager = context.getSystemService<SubscriptionManager>()
        val slots = mutableListOf<Int>()

        subscriptionManager?.let { manager ->
            try {
                val activeSubscriptions = manager.activeSubscriptionInfoList

                activeSubscriptions?.forEach { subscriptionInfo ->
                    // Pour Android 5.1+ (API 22)
                    val slotIndex = subscriptionInfo.simSlotIndex
                    if (slotIndex >= 0) {
                        slots.add(slotIndex)
                    }
                }

                // Trier et supprimer les doublons
                return slots.distinct().sorted()

            } catch (e: SecurityException) {
                Log.e(TAG, "Permission manquante pour lire les infos SIM", e)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur détection SIM", e)
            }
        }

        // Si on ne peut pas détecter, retourner au moins SIM 0
        return if (slots.isEmpty()) listOf(0) else slots
    }

    /**
     * Change la SIM sélectionnée
     */
    fun selectSimSlot(slot: Int) {
        selectedSimSlot = slot
        Log.d(TAG, "📱 SIM sélectionnée: slot $slot")
    }
    fun initFromPreferences(context: Context) {
        val saved = SimPreference.loadSelectedSim(context)
        selectedSimSlot = saved
        Log.d(ContentValues.TAG, "🔄 SMS Sender chargé avec la SIM $saved")
    }

    /**
     * Obtient le SmsManager pour la SIM sélectionnée
     */
    private fun getSmsManagerForSelectedSim(): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && selectedSimSlot >= 0) {
            try {
                SmsManager.getSmsManagerForSubscriptionId(getSubscriptionIdForSlot(selectedSimSlot))
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "SIM slot $selectedSimSlot invalide, utilisation par défaut")
                SmsManager.getDefault()
            }
        } else {
            SmsManager.getDefault()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @SuppressLint("MissingPermission")
    private fun getSubscriptionIdForSlot(slotIndex: Int): Int {
        // Retourner un ID de subscription valide pour le slot
        // En pratique, il faudrait récupérer depuis SubscriptionManager
        // Pour simplifier, on retourne slotIndex
        return slotIndex
    }

    /**
     * Récupère les infos des SIM disponibles
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @SuppressLint("MissingPermission")
    fun getSimInfoList(context: Context): List<SimInfo> {
        val simList = mutableListOf<SimInfo>()
        val subscriptionManager = context.getSystemService<SubscriptionManager>()

        subscriptionManager?.let { manager ->
            try {
                val subscriptions = manager.activeSubscriptionInfoList ?: return simList

                subscriptions.forEach { subscription ->
                    simList.add(
                        SimInfo(
                            slotIndex = subscription.simSlotIndex,
                            subscriptionId = subscription.subscriptionId,
                            displayName = subscription.displayName?.toString() ?: "SIM ${subscription.simSlotIndex + 1}",
                            carrierName = subscription.carrierName?.toString() ?: "Opérateur ${subscription.simSlotIndex + 1}",
                            number = subscription.number ?: "Numéro inconnu"
                        )
                    )
                }

                // Trier par slot
                return simList.sortedBy { it.slotIndex }

            } catch (e: SecurityException) {
                Log.e(TAG, "Permission manquante pour lire les infos SIM", e)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur récupération infos SIM", e)
            }
        }

        return simList
    }

    /**
     * Envoi SMS avec la SIM sélectionnée
     */
    fun sendSmsHybrid(
        context: Context,
        idSms: Long,
        phoneNumber: String,
        message: String,
        simSlot: Int = selectedSimSlot
    ) {
        try {
            val formattedNumber = formatPhoneNumberSimple(phoneNumber)
            Log.d(TAG, "📤 Envoi SMS $idSms à $formattedNumber (SIM: slot $simSlot)")

            // Utiliser la SIM spécifiée
            val tempSelectedSim = simSlot
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && tempSelectedSim >= 0) {
                try {
                    SmsManager.getSmsManagerForSubscriptionId(tempSelectedSim)
                } catch (e: Exception) {
                    Log.w(TAG, "Erreur sélection SIM, utilisation par défaut")
                    SmsManager.getDefault()
                }
            } else {
                SmsManager.getDefault()
            }

            val sentIntent = Intent("SMS_HYBRID_SENT").apply {
                putExtra("id", idSms)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }

            val sentPI = PendingIntent.getBroadcast(
                context,
                idSms.toInt(),
                sentIntent,
                flags
            )


            val parts = smsManager.divideMessage(message)
            val sentIntents = ArrayList<android.app.PendingIntent>().apply {
                repeat(parts.size) { add(sentPI) }
            }

            smsManager.sendMultipartTextMessage(
                formattedNumber,
                null,
                parts,
                sentIntents,
                null
            )

            Log.d(TAG, "✅ SMS $idSms envoyé avec SIM slot $simSlot")

        } catch (e: SecurityException) {
            Log.e(TAG, "🔒 Permission SMS manquante", e)
            markAsFailedImmediately(idSms, "Permission manquante")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "📞 Numéro invalide: '$phoneNumber'", e)
            markAsFailedImmediately(idSms, "Numéro invalide")
        } catch (e: Exception) {
            Log.e(TAG, "💥 Erreur inattendue", e)
            markAsFailedImmediately(idSms, "Erreur: ${e.message}")
        }
    }
    private fun markAsFailedImmediately(idSms: Long, reason: String) {
        Log.e(TAG, "❌ Échec avant envoi SMS $idSms: $reason")
    }

    private fun formatPhoneNumberSimple(rawNumber: String): String {
        val cleaned = rawNumber.replace(Regex("[^0-9+]"), "")
        return when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("237") -> "+$cleaned"
            cleaned.startsWith("0") && cleaned.length >= 10 -> "+237${cleaned.substring(1)}"
            cleaned.length == 9 -> "+237$cleaned"
            else -> cleaned
        }
    }
}



data class SimInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val number: String
) {
    val displayText: String
        get() = "$displayName ($carrierName)"
}

class HybridSentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val idSms = intent.getLongExtra("id", -1L)
        val success = intent.getBooleanExtra("success", false)

        Log.d("HybridReceiver", "📨 Manifest Receiver -> id=$idSms success=$success")
    }
}
class HybridSentCallbackReceiver(
    private val handler: (Long, Boolean) -> Unit
) : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {

        val id = intent.getLongExtra("id", -1L)
        val success = resultCode == Activity.RESULT_OK

        handler(id, success)
    }
}
