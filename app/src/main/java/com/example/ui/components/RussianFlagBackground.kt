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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThemeManager
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

class RussianFlagBackground {

    // Pre-allocated Paint instances
    private val paint = Paint().apply { isAntiAlias = true }
    private val holoPaint = Paint().apply { isAntiAlias = true; alpha = 50 }
    private val glossPaint1 = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val glossPaint2 = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val skyPaint = Paint().apply { isAntiAlias = true }
    private val cloudPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val birchPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val seagullPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 3.5f }
    private val landscapePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val pondPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val bfPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val sparklePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    internal val brushPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    internal val spacePaint = Paint().apply { isAntiAlias = true }
    internal val windPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2f }
    internal val starPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    internal val sonarRadar = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }

    // Pre-allocated Path instances to avoid heavy onDraw allocations
    private val whitePath = Path()
    private val bluePath = Path()
    private val redPath = Path()
    internal val orbitPath = Path()
    private val bPath = Path()
    private val landscapePath = Path()
    private val lilyPath = Path()
    private val tailPath = Path()
    internal val audioPath = Path()

    // Pre-allocated Bounds
    internal val rectF = RectF()

    // Matrix caches
    private val matrix = Matrix()
    private val glossMatrix1 = Matrix()
    private val glossMatrix2 = Matrix()

    // Cache parameters to track layout size and theme changes
    private var lastWidth = -1f
    private var lastHeight = -1f
    private var lastIsDark = false

    // Gradient / Shader caches
    private var skyGradient: LinearGradient? = null
    private var sheenGradient1: LinearGradient? = null
    private var sheenGradient2: LinearGradient? = null
    private var holoGradient: LinearGradient? = null
    internal var spaceGrad: LinearGradient? = null
    internal var gridMaterialPaint: Paint? = null

    private fun checkAndRecreateCache(width: Float, height: Float, isDarkTheme: Boolean) {
        if (width == lastWidth && height == lastHeight && isDarkTheme == lastIsDark) {
            return
        }
        lastWidth = width
        lastHeight = height
        lastIsDark = isDarkTheme

        // 1. Center Crop 3:2 aspect ratio layout bounds
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

        // Sheen Gradient 1
        sheenGradient1 = LinearGradient(
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

        // Sheen Gradient 2
        sheenGradient2 = LinearGradient(
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

        // Holo Watermark Gradient
        val watermarkWidth = width * 0.62f
        val watermarkHeight = watermarkWidth / 1.5f
        val wl = (width - watermarkWidth) / 2f
        val wt = (height - watermarkHeight) / 2f
        val wr = wl + watermarkWidth
        val wb = wt + watermarkHeight

        holoGradient = LinearGradient(
            wl, wt, wr, wb,
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

        // Sky Gradient
        skyGradient = LinearGradient(
            0f, 0f, 0f, height * 0.75f,
            if (isDarkTheme) {
                intArrayOf(
                    android.graphics.Color.rgb(10, 24, 47),   // Twilight deep navy
                    android.graphics.Color.rgb(20, 40, 80),   // Middle dark indigo
                    android.graphics.Color.rgb(35, 65, 120)   // Lower warm slate
                )
            } else {
                intArrayOf(
                    android.graphics.Color.rgb(46, 123, 214),  // Saturated summer sky blue
                    android.graphics.Color.rgb(135, 206, 235), // Soft sky-blue middle
                    android.graphics.Color.rgb(224, 242, 254)  // Golden warm horizon light (Requirement 1)
                )
            },
            null, Shader.TileMode.CLAMP
        )

        // Incognito sky gradient
        spaceGrad = LinearGradient(
            0f, 0f, 0f, height,
            intArrayOf(
                android.graphics.Color.rgb(8, 8, 11),
                android.graphics.Color.rgb(15, 15, 22),
                android.graphics.Color.rgb(20, 20, 30)
            ),
            null, Shader.TileMode.CLAMP
        )

        // Grid lines (pre-configured)
        gridMaterialPaint = Paint().apply {
            color = android.graphics.Color.argb(12, 0, 255, 102)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
    }

    /**
     * Draw the Russian Tricolor programmatically using platform Canvas.
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
        if (canvas == null || width <= 1f || height <= 1f) return
        try {
            checkAndRecreateCache(width, height, isDarkTheme)

            // Dynamic color filtering configuration to avoid allocations
            paint.style = Paint.Style.FILL
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
            } else {
                paint.colorFilter = null
            }

            if (isWatermark) {
                paint.alpha = (alphaVal * 255f).toInt().coerceIn(0, 255)
                
                val watermarkWidth = width * 0.62f
                val watermarkHeight = watermarkWidth / 1.5f
                val drawLeft = (width - watermarkWidth) / 2f
                val drawTop = (height - watermarkHeight) / 2f
                val drawRight = drawLeft + watermarkWidth
                val drawBottom = drawTop + watermarkHeight
                
                val stripHeight = watermarkHeight / 3f

                // 1. Draw solid tricolor blocks of watermark using cached bounds
                paint.color = android.graphics.Color.WHITE
                rectF.set(drawLeft, drawTop, drawRight, drawTop + stripHeight)
                canvas.drawRect(rectF, paint)

                paint.color = android.graphics.Color.rgb(0, 57, 166)
                rectF.set(drawLeft, drawTop + stripHeight, drawRight, drawTop + stripHeight * 2f)
                canvas.drawRect(rectF, paint)

                paint.color = android.graphics.Color.rgb(213, 43, 30)
                rectF.set(drawLeft, drawTop + stripHeight * 2f, drawRight, drawBottom)
                canvas.drawRect(rectF, paint)

                // 2. Holographic shiny foil reflection overlay
                matrix.reset()
                val shimmerOffset = cos(phase.toDouble()).toFloat() * (watermarkWidth * 0.5f)
                matrix.setTranslate(shimmerOffset, 0f)
                matrix.postRotate(45f, width / 2f, height / 2f)

                holoGradient?.setLocalMatrix(matrix)
                holoPaint.shader = holoGradient
                
                rectF.set(drawLeft, drawTop, drawRight, drawBottom)
                canvas.drawRect(rectF, holoPaint)

            } else {
                // Full background waving flag logic
                paint.alpha = (alphaVal * 255f).toInt().coerceIn(0, 255)

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
                val points = 32 // Reduced from 48 for micro-optimization of CPU performance
                val dx = (drawRight - drawLeft) / points
                val amplitude = height * 0.025f

                // Dynamic waving model mimicking progressive, wind-driven fabric behavior
                val getWaveY = { x: Float ->
                    val xNormal = ((x - drawLeft) / (drawRight - drawLeft)).coerceIn(0f, 1f)
                    val envelope = 0.12f + 0.88f * xNormal
                    val primaryWave = sin(xNormal * 3.1f * PI.toFloat() - phase * 1.6f)
                    val secondaryWave = sin(xNormal * 6.5f * PI.toFloat() - phase * 3.1f)
                    (primaryWave * 0.8f + secondaryWave * 0.2f) * envelope * amplitude
                }

                // --- Strip 1: White ---
                whitePath.reset()
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
                bluePath.reset()
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
                redPath.reset()
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
                // Layer 1: Global deep creases (primary wind wave folds) skewed diagonally
                glossMatrix1.reset()
                val specularOffset1 = sin(phase.toDouble()).toFloat() * (width * 0.18f)
                glossMatrix1.setTranslate(specularOffset1, 0f)
                glossMatrix1.postSkew(-0.16f, 0f)

                sheenGradient1?.setLocalMatrix(glossMatrix1)
                glossPaint1.shader = sheenGradient1
                rectF.set(drawLeft, drawTop - 80f, drawRight, drawBottom + 80f)
                canvas.drawRect(rectF, glossPaint1)

                // Layer 2: Secondary micro-crease luxury ripples (fluttering wind micro-vibrations)
                glossMatrix2.reset()
                val specularOffset2 = cos(phase.toDouble() * 2.4).toFloat() * (width * 0.12f)
                glossMatrix2.setTranslate(specularOffset2, 0f)
                glossMatrix2.postSkew(0.08f, 0f)

                sheenGradient2?.setLocalMatrix(glossMatrix2)
                glossPaint2.shader = sheenGradient2
                canvas.drawRect(rectF, glossPaint2)
            }
        } catch (e: Throwable) {
            android.util.Log.e("RussianFlag", "Error in draw", e)
        }
    }

    /**
     * Draw the magnificent fully animated Summer Landscape representing White (clouds, chamomile),
     * Blue (sky, pond), and Red (poppy flowers) tricolor colors (Req 1-57).
     */
    fun drawSummerBackground(canvas: Canvas, width: Float, height: Float, isDark: Boolean, phase: Float, drift: Float) {
        if (canvas == null || width <= 1f || height <= 1f) return
        try {
            checkAndRecreateCache(width, height, isDark)

            // 1. SKY GRADIENT: Use cached skyGradient
            skyPaint.shader = skyGradient
            rectF.set(0f, 0f, width, height)
            canvas.drawRect(rectF, skyPaint)

            // 2. SUN WITH PULSATING GLOW (Highly Optimized)
            val sunCenterX = width * 0.84f
            val sunCenterY = height * 0.16f
            
            if (isDark) {
                skyPaint.shader = null
                skyPaint.color = android.graphics.Color.argb(220, 244, 245, 247)
                canvas.drawCircle(sunCenterX, sunCenterY, 35f, skyPaint)
                skyPaint.color = android.graphics.Color.argb(45, 147, 197, 253)
                canvas.drawCircle(sunCenterX, sunCenterY, 50f, skyPaint)
            } else {
                skyPaint.shader = null
                // Optimized Sun without math-heavy loops
                skyPaint.color = android.graphics.Color.argb(40, 255, 235, 150)
                canvas.drawCircle(sunCenterX, sunCenterY, 65f, skyPaint)

                skyPaint.color = android.graphics.Color.rgb(253, 184, 19)
                canvas.drawCircle(sunCenterX, sunCenterY, 40f, skyPaint)
            }

            // 3. TWO PARALLAX DRIFTING CLOUDS
            cloudPaint.color = if (isDark) android.graphics.Color.argb(100, 100, 120, 150) else android.graphics.Color.argb(210, 255, 255, 255)
            
            val cl1X = (width * 0.2f + drift * 0.5f) % (width + 250f) - 120f
            val cl1Y = height * 0.2f
            rectF.set(cl1X - 60f, cl1Y - 20f, cl1X + 60f, cl1Y + 20f)
            canvas.drawOval(rectF, cloudPaint)

            val cl2X = (width * 0.7f + drift * 0.9f) % (width + 250f) - 120f
            val cl2Y = height * 0.3f
            rectF.set(cl2X - 50f, cl2Y - 18f, cl2X + 50f, cl2Y + 18f)
            canvas.drawOval(rectF, cloudPaint)

            // 4. BIRCH TRUNKS (Simplified, static, extremely fast)
            birchPaint.color = android.graphics.Color.parseColor("#F5F5FA")
            rectF.set(0f, height * 0.3f, 30f, height)
            canvas.drawRect(rectF, birchPaint)
            
            birchPaint.color = android.graphics.Color.parseColor("#1C1A1A")
            // A few static stripes instead of math-heavy calculations
            for (i in 0..4) {
                val yPos = height * 0.4f + (height * 0.1f * i)
                rectF.set(0f, yPos, 15f, yPos + 6f)
                canvas.drawRect(rectF, birchPaint)
            }

            // Green canopy
            birchPaint.color = android.graphics.Color.argb(180, 46, 125, 50)
            canvas.drawCircle(20f, height * 0.28f, 60f, birchPaint)
            birchPaint.color = android.graphics.Color.argb(150, 76, 175, 80)
            canvas.drawCircle(40f, height * 0.33f, 45f, birchPaint)

            // 5. BOTTOM GREEN MEADOW LANDSCAPE (Meadow grass floor)
            landscapePath.reset()
            landscapePath.moveTo(0f, height)
            landscapePath.lineTo(0f, height * 0.78f)
            // Beautiful simple bezier hill
            landscapePath.quadTo(width * 0.5f, height * 0.74f, width, height * 0.77f)
            landscapePath.lineTo(width, height)
            landscapePath.close()

            landscapePaint.color = if (isDark) android.graphics.Color.rgb(20, 54, 30) else android.graphics.Color.rgb(56, 142, 60)
            canvas.drawPath(landscapePath, landscapePaint)

            // 6. BOTTOM CORNER WATER POND
            val pondCenterX = width * 0.22f
            val pondCenterY = height * 0.88f
            val pondRadiusX = width * 0.15f
            val pondRadiusY = height * 0.06f
            
            pondPaint.color = if (isDark) android.graphics.Color.rgb(15, 30, 60) else android.graphics.Color.rgb(79, 195, 247)
            rectF.set(pondCenterX - pondRadiusX, pondCenterY - pondRadiusY, pondCenterX + pondRadiusX, pondCenterY + pondRadiusY)
            canvas.drawOval(rectF, pondPaint)

            // 7. SIMPLE FLOWERS (Stems and simple single colored circles - Poppy representing Red/Chamomile representing White)
            // Draw 4 beautiful, clean minimalist flowers
            val baseFlowerColors = intArrayOf(
                android.graphics.Color.rgb(229, 57, 53), // Red poppy
                android.graphics.Color.WHITE,             // White chamomile
                android.graphics.Color.rgb(229, 57, 53), // Red poppy
                android.graphics.Color.WHITE              // White chamomile
            )
            for (f in 0..3) {
                val fx = width * 0.15f + (width * 0.7f / 3f) * f
                val fy = height * 0.8f + (15f * kotlin.math.sin(f.toDouble())).toFloat()
                
                // Stem
                sparklePaint.color = android.graphics.Color.rgb(76, 175, 80)
                sparklePaint.strokeWidth = 2f
                canvas.drawLine(fx, fy, fx, fy - 18f, sparklePaint)
                
                // Flower head
                sparklePaint.color = baseFlowerColors[f]
                canvas.drawCircle(fx, fy - 18f, 6f, sparklePaint)
                
                if (baseFlowerColors[f] == android.graphics.Color.WHITE) {
                    // Chamomile center
                    sparklePaint.color = android.graphics.Color.rgb(255, 235, 59)
                    canvas.drawCircle(fx, fy - 18f, 2.5f, sparklePaint)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("RussianFlag", "Error in drawSummerBackground", e)
        }
    }

    private fun drawFlutteringButterfly(canvas: Canvas, cx: Float, cy: Float, wingColor: Int, phase: Float, paint: Paint) {
        paint.color = wingColor
        val flapScale = cos(phase * 12f)
        val rWidth = 12f * flapScale
        val rHeight = 8f
        
        rectF.set(cx - rWidth, cy - rHeight, cx, cy)
        canvas.drawOval(rectF, paint)
        rectF.set(cx, cy - rHeight, cx + rWidth, cy)
        canvas.drawOval(rectF, paint)
        
        paint.color = android.graphics.Color.DKGRAY
        rectF.set(cx - 1.5f, cy - rHeight - 2f, cx + 1.5f, cy + 2f)
        canvas.drawRect(rectF, paint)
    }

    private fun drawProceduralCloud(canvas: Canvas, cx: Float, cy: Float, scale: Float, paint: Paint) {
        val r1 = 30f * scale
        val r2 = 45f * scale
        val r3 = 35f * scale
        canvas.drawCircle(cx, cy, r1, paint)
        canvas.drawCircle(cx + 34f * scale, cy - 8f * scale, r2, paint)
        canvas.drawCircle(cx + 68f * scale, cy, r3, paint)
        rectF.set(cx, cy - 5f * scale, cx + 68f * scale, cy + r3)
        canvas.drawRect(rectF, paint)
    }
}

/**
 * An exceptionally polished backdrop Composable that renders the dynamic backgrounds
 * for all browser modes in hardware-accelerated high efficiency.
 */
@Composable
fun PremiumBackdrop(
    modifier: Modifier = Modifier,
    browserMode: Int = 0,
    isWatermark: Boolean = false,
    alphaVal: Float = 1.0f,
    selectedBgTheme: Int = 0
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
            if (canvas == null) return@ComposeCanvas
            val width = size.width
            val height = size.height
            if (width <= 1f || height <= 1f) return@ComposeCanvas
            try {
                if (isWatermark) {
                    flagPainter.draw(canvas, width, height, isDark, isWatermark = true, alphaVal = alphaVal, phase = phase)
                } else {
                    when (browserMode) {
                        1 -> { // INCOGNITO MODE: Cosmic protective dark wind & drifting stars
                            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                            if (flagPainter.spaceGrad != null) {
                                flagPainter.spacePaint.shader = flagPainter.spaceGrad
                                flagPainter.rectF.set(0f, 0f, width, height)
                                canvas.drawRect(flagPainter.rectF, flagPainter.spacePaint)
                            }

                            // Orbiting stellar waves
                            val driftRad = Math.toRadians(drift.toDouble()).toFloat()
                            for (i in 0..2) { // Optimized from 5
                                flagPainter.windPaint.color = android.graphics.Color.argb(12 + i * 4, 120, 130, 255)
                                flagPainter.orbitPath.reset()
                                val cx = width / 2f + 45f * cos(driftRad * 1.4f + i)
                                val cy = height / 2f + 45f * sin(driftRad * 0.9f + i)
                                val rOffset = i * 65f
                                
                                var isFirst = true
                                for (a in 0..360 step 12) { // Optimized from step 6
                                    val deg = Math.toRadians(a.toDouble()).toFloat()
                                    val r = (140f + rOffset + 16f * sin(deg * 3f + phase * 2f)).toFloat()
                                    val x = cx + r * cos(deg)
                                    val y = cy + r * sin(deg)
                                    if (isFirst) {
                                        flagPainter.orbitPath.moveTo(x, y)
                                        isFirst = false
                                    } else {
                                        flagPainter.orbitPath.lineTo(x, y)
                                    }
                                }
                                flagPainter.orbitPath.close()
                                canvas.drawPath(flagPainter.orbitPath, flagPainter.windPaint)
                            }

                            // Floating privacy particulate elements
                            flagPainter.starPaint.color = android.graphics.Color.argb(90, 148, 163, 184)
                            for (s in 0..8) { // Optimized from 16
                                val seed = s * 77.21f
                                val sX = (width * 0.5f + (width * 0.44f) * cos(driftRad + seed)).toFloat()
                                val sY = (height * 0.5f + (height * 0.44f) * sin(driftRad * 0.75f + seed)).toFloat()
                                val radius = (3.5f + 2.5f * sin(phase * 1.3f + s)).toFloat()
                                canvas.drawCircle(sX, sY, radius, flagPainter.starPaint)
                            }
                        }
                        3 -> { // CHILD MODE: Vivid warm pastel watercolors liquid morphing background
                            canvas.drawColor(android.graphics.Color.rgb(253, 248, 255))

                            val watercolorBlobs = listOf(
                                android.graphics.Color.argb(55, 144, 202, 249), // soft baby blue
                                android.graphics.Color.argb(55, 244, 143, 177), // soft rose pink
                                android.graphics.Color.argb(55, 206, 147, 216), // soft lavender
                                android.graphics.Color.argb(45, 255, 224, 130)  // soft amber yellow
                            )

                            val driftRad = Math.toRadians(drift.toDouble()).toFloat()
                            watercolorBlobs.forEachIndexed { idx, col ->
                                flagPainter.brushPaint.color = col
                                val angle = driftRad * 0.7f + idx * (PI / 2f).toFloat()
                                val cx = width * 0.5f + (width * 0.36f) * cos(angle)
                                val cy = height * 0.5f + (height * 0.36f) * sin(angle * 1.3f)
                                
                                val blobSize = (width * 0.34f + (width * 0.04f) * sin(phase + idx)).toFloat()
                                canvas.drawCircle(cx, cy, blobSize, flagPainter.brushPaint)
                            }
                        }
                        4 -> { // STEALTH MODE: pure black, neon grids, active radar-like sound waves matching "зеленая аудиоволна"
                            canvas.drawColor(android.graphics.Color.BLACK)

                            // Futuristic secure grids
                            if (flagPainter.gridMaterialPaint != null) {
                                val step = 64f
                                var crX = 0f
                                while (crX < width) {
                                    canvas.drawLine(crX, 0f, crX, height, flagPainter.gridMaterialPaint!!)
                                    crX += step
                                }
                                var crY = 0f
                                while (crY < height) {
                                    canvas.drawLine(0f, crY, width, crY, flagPainter.gridMaterialPaint!!)
                                    crY += step
                                }
                            }

                            // Wavy secure radar audio frequencies
                            val waveBaseY = height - 140f
                            for (w in 0..1) { // Optimized from 3
                                val amplitudeWave = 15f + w * 20f
                                flagPainter.sonarRadar.color = android.graphics.Color.argb(35 + w * 45, 0, 255, 102)
                                flagPainter.sonarRadar.strokeWidth = 1.5f + w * 1.2f

                                flagPainter.audioPath.reset()
                                var initialized = false
                                for (pos in 0..width.toInt() step 12) { // Optimized from step 6
                                    val x = pos.toFloat()
                                    val scaleHarmonic = sin(x * 0.007f + phase * 2.5f + w)
                                    val secondaryHarmonic = cos(x * 0.018f - phase * 1.4f)
                                    
                                    val shapeHump = cos((x - width / 2f) / (width / 2f) * (PI / 2f).toFloat())
                                    val y = waveBaseY + amplitudeWave * scaleHarmonic * secondaryHarmonic * shapeHump

                                    if (!initialized) {
                                        flagPainter.audioPath.moveTo(x, y)
                                        initialized = true
                                    } else {
                                        flagPainter.audioPath.lineTo(x, y)
                                    }
                                }
                                canvas.drawPath(flagPainter.audioPath, flagPainter.sonarRadar)
                            }
                        }
                        else -> { // STANDARD/GUEST MODE: Summer landscape, Russian flag or minimal
                            when (selectedBgTheme) {
                                1 -> { // Russian flag
                                    flagPainter.draw(canvas, width, height, isDark, isWatermark = false, alphaVal = alphaVal, phase = phase)
                                }
                                2 -> { // Minimalist solid
                                    canvas.drawColor(if (isDark) android.graphics.Color.rgb(18, 18, 24) else android.graphics.Color.rgb(245, 246, 249))
                                }
                                else -> { // Summer landscape
                                    flagPainter.drawSummerBackground(canvas, width, height, isDark, phase = phase, drift = drift)
                                }
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.e("PremiumBackdrop", "Draw error inside ComposeCanvas", t)
            }
        }

        // Transparency filters and gloss cover
        if (!isWatermark) {
            if (browserMode != 1 && browserMode != 4 && browserMode != 3) {
                val overlayColor = if (isDark) {
                    Color(0x99000000)
                } else {
                    Color(0xBFFFFFFF)
                }
                ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = overlayColor)
                }
            } else if (browserMode == 3) {
                ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = Color.White.copy(alpha = 0.65f))
                }
            }
        }
    }
}
