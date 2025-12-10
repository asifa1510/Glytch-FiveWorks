package com.example.glytch

enum class GestureType {
    YES, NO, HELP, FALL, UNKNOWN
}

data class GestureEvent(
    val type: GestureType,
    val raw: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class AppContext {
    object Idle : AppContext()
    data class Question(val text: String) : AppContext()   // e.g. "are you in pain?"
    data class Medicine(val name: String) : AppContext()   // e.g. "Paracetamol 500mg"
}

sealed class IntentResult {
    object None : IntentResult()

    data class AnswerYes(
        val spokenText: String,
        val logText: String,
        val glassesText: String
    ) : IntentResult()

    data class AnswerNo(
        val spokenText: String,
        val logText: String,
        val glassesText: String
    ) : IntentResult()

    data class Emergency(
        val spokenText: String,
        val smsText: String,
        val glassesText: String,
        val source: String
    ) : IntentResult()
}

class IntentEngine {

    var currentContext: AppContext = AppContext.Idle
        private set

    fun setQuestion(text: String) {
        currentContext = AppContext.Question(text)
    }

    fun setMedicine(name: String) {
        currentContext = AppContext.Medicine(name)
    }

    fun setIdle() {
        currentContext = AppContext.Idle
    }

    fun handleGesture(event: GestureEvent): IntentResult {
        return when (event.type) {
            GestureType.YES  -> handleYes()
            GestureType.NO   -> handleNo()
            GestureType.HELP -> handleEmergency("HELP Gesture")
            GestureType.FALL -> handleEmergency("FALL")
            GestureType.UNKNOWN -> IntentResult.None
        }
    }

    // ✅ After YES
    private fun handleYes(): IntentResult {
        return when (val ctx = currentContext) {
            is AppContext.Question -> {
                val q = ctx.text
                IntentResult.AnswerYes(
                    spokenText = "Yes, $q",
                    logText = "Q: \"$q\" → YES",
                    glassesText = "YES"
                )
            }
            is AppContext.Medicine -> {
                val med = ctx.name
                IntentResult.AnswerYes(
                    spokenText = "Medicine accepted: $med",
                    logText = "Medicine accepted: $med (notify caregiver: give 1 tablet)",
                    glassesText = "Medicine accepted"
                )
            }
            AppContext.Idle -> {
                IntentResult.AnswerYes(
                    spokenText = "Yes, I need attention.",
                    logText = "Idle YES → user needs attention",
                    glassesText = "YES – needs attention"
                )
            }
        }
    }

    // ❌ After NO
    private fun handleNo(): IntentResult {
        return when (val ctx = currentContext) {
            is AppContext.Question -> {
                val q = ctx.text
                IntentResult.AnswerNo(
                    spokenText = "No, I am okay.",
                    logText = "Q: \"$q\" → NO",
                    glassesText = "NO"
                )
            }
            is AppContext.Medicine -> {
                val med = ctx.name
                IntentResult.AnswerNo(
                    spokenText = "Medicine declined or postponed.",
                    logText = "Medicine declined/postponed: $med",
                    glassesText = "Medicine postponed"
                )
            }
            AppContext.Idle -> {
                IntentResult.AnswerNo(
                    spokenText = "No.",
                    logText = "Idle NO",
                    glassesText = "NO"
                )
            }
        }
    }

    // 🚨 After HELP / FALL
    private fun handleEmergency(source: String): IntentResult {
        val spoken = "Emergency. The user needs help."
        val sms = "Patient requesting urgent help at location: [x,y]" // plug real lat,lng
        val glasses = "SOS SENT"
        return IntentResult.Emergency(
            spokenText = spoken,
            smsText = sms,
            glassesText = glasses,
            source = source
        )
    }
}
