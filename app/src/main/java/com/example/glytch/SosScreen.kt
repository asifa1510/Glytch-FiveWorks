package com.example.glytch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SosScreen(
    navController: NavController,
    source: String,
    statusText: String,
    onCallCaregiver: () -> Unit,
    onSendSmsDemo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEBEE))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "🚨 EMERGENCY TRIGGERED",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )
            Spacer(Modifier.height(12.dp))
            Text("Source: $source", color = Color(0xFF880E4F))

            Spacer(Modifier.height(24.dp))

            Text("Status:", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(statusText, color = Color.DarkGray)
        }

        Column {
            Button(
                onClick = onCallCaregiver,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8BBD0))
            ) {
                Text("📞 Call caregiver")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onSendSmsDemo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCDD2))
            ) {
                Text("📩 Send SMS (demo)")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { navController.navigate("live") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC8E6C9))
            ) {
                Text("Back to Live Assist")
            }
        }
    }
}
