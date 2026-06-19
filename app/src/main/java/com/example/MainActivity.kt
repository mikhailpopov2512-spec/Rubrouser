package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.BookmarksHistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BrowserViewModel

enum class AppScreen {
    BROWSER,
    SETTINGS,
    BOOKMARKS
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      android.util.Log.e("MainActivity", "FATAL CRASH on thread ${thread.name}", throwable)
      throwable.printStackTrace()
    }
    try {
      super.onCreate(savedInstanceState)
      enableEdgeToEdge()
      setContent {
        val viewModel: BrowserViewModel = viewModel()
        val hasAcceptedTerms by viewModel.hasAcceptedTerms.collectAsState()

        LaunchedEffect(hasAcceptedTerms) {
          if (hasAcceptedTerms) {
            // Safety: Initialize RKN blocklist manager on background thread
            com.example.utils.RknBlocklistManager.initialize(applicationContext, viewModel.repository)
          }
        }
        val themeMode by viewModel.selectedThemeMode.collectAsState()
        val isDarkTheme = when (themeMode) {
          1 -> false
          2 -> true
          else -> androidx.compose.foundation.isSystemInDarkTheme()
        }

        MyApplicationTheme(darkTheme = isDarkTheme, dynamicColor = false) {
          var isSplashActive by remember { mutableStateOf(true) }
          var currentScreen by remember { mutableStateOf(AppScreen.BROWSER) }

          if (isSplashActive) {
            com.example.ui.components.YandexSplashScreen(
              onFinished = { isSplashActive = false }
            )
          } else {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
              when (currentScreen) {
                AppScreen.BROWSER -> {
                  BrowserScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { currentScreen = AppScreen.SETTINGS },
                    onNavigateToBookmarks = { currentScreen = AppScreen.BOOKMARKS },
                    modifier = Modifier.padding(innerPadding)
                  )
                }
                AppScreen.SETTINGS -> {
                  SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = AppScreen.BROWSER },
                    modifier = Modifier.padding(innerPadding)
                  )
                }
                AppScreen.BOOKMARKS -> {
                  BookmarksHistoryScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = AppScreen.BROWSER },
                    onUrlSelected = { url ->
                       currentScreen = AppScreen.BROWSER
                       viewModel.loadUrl(url)
                    },
                    modifier = Modifier.padding(innerPadding)
                  )
                }
              }
            }
          }
        }
      }
    } catch (e: Throwable) {
      android.util.Log.e("MainActivity", "Fatal error in MainActivity onCreate", e)
    }
  }
}

