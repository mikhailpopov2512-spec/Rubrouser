package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RusBlue
import com.example.ui.theme.RusRed
import com.example.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserOnboardingScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStage by remember { mutableStateOf(0) } // 0 = Profile/Interests, 1 = Permissions, 2 = Wallpaper
    
    // Stage 0 state
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    val availableInterests = listOf(
        "Технологии", "Наука", "Спорт", "Экономика", "Политика", "Авто", "Культура", "Досуг"
    )
    val selectedInterests = remember { mutableStateListOf<String>() }
    var errorMessage by remember { mutableStateOf("") }

    // Stage 1 permissions state
    var isLocationGranted by remember { mutableStateOf(false) }
    var isNotificationsGranted by remember { mutableStateOf(false) }
    var isMicrophoneGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        isLocationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isNotificationsGranted = results[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            isNotificationsGranted = true
        }
        isMicrophoneGranted = results[Manifest.permission.RECORD_AUDIO] == true

        viewModel.isLocationPermGranted.value = isLocationGranted
        viewModel.isNotificationPermGranted.value = isNotificationsGranted
        viewModel.isVoicePermGranted.value = isMicrophoneGranted
    }

    // Stage 2 wallpapers state
    val selectedWallpaperIndex by viewModel.selectedNtpThemeBackground.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("onboarding_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Branded Logo Header representing Russian Patriot Theme
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(RusBlue, RusRed)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Звезда",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "РОСБРАУЗЕР",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Версия 2.0 • ГЛУБОКОЕ ОБНОВЛЕНИЕ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                // STEPPER / DOTS INDICATOR
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    repeat(3) { stageIdx ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (currentStage == stageIdx) 10.dp else 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (currentStage == stageIdx) Color(0xFF38BDF8) else Color(0xFF475569))
                        )
                    }
                }

                Divider(color = Color(0xFF334155), modifier = Modifier.padding(bottom = 16.dp))

                // STAGE ROUTER WITH ANIMATIONS
                AnimatedContent(
                    targetState = currentStage,
                    transitionSpec = {
                        slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                    },
                    label = "stageTransition"
                ) { stage ->
                    when (stage) {
                        0 -> {
                            // STAGE 0: Profile info and selection of interests
                            Column(fillMaxWidth()) {
                                Text(
                                    text = "Шаг 1: Личные интересы",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Настройте ваш профиль и выберите любимые категории блога Дзен.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = firstName,
                                    onValueChange = {
                                        firstName = it
                                        errorMessage = ""
                                    },
                                    label = { Text("Имя пользователя", color = Color(0xFF94A3B8)) },
                                    placeholder = { Text("Введите имя") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF475569)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_first_name_input")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = lastName,
                                    onValueChange = { lastName = it },
                                    label = { Text("Фамилия (необязательно)", color = Color(0xFF94A3B8)) },
                                    placeholder = { Text("Введите фамилию") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF475569)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_last_name_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Категории ваших интересов для ленты:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Box(modifier = Modifier.height(115.dp)) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(availableInterests) { interest ->
                                            val isSelected = selectedInterests.contains(interest)
                                            Card(
                                                onClick = {
                                                    if (isSelected) {
                                                        selectedInterests.remove(interest)
                                                    } else {
                                                        selectedInterests.add(interest)
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF475569)
                                                ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0x2238BDF8) else Color(0xFF1F2937)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(34.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 6.dp),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color(0xFF38BDF8),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                    }
                                                    Text(
                                                        text = interest,
                                                        color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (errorMessage.isNotBlank()) {
                                    Text(
                                        text = errorMessage,
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                Button(
                                    onClick = {
                                        if (firstName.trim().isBlank()) {
                                            errorMessage = "Пожалуйста, введите имя для продолжения!"
                                        } else {
                                            currentStage = 1
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Далее: Системные полномочия", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        1 -> {
                            // STAGE 1: Permissions
                            Column(fillMaxWidth()) {
                                Text(
                                    text = "Шаг 2: Системные функции",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Для работы суверенных виджетов погоды и аудио ввода требуются системные доступы.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                // Perm 1: Location Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2937)),
                                    border = BorderStroke(1.dp, Color(0xFF475569))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Точная Геолокация", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Умный виджет погоды определит ваш город.", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                        }
                                        Switch(
                                            checked = isLocationGranted,
                                            onCheckedChange = {
                                                permissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }

                                // Perm 2: Microphone Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2937)),
                                    border = BorderStroke(1.dp, Color(0xFF475569))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Голосовой Поиск и Ввод", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Полноценное управление браузером голосом.", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                        }
                                        Switch(
                                            checked = isMicrophoneGranted,
                                            onCheckedChange = {
                                                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                            }
                                        )
                                    }
                                }

                                // Perm 3: Notifications Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2937)),
                                    border = BorderStroke(1.dp, Color(0xFF475569))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = Color(0xFFEAB308),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Уведомления О Загрузках", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Оповещения об успешных загрузках файлов.", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                        }
                                        Switch(
                                            checked = isNotificationsGranted,
                                            onCheckedChange = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                                                } else {
                                                    isNotificationsGranted = true
                                                    viewModel.isNotificationPermGranted.value = true
                                                }
                                            }
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        // Request everything sequentially if not already prompted
                                        val reqList = mutableListOf<String>()
                                        reqList.add(Manifest.permission.ACCESS_FINE_LOCATION)
                                        reqList.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        reqList.add(Manifest.permission.RECORD_AUDIO)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            reqList.add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        permissionLauncher.launch(reqList.toTypedArray())
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .height(34.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3338BDF8)),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                                ) {
                                    Text("Запросить все полномочия списком", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(modifier = Modifier.fillMaxWidth(), gap = 8.dp) {
                                    OutlinedButton(
                                        onClick = { currentStage = 0 },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        border = BorderStroke(1.dp, Color(0xFF475569)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Назад", fontSize = 13.sp)
                                    }

                                    Button(
                                        onClick = { currentStage = 2 },
                                        modifier = Modifier.weight(1.5f).height(44.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                                    ) {
                                        Text("Далее: Выбор Обоев", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        2 -> {
                            // STAGE 2: Personalized Wallpaper
                            Column(fillMaxWidth()) {
                                Text(
                                    text = "Шаг 3: Дизайн и Стиль",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Выберите великолепный фоновый рисунок для вашей главной страницы.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                // Horizontal/grid choice of premium built-in wallpapers
                                Box(modifier = Modifier.height(180.dp)) {
                                    val wallNames = listOf(
                                        "Летний Пляж" to 0,
                                        "Флаг России" to 1,
                                        "Монохром" to 2,
                                        "Озеро Байкал" to 3,
                                        "Кремль Москва" to 4,
                                        "Гейзеры Камчатки" to 5
                                    )
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(wallNames) { pair ->
                                            val wName = pair.first
                                            val wIndex = pair.second
                                            val isSelected = selectedWallpaperIndex == wIndex

                                            Card(
                                                onClick = { viewModel.setNtpBackgroundTheme(wIndex) },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(
                                                    width = 2.dp,
                                                    color = if (isSelected) Color(0xFF38BDF8) else Color.Transparent
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(84.dp)
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                                                    // Draw corresponding preview or colored background
                                                    when (wIndex) {
                                                        3 -> Image(
                                                            painter = painterResource(id = com.example.R.drawable.baikal_wallpaper),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                        4 -> Image(
                                                            painter = painterResource(id = com.example.R.drawable.kremlin_wallpaper),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                        5 -> Image(
                                                            painter = painterResource(id = com.example.R.drawable.kamchatka_wallpaper),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                        1 -> Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(
                                                                    Brush.verticalGradient(
                                                                        colors = listOf(Color.White, Color(0xFF0039A6), Color(0xFFD52B1E))
                                                                    )
                                                                )
                                                        )
                                                        2 -> Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color(0xFF0F172A))
                                                        )
                                                        else -> Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(
                                                                    Brush.linearGradient(
                                                                        colors = listOf(Color(0xFF45B6FE), Color(0xFF3B82F6))
                                                                    )
                                                                )
                                                        )
                                                    }
                                                    
                                                    // Caption
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color.Black.copy(alpha = 0.65f))
                                                            .padding(vertical = 3.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = wName,
                                                            color = Color.White,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(modifier = Modifier.fillMaxWidth(), gap = 8.dp) {
                                    OutlinedButton(
                                        onClick = { currentStage = 1 },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        border = BorderStroke(1.dp, Color(0xFF475569)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Назад", fontSize = 13.sp)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.saveOnboardingData(
                                                firstName.trim(),
                                                lastName.trim(),
                                                selectedInterests.toSet()
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .height(44.dp)
                                            .testTag("onboarding_submit_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("Начать запуск!", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
