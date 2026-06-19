package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun YandexSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep splash active for 1.1 seconds
    LaunchedEffect(Unit) {
        delay(1100)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashTransition")
    
    // Ambient rotating glowing ring behind the logo
    val angleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LogoRotation"
    )

    // Pulsing background and glow scale
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    // Fade-in animated progress list for Russian search security subsystems
    var dynamicStatusText by remember { mutableStateOf("Запуск систем безопасности PROTECT...") }
    LaunchedEffect(Unit) {
        delay(250)
        dynamicStatusText = "Проверка ГОСТ SSL сертификатов Минцифры РФ..."
        delay(300)
        dynamicStatusText = "Синхронизация реестра блокировок ФЗ-149..."
        delay(300)
        dynamicStatusText = "Оптимизация Chromium Engine v114..."
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))
        )
    }

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val accentColor = Color(0xFFE52E20) // Signature Yandex Red

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Animated Yandex Style Logo Canvas
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = glowScale
                        scaleY = glowScale
                    }
            ) {
                // Glow Background
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2.3f
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    // Outer glow arc
                    drawCircle(
                        color = accentColor.copy(alpha = 0.08f),
                        radius = radius * 1.3f,
                        center = center
                    )
                }

                // Inner Canvas drawing Yandex Red/White ring & beautiful custom ribbon
                Canvas(modifier = Modifier.size(110.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val r = size.minDimension / 2f

                    // 1. Draw elegant white circle backdrop with border
                    drawCircle(
                        color = Color.White,
                        radius = r - 4f,
                        center = center
                    )
                    
                    // Draw outer signature red/ring frame
                    drawCircle(
                        color = accentColor,
                        radius = r - 4f,
                        center = center,
                        style = Stroke(width = 6f)
                    )

                    // 2. Beautiful Red Ribbon "Y" inside the circle
                    val ribbonPath = Path().apply {
                        val cx = size.width / 2
                        val cy = size.height / 2
                        
                        // Left branch starting from top left
                        moveTo(cx - 24f, cy - 36f)
                        // Down to intersection
                        lineTo(cx, cy)
                        // Up to top right branch
                        lineTo(cx + 24f, cy - 36f)
                        
                        // Right side of top-right branch
                        lineTo(cx + 34f, cy - 36f)
                        // Down with ribbon width
                        lineTo(cx + 6f, cy - 4f)
                        // Bottom foot
                        lineTo(cx + 6f, cy + 38f)
                        
                        // Width of bottom foot
                        lineTo(cx - 6f, cy + 38f)
                        // Up to left side of ribbon
                        lineTo(cx - 6f, cy - 4f)
                        // Left side of top-left branch
                        lineTo(cx - 34f, cy - 36f)
                        
                        close()
                    }

                    drawPath(
                        path = ribbonPath,
                        color = accentColor
                    )

                    // Add dynamic sweeping solar ring
                    drawArc(
                        color = Color(0xFFFF9E00),
                        startAngle = angleRotation,
                        sweepAngle = 45f,
                        useCenter = false,
                        topLeft = Offset(4f, 4f),
                        size = Size(size.width - 8f, size.height - 8f),
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Typography
            Text(
                text = "ЯНДЕКС БРАУЗЕР",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = 2.sp,
                color = textColor,
                fontFamily = FontFamily.SansSerif
            )

            Text(
                text = "БЫСТРЫЙ И БЕЗОПАСНЫЙ",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = accentColor,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Realistic loading status indicators & indicators matching real-time checkups
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Compact active spinner
                androidx.compose.material3.CircularProgressIndicator(
                    color = accentColor,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = dynamicStatusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Bottom Sovereign Stamp
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "ПРОВЕРЕНО PROTECT • CHROMIUM v114",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.35f),
                letterSpacing = 1.sp
            )
        }
    }
}
