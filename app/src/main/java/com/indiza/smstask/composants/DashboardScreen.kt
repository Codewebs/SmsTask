// DashboardScreen.kt
package com.indiza.smstask.composants

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indiza.smstask.viewmodel.StatsViewModel

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.SimCard

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    var showFailedScreen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Crée le ViewModel
    val viewModel: StatsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>
            ): T {
                return StatsViewModel(application) as T
            }
        }
    )

    // Observe les états
    val currentStats by viewModel.currentStats.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val recentMessages by viewModel.recentMessages.collectAsState()

    val lazyListState = rememberLazyListState()
    var showServerScreen by remember { mutableStateOf(false) }
    if (showServerScreen) {
        ServerSettingsScreen()
        return
    }
    if (showFailedScreen) {
        // On passe la période au FailedScreen
        FailedScreen(period = selectedPeriod, onBack = { showFailedScreen = false })
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // En-tête avec bouton de rafraîchissement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tableau de bord",
                    style = MaterialTheme.typography.headlineMedium
                )

                IconButton(
                    onClick = { showServerScreen = true }
                ) {
                    Icon(Icons.Default.SimCard, "Choisir SIM")
                }
            }
        }

        item {
            // Afficher les erreurs
            errorMessage?.let { message ->
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        onClick = { viewModel.refreshStats() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Erreur",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))

            // Sélecteur de période
            SmartPeriodButton(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { period ->
                    viewModel.selectPeriod(period)
                }
            )
        }

        val percentSent = if (currentStats.total > 0) {
            currentStats.sent.toFloat() / currentStats.total.toFloat()
        } else {
            0f
        }

        if (isLoading && currentStats.total == 0) {
            item {
                Spacer(Modifier.height(16.dp))
                // Écran de chargement initial
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Chargement des statistiques...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Spacer(Modifier.height(16.dp))

                // Cartes de statistiques
                StatsCardsSection(
                    stats = currentStats,
                    selectedPeriod = selectedPeriod,
                    onTotalClicked = { viewModel.selectPeriod("day") },
                    onSentClicked = { viewModel.selectPeriod("week") },
                    onFailedClicked = { showFailedScreen = true },
                    onPendingClicked = { viewModel.refreshStats() }
                )
            }

            item {
                Spacer(Modifier.height(20.dp))

                // Section informations détaillées
                DetailedStatsSection(
                    stats = currentStats,
                    period = selectedPeriod
                )
            }

            item {
                Spacer(Modifier.height(20.dp))

                RepartitionSection(
                    modifier = Modifier.fillMaxWidth(),
                    percentSent = percentSent
                )


            }

            item {
                // Liste des messages RECENTE (maintenant après le diagramme)
                RecentMessagesSection(
                    modifier = Modifier.fillMaxWidth(),
                    percentSent = percentSent,  // Optionnel : à adapter si besoin
                    recentMessages = recentMessages
                )
            }

            item {
                // Espace en bas pour le scroll
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
@Composable
fun SmartPeriodButton(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        onClick = {
            val nextPeriod = when (selectedPeriod) {
                "day" -> "week"
                "week" -> "month"
                "month" -> "day"
                else -> "day"
            }
            onPeriodSelected(nextPeriod)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedPeriod) {
                            "day" -> "J"
                            "week" -> "S"
                            "month" -> "M"
                            else -> "J"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Filtre actuel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = getPeriodLabel(selectedPeriod),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Changer période",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun getPeriodLabel(period: String): String {
    return when (period) {
        "day" -> "Aujourd'hui"
        "week" -> "Cette semaine"
        "month" -> "Ce mois"
        else -> "Aujourd'hui"
    }
}

