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
import com.example.ui.screens.ConsentScreen
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
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: BrowserViewModel = viewModel()
        val hasAcceptedTerms by viewModel.hasAcceptedTerms.collectAsState()
        var currentScreen by remember { mutableStateOf(AppScreen.BROWSER) }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          if (!hasAcceptedTerms) {
            ConsentScreen(
              modifier = Modifier.padding(innerPadding),
              onAccept = {
                viewModel.acceptTerms()
              }
            )
          } else {
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
  }
}

