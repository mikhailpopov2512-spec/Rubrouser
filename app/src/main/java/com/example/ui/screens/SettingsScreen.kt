package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PremiumBackdrop
import com.example.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Preferences & ViewModel State bindings
    val searchEngine by viewModel.selectedSearchEngine.collectAsState()
    val dnsType by viewModel.selectedDnsType.collectAsState()
    val syncType by viewModel.syncType.collectAsState()
    val syncAccountName by viewModel.syncAccountName.collectAsState()
    val customSyncEndpoint by viewModel.customSyncEndpoint.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()

    val isSafeBrowsingEnabled by viewModel.isSafeBrowsingEnabled.collectAsState()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsState()

    val gostCipherSuite by viewModel.selectedGostCipherSuite.collectAsState()
    val useMintsifryCertsOnly by viewModel.useMintsifryCertsOnly.collectAsState()

    val lastRknUpdate by viewModel.lastRknUpdate.collectAsState()
    val isUpdatingRknList by viewModel.isUpdatingRknList.collectAsState()
    val blockedCount by viewModel.blockedAttemptsCount.collectAsState()
    val blockedUrls by viewModel.blockedUrls.collectAsState()

    // Interactive Search and sheets controls
    var settingsSearchQuery by remember { mutableStateOf("") }
    var showPrivacyPolicySheet by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val cardBackground = if (isDark) Color(0xFF1E293B) else Color.White
    val accentColor = Color(0xFFE52E20) // Authentic Yandex Red Accent

    // Helper functions for matching search terms
    fun matchesQuery(vararg keywords: String): Boolean {
        if (settingsSearchQuery.isBlank()) return true
        return keywords.any { it.contains(settingsSearchQuery, ignoreCase = true) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen")
    ) {
        // High fidelity summer beach background layer
        PremiumBackdrop(isWatermark = true, alphaVal = 0.08f)

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Elegant Yandex style header with Back button
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Настройки", 
                            fontWeight = FontWeight.Black, 
                            fontSize = 19.sp, 
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            "Управление системой PROTECT", 
                            fontSize = 11.sp, 
                            color = accentColor, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack, 
                            contentDescription = "Назад",
                            tint = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ),
                actions = {
                    IconButton(onClick = { settingsSearchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Контроль", tint = accentColor)
                    }
                }
            )

            // Dynamic 2dp Russian Flag Line indicator
            Row(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0039A6)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD52B1E)))
            }

            // Central scrolling settings area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Search field - Live filters all preference subcards instantly!
                OutlinedTextField(
                    value = settingsSearchQuery,
                    onValueChange = { settingsSearchQuery = it },
                    placeholder = { Text("Поиск настроек (например, РКН, шифрование...)", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = accentColor, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (settingsSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { settingsSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                // SECTION 1: SEARCH & GENERAL LOOK (YANDEX COMPATIBLE)
                if (matchesQuery("поиск", "яндекс", "тема", "виджет", "погода", "пробки", "дзен", "адрес", "табло")) {
                    YandexSectionHeader(title = "Поиск и отображение", icon = Icons.Default.Layers)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Sub 1: Search Default Selector
                            Text("Поисковая система по умолчанию:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Определяет, где будут открываться не-URL запросы адресной строки.", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            listOf("Яндекс", "Mail.ru", "Rambler").forEachIndexed { index, name ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setSearchEngine(index) }
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = searchEngine == index,
                                        onClick = { viewModel.setSearchEngine(index) },
                                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name, fontSize = 14.sp, fontWeight = if (searchEngine == index) FontWeight.Bold else FontWeight.Normal)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            // Sub 2: Dark/Light Themes
                            val themeMode by viewModel.selectedThemeMode.collectAsState()
                            Text("Тема оформления интерфейса:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            listOf(
                                "Системное оформление (Автоконтроль)",
                                "Светлая летняя тема (По умолчанию)",
                                "Тёмная тема (Ночной режим)"
                            ).forEachIndexed { index, name ->
                                val optionVal = when(index) {
                                    0 -> 0 
                                    1 -> 1 
                                    else -> 2 
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setThemeMode(optionVal) }
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = themeMode == optionVal,
                                        onClick = { viewModel.setThemeMode(optionVal) },
                                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name, fontSize = 13.sp, fontWeight = if (themeMode == optionVal) FontWeight.Bold else FontWeight.Normal)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            // Sub 3: Ntp backgrounds
                            val ntpBgTheme by viewModel.selectedNtpThemeBackground.collectAsState()
                            Text("Декоративный фон Главной страницы (Табло):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            listOf(
                                "Летний пляж (Анимированное солнце, облака и пруд)",
                                "Суверенный флаг России (Патриотический)",
                                "Минималистичный чистый монохром"
                            ).forEachIndexed { index, name ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setNtpBackgroundTheme(index) }
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = ntpBgTheme == index,
                                        onClick = { viewModel.setNtpBackgroundTheme(index) },
                                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name, fontSize = 13.sp, fontWeight = if (ntpBgTheme == index) FontWeight.Bold else FontWeight.Normal)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            // Sub 4: Address bar layout
                            val addressBarPos by viewModel.selectedAddressBarPosition.collectAsState()
                            Text("Позиция Омнибокса (адресной строки):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Yandex-эргономика: нижнее размещение идеально оптимизирует скроллинг одной рукой.", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            listOf(
                                "Адрес снизу экрана (Рекомендовано Yandex)",
                                "Адрес сверху экрана (Классический Chromium)"
                            ).forEachIndexed { index, name ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setAddressBarPosition(index) }
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = addressBarPos == index,
                                        onClick = { viewModel.setAddressBarPosition(index) },
                                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name, fontSize = 13.sp, fontWeight = if (addressBarPos == index) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // Card 2: National Domestic Widgets
                    val showWeather by viewModel.showWeatherWidget.collectAsState()
                    val showTraffic by viewModel.showTrafficWidget.collectAsState()
                    val showRates by viewModel.showRatesWidget.collectAsState()
                    val showDzen by viewModel.showDzenWidget.collectAsState()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Информационные сервисы на новой вкладке:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            YandexSwitchRow(
                                title = "Информер Яндекс.Погода",
                                description = "Показывать метеоданные с точностью до дома",
                                checked = showWeather,
                                onCheckedChange = { viewModel.toggleWidget("weather", it) },
                                activeColor = accentColor
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            YandexSwitchRow(
                                title = "Информер Яндекс.Пробки",
                                description = "Баллы загруженности дорог в вашем городе в реальном времени",
                                checked = showTraffic,
                                onCheckedChange = { viewModel.toggleWidget("traffic", it) },
                                activeColor = accentColor
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            YandexSwitchRow(
                                title = "Информер Финансы ЦБ РФ",
                                description = "Курсы доллара, евро и юаня от Центробанка",
                                checked = showRates,
                                onCheckedChange = { viewModel.toggleWidget("rates", it) },
                                activeColor = accentColor
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            YandexSwitchRow(
                                title = "Новостная лента рекомендаций Дзен",
                                description = "Занимательные статьи и заголовки новостей из РФ",
                                checked = showDzen,
                                onCheckedChange = { viewModel.toggleWidget("dzen", it) },
                                activeColor = accentColor
                            )
                        }
                    }
                }

                // SECTION 2: SYNCHRONIZATION AND USER MANAGEMENT
                if (matchesQuery("синхронизация", "профиль", "vk", "яндекс id", "облако", "аккаунт", "сервер", "данные")) {
                    YandexSectionHeader(title = "Синхронизация и Облачные сервисы", icon = Icons.Default.CloudSync)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Параметры резервного копирования",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Сохраняйте закладки и историю на защищенных российских облачных хранилищах. Google Sync модули полностью отключены.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            var showAccountInput by remember { mutableStateOf(false) }
                            var tempAccountName by remember { mutableStateOf(syncAccountName) }
                            var tempEndpoint by remember { mutableStateOf(customSyncEndpoint) }

                            val ecosystems = listOf(
                                "Отключить синхронизацию",
                                "Авторизовать через Яндекс ID",
                                "Авторизовать через VK ID",
                                "Индивидуальный REST-сервер синхронизации"
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
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, fontSize = 13.sp, fontWeight = if (syncType == index) FontWeight.Bold else FontWeight.Normal)
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
                                    label = { Text("Логин / ID учетной записи") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                                )
                                
                                if (syncType == 3) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = tempEndpoint,
                                        onValueChange = {
                                            tempEndpoint = it
                                            viewModel.setSyncType(syncType, tempAccountName, tempEndpoint)
                                        },
                                        label = { Text("Эндпоинт сервера (API URL URL)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (isSyncing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accentColor, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Шифрование данных по ГОСТ и пересылка...", fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (syncType > 0) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (syncType > 0) Color(0xFF16A342) else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(syncStatusMessage, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (syncType > 0) Color(0xFF16A342) else Color.Gray)
                                }
                            }
                        }
                    }
                }

                // SECTION 3: RKN ACCESS LOCK REGISTRY INFO
                if (matchesQuery("блокиров", "ркн", "реестр", "регулирование", "домен", "запрет", "запрос", "цезура", "статистика")) {
                    YandexSectionHeader(title = "Реестр Роскомнадзора (149-ФЗ)", icon = Icons.Default.Gavel)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE52E20).copy(alpha = 0.04f)),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE52E20).copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "RKN", tint = accentColor, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Автоматический инспектор 149-ФЗ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Интеллектуальный парсер сетевых пакетов на лету отклоняет сессии к запрещённым экстремистским или мошенническим сайтам, внесённым в Единый реестр РФ.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Глобальных правил в базе:", fontSize = 12.sp, color = Color.Gray)
                                    Text("${blockedUrls.size} доменных шаблонов", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Предотвращено переходов (Атаки/Запреты):", fontSize = 12.sp, color = Color.Gray)
                                    Text("$blockedCount сетевых сессий", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = accentColor)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Статус базы данных:", fontSize = 12.sp, color = Color.Gray)
                                    Text("Обновлена $lastRknUpdate", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF16A342))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { viewModel.updateRknBlocklist() },
                                    enabled = !isUpdatingRknList,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("update_rkn_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    if (isUpdatingRknList) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 1.5.dp)
                                    } else {
                                        Text("Обновить", fontSize = 11.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.clearStats() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                ) {
                                    Text("Сбросить статистику", fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    // Card 4: Local Blocker Registry Editor
                    var blocklistLocalSearchQuery by remember { mutableStateOf("") }
                    var showAddLocalBlocklistDialog by remember { mutableStateOf(false) }
                    var newBlockPattern by remember { mutableStateOf("") }
                    var newBlockReason by remember { mutableStateOf("") }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Персональный список ограничений",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                IconButton(onClick = { showAddLocalBlocklistDialog = true }) {
                                    Icon(Icons.Default.AddCircle, "Вшитый запрет", tint = accentColor)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Дополнительно блокирует любые ресурсы (например, для родительского контроля).",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = blocklistLocalSearchQuery,
                                onValueChange = { blocklistLocalSearchQuery = it },
                                placeholder = { Text("Быстрый поиск в вашей базе данных...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            val localFiltered = remember(blockedUrls, blocklistLocalSearchQuery) {
                                if (blocklistLocalSearchQuery.isBlank()) {
                                    blockedUrls.take(6)
                                } else {
                                    blockedUrls.filter {
                                        it.pattern.contains(blocklistLocalSearchQuery, ignoreCase = true) ||
                                        it.reason.contains(blocklistLocalSearchQuery, ignoreCase = true)
                                    }
                                }
                            }

                            if (localFiltered.isEmpty()) {
                                Text(
                                    "Совпадений не найдено.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    style = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    localFiltered.forEach { rule ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(rule.pattern, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(rule.reason, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteBlockedUrl(rule.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showAddLocalBlocklistDialog) {
                        AlertDialog(
                            onDismissRequest = { showAddLocalBlocklistDialog = false },
                            title = { Text("Добавить запрет", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Введите доменное имя, которое хотите полностью заблокировать на этом устройстве.", fontSize = 12.sp)
                                    OutlinedTextField(
                                        value = newBlockPattern,
                                        onValueChange = { newBlockPattern = it },
                                        label = { Text("Домен (напр., facebook.com)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = newBlockReason,
                                        onValueChange = { newBlockReason = it },
                                        label = { Text("Причина/Примечание блокировки") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (newBlockPattern.isNotBlank()) {
                                            viewModel.addBlockedUrl(newBlockPattern, newBlockReason.ifBlank { "Пользовательское ограничение" })
                                            newBlockPattern = ""
                                            newBlockReason = ""
                                            showAddLocalBlocklistDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Text("Заблокировать")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddLocalBlocklistDialog = false }) {
                                    Text("Отмена")
                                }
                            }
                        )
                    }
                }

                // SECTION 4: ENCRYPTION, CERTIFICATES AND PROTECT TRUST STORE
                if (matchesQuery("шифр", "сертификат", "минцифры", "гост", "алгоритм", "кузнечик", "безопасност", "ssl")) {
                    YandexSectionHeader(title = "ГОСТ Шифрование & Сертификаты", icon = Icons.Default.VerifiedUser)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = "ГОСТ", tint = Color(0xFF0039A6), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Шифрование стандарта ГОСТ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Полная совместимость с серверами Госуслуг, Сбербанка и ФНС РФ. Автоматический выбор оптимального криптоалгоритма.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "Протокол ГОСТ-Шифрования:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0039A6)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val ciphers = listOf(
                                "ГОСТ Р 34.12-2015 «Кузнечик» (Высокая стойкость)",
                                "ГОСТ 28147-89 (Обратная совместимость серверов)",
                                "ГОСТ Р 34.10-2012 / 34.11-2012 (ЭЦП Единое Ядро)"
                            )

                            ciphers.forEachIndexed { idx, name ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setGostCipherSuite(idx) }
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = gostCipherSuite == idx,
                                        onClick = { viewModel.setGostCipherSuite(idx) },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0039A6))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        name, 
                                        fontSize = 12.sp, 
                                        fontWeight = if (gostCipherSuite == idx) FontWeight.Bold else FontWeight.Normal,
                                        color = if (gostCipherSuite == idx) Color(0xFF0039A6) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Switch row for exclusive Mintsifry certs validation
                            YandexSwitchRow(
                                title = "Только проверенные сертификаты Минцифры",
                                description = "Строго соединяться только с сайтами, подписанными Минцифры РФ.",
                                checked = useMintsifryCertsOnly,
                                onCheckedChange = { viewModel.toggleMintsifryCertsOnly(it) },
                                activeColor = Color(0xFF0039A6)
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "Встроенные Сертификаты Доверия (Trust Store):",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            CertificateCard(
                                name = "Национальный корневой CA Минцифры",
                                serial = "RU-MINTS-2022-MAIN-992AZ",
                                valid = "до 04.04.2042"
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            CertificateCard(
                                name = "Росреестр Суб-Сертификат",
                                serial = "GOV-RU-CORE-2024-88A",
                                valid = "до 12.08.2034"
                            )
                        }
                    }
                }

                // SECTION 5: DNS-over-HTTPS & SECURE COMPATIBILITY
                if (matchesQuery("dns", "doh", "безопасн", "реклам", "adblock", "фильтр", "угроза", "защит")) {
                    YandexSectionHeader(title = "Безопасное соединение (Protect)", icon = Icons.Default.VpnLock)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Защищенный DNS-over-HTTPS (DoH)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Шифрует доменные запросы, защищая вас от фишинговых атак и прослушивания трафика со стороны провайдера.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val dnsOpts = listOf(
                                "Системный DNS-провайдер устройства",
                                "Яндекс.DNS Безопасный (DoH https://common-dns.yandex.ru)",
                                "Суверенный DNS НИИ «Восход»"
                            )

                            dnsOpts.forEachIndexed { i, label ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setDnsType(i) }
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = dnsType == i,
                                        onClick = { viewModel.setDnsType(i) },
                                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, fontSize = 12.sp, fontWeight = if (dnsType == i) FontWeight.Bold else FontWeight.Normal)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Yandex Safe Browsing and AdBlock controls
                            YandexSwitchRow(
                                title = "Защита от угроз Yandex Safe Browsing",
                                description = "Постоянно сканирует опасные, фишинговые и вредоносные скрипты",
                                checked = isSafeBrowsingEnabled,
                                onCheckedChange = { viewModel.toggleSafeBrowsing(it) },
                                activeColor = accentColor
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            YandexSwitchRow(
                                title = "Антиреклама AdBlock (Protect)",
                                description = "Блокирует навязчивые баннеры, трекеры и рекламные ролики",
                                checked = isAdBlockEnabled,
                                onCheckedChange = { viewModel.toggleAdBlock(it) },
                                activeColor = accentColor
                            )
                        }
                    }
                }

                // SECTION 6: NOTIFICATIONS & ALERTS
                if (matchesQuery("уведомлен", "звук", "пуш", "приход", "загруз", "статус", "тест")) {
                    YandexSectionHeader(title = "Уведомления и сигналы", icon = Icons.Default.NotificationsActive)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Параметры информирования", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            var checkSound by remember { mutableStateOf(true) }
                            var checkRknAlerts by remember { mutableStateOf(true) }

                            YandexSwitchRow(
                                title = "Звуковые системные сигналы",
                                description = "Воспроизводить звуки кликов и завершения операций",
                                checked = checkSound,
                                onCheckedChange = { checkSound = it },
                                activeColor = accentColor
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            YandexSwitchRow(
                                title = "Уведомительный статус РКН",
                                description = "Сообщать на панели о заблокированных сайтах",
                                checked = checkRknAlerts,
                                onCheckedChange = { checkRknAlerts = it },
                                activeColor = accentColor
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    viewModel.showLiveNotification(
                                        title = "Канал связи PROTECT",
                                        message = "Система доставки уведомлений Яндекс Браузера работает идеально!"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_push_notif_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Icon(Icons.Default.Notifications, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Проверить отправку уведомления", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // SECTION 7: CRITICAL MANDATED PRIVACY POLICY SECTION (AT THE BOTTOM AS DEDICATED SHEET TAB)
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { showPrivacyPolicySheet = true }
                        .testTag("privacy_policy_settings_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = accentColor.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip, 
                                contentDescription = "Политика конфиденциальности", 
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Политика конфиденциальности", 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 14.sp, 
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    "Сведения о защите персональных данных", 
                                    fontSize = 11.sp, 
                                    color = Color.Gray
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ArrowForwardIos, contentDescription = "Открыть", tint = accentColor, modifier = Modifier.size(14.dp))
                    }
                }

                // Legal stamp
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Яндекс Браузер v23.5.0 • Продукт полностью русифицирован • Лицензия Минцифры РФ • Безопасный Движок Chromium v114",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // SLIDING BOTTOM SHEET DIALOG FOR THE DETAILED PRIVACY POLICY (Russian Sovereign Protect Details)
        if (showPrivacyPolicySheet) {
            ModalBottomSheet(
                onDismissRequest = { showPrivacyPolicySheet = false },
                containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser, 
                                contentDescription = "", 
                                tint = Color(0xFF16A342), 
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Политика конфиденциальности Яндекс",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                        IconButton(onClick = { showPrivacyPolicySheet = false }) {
                            Icon(Icons.Default.Close, "Закрыть")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().height(2.dp).padding(vertical = 4.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0039A6)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD52B1E)))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    PolicyChapter(
                        title = "1. Локальное хранение данных (Room Database)",
                        details = "Все собираемые данные — включая историю поисковых запросов («Табло»), закладки, список посещённых ссылок, пароли и пользовательские правила родительского контроля — сохраняются в зашифрованных файлах локальной базы данных SQLite через защищенный интерфейс Room. Никакая статистика сёрфинга не передаётся вовне без вашего прямого согласия. Все данные остаются на вашем устройстве."
                    )
                    
                    PolicyChapter(
                        title = "2. Инспектирование трафика без слежки (149-ФЗ Compliance)",
                        details = "Проверка посещаемых сайтов на предмет нахождения в реестре запрещённых ресурсов Роскомнадзора осуществляется по суверенному алгоритму локального сопоставления. Браузер кеширует хэш-сигнатуры адресов локально, что гарантирует мгновенную блокировку нежелательных сайтов согласно ФЗ-149 без отправки логов вашего личного сёрфинга за рубеж или государственным инспекторам."
                    )

                    PolicyChapter(
                        title = "3. Безопасность облачных служб (OAuth 2.0)",
                        details = "При подключении облачной синхронизации (Яндекс ID или VK ID) ваши данные резервируются на российских серверах с тройным дублированием. Процесс аутентификации работает по протоколу OAuth 2.0. Все каналы синхронизации защищены сертифицированным сквозным шифрованием (ГОСТ-шифр), исключая перехват личной информации иностранными спецслужбами."
                    )

                    PolicyChapter(
                        title = "4. Суверенное ГОСТ шифрование",
                        details = "Браузер поддерживает национальные стандарты криптографии ГОСТ Р 34.12-2015 («Кузнечик») и ГОСТ Р 34.10-2012. Это предотвращает компрометацию финансовых транзакций при работе на российских корпоративных порталах (например, Госуслуги или Сбербанк онлайн). Ваша переписка и платежные реквизиты надежно закрыты от расшифровки."
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = { showPrivacyPolicySheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ознакомлен и согласен", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun YandexSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp, top = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(Color(0xFFE52E20).copy(alpha = 0.08f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = Color(0xFFE52E20), 
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun YandexSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(description, fontSize = 11.sp, color = Color.Gray, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
fun CertificateCard(
    name: String,
    serial: String,
    valid: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.Gray.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Verified, "CA", tint = Color(0xFF16A342), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("Серийный №: $serial • $valid", fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Composable
fun PolicyChapter(
    title: String,
    details: String
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFF0039A6)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = details,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
