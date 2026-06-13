package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.database.BlockedUrl
import com.example.ui.components.RussianFlagBackdrop
import com.example.ui.viewmodel.BrowserViewModel
import com.example.utils.WebInterceptors
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.net.URL

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

    var textInput by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Update address field text when webview changes pages
    LaunchedEffect(currentUrl) {
        if (currentUrl != "about:home") {
            textInput = currentUrl
        } else {
            textInput = ""
        }
    }

    // Handle back button clicks
    BackHandler(enabled = currentUrl != "about:home") {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            viewModel.loadUrl("about:home")
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                // Address Bar Top
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.loadUrl("about:home")
                            webViewRef?.loadUrl("about:home")
                        },
                        modifier = Modifier.testTag("home_button")
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Главная")
                    }

                    // Omnibox Bar
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("omnibox_input"),
                        placeholder = { Text("Поиск или адрес (ya.ru)", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (currentUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.Language,
                                contentDescription = "Соединение",
                                tint = if (currentUrl.startsWith("https://")) Color(0xFF3DDC84) else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (textInput.isNotEmpty()) {
                                IconButton(onClick = { textInput = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Очистить", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                viewModel.loadUrl(textInput)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Bookmark active indicator toggle
                    var isCurrentBookmarked by remember { mutableStateOf(false) }
                    LaunchedEffect(currentUrl) {
                        isCurrentBookmarked = viewModel.repository.isBookmarked(currentUrl)
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (isCurrentBookmarked) {
                                    viewModel.removeBookmark(currentUrl)
                                    isCurrentBookmarked = false
                                } else {
                                    viewModel.addBookmark(pageTitle, currentUrl)
                                    isCurrentBookmarked = true
                                }
                            }
                        },
                        enabled = currentUrl != "about:home",
                        modifier = Modifier.testTag("add_bookmark_btn")
                    ) {
                        Icon(
                            imageVector = if (isCurrentBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "В закладки",
                            tint = if (isCurrentBookmarked) Color(0xFFD52B1E) else Color.Gray
                        )
                    }
                }

                // Narrow Tricolor separator line of 2dp height
                // Ratio: 1/3 White, 1/3 Blue, 1/3 Red
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0039A6)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD52B1E)))
                }

                // Loading linear progress bar
                if (isWebLoading) {
                    LinearProgressIndicator(
                        progress = webProgress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = Color(0xFF0039A6),
                        trackColor = Color.Transparent
                    )
                }
            }
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier
                    .height(60.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                contentPadding = PaddingValues(horizontal = 8.dp)
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (currentUrl == "about:home") {
                // Render the elegant New Tab Page (NTP)
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
                            
                            webViewClient = object : WebViewClient() {
                                
                                // Monitor URL starts to check RKN blocks and Safe browsing
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    url?.let {
                                        viewModel.isWebLoading.value = true
                                        viewModel.currentUrl.value = it
                                        
                                        // Run block and security inspections
                                        coroutineScope.launch {
                                            val isBlocked = WebInterceptors.checkRknBlock(it, viewModel.repository) { matchedBlock ->
                                                // Log blocked attempt to DB for live statistics!
                                                viewModel.repository.logBlockedAttempt(it, matchedBlock.reason)
                                                
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
                                        // Intercept and prevent crashing. Act as a deep-link routing
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
                                        // Return empty resource
                                        return WebResourceResponse(
                                            "text/javascript",
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

                            loadUrl(currentUrl)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        if (view.url != currentUrl) {
                            view.loadUrl(currentUrl)
                        }
                        webViewRef = view
                    },
                    modifier = Modifier.fillMaxSize()
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
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }
}

@Composable
fun NewTabPageView(
    viewModel: BrowserViewModel,
    onUrlSelected: (String) -> Unit
) {
    val searchEngine by viewModel.selectedSearchEngine.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("new_tab_page")
    ) {
        // Programmatic Russian flag background
        RussianFlagBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Brand Header
            Text(
                text = "РОСБРАУЗЕР",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "СУВЕРЕННЫЙ ОТЕЧЕСТВЕННЫЙ БРАУЗЕР",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Centered Search Engine Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Поиск",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = { Text("Поиск в ${if (searchEngine == 0) "Яндекс" else if (searchEngine == 1) "Mail.ru" else "Rambler"}...", fontSize = 14.sp) },
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
                    if (searchInput.isNotBlank()) {
                        IconButton(onClick = { searchInput = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Сброс")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Quick Access Grid of Russian Ecological Services
            val quickTiles = listOf(
                QuickServiceTile("Яндекс", "https://ya.ru", Icons.Default.Search, Color(0xFFD52B1E)),
                QuickServiceTile("ВКонтакте", "https://vk.com", Icons.Default.Group, Color(0xFF0077FF)),
                QuickServiceTile("Госуслуги", "https://www.gosuslugi.ru", Icons.Default.AccountBalance, Color(0xFF0C5BFF)),
                QuickServiceTile("Rutube", "https://rutube.ru", Icons.Default.Tv, Color(0xFFE50914)),
                QuickServiceTile("Почта Mail", "https://mail.ru", Icons.Default.Email, Color(0xFF005FFC)),
                QuickServiceTile("Рамблер", "https://www.rambler.ru", Icons.Default.Explore, Color(0xFF2E6FF2)),
                QuickServiceTile("RuStore", "https://rustore.ru", Icons.Default.Shop, Color(0xFF33BB55)),
                QuickServiceTile("ТАСС", "https://tass.ru", Icons.Default.Info, Color(0xFF003D99))
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(quickTiles) { tile ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onUrlSelected(tile.url) }
                            .padding(top = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(tile.brandColor.copy(alpha = 0.15f), CircleShape)
                                .testTag("tile_${tile.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tile.icon,
                                contentDescription = tile.name,
                                tint = tile.brandColor,
                                modifier = Modifier.size(24.dp)
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

            // High Safety stamp on NTP
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFF3DDC84).copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = "Защита", tint = Color(0xFF3DDC84), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ГОСТ TLS криптография и Безопасный поиск активны", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

data class QuickServiceTile(
    val name: String,
    val url: String,
    val icon: ImageVector,
    val brandColor: Color
)
