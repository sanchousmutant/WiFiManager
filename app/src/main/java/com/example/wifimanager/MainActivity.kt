package com.example.wifimanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.wifimanager.ui.theme.WiFiManagerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Все разрешения получены
        } else {
            // Некоторые разрешения не получены
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Запрашиваем необходимые разрешения
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
        
        setContent {
            WiFiManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WiFiListScreen()
                }
            }
        }
    }
}

@Composable
fun WiFiListScreen() {
    val context = LocalContext.current
    val wifiManager = remember { context.getSystemService(WifiManager::class.java) }
    var wifiList by remember { mutableStateOf<List<android.net.wifi.ScanResult>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            if (wifiManager?.isWifiEnabled == true) {
                wifiManager.startScan()
                delay(2000) // Даем время на сканирование
                wifiList = wifiManager.scanResults.sortedByDescending { it.level }
            }
            delay(5000) // Обновляем каждые 5 секунд
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Wi-Fi сети",
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(
                onClick = {
                    scope.launch {
                        isRefreshing = true
                        wifiManager?.startScan()
                        delay(2000)
                        wifiList = wifiManager?.scanResults?.sortedByDescending { it.level } ?: emptyList()
                        isRefreshing = false
                    }
                },
                enabled = !isRefreshing
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Обновить"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(wifiList) { wifi ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            // Открываем настройки Wi-Fi при нажатии
                            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                                       Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                       Intent.FLAG_ACTIVITY_NO_ANIMATION
                            })
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = wifi.SSID,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Сигнал: ${WifiManager.calculateSignalLevel(wifi.level, 5) + 1}/5",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}