// LiveAssistScreen.kt

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

@Composable
fun LiveAssistScreen(
    navController: NavController,
    intentEngine: IntentEngine,
    lastEvent: GestureEvent?,
    currentEmotion: String?,
    speak: (String) -> Unit,
    sendToGlasses: (String) -> Unit,
    onLogEvent: (String) -> Unit,
    userEmail: String
) {
    // 🔹 No default "Waiting for gesture..." text anymore – starts empty
    var lastGestureText by remember { mutableStateOf("") }
    var interpretedText by remember { mutableStateOf("") }
    var emotionLabel by remember { mutableStateOf("Unknown") }

    LaunchedEffect(currentEmotion) {
        currentEmotion?.let {
            emotionLabel = it
        }
    }

    LaunchedEffect(lastEvent) {
        lastEvent?.let { e ->
            // Text that will be shown in the big white card
            lastGestureText = when (e.type) {
                GestureType.YES -> "YES ✅"
                GestureType.NO -> "NO ❌"
                GestureType.HELP -> "HELP 🚨"
                GestureType.FALL -> "FALL 🚨"
                GestureType.UNKNOWN -> ""   // nothing if we don't understand
            }

            // Let the intent engine react to the gesture
            val result = intentEngine.handleGesture(e)
            when (result) {
                is IntentResult.AnswerYes -> {
                    interpretedText = result.spokenText
                    speak(result.spokenText)
                    sendToGlasses(result.glassesText)
                    onLogEvent(result.logText)
                }
                is IntentResult.AnswerNo -> {
                    interpretedText = result.spokenText
                    speak(result.spokenText)
                    sendToGlasses(result.glassesText)
                    onLogEvent(result.logText)
                }
                is IntentResult.Emergency -> {
                    interpretedText = result.spokenText
                    speak(result.spokenText)
                    sendToGlasses(result.glassesText)
                    onLogEvent("EMERGENCY from ${result.source}")
                    navController.navigate("sos/${result.source}")
                }
                IntentResult.None -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "GLYTCH Live Assist",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6A5ACD)
            )
            Spacer(Modifier.height(8.dp))
            Text("Wristband: Connected ✅", color = Color.DarkGray)

            Spacer(Modifier.height(16.dp))

            Text("Context:", fontWeight = FontWeight.SemiBold)
            Text(
                when (val ctx = intentEngine.currentContext) {
                    is AppContext.Question -> "Asking: \"${ctx.text}\""
                    is AppContext.Medicine -> "Medicine: ${ctx.name}"
                    AppContext.Idle -> "Idle"
                },
                color = Color.Gray
            )

            Spacer(Modifier.height(12.dp))

            Text("Emotion state:", fontWeight = FontWeight.SemiBold)
            Text(
                when (emotionLabel) {
                    "CALM" -> "CALM 😌"
                    "STRESSED" -> "STRESSED 😟"
                    else -> "Unknown"
                },
                color = Color.Gray
            )

            Spacer(Modifier.height(20.dp))

            // 🔹 Big card where the detected gesture will appear
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        // shows YES/NO/FALL/HELP; empty until first gesture comes in
                        lastGestureText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333366)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (interpretedText.isNotEmpty()) {
                Text("Spoken intent:", fontWeight = FontWeight.SemiBold)
                Text(interpretedText, color = Color.DarkGray)
            }

            Spacer(Modifier.height(16.dp))

            Text("Demo context buttons:", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { intentEngine.setQuestion("are you in pain?") }) {
                    Text("Set Q: Pain?")
                }
                Button(onClick = { intentEngine.setMedicine("Paracetamol 500mg") }) {
                    Text("Set Medicine")
                }
                Button(onClick = { intentEngine.setIdle() }) {
                    Text("Set Idle")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { navController.navigate("sos/Manual") }) {
                Text("SOS Panel")
            }
            Button(onClick = { navController.navigate("emotion") }) {
                Text("NeuroMind")
            }
            Button(onClick = { navController.navigate("medicine") }) {
                Text("Medicine Assist")
            }
            Button(onClick = { navController.navigate("settings") }) {
                Text("Settings")
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { navController.navigate("contacts/$userEmail") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Emergency Contacts")
        }
    }
}
