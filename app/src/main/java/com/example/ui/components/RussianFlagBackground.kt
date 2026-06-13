package com.example.ui.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas

class RussianFlagBackground {
    /**
     * Draw the Russian Tricolor programmatically using platform Canvas.
     * Dimensions are supplied as width and height.
     * Supports Dark Theme by dimming saturation/brightness.
     */
    fun draw(canvas: Canvas, width: Float, height: Float, isDarkTheme: Boolean) {
        val paint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val stripHeight = height / 3f

        // Base values:
        // White: #FFFFFF, Blue: #0039A6, Red: #D52B1E
        // Dimmed ratios: saturation reduced, or alpha reduced, or darker colors.
        val whiteColor = if (isDarkTheme) android.graphics.Color.rgb(45, 45, 48) else android.graphics.Color.WHITE
        val blueColor = if (isDarkTheme) android.graphics.Color.rgb(0, 31, 92) else android.graphics.Color.rgb(0, 57, 166)
        val redColor = if (isDarkTheme) android.graphics.Color.rgb(115, 23, 16) else android.graphics.Color.rgb(213, 43, 30)

        // Top strip
        paint.color = whiteColor
        canvas.drawRect(RectF(0f, 0f, width, stripHeight), paint)

        // Middle strip
        paint.color = blueColor
        canvas.drawRect(RectF(0f, stripHeight, width, stripHeight * 2f), paint)

        // Bottom strip
        paint.color = redColor
        canvas.drawRect(RectF(0f, stripHeight * 2f, width, height), paint)
    }
}

/**
 * A beautiful background Composable that renders the static flag and places a semi-transparent
 * overlay on top of it to preserve text/icon legibility.
 * Supports watermarks (with opacity 10-15%) or full backdrop options.
 */
@Composable
fun RussianFlagBackdrop(
    modifier: Modifier = Modifier,
    isWatermark: Boolean = false,
    alpha: Float = if (isWatermark) 0.12f else 1.0f
) {
    val isDark = isSystemInDarkTheme()
    val flagPainter = RussianFlagBackground()

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val canvas = drawContext.canvas.nativeCanvas
                if (isWatermark) {
                    // Center a smaller watermark of the flag (e.g. 50% scale, centered with low opacity)
                    canvas.save()
                    val w = size.width
                    val h = size.height
                    canvas.clipRect(0f, 0f, w, h)
                    // Draw localized flag
                    flagPainter.draw(canvas, w, h, isDark)
                    canvas.restore()
                } else {
                    flagPainter.draw(canvas, size.width, size.height, isDark)
                }
            }
    ) {
        if (!isWatermark) {
            // Apply semi-transparent overlay to ensure extreme content readability over the flag
            // For light mode: 75% white (#BFFFFFFF). For dark mode: 85% dark grey/black (#D9121212)
            val overlayColor = if (isDark) {
                Color(0xD9121212)
            } else {
                Color(0xBFFFFFFF)
            }
            // Simple canvas overlay
            ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = overlayColor)
            }
        } else {
            // Overlaid dimming for watermark
            val overlayColor = if (isDark) {
                Color(0xF2121212) // higher dark overlay
            } else {
                Color(0xF2FFFFFF) // high white overlay
            }
            ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = overlayColor)
            }
        }
    }
}
