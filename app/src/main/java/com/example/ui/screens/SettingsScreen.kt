package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.RussianFlagBackdrop
import com.example.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Viewmodel state properties
    val searchEngine by viewModel.selectedSearchEngine.collectAsState()
    val dnsType by viewModel.selectedDnsType.collectAsState()
    val syncType by viewModel.syncType.collectAsState()
    val syncAccountName by viewModel.syncAccountName.collectAsState()
    val customSyncEndpoint by viewModel.customSyncEndpoint.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()

    val isSafeBrowsingEnabled by viewModel.isSafeBrowsingEnabled.collectAsState()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsState()

    val lastRknUpdate by viewModel.lastRknUpdate.collectAsState()
    val isUpdatingRknList by viewModel.isUpdatingRknList.collectAsState()
    val blockedCount by viewModel.blockedAttemptsCount.collectAsState()
    val blockedUrls by viewModel.blockedUrls.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen")
    ) {
        // Low-opacity watermark of Russian flag inside settings background
        RussianFlagBackdrop(isWatermark = true)

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {

                // SECTION 1: SEARCH & SELECTION
                SectionHeader(title = "Поиск и отображение", icon = Icons.Default.Search)
                
                // Search Engine select card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Поисковая система по умолчанию:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf("Яндекс", "Mail.ru", "Rambler").forEachIndexed { index, name ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setSearchEngine(index) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = searchEngine == index,
                                    onClick = { viewModel.setSearchEngine(index) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(name, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Theme Mode Select Card
                val themeMode by viewModel.selectedThemeMode.collectAsState()
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Тема оформления интерфейса:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf(
                            "Системное оформление (Авто)",
                            "Светлая тема (По умолчанию)",
                            "Тёмная тема (Ночная)"
                        ).forEachIndexed { index, name ->
                            val optionVal = when(index) {
                                0 -> 0 // System
                                1 -> 1 // Light
                                else -> 2 // Dark
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setThemeMode(optionVal) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = themeMode == optionVal,
                                    onClick = { viewModel.setThemeMode(optionVal) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(name, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Address Bar Position Card
                val addressBarPos by viewModel.selectedAddressBarPosition.collectAsState()
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Расположение адресной строки:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Оптимизируйте управление браузером одной рукой или используйте классическую компоновку.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf(
                            "Снизу страницы (Рекомендуется для одной руки)",
                            "Сверху страницы (Классическое)"
                        ).forEachIndexed { index, name ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setAddressBarPosition(index) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = addressBarPos == index,
                                    onClick = { viewModel.setAddressBarPosition(index) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(name, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Custom NTP widgets card setup
                val showWeather by viewModel.showWeatherWidget.collectAsState()
                val showTraffic by viewModel.showTrafficWidget.collectAsState()
                val showRates by viewModel.showRatesWidget.collectAsState()
                val showDzen by viewModel.showDzenWidget.collectAsState()
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Отечественные инфо-виджеты Табло:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Показывать виджет Яндекс.Погода", fontSize = 13.sp)
                            Switch(
                                checked = showWeather,
                                onCheckedChange = { viewModel.toggleWidget("weather", it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Показывать виджет Яндекс.Пробки", fontSize = 13.sp)
                            Switch(
                                checked = showTraffic,
                                onCheckedChange = { viewModel.toggleWidget("traffic", it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Показывать виджет Курсы валют ЦБ РФ", fontSize = 13.sp)
                            Switch(
                                checked = showRates,
                                onCheckedChange = { viewModel.toggleWidget("rates", it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Показывать ленту Мультирубрика Дзен", fontSize = 13.sp)
                            Switch(
                                checked = showDzen,
                                onCheckedChange = { viewModel.toggleWidget("dzen", it) }
                            )
                        }
                    }
                }


                // SECTION 2: INDEPENDENT RUSSIAN SYNC
                SectionHeader(title = "Импортозамещённая синхронизация", icon = Icons.Default.Sync)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Профиль и облачное резервирование (VK ID / Яндекс ID)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Свяжите свои закладки, историю и пароли с суверенной учётной записью. Google Account полностью заблокирован на уровне ядра.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Выберите экосистему:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        
                        var showAccountInput by remember { mutableStateOf(false) }
                        var tempAccountName by remember { mutableStateOf(syncAccountName) }
                        var tempEndpoint by remember { mutableStateOf(customSyncEndpoint) }

                        val ecosystems = listOf(
                            "Отключить синхронизацию",
                            "Яндекс ID",
                            "VK ID",
                            "Собственный сервер синхронизации"
                        )

                        ecosystems.forEachIndexed { index, label ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (index == 0) {
                                            viewModel.setSyncType(0)
                                            showAccountInput = false
                                        } else {
                                            showAccountInput = true
                                            viewModel.setSyncType(index, tempAccountName, tempEndpoint)
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = syncType == index,
                                    onClick = {
                                        if (index == 0) {
                                            viewModel.setSyncType(0)
                                            showAccountInput = false
                                        } else {
                                            showAccountInput = true
                                            viewModel.setSyncType(index, tempAccountName, tempEndpoint)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, fontSize = 14.sp)
                            }
                        }

                        if (syncType > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = tempAccountName,
                                onValueChange = {
                                    tempAccountName = it
                                    viewModel.setSyncType(syncType, tempAccountName, tempEndpoint)
                                },
                                label = { Text("Имя пользователя / ID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            if (syncType == 3) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = tempEndpoint,
                                    onValueChange = {
                                        tempEndpoint = it
                                        viewModel.setSyncType(syncType, tempAccountName, tempEndpoint)
                                    },
                                    label = { Text("Эндпоинт сервера (API URL)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (isSyncing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Выполняется синхронизация данных...", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (syncType > 0) Icons.Default.Check else Icons.Default.Info,
                                    contentDescription = "Статус",
                                    tint = if (syncType > 0) Color(0xFF3DDC84) else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(syncStatusMessage, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }


                // SECTION 3: RKN ACCESS LOCK REGISTRY INFO
                SectionHeader(title = "Реестр блокировок Роскомнадзора", icon = Icons.Default.Block)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD52B1E).copy(alpha = 0.08f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = "RKN", tint = Color(0xFFD52B1E))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Информационный реестр ФЗ №149-ФЗ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFD52B1E)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Браузер осуществляет строгую сверку сетевых запросов перед отправкой для пресечения обхода регламентированных государством условий навигации.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Количество правил в базе:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${blockedUrls.size} ресурсов", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Попыток обхода блокировок:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$blockedCount запросов", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFD52B1E))
                            }

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Последнее обновление базы:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(lastRknUpdate, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.updateRknBlocklist() },
                                enabled = !isUpdatingRknList,
                                modifier = Modifier.weight(1f).testTag("update_rkn_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isUpdatingRknList) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("Обновить", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearStats() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Сбросить ст.", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.restoreBlocklistDefaults() },
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("По умолч.", fontSize = 12.sp)
                            }
                        }
                    }
                }


                // SECTION 4: SECURITY & CERTIFICATES (TRUST STORE MINTSIRY)
                SectionHeader(title = "Шифрование и Сертификаты", icon = Icons.Default.Shield)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "ГОСТ", tint = Color(0xFF0039A6))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ГОСТ-криптография & Хранилище",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Браузер поддерживает шифрование ГОСТ 28147-89, ГОСТ Р 34.10-2012 и ГОСТ Р 34.11-2012 для безопасного подключения к государственным сервисам (СМЭВ, Госуслуги).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Доверенные корневые сертификаты РФ:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Root certificate row 1
                        CertificateRow(
                            name = "Минцифры России Root CA",
                            issuedTo = "Национальный Удостоверяющий Центр",
                            algorithm = "ГОСТ Р 34.10-2012 (256 бит)",
                            serialStr = "RU-MINTS-2022-MAIN-992AZ",
                            validUntil = "04.04.2042"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Root certificate row 2
                        CertificateRow(
                            name = "Правительство РФ Суб-Сертификат",
                            issuedTo = "СМЭВ и Госуслуги Единое ядро",
                            algorithm = "ГОСТ Р 34.11-2012 / ГОСТ 28147",
                            serialStr = "GOV-RU-CORE-2024-88A",
                            validUntil = "12.08.2034"
                        )
                    }
                }


                // SECTION 5: DNS-over-HTTPS & SAFETY MODES
                SectionHeader(title = "Безопасное соединение", icon = Icons.Default.Dns)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Принудительный DNS-over-HTTPS (DoH)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Запрещает смену DNS на зарубежные серверы (Google, Cloudflare), предотвращая обход трафикового контроля и утечку данных.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val dnsOptions = listOf(
                            "Системный DNS (Fallthrough)",
                            "Яндекс.DNS Безопасный (DoH https://common-dns.yandex.ru)",
                            "НИИ «Восход» Суверенный DNS"
                        )

                        dnsOptions.forEachIndexed { index, dnsLabel ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setDnsType(index) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = dnsType == index,
                                    onClick = { viewModel.setDnsType(index) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dnsLabel, fontSize = 13.sp)
                            }
                        }

                        // Switches for Safe browsing and AdBlock
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Проверка угроз (Yandex Safe Browsing)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Проверяет адреса по базам небезопасных сайтов Яндекса", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                            Switch(
                                checked = isSafeBrowsingEnabled,
                                onCheckedChange = { viewModel.toggleSafeBrowsing(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Защита от рекламы (AdBlock)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Блокирует российские рекламные модули и следящие трекеры", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                            Switch(
                                checked = isAdBlockEnabled,
                                onCheckedChange = { viewModel.toggleAdBlock(it) }
                            )
                        }
                    }
                }

                // Legal stamp
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Росбраузер v1.0.4-chromium • Продукт полностью автономен • Лицензия Минцифры РФ",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun CertificateRow(
    name: String,
    issuedTo: String,
    algorithm: String,
    serialStr: String,
    validUntil: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Доверенный", tint = Color(0xFF3DDC84), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Активен", fontSize = 11.sp, color = Color(0xFF3DDC84), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Кому выдан: $issuedTo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            Text("Алгоритм: $algorithm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            Text("Серийный номер: $serialStr", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Text("Срок действия: до $validUntil", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        }
    }
}
