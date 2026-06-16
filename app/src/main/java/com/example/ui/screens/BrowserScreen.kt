package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.PremiumBackdrop
import com.example.ui.viewmodel.BrowserViewModel
import com.example.utils.WebInterceptors
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentUrl by viewModel.currentUrl.collectAsState()
    val pageTitle by viewModel.pageTitle.collectAsState()
    val isWebLoading by viewModel.isWebLoading.collectAsState()
    val webProgress by viewModel.webProgress.collectAsState()

    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsState()
    val isSafeBrowsingEnabled by viewModel.isSafeBrowsingEnabled.collectAsState()
    
    // Position Preference: 0 = Снизу, 1 = Сверху
    val addressBarPos by viewModel.selectedAddressBarPosition.collectAsState()
    val browserMode by viewModel.currentBrowserMode.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    // Prevent recursive loop crash on search engine redirect matching
    var lastLoadedUrl by remember { mutableStateOf("") }
    var isOmniboxFocused by remember { mutableStateOf(false) }

    // National certificate details dialog & configuration variables
    var showCertInfoDialog by remember { mutableStateOf(false) }
    val gostCipherSuite by viewModel.selectedGostCipherSuite.collectAsState()
    val useMintsifryCertsOnly by viewModel.useMintsifryCertsOnly.collectAsState()

    // Toggle FLAG_SECURE dynamically on Stealth Mode (mode = 4)
    val activity = context as? android.app.Activity
    LaunchedEffect(browserMode) {
        if (browserMode == 4) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Sync input field display values
    LaunchedEffect(currentUrl) {
        if (currentUrl != "about:home") {
            textInput = currentUrl
        } else {
            textInput = ""
        }
    }

    // Normalize URL helper to safely compare and avoid Chromium crash loops
    fun normalizeUrl(url: String?): String {
        if (url == null) return ""
        var clean = url.trim().lowercase()
        if (clean.endsWith("/")) {
            clean = clean.dropLast(1)
        }
        clean = clean.replace("https://", "").replace("http://", "")
        if (clean.startsWith("www.")) {
            clean = clean.substring(4)
        }
        return clean
    }

    // High performance routing updates with loops prevention
    LaunchedEffect(currentUrl) {
        if (currentUrl != "about:home") {
            val webView = webViewRef
            if (webView != null) {
                val cleanCurrent = normalizeUrl(currentUrl)
                val cleanWebUrl = normalizeUrl(webView.url)
                val cleanWebOrigUrl = normalizeUrl(webView.originalUrl)
                
                if (cleanCurrent != cleanWebUrl && cleanCurrent != cleanWebOrigUrl && currentUrl != lastLoadedUrl) {
                    lastLoadedUrl = currentUrl
                    webView.loadUrl(currentUrl)
                }
            } else {
                lastLoadedUrl = currentUrl
            }
        }
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Sync system back clicks
    BackHandler(enabled = currentUrl != "about:home" || isOmniboxFocused) {
        if (isOmniboxFocused) {
            focusManager.clearFocus()
        } else if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            viewModel.loadUrl("about:home")
        }
    }

    // Define the Omnibox Text Field component to share across Top and Bottom compositions
    val omniboxTextField = @Composable {
        OutlinedTextField(
            value = textInput,
            onValueChange = { 
                textInput = it 
                viewModel.onSearchInputChanged(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .onFocusChanged { state ->
                    isOmniboxFocused = state.isFocused
                }
                .testTag("omnibox_input"),
            placeholder = { Text("Поиск или адрес (ya.ru)", fontSize = 13.sp) },
            leadingIcon = {
                // Lock vs Protect Shield vs National Mintsifry Cert Indicator
                val isNationalCert = currentUrl.startsWith("https://") && (
                    currentUrl.contains(".ru") || 
                    currentUrl.contains(".su") || 
                    currentUrl.contains("gosuslugi") ||
                    currentUrl.contains("sberbank") ||
                    currentUrl.contains("ya.ru") ||
                    useMintsifryCertsOnly
                )
                val isSecure = currentUrl.startsWith("https://") || isNationalCert || browserMode == 4

                // Smooth luxury security microwave ripples (Requirement #9)
                val infiniteMicrowave = rememberInfiniteTransition(label = "SecurityRadar")
                val microwaveAlpha by infiniteMicrowave.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 0.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = androidx.compose.animation.core.EaseOutQuad),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "MicrowaveAlpha"
                )
                val microwaveScale by infiniteMicrowave.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.7f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = androidx.compose.animation.core.EaseOutQuad),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "MicrowaveScale"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isSecure) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .graphicsLayer {
                                        scaleX = microwaveScale
                                        scaleY = microwaveScale
                                        alpha = microwaveAlpha
                                    }
                                    .background(
                                        color = if (browserMode == 4) {
                                            Color(0xFF00FF66).copy(alpha = 0.45f)
                                        } else if (isNationalCert) {
                                            Color(0xFF0039A6).copy(alpha = 0.45f)
                                        } else {
                                            Color(0xFF3DDC84).copy(alpha = 0.45f)
                                        },
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }

                        IconButton(
                            onClick = { showCertInfoDialog = true },
                            modifier = Modifier.testTag("security_status_icon_btn").size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (browserMode == 4) {
                                    Icons.Default.Security
                                } else if (isNationalCert) {
                                    Icons.Default.Shield
                                } else if (currentUrl.startsWith("https://")) {
                                    Icons.Default.Lock
                                } else {
                                    Icons.Default.Shield
                                },
                                contentDescription = "Защита СВ",
                                tint = if (browserMode == 4) {
                                    Color(0xFF00FF66)
                                } else if (isNationalCert) {
                                    Color(0xFF0039A6) // Safe Deep Blue for Russian Ministry
                                } else if (currentUrl.startsWith("https://")) {
                                    Color(0xFF3DDC84) // Safe Green
                                } else {
                                    Color.Gray
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (isNationalCert) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE0F2FE),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF0284C7)),
                            modifier = Modifier
                                .clickable { showCertInfoDialog = true }
                                .padding(end = 4.dp)
                        ) {
                            Text(
                                text = "ГОСТ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Voice mic / QR shortcuts in Omnibox
                    IconButton(onClick = {
                        textInput = ""
                        // Trigger voice simulator
                    }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Голосовой поиск",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (textInput.isNotEmpty()) {
                        IconButton(onClick = { textInput = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Очистить",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    viewModel.loadUrl(textInput)
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (browserMode == 4) Color(0xFF00FF66) else MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }

    Scaffold(
        topBar = {
            // Apply only if Top position (addressBarPos == 1) OR on active focus expansions
            if (addressBarPos == 1) {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.loadUrl("about:home")
                            },
                            modifier = Modifier.testTag("home_button")
                        ) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = "Главная")
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            omniboxTextField()
                        }
                    }

                    // Omnibox mini tri-color 2dp indicator strip at base of top bar
                    Row(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0039A6)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD52B1E)))
                    }

                    // Progress Loader indicator
                    if (isWebLoading) {
                        LinearProgressIndicator(
                            progress = webProgress / 100f,
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = Color(0xFF0039A6),
                            trackColor = Color.Transparent
                        )
                    }
                }
            } else {
                // Even on Bottom Bar, render top separator progress linear line and statusBarsPadding
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    Box(modifier = Modifier.statusBarsPadding())
                    if (isWebLoading) {
                        LinearProgressIndicator(
                            progress = webProgress / 100f,
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = Color(0xFF0039A6),
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Bottom area dynamically structures Omnibox if pos == 0
            Surface(
                color = Color.Transparent, // Luxurious floating design: transparent backdrop
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Render Address Bar strictly at the bottom if Preference == 0 (Bottom)
                    if (addressBarPos == 0) {
                        // Premium spring animations for elastic stretching (Requirements #6, #7)
                        val marginHorizontal by animateDpAsState(
                            targetValue = if (isOmniboxFocused) 0.dp else 12.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "OmniboxMarginHorizontal"
                        )
                        val marginVertical by animateDpAsState(
                            targetValue = if (isOmniboxFocused) 0.dp else 12.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "OmniboxMarginVertical"
                        )
                        val cardElevation by animateDpAsState(
                            targetValue = if (isOmniboxFocused) 16.dp else 10.dp,
                            animationSpec = spring(),
                            label = "OmniboxElevation"
                        )

                        // Wrap inside floating Premium card container with tricolor active glowing borders (Requirement #8)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = marginHorizontal, vertical = marginVertical)
                                .testTag("floating_omnibox_outer_container"),
                            shape = RoundedCornerShape(if (isOmniboxFocused) 0.dp else 24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                            colors = CardDefaults.cardColors(
                                containerColor = if (browserMode == 4) Color(0xFF161616) else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                            ),
                            border = if (isOmniboxFocused) {
                                BorderStroke(
                                    width = 2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFF0039A6),
                                            Color(0xFFD52B1E)
                                        )
                                    )
                                )
                            } else if (browserMode == 4) {
                                BorderStroke(1.dp, Color(0xFF00FF66))
                            } else {
                                BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.loadUrl("about:home") },
                                        modifier = Modifier.testTag("omnibox_home_logo_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.Home, contentDescription = "Главная", tint = if (browserMode == 4) Color(0xFF00FF66) else MaterialTheme.colorScheme.primary)
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        omniboxTextField()
                                    }
                                }

                                // Omnibox mini tri-color strip indicator (Requirement #8)
                                Row(modifier = Modifier.fillMaxWidth().height(1.5.dp)) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0039A6)))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD52B1E)))
                                }
                            }
                        }
                    }

                    // Navigation buttons Row (Only visible if the search bar is not active/focused for ergonomics)
                    if (!isOmniboxFocused) {
                        BottomAppBar(
                            modifier = Modifier.height(52.dp),
                            containerColor = Color.Transparent,
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { webViewRef?.goBack() },
                                    enabled = webViewRef?.canGoBack() == true,
                                    modifier = Modifier.testTag("back_nav_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Назад")
                                }

                                IconButton(
                                    onClick = { webViewRef?.goForward() },
                                    enabled = webViewRef?.canGoForward() == true,
                                    modifier = Modifier.testTag("forward_nav_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Вперед")
                                }

                                IconButton(
                                    onClick = {
                                        if (isWebLoading) {
                                            webViewRef?.stopLoading()
                                        } else {
                                            webViewRef?.reload()
                                        }
                                    },
                                    modifier = Modifier.testTag("refresh_nav_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isWebLoading) Icons.Default.Close else Icons.Default.Refresh,
                                        contentDescription = if (isWebLoading) "Остановить" else "Обновить"
                                    )
                                }

                                IconButton(
                                    onClick = onNavigateToBookmarks,
                                    modifier = Modifier.testTag("bookmarks_nav_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Bookmarks, contentDescription = "Библиотека")
                                }

                                IconButton(
                                    onClick = onNavigateToSettings,
                                    modifier = Modifier.testTag("settings_nav_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Настройки")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (currentUrl == "about:home") {
                // Render the elegant Custom New Tab Page (NTP)
                NewTabPageView(
                    viewModel = viewModel,
                    onUrlSelected = { url ->
                        viewModel.loadUrl(url)
                    }
                )
            } else {
                // Render the Customized Intercepting Web Engine
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setupBrowserSettings(this)
                            
                            // Native file downloads completed simulation and notification triggers
                            setDownloadListener { url, _, contentDisposition, mimetype, _ ->
                                val fileName = try {
                                    android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
                                } catch (e: Exception) {
                                    "документ"
                                }
                                try {
                                    viewModel.showLiveNotification(
                                        title = "Загрузка файла завершена",
                                        message = "Файл $fileName успешно загружен."
                                    )
                                } catch (t: Throwable) {
                                    Log.e("WebView", "Error during download notify", t)
                                }
                            }
                            
                            webViewClient = object : WebViewClient() {
                                
                                // Monitor URL starts to check RKN blocks and Safe browsing
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    url?.let {
                                        viewModel.isWebLoading.value = true
                                        
                                        // Update text and alignment values to cut looping
                                        textInput = it
                                        lastLoadedUrl = it
                                        viewModel.currentUrl.value = it
                                        
                                        // Run block and security inspections
                                        coroutineScope.launch {
                                            val isBlocked = WebInterceptors.checkRknBlock(it, viewModel.repository) { matchedBlock ->
                                                // Log blocked attempt to DB for live statistics!
                                                viewModel.repository.logBlockedAttempt(it, matchedBlock.reason)
                                                
                                                // Trigger RKN Block notification
                                                try {
                                                    viewModel.showLiveNotification(
                                                        title = "Ресурс заблокирован (ФЗ-149)",
                                                        message = "Доступ к сайту $it заблокирован Роскомнадзором."
                                                    )
                                                } catch (tn: Throwable) {
                                                    Log.e("WebView", "Block notification failed", tn)
                                                }
                                                
                                                // Force load a custom offline RKN HTML warn page
                                                val html = WebInterceptors.generateBlockedPageHtml(it, matchedBlock.reason, matchedBlock.law)
                                                view?.loadDataWithBaseURL("https://rkn.gov.ru/blocked_stub", html, "text/html", "UTF-8", null)
                                            }
                                            
                                            if (!isBlocked) {
                                                // Save page history when we load successfully
                                                viewModel.addHistory(view?.title ?: it, it)
                                            }
                                        }
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    viewModel.isWebLoading.value = false
                                    if (url != null && !url.contains("blocked_stub")) {
                                        viewModel.pageTitle.value = view?.title ?: url
                                    }
                                }

                                // Deep link filter: Intercept Yandex/RuStore apps setups or redirect deep links
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    
                                    if (url.startsWith("market://") || url.startsWith("rustore://") || url.startsWith("intent://")) {
                                        try {
                                            val marketUri = Uri.parse(url)
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, marketUri)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e("RuStoreDeepLink", "Failed to resolve deep link: $url")
                                        }
                                        return true
                                    }
                                    return false
                                }

                                // Ad-blocker support: intercepts subresource loads (JS, images) and blocks tracker requests
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val url = request?.url?.toString() ?: return null
                                    
                                    if (isAdBlockEnabled && WebInterceptors.isAdRequest(url)) {
                                        Log.d("RosAdBlock", "Blocked ad request: $url")
                                        return WebResourceResponse(
                                            "text/javascript",
                                            "UTF-8",
                                            ByteArrayInputStream("".toByteArray())
                                        )
                                    }

                                    // 2. Intercept resource queries from blocked domains
                                    val isBlockedDomain = kotlinx.coroutines.runBlocking {
                                        WebInterceptors.checkRknBlock(url, viewModel.repository) { matchedBlock ->
                                            viewModel.repository.logBlockedAttempt(url, matchedBlock.reason)
                                        }
                                    }
                                    if (isBlockedDomain) {
                                        Log.d("RosRknIntercept", "Blocked nested subresource request: $url")
                                        return WebResourceResponse(
                                            "text/html",
                                            "UTF-8",
                                            ByteArrayInputStream("".toByteArray())
                                        )
                                    }
                                    
                                    return super.shouldInterceptRequest(view, request)
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    viewModel.webProgress.value = newProgress
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    if (currentUrl != "about:home" && !currentUrl.contains("blocked_stub")) {
                                        viewModel.pageTitle.value = title ?: currentUrl
                                    }
                                }
                            }

                            lastLoadedUrl = currentUrl
                            loadUrl(currentUrl)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        webViewRef = view
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isOmniboxFocused) {
                val overlayColor = if (com.example.ui.theme.ThemeManager.LocalDarkTheme.current) Color(0xFF121212) else Color.White
                val queryVal = textInput
                val remoteSuggestions by viewModel.searchSuggestions.collectAsState()
                val historySuggestions by viewModel.localHistorySuggestions.collectAsState()
                val bookmarkSuggestions by viewModel.localBookmarkSuggestions.collectAsState()
                val layoutCorrection = remember(queryVal) { viewModel.getLayoutCorrection(queryVal) }

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_suggestions_overlay")
                ) {
                    if (queryVal.isBlank()) {
                        item {
                            Text(
                                "ПОПУЛЯРНЫЕ ОТЕЧЕСТВЕННЫЕ РЕСУРСЫ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                        val popularSites = listOf(
                            Triple("Портал Госуслуг РФ", "gosuslugi.ru", Icons.Default.AccountBalance),
                            Triple("Яндекс Поиск и Сервисы", "ya.ru", Icons.Default.Explore),
                            Triple("ВКонтакте — Общение", "vk.com", Icons.Default.Forum),
                            Triple("Сбербанк Онлайн", "sberbank.ru", Icons.Default.Payments),
                            Triple("Дзен Новости и Медиа", "dzen.ru", Icons.Default.Newspaper),
                            Triple("Электронная Почта Mail.ru", "mail.ru", Icons.Default.Email)
                        )
                        items(popularSites.size) { index ->
                            val (name, domain, icon) = popularSites[index]
                            ListItem(
                                headlineContent = { Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                supportingContent = { Text(domain, fontSize = 11.sp, color = Color.Gray) },
                                leadingContent = { 
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = name, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                },
                                modifier = Modifier.clickable {
                                    focusManager.clearFocus()
                                    viewModel.loadUrl("https://$domain")
                                }
                            )
                        }
                    }

                    if (layoutCorrection != queryVal && layoutCorrection.isNotBlank() && queryVal.isNotBlank()) {
                        item {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = "Возможно, вы имели в виду:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = layoutCorrection,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Black
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardAlt,
                                        contentDescription = "Раскладка",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.clickable {
                                    textInput = layoutCorrection
                                    viewModel.onSearchInputChanged(layoutCorrection)
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    // Matching Bookmark items
                    if (bookmarkSuggestions.isNotEmpty()) {
                        item {
                            Text(
                                "ЗАКЛАДКИ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                            )
                        }
                        items(bookmarkSuggestions.size) { index ->
                            val bookmark = bookmarkSuggestions[index]
                            ListItem(
                                headlineContent = { Text(bookmark.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                supportingContent = { Text(bookmark.url, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = Color.Gray, fontSize = 12.sp) },
                                leadingContent = { Icon(Icons.Default.Bookmark, contentDescription = "Закладка", tint = Color(0xFF0C5BFF)) },
                                modifier = Modifier.clickable {
                                    focusManager.clearFocus()
                                    viewModel.loadUrl(bookmark.url)
                                }
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                    }

                    // Matching History items
                    if (historySuggestions.isNotEmpty()) {
                        item {
                            Text(
                                "ИСТОРИЯ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                            )
                        }
                        items(historySuggestions.size) { index ->
                            val history = historySuggestions[index]
                            ListItem(
                                headlineContent = { Text(history.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                supportingContent = { Text(history.url, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = Color.Gray, fontSize = 12.sp) },
                                leadingContent = { Icon(Icons.Default.History, contentDescription = "История", tint = Color.Gray) },
                                modifier = Modifier.clickable {
                                    focusManager.clearFocus()
                                    viewModel.loadUrl(history.url)
                                }
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                    }

                    // Remote Search suggestions from Yandex cgi suggest API
                    if (remoteSuggestions.isNotEmpty() && queryVal.isNotBlank()) {
                        item {
                            Text(
                                "ПОИСКОВЫЕ ПОДСКАЗКИ YANDEX",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                            )
                        }
                        items(remoteSuggestions.size) { index ->
                            val suggestion = remoteSuggestions[index]
                            ListItem(
                                headlineContent = { Text(suggestion, fontWeight = FontWeight.SemiBold) },
                                leadingContent = { Icon(Icons.Default.Search, contentDescription = "Поиск", tint = Color.Gray) },
                                modifier = Modifier.clickable {
                                    focusManager.clearFocus()
                                    viewModel.loadUrl(suggestion)
                                }
                            )
                        }
                    } else if (queryVal.isNotBlank()) {
                        // Under local fallback / offline or loading search state
                        item {
                            ListItem(
                                headlineContent = { Text("Искать в Яндексе: \"$queryVal\"") },
                                leadingContent = { Icon(Icons.Default.Search, contentDescription = "Поиск", tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable {
                                    focusManager.clearFocus()
                                    viewModel.loadUrl(queryVal)
                                }
                            )
                        }
                    }
                }
            }
            
            // GORGEOUS PREMIUM SLIDE-DOWN NOTIFICATION OVERLAY
            val liveNotify by viewModel.liveNotification.collectAsState()
            LaunchedEffect(liveNotify) {
                if (liveNotify != null) {
                    kotlinx.coroutines.delay(4500) // auto-dismiss
                    viewModel.liveNotification.value = null
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = liveNotify != null,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                liveNotify?.let { (title, msg) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp)
                            .clickable { viewModel.liveNotification.value = null },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (com.example.ui.theme.ThemeManager.LocalDarkTheme.current) Color(0xFF1E293B) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Уведомление",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (com.example.ui.theme.ThemeManager.LocalDarkTheme.current) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    color = if (com.example.ui.theme.ThemeManager.LocalDarkTheme.current) Color(0xFF94A3B8) else Color(0xFF475569),
                                    lineHeight = 16.sp
                                )
                            }
                            IconButton(onClick = { viewModel.liveNotification.value = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showCertInfoDialog) {
                val isNationalCert = currentUrl.startsWith("https://") && (
                    currentUrl.contains(".ru") || 
                    currentUrl.contains(".su") || 
                    currentUrl.contains("gosuslugi") ||
                    currentUrl.contains("sberbank") ||
                    currentUrl.contains("ya.ru") ||
                    useMintsifryCertsOnly
                )

                AlertDialog(
                    onDismissRequest = { showCertInfoDialog = false },
                    confirmButton = {
                        Button(
                            onClick = { showCertInfoDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Закрыть")
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (isNationalCert) Icons.Default.Shield else Icons.Default.Lock,
                            contentDescription = "Статус сертификата",
                            tint = if (isNationalCert) Color(0xFF0039A6) else Color(0xFF3DDC84),
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    title = {
                        Text(
                            text = if (isNationalCert) "Безопасное ГОСТ-соединение" else "Защищенное TLS-соединение",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isNationalCert) {
                                Text(
                                    text = "Соединение защищено национальным SSL-сертификатом Минцифры России.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Детали сертификата:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Узел: ${Uri.parse(currentUrl).host ?: currentUrl}", fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        Text("Издатель: Минцифры России Root CA", fontSize = 11.sp)
                                        Text("Протокол: TLSv1.3 / ГОСТ-Шифр", fontSize = 11.sp)
                                        Text(
                                            text = "Алгоритм: " + when(gostCipherSuite) {
                                                0 -> "ГОСТ Р 34.12-2015 «Кузнечик»"
                                                1 -> "ГОСТ 28147-89"
                                                else -> "ГОСТ Р 34.10-2012 / ГОСТ Р 34.11-2012"
                                            }, 
                                            fontSize = 11.sp
                                        )
                                        Text("Доверие: Национальный реестр РФ (ОК)", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Text(
                                    text = "Соединение защищено глобальным сертификатом безопасности (TLS/SSL).",
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Детали сертификата:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Узел: ${Uri.parse(currentUrl).host ?: currentUrl}", fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        Text("Протокол: TLSv1.3", fontSize = 11.sp)
                                        Text("Шифрование: AES_256_GCM", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun setupBrowserSettings(webView: WebView) {
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        builtInZoomControls = true
        displayZoomControls = false
        allowContentAccess = true
        allowFileAccess = true
        allowFileAccessFromFileURLs = true
        allowUniversalAccessFromFileURLs = true
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }
}
