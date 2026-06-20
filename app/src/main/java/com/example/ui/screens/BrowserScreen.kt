package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.RussianFlagBackground
import com.example.ui.viewmodel.BrowserTab
import com.example.ui.viewmodel.BrowserViewModel
import com.example.utils.FilteringLevel
import com.example.utils.RknBlocklistManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val bgTheme by viewModel.backgroundTheme.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val blockedCount by viewModel.blockedCount.collectAsState()
    val securityLevel by viewModel.filteringLevel.collectAsState()
    val listBlockedLogs by viewModel.blockedLogs.collectAsState()

    var addressBarInput by remember { mutableStateOf("") }
    
    // Lock addressBarInput tracker with currentUrl updates
    LaunchedEffect(currentUrl) {
        if (!currentUrl.startsWith("rosbrowser://")) {
            addressBarInput = currentUrl
        } else {
            addressBarInput = ""
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // Modern Web Search & Navigation Top Bar
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sovereign shield badge indicator for network/connections
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (securityLevel == FilteringLevel.LOW) Color(0xFF64748B) 
                                else Color(0xFF10B981)
                            )
                            .clickable { viewModel.selectTab(BrowserTab.SETTINGS) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Security Status",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Address and Search entry Field
                    OutlinedTextField(
                        value = addressBarInput,
                        onValueChange = { addressBarInput = it },
                        placeholder = { 
                            Text(
                                "Поиск в Яндексе или URL...", 
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.submitSearch(addressBarInput)
                            }
                        ),
                        trailingIcon = {
                            if (addressBarInput.isNotEmpty()) {
                                IconButton(onClick = { addressBarInput = "" }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            } else {
                                Icon(Icons.Default.Search, "Search", tint = Color.Gray)
                            }
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("address_bar_input")
                    )

                    if (!currentUrl.startsWith("rosbrowser://")) {
                        Spacer(modifier = Modifier.width(6.dp))
                        // Stop / Reload / Home action button
                        IconButton(onClick = { viewModel.goHome() }) {
                            Icon(Icons.Default.Home, "Go Home", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        bottomBar = {
            // HIGHLY POLISHED MODERN BOTTOM NAVIGATION BAR
            // Respect proper insets of device navigation (gestures pill)
            Surface(
                tonalElevation = 16.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .navigationBarsPadding()
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        BottomNavItem(BrowserTab.HOME, Icons.Default.Language, "Браузер"),
                        BottomNavItem(BrowserTab.PASSWORD_MANAGER, Icons.Default.VpnKey, "Пароли"),
                        BottomNavItem(BrowserTab.SETTINGS, Icons.Default.Settings, "Настройки")
                    )

                    tabs.forEach { item ->
                        val isSelected = activeTab == item.tab
                        val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.selectTab(item.tab)
                                    if (item.tab == BrowserTab.HOME) viewModel.goHome()
                                }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("nav_icon_${item.label.lowercase()}")
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tint
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                BrowserTab.HOME -> {
                    if (currentUrl == "rosbrowser://home") {
                        // RENDER SOVEREIGN STARTUP HOMEPAGE
                        SovereignHomePage(
                            viewModel = viewModel,
                            blockedCount = blockedCount,
                            securityLevel = securityLevel,
                            listBlockedLogs = listBlockedLogs,
                            bgTheme = bgTheme
                        )
                    } else if (currentUrl.startsWith("rosbrowser://blocked")) {
                        // BLOCKED PAGE RENDER
                        SovereignBlockWarningPage(
                            url = currentUrl.substringAfter("url="),
                            onGoBack = { viewModel.goHome() }
                        )
                    } else {
                        // LOAD IMMERSIVE REAL INTERACTIVE WEB VIEW with blocklist interception
                        RealSovereignWebView(
                            url = currentUrl,
                            onBlock = { blockedUrl ->
                                viewModel.setUrl("rosbrowser://blocked?url=$blockedUrl")
                            }
                        )
                    }
                }
                BrowserTab.PASSWORD_MANAGER -> {
                    PasswordManagerScreen(viewModel = viewModel)
                }
                BrowserTab.SETTINGS -> {
                    SettingsScreen(viewModel = viewModel)
                }
                BrowserTab.ABOUT -> {
                    AboutAppScreen()
                }
            }
        }
    }
}

/**
 * Custom Russian Home Page with dynamic trees, waving Russian flag, security widgets and search shortcuts
 */
@Composable
fun SovereignHomePage(
    viewModel: BrowserViewModel,
    blockedCount: Int,
    securityLevel: FilteringLevel,
    listBlockedLogs: List<String>,
    bgTheme: com.example.ui.components.BackgroundTheme
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Live animated background
        RussianFlagBackground(
            modifier = Modifier.fillMaxSize(),
            bgTheme = bgTheme,
            windStrength = 1.0f
        )

        // Semi-transparent overlay to keep search and card widgets highly readable
        val overlayColor = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) {
            Color(0xDD0F172A)
        } else {
            Color(0xE6FFFFFF)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Title Header with sovereign theme
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "RosBrowser Globe",
                        tint = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color(0xFF60A5FA) else Color(0xFF2563EB),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "РОСБРАУЗЕР",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color.White else Color(0xFF1E293B),
                        letterSpacing = 1.sp
                    )
                }
            }

            // Real-Time Connection Security & Firewall Widget (Requested!)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color(0x33FFFFFF) else Color(0x0D000000)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(
                            1.dp,
                            if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color(0x22FFFFFF) else Color(0x11000000),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Shield Active",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ГОСТ-Защита Соединения",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color.White else Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "АКТИВНО",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Connection Type metadata
                        Text(
                            text = "Крипто-протокол: ГОСТ Р 34.12-2015 TLS-клиент\n" +
                                    "Режим фильтрации сайтов: ${securityLevel.displayName}",
                            fontSize = 12.sp,
                            color = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color(0xFF94A3B8) else Color(0xFF64748B),
                            lineHeight = 16.sp
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color(0x22FFFFFF) else Color(0x11000000)
                        )

                        // Blocked Counter layout
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    "Заблокировано угроз и спам-узлов",
                                    fontSize = 11.sp,
                                    color = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$blockedCount доменов",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color.White else Color(0xFF1E293B)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Action to trigger FS alert debug test
                            Button(
                                onClick = { viewModel.submitSearch("coin-miner.org") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDF3B3F)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Проверить защиту", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        // Real-time block log highlights
                        if (listBlockedLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "История сетевой чистки:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color(0xFF60A5FA) else Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            listBlockedLogs.take(3).forEach { log ->
                                Text(
                                    text = "• $log",
                                    fontSize = 10.sp,
                                    color = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Access Sovereign Russian Portals Grid
            item {
                Text(
                    text = "Популярные российские сервисы",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color(0xFF94A3B8) else Color(0xFF475569),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, start = 4.dp),
                    textAlign = TextAlign.Start
                )
            }

            item {
                val gridList = listOf(
                    SovereignPortalItem("Яндекс", "ya.ru", "https://ya.ru", Color(0xFFFF2525)),
                    SovereignPortalItem("Госуслуги", "gosuslugi.ru", "https://gosuslugi.ru", Color(0xFF0F54C6)),
                    SovereignPortalItem("ВКонтакте", "vk.com", "https://vk.com", Color(0xFF0077FF)),
                    SovereignPortalItem("Mail.ru", "mail.ru", "https://mail.ru", Color(0xFFFF9E00))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridList.forEach { portal ->
                        Card(
                            onClick = { viewModel.setUrl(portal.targetUrl) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color(0x1AFFFFFF) else Color(0x33E2E8F0)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .border(
                                    0.5.dp,
                                    if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color(0x11FFFFFF) else Color(0x09000000),
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(portal.themeColor)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        portal.name.take(1),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    portal.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color.White else Color(0xFF1E293B)
                                )
                                Text(
                                    portal.displayUrl,
                                    fontSize = 8.sp,
                                    color = if (bgTheme == com.example.ui.components.BackgroundTheme.DARK) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Android WebView representation wrapper with blocklist web interceptor
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RealSovereignWebView(
    url: String,
    onBlock: (blockedUrl: String) -> Unit
) {
    val context = LocalContext.current
    
    // Web view implementation check
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val reqUrl = request?.url?.toString() ?: ""
                        if (RknBlocklistManager.shouldBlock(reqUrl)) {
                            onBlock(reqUrl)
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let {
                            if (RknBlocklistManager.shouldBlock(it)) {
                                onBlock(it)
                            }
                        }
                    }
                }
            }
        },
        update = { webView ->
            if (!RknBlocklistManager.shouldBlock(url)) {
                webView.loadUrl(url)
            } else {
                onBlock(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Beautiful Russian sovereign lock banner warning
 */
@Composable
fun SovereignBlockWarningPage(
    url: String,
    onGoBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF7F1D1D))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning Blocked",
            tint = Color.White,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "ДОСТУП ОГРАНИЧЕН",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Единая суверенная система фильтрации трафика Росбраузера ограничила доступ к следующему сайту:\n\n$url\n\nПричина: данный ресурс занесён в реестр запрещенных сайтов РКН РФ или осуществляет несанкционированную сетевую кражу/слежку данных граждан.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFFFECACA),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onGoBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF7F1D1D)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Вернуться на главную", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AboutAppScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Security, "About", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Росбраузер v1.0", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Разработано с заботой о суверенитете и защите конфиденциальности граждан Российской Федерации.\nШифрование ГОСТ 34.12.", textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

data class BottomNavItem(
    val tab: BrowserTab,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

data class SovereignPortalItem(
    val name: String,
    val displayUrl: String,
    val targetUrl: String,
    val themeColor: Color
)
