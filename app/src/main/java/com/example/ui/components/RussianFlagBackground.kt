package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

/**
 * Themes for the background.
 */
enum class BackgroundTheme {
    SUMMER,
    DARK,
    LIGHT
}

/**
 * Beautiful, high-performance background drawing a realistic waving Russian flag
 * alongside procedurally generated realistic trees (Birch/Pine) swaying in the wind.
 */
@Composable
fun RussianFlagBackground(
    modifier: Modifier = Modifier,
    bgTheme: BackgroundTheme = BackgroundTheme.SUMMER,
    windStrength: Float = 1.0f
) {
    // Phase for flag waves and tree sways
    val infiniteTransition = rememberInfiniteTransition(label = "background_dynamics")
    
    // Wave phase animation
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * java.lang.Math.PI.toFloat(),
        animationSpec = infiniteSpec(4000),
        label = "flag_phase"
    )

    // Wind sway animation for trees
    val treeSway by infiniteTransition.animateFloat(
        initialValue = -0.05f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tree_sway"
    )

    // Star pulsing animation for dark theme
    val starPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_pulse"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw static background base
            drawBackgroundBase(bgTheme, width, height, starPulse)

            // 2. Draw procedural landscape (hills / grass)
            drawLandscape(bgTheme, width, height, phase)

            // 3. Draw realistic procedural trees (Birch or Pine) swaying in the wind
            drawTrees(bgTheme, width, height, treeSway, windStrength)

            // 4. Draw realistic waving Russian Flag
            drawWavingRussianFlag(width, height, phase)
        }
    }
}

@Composable
private fun infiniteSpec(durationMillis: Int): InfiniteRepeatableSpec<Float> {
    return infiniteRepeatable(
        animation = tween(durationMillis, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
}

/**
 * Draw background Sky and Atmosphere
 */
private fun DrawScope.drawBackgroundBase(
    theme: BackgroundTheme,
    width: Float,
    height: Float,
    starPulse: Float
) {
    when (theme) {
        BackgroundTheme.SUMMER -> {
            // Sunny blue sky gradient
            drawRect(
                color = Color(0xFFE3F2FD),
                size = Size(width, height)
            )
            // Soft white clouds/sun glow at top left
            drawCircle(
                color = Color(0x33FFFDE7),
                radius = width * 0.4f,
                center = Offset(width * 0.1f, height * 0.15f)
            )
        }
        BackgroundTheme.DARK -> {
            // Deep space/starry night gradient
            drawRect(
                color = Color(0xFF0F172A),
                size = Size(width, height)
            )
            // Star fields
            val stars = listOf(
                Offset(width * 0.15f, height * 0.1f),
                Offset(width * 0.35f, height * 0.18f),
                Offset(width * 0.70f, height * 0.08f),
                Offset(width * 0.85f, height * 0.22f),
                Offset(width * 0.50f, height * 0.12f),
                Offset(width * 0.22f, height * 0.30f),
                Offset(width * 0.80f, height * 0.35f),
                Offset(width * 0.05f, height * 0.45f)
            )
            stars.forEachIndexed { i, offset ->
                val p = if (i % 2 == 0) starPulse else (1.3f - starPulse).coerceIn(0.2f, 1.0f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f * p),
                    radius = 3.5f * p,
                    center = offset
                )
            }
        }
        BackgroundTheme.LIGHT -> {
            // Elegant light clean gray
            drawRect(
                color = Color(0xFFF8FAFC),
                size = Size(width, height)
            )
        }
    }
}

/**
 * Draw grass / landscape hills
 */
private fun DrawScope.drawLandscape(
    theme: BackgroundTheme,
    width: Float,
    height: Float,
    phase: Float
) {
    val hillY = height * 0.72f
    when (theme) {
        BackgroundTheme.SUMMER -> {
            // Warm green grassy hills
            val hillPath1 = Path().apply {
                moveTo(0f, height)
                lineTo(0f, hillY + 40f)
                quadraticBezierTo(width * 0.35f, hillY - 30f, width * 0.7f, hillY + 20f)
                quadraticBezierTo(width * 0.85f, hillY + 40f, width, hillY)
                lineTo(width, height)
                close()
            }
            drawPath(hillPath1, Color(0xFF4CAF50))

            val hillPath2 = Path().apply {
                moveTo(0f, height)
                lineTo(0f, hillY + 90f)
                quadraticBezierTo(width * 0.5f, hillY + 30f, width, hillY + 70f)
                lineTo(width, height)
                close()
            }
            drawPath(hillPath2, Color(0xFF388E3C))
        }
        BackgroundTheme.DARK -> {
            // Deep forest-dark landscape
            val hillPath1 = Path().apply {
                moveTo(0f, height)
                lineTo(0f, hillY + 50f)
                quadraticBezierTo(width * 0.4f, hillY, width * 0.8f, hillY + 60f)
                quadraticBezierTo(width * 0.9f, hillY + 70f, width, hillY + 30f)
                lineTo(width, height)
                close()
            }
            drawPath(hillPath1, Color(0xFF1E293B))

            val hillPath2 = Path().apply {
                moveTo(0f, height)
                lineTo(0f, hillY + 110f)
                quadraticBezierTo(width * 0.5f, hillY + 60f, width, hillY + 100f)
                lineTo(width, height)
                close()
            }
            drawPath(hillPath2, Color(0xFF0F172A))
        }
        BackgroundTheme.LIGHT -> {
            // Soft grey/beige minimal wave hills
            val hillPath = Path().apply {
                moveTo(0f, height)
                lineTo(0f, hillY + 80f)
                quadraticBezierTo(width * 0.5f, hillY + 20f, width, hillY + 80f)
                lineTo(width, height)
                close()
            }
            drawPath(hillPath, Color(0xFFE2E8F0))
        }
    }
}

/**
 * Draw procedurally generated, beautifully realistic Birch/Pine trees
 */
private fun DrawScope.drawTrees(
    theme: BackgroundTheme,
    width: Float,
    height: Float,
    swayAngle: Float,
    windStrength: Float
) {
    if (theme == BackgroundTheme.LIGHT) return // Simple style has no heavy trees

    // Determine position coordinates on the hills
    val positions = listOf(
        // x pos factor, size scale, type (0=Birch, 1=Pine)
        TreeDefinition(0.12f, 0.95f, if (theme == BackgroundTheme.SUMMER) 0 else 1),
        TreeDefinition(0.28f, 0.75f, if (theme == BackgroundTheme.SUMMER) 0 else 1),
        TreeDefinition(0.68f, 0.8f, 1), // Pine is beautiful in both
        TreeDefinition(0.85f, 1.1f, if (theme == BackgroundTheme.SUMMER) 0 else 1)
    )

    positions.forEach { def ->
        val treeX = width * def.xFactor
        val treeY = height * 0.76f + (def.scale * 15f) // Place securely on grass

        withTransform({
            translate(left = treeX, top = treeY)
            scale(scaleX = def.scale, scaleY = def.scale, pivot = Offset(0f, 0f))
        }) {
            if (def.type == 0) {
                // Realistic Russian Birch (White trunk, detailed branches, green organic canopies)
                drawBirchTree(swayAngle * windStrength)
            } else {
                // Realistic Russian Pine (Dense evergreen stacked needle needles, organic dark branches)
                drawPineTree(swayAngle * windStrength)
            }
        }
    }
}

/**
 * Draw Birch tree with white bark, black patterns, and detailed branches
 */
private fun DrawScope.drawBirchTree(sway: Float) {
    val trunkHeight = 120f
    val trunkWidth = 10f

    // Swaying math for branches: progressively sways more towards top
    // 1. Draw Birch Trunk
    val trunkPath = Path().apply {
        moveTo(-trunkWidth / 2f, 0f)
        lineTo(-trunkWidth / 2.5f + (sway * 20f), -trunkHeight)
        lineTo(trunkWidth / 2.5f + (sway * 20f), -trunkHeight)
        lineTo(trunkWidth / 2f, 0f)
        close()
    }
    // Birch bark is white with dark patterns
    drawPath(trunkPath, Color.White)
    
    // Draw black lines/stripes on the bark (characteristic birch look)
    val barkDots = listOf(
        Offset(-trunkWidth/2f, -15f) to Offset(-trunkWidth/4f, -17f),
        Offset(trunkWidth/4f, -35f) to Offset(trunkWidth/2f, -37f),
        Offset(-trunkWidth/2f, -55f) to Offset(-trunkWidth/5f, -57f),
        Offset(trunkWidth/5f, -75f) to Offset(trunkWidth/2f, -78f),
        Offset(-trunkWidth/2.5f, -95f) to Offset(-5f, -97f)
    )
    barkDots.forEach { (start, end) ->
        drawLine(
            color = Color(0xFF1E293B),
            start = start,
            end = end,
            strokeWidth = 2.5f
        )
    }

    // 2. Draw Realistic Branches (Birch branches sweep downwards/outwards)
    // Branch left
    val leftBranchStart = Offset((sway * 10f), -trunkHeight * 0.4f)
    val leftBranchEnd = Offset(-45f + (sway * 35f), -trunkHeight * 0.65f)
    drawLine(Color(0xFF2E1A11), leftBranchStart, leftBranchEnd, strokeWidth = 3f)
    // Green canopy on left branch
    drawCircle(Color(0xFF4CAF50).copy(alpha = 0.9f), radius = 28f, center = leftBranchEnd)
    drawCircle(Color(0xFF81C784).copy(alpha = 0.85f), radius = 20f, center = leftBranchEnd + Offset(10f, -8f))

    // Branch right
    val rightBranchStart = Offset((sway * 15f), -trunkHeight * 0.65f)
    val rightBranchEnd = Offset(50f + (sway * 45f), -trunkHeight * 0.9f)
    drawLine(Color(0xFF2E1A11), rightBranchStart, rightBranchEnd, strokeWidth = 2.5f)
    // Green canopy on right branch
    drawCircle(Color(0xFF388E3C).copy(alpha = 0.9f), radius = 32f, center = rightBranchEnd)
    drawCircle(Color(0xFF4CAF50).copy(alpha = 0.8f), radius = 22f, center = rightBranchEnd + Offset(-12f, -5f))

    // Top crown branches
    val topCrownEnd = Offset((sway * 60f), -trunkHeight - 35f)
    drawLine(Color(0xFF2E1A11), Offset((sway * 20f), -trunkHeight), topCrownEnd, strokeWidth = 2.5f)
    drawCircle(Color(0xFF2E7D32), radius = 38f, center = topCrownEnd)
    drawCircle(Color(0xFF66BB6A).copy(alpha = 0.85f), radius = 26f, center = topCrownEnd + Offset(-5f, -12f))
}

/**
 * Draw robust Pine tree with stacked needle clusters
 */
private fun DrawScope.drawPineTree(sway: Float) {
    val trunkHeight = 140f
    val trunkWidth = 12f

    // 1. Draw trunk
    val trunkPath = Path().apply {
        moveTo(-trunkWidth/2f, 0f)
        lineTo(-trunkWidth/3f + (sway * 15f), -trunkHeight)
        lineTo(trunkWidth/3f + (sway * 15f), -trunkHeight)
        lineTo(trunkWidth/2f, 0f)
        close()
    }
    // Forest pine dark brown trunk
    drawPath(trunkPath, Color(0xFF3E2723))

    // 2. Draw Pine needle layers (stacked cones / triangles)
    val baseColor = Color(0xFF1B5E20)
    val layers = listOf(
        // centerOffsetFactor, radiusMultiplier, verticalIndex, width
        PineLayer(0.2f, 1.1f, 0.25f, 65f), // bottom level
        PineLayer(0.5f, 1.0f, 0.5f, 55f),  // middle level
        PineLayer(0.75f, 0.9f, 0.72f, 45f), // upper level
        PineLayer(1.0f, 0.8f, 0.95f, 32f)   // peak top
    )

    layers.forEach { layer ->
        val yPos = -trunkHeight * layer.verticalIndex
        val windOffset = sway * 40f * layer.verticalIndex
        
        val needlePath = Path().apply {
            moveTo(windOffset, yPos - 30f) // tip of layer
            lineTo(-layer.width + windOffset, yPos + 15f) // bottom left
            lineTo(layer.width + windOffset, yPos + 15f) // bottom right
            close()
        }
        
        // Stack color variation (bottom layer darker, top layer lighter shade of pine green)
        val densityColor = baseColor.copy(
            red = (baseColor.red + (layer.verticalIndex * 0.12f)).coerceIn(0f, 1f),
            green = (baseColor.green + (layer.verticalIndex * 0.15f)).coerceIn(0f, 1f),
            blue = (baseColor.blue + (layer.verticalIndex * 0.08f)).coerceIn(0f, 1f)
        )
        drawPath(needlePath, densityColor)
    }
}

/**
 * Draw a beautiful waving Russian Flag using clean sine-wave math and realistic gradients
 */
private fun DrawScope.drawWavingRussianFlag(
    width: Float,
    height: Float,
    phase: Float
) {
    // Flag dimensions & position (placed beautifully in the upper right)
    val flagLeft = width * 0.42f
    val flagTop = height * 0.12f
    val flagHeight = height * 0.18f
    val flagWidth = width * 0.48f

    // Flagpole parameters
    val poleX = flagLeft - 6f
    val poleTop = flagTop - 15f
    val poleBottom = flagTop + flagHeight + 80f

    // Draw Pole
    drawLine(
        color = Color(0xFF78909C), // Steel pole
        start = Offset(poleX, poleTop),
        end = Offset(poleX, poleBottom),
        strokeWidth = 5f
    )
    drawCircle(
        color = Color(0xFFFFD54F), // Golden tip/globe
        radius = 7f,
        center = Offset(poleX, poleTop)
    )

    // Draw waving stripes with 3 bands (Top=White, Middle=Blue, Bottom=Red)
    val stripeHeight = flagHeight / 3f

    val colors = listOf(
        Color.White,            // White
        Color(0xFF0039A6),      // Russian Blue
        Color(0xFFD52B1E)       // Russian Red
    )

    colors.forEachIndexed { stripeIdx, stripeColor ->
        val stripeTop = flagTop + (stripeIdx * stripeHeight)
        
        // Integrate sine waves to generate waves with simulated shadow depths
        val wavePath = Path().apply {
            // Traverse from left to right along top boundary of the stripe
            for (x in 0..flagWidth.toInt() step 5) {
                val fx = x.toFloat()
                // Beautiful wave equation
                val waveY = getWaveOffset(fx, phase)
                if (fx == 0f) {
                    moveTo(flagLeft + fx, stripeTop + waveY)
                } else {
                    lineTo(flagLeft + fx, stripeTop + waveY)
                }
            }
            // Traverse right to left along bottom boundary of the stripe
            for (x in flagWidth.toInt() downTo 0 step 5) {
                val fx = x.toFloat()
                val waveY = getWaveOffset(fx, phase)
                lineTo(flagLeft + fx, stripeTop + stripeHeight + waveY)
            }
            close()
        }

        // Draw basic stripe body
        drawPath(wavePath, stripeColor)

        // Draw standard dynamic gradient shadow overlays for realistic wave depth
        val shadowPath = Path().apply {
            for (x in 0..flagWidth.toInt() step 5) {
                val fx = x.toFloat()
                val waveY = getWaveOffset(fx, phase)
                if (fx == 0f) {
                    moveTo(flagLeft + fx, stripeTop + waveY)
                } else {
                    lineTo(flagLeft + fx, stripeTop + waveY)
                }
            }
            for (x in flagWidth.toInt() downTo 0 step 5) {
                val fx = x.toFloat()
                val waveY = getWaveOffset(fx, phase)
                lineTo(flagLeft + fx, stripeTop + stripeHeight + waveY)
            }
            close()
        }

        // Add shading multiplier overlays matching flag folds
        for (x in 0..flagWidth.toInt() step 20) {
            val fx = x.toFloat()
            // Intensity is related to cosine/wave curvature to model folds
            val cosVal = cos(fx * 0.02f - phase)
            val shadeAlpha = (cosVal * 0.15f).coerceIn(0f, 0.25f)
            val lightAlpha = (-cosVal * 0.12f).coerceIn(0f, 0.2f)

            if (shadeAlpha > 0f) {
                // Wave folds / shadows
                val foldPath = Path().apply {
                    val wY1 = getWaveOffset(fx, phase)
                    val wY2 = getWaveOffset(fx + 20f, phase)
                    moveTo(flagLeft + fx, stripeTop + wY1)
                    lineTo(flagLeft + fx + 20f, stripeTop + wY2)
                    lineTo(flagLeft + fx + 20f, stripeTop + stripeHeight + wY2)
                    lineTo(flagLeft + fx, stripeTop + stripeHeight + wY1)
                    close()
                }
                drawPath(foldPath, Color.Black.copy(alpha = shadeAlpha))
            } else if (lightAlpha > 0f && stripeIdx > 0) {
                // Highlights on fold ridges (excluding absolute White top stripe for visibility)
                val peakPath = Path().apply {
                    val wY1 = getWaveOffset(fx, phase)
                    val wY2 = getWaveOffset(fx + 20f, phase)
                    moveTo(flagLeft + fx, stripeTop + wY1)
                    lineTo(flagLeft + fx + 20f, stripeTop + wY2)
                    lineTo(flagLeft + fx + 20f, stripeTop + stripeHeight + wY2)
                    lineTo(flagLeft + fx, stripeTop + stripeHeight + wY1)
                    close()
                }
                drawPath(peakPath, Color.White.copy(alpha = lightAlpha))
            }
        }
    }
}

/**
 * Wave function: returns Y delta based on distance x and phase
 */
private fun getWaveOffset(x: Float, phase: Float): Float {
    // Elegant waves decaying towards flagpole (attached on the left at flagpole)
    val attachmentStrength = (x / 140f).coerceIn(0f, 1.2f)
    return sin(x * 0.022f - phase * 1.2f) * 11f * attachmentStrength
}

// Data structures for tree generation config
data class TreeDefinition(val xFactor: Float, val scale: Float, val type: Int)
data class PineLayer(val centerOffsetFactor: Float, val radiusMultiplier: Float, val verticalIndex: Float, val width: Float)
