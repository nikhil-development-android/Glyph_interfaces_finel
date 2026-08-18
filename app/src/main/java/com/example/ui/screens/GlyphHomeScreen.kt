package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundType
import com.example.hardware.GlyphHardwareManager
import com.example.model.GlyphAnimationType
import com.example.model.GlyphState
import com.example.model.PortingDocSection
import com.example.ui.components.Phone2aGlyphCanvas
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.GlyphGlow
import com.example.ui.theme.GlyphOff
import com.example.ui.theme.GlyphOffBorder
import com.example.ui.theme.GlyphWhite
import com.example.ui.theme.GlyphWhiteOff
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingRedDark
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.GlyphViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class GlyphTab(val title: String, val icon: ImageVector) {
    CONTROL("Controller", Icons.Default.Tune),
    MUSIC("Visualizer", Icons.Default.Equalizer),
    TIMER("Timer & Alert", Icons.Default.Timer),
    COMPOSER("Composer", Icons.Default.GridView),
    PORTING_GUIDE("HyperOS Port", Icons.Default.Code)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlyphHomeScreen(viewModel: GlyphViewModel) {
    val glyphState by viewModel.glyphState.collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()
    val detectedNodes by viewModel.detectedSysfsNodes.collectAsState()
    val isExecuting by viewModel.isExecutingCommand.collectAsState()

    var selectedTab by remember { mutableStateOf(GlyphTab.CONTROL) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Red accent square dot (Nothing signature design)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NothingRed, RoundedCornerShape(1.dp))
                        )
                        Column {
                            Text(
                                text = "GLYPH INTERFACE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "NOTHING PHONE (2A) // PACMAN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Master Switch with glowing indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (glyphState.isMasterOn) StatusActive else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = glyphState.isMasterOn,
                            onCheckedChange = { viewModel.setMasterToggle(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GlyphWhite,
                                checkedTrackColor = NothingRed
                            ),
                            modifier = Modifier.testTag("master_glyph_switch")
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Preview: Interactive Phone (2a) Backplate Visualization Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D0D0D), Color(0xFF050505))
                        )
                    )
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(32.dp))
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                // Subtle radial backdrop glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x22FF3131), Color.Transparent),
                                radius = 400f
                            )
                        )
                )

                Phone2aGlyphCanvas(
                    glyphState = glyphState,
                    onSegmentClick = { segIndex ->
                        if (segIndex in 0..23) {
                            viewModel.toggleArcSegment(segIndex)
                        } else {
                            viewModel.toggleStrip(segIndex)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Status Badge Overlay (Top Left: Interface Status)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF141414).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (glyphState.isMasterOn) StatusActive else TextMuted)
                        )
                        Text(
                            text = if (glyphState.isMasterOn) "INTERFACE ACTIVE" else "STANDBY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (glyphState.isMasterOn) GlyphWhite else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                // Quick Status Overlay Badge (Top Right: Brightness % / Torch)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF141414).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (glyphState.isTorchMode) NothingRed else GlyphWhite,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (glyphState.isTorchMode) "TORCH MAX" else "${(glyphState.brightness * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = GlyphWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Scrollable Tab Navigation with Sophisticated Dark Styling
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        height = 2.dp,
                        color = NothingRed
                    )
                },
                divider = {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                }
            ) {
                GlyphTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                    letterSpacing = 1.2.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (selectedTab == tab) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(18.dp),
                                tint = if (selectedTab == tab) NothingRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Tab Content Body
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (selectedTab) {
                    GlyphTab.CONTROL -> ControlTabContent(viewModel, glyphState)
                    GlyphTab.MUSIC -> MusicVisualizerTabContent(viewModel, glyphState)
                    GlyphTab.TIMER -> TimerAndAlertTabContent(viewModel, glyphState)
                    GlyphTab.COMPOSER -> ComposerTabContent(viewModel, glyphState)
                    GlyphTab.PORTING_GUIDE -> PortingGuideTabContent(
                        viewModel = viewModel,
                        detectedNodes = detectedNodes,
                        terminalLogs = terminalLogs,
                        isExecuting = isExecuting,
                        onCopy = { text, label ->
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

// =================================================================
// 1. CONTROLLER & TORCH TAB
// =================================================================
@Composable
private fun ControlTabContent(viewModel: GlyphViewModel, glyphState: GlyphState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brightness & Torch Control Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BRIGHTNESS CONTROL",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        )
                        Text(
                            text = if (glyphState.brightness >= 0.95f) "MAX" else "${(glyphState.brightness * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NothingRed,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stylized Gradient Slider Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = glyphState.brightness,
                            onValueChange = { viewModel.setBrightness(it) },
                            valueRange = 0.05f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = GlyphWhite,
                                activeTrackColor = NothingRed,
                                inactiveTrackColor = Color(0x33FF3131)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("brightness_slider")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleTorch() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (glyphState.isTorchMode) NothingRed else DarkSurfaceElevated,
                                contentColor = GlyphWhite
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("torch_button")
                        ) {
                            Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Glyph Torch", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { viewModel.toggleFlipToGlyph() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (glyphState.isFlipToGlyphActive) NothingRedDark else DarkSurfaceElevated,
                                contentColor = GlyphWhite
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Flip to Glyph", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Individual Strip Zone Toggles
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "PHYSICAL STRIP CHANNELS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StripQuickToggle(
                            label = "Strip 1 (Arc)",
                            sublabel = "24 LEDs",
                            isActive = glyphState.strip1Segments.any { it > 0.1f },
                            onClick = { viewModel.toggleStrip(0) },
                            modifier = Modifier.weight(1f)
                        )
                        StripQuickToggle(
                            label = "Strip 2",
                            sublabel = "Top-Right",
                            isActive = glyphState.strip2Value > 0.1f,
                            onClick = { viewModel.toggleStrip(24) },
                            modifier = Modifier.weight(1f)
                        )
                        StripQuickToggle(
                            label = "Strip 3",
                            sublabel = "Bottom",
                            isActive = glyphState.strip3Value > 0.1f,
                            onClick = { viewModel.toggleStrip(25) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Preset Animation Selector
        item {
            Text(
                text = "ANIMATION PRESETS",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextTertiary,
                    letterSpacing = 2.sp,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        items(GlyphAnimationType.values()) { preset ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (glyphState.activePreset == preset) Color(0xFF181818) else DarkSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (glyphState.activePreset == preset) NothingRed else DarkBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setPreset(preset) }
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = GlyphWhite
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        )
                    }
                    if (glyphState.activePreset == preset) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(NothingRed, CircleShape)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StripQuickToggle(
    label: String,
    sublabel: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) Color(0xFF1E1E1E) else Color(0xFF0D0D0D),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) NothingRed else DarkBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isActive) GlyphWhite else TextMuted,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isActive) NothingRed else TextSubtle,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

// =================================================================
// 2. MUSIC VISUALIZER TAB
// =================================================================
@Composable
private fun MusicVisualizerTabContent(viewModel: GlyphViewModel, glyphState: GlyphState) {
    val coroutineScope = rememberCoroutineScope()
    var isLoopPlaying by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = NothingRed)
                        Text(
                            text = "GLYPH MUSIC EQUALIZER",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Phone (2a)'s 24-segment arc strip acts as an acoustic spectrum visualizer, reacting to transients, bass hits, and frequencies in real-time.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                isLoopPlaying = !isLoopPlaying
                                if (isLoopPlaying) {
                                    coroutineScope.launch {
                                        var beat = 0
                                        while (isLoopPlaying) {
                                            viewModel.triggerMusicBeat(beat++)
                                            delay(160)
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLoopPlaying) NothingRed else DarkSurfaceElevated,
                                contentColor = GlyphWhite
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (isLoopPlaying) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isLoopPlaying) "Stop Beat Loop" else "Start Beat Loop",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "MANUAL BEAT TRIGGERS",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextTertiary,
                    letterSpacing = 2.sp,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BeatTriggerButton("Sub Bass", "Drop 808", onClick = { viewModel.triggerMusicBeat(0) }, modifier = Modifier.weight(1f))
                    BeatTriggerButton("Hi-Hat", "16-Beat", onClick = { viewModel.triggerMusicBeat(1) }, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BeatTriggerButton("Snare / Clap", "Punch", onClick = { viewModel.triggerMusicBeat(2) }, modifier = Modifier.weight(1f))
                    BeatTriggerButton("Glitch Riff", "Cyber", onClick = { viewModel.triggerMusicBeat(3) }, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BeatTriggerButton("Synth Lead", "Pulse", onClick = { viewModel.triggerMusicBeat(4) }, modifier = Modifier.weight(1f))
                    BeatTriggerButton("Full Drop", "All 26 CH", onClick = { viewModel.triggerMusicBeat(5) }, modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun BeatTriggerButton(label: String, sublabel: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = GlyphWhite))
            Spacer(modifier = Modifier.height(2.dp))
            Text(sublabel, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace))
        }
    }
}

// =================================================================
// 3. TIMER & ESSENTIAL ALERTS TAB
// =================================================================
@Composable
private fun TimerAndAlertTabContent(viewModel: GlyphViewModel, glyphState: GlyphState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Glyph Countdown Timer
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = NothingRed)
                        Text(
                            text = "GLYPH TIMER PROGRESS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The 24-segment arc smoothly counts down and depletes counter-clockwise as time ticks down, followed by a double flash chime.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimerPresetButton("10s", 10, onClick = { viewModel.startTimer(10) }, modifier = Modifier.weight(1f))
                        TimerPresetButton("20s", 20, onClick = { viewModel.startTimer(20) }, modifier = Modifier.weight(1f))
                        TimerPresetButton("30s", 30, onClick = { viewModel.startTimer(30) }, modifier = Modifier.weight(1f))
                        TimerPresetButton("60s", 60, onClick = { viewModel.startTimer(60) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Essential Notifications Feature
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = NothingRed)
                        Text(
                            text = "ESSENTIAL NOTIFICATIONS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Keeps Strip 2 or Strip 3 permanently lit when high-priority notifications (WhatsApp, Telegram, Missed Calls) are unread.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleStrip(24) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = GlyphWhite),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Strip 2 Alert", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { viewModel.toggleStrip(25) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = GlyphWhite),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Strip 3 Alert", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TimerPresetButton(label: String, seconds: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkSurfaceElevated,
            contentColor = GlyphWhite
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(44.dp)
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

// =================================================================
// 4. COMPOSER TAB
// =================================================================
@Composable
private fun ComposerTabContent(viewModel: GlyphViewModel, glyphState: GlyphState) {
    val soundTypes = SoundType.values()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "GLYPH SOUNDBOARD & COMPOSER",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap any pad to play custom Nothing sound frequencies paired with synchronized LED lighting sequences.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val leftIdx = row * 2
                        val rightIdx = row * 2 + 1

                        if (leftIdx < soundTypes.size) {
                            SoundPadButton(
                                soundType = soundTypes[leftIdx],
                                index = leftIdx,
                                onClick = { viewModel.triggerSoundPad(soundTypes[leftIdx], leftIdx) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rightIdx < soundTypes.size) {
                            SoundPadButton(
                                soundType = soundTypes[rightIdx],
                                index = rightIdx,
                                onClick = { viewModel.triggerSoundPad(soundTypes[rightIdx], rightIdx) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundPadButton(
    soundType: SoundType,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier
            .height(76.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = soundType.padName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NothingRed,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                )
                Box(modifier = Modifier.size(6.dp).background(GlyphWhite, CircleShape))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = soundType.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = GlyphWhite
                )
            )
        }
    }
}

// =================================================================
// 5. HYPEROS PORTING GUIDE & SYSFS BRIDGE TAB
// =================================================================
@Composable
private fun PortingGuideTabContent(
    viewModel: GlyphViewModel,
    detectedNodes: List<String>,
    terminalLogs: List<com.example.hardware.ExecutionResult>,
    isExecuting: Boolean,
    onCopy: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device Identification Status Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PORTING HARDWARE BRIDGE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (GlyphHardwareManager.isPhone2a) StatusActive.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (GlyphHardwareManager.isPhone2a) StatusActive else DarkBorder)
                        ) {
                            Text(
                                text = if (GlyphHardwareManager.isPhone2a) "2A DETECTED" else "PORT SIMULATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (GlyphHardwareManager.isPhone2a) StatusActive else TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This bridge provides direct low-level kernel node access, root sysfs diagnostics, and complete HyperOS/AOSP driver implementation blueprints.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.scanHardware() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = GlyphWhite),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan /sys Nodes", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { viewModel.runSysfsCommand("ls -l /sys/class/leds/", useRoot = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = GlyphWhite),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Root 'ls' Test", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Live Sysfs Hardware Nodes Tester
        item {
            Text(
                text = "NOTHING 2A SYSFS HARDWARE NODES",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextTertiary,
                    letterSpacing = 2.sp,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        items(GlyphHardwareManager.knownSysfsNodes) { node ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = node.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = node.channelRange,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NothingRed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = node.path,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = GlyphWhiteOff,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = node.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.runSysfsCommand(node.testCommand, useRoot = true) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Write (Root)", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        OutlinedButton(
                            onClick = { onCopy(node.testCommand, "Sysfs command") },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Copy Cmd", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Live Sysfs Command Terminal Output Box
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070709)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22222E))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(StatusActive, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TERMINAL LOG",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            )
                        }
                        if (terminalLogs.isNotEmpty()) {
                            Text(
                                text = "CLEAR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NothingRed,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.clickable { viewModel.clearTerminalLogs() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (terminalLogs.isEmpty()) {
                        Text(
                            text = "# No commands run yet. Tap 'Test Write' above to probe /sys/class/leds.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        )
                    } else {
                        terminalLogs.take(5).forEach { log ->
                            Text(
                                text = "$ ${log.command}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = GlyphWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )
                            if (log.output.isNotBlank()) {
                                Text(
                                    text = log.output,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = StatusActive,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            if (log.error.isNotBlank()) {
                                Text(
                                    text = log.error,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = StatusError,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // Porting Guide Blueprints & Documentation
        item {
            Text(
                text = "HYPEROS / AOSP IMPLEMENTATION BLUEPRINTS",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextTertiary,
                    letterSpacing = 2.sp,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        items(GlyphHardwareManager.portingGuides) { guide ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = guide.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = GlyphWhite
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = guide.summary,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Code snippet container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF09090D), RoundedCornerShape(12.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = guide.codeSnippet,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GlyphWhiteOff,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { onCopy(guide.codeSnippet, guide.title) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceElevated,
                                contentColor = GlyphWhite
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Copy Code Snippet", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
