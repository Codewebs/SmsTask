package com.indiza.smstask.composants

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ServerSettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    // Collecter l'URL depuis le ViewModel
    val storedUrl by viewModel.baseUrl.collectAsState(initial = "")

    // État pour le texte modifié
    var text by remember { mutableStateOf("") }

    // État pour suivre si le texte a été modifié
    var isModified by remember { mutableStateOf(false) }

    // Initialiser avec l'URL stockée quand elle est disponible
    LaunchedEffect(storedUrl) {
        if (text.isEmpty() && storedUrl.isNotEmpty()) {
            text = storedUrl
            isModified = false
        }
    }

    // Fonction pour vérifier si le texte est différent de l'URL stockée
    val checkIfModified = { newText: String ->
        isModified = newText != storedUrl && storedUrl.isNotEmpty()
    }

    Column(Modifier.padding(16.dp)) {
        Text(
            "Adresse du serveur",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        // Afficher l'URL stockée actuelle
        if (storedUrl.isNotEmpty()) {
            Text(
                text = "URL actuelle : $storedUrl",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                checkIfModified(it)
            },
            label = { Text("http://192.168.1.X:3000/") },
            placeholder = {
                if (storedUrl.isNotEmpty()) {
                    Text(storedUrl)
                } else {
                    Text("Entrez l'URL du serveur...")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.saveUrl(text)
                isModified = false
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isModified && text.isNotBlank()
        ) {
            Text("Enregistrer")
        }

        // Message d'information
        if (!isModified && text.isNotEmpty()) {
            Text(
                text = "✓ URL identique à celle enregistrée",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}