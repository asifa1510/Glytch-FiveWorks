package com.example.glytch

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SettingsScreen(
    navController: NavController,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    fallDetectionEnabled: Boolean,
    onFallDetectionChange: (Boolean) -> Unit,
    emotionDetectionEnabled: Boolean,
    onEmotionDetectionChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Settings / Pairing", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Text("Language for TTS:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Row {
            FilterChip(
                selected = currentLanguage == "EN",
                onClick = { onLanguageChange("EN") },
                label = { Text("English") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = currentLanguage == "TA",
                onClick = { onLanguageChange("TA") },
                label = { Text("Tamil") }
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Fall detection enabled")
            Switch(checked = fallDetectionEnabled, onCheckedChange = onFallDetectionChange)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Emotion detection enabled")
            Switch(checked = emotionDetectionEnabled, onCheckedChange = onEmotionDetectionChange)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Pairing is handled when the app connects to the GLYTCH_Wristband " +
                    "(and optional GLYTCH_Glasses) over Bluetooth.",
            color = androidx.compose.ui.graphics.Color.DarkGray
        )

        Spacer(Modifier.height(24.dp))

        Button(onClick = { navController.navigateUp() }) {
            Text("Back")
        }
    }
}
