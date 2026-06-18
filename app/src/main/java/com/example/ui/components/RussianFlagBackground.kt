package com.example.ui.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Matrix
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThemeManager
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

class RussianFlagBackground {
    /**
     * Draw the Russian Tricolor programmatically using platform Canvas.
     * Scale style: CENTER_CROP with aspect ratio of 1.5 (3:2) if isWatermark is false.
     * If isWatermark is true: renders centered, 62% of width, with light iridescent holographic overlay.
     * Supports Dark Theme by adjusting saturation and brightness.
     * Phase introduces smooth shifting wave displacement.
     */
    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        isDarkTheme: Boolean,
        isWatermark: Boolean,
        alphaVal: Float = 1.0f,
        phase: Float = 0f
    ) {
        val paint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Apply dark mode color filter if requested
        if (isDarkTheme) {
            val satMatrix = ColorMatrix().apply { setSaturation(0.55f) }
            val brightMatrix = ColorMatrix(floatArrayOf(
                0.80f, 0f, 0f, 0f, 0f,
                0f, 0.80f, 0f, 0f, 0f,
                0f, 0f, 0.80f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            satMatrix.postConcat(brightMatrix)
            paint.colorFilter = ColorMatrixColorFilter(satMatrix)
        }

        if (isWatermark) {
            // Iridescent Foil Watermark (centered, 60% of viewport, with rainbow foil glints)
            paint.alpha = (alphaVal * 255f).toInt().coerceIn(0, 255)
            
            val watermarkWidth = width * 0.62f
            val watermarkHeight = watermarkWidth / 1.5f
            val drawLeft = (width - watermarkWidth) / 2f
            val drawTop = (height - watermarkHeight) / 2f
            val drawRight = drawLeft + watermarkWidth
            val drawBottom = drawTop + watermarkHeight
            
            val stripHeight = watermarkHeight / 3f

            // 1. Draw solid tricolor blocks of watermark
            paint.color = android.graphics.Color.WHITE
            canvas.drawRect(RectF(drawLeft, drawTop, drawRight, drawTop + stripHeight), paint)

            paint.color = android.graphics.Color.rgb(0, 57, 166)
            canvas.drawRect(RectF(drawLeft, drawTop + stripHeight, drawRight, drawTop + stripHeight * 2f), paint)

            paint.color = android.graphics.Color.rgb(213, 43, 30)
            canvas.drawRect(RectF(drawLeft, drawTop + stripHeight * 2f, drawRight, drawBottom), paint)

            // 2. Holographic shiny foil reflection overlay
            val holoPaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
                alpha = 50 // Soft subtle presence
            }
            val matrix = Matrix()
            // Shift the hologram sweep across the watermark area with time
            val shimmerOffset = cos(phase.toDouble()).toFloat() * (watermarkWidth * 0.5f)
            matrix.setTranslate(shimmerOffset, 0f)
            matrix.postRotate(45f, width / 2f, height / 2f)

            // Dynamic holographic rainbow specs
            val holoGradient = LinearGradient(
                drawLeft, drawTop, drawRight, drawBottom,
                intArrayOf(
                    android.graphics.Color.rgb(255, 120, 120),
                    android.graphics.Color.rgb(255, 220, 120),
                    android.graphics.Color.rgb(120, 255, 120),
                    android.graphics.Color.rgb(120, 255, 255),
                    android.graphics.Color.rgb(120, 120, 255),
                    android.graphics.Color.rgb(255, 120, 255)
                ),
                floatArrayOf(0.0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f),
                Shader.TileMode.MIRROR
            )
            holoGradient.setLocalMatrix(matrix)
            holoPaint.shader = holoGradient
            
            // Render the foil reflective glint inside the bookmark
            canvas.drawRect(RectF(drawLeft, drawTop, drawRight, drawBottom), holoPaint)

        } else {
            // Full background waving flag logic
            paint.alpha = (alphaVal * 255f).toInt().coerceIn(0, 255)

            // Center Crop 3:2 aspect ratio layout bounds
            val intrinsicRatio = 1.5f
            val screenRatio = width / height
            
            var drawLeft = 0f
            var drawTop = 0f
            var drawRight = width
            var drawBottom = height

            if (screenRatio > intrinsicRatio) {
                val targetHeight = width / intrinsicRatio
                val offset = (targetHeight - height) / 2f
                drawLeft = 0f
                drawRight = width
                drawTop = -offset
                drawBottom = height + offset
            } else {
                val targetWidth = height * intrinsicRatio
                val offset = (targetWidth - width) / 2f
                drawLeft = -offset
                drawRight = width + offset
                drawTop = 0f
                drawBottom = height
            }

            val stripHeight = (drawBottom - drawTop) / 3f
            
            // Draw smooth sine-wave paths to eliminate blocky lines and gaps
            val points = 48
            val dx = (drawRight - drawLeft) / points
            val amplitude = height * 0.025f

            // Dynamic waving model mimicking progressive, wind-driven fabric behavior
            val getWaveY = { x: Float ->
                val xNormal = ((x - drawLeft) / (drawRight - drawLeft)).coerceIn(0f, 1f)
                // Left is anchored on a pole, flapping intensifies dynamically on the right end
                val envelope = 0.12f + 0.88f * xNormal
                
                // Poly-harmonic waves representing primary gust, secondary wind fluctuation, and micro-flutter
                val primaryWave = sin(xNormal * 3.1f * PI.toFloat() - phase * 1.6f)
                val secondaryWave = sin(xNormal * 6.5f * PI.toFloat() - phase * 3.1f)
                val flutterWave = sin(xNormal * 12.0f * PI.toFloat() - phase * 5.2f)
                
                (primaryWave * 0.72f + secondaryWave * 0.22f + flutterWave * 0.06f) * envelope * amplitude
            }

            // --- Strip 1: White ---
            val whitePath = Path()
            whitePath.moveTo(drawLeft, drawTop - 120f)
            for (i in 0..points) {
                val x = drawLeft + i * dx
                val y = drawTop + stripHeight + getWaveY(x)
                whitePath.lineTo(x, y)
            }
            whitePath.lineTo(drawRight, drawTop - 120f)
            whitePath.close()
            paint.color = android.graphics.Color.WHITE
            canvas.drawPath(whitePath, paint)

            // --- Strip 2: Blue ---
            val bluePath = Path()
            bluePath.moveTo(drawLeft, drawTop + stripHeight + getWaveY(drawLeft))
            for (i in 0..points) {
                val x = drawLeft + i * dx
                val y1 = drawTop + stripHeight + getWaveY(x)
                bluePath.lineTo(x, y1)
            }
            for (i in points downTo 0) {
                val x = drawLeft + i * dx
                val y2 = drawTop + stripHeight * 2f + getWaveY(x)
                bluePath.lineTo(x, y2)
            }
            bluePath.close()
            paint.color = android.graphics.Color.rgb(0, 57, 166)
            canvas.drawPath(bluePath, paint)

            // --- Strip 3: Red ---
            val redPath = Path()
            redPath.moveTo(drawLeft, drawTop + stripHeight * 2f + getWaveY(drawLeft))
            for (i in 0..points) {
                val x = drawLeft + i * dx
                val y2 = drawTop + stripHeight * 2f + getWaveY(x)
                redPath.lineTo(x, y2)
            }
            redPath.lineTo(drawRight, drawBottom + 120f)
            redPath.lineTo(drawLeft, drawBottom + 120f)
            redPath.close()
            paint.color = android.graphics.Color.rgb(213, 43, 30)
            canvas.drawPath(redPath, paint)

            // --- Specular satin highlights & deep creases ---
            val glossPaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            // Layer 1: Global deep creases (primary wind wave folds) skewed diagonally
            val glossMatrix1 = Matrix()
            val specularOffset1 = sin(phase.toDouble()).toFloat() * (width * 0.18f)
            glossMatrix1.setTranslate(specularOffset1, 0f)
            glossMatrix1.postSkew(-0.16f, 0f) // Wind-swept diagonal folds

            val sheenGradient1 = LinearGradient(
                drawLeft, drawTop, drawRight, drawBottom,
                intArrayOf(
                    android.graphics.Color.argb(if (isDarkTheme) 12 else 20, 255, 255, 255), // folds peak reflection
                    android.graphics.Color.argb(if (isDarkTheme) 55 else 36, 0, 0, 0),       // shadow crease
                    android.graphics.Color.argb(if (isDarkTheme) 8 else 12, 255, 255, 255),  // minor peak reflection
                    android.graphics.Color.argb(if (isDarkTheme) 50 else 32, 0, 0, 0),       // deep shadow crease
                    android.graphics.Color.argb(if (isDarkTheme) 12 else 20, 255, 255, 255)
                ),
                floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f),
                Shader.TileMode.MIRROR
            )
            sheenGradient1.setLocalMatrix(glossMatrix1)
            glossPaint.shader = sheenGradient1
            canvas.drawRect(RectF(drawLeft, drawTop - 80f, drawRight, drawBottom + 80f), glossPaint)

            // Layer 2: Secondary micro-crease luxury ripples (fluttering wind micro-vibrations)
            val glossMatrix2 = Matrix()
            val specularOffset2 = cos(phase.toDouble() * 2.4).toFloat() * (width * 0.12f)
            glossMatrix2.setTranslate(specularOffset2, 0f)
            glossMatrix2.postSkew(0.08f, 0f) // Counter wind turbulence vortex shear

            val sheenGradient2 = LinearGradient(
                drawLeft, drawTop, drawRight, drawBottom,
                intArrayOf(
                    android.graphics.Color.argb(if (isDarkTheme) 8 else 12, 255, 255, 255), // gentle soft glint
                    android.graphics.Color.argb(if (isDarkTheme) 35 else 22, 0, 0, 0),      // soft crevice crease
                    android.graphics.Color.argb(if (isDarkTheme) 4 else 6, 255, 255, 255),   // specular micro-glint
                    android.graphics.Color.argb(if (isDarkTheme) 30 else 18, 0, 0, 0),      // soft crevice crease
                    android.graphics.Color.argb(if (isDarkTheme) 8 else 12, 255, 255, 255)
                ),
                floatArrayOf(0.0f, 0.15f, 0.5f, 0.85f, 1.0f),
                Shader.TileMode.MIRROR
            )
            sheenGradient2.setLocalMatrix(glossMatrix2)
            glossPaint.shader = sheenGradient2
            canvas.drawRect(RectF(drawLeft, drawTop - 80f, drawRight, drawBottom + 80f), glossPaint)
        }
    }
}

/**
 * An exceptionally polished backdrop Composable that renders the dynamic backgrounds
 * for all browser modes in hardware-accelerated high efficiency.
 * Automatically configures standard waving Russian flag, private dark vortex, children watercolor blobs,
 * or stealth green-glowing audio protection signals.
 */
@Composable
fun PremiumBackdrop(
    modifier: Modifier = Modifier,
    browserMode: Int = 0,
    isWatermark: Boolean = false,
    alphaVal: Float = 1.0f
) {
    val isDark = ThemeManager.LocalDarkTheme.current
    val flagPainter = remember { RussianFlagBackground() }
    
    // Smooth frame animator loop for fluid waves
    val infiniteTransition = rememberInfiniteTransition(label = "BackdropWaveTransition")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PhaseOscillator"
    )

    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DriftOscillator"
    )

    var animateAlpha by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateAlpha = true
    }
    val startAlpha by animateFloatAsState(
        targetValue = if (animateAlpha) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "StartAlpha"
    )

    val blurRadius = when (browserMode) {
        3 -> 24.dp // Kid mode colorful watercolor blended together
        4 -> 0.dp  // Stealth mode remains sharp digital
        else -> {
            if (isDark) 3.dp else 25.dp // Elegant visible flag waving in the wind in beautiful dark theme!
        }
    }

    val blurModifier = if (!isWatermark && blurRadius > 0.dp && android.os.Build.VERSION.SDK_INT >= 31) {
        modifier.blur(radius = blurRadius)
    } else {
        modifier
    }

    Box(
        modifier = blurModifier
            .graphicsLayer { alpha = alphaVal * startAlpha }
            .fillMaxSize()
    ) {
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            val canvas = drawContext.canvas.nativeCanvas
            val width = size.width
            val height = size.height

            if (isWatermark) {
                // Centered silver holographic signets
                flagPainter.draw(canvas, width, height, isDark, isWatermark = true, alphaVal = alphaVal, phase = phase)
            } else {
                when (browserMode) {
                    1 -> { // INCOGNITO MODE: Cosmic protective dark wind & drifting stars
                        // Draw obsidian sky gradient
                        val spaceGrad = LinearGradient(
                            0f, 0f, 0f, height,
                            intArrayOf(
                                android.graphics.Color.rgb(8, 8, 11),
                                android.graphics.Color.rgb(15, 15, 22),
                                android.graphics.Color.rgb(20, 20, 30)
                            ),
                            null, Shader.TileMode.CLAMP
                        )
                        val spacePaint = Paint().apply { shader = spaceGrad }
                        canvas.drawRect(RectF(0f, 0f, width, height), spacePaint)

                        // Orbiting stelar waves
                        val driftRad = Math.toRadians(drift.toDouble()).toFloat()
                        val windPaint = Paint().apply {
                            isAntiAlias = true
                            style = Paint.Style.STROKE
                            strokeWidth = 2f
                        }

                        for (i in 0..4) {
                            windPaint.color = android.graphics.Color.argb(12 + i * 4, 120, 130, 255)
                            val orbitPath = Path()
                            val cx = width / 2f + 45f * cos(driftRad * 1.4f + i)
                            val cy = height / 2f + 45f * sin(driftRad * 0.9f + i)
                            val rOffset = i * 65f
                            
                            var isFirst = true
                            for (a in 0..360 step 6) {
                                val deg = Math.toRadians(a.toDouble()).toFloat()
                                val r = (140f + rOffset + 16f * sin(deg * 3f + phase * 2f)).toFloat()
                                val x = cx + r * cos(deg)
                                val y = cy + r * sin(deg)
                                if (isFirst) {
                                    orbitPath.moveTo(x, y)
                                    isFirst = false
                                } else {
                                    orbitPath.lineTo(x, y)
                                }
                            }
                            orbitPath.close()
                            canvas.drawPath(orbitPath, windPaint)
                        }

                        // Floating privacy particulate elements
                        val starPaint = Paint().apply {
                            isAntiAlias = true
                            style = Paint.Style.FILL
                            color = android.graphics.Color.argb(90, 148, 163, 184)
                        }
                        for (s in 0..15) {
                            val seed = s * 77.21f
                            val sX = (width * 0.5f + (width * 0.44f) * cos(driftRad + seed)).toFloat()
                            val sY = (height * 0.5f + (height * 0.44f) * sin(driftRad * 0.75f + seed)).toFloat()
                            // Twinkling animation via wave harmonics
                            val radius = (3.5f + 2.5f * sin(phase * 1.3f + s)).toFloat()
                            canvas.drawCircle(sX, sY, radius, starPaint)
                        }
                    }
                    3 -> { // CHILD MODE: Vivid warm pastel watercolors liquid morphing background
                        // Background base
                        canvas.drawColor(android.graphics.Color.rgb(253, 248, 255))

                        val brushPaint = Paint().apply {
                            isAntiAlias = true
                            style = Paint.Style.FILL
                        }

                        val watercolorBlobs = listOf(
                            android.graphics.Color.argb(55, 144, 202, 249), // soft baby blue
                            android.graphics.Color.argb(55, 244, 143, 177), // soft rose pink
                            android.graphics.Color.argb(55, 206, 147, 216), // soft lavender
                            android.graphics.Color.argb(45, 255, 224, 130)  // soft amber yellow
                        )

                        val driftRad = Math.toRadians(drift.toDouble()).toFloat()
                        watercolorBlobs.forEachIndexed { idx, col ->
                            brushPaint.color = col
                            val angle = driftRad * 0.7f + idx * (PI / 2f).toFloat()
                            val cx = width * 0.5f + (width * 0.36f) * cos(angle)
                            val cy = height * 0.5f + (height * 0.36f) * sin(angle * 1.3f)
                            
                            val blobSize = (width * 0.34f + (width * 0.04f) * sin(phase + idx)).toFloat()
                            canvas.drawCircle(cx, cy, blobSize, brushPaint)
                        }
                    }
                    4 -> { // STEALTH MODE: pure black, neon grids, active radar-like sound waves matching "зеленая аудиоволна"
                        // Black slate canvas
                        canvas.drawColor(android.graphics.Color.BLACK)

                        // Futuristic secure grids
                        val gridMaterial = Paint().apply {
                            color = android.graphics.Color.argb(12, 0, 255, 102)
                            strokeWidth = 1f
                            style = Paint.Style.STROKE
                        }
                        val step = 64f
                        var crX = 0f
                        while (crX < width) {
                            canvas.drawLine(crX, 0f, crX, height, gridMaterial)
                            crX += step
                        }
                        var crY = 0f
                        while (crY < height) {
                            canvas.drawLine(0f, crY, width, crY, gridMaterial)
                            crY += step
                        }

                        // Wavy secure radar audio frequencies
                        val sonarRadar = Paint().apply {
                            isAntiAlias = true
                            style = Paint.Style.STROKE
                        }

                        val waveBaseY = height - 140f
                        for (w in 0..2) {
                            val amplitudeWave = 15f + w * 20f
                            sonarRadar.color = android.graphics.Color.argb(35 + w * 45, 0, 255, 102)
                            sonarRadar.strokeWidth = 1.5f + w * 1.2f

                            val p = Path()
                            var initialized = false
                            for (pos in 0..width.toInt() step 6) {
                                val x = pos.toFloat()
                                val scaleHarmonic = sin(x * 0.007f + phase * 2.5f + w)
                                val secondaryHarmonic = cos(x * 0.018f - phase * 1.4f)
                                
                                // Anchor the ends of audio wave using bell curve hump
                                val shapeHump = cos((x - width / 2f) / (width / 2f) * (PI / 2f).toFloat())
                                val y = waveBaseY + amplitudeWave * scaleHarmonic * secondaryHarmonic * shapeHump

                                if (!initialized) {
                                    p.moveTo(x, y)
                                    initialized = true
                                } else {
                                    p.lineTo(x, y)
                                }
                            }
                            canvas.drawPath(p, sonarRadar)
                        }
                    }
                    else -> { // STANDARD/GUEST MODE: Premium waving tricolor backdrops
                        flagPainter.draw(canvas, width, height, isDark, isWatermark = false, alphaVal = 1.0f, phase = phase)
                    }
                }
            }
        }

        // Transparency filters and gloss cover
        if (!isWatermark) {
            if (browserMode != 1 && browserMode != 4 && browserMode != 3) {
                val overlayColor = if (isDark) {
                    Color(0x99000000) // Requirement #2 calls for #99000000 (тёмная)
                } else {
                    Color(0xBFFFFFFF) // Requirement #2 calls for #BFFFFFFF (светлая)
                }
                ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = overlayColor)
                }
            } else if (browserMode == 3) {
                // Dim down kids watercolors for card visual contrast
                ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = Color.White.copy(alpha = 0.65f))
                }
            }
        }
    }
}
