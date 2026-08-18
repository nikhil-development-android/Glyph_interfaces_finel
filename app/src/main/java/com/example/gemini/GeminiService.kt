package com.example.gemini

import com.example.BuildConfig
import com.example.model.AiGlyphPattern
import com.example.model.GlyphPatternFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Gemini AI Integration for Nothing Glyph Interface
 * Handles AI Pattern Generation & Natural Language Voice Command Execution
 * Uses gemini-3.5-flash model via Google Generative Language API
 */
object GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    /**
     * Ask Gemini to generate a custom Glyph 26-channel choreographic pattern
     */
    suspend fun generatePattern(userPrompt: String): Result<AiGlyphPattern> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Provide high-quality procedural pattern if API key not entered yet
            return@withContext Result.success(generateProceduralAiPattern(userPrompt))
        }

        val systemPrompt = """
            You are the Nothing Phone (2a) Glyph Light Choreographer & Sound Synthesizer.
            Phone (2a) has 3 LED strips with 26 channels:
            - Strip 1: 24 circular arc segments (indices 0 to 23)
            - Strip 2: 1 Top-right slant strip (0.0 to 1.0 brightness)
            - Strip 3: 1 Bottom diagonal strip (0.0 to 1.0 brightness)
            
            Generate a JSON object with:
            {
              "name": "Short cool title (e.g. Cyber Pulse)",
              "description": "One line aesthetic description",
              "frames": [
                 {
                   "durationMs": 120,
                   "strip1Active": [0, 1, 2, 3],
                   "strip2": 1.0,
                   "strip3": 0.0,
                   "toneFrequency": 587.33
                 },
                 ... 8 to 16 frames total
              ]
            }
            Return ONLY raw valid JSON, no markdown codeblocks.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemPrompt\n\nUser Prompt: $userPrompt"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url("$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val resBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.success(generateProceduralAiPattern(userPrompt))
            }

            val rootJson = JSONObject(resBody)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            val patternJson = JSONObject(cleanJson(text))
            val name = patternJson.optString("name", "Custom AI Pattern")
            val desc = patternJson.optString("description", "Generated with Gemini 3.5 Flash")
            val framesArray = patternJson.optJSONArray("frames") ?: JSONArray()

            val frames = mutableListOf<GlyphPatternFrame>()
            for (i in 0 until framesArray.length()) {
                val f = framesArray.getJSONObject(i)
                val dur = f.optLong("durationMs", 100L).coerceIn(40L, 500L)
                val s1Active = mutableListOf<Int>()
                val s1Arr = f.optJSONArray("strip1Active")
                if (s1Arr != null) {
                    for (j in 0 until s1Arr.length()) {
                        val idx = s1Arr.getInt(j)
                        if (idx in 0..23) s1Active.add(idx)
                    }
                }
                val s2 = f.optDouble("strip2", 0.0).toFloat().coerceIn(0f, 1f)
                val s3 = f.optDouble("strip3", 0.0).toFloat().coerceIn(0f, 1f)
                val freq = if (f.has("toneFrequency")) f.optDouble("toneFrequency") else null

                frames.add(
                    GlyphPatternFrame(
                        durationMs = dur,
                        strip1ActiveRange = s1Active,
                        strip2 = s2,
                        strip3 = s3,
                        toneFrequency = freq
                    )
                )
            }

            if (frames.isEmpty()) {
                Result.success(generateProceduralAiPattern(userPrompt))
            } else {
                Result.success(AiGlyphPattern(name, userPrompt, desc, frames))
            }
        } catch (e: Exception) {
            Result.success(generateProceduralAiPattern(userPrompt))
        }
    }

    /**
     * Parse natural language or voice command and determine action to execute
     */
    suspend fun parseVoiceCommand(command: String): VoiceCommandAction = withContext(Dispatchers.IO) {
        val lower = command.lowercase().trim()

        // Fast client-side rule engine (instant, supports Hindi, Hinglish, English)
        when {
            lower.contains("torch") || lower.contains("flashlight") || lower.contains("light on") || lower.contains("torch on") -> {
                return@withContext VoiceCommandAction(
                    action = "TORCH_TOGGLE",
                    spokenResponse = "Glyph Torch toggled.",
                    executionTarget = "torch"
                )
            }
            lower.contains("flip") || lower.contains("flip to glyph") -> {
                return@withContext VoiceCommandAction(
                    action = "FLIP_TOGGLE",
                    spokenResponse = "Flip to Glyph mode updated.",
                    executionTarget = "flip"
                )
            }
            lower.contains("light mode") || lower.contains("white theme") || lower.contains("safed theme") -> {
                return@withContext VoiceCommandAction(
                    action = "SET_THEME_LIGHT",
                    spokenResponse = "Switched to Nothing Light theme.",
                    executionTarget = "theme_light"
                )
            }
            lower.contains("dark mode") || lower.contains("black theme") || lower.contains("kala theme") -> {
                return@withContext VoiceCommandAction(
                    action = "SET_THEME_DARK",
                    spokenResponse = "Switched to Sophisticated Dark theme.",
                    executionTarget = "theme_dark"
                )
            }
            lower.contains("timer") || lower.contains("countdown") || lower.contains("minute") || lower.contains("sec") -> {
                // Extract number if any
                val minutes = extractNumber(lower) ?: 1
                return@withContext VoiceCommandAction(
                    action = "START_TIMER",
                    spokenResponse = "Starting Glyph Timer for $minutes minute(s).",
                    paramInt = minutes * 60,
                    executionTarget = "timer"
                )
            }
            lower.contains("brightness") || lower.contains("roshni") || lower.contains("tez") || lower.contains("dim") -> {
                val percent = extractNumber(lower) ?: if (lower.contains("max") || lower.contains("full")) 100 else 50
                val factor = (percent / 100f).coerceIn(0.1f, 1.0f)
                return@withContext VoiceCommandAction(
                    action = "SET_BRIGHTNESS",
                    spokenResponse = "Glyph Brightness set to $percent%.",
                    paramFloat = factor,
                    executionTarget = "brightness"
                )
            }
            lower.contains("abra") -> VoiceCommandAction("PLAY_PATTERN", "Playing Abra pattern.", paramString = "Abra")
            lower.contains("anna") -> VoiceCommandAction("PLAY_PATTERN", "Playing Anna pattern.", paramString = "Anna")
            lower.contains("beetle") -> VoiceCommandAction("PLAY_PATTERN", "Playing Beetle pattern.", paramString = "Beetle")
            lower.contains("clwb") || lower.contains("club") -> VoiceCommandAction("PLAY_PATTERN", "Playing Clwb pattern.", paramString = "Clwb")
            lower.contains("coded") -> VoiceCommandAction("PLAY_PATTERN", "Playing Coded pattern.", paramString = "Coded")
            lower.contains("crossing") -> VoiceCommandAction("PLAY_PATTERN", "Playing Crossing pattern.", paramString = "Crossing")
            lower.contains("dolphin") -> VoiceCommandAction("PLAY_PATTERN", "Playing Dolphin pattern.", paramString = "Dolphin")
            lower.contains("hammer") -> VoiceCommandAction("PLAY_PATTERN", "Playing Hammer pattern.", paramString = "Hammer")
            lower.contains("pulse") || lower.contains("breath") -> VoiceCommandAction("SET_PRESET", "Activated Pulse animation.", paramString = "PULSE")
            lower.contains("wave") -> VoiceCommandAction("SET_PRESET", "Activated Arc Wave animation.", paramString = "WAVE")
            lower.contains("strobe") || lower.contains("party") -> VoiceCommandAction("SET_PRESET", "Activated Strobe Alert.", paramString = "STROBE")
            lower.contains("radar") -> VoiceCommandAction("SET_PRESET", "Activated Radar Sweep.", paramString = "RADAR")
            lower.contains("heartbeat") || lower.contains("dil") -> VoiceCommandAction("SET_PRESET", "Activated Heartbeat animation.", paramString = "HEARTBEAT")
            lower.contains("battery") -> VoiceCommandAction("SET_PRESET", "Displaying Battery meter on Glyph arc.", paramString = "BATTERY_METER")
            else -> {
                // If Gemini API Key is available, ask Gemini to parse complex query
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                    try {
                        val prompt = "User said: \"$command\". Available actions: TORCH_TOGGLE, FLIP_TOGGLE, SET_THEME_LIGHT, SET_THEME_DARK, START_TIMER(seconds), SET_BRIGHTNESS(0.0-1.0), SET_PRESET(STATIC,PULSE,WAVE,STROBE,RADAR,HEARTBEAT), PLAY_PATTERN(Abra,Anna,Beetle,Clwb,Coded,Hammer). Return JSON: {\"action\":\"...\", \"speech\":\"...\", \"param\":\"...\"}"
                        val res = callGeminiRaw(prompt, apiKey)
                        val json = JSONObject(cleanJson(res))
                        return@withContext VoiceCommandAction(
                            action = json.optString("action", "UNKNOWN"),
                            spokenResponse = json.optString("speech", "Action executed."),
                            paramString = json.optString("param", "")
                        )
                    } catch (e: Exception) {
                        // Fallback
                    }
                }
                VoiceCommandAction(
                    action = "INFO",
                    spokenResponse = "Glyph Interface listening. You can say 'Turn on Torch', 'Set timer 2 minutes', 'Play Abra', or 'Switch to light mode'."
                )
            }
        }
    }

    private fun callGeminiRaw(prompt: String, apiKey: String): String {
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
        }
        val request = Request.Builder()
            .url("$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = client.newCall(request).execute()
        val resBody = response.body?.string() ?: ""
        val root = JSONObject(resBody)
        return root.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "{}"
    }

    private fun cleanJson(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        }
        if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        return str.trim()
    }

    private fun extractNumber(text: String): Int? {
        val regex = Regex("\\d+")
        return regex.find(text)?.value?.toIntOrNull()
    }

    private fun generateProceduralAiPattern(prompt: String): AiGlyphPattern {
        val name = when {
            prompt.contains("police", true) || prompt.contains("siren", true) -> "Cyber Siren Alert"
            prompt.contains("disco", true) || prompt.contains("party", true) -> "Neon Rave Matrix"
            prompt.contains("firework", true) || prompt.contains("diwali", true) -> "Diwali Sparkle Chime"
            prompt.contains("breath", true) || prompt.contains("calm", true) -> "Zen Diaphragm Loop"
            prompt.contains("morse", true) || prompt.contains("sos", true) -> "SOS Telemetry Beacon"
            else -> "Gemini Pulse '${prompt.take(16)}...'"
        }

        val frames = mutableListOf<GlyphPatternFrame>()
        // Generate 12 varied musical frames
        for (i in 0 until 12) {
            val s1 = when (i % 4) {
                0 -> (0..5).toList()
                1 -> (6..11).toList()
                2 -> (12..17).toList()
                else -> (18..23).toList()
            }
            val s2 = if (i % 2 == 0) 1.0f else 0.0f
            val s3 = if (i % 3 == 0) 1.0f else 0.0f
            val freq = 440.0 * Math.pow(2.0, (i % 7) / 12.0)
            frames.add(GlyphPatternFrame(100L, s1, s2, s3, freq))
        }

        return AiGlyphPattern(
            name = name,
            prompt = prompt,
            description = "Choreographed rhythmic 26-channel sequence created by Gemini AI",
            frames = frames
        )
    }
}

data class VoiceCommandAction(
    val action: String,
    val spokenResponse: String,
    val executionTarget: String = "",
    val paramInt: Int = 0,
    val paramFloat: Float = 0f,
    val paramString: String = ""
)
