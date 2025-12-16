package com.indiza.smstask.tools

// Modèles de données
data class StatsResponse(
    val total: Int,
    val sent: Int,
    val failed: Int,
    val pending: Int,
    val period: String? = null
)

data class AllStatsResponse(
    val daily: PeriodStats,
    val weekly: PeriodStats,
    val monthly: PeriodStats
)

data class PeriodStats(
    val total: Int,
    val sent: Int,
    val failed: Int,
    val pending: Int
)

data class ApiResponse(
    val ok: Boolean,
    val affected: Int
)

data class SmsPendingResponse(
    val idSms: Long,
    val contenuSMS: String,
    val numeroDestinataire: String,
    val statut: Int
)

data class RecentMessageResponse(
    val idSms: Long,
    val numeroDestinataire: String,
    val contenuSMS: String,
    val date: String,
    val time: String,
    val status: String
)

// Modèle de données
data class RecentMessage(
    val id: Int,
    val recipient: String,
    val message: String,
    val time: String,
    val status: MessageStatus
)
data class PendingMessage(
    val id: Long,
    val recipient: String,
    val message: String,
    val isSending: Boolean = false,
    val syncFailed: Boolean = false,
    val pendingSync: Boolean = false,
    val syncStatus: String = ""
)

enum class MessageStatus {
    SENT, FAILED, PENDING
}