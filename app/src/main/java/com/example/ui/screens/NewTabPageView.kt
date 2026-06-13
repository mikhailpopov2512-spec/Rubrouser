package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.RussianFlagBackdrop
import com.example.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewTabPageView(
    viewModel: BrowserViewModel,
    onUrlSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe state from ViewModel
    val searchEngine by viewModel.selectedSearchEngine.collectAsState()
    val browserMode by viewModel.currentBrowserMode.collectAsState() // 0=Std, 1=Incognito, 2=Guest, 3=Child, 4=Stealth
    val isDark = com.example.ui.theme.ThemeManager.LocalDarkTheme.current

    // Banners alerts dimiss state
    var showRknBanner by remember { mutableStateOf(true) }
    var showWifiWarning by remember { mutableStateOf(true) }
    var showSyncStatus by remember { mutableStateOf(true) }

    // Widgets visibility settings
    val showWeather by viewModel.showWeatherWidget.collectAsState()
    val showTraffic by viewModel.showTrafficWidget.collectAsState()
    val showRates by viewModel.showRatesWidget.collectAsState()
    val showDzen by viewModel.showDzenWidget.collectAsState()

    var searchInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Background selection according to modes
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                when (browserMode) {
                    1 -> Color(0xFF1E1E24) // Incognito - Dark Slate Grey
                    4 -> Color.Black       // Stealth - Pitch Black
                    else -> MaterialTheme.colorScheme.background
                }
            )
            .testTag("new_tab_page")
    ) {
        // Render Russian Flag Background ONLY in Standard, Guest, and Child modes
        if (browserMode != 1 && browserMode != 4) {
            // Child mode has a bright colorful overlay (~90% white), standard/guest has ~75%
            RussianFlagBackdrop(
                alphaVal = if (browserMode == 3) 0.90f else 1.0f
            )
            
            // Centered fixed low-opacity flag watermark (10% opacity) that does not scroll
            RussianFlagBackdrop(
                isWatermark = true,
                alphaVal = 0.10f
            )
        }

        // Scrollable overlay content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // TOP HEADER BAR: Logo & Mode Selection
            TopModeHeader(
                currentMode = browserMode,
                onModeChange = { viewModel.setBrowserMode(it) }
            )

            // DYNAMIC ALERT BANNERS BLOCK
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // RKN update notification
                AnimatedVisibility(
                    visible = showRknBanner && browserMode == 0,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    AlertBanner(
                        text = "База запрещённых сайтов ФЗ-149 обновлена",
                        buttonText = "Понятно",
                        icon = Icons.Default.Shield,
                        backgroundColor = Color(0xFFFFF9E0), // light yellow
                        contentColor = Color(0xFF856404),
                        onClick = { showRknBanner = false }
                    )
                }

                // Public Unsafe Wi-Fi Alert
                AnimatedVisibility(
                    visible = showWifiWarning && browserMode != 4,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    AlertBanner(
                        text = "Подключение к открытой сети Wi-Fi небезопасно",
                        buttonText = "Включить VPN",
                        icon = Icons.Default.Warning,
                        backgroundColor = Color(0xFFFFEBEE), // light red
                        contentColor = Color(0xFFC62828),
                        onClick = {
                            showWifiWarning = false
                            // Trigger simulated VPN shield
                        }
                    )
                }

                // Cloud Sync error alert
                AnimatedVisibility(
                    visible = showSyncStatus && viewModel.syncType.value > 0 && browserMode == 0,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    AlertBanner(
                        text = "${viewModel.syncStatusMessage.value}. Безопасное облако активно.",
                        buttonText = "ОК",
                        icon = Icons.Default.Cloud,
                        backgroundColor = Color(0xFFE8F5E9), // light green
                        contentColor = Color(0xFF2E7D32),
                        onClick = { showSyncStatus = false }
                    )
                }
            }

            // MIDDLE PAGE CONTENT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Brand Title Logo
                Text(
                    text = if (browserMode == 3) "РОСБРАУЗЕР (ДЕТСКИЙ)" else "РОСБРАУЗЕР",
                    fontSize = if (browserMode == 3) 26.sp else 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = when (browserMode) {
                        1 -> Color(0xFFCBD5E1) // Incognito text
                        3 -> Color(0xFFE040FB) // Child mode playful pink text
                        4 -> Color(0xFF00FF66) // Stealth mode high safety neon-green text
                        else -> MaterialTheme.colorScheme.primary
                    },
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when (browserMode) {
                        1 -> "ИНКОГНИТО • ПРИВАТНЫЙ РЕЖИМ АКТИВЕН"
                        2 -> "ГОСТЕВОЙ СЕАНС • ИСТОРИЯ НЕ СОХРАНЯЕТСЯ"
                        3 -> "ДЕТСКИЙ ФИЛЬТР • СЕМЕЙНЫЙ ПОИСК"
                        4 -> "STEALTH SECURITY • МАКСИМАЛЬНАЯ КОНФИДЕНЦИАЛЬНОСТЬ"
                        else -> "СУВЕРЕННЫЙ ОТЕЧЕСТВЕННЫЙ БРАУЗЕР"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = when (browserMode) {
                        4 -> Color(0xFF00FF66).copy(alpha = 0.8f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // SPECIAL INCOGNITO / STEALTH EMBELLISHMENTS
                if (browserMode == 1) {
                    // Incognito visual indicator
                    Card(
                        modifier = Modifier.padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D35)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Инкогнито",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Вы в приватном режиме",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Браузер не сохраняет историю поиска, пароли и данные сайтов для этой вкладки.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else if (browserMode == 4) {
                    // Stealth mode display details
                    Card(
                        modifier = Modifier.padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                        border = BorderStroke(1.dp, Color(0xFF00FF66)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Stealth Active",
                                    tint = Color(0xFF00FF66),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "STEALTH ACTIVE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF00FF66)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Запись скриншотов заблокирована (FLAG_SECURE)\nЗащита от цифрового отпечатка включена.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // CENTRED SEARCH ENGINE OMNIBOX (Unless Stealth Mode limits its footprint)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (browserMode == 4) Color(0xFF161616) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (browserMode == 4) BorderStroke(1.dp, Color(0xFF00FF66).copy(0.4f)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (browserMode == 4) Icons.Default.SafetyCheck else Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = if (browserMode == 4) Color(0xFF00FF66) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = {
                                Text(
                                    text = "Поиск в ${if (searchEngine == 0) "Яндекс" else if (searchEngine == 1) "Mail.ru" else "Rambler"}...",
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ntp_search_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    if (searchInput.isNotBlank()) {
                                        onUrlSelected(searchInput)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        
                        // Action buttons: VOICE & QR Code Scan
                        IconButton(onClick = {
                            // Simulated scan action
                            onUrlSelected("https://rustore.ru/qr_target_mocked")
                        }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "QR Сканер",
                                tint = if (browserMode == 4) Color(0xFF00FF66) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (searchInput.isNotBlank()) {
                            IconButton(onClick = { searchInput = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Сброс",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // RENDER TABLO GRIDS OR WIDGETS
                // (Incognito / Stealth Modes have no Tablo or external widgets as defined in the spec!)
                if (browserMode != 1 && browserMode != 4) {
                    
                    // SECTION 1: TABLO SITES GRID (Favorite panel)
                    Text(
                        text = "ТАБЛО БЫСТРОГО ДОСТУПА",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start).padding(start = 6.dp, bottom = 10.dp)
                    )

                    // Default set of Russian sovereign websites combined with custom brand colors
                    // In child mode, we filter strictly to parent-approved friendly websites!
                    val sovereignTiles = listOf(
                        QuickServiceTile("Яндекс", "https://ya.ru", Icons.Default.Search, Color(0xFFD52B1E)),
                        QuickServiceTile("Госуслуги", "https://www.gosuslugi.ru", Icons.Default.AccountBalance, Color(0xFF0C5BFF)),
                        QuickServiceTile("RuStore", "https://rustore.ru", Icons.Default.Shop, Color(0xFF33BB55)),
                        QuickServiceTile("VK Новости", "https://vk.com", Icons.Default.Group, Color(0xFF0077FF)),
                        QuickServiceTile("Яндекс.Почта", "https://mail.yandex.ru", Icons.Default.Email, Color(0xFFFFB300)),
                        QuickServiceTile("Кинопоиск", "https://www.kinopoisk.ru", Icons.Default.Tv, Color(0xFFFF5722)),
                        QuickServiceTile("Яндекс.Карты", "https://yandex.ru/maps", Icons.Default.Map, Color(0xFF4CAF50)),
                        QuickServiceTile("Яндекс.Музыка", "https://music.yandex.ru", Icons.Default.MusicNote, Color(0xFFE040FB))
                    )

                    val approvedChildTiles = listOf(
                        QuickServiceTile("Детские Госуслуги", "https://www.gosuslugi.ru", Icons.Default.AccountBalance, Color(0xFF0C5BFF)),
                        QuickServiceTile("Чебурашка", "https://www.cheburashka-film.ru", Icons.Default.Face, Color(0xFFFF3D00)),
                        QuickServiceTile("Мультфильмы", "https://rutube.ru", Icons.Default.Tv, Color(0xFFE50914)),
                        QuickServiceTile("Детские игры", "https://yandex.ru/games", Icons.Default.Gamepad, Color(0xFF00E676))
                    )

                    val activeTiles = if (browserMode == 3) approvedChildTiles else sovereignTiles

                    // LazyVerticalGrid inside Column with fixed height to prevent nesting scroll issues
                    val gridHeight = if (activeTiles.size > 4) 180.dp else 90.dp
                    Box(modifier = Modifier.fillMaxWidth().height(gridHeight)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            userScrollEnabled = false
                        ) {
                            items(activeTiles) { tile ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onUrlSelected(tile.url) }
                                        .padding(top = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                if (browserMode == 3) tile.brandColor.copy(alpha = 0.25f) else Color.White,
                                                shape = if (browserMode == 3) RoundedCornerShape(14.dp) else RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                width = if (browserMode == 3) 2.dp else 1.dp,
                                                color = if (browserMode == 3) tile.brandColor else Color.LightGray.copy(alpha = 0.5f),
                                                shape = if (browserMode == 3) RoundedCornerShape(14.dp) else RoundedCornerShape(10.dp)
                                            )
                                            .testTag("tile_${tile.name.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = tile.icon,
                                            contentDescription = tile.name,
                                            tint = tile.brandColor,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = tile.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 2: LIVE METRICS SUITE WIDGETS
                    // Only show in Standard mode and when activated individually
                    if (browserMode == 0) {
                        Text(
                            text = "ОТЕЧЕСТВЕННЫЕ СЕРВИСЫ-ВИДЖЕТЫ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Start).padding(start = 6.dp, bottom = 10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Weather Widget (Яндекс.Погода)
                            if (showWeather) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Погода • Москва", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(imageVector = Icons.Default.WbSunny, contentDescription = "", tint = Color(0xFFFFB300), modifier = Modifier.size(28.dp))
                                            Text("+24°C", fontSize = 22.sp, fontWeight = FontWeight.Black)
                                        }
                                        Text("Переменная облачность", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Днём: +26° • Ночью: +17°", fontSize = 9.sp, color = Color.Gray)
                                    }
                                }
                            }

                            // Traffic Widget (Яндекс.Пробки)
                            if (showTraffic) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Пробки • Москва", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Traffic lights red/green light logic
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(Color(0xFFFFC107), CircleShape), // Yellow lights
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(imageVector = Icons.Default.Traffic, contentDescription = "", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                            Text("4 балла", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB300))
                                        }
                                        Text("Дороги свободны", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Движение стабильное", fontSize = 9.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Currencies Widget (USD / EUR / CNY)
                        if (showRates) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Курсы валют ЦБ РФ", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CurrencyItem(label = "USD", rate = "89.42 ₽", change = "-0.14", isUp = false)
                                        CurrencyItem(label = "EUR", rate = "96.15 ₽", change = "+0.28", isUp = true)
                                        CurrencyItem(label = "CNY", rate = "12.24 ₽", change = "+0.03", isUp = true)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // SECTION 3: MOCK DZEN NEWS FEED (If enabled)
                    if (showDzen && browserMode == 0) {
                        Text(
                            text = "ОТЕЧЕСТВЕННАЯ ЛЕНТА НОВОСТЕЙ • ДЗЕН",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Start).padding(start = 6.dp, bottom = 10.dp)
                        )

                        // Dzen News Feed mock items
                        listOf(
                            DzenItem(
                                title = "Отечественные IT-компании завершили локализацию 90% критического софта",
                                source = "Минцифры РФ",
                                time = "2 часа назад",
                                likes = 1251
                            ),
                            DzenItem(
                                title = "Яндекс расширяет сеть зарядных станций для электромобилей по всей России",
                                source = "Яндекс Новости",
                                time = "4 часа назад",
                                likes = 455
                            ),
                            DzenItem(
                                title = "РКН напоминает об ужесточении требований к персональным данным за границей",
                                source = "ТАСС",
                                time = "6 часов назад",
                                likes = 99
                            )
                        ).forEach { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onUrlSelected("https://yandex.ru/internet_news_mock") },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${item.source} • ${item.time}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.ThumbUp, contentDescription = "", tint = Color.Gray, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = item.likes.toString(), fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // SAFETY FOOTER LABELS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (browserMode == 4) Color(0xFF00FF66).copy(0.1f) else Color(0xFF3DDC84).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Защита",
                        tint = if (browserMode == 4) Color(0xFF00FF66) else Color(0xFF3DDC84),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (browserMode == 4) "Stealth Shield Active. Скриншоты заблокированы." else "ГОСТ TLS шифрование и защищённая навигация активны",
                        fontSize = 11.sp,
                        color = if (browserMode == 4) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun TopModeHeader(
    currentMode: Int,
    onModeChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Identity logo letter "Р" with Russian Flag color strips
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(Color(0xFF0039A6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("Р", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text("РОСБРАУЗЕР", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 0.5.sp)
        }

        // Dropdown Mode Switcher
        Box {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (currentMode) {
                        1 -> Color(0xFF475569) // Incognito
                        3 -> Color(0xFFE040FB) // Child
                        4 -> Color(0xFF00FF66) // Stealth
                        else -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (currentMode == 4) Color.Black else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(34.dp)
            ) {
                val label = when (currentMode) {
                    1 -> "Инкогнито"
                    2 -> "Гостевой"
                    3 -> "Детский"
                    4 -> "Скрытный"
                    else -> "Обычный"
                }
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "", modifier = Modifier.size(16.dp))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Обычный режим", fontSize = 13.sp) },
                    onClick = { onModeChange(0); expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Режим Инкогнито (🔒 Private)", fontSize = 13.sp) },
                    onClick = { onModeChange(1); expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Гостевой режим", fontSize = 13.sp) },
                    onClick = { onModeChange(2); expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Детский режим 🧸", fontSize = 13.sp) },
                    onClick = { onModeChange(3); expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Stealth режим (🟢 Скрытный)", fontSize = 13.sp) },
                    onClick = { onModeChange(4); expanded = false }
                )
            }
        }
    }
}

@Composable
fun AlertBanner(
    text: String,
    buttonText: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = "", tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = buttonText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun CurrencyItem(
    label: String,
    rate: String,
    change: String,
    isUp: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(rate, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = "",
                tint = if (isUp) Color(0xFF3DDC84) else Color(0xFFD52B1E),
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = change,
                fontSize = 10.sp,
                color = if (isUp) Color(0xFF3DDC84) else Color(0xFFD52B1E),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class DzenItem(
    val title: String,
    val source: String,
    val time: String,
    val likes: Int
)

data class QuickServiceTile(
    val name: String,
    val url: String,
    val icon: ImageVector,
    val brandColor: Color
)

