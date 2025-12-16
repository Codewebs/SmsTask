package com.indiza.smstask.composants

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indiza.smstask.tools.MessageStatus
import com.indiza.smstask.tools.RecentMessage
import com.indiza.smstask.viewmodel.StatsViewModel

@Composable
fun RecentMessagesSection(
    modifier: Modifier = Modifier,
    percentSent: Float = 0.85f,
    recentMessages: List<RecentMessage>
) {



    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Spacer(Modifier.width(20.dp))

        // Liste des messages récents
        RecentMessagesList(recentMessages = recentMessages)
    }
}

// Modifiez le composant RecentMessagesList
@Composable
fun RecentMessagesList(recentMessages: List<RecentMessage>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // En-tête
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Derniers messages",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "${recentMessages.size} messages",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        if (recentMessages.isEmpty()) {
            // Si pas de messages
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun message récent",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Liste des messages réels
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentMessages) { message ->
                    MessageItem(message = message)
                }
            }

            // Légende des statuts
            Spacer(Modifier.height(12.dp))
            StatusLegend()
        }
    }
}

@Composable
fun MessageItem(message: RecentMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicateur de statut
            StatusIndicator(status = message.status)

            Spacer(Modifier.width(12.dp))

            // Contenu du message
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message.recipient,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = message.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(status: MessageStatus) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(
                color = when (status) {
                    MessageStatus.SENT -> Color(0xFF2E7D32)
                    MessageStatus.FAILED -> Color(0xFFC62828)
                    MessageStatus.PENDING -> Color(0xFFFB8C00)
                },
                shape = CircleShape
            )
    )
}

@Composable
fun StatusLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatusLegendItem(
            color = Color(0xFF2E7D32),
            label = "Envoyé"
        )
        StatusLegendItem(
            color = Color(0xFFC62828),
            label = "Échec"
        )
        StatusLegendItem(
            color = Color(0xFFFB8C00),
            label = "En attente"
        )
    }
}

@Composable
fun StatusLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

