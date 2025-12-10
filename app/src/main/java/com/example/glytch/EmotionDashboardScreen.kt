package com.example.glytch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionDashboardScreen(
    navController: NavController,
    emotionHistory: List<Pair<String, String>>,
    eventLog: List<String>
) {
    val stressCount = emotionHistory.count { it.second == "STRESSED" }
    val calmCount = emotionHistory.count { it.second == "CALM" }
    val total = stressCount + calmCount
    val calmPercent = if (total == 0) 0f else calmCount.toFloat() / total.toFloat()
    val stressPercent = if (total == 0) 0f else stressCount.toFloat() / total.toFloat()

    val trendText = when {
        total == 0 -> "No data yet"
        stressCount <= calmCount -> "Your recent pattern looks mostly calm."
        else -> "We noticed repeated stress spikes recently."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NeuroMind", fontWeight = FontWeight.Bold)
                        Text(
                            "Emotion & Tremor Dashboard",
                            fontSize = 12.sp,
                            color = Color(0xFFB3C1FF)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF252A5A),
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Text("←", color = Color.White, fontSize = 20.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF252A5A), Color(0xFFE8EAF6))
                    )
                )
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            // SUMMARY CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF30386F)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Today’s Emotional Snapshot",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        trendText,
                        color = Color(0xFFCFD5FF),
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatChip(
                            label = "Calm time",
                            value = "$calmCount",
                            accentColor = Color(0xFF4CAF50),
                            emoji = "😌"
                        )
                        StatChip(
                            label = "Stress spikes",
                            value = "$stressCount",
                            accentColor = Color(0xFFFF7043),
                            emoji = "😟"
                        )
                        StatChip(
                            label = "Samples",
                            value = "$total",
                            accentColor = Color(0xFF42A5F5),
                            emoji = "🧠"
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // SIMPLE BAR PERCENTAGE
                    if (total > 0) {
                        Text(
                            "Calm vs Stress (last window)",
                            color = Color(0xFFCFD5FF),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF1E2246))
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(calmPercent)
                                        .background(Color(0xFF66BB6A))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(stressPercent)
                                        .background(Color(0xFFEF5350))
                                )
                            }
                        }
                    } else {
                        Text(
                            "Waiting for live data...",
                            color = Color(0xFFCFD5FF),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent emotion stream",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF253056)
                )
                Text(
                    "Last ${emotionHistory.size} samples",
                    fontSize = 11.sp,
                    color = Color(0xFF6F7AB5)
                )
            }

            Spacer(Modifier.height(8.dp))

            // MAIN CONTENT
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // LEFT: Emotion history list
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    if (emotionHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No emotion samples yet.\nLive data will appear here.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            items(emotionHistory.reversed()) { (time, state) ->
                                EmotionRow(time = time, state = state)
                            }
                        }
                    }
                }

                // RIGHT: Events list
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            "Tagged events",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF283593)
                        )
                        Spacer(Modifier.height(8.dp))

                        if (eventLog.isEmpty()) {
                            Text(
                                "No events recorded yet.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(eventLog.reversed()) { e ->
                                    EventRow(e)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // BOTTOM BUTTON
            Button(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3949AB),
                    contentColor = Color.White
                )
            ) {
                Text("Back to Live Assist")
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    accentColor: Color,
    emoji: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(min = 80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            fontSize = 11.sp,
            color = Color(0xFFCFD5FF)
        )
    }
}

@Composable
private fun EmotionRow(time: String, state: String) {
    val isCalm = state == "CALM"
    val bgColor = if (isCalm) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val textColor = if (isCalm) Color(0xFF2E7D32) else Color(0xFFC62828)
    val label = when (state) {
        "CALM" -> "Calm"
        "STRESSED" -> "Stressed"
        else -> state
    }
    val emoji = when (state) {
        "CALM" -> "😌"
        "STRESSED" -> "😟"
        else -> "🙂"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                label,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                time,
                fontSize = 11.sp,
                color = Color.DarkGray
            )
        }
        Text(
            emoji,
            fontSize = 22.sp
        )
    }
}

@Composable
private fun EventRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFEDE7F6))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "•",
            color = Color(0xFF5E35B1),
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text,
            fontSize = 12.sp,
            color = Color(0xFF311B92),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
