// MainActivity.kt

package com.example.glytch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import com.example.glytch.ui.theme.GlytchTheme
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {

    private var btSocket: BluetoothSocket? = null
    private var listenJob: Job? = null
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private var tts: TextToSpeech? = null
    private var languageCode: String = "EN"

    private var onRawMessage: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        requestPermissionsIfNeeded()
        initBluetooth()
        initTts()

        setContent {
            GlytchTheme {
                val intentEngine = remember { IntentEngine() }

                var lastEvent by remember { mutableStateOf<GestureEvent?>(null) }
                var currentEmotion by remember { mutableStateOf<String?>(null) }
                var emotionHistory by remember { mutableStateOf(listOf<Pair<String, String>>()) }
                var eventLog by remember { mutableStateOf(listOf<String>()) }
                var statusText by remember {
                    mutableStateOf("Waiting for wristband gesture...")
                }
                var fallDetectionEnabled by remember { mutableStateOf(true) }
                var emotionDetectionEnabled by remember { mutableStateOf(true) }

                val userEmail = "asifareh8@gmail.com"

                onRawMessage = { line ->
                    runOnUiThread {
                        val cleanLine = line.trim()
                        val now = System.currentTimeMillis()
                        Log.d("BT", "Parsed line: $cleanLine")

                        when {
                            cleanLine.startsWith("GESTURE:") || cleanLine.startsWith("EVENT:") -> {
                                val event = parseLineToGesture(cleanLine)
                                lastEvent = event

                                val t = android.text.format.DateFormat
                                    .format("HH:mm", now).toString()

                                when (event.type) {
                                    GestureType.YES -> {
                                        statusText = "Last gesture: YES at $t"
                                        eventLog = eventLog + "YES at $t"
                                    }
                                    GestureType.NO -> {
                                        statusText = "Last gesture: NO at $t"
                                        eventLog = eventLog + "NO at $t"
                                    }
                                    GestureType.FALL -> {
                                        statusText = "FALL detected at $t"
                                        eventLog = eventLog + "FALL at $t"
                                    }
                                    GestureType.HELP -> {
                                        statusText = "HELP gesture at $t"
                                        eventLog = eventLog + "HELP at $t"
                                    }
                                    else -> {
                                        statusText = "Unknown gesture at $t"
                                    }
                                }
                            }

                            cleanLine.startsWith("STATE:") && emotionDetectionEnabled -> {
                                val state = when {
                                    cleanLine.contains("CALM", ignoreCase = true) -> "CALM"
                                    cleanLine.contains("STRESSED", ignoreCase = true) -> "STRESSED"
                                    else -> null
                                }
                                state?.let {
                                    currentEmotion = it
                                    val t = android.text.format.DateFormat
                                        .format("HH:mm:ss", now).toString()
                                    emotionHistory = (emotionHistory + (t to it)).takeLast(30)
                                    if (it == "STRESSED") {
                                        eventLog = eventLog + "Stress detected at $t"
                                    }
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    listenToBluetooth(this@MainActivity)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    AppNavigation(
                        intentEngine = intentEngine,
                        lastEvent = lastEvent,
                        currentEmotion = currentEmotion,
                        emotionHistory = emotionHistory,
                        eventLog = eventLog,
                        speak = { text -> speak(text) },
                        sendToGlasses = { text -> sendToGlasses(text) },
                        onLogEvent = { log ->
                            val t = android.text.format.DateFormat
                                .format("HH:mm", System.currentTimeMillis()).toString()
                            eventLog = eventLog + "$log at $t"
                        },
                        userEmail = userEmail,
                        statusText = statusText,
                        onCallCaregiver = {
                            statusText = "Call caregiver (simulated)."
                        },
                        onSendSmsDemo = {
                            statusText = "SMS to emergency contacts (demo placeholder)."
                        },
                        currentLanguage = languageCode,
                        onLanguageChange = { code ->
                            languageCode = code
                            updateTtsLanguage()
                        },
                        fallDetectionEnabled = fallDetectionEnabled,
                        onFallDetectionChange = { fallDetectionEnabled = it },
                        emotionDetectionEnabled = emotionDetectionEnabled,
                        onEmotionDetectionChange = { emotionDetectionEnabled = it }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenJob?.cancel()
        btSocket?.close()
        tts?.shutdown()
    }

    private fun requestPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.BLUETOOTH_CONNECT)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.BLUETOOTH_SCAN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.POST_NOTIFICATIONS)

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1)
        }
    }

    private fun initBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    private fun listenToBluetooth(context: Context) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BT", "No BLUETOOTH_CONNECT permission")
            return
        }

        val device: BluetoothDevice? = bluetoothAdapter.bondedDevices.find {
            it.name == "GLYTCH_Wristband"
        }

        if (device == null) {
            Log.e("BT", "GLYTCH_Wristband not paired")
            return
        }

        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        btSocket = device.createRfcommSocketToServiceRecord(uuid)

        listenJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                bluetoothAdapter.cancelDiscovery()
                btSocket?.connect()
                Log.d("BT", "Connected to wristband")

                val reader = BufferedReader(InputStreamReader(btSocket?.inputStream))
                while (isActive) {
                    val line = reader.readLine() ?: continue
                    Log.d("BT", "Received: $line")
                    onRawMessage?.invoke(line)
                }
            } catch (e: Exception) {
                Log.e("BT", "Error: ${e.message}")
            }
        }
    }

    private fun parseLineToGesture(line: String): GestureEvent {
        val clean = line.trim()
        val type = when {
            clean.startsWith("GESTURE:YES")  -> GestureType.YES
            clean.startsWith("GESTURE:NO")   -> GestureType.NO
            clean.startsWith("GESTURE:HELP") -> GestureType.HELP
            clean.startsWith("GESTURE:FALL") -> GestureType.FALL
            clean.startsWith("EVENT:FALL")   -> GestureType.FALL
            else -> GestureType.UNKNOWN
        }
        return GestureEvent(type = type, raw = clean)
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                updateTtsLanguage()
            }
        }
    }

    private fun updateTtsLanguage() {
        val locale = when (languageCode) {
            "TA" -> Locale("ta", "IN")
            else -> Locale.ENGLISH
        }
        tts?.language = locale
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GLYTCH_TTS")
    }

    private fun sendToGlasses(text: String) {
        Log.d("Glasses", "Display: $text")
    }
}
