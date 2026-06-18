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
        if (canvas == null || width <= 1f || height <= 1f) return
        try {
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
            val paint = Paint().apply { isAntiAlias = true }

        // 1. SKY GRADIENT: Deep blue to light horizon blue (Req 1)
        val skyGrad = LinearGradient(
            0f, 0f, 0f, height * 0.75f,
            if (isDark) {
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
        paint.shader = skyGrad
        canvas.drawRect(RectF(0f, 0f, width, height), paint)
        paint.shader = null

        // 2. ROTATING SUN WITH PULSATING CORONA AND LIGHT RAYS (Req 2, 3, 4, 5, 8, 9)
        val sunCenterX = width * 0.84f
        val sunCenterY = height * 0.16f
        
        if (isDark) {
            paint.color = android.graphics.Color.argb(220, 244, 245, 247)
            canvas.drawCircle(sunCenterX, sunCenterY, 50f, paint)
            paint.color = android.graphics.Color.argb(45, 147, 197, 253)
            canvas.drawCircle(sunCenterX, sunCenterY, 70f, paint)
        } else {
            val coronaRadius = 75f + 10f * sin(phase * 3f)
            paint.color = android.graphics.Color.argb(60, 255, 235, 150)
            canvas.drawCircle(sunCenterX, sunCenterY, coronaRadius, paint)

            paint.color = android.graphics.Color.rgb(253, 184, 19)
            canvas.drawCircle(sunCenterX, sunCenterY, 48f, paint)

            paint.color = android.graphics.Color.argb(120, 255, 215, 0)
            paint.strokeWidth = 6f
            val numRays = 12
            val rayOffsetAngle = phase * 0.15f
            for (i in 0 until numRays) {
                val angle = ((2 * PI / numRays) * i + rayOffsetAngle).toFloat()
                val innerR = 56f
                val outerR = 90f + 12f * sin(phase * 4f + i)
                
                val startX = (sunCenterX + innerR * cos(angle)).toFloat()
                val startY = (sunCenterY + innerR * sin(angle)).toFloat()
                val endX = (sunCenterX + outerR * cos(angle)).toFloat()
                val endY = (sunCenterY + outerR * sin(angle)).toFloat()
                canvas.drawLine(startX, startY, endX, endY, paint)
            }
            paint.strokeWidth = 0f
        }

        // 3. THREE PARALLAX DRIFTING CLOUDS (White clouds of the landscape)
        val cloudPaint = Paint().apply {
            isAntiAlias = true
            color = if (isDark) android.graphics.Color.argb(100, 100, 120, 150) else android.graphics.Color.argb(230, 255, 255, 255)
            style = Paint.Style.FILL
        }
        
        val cl1X = (width * 0.15f + drift * 0.8f) % (width + 300f) - 150f
        val cl1Y = height * 0.22f
        drawProceduralCloud(canvas, cl1X, cl1Y, 1.0f, cloudPaint)

        val cl2X = (width * 0.65f + drift * 1.5f) % (width + 300f) - 150f
        val cl2Y = height * 0.32f
        drawProceduralCloud(canvas, cl2X, cl2Y, 0.8f, cloudPaint)

        val cl3X = (width * 0.40f + drift * 2.4f) % (width + 300f) - 150f
        val cl3Y = height * 0.14f
        drawProceduralCloud(canvas, cl3X, cl3Y, 0.6f, cloudPaint)

        // 4. BIRCH TRUNKS ON LATERIAL EDGES (Req 56, 57)
        val birchPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        birchPaint.color = android.graphics.Color.parseColor("#F5F5FA")
        canvas.drawRect(RectF(0f, height * 0.3f, 50f, height), birchPaint)
        
        birchPaint.color = android.graphics.Color.parseColor("#1C1A1A")
        val numStripes = 12
        for (i in 0 until numStripes) {
            val yPos = height * 0.35f + (height * 0.6f / numStripes) * i
            val stripeHeight = 6f + 4f * sin(i * 1.5f).toFloat()
            canvas.drawRect(RectF(0f, yPos, 22f + 10f * sin(i.toDouble()).toFloat(), yPos + stripeHeight), birchPaint)
        }

        birchPaint.color = android.graphics.Color.parseColor("#F5F5FA")
        canvas.drawRect(RectF(width - 50f, height * 0.25f, width, height), birchPaint)
        birchPaint.color = android.graphics.Color.parseColor("#1C1A1A")
        for (i in 0 until numStripes) {
            val yPos = height * 0.3f + (height * 0.65f / numStripes) * i
            val stripeHeight = 6f + 4f * cos(i * 1.5f).toFloat()
            canvas.drawRect(RectF(width - 22f - 10f * cos(i.toDouble()).toFloat(), yPos, width, yPos + stripeHeight), birchPaint)
        }

        val leafColor1 = android.graphics.Color.argb(200, 46, 125, 50)
        val leafColor2 = android.graphics.Color.argb(180, 76, 175, 80)
        birchPaint.color = leafColor1
        
        val fSwayLeft = 12f * sin(phase * 1.5f)
        canvas.drawCircle(30f + fSwayLeft, height * 0.28f, 75f, birchPaint)
        birchPaint.color = leafColor2
        canvas.drawCircle(55f + fSwayLeft, height * 0.35f, 60f, birchPaint)

        birchPaint.color = leafColor1
        val fSwayRight = 10f * cos(phase * 1.3f)
        canvas.drawCircle(width - 30f + fSwayRight, height * 0.22f, 85f, birchPaint)
        birchPaint.color = leafColor2
        canvas.drawCircle(width - 60f + fSwayRight, height * 0.29f, 65f, birchPaint)

        // 5. BIRDS / SEAGULLS FLOCK IN V-FORMATION IN DEEP SKY (Req 36, 37, 38)
        val seagullPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            color = if (isDark) android.graphics.Color.argb(120, 180, 200, 220) else android.graphics.Color.argb(220, 255, 255, 255)
        }
        
        val flockX = (width * 0.1f + drift * 2.2f) % (width + 400f) - 200f
        val flockY = height * 0.3f
        val wingPhase = phase * 4.5f
        
        val offsets = listOf(
            Pair(0f, 0f),
            Pair(-45f, -30f),
            Pair(-45f, 30f),
            Pair(-90f, -60f),
            Pair(-90f, 60f)
        )
        
        offsets.forEachIndexed { i, off ->
            val bx = flockX + off.first
            val by = flockY + off.second + 15f * sin(phase * 0.6f + i)
            
            val flap = sin(wingPhase + i).toFloat() * 14f
            val bPath = Path()
            bPath.moveTo(bx, by)
            bPath.quadTo(bx - 12f, by - 12f + flap, bx - 25f, by - 6f)
            bPath.moveTo(bx, by)
            bPath.quadTo(bx + 12f, by - 12f + flap, bx + 25f, by - 6f)
            canvas.drawPath(bPath, seagullPaint)
        }
        seagullPaint.strokeWidth = 0f

        // 6. BOTTOM GREEN MEADOW LANDSCAPE (Meadow grass floor)
        val landscapePath = Path()
        landscapePath.moveTo(0f, height)
        landscapePath.lineTo(0f, height * 0.77f)
        landscapePath.quadTo(width * 0.33f, height * 0.74f + 16f * sin(phase * 0.4f), width * 0.66f, height * 0.78f)
        landscapePath.quadTo(width * 0.85f, height * 0.79f, width, height * 0.76f)
        landscapePath.lineTo(width, height)
        landscapePath.close()

        val landscapePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = if (isDark) android.graphics.Color.rgb(20, 54, 30) else android.graphics.Color.rgb(56, 142, 60)
        }
        canvas.drawPath(landscapePath, landscapePaint)

        // 7. BOTTOM CORNER WATER POND WITH INTEGRATED SWIMMING FISH AND RIPPLES (Req 46, 47, 48, 49)
        val pondCenterX = width * 0.22f
        val pondCenterY = height * 0.88f
        val pondRadiusX = width * 0.18f
        val pondRadiusY = height * 0.08f
        
        val pondPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = if (isDark) android.graphics.Color.rgb(15, 30, 60) else android.graphics.Color.rgb(79, 195, 247)
        }
        canvas.drawOval(RectF(pondCenterX - pondRadiusX, pondCenterY - pondRadiusY, pondCenterX + pondRadiusX, pondCenterY + pondRadiusY), pondPaint)

        pondPaint.style = Paint.Style.STROKE
        pondPaint.strokeWidth = 2.5f
        pondPaint.color = android.graphics.Color.argb(130, 255, 255, 255)
        for (r in 0..1) {
            val rippleFactor = ((phase * 18f + r * 40f) % 75f) / 75f
            val rx = pondRadiusX * rippleFactor
            val ry = pondRadiusY * rippleFactor
            paint.alpha = ((1f - rippleFactor) * 200).toInt()
            canvas.drawOval(RectF(pondCenterX - rx, pondCenterY - ry, pondCenterX + rx, pondCenterY + ry), pondPaint)
        }
        pondPaint.strokeWidth = 0f
        pondPaint.style = Paint.Style.FILL

        val lilyX = pondCenterX + 30f
        val lilyY = pondCenterY - 10f
        paint.color = android.graphics.Color.rgb(46, 117, 89)
        val lilyPath = Path()
        lilyPath.arcTo(RectF(lilyX - 14f, lilyY - 9f, lilyX + 14f, lilyY + 9f), 35f, 290f, true)
        lilyPath.close()
        canvas.drawPath(lilyPath, paint)

        val fishX = pondCenterX - 35f + 18f * cos(phase * 1.8f)
        val fishY = pondCenterY + 5f + 10f * sin(phase * 1.8f)
        paint.color = android.graphics.Color.rgb(255, 111, 0)
        canvas.drawOval(RectF(fishX - 9f, fishY - 5f, fishX + 9f, fishY + 5f), paint)
        val tailPath = Path().apply {
            moveTo(fishX + 8f, fishY)
            val tailSway = sin(phase * 6f) * 6f
            lineTo(fishX + 16f, fishY - 6f + tailSway)
            lineTo(fishX + 16f, fishY + 6f + tailSway)
            close()
        }
        canvas.drawPath(tailPath, paint)

        // 8. FLOWERS SPECIALLY RED POPPIES AND WHITE CHAMOMILES FORMING NATURAL TRICOLOR (Req 26, 27, 28, 29, 31, 32, 33)
        val numFlowers = 14
        for (f in 0 until numFlowers) {
            val seed = f * 115.11f
            val fx = width * 0.08f + (width * 0.84f / numFlowers) * f
            val curveY = height * 0.77f + 50f
            val slopeY = curveY - height * 0.015f * sin(f * 0.45).toFloat()
            val fy = slopeY + 36f * sin(seed).toFloat()

            paint.color = android.graphics.Color.rgb(76, 175, 80)
            paint.strokeWidth = 2.5f
            val sway = 5f * sin(phase * 0.8f + f).toFloat()
            canvas.drawLine(fx, fy, fx + sway, fy - 22f, paint)
            paint.strokeWidth = 0f

            val flowerCenterX = fx + sway
            val flowerCenterY = fy - 22f

            if (f % 2 == 0) {
                // RED POPPY
                paint.color = android.graphics.Color.rgb(229, 57, 53)
                val petalRadiusX = 9f
                val petalRadiusY = 7f
                canvas.drawOval(RectF(flowerCenterX - petalRadiusX, flowerCenterY - 4f, flowerCenterX, flowerCenterY + 4f), paint)
                canvas.drawOval(RectF(flowerCenterX, flowerCenterY - 4f, flowerCenterX + petalRadiusX, flowerCenterY + 4f), paint)
                canvas.drawOval(RectF(flowerCenterX - 4f, flowerCenterY - petalRadiusY, flowerCenterX + 4f, flowerCenterY), paint)
                canvas.drawOval(RectF(flowerCenterX - 4f, flowerCenterY, flowerCenterX + 4f, flowerCenterY + petalRadiusY), paint)
                
                paint.color = android.graphics.Color.BLACK
                canvas.drawCircle(flowerCenterX, flowerCenterY, 3.5f, paint)
            } else {
                // WHITE CHAMOMILE
                paint.color = android.graphics.Color.WHITE
                val petalR = 6.5f
                for (p in 0 until 8) {
                    val pAngle = ((2 * PI / 8) * p).toFloat()
                    val px = (flowerCenterX + petalR * cos(pAngle)).toFloat()
                    val py = (flowerCenterY + petalR * sin(pAngle)).toFloat()
                    canvas.drawCircle(px, py, 3f, paint)
                }
                paint.color = android.graphics.Color.rgb(255, 235, 59)
                canvas.drawCircle(flowerCenterX, flowerCenterY, 3.5f, paint)
            }
        }

        // 9. FLUTTERING BUTTERFLIES HOVERING OVER POPPIES (Req 39, 40, 41)
        val bfPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val butterPhase = phase * 1.2f
        
        val bf1X = width * 0.44f + 80f * cos(butterPhase * 0.7f).toFloat()
        val bf1Y = height * 0.79f + 40f * sin(butterPhase * 1.1f).toFloat()
        drawFlutteringButterfly(canvas, bf1X, bf1Y, android.graphics.Color.rgb(255, 152, 0), phase, bfPaint)

        val bf2X = width * 0.72f + 70f * sin(butterPhase * 0.9f).toFloat()
        val bf2Y = height * 0.78f + 35f * cos(butterPhase * 1.3f).toFloat()
        drawFlutteringButterfly(canvas, bf2X, bf2Y, android.graphics.Color.rgb(0, 188, 212), phase + 1.2f, bfPaint)

        // 10. GOLDEN POLLEN & SPARKLES SHINING IN SUNBEAMS (Req 66, 67, 69)
        val sparklePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = if (isDark) android.graphics.Color.argb(160, 241, 245, 249) else android.graphics.Color.argb(110, 255, 215, 0)
        }
        for (s in 0..12) {
            val particleSeed = s * 88.35f
            val px = (width * 0.1f + (width * 0.8f) * ((sin(particleSeed).toFloat() + 1f) / 2f))
            val py = (height * 0.35f + (height * 0.55f) * ((1.0f - (phase * 0.4f + s * 0.15f) % 1.0f).toFloat()))
            val sizeP = 2.5f + 1.5f * sin(phase * 3f + s).toFloat()
            canvas.drawCircle(px, py, sizeP, sparklePaint)
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
        canvas.drawOval(RectF(cx - rWidth, cy - rHeight, cx, cy), paint)
        canvas.drawOval(RectF(cx, cy - rHeight, cx + rWidth, cy), paint)
        paint.color = android.graphics.Color.DKGRAY
        canvas.drawRect(RectF(cx - 1.5f, cy - rHeight - 2f, cx + 1.5f, cy + 2f), paint)
    }

    private fun drawProceduralCloud(canvas: Canvas, cx: Float, cy: Float, scale: Float, paint: Paint) {
        val r1 = 30f * scale
        val r2 = 45f * scale
        val r3 = 35f * scale
        canvas.drawCircle(cx, cy, r1, paint)
        canvas.drawCircle(cx + 34f * scale, cy - 8f * scale, r2, paint)
        canvas.drawCircle(cx + 68f * scale, cy, r3, paint)
        canvas.drawRect(RectF(cx, cy - 5f * scale, cx + 68f * scale, cy + r3), paint)
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
            if (canvas == null) return@ComposeCanvas
            val width = size.width
            val height = size.height
            if (width <= 1f || height <= 1f) return@ComposeCanvas
            try {
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
                    else -> { // STANDARD/GUEST MODE: Beautiful fully animated Summer landscape (White/Blue/Red tricolor flowers & sky)
                        flagPainter.drawSummerBackground(canvas, width, height, isDark, phase = phase, drift = drift)
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
