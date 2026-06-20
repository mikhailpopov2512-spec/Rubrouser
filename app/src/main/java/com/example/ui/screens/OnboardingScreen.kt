package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BackgroundTheme
import com.example.ui.components.RussianFlagBackground
import com.example.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: BrowserViewModel,
    onComplete: () -> Unit
) {
    val isRegistered by viewModel.isRegistered.collectAsState()
    var isLoginMode by remember { mutableStateOf(isRegistered) }

    // Forms states
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPolicyAccepted by remember { mutableStateOf(false) }
    var showDialogPolicy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // Draw the background waving Russian flag with Birches
        RussianFlagBackground(
            modifier = Modifier.fillMaxSize(),
            bgTheme = BackgroundTheme.SUMMER,
            windStrength = 0.5f
        )

        // Dimmer overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD90F172A))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Identity Header
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Shield",
                tint = Color(0xFF64B5F6),
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 12.dp)
            )

            Text(
                text = "РОСБРАУЗЕР",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                color = Color.White,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Отечественная система интернет-навигации",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Glassmorphic main form card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x26FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isLoginMode) "Авторизация пользователя" else "Регистрация профиля РФ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    if (errorMsg.isNotEmpty()) {
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (!isLoginMode) {
                        // FULL NAME FIELD
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it; errorMsg = "" },
                            label = { Text("ФИО гражданина", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = Color.LightGray) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0x66FFFFFF),
                                focusedLabelColor = Color(0xFF3B82F6),
                                cursorColor = Color(0xFF3B82F6),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("register_name_input")
                        )
                    }

                    // EMAIL FIELD
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it; errorMsg = "" },
                        label = { Text("Почта / Логин ID", color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = Color.LightGray) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0x66FFFFFF),
                            focusedLabelColor = Color(0xFF3B82F6),
                            cursorColor = Color(0xFF3B82F6),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("auth_email_input")
                    )

                    // PASSWORD FIELD
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; errorMsg = "" },
                        label = { Text("Пароль доступа", color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = Color.LightGray) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0x66FFFFFF),
                            focusedLabelColor = Color(0xFF3B82F6),
                            cursorColor = Color(0xFF3B82F6),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("auth_password_input")
                    )

                    if (!isLoginMode) {
                        // PRIVACY POLICY CHECKBOX
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        ) {
                            Checkbox(
                                checked = isPolicyAccepted,
                                onCheckedChange = { isPolicyAccepted = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF22C55E),
                                    uncheckedColor = Color(0x66FFFFFF)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                onClick = { showDialogPolicy = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Я принимаю политику конфиденциальности",
                                    fontSize = 12.sp,
                                    color = Color(0xFF60A5FA),
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                )
                            }
                        }
                    }

                    // SUBMIT BUTTON
                    Button(
                        onClick = {
                            if (isLoginMode) {
                                if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                                    errorMsg = "Пожалуйста, заполните все поля"
                                } else {
                                    if (emailInput.trim().lowercase() == viewModel.userEmail.value.lowercase()) {
                                        viewModel.loginUser(emailInput) {
                                            onComplete()
                                        }
                                    } else {
                                        // Give friendly registration suggestion
                                        errorMsg = "Профиль с такой почтой не найден.\nПерейдите к регистрации ниже."
                                    }
                                }
                            } else {
                                if (nameInput.isEmpty() || emailInput.isEmpty() || passwordInput.isEmpty()) {
                                    errorMsg = "Пожалуйста, заполните все поля"
                                } else if (!isPolicyAccepted) {
                                    errorMsg = "Необходимо принять Политику конфиденциальности"
                                } else if (!emailInput.contains("@") || !emailInput.contains(".")) {
                                    errorMsg = "Пожалуйста, введите корректный Email"
                                } else {
                                    viewModel.registerUser(nameInput, emailInput) {
                                        onComplete()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button")
                    ) {
                        Text(
                            text = if (isLoginMode) "Войти в Росбраузер" else "Создать единый профиль",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Switch Mode text link btn
                    TextButton(
                        onClick = {
                            isLoginMode = !isLoginMode
                            errorMsg = ""
                        },
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(
                            text = if (isLoginMode) "У меня еще нет профиля. Зарегистрироваться" else "У меня уже есть профиль. Войти",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }

    // POLICY DIALOG MODAL (Sovereign detailed Russian Privacy Agreement)
    if (showDialogPolicy) {
        AlertDialog(
            onDismissRequest = { showDialogPolicy = false },
            title = {
                Text(
                    text = "Политика конфиденциальности связи РФ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "1. ОБЩИЕ ПОЛОЖЕНИЯ\n" +
                                "Настоящее Соглашение определяет порядок обработки персональных данных граждан Российской Федерации в приложении Росбраузер. Программа создана с соблюдением требований Федерального закона от 27.07.2006 № 152-ФЗ «О персональных данных».\n\n" +
                                "2. МОНИТОРИНГ И БЕЗОПАСНОСТЬ\n" +
                                "В целях повышения суверенной безопасности граждан, шифрования трафика по ГОСТ-криптографии, предупреждения деструктивного воздействия в Интернете, приложение осуществляет автоматический контроль запрещенных сайтов из Единого реестра РКН.\n\n" +
                                "3. СОГЛАСИЕ НА УВЕДОМЛЕНИЯ\n" +
                                "Пользователь признает за государственными службами право информирования о проверках безопасности. Каждый час система направляет отчетное сообщение для обеспечения прозрачности контроля.\n\n" +
                                "4. ЗАЩИТА ПАРОЛЕЙ\n" +
                                "Все сохраненные авторизованные записи шифруются на устройстве локально. Доступ предоставляется исключительно после успешного прохождения биометрического сканирования пальца/лица.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isPolicyAccepted = true
                        showDialogPolicy = false
                    }
                ) {
                    Text("Согласен")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogPolicy = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}
