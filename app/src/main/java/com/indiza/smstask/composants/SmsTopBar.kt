package com.indiza.smstask.composants

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsTopBar(
    autoSendEnabled: Boolean,
    onToggleAutoSend: (Boolean) -> Unit,
    isNetworkOk: Boolean,
    lastSync: String
) {

    TopAppBar(
        title = {
            Column {

                Text(
                    text = "Dernière synchronisation : $lastSync",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            // Indicateur d'état du réseau
            NetworkStatusBadge(
                isNetworkOk = isNetworkOk,
                modifier = Modifier.padding(end = 12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Auto-send switch avec label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = if (autoSendEnabled) "ON" else "OFF",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (autoSendEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Switch(
                    checked = autoSendEnabled,
                    onCheckedChange = { onToggleAutoSend(it) },
                    modifier = Modifier
                )
            }
        }
    )
}

// Variante avec badge coloré pour plus de visibilité
@Composable
fun NetworkStatusBadge(
    isNetworkOk: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isNetworkOk) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // CORRECTION : Utilisez les bonnes icônes
            Icon(
                imageVector = if (isNetworkOk) Icons.Filled.NetworkCheck
                else Icons.Filled.SignalWifiOff,
                contentDescription = if (isNetworkOk) "Réseau disponible"
                else "Réseau indisponible",
                tint = if (isNetworkOk) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = if (isNetworkOk) "En ligne" else "Hors ligne",  // Changé à français
                style = MaterialTheme.typography.labelSmall,
                color = if (isNetworkOk) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}