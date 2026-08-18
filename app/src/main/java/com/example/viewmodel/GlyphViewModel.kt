package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GlyphSoundSynth
import com.example.audio.SoundType
import com.example.gemini.GeminiService
import com.example.gemini.VoiceCommandAction
import com.example.hardware.ExecutionResult
import com.example.hardware.GlyphHardwareManager
import com.example.model.AiGlyphPattern
import com.example.model.AppScreen
import com.example.model.GlyphAnimationType
import com.example.model.GlyphPatternFrame
import com.example.model.GlyphState
import com.example.model.RingtonePattern
import com.example.model.ThemeMode
import com.example.model.VoiceCommandLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class GlyphViewModel(application: Application) : AndroidViewModel(application) {

    private val synth = GlyphSoundSynth(viewModelScope)
    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    // Navigation & Theme State
    private val _currentScreen = MutableStateFlow(AppScreen.VISUALS)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Core Glyph State
    private val _glyphState = MutableStateFlow(GlyphState())
    val glyphState: StateFlow<GlyphState> = _glyphState.asStateFlow()

    // Timer State
    private val _timerRemainingSec = MutableStateFlow(60)
    val timerRemainingSec: StateFlow<Int> = _timerRemainingSec.asStateFlow()

    private val _timerTotalSec = MutableStateFlow(60)
    val timerTotalSec: StateFlow<Int> = _timerTotalSec.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _selectedPresetMinutes = MutableStateFlow(1)
    val selectedPresetMinutes: StateFlow<Int> = _selectedPresetMinutes.asStateFlow()

    // Ringtone Patterns
    val ringtoneList = listOf(
        RingtonePattern("abra", "Abra", 120, "Iconic Phone (2a) syncopated stutter & camera arc chime"),
        RingtonePattern("anna", "Anna", 140, "Fast electronic rhythm with staccato LED pulses"),
        RingtonePattern("beetle", "Beetle", 110, "Heavy sub-bass kicks with alternating strip flashes"),
        RingtonePattern("clwb", "Clwb", 128, "Club progressive wave chasing across 24-segment arc"),
        RingtonePattern("coded", "Coded", 130, "Digital telemetry & Morse code rapid bursts"),
        RingtonePattern("crossing", "Crossing", 105, "Subway alert chime with clockwise radar arc sweep"),
        RingtonePattern("dolphin", "Dolphin", 124, "Acoustic sonar pings with bottom ribbon accents"),
        RingtonePattern("hammer", "Hammer", 135, "Industrial heavy kick with full 26-channel strobe"),
        RingtonePattern("latency", "Latency", 90, "Ambient breathing build-up and glowing arc"),
        RingtonePattern("plot", "Plot", 115, "Cinematic suspense pulse and rising crescendo"),
        RingtonePattern("rapid", "Rapid", 160, "160 BPM high-velocity strobe burst"),
        RingtonePattern("vibrate", "Vibrate", 100, "Synchronized haptic vibration with arc sweep")
    )

    private val _selectedRingtone = MutableStateFlow("Abra")
    val selectedRingtone: StateFlow<String> = _selectedRingtone.asStateFlow()

    private val _isPlayingRingtone = MutableStateFlow(false)
    val isPlayingRingtone: StateFlow<Boolean> = _isPlayingRingtone.asStateFlow()

    // Gemini AI State
    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    private val _aiPatterns = MutableStateFlow<List<AiGlyphPattern>>(emptyList())
    val aiPatterns: StateFlow<List<AiGlyphPattern>> = _aiPatterns.asStateFlow()

    private val _latestAiPattern = MutableStateFlow<AiGlyphPattern?>(null)
    val latestAiPattern: StateFlow<AiGlyphPattern?> = _latestAiPattern.asStateFlow()

    private val _voiceLogs = MutableStateFlow<List<VoiceCommandLog>>(emptyList())
    val voiceLogs: StateFlow<List<VoiceCommandLog>> = _voiceLogs.asStateFlow()

    private val _isVoiceProcessing = MutableStateFlow(false)
    val isVoiceProcessing: StateFlow<Boolean> = _isVoiceProcessing.asStateFlow()

    private val _lastVoiceFeedback = MutableStateFlow<String?>(null)
    val lastVoiceFeedback: StateFlow<String?> = _lastVoiceFeedback.asStateFlow()

    // Sysfs & Hardware Logs
    private val _terminalLogs = MutableStateFlow<List<ExecutionResult>>(emptyList())
    val terminalLogs: StateFlow<List<ExecutionResult>> = _terminalLogs.asStateFlow()

    private val _detectedSysfsNodes = MutableStateFlow<List<String>>(emptyList())
    val detectedSysfsNodes: StateFlow<List<String>> = _detectedSysfsNodes.asStateFlow()

    private val _isExecutingCommand = MutableStateFlow(false)
    val isExecutingCommand: StateFlow<Boolean> = _isExecutingCommand.asStateFlow()

    private var animationJob: Job? = null
    private var timerJob: Job? = null
    private var ringtoneJob: Job? = null

    init {
        restartAnimation(GlyphAnimationType.STATIC)
        scanHardware()
    }

    // -------------------------------------------------------------
    // NAVIGATION & THEME
    // -------------------------------------------------------------
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        vibrate(15)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        _glyphState.update { it.copy(themeMode = mode) }
        vibrate(20)
    }

    fun toggle120HzMode() {
        val next = !_glyphState.value.is120HzOptimized
        _glyphState.update { it.copy(is120HzOptimized = next) }
        vibrate(25)
    }

    // -------------------------------------------------------------
    // CORE HARDWARE CONTROLS
    // -------------------------------------------------------------
    fun setMasterToggle(isOn: Boolean) {
        _glyphState.update { it.copy(isMasterOn = isOn) }
        vibrate(30)
        if (isOn) {
            restartAnimation(_glyphState.value.activePreset)
        } else {
            animationJob?.cancel()
            ringtoneJob?.cancel()
            pushHardwareBrightness(0)
        }
    }

    fun setBrightness(brightness: Float) {
        _glyphState.update { it.copy(brightness = brightness.coerceIn(0.05f, 1.0f)) }
        pushHardwareBrightness((brightness * 4095).toInt())
    }

    fun setPreset(type: GlyphAnimationType) {
        _glyphState.update { it.copy(activePreset = type, isTorchMode = false) }
        vibrate(20)
        restartAnimation(type)
    }

    fun toggleTorch() {
        val nextTorch = !_glyphState.value.isTorchMode
        _glyphState.update {
            it.copy(
                isTorchMode = nextTorch,
                isMasterOn = true,
                activePreset = if (nextTorch) GlyphAnimationType.STATIC else it.activePreset,
                strip1Segments = List(24) { 1.0f },
                strip2Value = 1.0f,
                strip3Value = 1.0f,
                brightness = if (nextTorch) 1.0f else it.brightness
            )
        }
        vibrate(40)
        synth.playTone(SoundType.CLICK)
        if (nextTorch) {
            pushHardwareBrightness(4095)
        }
    }

    fun toggleFlipToGlyph() {
        val next = !_glyphState.value.isFlipToGlyphActive
        _glyphState.update { it.copy(isFlipToGlyphActive = next) }
        vibrate(35)
        synth.playTone(SoundType.BLEEP_HIGH)
        if (next) {
            triggerFlipAnimation()
        }
    }

    private fun triggerFlipAnimation() {
        viewModelScope.launch {
            repeat(2) {
                _glyphState.update { it.copy(strip1Segments = List(24) { 1f }, strip2Value = 1f, strip3Value = 1f) }
                delay(120)
                _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 0f) }
                delay(120)
            }
            restartAnimation(_glyphState.value.activePreset)
        }
    }

    fun toggleStrip(stripIndex: Int) {
        _glyphState.update { state ->
            when (stripIndex) {
                0 -> {
                    val anyOn = state.strip1Segments.any { it > 0.1f }
                    state.copy(strip1Segments = List(24) { if (anyOn) 0.0f else 1.0f })
                }
                24 -> state.copy(strip2Value = if (state.strip2Value > 0.1f) 0.0f else 1.0f)
                25 -> state.copy(strip3Value = if (state.strip3Value > 0.1f) 0.0f else 1.0f)
                else -> state
            }
        }
        vibrate(15)
        synth.playTone(SoundType.CLICK)
    }

    fun toggleArcSegment(index: Int) {
        if (index in 0 until 24) {
            _glyphState.update { state ->
                val updated = state.strip1Segments.toMutableList()
                updated[index] = if (updated[index] > 0.1f) 0.0f else 1.0f
                state.copy(strip1Segments = updated)
            }
            vibrate(10)
        }
    }

    // -------------------------------------------------------------
    // ANIMATION ENGINE LOOP
    // -------------------------------------------------------------
    private fun restartAnimation(type: GlyphAnimationType) {
        animationJob?.cancel()
        if (!_glyphState.value.isMasterOn) return

        animationJob = viewModelScope.launch {
            when (type) {
                GlyphAnimationType.STATIC -> {
                    _glyphState.update {
                        it.copy(
                            strip1Segments = List(24) { 1.0f },
                            strip2Value = 1.0f,
                            strip3Value = 1.0f
                        )
                    }
                }
                GlyphAnimationType.PULSE -> {
                    var phase = 0.0
                    while (isActive) {
                        val factor = (0.5 + 0.5 * sin(phase)).toFloat()
                        _glyphState.update {
                            it.copy(
                                strip1Segments = List(24) { factor },
                                strip2Value = factor,
                                strip3Value = factor
                            )
                        }
                        phase += 0.1
                        delay(25)
                    }
                }
                GlyphAnimationType.WAVE -> {
                    var head = 0
                    while (isActive) {
                        val list = List(24) { i ->
                            val dist = (i - head + 24) % 24
                            if (dist < 8) (1.0f - (dist / 8.0f)) else 0.0f
                        }
                        _glyphState.update {
                            it.copy(
                                strip1Segments = list,
                                strip2Value = if (head % 6 == 0) 1.0f else 0.2f,
                                strip3Value = if (head % 12 == 0) 1.0f else 0.2f
                            )
                        }
                        head = (head + 1) % 24
                        delay(45)
                    }
                }
                GlyphAnimationType.STROBE -> {
                    while (isActive) {
                        _glyphState.update {
                            it.copy(
                                strip1Segments = List(24) { 1.0f },
                                strip2Value = 1.0f,
                                strip3Value = 1.0f
                            )
                        }
                        delay(60)
                        _glyphState.update {
                            it.copy(
                                strip1Segments = List(24) { 0.0f },
                                strip2Value = 0.0f,
                                strip3Value = 0.0f
                            )
                        }
                        delay(60)
                    }
                }
                GlyphAnimationType.RADAR -> {
                    var angleStep = 0
                    while (isActive) {
                        val list = List(24) { i ->
                            if (i == angleStep || i == (angleStep + 1) % 24) 1.0f else 0.05f
                        }
                        _glyphState.update {
                            it.copy(
                                strip1Segments = list,
                                strip2Value = if (angleStep in 18..23) 1.0f else 0.0f,
                                strip3Value = if (angleStep in 0..6) 1.0f else 0.0f
                            )
                        }
                        angleStep = (angleStep + 1) % 24
                        delay(50)
                    }
                }
                GlyphAnimationType.HEARTBEAT -> {
                    while (isActive) {
                        _glyphState.update { it.copy(strip1Segments = List(24) { 1.0f }, strip2Value = 1f, strip3Value = 1f) }
                        delay(90)
                        _glyphState.update { it.copy(strip1Segments = List(24) { 0.1f }, strip2Value = 0.1f, strip3Value = 0.1f) }
                        delay(80)
                        _glyphState.update { it.copy(strip1Segments = List(24) { 1.0f }, strip2Value = 1f, strip3Value = 1f) }
                        delay(120)
                        _glyphState.update { it.copy(strip1Segments = List(24) { 0.0f }, strip2Value = 0f, strip3Value = 0f) }
                        delay(700)
                    }
                }
                GlyphAnimationType.BATTERY_METER -> {
                    val batteryLevel = getBatteryLevel()
                    val activeSegments = ((batteryLevel / 100f) * 24).toInt().coerceIn(1, 24)
                    _glyphState.update {
                        it.copy(
                            strip1Segments = List(24) { idx -> if (idx < activeSegments) 1.0f else 0.0f },
                            strip2Value = 1.0f,
                            strip3Value = if (batteryLevel > 50) 1.0f else 0.0f
                        )
                    }
                }
                else -> {}
            }
        }
    }

    // -------------------------------------------------------------
    // GLYPH TIMER (Matches Screenshot 1)
    // -------------------------------------------------------------
    fun selectTimerPreset(minutes: Int) {
        _selectedPresetMinutes.value = minutes
        _timerTotalSec.value = minutes * 60
        _timerRemainingSec.value = minutes * 60
        vibrate(20)
        synth.playTone(SoundType.CLICK)
    }

    fun setCustomTimerMinutes(minutes: Int) {
        val m = minutes.coerceIn(1, 120)
        _selectedPresetMinutes.value = m
        _timerTotalSec.value = m * 60
        _timerRemainingSec.value = m * 60
    }

    fun toggleTimer() {
        if (_isTimerRunning.value) {
            stopTimer()
        } else {
            startTimerCountdown(_timerRemainingSec.value)
        }
    }

    fun startTimer(totalSeconds: Int = 15) {
        startTimerCountdown(totalSeconds)
    }

    fun startTimerCountdown(totalSeconds: Int) {
        timerJob?.cancel()
        _isTimerRunning.value = true
        _timerTotalSec.value = totalSeconds
        _timerRemainingSec.value = totalSeconds
        _glyphState.update { it.copy(activePreset = GlyphAnimationType.TIMER_PROGRESS, timerProgress = 1.0f, isTimerRunning = true) }
        synth.playTone(SoundType.BLEEP_HIGH)
        vibrate(40)

        timerJob = viewModelScope.launch {
            val totalMs = totalSeconds * 1000L
            val interval = 100L
            var elapsed = 0L

            while (elapsed <= totalMs && isActive && _isTimerRunning.value) {
                val remainingMs = totalMs - elapsed
                val remainingSec = (remainingMs / 1000L).toInt()
                _timerRemainingSec.value = remainingSec

                val progress = 1.0f - (elapsed.toFloat() / totalMs.toFloat())
                val segmentsCount = (progress * 24).toInt()

                _glyphState.update {
                    it.copy(
                        timerProgress = progress,
                        timerRemainingSeconds = remainingSec,
                        strip1Segments = List(24) { idx -> if (idx < segmentsCount) 1.0f else 0.0f },
                        strip2Value = if (progress > 0.5f) 1.0f else 0.0f,
                        strip3Value = if (progress > 0.1f) 1.0f else 0.0f
                    )
                }

                if (elapsed % 1000L == 0L && remainingSec <= 5 && remainingSec > 0) {
                    synth.playTone(SoundType.CLICK)
                }

                elapsed += interval
                delay(interval)
            }

            // Completion
            if (_isTimerRunning.value) {
                _isTimerRunning.value = false
                _timerRemainingSec.value = 0
                _glyphState.update { it.copy(isTimerRunning = false) }
                synth.playTone(SoundType.NOTHING_CHIME)
                vibrate(200)
                repeat(4) {
                    _glyphState.update { it.copy(strip1Segments = List(24) { 1.0f }, strip2Value = 1f, strip3Value = 1f) }
                    delay(100)
                    _glyphState.update { it.copy(strip1Segments = List(24) { 0.0f }, strip2Value = 0f, strip3Value = 0f) }
                    delay(100)
                }
                restartAnimation(GlyphAnimationType.STATIC)
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _glyphState.update { it.copy(isTimerRunning = false) }
        vibrate(20)
        synth.playTone(SoundType.BLEEP_LOW)
        restartAnimation(GlyphAnimationType.STATIC)
    }

    // -------------------------------------------------------------
    // GLYPH PATTERNS / CALL RINGTONES (Matches Screenshot 2)
    // -------------------------------------------------------------
    fun selectRingtone(name: String) {
        _selectedRingtone.value = name
        _glyphState.update { it.copy(activeRingtone = name) }
        vibrate(25)
        previewRingtone(name)
    }

    fun previewRingtone(patternName: String) {
        ringtoneJob?.cancel()
        animationJob?.cancel()
        _isPlayingRingtone.value = true

        ringtoneJob = viewModelScope.launch {
            when (patternName.lowercase()) {
                "abra" -> playAbraChoreography()
                "anna" -> playAnnaChoreography()
                "beetle" -> playBeetleChoreography()
                "clwb" -> playClwbChoreography()
                "coded" -> playCodedChoreography()
                "crossing" -> playCrossingChoreography()
                "dolphin" -> playDolphinChoreography()
                "hammer" -> playHammerChoreography()
                "latency" -> playLatencyChoreography()
                "plot" -> playPlotChoreography()
                "rapid" -> playRapidChoreography()
                "vibrate" -> playVibrateChoreography()
                else -> playAbraChoreography()
            }
            _isPlayingRingtone.value = false
            restartAnimation(GlyphAnimationType.STATIC)
        }
    }

    private suspend fun playAbraChoreography() {
        // Iconic Nothing Phone 2a syncopated stutter & dual chime
        repeat(2) {
            // Beat 1
            synth.playTone(SoundType.KICK)
            vibrate(30)
            _glyphState.update { it.copy(strip1Segments = List(24) { 1f }, strip2Value = 0f, strip3Value = 1f) }
            delay(120)
            _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 0f) }
            delay(80)

            // Beat 2
            synth.playTone(SoundType.BLEEP_HIGH)
            _glyphState.update { it.copy(strip1Segments = List(24) { if (it in 0..11) 1f else 0f }, strip2Value = 1f, strip3Value = 0f) }
            delay(100)
            _glyphState.update { it.copy(strip1Segments = List(24) { if (it in 12..23) 1f else 0f }, strip2Value = 0f, strip3Value = 1f) }
            delay(100)

            // Poly Chime
            synth.playTone(SoundType.NOTHING_CHIME)
            for (i in 0 until 24 step 3) {
                _glyphState.update { it.copy(strip1Segments = List(24) { idx -> if (idx <= i) 1f else 0f }, strip2Value = 1f, strip3Value = 1f) }
                delay(35)
            }
            delay(200)
            _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 0f) }
            delay(150)
        }
    }

    private suspend fun playAnnaChoreography() {
        repeat(3) {
            synth.playTone(SoundType.GLITCH)
            vibrate(20)
            _glyphState.update { it.copy(strip1Segments = List(24) { if (it % 2 == 0) 1f else 0f }, strip2Value = 1f, strip3Value = 0f) }
            delay(70)
            _glyphState.update { it.copy(strip1Segments = List(24) { if (it % 2 != 0) 1f else 0f }, strip2Value = 0f, strip3Value = 1f) }
            delay(70)
            synth.playTone(SoundType.BLEEP_HIGH)
            _glyphState.update { it.copy(strip1Segments = List(24) { 1f }, strip2Value = 1f, strip3Value = 1f) }
            delay(110)
            _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 0f) }
            delay(80)
        }
    }

    private suspend fun playBeetleChoreography() {
        repeat(3) {
            synth.playTone(SoundType.KICK)
            vibrate(40)
            _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 1f, strip3Value = 0f) }
            delay(130)
            synth.playTone(SoundType.KICK)
            _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 1f) }
            delay(130)
            synth.playTone(SoundType.BLEEP_MID)
            _glyphState.update { it.copy(strip1Segments = List(24) { 1f }, strip2Value = 1f, strip3Value = 1f) }
            delay(150)
            _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 0f) }
            delay(100)
        }
    }

    private suspend fun playClwbChoreography() {
        repeat(2) {
            for (step in 0 until 24) {
                if (step % 6 == 0) {
                    synth.playTone(SoundType.KICK)
                    vibrate(25)
                }
                _glyphState.update {
                    it.copy(
                        strip1Segments = List(24) { idx -> if (idx == step) 1f else 0.1f },
                        strip2Value = if (step > 12) 1f else 0f,
                        strip3Value = if (step <= 12) 1f else 0f
                    )
                }
                delay(30)
            }
        }
    }

    private suspend fun playCodedChoreography() {
        // Morse code telemetry
        val morsePattern = listOf(true, false, true, false, true, true, false, true)
        for (dot in morsePattern) {
            synth.playTone(if (dot) SoundType.BLEEP_HIGH else SoundType.CLICK)
            vibrate(if (dot) 30 else 10)
            _glyphState.update {
                it.copy(
                    strip1Segments = List(24) { if (dot) 1f else 0f },
                    strip2Value = if (dot) 1f else 0f,
                    strip3Value = if (dot) 1f else 0f
                )
            }
            delay(if (dot) 120 else 60)
            _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 0f) }
            delay(80)
        }
    }

    private suspend fun playCrossingChoreography() {
        repeat(2) {
            synth.playTone(SoundType.SWEEP)
            for (i in 0 until 24) {
                _glyphState.update {
                    it.copy(
                        strip1Segments = List(24) { idx -> if (idx == i || idx == (i + 1) % 24) 1f else 0f },
                        strip2Value = if (i in 0..6) 1f else 0f,
                        strip3Value = if (i in 12..18) 1f else 0f
                    )
                }
                delay(40)
            }
        }
    }

    private suspend fun playDolphinChoreography() {
        repeat(4) {
            synth.playTone(SoundType.BLEEP_HIGH)
            _glyphState.update { it.copy(strip1Segments = List(24) { idx -> if (idx < 12) 1f else 0f }, strip2Value = 1f, strip3Value = 0f) }
            delay(90)
            synth.playTone(SoundType.BLEEP_MID)
            _glyphState.update { it.copy(strip1Segments = List(24) { idx -> if (idx >= 12) 1f else 0f }, strip2Value = 0f, strip3Value = 1f) }
            delay(90)
            _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 0f) }
            delay(60)
        }
    }

    private suspend fun playHammerChoreography() {
        repeat(4) {
            synth.playTone(SoundType.KICK)
            vibrate(50)
            _glyphState.update { it.copy(strip1Segments = List(24) { 1f }, strip2Value = 1f, strip3Value = 1f) }
            delay(100)
            _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 0f) }
            delay(100)
        }
    }

    private suspend fun playLatencyChoreography() {
        synth.playTone(SoundType.SWEEP)
        for (i in 0..24) {
            _glyphState.update {
                it.copy(
                    strip1Segments = List(24) { idx -> if (idx < i) 1f else 0f },
                    strip2Value = i / 24f,
                    strip3Value = i / 24f
                )
            }
            delay(45)
        }
        delay(300)
        _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 0f) }
    }

    private suspend fun playPlotChoreography() {
        synth.playTone(SoundType.GLITCH)
        repeat(3) {
            _glyphState.update { it.copy(strip1Segments = List(24) { if (it in 8..16) 1f else 0f }, strip2Value = 1f, strip3Value = 0f) }
            delay(120)
            _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 1f) }
            delay(120)
        }
    }

    private suspend fun playRapidChoreography() {
        repeat(8) {
            synth.playTone(SoundType.CLICK)
            vibrate(15)
            _glyphState.update { it.copy(strip1Segments = List(24) { 1f }, strip2Value = 1f, strip3Value = 1f) }
            delay(40)
            _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 0f) }
            delay(40)
        }
    }

    private suspend fun playVibrateChoreography() {
        repeat(5) {
            vibrate(80)
            synth.playTone(SoundType.BLEEP_LOW)
            _glyphState.update { it.copy(strip1Segments = List(24) { 1f }, strip2Value = 1f, strip3Value = 1f) }
            delay(80)
            _glyphState.update { it.copy(strip1Segments = List(24) { 0f }, strip2Value = 0f, strip3Value = 0f) }
            delay(80)
        }
    }

    // -------------------------------------------------------------
    // GEMINI AI INTEGRATION (Pattern Generation & Voice Assistant)
    // -------------------------------------------------------------
    fun generateAiGlyphPattern(prompt: String) {
        viewModelScope.launch {
            _isAiGenerating.value = true
            vibrate(20)
            val result = GeminiService.generatePattern(prompt)
            result.onSuccess { pattern ->
                _latestAiPattern.value = pattern
                _aiPatterns.update { listOf(pattern) + it }
                playAiPattern(pattern)
            }
            _isAiGenerating.value = false
        }
    }

    fun playAiPattern(pattern: AiGlyphPattern) {
        ringtoneJob?.cancel()
        animationJob?.cancel()
        _isPlayingRingtone.value = true

        ringtoneJob = viewModelScope.launch {
            _glyphState.update { it.copy(activePreset = GlyphAnimationType.AI_CHOREOGRAPHY) }
            repeat(2) {
                for (frame in pattern.frames) {
                    if (frame.toneFrequency != null) {
                        synth.playFrequency(frame.toneFrequency, frame.toneDurationSec)
                    } else {
                        synth.playTone(SoundType.BLEEP_HIGH)
                    }
                    vibrate(20)

                    _glyphState.update {
                        it.copy(
                            strip1Segments = List(24) { idx -> if (idx in frame.strip1ActiveRange) 1f else 0.05f },
                            strip2Value = frame.strip2,
                            strip3Value = frame.strip3
                        )
                    }
                    delay(frame.durationMs)
                }
            }
            _isPlayingRingtone.value = false
            restartAnimation(GlyphAnimationType.STATIC)
        }
    }

    fun executeNaturalLanguageCommand(commandText: String) {
        viewModelScope.launch {
            _isVoiceProcessing.value = true
            vibrate(25)

            val action: VoiceCommandAction = GeminiService.parseVoiceCommand(commandText)
            _lastVoiceFeedback.value = action.spokenResponse

            // Execute the action on the app state & hardware!
            when (action.action) {
                "TORCH_TOGGLE" -> toggleTorch()
                "FLIP_TOGGLE" -> toggleFlipToGlyph()
                "SET_THEME_LIGHT" -> setThemeMode(ThemeMode.LIGHT)
                "SET_THEME_DARK" -> setThemeMode(ThemeMode.DARK)
                "START_TIMER" -> {
                    val sec = if (action.paramInt > 0) action.paramInt else 60
                    _currentScreen.value = AppScreen.TIMER
                    startTimerCountdown(sec)
                }
                "SET_BRIGHTNESS" -> {
                    val b = if (action.paramFloat > 0f) action.paramFloat else 0.8f
                    setBrightness(b)
                }
                "PLAY_PATTERN" -> {
                    val name = if (action.paramString.isNotBlank()) action.paramString else "Abra"
                    _selectedRingtone.value = name
                    _currentScreen.value = AppScreen.PATTERNS
                    previewRingtone(name)
                }
                "SET_PRESET" -> {
                    val p = try {
                        GlyphAnimationType.valueOf(action.paramString)
                    } catch (e: Exception) {
                        GlyphAnimationType.PULSE
                    }
                    setPreset(p)
                    _currentScreen.value = AppScreen.VISUALS
                }
            }

            val log = VoiceCommandLog(
                id = System.currentTimeMillis().toString(),
                queryText = commandText,
                responseText = action.spokenResponse,
                actionTaken = action.action
            )
            _voiceLogs.update { listOf(log) + it.take(20) }
            _isVoiceProcessing.value = false
        }
    }

    // -------------------------------------------------------------
    // MUSIC VISUALIZER & SOUNDBOARD
    // -------------------------------------------------------------
    fun triggerMusicBeat(trackBeatIndex: Int) {
        viewModelScope.launch {
            val sound = when (trackBeatIndex % 6) {
                0 -> SoundType.KICK
                1 -> SoundType.BLEEP_HIGH
                2 -> SoundType.GLITCH
                3 -> SoundType.BLEEP_MID
                4 -> SoundType.SWEEP
                else -> SoundType.CLICK
            }
            synth.playTone(sound)
            vibrate(18)

            val activeSegmentsCount = (8..24).random()
            val list = List(24) { idx -> if (idx < activeSegmentsCount) (0.5f + kotlin.random.Random.nextFloat() * 0.5f) else 0.05f }

            _glyphState.update {
                it.copy(
                    strip1Segments = list,
                    strip2Value = if ((0..1).random() == 1) 1.0f else 0.2f,
                    strip3Value = if ((0..1).random() == 1) 1.0f else 0.2f
                )
            }
            delay(120)
            _glyphState.update {
                it.copy(
                    strip1Segments = it.strip1Segments.map { v -> v * 0.3f },
                    strip2Value = it.strip2Value * 0.3f,
                    strip3Value = it.strip3Value * 0.3f
                )
            }
        }
    }

    fun triggerSoundPad(soundType: SoundType, padIndex: Int) {
        synth.playTone(soundType)
        vibrate(25)

        viewModelScope.launch {
            val pattern = when (padIndex) {
                0 -> List(24) { if (it in 0..5) 1.0f else 0.0f }
                1 -> List(24) { if (it in 6..11) 1.0f else 0.0f }
                2 -> List(24) { if (it in 12..17) 1.0f else 0.0f }
                3 -> List(24) { if (it in 18..23) 1.0f else 0.0f }
                4 -> List(24) { 1.0f }
                5 -> List(24) { if (it % 2 == 0) 1.0f else 0.0f }
                6 -> List(24) { if (it % 3 == 0) 1.0f else 0.0f }
                else -> List(24) { 1.0f }
            }
            _glyphState.update {
                it.copy(
                    strip1Segments = pattern,
                    strip2Value = if (padIndex % 2 == 0) 1.0f else 0.0f,
                    strip3Value = if (padIndex % 2 != 0) 1.0f else 0.0f
                )
            }
            delay(140)
            if (_glyphState.value.activePreset == GlyphAnimationType.STATIC) {
                _glyphState.update { it.copy(strip1Segments = List(24) { 1.0f }, strip2Value = 1f, strip3Value = 1f) }
            }
        }
    }

    // -------------------------------------------------------------
    // HARDWARE ROOT SYSFS TESTING & SCANNING
    // -------------------------------------------------------------
    fun scanHardware() {
        viewModelScope.launch {
            val nodes = GlyphHardwareManager.scanDeviceLeds()
            _detectedSysfsNodes.value = nodes
        }
    }

    fun runSysfsCommand(command: String, useRoot: Boolean = true) {
        viewModelScope.launch {
            _isExecutingCommand.value = true
            val result = GlyphHardwareManager.executeCommand(command, useRoot)
            _terminalLogs.update { listOf(result) + it.take(25) }
            _isExecutingCommand.value = false
        }
    }

    fun clearTerminalLogs() {
        _terminalLogs.value = emptyList()
    }

    private fun pushHardwareBrightness(value: Int) {
        viewModelScope.launch {
            GlyphHardwareManager.executeCommand("echo $value > /sys/class/leds/aw210xx_led/all_white_leds_br", useRoot = true)
        }
    }

    private fun vibrate(ms: Long) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ms)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun getBatteryLevel(): Int {
        return try {
            val bm = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            78
        }
    }
}
