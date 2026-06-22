package com.kargathra.fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kargathra.fitness.health.HealthConnectManager
import com.kargathra.fitness.ui.KargathraApp
import com.kargathra.fitness.ui.theme.KargathraTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var health: HealthConnectManager
    private val connected = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        health = (application as App).healthConnect

        setContent {
            KargathraTheme {
                val status      = remember { HealthConnectManager.sdkStatus(this) }
                val isConnected by connected.collectAsStateWithLifecycle()
                val scope       = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    if (status == HealthConnectManager.AVAILABLE) {
                        connected.value = health.hasAllPermissions()
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = health.requestPermissionContract()
                ) { granted ->
                    connected.value = granted.containsAll(health.permissions)
                }

                when (status) {
                    HealthConnectManager.AVAILABLE -> {
                        KargathraApp(
                            repo             = (application as App).repository,
                            exerciseRepo     = (application as App).exerciseRepository,
                            healthConnected  = isConnected,
                            healthStatusText = if (isConnected) "Connected"
                                              else "Available — not yet granted",
                            onConnectHealth  = {
                                scope.launch { permissionLauncher.launch(health.permissions) }
                            }
                        )
                    }
                    HealthConnectManager.UPDATE_REQUIRED ->
                        HealthUnavailable("Health Connect needs updating on this device.")
                    else ->
                        HealthUnavailable("Health Connect isn't available on this device.")
                }
            }
        }
    }
}

@Composable
private fun HealthUnavailable(message: String) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Kargathra", style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text(message, style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center)
            }
        }
    }
}
