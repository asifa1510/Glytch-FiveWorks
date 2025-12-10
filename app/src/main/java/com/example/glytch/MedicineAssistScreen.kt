package com.example.glytch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

// ✅ FIXED IP for ESP32-CAM hotspot GLYTCH_CAM
private const val ESP32_IP = "192.168.4.1"
private const val ESP32_SCAN_URL = "http://$ESP32_IP/scan"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineAssistScreen(
    navController: NavController,
    intentEngine: IntentEngine,
    speak: (String) -> Unit,
    sendToGlasses: (String) -> Unit
) {
    val client = remember { OkHttpClient() }

    var statusText by remember {
        mutableStateOf("Connect phone to Wi-Fi 'GLYTCH_CAM' (password 12345678), then tap Scan.")
    }
    var isLoading by remember { mutableStateOf(false) }
    var medicineName by remember { mutableStateOf<String?>(null) }
    var medicineDose by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun scanMedicine() {
        isLoading = true
        statusText = "Contacting ESP32-CAM at $ESP32_SCAN_URL ..."

        scope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(ESP32_SCAN_URL)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        statusText = "HTTP error ${response.code}: $body"
                    } else {
                        val parts = body.split("|")
                        if (parts.size >= 2) {
                            val name = parts[0].trim()
                            val dose = parts[1].trim()

                            medicineName = name
                            medicineDose = dose
                            statusText = "Detected: $name\nDose: $dose"

                            // Update your app context
                            intentEngine.setMedicine(name)

                            // Speak + send to glasses
                            val ttsText = "Medicine detected: $name. Dose: $dose."
                            speak(ttsText)
                            sendToGlasses("$name\n$dose")
                        } else {
                            statusText = "Unexpected response: \"$body\""
                        }
                    }
                }
            } catch (e: IOException) {
                statusText =
                    "Network error: ${e.message ?: "timeout"}\n\nCheck:\n• Phone is connected to 'GLYTCH_CAM'\n• Mobile data is OFF."
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medicine Assist", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE3F2FD))
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Point the ESP32-CAM at a prescription / medicine label.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0D47A1)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap Scan to fetch the medicine name and dose from the ESP32-CAM, then play it via audio.",
                    fontSize = 13.sp,
                    color = Color(0xFF355A8A)
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { scanMedicine() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF64B5F6),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isLoading) "Scanning..." else "📷 Scan Medicine (ESP32-CAM)")
                }

                Spacer(Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Detected medicine",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0D47A1)
                        )
                        Spacer(Modifier.height(8.dp))

                        if (medicineName == null) {
                            Text(
                                "No medicine detected yet.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        } else {
                            Text(
                                medicineName ?: "",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A237E)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                medicineDose ?: "",
                                fontSize = 14.sp,
                                color = Color(0xFF263238)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Status:",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0D47A1)
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(statusText, fontSize = 13.sp, color = Color(0xFF263238))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBBDEFB),
                    contentColor = Color(0xFF0D47A1)
                )
            ) {
                Text("Back")
            }
        }
    }
}
