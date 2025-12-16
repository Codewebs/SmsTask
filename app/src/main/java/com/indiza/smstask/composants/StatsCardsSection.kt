package com.indiza.smstask.composants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.indiza.smstask.viewmodel.StatsData

@Composable
fun StatsCardsSection(
    stats: StatsData,
    selectedPeriod: String,
    onTotalClicked: () -> Unit,
    onSentClicked: () -> Unit,
    onFailedClicked: () -> Unit,
    onPendingClicked: () -> Unit
) {
    // Première ligne - Total et Envoyés
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatCard(
            title = "Total SMS",
            value = stats.total.toString(),
            icon = Icons.Default.BarChart,
            color = Color(0xFF1976D2),
            onClick = onTotalClicked
        )

        StatCard(
            title = "Envoyés",
            value = stats.sent.toString(),
            icon = Icons.Default.Send,
            color = Color(0xFF2E7D32),
            onClick = onSentClicked
        )
    }

    Spacer(Modifier.height(12.dp))

    // Deuxième ligne - Échecs et En attente
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatCard(
            title = "Échecs",
            value = stats.failed.toString(),
            icon = Icons.Default.Warning,
            color = Color(0xFFC62828),
            onClick = onFailedClicked
        )

        StatCard(
            title = "En attente",
            value = stats.pending.toString(),
            icon = Icons.Default.Schedule,
            color = Color(0xFFFB8C00),
            onClick = onPendingClicked
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color
                )
            }
        }
    }
}


