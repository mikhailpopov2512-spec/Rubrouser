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
                // Lock vs Protect Shield Indicator
                IconButton(onClick = {
                    // Quick warning connections dialog or info
                }) {
                    Icon(
                        imageVector = if (browserMode == 4) {
                            Icons.Default.Security
                        } else if (currentUrl.startsWith("https://")) {
                            Icons.Default.Lock
                        } else {
                            Icons.Default.Shield
                        },
                        contentDescription = "Защита СВ",
                        tint = if (browserMode == 4) {
                            Color(0xFF00FF66)
                        } else if (currentUrl.startsWith("https://")) {
                            Color(0xFF3DDC84) // Safe Green
                        } else {
                            Color.Gray
                        },
                        modifier = Modifier.size(18.dp)
                    )
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
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Render Address Bar strictly at the bottom if Preference == 0 (Bottom)
                    if (addressBarPos == 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.loadUrl("about:home") }) {
                                Icon(imageVector = Icons.Default.Home, contentDescription = "Главная")
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                omniboxTextField()
                            }
                        }

                        // Omnibox mini tri-color strip indicator for Bottom Bar
                        Row(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0039A6)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD52B1E)))
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
                                    com.example.utils.BrowserNotificationHelper.showNotification(
                                        context,
                                        id = 1002,
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
                                                    com.example.utils.BrowserNotificationHelper.showNotification(
                                                        context,
                                                        id = 1001,
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
