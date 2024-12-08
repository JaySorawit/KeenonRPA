package com.example.rpa_accessibilityservice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.rpa_accessibilityservice.ui.theme.RPA_AccessibilityServiceTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val sharedPreferences = getSharedPreferences("RPA_PREFS", Context.MODE_PRIVATE)
            val accessibilityManager =
                getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

            val isAccessibilityServiceEnabled = remember {
                mutableStateOf(
                    accessibilityManager.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfo.FEEDBACK_ALL_MASK
                    ).any { it.id.contains("RPA_AccessibilityService") }
                )
            }

            RPA_AccessibilityServiceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ConnectionScreen(
                        modifier = Modifier.padding(innerPadding),
                        isConnected = isAccessibilityServiceEnabled.value,
                        onConnect = { ip, port ->
                            if (ip.isNotEmpty() && port.isNotEmpty()) {
                                with(sharedPreferences.edit()) {
                                    putString("IP", ip)
                                    putInt("PORT", port.toInt())
                                    apply()
                                }

                                // Open accessibility settings to enable the service
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                startActivity(intent)
                            } else {
                                Log.e("MainActivity", "IP or Port is empty!")
                            }
                        },
                        onDisconnect = {
                            with(sharedPreferences.edit()) {
                                clear()
                                apply()
                            }

                            // Open accessibility settings to disable the service
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionScreen(
    modifier: Modifier = Modifier,
    isConnected: Boolean,
    onConnect: (String, String) -> Unit,
    onDisconnect: () -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var connectionState by remember { mutableStateOf(isConnected) }
    val context = LocalContext.current
    val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    // Periodically check the state of the service
    LaunchedEffect(Unit) {
        while (true) {
            val isServiceEnabled = accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            ).any { it.id.contains("RPA_AccessibilityService") }

            connectionState = isServiceEnabled
            kotlinx.coroutines.delay(1000) // Check every 1 second
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "KEENON RPA",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (!connectionState) {
            TextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("Server IP Address") },
                modifier = Modifier.width(300.dp),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Server Port") },
                modifier = Modifier.width(300.dp),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (ip.isNotEmpty() && port.isNotEmpty()) {
                        onConnect(ip, port)
                    } else {
                        Log.e("ConnectionScreen", "Please fill in all fields!")
                    }
                },
                modifier = Modifier.width(300.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.Keenon_blue),
                    contentColor = colorResource(id = R.color.white)
                )
            ) {
                Text("Connect and Enable Accessibility Service")
            }
        } else {
            Text(
                text = "Service is enabled. Connected via RPA_AccessibilityService.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = onDisconnect,
                modifier = Modifier.width(300.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.red),
                    contentColor = colorResource(id = R.color.white)
                )
            ) {
                Text("Disable Accessibility Service")
            }
        }
    }
}