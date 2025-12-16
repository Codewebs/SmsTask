package com.indiza.smstask.composants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indiza.smstask.R
import com.indiza.smstask.viewmodel.MainViewModel
import com.indiza.smstask.viewmodel.SimInfo


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimScreen() {
    val viewModel: MainViewModel = viewModel()

    val availableSimSlots by viewModel.availableSimSlots.collectAsState()
    val selectedSimSlot by viewModel.selectedSimSlot.collectAsState()
    val simInfoList by viewModel.simInfoList.collectAsState()

    val context = LocalContext.current

    var showSimDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        // Afficher la SIM sélectionnée en permanence
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = { showSimDialog = true },
                            label = {
                                Text(viewModel.getSelectedSimShortName())
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_sim_card),
                                    contentDescription = "SIM",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )

                    }
                },
                actions = {
                    // Bouton de sélection SIM
                    IconButton(
                        onClick = { showSimDialog = true },
                        enabled = availableSimSlots.size > 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.SimCard,
                            contentDescription = "Changer de SIM",
                            tint = if (availableSimSlots.size > 1) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Bannière d'information SIM
            SimInfoBanner(
                selectedSimSlot = selectedSimSlot,
                simInfoList = simInfoList,
                onSimClick = { showSimDialog = true }
            )

            // ... reste du contenu inchangé ...

            // Dialog de sélection SIM
            if (showSimDialog) {
                SimSelectionDialog(
                    availableSimSlots = availableSimSlots,
                    selectedSimSlot = selectedSimSlot,
                    simInfoList = simInfoList,
                    onSelectSim = { slot ->
                        viewModel.selectSimSlot(slot)
                        showSimDialog = false
                    },
                    onDismiss = { showSimDialog = false }
                )
            }
        }
    }
}

/**
 * Bannière d'information SIM
 */
@Composable
fun SimInfoBanner(
    selectedSimSlot: Int,
    simInfoList: List<SimInfo>,
    onSimClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onSimClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Info SIM
            Column {
                Text(
                    text = "SIM d'envoi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val simInfo = simInfoList.find { it.slotIndex == selectedSimSlot }
                if (simInfo != null) {
                    Text(
                        text = simInfo.displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = simInfo.number,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = if (selectedSimSlot == -1) "SIM système" else "SIM ${selectedSimSlot + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Indicateur de changement
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Changer de SIM",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Dialog de sélection SIM
 */
@Composable
fun SimSelectionDialog(
    availableSimSlots: List<Int>,
    selectedSimSlot: Int,
    simInfoList: List<SimInfo>,
    onSelectSim: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sélectionner la SIM") },
        text = {
            Column {
                // Option SIM système
                SimOptionItem(
                    slot = -1,
                    isSelected = selectedSimSlot == -1,
                    displayName = "SIM système (défaut)",
                    onClick = { onSelectSim(-1) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Options SIM physiques
                availableSimSlots.forEach { slot ->
                    val simInfo = simInfoList.find { it.slotIndex == slot }

                    SimOptionItem(
                        slot = slot,
                        isSelected = selectedSimSlot == slot,
                        displayName = simInfo?.displayText ?: "SIM ${slot + 1}",
                        carrierName = simInfo?.carrierName,
                        number = simInfo?.number,
                        onClick = { onSelectSim(slot) }
                    )

                    if (slot != availableSimSlots.last()) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Message si une seule SIM
                if (availableSimSlots.size <= 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Une seule SIM détectée sur cet appareil",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

/**
 * Item d'option SIM
 */
@Composable
fun SimOptionItem(
    slot: Int,
    isSelected: Boolean,
    displayName: String,
    carrierName: String? = null,
    number: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icône radio
                RadioButton(
                    selected = isSelected,
                    onClick = null // Géré par le Card
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Contenu
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    carrierName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    number?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Indicateur sélection
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Sélectionné",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}