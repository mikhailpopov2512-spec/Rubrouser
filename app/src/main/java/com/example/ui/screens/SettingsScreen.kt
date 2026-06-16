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

    val gostCipherSuite by viewModel.selectedGostCipherSuite.collectAsState()
    val useMintsifryCertsOnly by viewModel.useMintsifryCertsOnly.collectAsState()

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
        PremiumBackdrop(isWatermark = true, alphaVal = 0.12f)

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

                // NEW SECTION: CUSTOM BLOCKLIST MANAGER
                var blocklistSearchQuery by remember { mutableStateOf("") }
                var showAddBlocklistDialog by remember { mutableStateOf(false) }
                var newBlocklistPattern by remember { mutableStateOf("") }
                var newBlocklistReason by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Редактор реестра запретов",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            IconButton(
                                onClick = { showAddBlocklistDialog = true },
                                modifier = Modifier.testTag("add_blocked_domain_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = "Добавить запрет",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Text(
                            "Добавляйте, ищите и удаляйте заблокированные доменные регулярные шаблоны из базы данных.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Realtime database filter input
                        OutlinedTextField(
                            value = blocklistSearchQuery,
                            onValueChange = { blocklistSearchQuery = it },
                            placeholder = { Text("Поиск в базе по домену...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )

                        // Show matched database records
                        val matchedBlocked = remember(blockedUrls, blocklistSearchQuery) {
                            if (blocklistSearchQuery.isBlank()) {
                                blockedUrls.take(10)
                            } else {
                                blockedUrls.filter {
                                    it.pattern.contains(blocklistSearchQuery, ignoreCase = true) ||
                                    it.reason.contains(blocklistSearchQuery, ignoreCase = true)
                                }
                            }
                        }

                        if (matchedBlocked.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "В базе нет записей, соответствующих запросу.",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())
                            ) {
                                matchedBlocked.forEach { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.pattern,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = item.reason,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteBlockedUrl(item.id) },
                                                modifier = Modifier.size(32.dp).testTag("delete_blocked_${item.pattern}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Удалить",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (blockedUrls.size > 10 && blocklistSearchQuery.isBlank()) {
                                Text(
                                    "Показано 10 из ${blockedUrls.size} правил. Воспользуйтесь поиском выше для нахождения других записей.",
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (showAddBlocklistDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showAddBlocklistDialog = false
                            newBlocklistPattern = ""
                            newBlocklistReason = ""
                        },
                        title = { Text("Внести сетевое правило", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Доменное имя или шаблон будет автоматически заблокировано во всех режимах серфинга.", fontSize = 12.sp)
                                OutlinedTextField(
                                    value = newBlocklistPattern,
                                    onValueChange = { newBlocklistPattern = it },
                                    label = { Text("Шаблон домена (напр., badsite.com)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("add_blocked_pattern_input")
                                )
                                OutlinedTextField(
                                    value = newBlocklistReason,
                                    onValueChange = { newBlocklistReason = it },
                                    label = { Text("Законодательное обоснование") },
                                    modifier = Modifier.fillMaxWidth().testTag("add_blocked_reason_input")
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newBlocklistPattern.isNotBlank()) {
                                        viewModel.addBlockedUrl(newBlocklistPattern, newBlocklistReason)
                                        showAddBlocklistDialog = false
                                        newBlocklistPattern = ""
                                        newBlocklistReason = ""
                                    }
                                },
                                modifier = Modifier.testTag("confirm_add_blocked_btn")
                            ) {
                                Text("Заблокировать")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showAddBlocklistDialog = false
                                newBlocklistPattern = ""
                                newBlocklistReason = ""
                            }) {
                                Text("Отмена")
                            }
                        }
                    )
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
                            text = "Росбраузер поддерживает отечественные стандарты шифрования ГОСТ 28147-89, ГОСТ Р 34.10-2012 и ГОСТ Р 34.11-2012 для безопасного и доверенного доступа к государственным и финансовым сервисам РФ.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactivity: Encryption Algorithm Selector
                        Text(
                            text = "Используемый протокол шифрования ГОСТ:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val ciphersList = listOf(
                            "ГОСТ Р 34.12-2015 «Кузнечик» (Высокий приоритет)",
                            "ГОСТ 28147-89 (Совместимость)",
                            "ГОСТ Р 34.10-2012 / 34.11-2012 (Классический)"
                        )

                        ciphersList.forEachIndexed { idx, title ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setGostCipherSuite(idx) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = gostCipherSuite == idx,
                                    onClick = { viewModel.setGostCipherSuite(idx) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (gostCipherSuite == idx) FontWeight.Bold else FontWeight.Normal,
                                    color = if (gostCipherSuite == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Interactivity: Exclusive Mintsifry domestic certification toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Только отечественные сертификаты Минцифры",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Строго соединяться только с узлами, подписанными Национальным удостоверяющим центром РФ. Иные доверенные узлы будут заблокированы.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(
                                checked = useMintsifryCertsOnly,
                                onCheckedChange = { viewModel.toggleMintsifryCertsOnly(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Доверенные корневые сертификаты РФ (Вшиты в Trust Store):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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

                // SECTION 6: NOTIFICATIONS & ALERTS
                SectionHeader(title = "Уведомления и оповещения", icon = Icons.Default.Notifications)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Каналы доставки уведомлений",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Настройте мгновенный приход оповещений о завершении загрузок, обновлении фильтров блокировок РКН и системном статусе.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        var ntfDownloads by remember { mutableStateOf(true) }
                        var ntfRkn by remember { mutableStateOf(true) }
                        var ntfInApp by remember { mutableStateOf(true) }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Звуковые пуш-уведомления", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Системные звуковые сигналы доставки", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = ntfDownloads,
                                onCheckedChange = { ntfDownloads = it }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Предупреждения Росбраузера", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Мгновенный показ статуса реестра РКН", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = ntfRkn,
                                onCheckedChange = { ntfRkn = it }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Всплывающие инфо-баннеры", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Интерактивный показ уведомлений в приложении", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = ntfInApp,
                                onCheckedChange = { ntfInApp = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.showLiveNotification(
                                    title = "Тест службы уведомлений РФ",
                                    message = "Канал доставки уведомлений активен! Уведомления поступают без задержек."
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("test_push_notif_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Отправить", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Проверить приход пуш-уведомления", fontSize = 13.sp)
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
