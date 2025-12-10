package com.example.glytch

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavigation(
    intentEngine: IntentEngine,
    lastEvent: GestureEvent?,
    currentEmotion: String?,
    emotionHistory: List<Pair<String, String>>,
    eventLog: List<String>,
    speak: (String) -> Unit,
    sendToGlasses: (String) -> Unit,
    onLogEvent: (String) -> Unit,
    userEmail: String,
    statusText: String,
    onCallCaregiver: () -> Unit,
    onSendSmsDemo: () -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    fallDetectionEnabled: Boolean,
    onFallDetectionChange: (Boolean) -> Unit,
    emotionDetectionEnabled: Boolean,
    onEmotionDetectionChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "live") {
        composable("live") {
            LiveAssistScreen(
                navController = navController,
                intentEngine = intentEngine,
                lastEvent = lastEvent,
                currentEmotion = currentEmotion,
                speak = speak,
                sendToGlasses = sendToGlasses,
                onLogEvent = onLogEvent,
                userEmail = userEmail
            )
        }

        composable(
            "sos/{source}",
            arguments = listOf(navArgument("source") { type = NavType.StringType })
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "Unknown"
            SosScreen(
                navController = navController,
                source = source,
                statusText = statusText,
                onCallCaregiver = onCallCaregiver,
                onSendSmsDemo = onSendSmsDemo
            )
        }

        composable("emotion") {
            EmotionDashboardScreen(
                navController = navController,
                emotionHistory = emotionHistory,
                eventLog = eventLog
            )
        }

        composable("medicine") {
            MedicineAssistScreen(
                navController = navController,
                intentEngine = intentEngine,
                speak = speak,
                sendToGlasses = sendToGlasses
            )
        }

        composable("settings") {
            SettingsScreen(
                navController = navController,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                fallDetectionEnabled = fallDetectionEnabled,
                onFallDetectionChange = onFallDetectionChange,
                emotionDetectionEnabled = emotionDetectionEnabled,
                onEmotionDetectionChange = onEmotionDetectionChange
            )
        }

        composable(
            "contacts/{email}",
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: userEmail
            ContactsScreen(navController = navController, userEmail = email)
        }
    }
}
