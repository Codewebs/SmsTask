    package com.indiza.smstask.composants

    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Refresh
    import androidx.compose.material.icons.filled.Send
    import androidx.compose.material.icons.filled.SendToMobile
    import androidx.compose.material.icons.filled.SimCard
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.dp
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.indiza.smstask.tools.PendingMessage
    import com.indiza.smstask.viewmodel.MainViewModel
    import com.indiza.smstask.viewmodel.SmsSender
    import androidx.compose.material.DismissValue
    import androidx.compose.material.DismissDirection
    import androidx.compose.material.SwipeToDismiss
    import androidx.compose.material.rememberDismissState
    import androidx.compose.material.ExperimentalMaterialApi

    import androidx.compose.material3.ExperimentalMaterial3Api
    import androidx.lifecycle.compose.collectAsStateWithLifecycle


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        val viewModel: MainViewModel = viewModel()

        val pendingMessages by viewModel.pendingMessages.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val errorMessage by viewModel.errorMessage.collectAsState()
        val context = LocalContext.current

        val autoSendEnabled by viewModel.autoSendEnabled.collectAsStateWithLifecycle()

        var showSimScreen by remember { mutableStateOf(false) }

        // 👉 Si on doit afficher l’écran SIM, on arrête MainScreen ici
        if (showSimScreen) {
            SimScreen()
            return
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Centre SMS") },
                    actions = {
                        IconButton(
                            onClick = { viewModel.refreshAll() },
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Refresh, "Rafraîchir")
                            }
                        }

                        if (pendingMessages.isNotEmpty()) {
                            IconButton(
                                onClick = { showSimScreen = true }
                            ) {
                                Icon(Icons.Default.SimCard, "Choisir SIM")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "Auto",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )

                            Switch(
                                checked = autoSendEnabled, // déjà collecté au-dessus
                                onCheckedChange = { viewModel.toggleAutoSend(it) }, // ta fonction existante
                                modifier = Modifier.height(24.dp)
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
                // En-tête avec compteur
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Messages en attente",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${pendingMessages.size} à envoyer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (pendingMessages.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.sendAllPendingMessagesHybrid() }
                            ) {
                                Text("Tout envoyer")
                            }
                        }
                    }
                }

                // Messages d'erreur
                errorMessage?.let { message ->
                    if (message.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Liste des messages
                if (pendingMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Aucun message",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Aucun message en attente",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Les messages envoyés disparaissent automatiquement",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingMessages, key = { it.id }) { message ->
                            SwipeablePendingMessage(
                                message = message,
                                onSwipeDelete = { viewModel.swipeDeleteMessage(message.id) },
                                onSendClick = { viewModel.sendMessageHybrid(message) }
                            )
                        }

                    }
                }
            }
        }
    }

    @Composable
    fun PendingMessageItem(
        message: PendingMessage,
        onSendClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Contenu du message
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = message.recipient,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Bouton d'envoi
                if (message.isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    IconButton(
                        onClick = onSendClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Envoyer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun SwipeablePendingMessage(
        message: PendingMessage,
        onSwipeDelete: () -> Unit,
        onSendClick: () -> Unit
    ) {
        var showConfirmDialog by remember { mutableStateOf(false) }
        var resetSwipe by remember { mutableStateOf(false) }

        val dismissState = rememberDismissState(
            confirmStateChange = { newValue ->
                if (newValue == DismissValue.DismissedToEnd ||
                    newValue == DismissValue.DismissedToStart
                ) {
                    showConfirmDialog = true
                    false
                } else false
            }
        )

        // 🟦 Reset automatique déclenché proprement par Compose
        if (resetSwipe) {
            LaunchedEffect(Unit) {
                dismissState.reset()
                resetSwipe = false
            }
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = {
                    showConfirmDialog = false
                    resetSwipe = true       // ❗Déclenche le reset
                },
                title = { Text("Confirmation") },
                text = { Text("Voulez-vous vraiment supprimer ce message ?") },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false
                        onSwipeDelete()
                        resetSwipe = true   // ❗Reset après suppression
                    }) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false
                        resetSwipe = true   // ❗Reset si annulation
                    }) {
                        Text("Annuler")
                    }
                }
            )
        }

        SwipeToDismiss(
            state = dismissState,
            directions = setOf(
                DismissDirection.StartToEnd,
                DismissDirection.EndToStart
            ),
            background = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            },
            dismissContent = {
                PendingMessageItem(
                    message = message,
                    onSendClick = onSendClick
                )
            }
        )
    }




