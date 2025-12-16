// StatsViewModel.kt
package com.indiza.smstask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indiza.smstask.ApiClient
import com.indiza.smstask.tools.MessageStatus
import com.indiza.smstask.tools.RecentMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatsData(
    val total: Int = 0,
    val sent: Int = 0,
    val failed: Int = 0,
    val pending: Int = 0
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.api

    // États pour les différentes périodes
    private val _dailyStats = MutableStateFlow(StatsData())
    private val _weeklyStats = MutableStateFlow(StatsData())
    private val _monthlyStats = MutableStateFlow(StatsData())

    // Période sélectionnée
    private val _selectedPeriod = MutableStateFlow("day") // "day", "week", "month"
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _recentMessages = MutableStateFlow<List<RecentMessage>>(emptyList())
    val recentMessages: StateFlow<List<RecentMessage>> = _recentMessages.asStateFlow()

    // Stats actuellement affichées
    private val _currentStats = MutableStateFlow(StatsData())
    val currentStats: StateFlow<StatsData> = _currentStats.asStateFlow()

    // États UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadAllStats()
        loadRecentMessages()
    }

    fun loadAllStats() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Charge toutes les stats en une seule requête
                val response = api.getAllStats()
                println("API Response: $response")

                _dailyStats.value = StatsData(
                    total = response.daily.total,
                    sent = response.daily.sent,
                    failed = response.daily.failed,
                    pending = response.daily.pending
                )

                _weeklyStats.value = StatsData(
                    total = response.weekly.total,
                    sent = response.weekly.sent,
                    failed = response.weekly.failed,
                    pending = response.weekly.pending
                )

                _monthlyStats.value = StatsData(
                    total = response.monthly.total,
                    sent = response.monthly.sent,
                    failed = response.monthly.failed,
                    pending = response.monthly.pending
                )

                // Met à jour les stats courantes
                updateCurrentStats()

                println("Daily stats: ${_dailyStats.value}")
                println("Weekly stats: ${_weeklyStats.value}")
                println("Monthly stats: ${_monthlyStats.value}")
                println("Current stats: ${_currentStats.value}")

            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
                println("Error loading stats: ${e.message}")
                e.printStackTrace()
                // En cas d'erreur, réinitialise
                resetStats()
            } finally {
                _isLoading.value = false
            }
        }
    }

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
                // Gérer l'erreur
            }
        }
    }

    fun selectPeriod(period: String) {
        println("Changing period to: $period")
        _selectedPeriod.value = period

        // Vérifier si on a déjà des données pour cette période
        val hasData = when (period) {
            "day" -> _dailyStats.value.total > 0
            "week" -> _weeklyStats.value.total > 0
            "month" -> _monthlyStats.value.total > 0
            else -> false
        }

        if (hasData) {
            // Si on a déjà des données, juste mettre à jour l'affichage
            updateCurrentStats()
            println("Using cached data for period: $period")
        } else {
            // Sinon, recharger toutes les données
            println("No cached data for period: $period, reloading...")
            loadAllStats()
        }
    }

    private fun updateCurrentStats() {
        val newStats = when (_selectedPeriod.value) {
            "day" -> _dailyStats.value
            "week" -> _weeklyStats.value
            "month" -> _monthlyStats.value
            else -> _dailyStats.value
        }

        println("Updating current stats for period ${_selectedPeriod.value}: $newStats")
        _currentStats.value = newStats
    }

    private fun resetStats() {
        _dailyStats.value = StatsData()
        _weeklyStats.value = StatsData()
        _monthlyStats.value = StatsData()
        updateCurrentStats()

    }

    fun refreshStats() {
        println("Refreshing all stats")
        loadAllStats()
        loadRecentMessages()
    }

    // Getters pour chaque période
    val dailyStats: StateFlow<StatsData> = _dailyStats.asStateFlow()
    val weeklyStats: StateFlow<StatsData> = _weeklyStats.asStateFlow()
    val monthlyStats: StateFlow<StatsData> = _monthlyStats.asStateFlow()
}