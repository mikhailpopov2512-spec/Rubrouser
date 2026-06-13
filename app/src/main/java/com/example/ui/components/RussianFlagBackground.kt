package com.example.ui.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas

class RussianFlagBackground {
    /**
     * Draw the Russian Tricolor programmatically using platform Canvas.
     * Scale style: CENTER_CROP with aspect ratio of 1.5 (3:2) if isWatermark is false.
     * If isWatermark is true: renders centered, 60% of width, with low opacity.
     * Supports Dark Theme by applying saturation (0.6) and brightness (0.8) color matrix on the Paint.
     */
    fun draw(canvas: Canvas, width: Float, height: Float, isDarkTheme: Boolean, isWatermark: Boolean, alphaVal: Float = 1.0f) {
        val paint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Apply dark mode color modification if requested
        if (isDarkTheme) {
            // Saturation 0.6 and Brightness 0.8 color filters
            val satMatrix = ColorMatrix().apply { setSaturation(0.6f) }
            val brightMatrix = ColorMatrix(floatArrayOf(
                0.8f, 0f, 0f, 0f, 0f,
                0f, 0.8f, 0f, 0f, 0f,
                0f, 0f, 0.8f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            satMatrix.postConcat(brightMatrix)
            paint.colorFilter = ColorMatrixColorFilter(satMatrix)
        }

        if (isWatermark) {
            // Set Alpha to requested value (e.g. 10%)
            paint.alpha = (alphaVal * 255f).toInt().coerceIn(0, 255)
            
            val watermarkWidth = width * 0.6f
            val watermarkHeight = watermarkWidth / 1.5f
            val drawLeft = (width - watermarkWidth) / 2f
            val drawTop = (height - watermarkHeight) / 2f
            val drawRight = drawLeft + watermarkWidth
            val drawBottom = drawTop + watermarkHeight
            
            val stripHeight = watermarkHeight / 3f

            // Top (White)
            paint.color = android.graphics.Color.WHITE
            canvas.drawRect(RectF(drawLeft, drawTop, drawRight, drawTop + stripHeight), paint)

            // Middle (Blue)
            paint.color = android.graphics.Color.rgb(0, 57, 166)
            canvas.drawRect(RectF(drawLeft, drawTop + stripHeight, drawRight, drawTop + stripHeight * 2f), paint)

            // Bottom (Red)
            paint.color = android.graphics.Color.rgb(213, 43, 30)
            canvas.drawRect(RectF(drawLeft, drawTop + stripHeight * 2f, drawRight, drawBottom), paint)
        } else {
            // Set full opacity or animated transition opacity
            paint.alpha = (alphaVal * 255f).toInt().coerceIn(0, 255)

            // Custom Center Crop Math for a 3:2 aspect ratio (1.5 width/height)
            val intrinsicRatio = 1.5f
            val screenRatio = width / height
            
            var drawLeft = 0f
            var drawTop = 0f
            var drawRight = width
            var drawBottom = height

            if (screenRatio > intrinsicRatio) {
                // Screen is wider than 3:2. Map to screen width & center vertical crop
                val targetHeight = width / intrinsicRatio
                val offset = (targetHeight - height) / 2f
                drawLeft = 0f
                drawRight = width
                drawTop = -offset
                drawBottom = height + offset
            } else {
                // Screen is taller than 3:2. Map to screen height & center horizontal crop
                val targetWidth = height * intrinsicRatio
                val offset = (targetWidth - width) / 2f
                drawLeft = -offset
                drawRight = width + offset
                drawTop = 0f
                drawBottom = height
            }

            val stripHeight = (drawBottom - drawTop) / 3f

            // Strip 1: White
            paint.color = android.graphics.Color.WHITE
            canvas.drawRect(RectF(drawLeft, drawTop, drawRight, drawTop + stripHeight), paint)

            // Strip 2: Blue (#0039A6)
            paint.color = android.graphics.Color.rgb(0, 57, 166)
            canvas.drawRect(RectF(drawLeft, drawTop + stripHeight, drawRight, drawTop + stripHeight * 2f), paint)

            // Strip 3: Red (#D52B1E)
            paint.color = android.graphics.Color.rgb(213, 43, 30)
            canvas.drawRect(RectF(drawLeft, drawTop + stripHeight * 2f, drawRight, drawBottom), paint)
        }
    }
}

/**
 * A beautiful background Composable that renders the static/custom scale flag and places
 * a semi-transparent overlay on top of it to preserve text/icon legibility.
 * Supports watermarks (with opacity 10%) or full backdrop options with 300ms fade-in animated entry.
 */
@Composable
fun RussianFlagBackdrop(
    modifier: Modifier = Modifier,
    isWatermark: Boolean = false,
    alphaVal: Float = if (isWatermark) 0.10f else 1.0f
) {
    val isDark = isSystemInDarkTheme()
    val flagPainter = remember { RussianFlagBackground() }
    
    // Animate alpha for cold-start fade-in transition (300ms) unless it's a static watermark
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(isWatermark) {
        if (!isWatermark) {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = EaseOut)
            )
        } else {
            alphaAnim.snapTo(1f) // watermark has static presentation
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Hardware accelerated fade-in
                alpha = alphaAnim.value
            }
            .drawBehind {
                val canvas = drawContext.canvas.nativeCanvas
                if (isWatermark) {
                    flagPainter.draw(canvas, size.width, size.height, isDark, isWatermark = true, alphaVal = alphaVal)
                } else {
                    flagPainter.draw(canvas, size.width, size.height, isDark, isWatermark = false, alphaVal = 1.0f)
                }
            }
    ) {
        if (!isWatermark) {
            // Apply overlay
            val overlayColor = if (isDark) {
                Color(0x99000000) // #99000000 dark with ~60% transparency
            } else {
                Color(0xBFFFFFFF) // #BFFFFFFF white with ~75% transparency
            }
            ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = overlayColor)
            }
        }
    }
}
