package com.indiza.smstask.viewmodel

import com.indiza.smstask.tools.NetworkUtils

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.smstask.ApiClient
import com.indiza.smstask.composants.autosend.AutoSendDataStore
import com.indiza.smstask.tools.DataStoreManager
import com.indiza.smstask.tools.MessageStatus
import com.indiza.smstask.tools.PendingMessage
import com.indiza.smstask.tools.RecentMessage
import com.indiza.smstask.tools.SimPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.api

    private val _isNetworkOk = MutableStateFlow(true)
    val isNetworkOk: StateFlow<Boolean> = _isNetworkOk.asStateFlow()

    private val _autoSendEnabled = MutableStateFlow(false)
    val autoSendEnabled: StateFlow<Boolean> = _autoSendEnabled.asStateFlow()

    private val _lastSync = MutableStateFlow("Jamais")
    val lastSync: StateFlow<String> = _lastSync.asStateFlow()

    // Messages en attente d'envoi
    private val _pendingMessages = MutableStateFlow<List<PendingMessage>>(emptyList())
    val pendingMessages: StateFlow<List<PendingMessage>> = _pendingMessages.asStateFlow()

    // Messages récents (pour le dashboard)
    private val _recentMessages = MutableStateFlow<List<RecentMessage>>(emptyList())
    val recentMessages: StateFlow<List<RecentMessage>> = _recentMessages.asStateFlow()

    // États UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // File d'attente des synchronisations en attente
    private val _syncQueue = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val syncQueue: StateFlow<Map<Long, Boolean>> = _syncQueue.asStateFlow()

    // Nombre de tentatives par SMS
    private val _retryCounts = mutableMapOf<Long, Int>()
    private val MAX_RETRIES = 3

    // SIM disponible
    private val _availableSimSlots = MutableStateFlow<List<Int>>(emptyList())
    val availableSimSlots: StateFlow<List<Int>> = _availableSimSlots.asStateFlow()

    // SIM sélectionnée (-1 = défaut système)
    private val _selectedSimSlot = MutableStateFlow(-1)
    val selectedSimSlot: StateFlow<Int> = _selectedSimSlot.asStateFlow()

    // Infos détaillées des SIM
    private val _simInfoList = MutableStateFlow<List<SimInfo>>(emptyList())
    val simInfoList: StateFlow<List<SimInfo>> = _simInfoList.asStateFlow()

    private val autoSendDataStore = AutoSendDataStore(getApplication())

    private val _failedMessages = MutableStateFlow<List<PendingMessage>>(emptyList())
    val failedMessages: StateFlow<List<PendingMessage>> = _failedMessages.asStateFlow()

    init {
        // Vérifier l'état réseau initial
        checkNetworkStatus()

        // Vérifier périodiquement l'état réseau (toutes les 30 secondes)
        viewModelScope.launch {
            while (true) {
                delay(30000) // 30 secondes
                checkNetworkStatus()
            }
        }

        loadPendingMessages()
        loadRecentMessages()

        // Vérifier périodiquement la file d'attente
        startSyncQueueChecker()
        detectAvailableSims()

        viewModelScope.launch {
            autoSendDataStore.autoSendEnabled.collect { enabled ->
                _autoSendEnabled.value = enabled
                // Si activé, lancer l'envoi automatique
                if (enabled) {
                    sendAllPendingMessagesHybrid()
                }
            }
        }

    }




    fun swipeDeleteMessage(idSms: Long) {
        println(" ------------------------------ ")
        println(" ------------------------------ ")
        println(" ------------------------------ ")
        viewModelScope.launch {
            try {
                api.markSmsAsSwiped(idSms)
                removeSentMessage(idSms)  // tu l'as déjà !
                println("🧹 SMS $idSms supprimé par swipe")
            } catch (e: Exception) {
                println("❌ Erreur suppression swipe: ${e.message}")
            }
        }
    }


    /**
     * Détecter les SIM disponibles
     */
    fun detectAvailableSims() {
        viewModelScope.launch {
            val context = getApplication<Application>()

            // CHARGER D'ABORD LA SIM SAUVEGARDÉE
            val savedSim = SimPreference.loadSelectedSim(context)
            println("📱 SIM sauvegardée retrouvée: $savedSim")

            // Détecter les slots disponibles
            val slots = SmsSender.getAvailableSimSlots(context)
            _availableSimSlots.value = slots

            // Charger les infos détaillées si Android 5.1+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val simInfos = SmsSender.getSimInfoList(context)
                _simInfoList.value = simInfos

                // PRIORITÉ À LA SIM SAUVEGARDÉE
                if (savedSim != -1 && slots.contains(savedSim)) {
                    // Utiliser la SIM sauvegardée
                    selectSimSlot(savedSim)
                    println("📱 SIM restaurée depuis préférences: slot $savedSim")
                } else if (simInfos.isNotEmpty()) {
                    // Sinon, sélectionner la première SIM
                    selectSimSlot(simInfos.first().slotIndex)
                    println("📱 Première SIM sélectionnée par défaut")
                }
            } else {
                // Pour anciennes versions
                if (savedSim != -1 && slots.contains(savedSim)) {
                    selectSimSlot(savedSim)
                } else if (slots.isNotEmpty()) {
                    selectSimSlot(slots.first())
                }
            }

            println("📱 SIM détectées: ${slots.size} slot(s)")
        }
    }
    /**
     * Sélectionner une SIM
     */
    fun selectSimSlot(slot: Int) {
        // Vérifier si le slot est disponible
        val isValidSlot = slot == -1 || _availableSimSlots.value.contains(slot)

        if (isValidSlot) {
            // 1. Mettre à jour ViewModel
            _selectedSimSlot.value = slot

            // 2. Informer SmsSender IMMÉDIATEMENT
            SmsSender.selectSimSlot(slot)

            // 3. Sauvegarder dans SharedPreferences
            val context = getApplication<Application>().applicationContext
            SimPreference.saveSelectedSim(context, slot)

            println("📱 SIM sélectionnée et sauvegardée: slot $slot (disponible: ${_availableSimSlots.value})")
        } else {
            println("⚠️ Slot $slot non disponible parmi ${_availableSimSlots.value}")
        }
    }

    /**
     * Obtenir le nom court de la SIM
     */
    fun getSelectedSimShortName(): String {
        val slot = _selectedSimSlot.value
        return if (slot == -1) "SIM" else "SIM ${slot + 1}"
    }

    fun checkNetworkStatus() {
        val context = getApplication<Application>().applicationContext
        val isAvailable = NetworkUtils.isNetworkAvailable(context)
        _isNetworkOk.value = isAvailable
    }

    fun toggleAutoSend(enabled: Boolean) {
        _autoSendEnabled.value = enabled
    }

    fun loadFailedMessages() {

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = api.getFailesSms(limit = 70)

                _failedMessages.value = response.map { sms ->
                    PendingMessage(
                        id = sms.idSms,
                        recipient = sms.numeroDestinataire,
                        message = sms.contenuSMS
                    )
                }

                println("📥 ${_failedMessages.value.size} messages echoués chargés")

            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
                println("❌ Error loading failed messages: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPendingMessages() {

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = api.getPendingSms(limit = 70)

                _pendingMessages.value = response.map { sms ->
                    PendingMessage(
                        id = sms.idSms,
                        recipient = sms.numeroDestinataire,
                        message = sms.contenuSMS
                    )
                }

                println("📥 ${_pendingMessages.value.size} messages en attente chargés")

            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
                println("❌ Error loading pending messages: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Charge les messages récents pour le dashboard
    private fun loadRecentMessages() {
        viewModelScope.launch {
            try {
                val response = api.getRecentMessages(10)
                _recentMessages.value = response.map { apiMessage ->
                    RecentMessage(
                        id = apiMessage.idSms.toInt(),
                        recipient = apiMessage.numeroDestinataire,
                        message = apiMessage.contenuSMS,
                        time = apiMessage.time,
                        status = when (apiMessage.status) {
                            "SENT" -> MessageStatus.SENT
                            "FAILED" -> MessageStatus.FAILED
                            else -> MessageStatus.PENDING
                        }
                    )
                }
            } catch (e: Exception) {
                // Ignorer l'erreur pour les messages récents
            }
        }
    }

    fun sendMessageHybrid(message: PendingMessage) {
        viewModelScope.launch {
            try {
                // Marquer comme "en cours d'envoi" dans l'UI
                updateMessageSendingStatus(message.id, true)

                val selectedSlot = _selectedSimSlot.value

                // Utiliser le mode hybride
                SmsSender.sendSmsHybrid(
                    context = getApplication<Application>().applicationContext,
                    idSms = message.id,
                    phoneNumber = message.recipient,
                    message = message.message,
                    simSlot = selectedSlot
                )

                println("📤 Envoi hybride lancé pour SMS ${message.id}")

                // Le cache se nettoiera automatiquement
                // Le Receiver mettra à jour l'API

            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
                updateMessageSendingStatus(message.id, false)
                println("❌ Erreur lancement SMS ${message.id}: ${e.message}")
            }
        }
    }
    // Pour envoyer tous les messages
    fun sendAllPendingMessagesHybrid() {
        viewModelScope.launch {
            val messages = _pendingMessages.value

            messages.forEachIndexed { index, message ->
                sendMessageHybrid(message)

                if (index < messages.size - 1) {
                    delay(5500)
                }
            }

            // Attendre un peu puis rafraîchir
            delay(5000)
            loadPendingMessages()
        }
    }

    // Rafraîchir toutes les données
    fun refreshAll() {

        loadPendingMessages()
        loadRecentMessages()
    }

    // Méthodes privées utilitaires
    private fun updateMessageSendingStatus(messageId: Long, isSending: Boolean) {
        _pendingMessages.value = _pendingMessages.value.map { msg ->
            if (msg.id == messageId) msg.copy(isSending = isSending) else msg
        }
    }

    /**
     * Démarrer le vérificateur périodique de la file d'attente
     */
    private fun startSyncQueueChecker() {
        viewModelScope.launch {
            while (true) {
                delay(30000) // Vérifier toutes les 30 secondes

                // Si la file n'est pas vide et qu'on a du réseau
                if (_syncQueue.value.isNotEmpty() && NetworkUtils.isNetworkAvailable(getApplication())) {
                    retryFailedSyncs()
                }
            }
        }
    }

    /**
     * Marquer le statut d'un SMS
     */
    fun markMessageStatus(idSms: Long, success: Boolean) {
        viewModelScope.launch {
            try {
                // 1. Mettre à jour l'API
                if (success) {
                    println("Une reussite +++++++++++++++++++")
                    println(success)
                    api.markSmsAsSent(idSms)
                } else {
                    println("send failed xxxxxxxxxxxxxxxxxxxxxxxx")
                    println(success)
                    api.markSmsAsFailed(idSms)
                }

                // 2. Mettre à jour l'UI IMMÉDIATEMENT
                updateUIAfterStatusChange(idSms, success)

                // 3. Retirer de la file si succès
                removeFromSyncQueue(idSms)

                if (success) {
                    delay(1000) // Petit délai
                    loadPendingMessages() // Recharger pour synchronisation
                }

            } catch (e: Exception) {
                println("⚠️ Échec sync SMS $idSms, ajouté à la file d'attente: ${e.message}")

                // Ajouter à la file d'attente pour réessayer plus tard
                addToSyncQueue(idSms, success)

                // Mettre à jour l'UI quand même
                updateUIAsPendingSync(idSms, success)
            }
        }
    }

    /**
     * Réessayer toutes les synchronisations en échec
     */
    fun retryFailedSyncs() {
        viewModelScope.launch {
            val queueCopy = _syncQueue.value.toMap() // Copie pour itérer

            queueCopy.forEach { (idSms, success) ->
                val retryCount = if (_retryCounts.containsKey(idSms)) _retryCounts[idSms]!! else 0
                if (retryCount < MAX_RETRIES) {
                    try {
                        println("🔄 Tentative $retryCount/$MAX_RETRIES pour SMS $idSms")
                        if (success) {
                            api.markSmsAsSent(idSms)
                        } else {
                            api.markSmsAsFailed(idSms)
                        }
                        // Succès : mettre à jour l'UI et retirer de la file
                        updateUIAfterStatusChange(idSms, success)
                        removeFromSyncQueue(idSms)
                        _retryCounts.remove(idSms)

                        println("✅ SMS $idSms synchronisé après réessai")

                    } catch (e: Exception) {
                        // Incrémenter le compteur de tentatives
                        _retryCounts[idSms] = retryCount + 1

                        if (retryCount + 1 >= MAX_RETRIES) {
                            println("❌ SMS $idSms abandonné après $MAX_RETRIES tentatives")
                            // Abandonner et mettre à jour l'UI en échec
                            updateUIAfterStatusChange(idSms, false)
                            removeFromSyncQueue(idSms)
                            _retryCounts.remove(idSms)
                        }
                    }
                }

                delay(1000) // Petit délai entre chaque tentative
            }
        }
    }

    /**
     * Forcer une tentative immédiate pour un SMS spécifique
     */
    fun retrySpecificSync(idSms: Long) {
        viewModelScope.launch {
            val success = _syncQueue.value[idSms] ?: return@launch

            try {
                if (success) {
                    api.markSmsAsSent(idSms)
                } else {
                    api.markSmsAsFailed(idSms)
                }

                removeFromSyncQueue(idSms)
                _retryCounts.remove(idSms)
                updateUIAfterStatusChange(idSms, success)

                println("✅ SMS $idSms resynchronisé manuellement")

            } catch (e: Exception) {
                println("❌ Échec resync manuel pour SMS $idSms")
            }
        }
    }

    /**
     * Vider complètement la file d'attente (pour tests)
     */
    fun clearSyncQueue() {
        _syncQueue.value = emptyMap()
        _retryCounts.clear()
        println("🧹 File d'attente vidée")
    }

    /**
     * Afficher l'état de la file d'attente (pour debug)
     */
    fun printSyncQueueStatus() {
        println("📊 État de la file d'attente:")
        _syncQueue.value.forEach { (id, status) ->
            val retryCount = _retryCounts.getOrDefault(id, 0)
            println("  - SMS $id: ${if (status) "envoyé" else "échoué"} (tentatives: $retryCount)")
        }
        if (_syncQueue.value.isEmpty()) {
            println("  Vide")
        }
    }


    private fun addToSyncQueue(idSms: Long, success: Boolean) {
        _syncQueue.value = _syncQueue.value.toMutableMap().apply {
            put(idSms, success)
        }
        _retryCounts[idSms] = 0
    }

    private fun removeFromSyncQueue(idSms: Long) {
        _syncQueue.value = _syncQueue.value.toMutableMap().apply {
            remove(idSms)
        }
    }

    private fun updateUIAfterStatusChange(idSms: Long, success: Boolean) {
        // Utiliser une copie mutable et la reconvertir en liste immuable
        val currentMessages = _pendingMessages.value.toMutableList()
        val messageIndex = currentMessages.indexOfFirst { it.id == idSms }

        if (messageIndex != -1) {
            if (success) {
                // Retirer le message
                currentMessages.removeAt(messageIndex)
                println("🗑️ SMS $idSms retiré de la liste UI")
            } else {
                // Marquer comme échec
                val failedMessage = currentMessages[messageIndex].copy(
                    isSending = false,
                    syncFailed = true,
                    //errorMessage = "Échec d'envoi"
                )
                currentMessages[messageIndex] = failedMessage
                println("❌ SMS $idSms marqué comme échec dans l'UI")
            }

            // Mettre à jour avec NOUVELLE liste (immuable)
            _pendingMessages.value = currentMessages.toList()
        } else {
            println("⚠️ SMS $idSms non trouvé dans la liste UI")
        }
    }

    private fun updateUIAsPendingSync(idSms: Long, success: Boolean) {
        _pendingMessages.value = _pendingMessages.value.map { msg ->
            if (msg.id == idSms) {
                msg.copy(
                    isSending = false,
                    pendingSync = true,  // Nouveau champ
                    syncStatus = if (success) "à marquer envoyé" else "à marquer échoué"
                )
            } else msg
        }
    }

    fun removeSentMessage(messageId: Long) {
        // Vérifier si le message existe encore
        val exists = _pendingMessages.value.any { it.id == messageId }
        if (!exists) {
            println("⚠️ SMS $messageId déjà retiré")
            return
        }

        // Créer une nouvelle liste (immuable)
        val newList = _pendingMessages.value.filter { it.id != messageId }

        // Mettre à jour le StateFlow avec la NOUVELLE liste
        _pendingMessages.value = newList

        println("🗑️ SMS $messageId retiré de la liste (${newList.size} restants)")

    }
}