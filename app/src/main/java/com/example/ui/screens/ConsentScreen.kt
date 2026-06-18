package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConsentScreen(
    modifier: Modifier = Modifier,
    onAccept: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("consent_screen")
    ) {
        // Static patriotic elegant background instead of heavy procedural CPU-blocking animations
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0C2340), // Premium Dark Nave Blue
                            Color(0xFF1D3557)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 16.dp)
                    .testTag("consent_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Безопасность",
                            tint = Color(0xFFD52B1E),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "РОСБРАУЗЕР",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                    // Scrollable Agreement
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 16.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = "Пользовательское соглашение",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "Добро пожаловать в Росбраузер (Отечество) — суверенный российский интернет-браузер, функционирующий полностью независимо от зарубежной технологической инфраструктуры.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )

                        Text(
                            text = "Важная информация о соблюдении законодательства РФ:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 6.dp),
                            color = Color(0xFFD52B1E)
                        )

                        Text(
                            text = "Данное программное обеспечение осуществляет автоматическую блокировку доступа к интернет-ресурсам, внесённым в Единый реестр запрещённой информации Федеральной службы по надзору в сфере связи, информационных технологий и массовых коммуникаций (Роскомнадзор) в соответствии с Федеральным законом № 149-ФЗ «Об информации, информационных технологиях и о защите информации».\n\n" +
                                   "Для обеспечения безопасности граждан и противодействия обходу блокировок, в браузере принудительно используется DNS-over-HTTPS серверов российских провайдеров. Браузер осуществляет фоновые запросы для регулярного обновления локальной базы блокировок.\n\n" +
                                   "Также в данном веб-браузере по умолчанию активированы корневые сертификаты Национального Удостоверяющего Центра и Минцифры России для защищённого подключения к государственным ресурсам.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFD52B1E).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Внимание",
                                tint = Color(0xFFD52B1E),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Нажимая кнопку «Принимаю условия и продолжить», вы безоговорочно соглашаетесь со всеми условиями эксплуатации данного приложения на территории РФ.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // Default checked to avoid user search/click difficulty on smaller screen sizes
                    var isChecked by remember { mutableStateOf(true) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isChecked = !isChecked }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it },
                            modifier = Modifier.testTag("consent_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Я ознакомлен(а) с правилами и требованиями ФЗ № 149-ФЗ",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Confirm buttons
                    Button(
                        onClick = onAccept,
                        enabled = isChecked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("accept_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD52B1E),
                            disabledContainerColor = Color(0xFFD52B1E).copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Принимаю и Продолжить",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    // Decline button (Requirement 3: Exit app if declined)
                    Button(
                        onClick = {
                            Toast.makeText(
                                context,
                                "Использование браузера невозможно без согласия со всеми условиями согласно законодательству РФ.",
                                Toast.LENGTH_LONG
                            ).show()
                            (context as? Activity)?.finish() ?: kotlin.system.exitProcess(0)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(44.dp)
                            .testTag("decline_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "Отклонить и Выйти",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
