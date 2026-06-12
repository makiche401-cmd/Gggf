package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VioraViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: VioraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support for full screen layout
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val screenState by viewModel.currentScreen.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Root level crossfade transition between Onboarding/Login and Main application views
                    AnimatedContent(
                        targetState = screenState,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "screen_navigation"
                    ) { activeScreen ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (activeScreen) {
                                "WELCOME" -> WelcomeScreen(
                                    onGetStarted = { viewModel.setScreen("HOW_IT_WORKS") },
                                    onLoginClick = { viewModel.setScreen("LOGIN") },
                                    onForgotPasswordClick = { viewModel.setScreen("FORGOT_PASSWORD") }
                                )
                                "HOW_IT_WORKS" -> HowItWorksScreen(
                                    onContinue = { viewModel.setScreen("MESSAGING_GUIDE") },
                                    onBack = { viewModel.setScreen("WELCOME") }
                                )
                                "MESSAGING_GUIDE" -> MessagingGuideScreen(
                                    onLetGetStarted = { viewModel.setScreen("SIGN_UP") },
                                    onBack = { viewModel.setScreen("HOW_IT_WORKS") }
                                )
                                "SIGN_UP" -> SignUpScreen(viewModel = viewModel)
                                "LOGIN" -> LoginScreen(viewModel = viewModel)
                                "FORGOT_PASSWORD" -> ForgotPasswordScreen(viewModel = viewModel)
                                "MAIN" -> MainScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
