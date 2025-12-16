package com.indiza.smstask

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.indiza.smstask.composants.SimScreen
import com.indiza.smstask.viewmodel.MainViewModel

/**
 * Interface visuelle pour gérer la file d'attente de synchronisation.
 * Utilise UNIQUEMENT les méthodes publiques disponibles dans MainViewModel :
 *  - retryFailedSyncs()
 *  - retrySpecificSync(idSms)
 *  - clearSyncQueue()
 *  - printSyncQueueStatus()
 *
 * Elle lit l'état de la file via viewModel.syncQueue (StateFlow<Map<Long, Boolean>>)
 * et affiche des actions qui appellent les méthodes publiques.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncQueueScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val syncQueueState by viewModel.syncQueue.collectAsState()
    val isNetworkOk by viewModel.isNetworkOk.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Gestion de la file de sync") })
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { viewModel.retryFailedSyncs() }) {
                    Text("Réessayer tout")
                }

                Button(onClick = { viewModel.clearSyncQueue() }) {
                    Text("Vider la file")
                }

                OutlinedButton(onClick = { viewModel.printSyncQueueStatus() }) {
                    Text("Afficher état (log)")
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(text = if (isNetworkOk) "Réseau: OK" else "Réseau: KO")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Empty state
            if (syncQueueState.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("La file de synchronisation est vide.")
                        Text(
                            "Les messages en échec apparaîtront ici et pourront être réessayés.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                return@Column
            }

            // List of queued items
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(syncQueueState.toList()) { pair ->
                    val idSms = pair.first
                    val shouldBeSent = pair.second

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "SMS id: $idSms", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = if (shouldBeSent) "État attendu: envoyé" else "État attendu: échoué",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Note: la suppression de la file se fait automatiquement si la sync réussit.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                // Retry specific
                                Button(
                                    onClick = { viewModel.retrySpecificSync(idSms) },
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text("Réessayer")
                                }

                                // Remarque: il n'existe pas de méthode publique "removeFromSyncQueue".
                                // La suppression est effectuée automatiquement dans la ViewModel quand la sync réussit.
                                OutlinedButton(onClick = { viewModel.printSyncQueueStatus() }) {
                                    Text("Voir log")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer with summary
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Total en file: ${syncQueueState.size}")
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.retryFailedSyncs() }) {
                        Text("Relancer tout")
                    }
                }
            }

        }
    }
}
