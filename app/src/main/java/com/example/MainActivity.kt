package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.RosBrowserTheme
import com.example.ui.viewmodel.BrowserViewModel
import com.example.utils.NotificationHelper

class MainActivity : FragmentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Set up notification channel required on Android O+
        NotificationHelper.createNotificationChannel(this)

        // 2. Schedule the hourly FSB monitoring alert (per guidelines / requirements)
        NotificationHelper.scheduleHourlyFsbAlert(this)

        // 3. Mount Content using Compose layout themer
        setContent {
            RosBrowserTheme(darkTheme = false) { // Themed dynamically inside views
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                    Crossfade(
                        targetState = isLoggedIn,
                        label = "main_flow"
                    ) { logged ->
                        if (logged) {
                            BrowserScreen(viewModel = viewModel)
                        } else {
                            // Display the detailed Russian Profile Registration / Login UI
                            OnboardingScreen(
                                viewModel = viewModel,
                                onComplete = { }
                            )
                        }
                    }
                }
            }
        }
    }
}
