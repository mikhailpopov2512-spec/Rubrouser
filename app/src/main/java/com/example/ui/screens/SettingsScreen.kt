package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BackgroundTheme
import com.example.ui.viewmodel.BrowserViewModel
import com.example.utils.FilteringLevel
import com.example.utils.NotificationHelper
import com.example.utils.RknBlocklistManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel
) {
    val context = LocalContext.current
    val currentTheme by viewModel.backgroundTheme.collectAsState()
    val filteringLevel by viewModel.filteringLevel.collectAsState()
    val customBlocks by viewModel.customBlockedList.collectAsState()
    val presets by viewModel.blockedServices.collectAsState()
    val uName by viewModel.userName.collectAsState()
    val uEmail by viewModel.userEmail.collectAsState()

    var customDomainInput by remember { mutableStateOf("") }
    var serviceNameInput by remember { mutableStateOf("") }
    var serviceDomainInput by remember { mutableStateOf("") }
    var showDialogAddService by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(18.dp)
    ) {
        // Active profile header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountCircle, "Profile", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (uName.isNotEmpty()) uName else "Сессионный гражданин",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (uEmail.isNotEmpty()) uEmail else "rosbrowser-user@gosuslugi.ru",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.logoutUser() }) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }

        // SECTION 1: THEMES & DESIGN SELECTION (Requested!)
        Text(
            text = "Оформление интерфейса",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val themes = listOf(
                ThemeOption(BackgroundTheme.SUMMER, "Летняя", Icons.Default.WbSunny),
                ThemeOption(BackgroundTheme.DARK, "Темная", Icons.Default.NightsStay),
                ThemeOption(BackgroundTheme.LIGHT, "Светлая", Icons.Default.LightMode)
            )

            themes.forEach { op ->
                val active = currentTheme == op.theme
                Card(
                    onClick = { viewModel.updateTheme(op.theme) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .border(
                            1.dp,
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(op.icon, contentDescription = op.name, modifier = Modifier.size(18.dp), tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(op.name, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }

        // SECTION 2: SYSTEM FILTER LEVELS (Requested!)
        Text(
            text = "Суверенная фильтрация сайтов",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                FilteringLevel.values().forEach { level ->
                    val selected = filteringLevel == level
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateFilteringLevel(level) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { viewModel.updateFilteringLevel(level) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = level.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = level.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    if (level != FilteringLevel.HIGH) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }

        // SECTION 3: CORE BLOCK CONTROLS (Requested!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Сервисы блокировки",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { showDialogAddService = true },
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, "Add Service", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Добавить сервис", fontSize = 12.sp)
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // List currently toggled preset engines
                presets.forEach { service ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (service.isEnabled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (service.isEnabled) Icons.Default.Block else Icons.Default.Check,
                                contentDescription = "Block Sign",
                                tint = if (service.isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(service.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(service.domain, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = service.isEnabled,
                            onCheckedChange = { RknBlocklistManager.toggleServiceBlock(service.name, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.error, checkedTrackColor = MaterialTheme.colorScheme.errorContainer)
                        )
                    }
                }
            }
        }

        // SECTION 4: NOTIFICATIONS & SYSTEM COMMANDS (Requested!)
        Text(
            text = "Служба безопасности уведомлений",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Button 1: Request permission
                Button(
                    onClick = {
                        val act = context as? Activity
                        if (act != null) {
                            NotificationHelper.requestNotificationPermission(act)
                            Toast.makeText(context, "Инициирован системный запрос разрешений", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, "Alert Setup")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Разрешить уведомления", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Button 2: Immediate FSB alert check (Highly requested periodic simulation fallback!)
                Button(
                    onClick = {
                        NotificationHelper.fireFsbAlertNow(context)
                        Toast.makeText(context, "Сигнал ФСБ отправлен на шторку", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDF3B3F)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VolumeUp, "Test Fsb Alert")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Проверить сигнал ФСБ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "Согласно законодательству РФ, приложение информирует пользователя о работающей системе фоновой прослушки со стороны ФСБ каждый час для прозрачности контроля.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                    lineHeight = 14.sp
                )
            }
        }
    }

    // ADD SOVEREIGN BLOCK Preset Dialog
    if (showDialogAddService) {
        AlertDialog(
            onDismissRequest = { showDialogAddService = false },
            title = { Text("Добавить сервис блокировки", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = serviceNameInput,
                        onValueChange = { serviceNameInput = it },
                        label = { Text("Название сервиса") },
                        placeholder = { Text("например, Netflix") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = serviceDomainInput,
                        onValueChange = { serviceDomainInput = it },
                        label = { Text("Домен для блокировки") },
                        placeholder = { Text("например, netflix.com") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (serviceNameInput.isNotEmpty() && serviceDomainInput.isNotEmpty()) {
                            RknBlocklistManager.addCustomService(serviceNameInput, serviceDomainInput)
                            serviceNameInput = ""
                            serviceDomainInput = ""
                            showDialogAddService = false
                            Toast.makeText(context, "Заблокировано новое направление", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Все поля обязательны", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Занести в реестр")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogAddService = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

data class ThemeOption(
    val theme: BackgroundTheme,
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
