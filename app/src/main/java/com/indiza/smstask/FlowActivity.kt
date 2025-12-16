// MainActivity.kt
package com.indiza.smstask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.indiza.smstask.composants.DashboardScreen
import com.indiza.smstask.composants.MainScreen
import android.Manifest
import android.app.Application
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.material.icons.filled.Sync
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indiza.smstask.composants.PermissionRequestScreen

import com.indiza.smstask.tools.DataStoreManager
import com.indiza.smstask.viewmodel.HybridSentReceiver
import com.indiza.smstask.viewmodel.MainViewModel
import com.indiza.smstask.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first



import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import com.indiza.smstask.viewmodel.HybridSentCallbackReceiver


class FlowActivity : ComponentActivity() {

    // 👉 UN SEUL LAUNCHER
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    // 👉 Sert à communiquer le résultat à l’UI
    private var permissionsState: MutableState<Boolean>? = null
    private val apiReady = mutableStateOf(false)


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --------------------------
        // 1️⃣ ENREGISTREMENT LAUNCHER
        // --------------------------
        permissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val allGranted = results.values.all { it }
            println("📌 Permissions results : $results")
            permissionsState?.value = allGranted
        }

        // Initialisation API
        val ds = DataStoreManager(this)
        lifecycleScope.launch {
            val url = ds.baseUrl.first()
            ApiClient.init(url)
            apiReady.value = true
        }


        // --------------------------
        // 2️⃣ UI COMPOSE
        // --------------------------
        setContent {
            MaterialTheme {

                if (!apiReady.value) {
                    // Petit écran de chargement
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@MaterialTheme
                }

                val hasPermissions = remember { mutableStateOf(hasAllPermissions()) }
                permissionsState = hasPermissions

                if (hasPermissions.value) {
                    SimpleAppNavigation()
                } else {
                    PermissionRequestScreen(onRequestPermissions = {
                        requestAllPermissions()
                    })
                }
            }
        }

    }

    // --------------------------
    // 3️⃣ LANCE LA DEMANDE
    // --------------------------
    private fun requestAllPermissions() {
        val list = mutableListOf(Manifest.permission.SEND_SMS)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Android 9 et moins
            list.add(Manifest.permission.READ_PHONE_STATE)
        } else {
            // Android 10+
            list.add(Manifest.permission.READ_PHONE_NUMBERS)
        }

        permissionsLauncher.launch(list.toTypedArray())
    }


    // --------------------------
    // 4️⃣ TEST PERMISSIONS
    // --------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasAllPermissions(): Boolean {

        val sms = checkSelfPermission(Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED

        val phone = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            checkSelfPermission(Manifest.permission.READ_PHONE_STATE) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) ==
                    PackageManager.PERMISSION_GRANTED
        }

        return sms && phone
    }

}

@Composable
fun SimpleAppNavigation() {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(context.applicationContext as Application))

    DisposableEffect(Unit) {

        val receiver = HybridSentCallbackReceiver { id, success ->
            println(success)
            viewModel.markMessageStatus(id, success)
        }

        val filter = IntentFilter().apply {
            addAction("SMS_HYBRID_SENT")
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Send, "Envoi") },
                    label = { Text("Envoi") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dashboard, "Tableau de bord") },
                    label = { Text("Dashboard") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Sync, "Sync") },
                    label = { Text("Sync") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )

            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> MainScreen()
                1 -> DashboardScreen()
                2 -> SyncQueueScreen(viewModel)
            }
        }
    }
}