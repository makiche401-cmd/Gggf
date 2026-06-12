package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.VioraGradientButton
import com.example.ui.components.glassmorphic
import com.example.ui.components.purpleGlow
import com.example.ui.theme.*
import com.example.ui.viewmodel.VioraViewModel

// =============================================================================
// SIGN UP SCREEN
// =============================================================================
@Composable
fun SignUpScreen(viewModel: VioraViewModel) {
    val email by viewModel.authEmail.collectAsState()
    val password by viewModel.authPassword.collectAsState()
    val confirmPassword by viewModel.authConfirmPassword.collectAsState()
    val termsAccepted by viewModel.termsAccepted.collectAsState()
    val showOtpDialog by viewModel.showOtpDialog.collectAsState()

    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setScreen("WELCOME") }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Join Viora", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            // Glassmoprhic Auth Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(backgroundColor = Color(0x26140B1F), borderColor = BorderColor, borderRadius = 22.dp)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Create Account",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Join our authenticated community of authentic souls",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Email Input
                Text("Email Address", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.setAuthEmail(it) },
                    placeholder = { Text("Enter your email address", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = BrightNeonPurple) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BrightNeonPurple,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color(0xFF10081C),
                        unfocusedContainerColor = Color(0xFF10081C)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_email_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                Text("Password", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.setAuthPassword(it) },
                    placeholder = { Text("Create secure password", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock", tint = BrightNeonPurple) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BrightNeonPurple,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color(0xFF10081C),
                        unfocusedContainerColor = Color(0xFF10081C)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_password_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password Input
                Text("Confirm Password", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { viewModel.setAuthConfirmPassword(it) },
                    placeholder = { Text("Confirm your password", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock Confirm", tint = BrightNeonPurple) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BrightNeonPurple,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color(0xFF10081C),
                        unfocusedContainerColor = Color(0xFF10081C)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Terms Acceptance Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTermsAccepted(!termsAccepted) }
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { viewModel.setTermsAccepted(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryPinkPurple,
                            uncheckedColor = BorderColor,
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("I accept the Terms and Privacy Agreement.", color = TextSecondary, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                VioraGradientButton(
                    text = "Create Account",
                    onClick = { viewModel.handleSignUp() },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_signup_submit_account"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Already registered? ", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        "Login",
                        color = BrightNeonPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { viewModel.setScreen("LOGIN") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // OTP Code Verification Drawer
    if (showOtpDialog) {
        OtpDialog(viewModel = viewModel)
    }
}


// =============================================================================
// LOGIN SCREEN
// =============================================================================
@Composable
fun LoginScreen(viewModel: VioraViewModel) {
    val email by viewModel.authEmail.collectAsState()
    val password by viewModel.authPassword.collectAsState()
    val rememberMe by viewModel.rememberMe.collectAsState()

    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setScreen("WELCOME") }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            // Glassmoprhic Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(backgroundColor = Color(0x26140B1F), borderColor = BorderColor, borderRadius = 22.dp)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Welcome Back",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Login to find authentic connections",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Email Address
                Text("Email Address", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                // Exact styling matching screenshots
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.setAuthEmail(it) },
                    placeholder = { Text("Enter your email address", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = BrightNeonPurple) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BrightNeonPurple,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color(0xFF10081C),
                        unfocusedContainerColor = Color(0xFF10081C)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                Text("Password", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.setAuthPassword(it) },
                    placeholder = { Text("Enter secure password", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock", tint = BrightNeonPurple) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BrightNeonPurple,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color(0xFF10081C),
                        unfocusedContainerColor = Color(0xFF10081C)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Remember Me / Forgot Password Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.setRememberMe(!rememberMe) }
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { viewModel.setRememberMe(it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryPinkPurple,
                                uncheckedColor = BorderColor,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remember Me", color = TextSecondary, fontSize = 13.sp)
                    }

                    Text(
                        text = "Forgot password?",
                        color = BrightNeonPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { viewModel.setScreen("FORGOT_PASSWORD") }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                VioraGradientButton(
                    text = "Login",
                    onClick = { viewModel.handleLogin() },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_login_submit"
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Don't have an account? ", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        "Sign Up",
                        color = BrightNeonPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { viewModel.setScreen("SIGN_UP") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


// =============================================================================
// FORGOT PASSWORD SCREEN
// =============================================================================
@Composable
fun ForgotPasswordScreen(viewModel: VioraViewModel) {
    val email by viewModel.authEmail.collectAsState()
    val showOtpDialog by viewModel.showOtpDialog.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setScreen("WELCOME") }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Password", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            // Recovery Form Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(backgroundColor = Color(0x26140B1F), borderColor = BorderColor, borderRadius = 22.dp)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Password Recovery",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A 4-digit security OTP code will be delivered to verify your identity.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Enter Email Address", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.setAuthEmail(it) },
                    placeholder = { Text("email@example.com", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = BrightNeonPurple) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BrightNeonPurple,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color(0xFF10081C),
                        unfocusedContainerColor = Color(0xFF10081C)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(28.dp))

                VioraGradientButton(
                    text = "Send Secure OTP",
                    onClick = { viewModel.handleForgotPassword() },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_forgot_password_submit"
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Return to Welcoming Lobby",
                    color = BrightNeonPurple,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setScreen("WELCOME") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showOtpDialog) {
        OtpDialog(viewModel = viewModel)
    }
}


// =============================================================================
// SECURE VERIFY OTP DIALOG
// =============================================================================
@Composable
fun OtpDialog(viewModel: VioraViewModel) {
    val code by viewModel.otpCode.collectAsState()
    val email by viewModel.authEmail.collectAsState()

    Dialog(onDismissRequest = { viewModel.setShowOtpDialog(false) }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x3300FF87)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LockClock, contentDescription = "Clock secure", tint = SuccessGreen, modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Enter Verification Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "We've sent a 4-digit code to $email. Enter it to activate secure session token.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // OTP Outlined Form
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 4) viewModel.setOtpCode(it) },
                    placeholder = { Text("• • • •", color = TextSecondary, textAlign = TextAlign.Center) },
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrightNeonPurple),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrightNeonPurple,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color(0xFF10081C),
                        unfocusedContainerColor = Color(0xFF10081C)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .width(160.dp)
                        .testTag("otp_code_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                VioraGradientButton(
                    text = "Verify & Access",
                    onClick = { viewModel.verifyOtp() },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_otp_verify_submit"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Resend Code",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        viewModel.setOtpCode("")
                        // Simply refresh
                    }
                )
            }
        }
    }
}
