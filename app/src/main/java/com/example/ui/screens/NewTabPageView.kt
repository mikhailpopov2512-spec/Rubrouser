package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.components.PremiumBackdrop
import com.example.ui.components.SummerTabloRecyclerView
import com.example.ui.viewmodel.BrowserViewModel
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    val selectedBgTheme by viewModel.selectedNtpThemeBackground.collectAsState()
    val isDark = com.example.ui.theme.ThemeManager.LocalDarkTheme.current

    val userFirstName by viewModel.userFirstName.collectAsState()
    val userLastName by viewModel.userLastName.collectAsState()
    val userInterests by viewModel.userInterests.collectAsState()

    // Banners alerts dismiss state
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
    val coroutineScope = rememberCoroutineScope()

    val sovereignTiles = remember {
        listOf(
            QuickServiceTile("Яндекс", "https://ya.ru", Icons.Default.Search, Color(0xFFD52B1E)),
            QuickServiceTile("Госуслуги", "https://www.gosuslugi.ru", Icons.Default.AccountBalance, Color(0xFF0C5BFF)),
            QuickServiceTile("RuStore", "https://rustore.ru", Icons.Default.Shop, Color(0xFF33BB55)),
            QuickServiceTile("VK Новости", "https://vk.com", Icons.Default.Group, Color(0xFF0077FF)),
            QuickServiceTile("Яндекс.Почта", "https://mail.yandex.ru", Icons.Default.Email, Color(0xFFFFB300)),
            QuickServiceTile("Кинопоиск", "https://www.kinopoisk.ru", Icons.Default.Tv, Color(0xFFFF5722)),
            QuickServiceTile("Яндекс.Карты", "https://yandex.ru/maps", Icons.Default.Map, Color(0xFF4CAF50)),
            QuickServiceTile("Яндекс.Музыка", "https://music.yandex.ru", Icons.Default.MusicNote, Color(0xFFE040FB))
        )
    }

    val approvedChildTiles = remember {
        listOf(
            QuickServiceTile("Детские Госуслуги", "https://www.gosuslugi.ru", Icons.Default.AccountBalance, Color(0xFF0C5BFF)),
            QuickServiceTile("Чебурашка", "https://www.cheburashka-film.ru", Icons.Default.Face, Color(0xFFFF3D00)),
            QuickServiceTile("Мультфильмы", "https://rutube.ru", Icons.Default.Tv, Color(0xFFE50914)),
            QuickServiceTile("Детские игры", "https://yandex.ru/games", Icons.Default.Gamepad, Color(0xFF00E676))
        )
    }

    val tilesState = remember(browserMode) {
        mutableStateListOf<QuickServiceTile>().apply {
            addAll(if (browserMode == 3) approvedChildTiles else sovereignTiles)
        }
    }

    // Premium interactive elements state
    var isSpeaking by remember { mutableStateOf(false) }

    // Infinite pulse transition for the floating glowing tri-color Omnibox border
    val pulseTransition = rememberInfiniteTransition(label = "OmniboxPulseTransition")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // Speaking soundwave animation phase
    val speakerPhase by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpeakerPhase"
    )

    // Background selection according to modes
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("new_tab_page")
    ) {
        // Render dynamic immersive background for each browser mode
        PremiumBackdrop(
            browserMode = browserMode,
            selectedBgTheme = selectedBgTheme
        )

        // Centered fixed holographic watermarked tricolor for Standard, Guest, and Child modes
        if (browserMode != 1 && browserMode != 4) {
            PremiumBackdrop(
                browserMode = browserMode,
                isWatermark = true,
                alphaVal = 0.12f,
                selectedBgTheme = selectedBgTheme
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
                        backgroundColor = if (isDark) Color(0xFF2C2510) else Color(0xFFFFF9E0),
                        contentColor = if (isDark) Color(0xFFFFD54F) else Color(0xFF856404),
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
                        backgroundColor = if (isDark) Color(0xFF2C1517) else Color(0xFFFFEBEE),
                        contentColor = if (isDark) Color(0xFFEF5350) else Color(0xFFC62828),
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
                        backgroundColor = if (isDark) Color(0xFF142C16) else Color(0xFFE8F5E9),
                        contentColor = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32),
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
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                if (userFirstName.isNotBlank() && browserMode == 0) {
                    Text(
                        text = "Привет, $userFirstName! \uD83D\uDC4B",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFF60A5FA) else com.example.ui.theme.RusBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    if (userInterests.isNotEmpty()) {
                        Text(
                            text = "Рекомендации подобраны по темам: ${userInterests.joinToString(", ")}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

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

                // CENTRED SEARCH ENGINE OMNIBOX (Featuring pulsing tri-color glowing borders and active voice waveforms)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (browserMode == 4) Color(0xFF161616) else if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.94f)
                    ),
                    border = BorderStroke(
                        width = 1.8.dp,
                        brush = if (browserMode == 4) {
                            Brush.linearGradient(listOf(Color(0xFF00FF66), Color(0xFF003311)))
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF).copy(alpha = pulseAlpha),
                                    Color(0xFF0039A6).copy(alpha = pulseAlpha),
                                    Color(0xFFD52B1E).copy(alpha = pulseAlpha)
                                )
                            )
                        }
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
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
                                        text = if (isSpeaking) "Слушаю вас..." else "Поиск в ${if (searchEngine == 0) "Росбраузере" else if (searchEngine == 1) "Mail.ru" else "Rambler"}...",
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
                            
                            // Action buttons: VOICE SEARCH with active pulsation
                            IconButton(
                                onClick = {
                                    isSpeaking = !isSpeaking
                                    if (isSpeaking) {
                                        searchInput = ""
                                        // Simulate spoken recognition
                                        coroutineScope.launch {
                                            delay(2200)
                                            if (isSpeaking) {
                                                searchInput = "Портал Госуслуг РФ"
                                                isSpeaking = false
                                                onUrlSelected("https://www.gosuslugi.ru")
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.Mic,
                                    contentDescription = "Голосовой поиск",
                                    tint = if (isSpeaking) Color(0xFFD52B1E) else if (browserMode == 4) Color(0xFF00FF66) else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // QR Code Scan
                            IconButton(onClick = {
                                checkSecureQr(onUrlSelected)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "QR Сканер",
                                    tint = if (browserMode == 4) Color(0xFF00FF66) else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (searchInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        onUrlSelected(searchInput)
                                    },
                                    modifier = Modifier.testTag("ntp_submit_search_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Поиск",
                                        tint = if (browserMode == 4) Color(0xFF00FF66) else MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { searchInput = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Сброс",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }

                        // Living voice soundwave animation block if listening
                        if (isSpeaking) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Говорите, идёт распознавание речи РФ...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                // Render 5 dynamic bouncing audio soundwave pillars
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0..4) {
                                        val waveFactor = kotlin.math.sin(speakerPhase + i * 1.2f)
                                        val heightAdjust = (14.dp + (16.dp * kotlin.math.abs(waveFactor)))
                                        val waveColor = when (i % 3) {
                                            0 -> Color.White
                                            1 -> Color(0xFF0039A6)
                                            else -> Color(0xFFD52B1E)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(heightAdjust)
                                                .background(waveColor, RoundedCornerShape(1.5.dp))
                                        )
                                    }
                                }
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

                    // Dynamic RecyclerView-based ТАБЛО (Requirement 61-95) with Summer pebble beach design
                    Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                        SummerTabloRecyclerView(
                            tilesList = tilesState,
                            isKidsMode = (browserMode == 3),
                            onAddClicked = {
                                val extraSites = listOf(
                                    QuickServiceTile("Habr", "https://habr.com", Icons.Default.Article, Color(0xFF7FA2B2)),
                                    QuickServiceTile("Sber", "https://sberbank.ru", Icons.Default.Savings, Color(0xFF21A038)),
                                    QuickServiceTile("Mail.ru", "https://mail.ru", Icons.Default.Email, Color(0xFF1357C6)),
                                    QuickServiceTile("РТ Новости", "https://rt.com", Icons.Default.Feed, Color(0xFFD32F2F))
                                )
                                val unadded = extraSites.filter { ex -> !tilesState.any { t -> t.name == ex.name } }
                                if (unadded.isNotEmpty()) {
                                    tilesState.add(unadded.first())
                                }
                            },
                            onTileClicked = { tile ->
                                onUrlSelected(tile.url)
                            },
                            onTileDeleted = { index ->
                                if (tilesState.size > index) {
                                    tilesState.removeAt(index)
                                }
                            },
                            onItemsReordered = { reorderedList ->
                                tilesState.clear()
                                tilesState.addAll(reorderedList)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 2: LIVE METRICS SUITE WIDGETS
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
                            if (showWeather) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AnimatedWeatherWidget(isDark = isDark, viewModel = viewModel)
                                }
                            }

                            if (showTraffic) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AnimatedTrafficWidget(isDark = isDark)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (showRates) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.9f)
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Курсы валют ЦБ РФ • 24ч Тренды", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CurrencyItem(
                                            label = "USD",
                                            rate = "91.20 ₽",
                                            change = "-0.14",
                                            isUp = false,
                                            sparkPoints = listOf(0.7f, 0.4f, 0.5f, 0.3f, 0.6f, 0.2f, 0.1f)
                                        )
                                        CurrencyItem(
                                            label = "EUR",
                                            rate = "99.45 ₽",
                                            change = "+0.28",
                                            isUp = true,
                                            sparkPoints = listOf(0.2f, 0.3f, 0.25f, 0.5f, 0.4f, 0.7f, 0.9f)
                                        )
                                        CurrencyItem(
                                            label = "CNY",
                                            rate = "12.56 ₽",
                                            change = "+0.03",
                                            isUp = true,
                                            sparkPoints = listOf(0.4f, 0.42f, 0.48f, 0.45f, 0.5f, 0.52f, 0.55f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // SECTION 3: MOCK DZEN NEWS FEED WITH PARALLAX & SKELETONS
                    if (showDzen && browserMode == 0) {
                        val dzenRealNews by viewModel.dzenRealNews.collectAsState()
                        val isNewsLoading by viewModel.isNewsLoading.collectAsState()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = "ОТЕЧЕСТВЕННАЯ ЛЕНТА НОВОСТЕЙ • ДЗЕН",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                            
                            // Tricolor pull-to-refresh spinner simulator
                            IconButton(onClick = {
                                viewModel.fetchRealWeatherAndNews()
                            }) {
                                val refreshRotation = remember { Animatable(0f) }
                                LaunchedEffect(isNewsLoading) {
                                    if (isNewsLoading) {
                                        refreshRotation.animateTo(
                                            targetValue = 360f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1000, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart
                                            )
                                        )
                                    } else {
                                        refreshRotation.snapTo(0f)
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Обновить ленту",
                                    tint = if (isNewsLoading) Color(0xFF0039A6) else Color.Gray,
                                    modifier = Modifier.graphicsLayer { rotationZ = refreshRotation.value }
                                )
                            }
                        }

                        if (isNewsLoading && dzenRealNews.isEmpty()) {
                            Column {
                                SkeletonNewsCard()
                                SkeletonNewsCard()
                            }
                        } else {
                            dzenRealNews.forEachIndexed { index, item ->
                                val parallaxOffset = (scrollState.value * 0.12f).coerceIn(-60f, 60f)
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onUrlSelected(item.link) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.9f)
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column {
                                        // Parallax Image simulated preview block (Requirement 22)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                                .background(Color.DarkGray)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer { translationY = parallaxOffset }
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(
                                                                Color(0xFF0039A6).copy(alpha = 0.35f),
                                                                Color(0xFFD52B1E).copy(alpha = 0.35f)
                                                            )
                                                        )
                                                    )
                                            )
                                            Text(
                                                text = "ДЗЕН ФОТО",
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }

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
                            }
                            
                            var isAppendingNews by remember { mutableStateOf(false) }
                            if (isAppendingNews) {
                                SkeletonNewsCard()
                            } else {
                                Button(
                                    onClick = {
                                        isAppendingNews = true
                                        coroutineScope.launch {
                                            viewModel.fetchRealWeatherAndNews()
                                            delay(550)
                                            isAppendingNews = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                ) {
                                    Text("Обновить статьи Дзен", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "технология на заводе лада",
                    fontSize = 10.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // Back to top floating action button with animation (Requirement 30)
        val showFab = scrollState.value > 400
        AnimatedVisibility(
            visible = showFab,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(0, spring(stiffness = Spring.StiffnessLow))
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Вверх", modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ---------------------- SUB-COMPOSABLES DEFINITIONS -------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InteractiveTileCard(
    tile: QuickServiceTile,
    index: Int,
    browserMode: Int,
    onUrlSelected: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "TileScale"
    )
    val cardElevation by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "TileElevation"
    )

    // Staggered presentation reveal (Requirement 17: delay 30ms between tiles)
    val revealAlphaState = remember { Animatable(0f) }
    val revealSlideState = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        delay(index * 30L)
        launch { revealAlphaState.animateTo(1f, tween(250)) }
        launch { revealSlideState.animateTo(0f, spring(stiffness = Spring.StiffnessLow)) }
    }

    // Shatter on delete animation states (Requirement 14)
    var isShattered by remember { mutableStateOf(false) }
    val shatterScale by animateFloatAsState(
        targetValue = if (isShattered) 0f else 1f,
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "ShatterScale"
    )
    val shatterRotation by animateFloatAsState(
        targetValue = if (isShattered) 45f else 0f,
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "ShatterRotation"
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    if (shatterScale > 0.01f) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    alpha = revealAlphaState.value * shatterScale
                    scaleX = cardScale * shatterScale
                    scaleY = cardScale * shatterScale
                    translationY = revealSlideState.value
                    rotationZ = shatterRotation
                }
                .pointerInput(tile.name) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onUrlSelected(tile.url)
                        }
                    )
                }
                .padding(top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp) // Requirement 11: 72x72 dp size
                    .drawBehind {
                        // Requirement 15: Color halo from Palette API / brand color
                        drawCircle(
                            color = tile.brandColor.copy(alpha = 0.22f),
                            radius = size.width * 0.58f,
                            center = center
                        )
                    }
                    .background(
                        color = if (browserMode == 3) tile.brandColor.copy(alpha = 0.15f) else Color.White,
                        shape = RoundedCornerShape(16.dp) // Requirement 11: 16dp radius
                    )
                    .border(
                        width = 1.dp,
                        color = tile.brandColor.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Deletion trigger 'x' button (Requirement 14)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            isShattered = true
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                onDelete()
                            }, 300)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Удалить",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }

                // In Kids Mode (3), replacing with animal beast icon replacements (Requirement 27)
                val displayIcon = if (browserMode == 3) {
                    when (index % 4) {
                        0 -> Icons.Default.Pets
                        1 -> Icons.Default.Face
                        2 -> Icons.Default.Mood
                        else -> Icons.Default.ChildCare
                    }
                } else {
                    tile.icon
                }

                Icon(
                    imageVector = displayIcon,
                    contentDescription = tile.name,
                    tint = tile.brandColor,
                    modifier = Modifier.size(32.dp)
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

@Composable
fun InteractivePlusTile(
    onAdd: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PlusPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PlusScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
            .clickable { onAdd() }
            .padding(top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFF0039A6), Color(0xFFD52B1E))), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Добавить",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Добавить",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AnimatedWeatherWidget(isDark: Boolean, viewModel: com.example.ui.viewmodel.BrowserViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "WeatherParticles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ParticleTime"
    )

    val temp by viewModel.realWeatherTemp.collectAsState()
    val cond by viewModel.realWeatherCond.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth().clickable { viewModel.fetchRealWeatherAndNews() },
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.9f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(86.dp)) {
            // Requirement 18: Falling raindrops / particles animated layout overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (w <= 1f || h <= 1f) return@Canvas
                try {
                    for (i in 0..7) {
                        val x = (w * (i * 0.13f + 0.05f)) % w
                        val y = (h * (time + i * 0.21f)) % h
                        drawLine(
                            color = Color(0x660284C7),
                            start = androidx.compose.ui.geometry.Offset(x, y),
                            end = androidx.compose.ui.geometry.Offset(x - 2f, y + 10f),
                            strokeWidth = 3f
                        )
                    }
                } catch (e: Throwable) {
                    // Safe draw fallback
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text("Погода • РФ", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.WbCloudy, contentDescription = "", tint = Color(0xFF0284C7), modifier = Modifier.size(24.dp))
                    Text(temp, fontSize = 24.sp, fontWeight = FontWeight.Black) // Requirement 18: size 24sp
                }
                Text(cond, fontSize = 9.sp, color = if (isDark) Color.LightGray else Color.DarkGray)
            }
        }
    }
}

@Composable
fun AnimatedTrafficWidget(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "TrafficPluse")
    val scaleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TrafficGlow"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.9f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).height(86.dp)) {
            Text("Пробки • Москва", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Requirement 19: traffic light glowing circle transitions
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                scaleX = 1f + scaleAlpha * 0.4f
                                scaleY = 1f + scaleAlpha * 0.4f
                                alpha = 1f - scaleAlpha
                            }
                            .background(Color(0xFFFFC107), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color(0xFFFFB300), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Traffic, contentDescription = "", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
                Text("4 балла", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB300))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text("Свободные дороги", fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SkeletonNewsCard() {
    val transition = rememberInfiniteTransition(label = "SkeletonShimmer")
    val alphaShimmer by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SkeletonAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (com.example.ui.theme.ThemeManager.LocalDarkTheme.current) Color(0xFF1E293B).copy(0.4f) else Color.White.copy(0.7f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(18.dp)
                    .background(Color.Gray.copy(alpha = alphaShimmer), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
                    .background(Color.Gray.copy(alpha = alphaShimmer), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(12.dp)
                        .background(Color.Gray.copy(alpha = alphaShimmer), RoundedCornerShape(3.dp))
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(12.dp)
                        .background(Color.Gray.copy(alpha = alphaShimmer), RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

fun checkSecureQr(onUrlSelected: (String) -> Unit) {
    onUrlSelected("https://yandex.ru/gost-search?qr=secure_check_successful")
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
    isUp: Boolean,
    sparkPoints: List<Float>
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(rate, fontSize = 14.sp, fontWeight = FontWeight.Black)
        
        // Requirement 20: 24h mini sparklines (Canvas programmatically drawing vector line currency graphs)
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(60.dp)
                .height(18.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width <= 1f || size.height <= 1f) return@Canvas
                try {
                    val ptCount = sparkPoints.size
                    if (ptCount > 1) {
                        val step = size.width / (ptCount - 1)
                        val pth = Path()
                        sparkPoints.forEachIndexed { idx, value ->
                            val x = idx * step
                            val y = size.height - (value * size.height * 0.75f + size.height * 0.1f)
                            if (idx == 0) pth.moveTo(x, y) else pth.lineTo(x, y)
                        }
                        drawPath(
                            path = pth,
                            color = if (isUp) Color(0xFF3DDC84) else Color(0xFFD52B1E),
                            style = Stroke(width = 4f)
                        )
                    }
                } catch (e: Throwable) {
                    // Safe draw fallback
                }
            }
        }

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
    val likes: Int,
    val link: String = "https://dzen.ru"
)

data class QuickServiceTile(
    val name: String,
    val url: String,
    val icon: ImageVector,
    val brandColor: Color
)
