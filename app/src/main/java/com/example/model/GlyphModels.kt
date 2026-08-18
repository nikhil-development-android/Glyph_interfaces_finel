package com.example.model

/**
 * Nothing Phone (2a) Glyph Hardware & Animation Data Models
 *
 * Phone (2a) ("pacman" / "pacmanpro") Glyph Hardware Layout:
 * - Total 3 Physical Light Strips containing 26 Addressable LED Channels
 * - Strip 1 (Top-Left Arc around Camera): 24 addressable segment LEDs (index 0..23)
 *   Used for: Progress bar, Volume level, Timer countdown, Camera timer, Music visualizer.
 * - Strip 2 (Top-Right Camera Accent): 1 LED channel (index 24)
 * - Strip 3 (Bottom Ribbon / Slant Line): 1 LED channel (index 25)
 */
data class GlyphState(
    val isMasterOn: Boolean = true,
    val brightness: Float = 0.85f, // 0.0f to 1.0f
    val strip1Segments: List<Float> = List(24) { 1.0f }, // 24 segments (0.0f..1.0f)
    val strip2Value: Float = 1.0f,
    val strip3Value: Float = 1.0f,
    val activePreset: GlyphAnimationType = GlyphAnimationType.STATIC,
    val activeRingtone: String = "Abra",
    val isTorchMode: Boolean = false,
    val isFlipToGlyphActive: Boolean = false,
    val isMusicVisualizerActive: Boolean = false,
    val timerProgress: Float = 1.0f, // 1.0 = full (all 24 on), 0.0 = empty
    val timerRemainingSeconds: Int = 60,
    val timerTotalSeconds: Int = 60,
    val isTimerRunning: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val is120HzOptimized: Boolean = true
) {
    fun getSegment(index: Int): Float {
        if (!isMasterOn) return 0f
        return when (index) {
            in 0..23 -> strip1Segments.getOrElse(index) { 0f } * brightness
            24 -> strip2Value * brightness
            25 -> strip3Value * brightness
            else -> 0f
        }
    }
}

enum class ThemeMode(val title: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}

enum class AppScreen(val route: String, val title: String) {
    VISUALS("visuals", "Visuals"),
    PATTERNS("patterns", "Patterns"),
    TIMER("timer", "Timer"),
    GEMINI("gemini", "Gemini AI"),
    SETTINGS("settings", "Settings")
}

enum class GlyphAnimationType(val title: String, val description: String) {
    STATIC("Static On", "Continuous steady illumination"),
    PULSE("Pulse / Breath", "Smooth rhythmic breathing cycle"),
    WAVE("Arc Wave", "Circular wave chasing through 24 arc segments"),
    STROBE("Strobe Alert", "High-frequency strobe flash for emergency / party"),
    RADAR("Radar Sweep", "Clockwise radar beam rotation"),
    HEARTBEAT("Heartbeat", "Double-pulse rhythmic heartbeat pattern"),
    BATTERY_METER("Battery Indicator", "Arc segments show current battery percentage"),
    TIMER_PROGRESS("Timer Progress", "Visual countdown emptying 24 arc segments"),
    CUSTOM_RINGTONE("Ringtone Pattern", "Authentic Nothing synchronized call choreography"),
    AI_CHOREOGRAPHY("Gemini AI Pattern", "AI-composed lighting & tone sequence")
}

data class RingtonePattern(
    val id: String,
    val name: String,
    val bpm: Int = 120,
    val description: String,
    val durationSec: Double = 4.0,
    val isAiGenerated: Boolean = false
)

data class AiGlyphPattern(
    val name: String,
    val prompt: String,
    val description: String,
    val frames: List<GlyphPatternFrame> = emptyList()
)

data class GlyphPatternFrame(
    val durationMs: Long,
    val strip1ActiveRange: List<Int>, // Indices of active segments
    val strip2: Float,
    val strip3: Float,
    val toneFrequency: Double? = null,
    val toneDurationSec: Double = 0.1
)

data class VoiceCommandLog(
    val id: String,
    val queryText: String,
    val responseText: String,
    val actionTaken: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SysfsNodeInfo(
    val name: String,
    val path: String,
    val channelRange: String,
    val description: String,
    val testCommand: String
)

data class PortingDocSection(
    val title: String,
    val summary: String,
    val codeSnippet: String,
    val language: String = "bash"
)
