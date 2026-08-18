package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.GlyphState
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.GlyphGlow
import com.example.ui.theme.GlyphOff
import com.example.ui.theme.GlyphOffBorder
import com.example.ui.theme.GlyphWhite
import com.example.ui.theme.NothingRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive Vector Canvas rendering Nothing Phone (2a) backplate and its 3 Glyph LED Strips.
 * Strip 1: 24-Segment Arc around camera module
 * Strip 2: Top-Right Slant Strip
 * Strip 3: Bottom Accent Strip
 */
@Composable
fun Phone2aGlyphCanvas(
    glyphState: GlyphState,
    onSegmentClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val masterOn = glyphState.isMasterOn
    val brightness = glyphState.brightness

    Box(
        modifier = modifier
            .aspectRatio(0.48f) // Phone 2a aspect ratio ~ 20:9
            .padding(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(masterOn) {
                    detectTapGestures { offset ->
                        // Detect click on strips or segments
                        // Left/Top Arc click
                        if (offset.y < size.height * 0.45f) {
                            if (offset.x < size.width * 0.5f) {
                                onSegmentClick(0) // Arc trigger
                            } else {
                                onSegmentClick(24) // Strip 2 trigger
                            }
                        } else if (offset.y > size.height * 0.5f && offset.y < size.height * 0.85f) {
                            onSegmentClick(25) // Strip 3 trigger
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Phone Body Chassis (Rounded Rectangle with Dark Glass Tint)
            val phoneRect = Rect(0f, 0f, w, h)
            val cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)

            // Outer Frame
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF141414), Color(0xFF0A0A0A), Color(0xFF000000))
                ),
                topLeft = Offset(0f, 0f),
                size = Size(w, h),
                cornerRadius = cornerRadius
            )
            drawRoundRect(
                color = Color(0xFF222222),
                topLeft = Offset(0f, 0f),
                size = Size(w, h),
                cornerRadius = cornerRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner Translucent Glass Layer
            val innerPadding = w * 0.035f
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF111111), Color(0xFF050505)),
                    center = Offset(w * 0.5f, h * 0.3f),
                    radius = w * 0.8f
                ),
                topLeft = Offset(innerPadding, innerPadding),
                size = Size(w - innerPadding * 2, h - innerPadding * 2),
                cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
            )

            // 2. Hardware Texture & Internal Circuitry details (Phone 2a aesthetic)
            drawPhone2aInternals(w, h)

            // 3. Central Dual-Camera Visor ("The Eyes" of 2a)
            val cameraCenter = Offset(w * 0.5f, h * 0.26f)
            val cameraVisorRadius = w * 0.24f

            // Camera Circular Visor Ring
            drawCircle(
                color = Color(0xFF050505),
                radius = cameraVisorRadius,
                center = cameraCenter
            )
            drawCircle(
                color = Color(0xFF1F1F1F),
                radius = cameraVisorRadius,
                center = cameraCenter,
                style = Stroke(width = 2.dp.toPx())
            )

            // Dual Horizontal Lenses
            val lensRadius = w * 0.08f
            val lensOffset = w * 0.11f

            // Left Lens
            drawLens(Offset(cameraCenter.x - lensOffset, cameraCenter.y), lensRadius)
            // Right Lens
            drawLens(Offset(cameraCenter.x + lensOffset, cameraCenter.y), lensRadius)

            // Red Dot Accent (Signature Nothing 2a detail at top-right of visor)
            val redDotPos = Offset(w * 0.82f, h * 0.16f)
            drawCircle(
                color = NothingRed,
                radius = 3.5.dp.toPx(),
                center = redDotPos
            )

            // 4. DRAW GLYPH STRIPS
            // -------------------------------------------------------------
            // STRIP 1: 24-Segment Arc (Left and Top-Left around Camera Visor)
            // Arc spans from approx 100 degrees to 330 degrees
            val arcRadius = cameraVisorRadius + w * 0.075f
            val numSegments = 24
            val startAngleDeg = 110f
            val sweepTotalDeg = 215f
            val gapAngleDeg = 1.8f
            val segSweepDeg = (sweepTotalDeg - (numSegments - 1) * gapAngleDeg) / numSegments

            for (i in 0 until numSegments) {
                val segStartAngle = startAngleDeg + i * (segSweepDeg + gapAngleDeg)
                val segVal = if (masterOn) glyphState.strip1Segments.getOrElse(i) { 1.0f } * brightness else 0f
                drawGlyphArcSegment(
                    center = cameraCenter,
                    radius = arcRadius,
                    startAngle = segStartAngle,
                    sweepAngle = segSweepDeg,
                    strokeWidth = 7.dp.toPx(),
                    intensity = segVal
                )
            }

            // STRIP 2: Top-Right Slant Accent Strip (Upper Right Visor)
            val s2P1 = Offset(w * 0.68f, h * 0.105f)
            val s2P2 = Offset(w * 0.88f, h * 0.18f)
            val s2Val = if (masterOn) glyphState.strip2Value * brightness else 0f
            drawGlyphLinearStrip(s2P1, s2P2, strokeWidth = 7.5.dp.toPx(), intensity = s2Val)

            // STRIP 3: Bottom Slant Ribbon Strip (Lower Mid Section)
            val s3P1 = Offset(w * 0.38f, h * 0.52f)
            val s3P2 = Offset(w * 0.58f, h * 0.63f)
            val s3Val = if (masterOn) glyphState.strip3Value * brightness else 0f
            drawGlyphLinearStrip(s3P1, s3P2, strokeWidth = 7.5.dp.toPx(), intensity = s3Val)
        }
    }
}

private fun DrawScope.drawLens(center: Offset, radius: Float) {
    // Outer metallic ring
    drawCircle(
        color = Color(0xFF1A1A22),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color(0xFF3E3E4E),
        radius = radius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
    // Dark optical lens
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF0F1A24), Color(0xFF05070A)),
            center = center,
            radius = radius * 0.8f
        ),
        radius = radius * 0.8f,
        center = center
    )
    // Blue/cyan antireflective flare reflection
    drawCircle(
        color = Color(0x3338B6FF),
        radius = radius * 0.35f,
        center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f)
    )
    drawCircle(
        color = Color(0x88FFFFFF),
        radius = radius * 0.1f,
        center = Offset(center.x - radius * 0.22f, center.y - radius * 0.22f)
    )
}

private fun DrawScope.drawPhone2aInternals(w: Float, h: Float) {
    // Circuit line accents
    val pcbPaint = Color(0x18FFFFFF)

    // Ribbon cable paths
    val path = Path().apply {
        moveTo(w * 0.2f, h * 0.45f)
        lineTo(w * 0.2f, h * 0.75f)
        cubicTo(w * 0.2f, h * 0.8f, w * 0.4f, h * 0.82f, w * 0.5f, h * 0.82f)
        lineTo(w * 0.8f, h * 0.82f)
    }
    drawPath(path, color = pcbPaint, style = Stroke(width = 1.5.dp.toPx()))

    // Decorative Screws
    val screwColor = Color(0xFF333342)
    val screwPositions = listOf(
        Offset(w * 0.12f, h * 0.08f),
        Offset(w * 0.88f, h * 0.08f),
        Offset(w * 0.12f, h * 0.92f),
        Offset(w * 0.88f, h * 0.92f),
        Offset(w * 0.22f, h * 0.48f),
        Offset(w * 0.78f, h * 0.48f)
    )
    for (sp in screwPositions) {
        drawCircle(color = screwColor, radius = 2.5.dp.toPx(), center = sp)
        drawLine(
            color = Color(0xFF1E1E26),
            start = Offset(sp.x - 1.5.dp.toPx(), sp.y),
            end = Offset(sp.x + 1.5.dp.toPx(), sp.y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Nothing Dot Matrix Logo watermark (subtle bottom)
    val watermarkColor = Color(0x22FFFFFF)
    drawRoundRect(
        color = watermarkColor,
        topLeft = Offset(w * 0.35f, h * 0.88f),
        size = Size(w * 0.3f, 4.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
}

private fun DrawScope.drawGlyphArcSegment(
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    strokeWidth: Float,
    intensity: Float
) {
    val baseColor = if (intensity > 0.01f) {
        GlyphWhite.copy(alpha = (0.25f + intensity * 0.75f).coerceIn(0f, 1f))
    } else {
        GlyphOff
    }

    // Draw Background Off Track
    drawArc(
        color = GlyphOff,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Draw Active Glowing Segment if illuminated
    if (intensity > 0.01f) {
        // Outer Bloom / Halo
        drawArc(
            color = GlyphGlow.copy(alpha = (intensity * 0.45f).coerceIn(0f, 0.6f)),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth + 6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Core White LED
        drawArc(
            color = baseColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    } else {
        // Subtle outline when off
        drawArc(
            color = GlyphOffBorder,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawGlyphLinearStrip(
    p1: Offset,
    p2: Offset,
    strokeWidth: Float,
    intensity: Float
) {
    // Base off line
    drawLine(
        color = GlyphOff,
        start = p1,
        end = p2,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    if (intensity > 0.01f) {
        // Outer Bloom
        drawLine(
            color = GlyphGlow.copy(alpha = (intensity * 0.45f).coerceIn(0f, 0.6f)),
            start = p1,
            end = p2,
            strokeWidth = strokeWidth + 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Core White LED
        val activeColor = GlyphWhite.copy(alpha = (0.25f + intensity * 0.75f).coerceIn(0f, 1f))
        drawLine(
            color = activeColor,
            start = p1,
            end = p2,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    } else {
        drawLine(
            color = GlyphOffBorder,
            start = p1,
            end = p2,
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
