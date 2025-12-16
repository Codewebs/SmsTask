package com.indiza.smstask.composants

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indiza.smstask.viewmodel.StatsData
import com.indiza.smstask.viewmodel.StatsViewModel

@Composable
fun DetailedStatsSection(
    stats: StatsData,
    period: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Titre avec période
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Détails des statistiques",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = when (period) {
                        "day" -> "Aujourd'hui"
                        "week" -> "Cette semaine"
                        "month" -> "Ce mois"
                        else -> "Aujourd'hui"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Statistiques détaillées
            StatDetailRow(
                label = "Total des SMS",
                value = stats.total.toString(),
                icon = Icons.Default.BarChart,
                color = Color(0xFF1976D2)
            )

            StatDetailRow(
                label = "Envoyés avec succès",
                value = "${stats.sent} (${calculatePercentage(stats.sent, stats.total)}%)",
                icon = Icons.Default.Send,
                color = Color(0xFF2E7D32)
            )

            StatDetailRow(
                label = "Échecs d'envoi",
                value = "${stats.failed} (${calculatePercentage(stats.failed, stats.total)}%)",
                icon = Icons.Default.Warning,
                color = Color(0xFFC62828)
            )

            StatDetailRow(
                label = "En attente d'envoi",
                value = stats.pending.toString(),
                icon = Icons.Default.Schedule,
                color = Color(0xFFFB8C00)
            )

            Spacer(Modifier.height(12.dp))

            // Taux de succès
            val successRate = if (stats.total > 0) {
                (stats.sent.toFloat() / stats.total * 100).toInt()
            } else {
                0
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        successRate >= 90 -> Color(0xFF2E7D32).copy(alpha = 0.1f)
                        successRate >= 70 -> Color(0xFFFB8C00).copy(alpha = 0.1f)
                        else -> Color(0xFFC62828).copy(alpha = 0.1f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Taux de succès",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "$successRate%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = when {
                            successRate >= 90 -> Color(0xFF2E7D32)
                            successRate >= 70 -> Color(0xFFFB8C00)
                            else -> Color(0xFFC62828)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatDetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)  // CORRIGÉ : weight(1f) avec parenthèses
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}


private fun calculatePercentage(part: Int, total: Int): String {
    return if (total > 0) {
        "%.1f".format(part.toFloat() / total * 100)
    } else {
        "0"
    }
}
