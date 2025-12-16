package com.indiza.smstask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import com.indiza.smstask.composants.DashboardScreen
import com.indiza.smstask.composants.SmsTopBar
import com.indiza.smstask.viewmodel.MainViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // CORRECTION : Utiliser viewModel() avec l'import correct
            val viewModel: MainViewModel = viewModel()

            // Observer les états du ViewModel
            val networkOk by viewModel.isNetworkOk.collectAsState()
            val autoSendEnabled by viewModel.autoSendEnabled.collectAsState()
            val lastSync by viewModel.lastSync.collectAsState()

            Scaffold(
                topBar = {
                    SmsTopBar(
                        autoSendEnabled = autoSendEnabled,
                        onToggleAutoSend = { enabled ->
                            // Mettre à jour le ViewModel
                            viewModel.toggleAutoSend(enabled)
                            if (enabled) {
                               // scheduleSmsSync(this@DashboardActivity)
                            } else {
                                // cancelSmsSync(this@DashboardActivity)
                            }
                        },
                        isNetworkOk = networkOk, // Note: paramètre nommé
                        lastSync = lastSync
                    )
                }
            ) { padding ->
                DashboardScreen(
                    modifier = Modifier.padding(padding)
                )


            }
        }

    }
    override fun onResume() {
        super.onResume()
        // Vérifier le réseau quand l'activité reprend
        // (Vous devrez peut-être passer le ViewModel différemment)
    }
}