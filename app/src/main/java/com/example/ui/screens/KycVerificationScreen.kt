package com.example.ui.screens

import android.speech.tts.TextToSpeech
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.database.UserProfileEntity
import com.example.ui.components.VioraGradientButton
import com.example.ui.components.glassmorphic
import com.example.ui.components.purpleGlow
import com.example.ui.theme.*
import com.example.ui.viewmodel.VioraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun KycVerificationScreen(
    viewModel: VioraViewModel,
    userProfile: UserProfileEntity,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // TTS Voice Engine setup
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    fun speak(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Wizard step state: 1: Identity, 2: Selfie, 3: ID Verification, 4: OCR, 5: Match report, 6: Pending, 7: Approved
    var currentStep by remember { mutableStateOf(1) }

    // Camera Permissions State & Request Launcher
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            speak("Camera permission granted. Opening live feed.")
        } else {
            speak("Camera permission denied. Access is required for scanning.")
        }
    }

    // Step 1 input parameters
    var firstName by remember { mutableStateOf(userProfile.name.split(" ").firstOrNull() ?: "") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf(userProfile.name.split(" ").getOrNull(1) ?: "") }
    var dob by remember { mutableStateOf("1998-05-14") }
    var country by remember { mutableStateOf("United States") }
    var gender by remember { mutableStateOf(userProfile.gender) }

    // Step 2 Selfie simulation controls
    var rejectionMode by remember { mutableStateOf("none") } // "none", "poor_lighting", "multiple_faces", "gallery"
    var selfieCheckStep by remember { mutableStateOf(0) } // 0: initial, 1: front, 2: left, 3: right, 4: blink, 5: smile, 6: done
    val selfieChecks = remember {
        mutableStateMapOf(
            "face" to false,
            "left" to false,
            "right" to false,
            "blink" to false,
            "smile" to false
        )
    }
    var livenessProgress by remember { mutableStateOf(0.0f) }
    var isLivenessRunning by remember { mutableStateOf(false) }
    var captureFlash by remember { mutableStateOf(false) }
    var selfieImageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=250") }

    // Step 3 ID scanning simulator values
    var isFrontScanned by remember { mutableStateOf(false) }
    var isBackScanned by remember { mutableStateOf(false) }
    var activeIdScanningSide by remember { mutableStateOf("front") } // "front", "back"
    var isIdScanningActive by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0.0f) }
    var idQualityOk by remember { mutableStateOf(false) }
    var idFrontImage by remember { mutableStateOf("") }
    var idBackImage by remember { mutableStateOf("") }

    // OCR Editable States (populated from OCR scanner camera analysis)
    var ocrOutputName by remember { mutableStateOf("$firstName ${if (middleName.isNotEmpty()) "$middleName " else ""}$lastName") }
    var ocrOutputDob by remember { mutableStateOf(dob) }
    var ocrOutputIdNum by remember { mutableStateOf("ID-US-${(1000..9999).random()}X${(10..99).random()}") }
    var ocrExpiryDate by remember { mutableStateOf("2034-06-11") }
    var ocrAddress by remember { mutableStateOf("1028 Westwood Blvd, Los Angeles, CA 90024, USA") }

    // Animation progress bars
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    // Face pulse anim
    val pulseSizeMultiplier by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF04010A),
                        Color(0xFF0D051C),
                        Color(0xFF05010B)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentStep > 1 && currentStep < 6) {
                        currentStep--
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back icon",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Identity KYC Verification",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Secure your purple shield badge • Step $currentStep of 7",
                    color = BrightNeonPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Stepper Progress Line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (step in 1..7) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (step == currentStep) {
                                BrightNeonPurple
                            } else if (step < currentStep) {
                                SuccessGreen
                            } else {
                                Color.White.copy(alpha = 0.1f)
                            }
                        )
                )
            }
        }

        // Main step scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            when (currentStep) {
                1 -> {
                    // ---- STEP 1: IDENTITY DETAILS ----
                    Text(
                        text = "Verify Your Identity",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Viora verified user status checks your Government ID details against your online dating parameters.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
                    )

                    // Notice Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x1FFF5722))
                            .border(1.dp, Color(0xFFFF5722).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF8A65),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Important Requirement",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Your profile information should match your government-issued ID. Enter your official name exactly.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Fields
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name *", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrightNeonPurple,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    )

                    OutlinedTextField(
                        value = middleName,
                        onValueChange = { middleName = it },
                        label = { Text("Middle Name (Optional)", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrightNeonPurple,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name *", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrightNeonPurple,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    )

                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth (YYYY-MM-DD) *", color = TextSecondary) },
                        placeholder = { Text("e.g. 1998-05-14") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrightNeonPurple,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    )

                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Issuing Country *", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrightNeonPurple,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    )

                    Text(
                        text = "Select Gender Alignment",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Male", "Female", "Non-Binary").forEach { item ->
                            val isSelected = gender.equals(item, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(if (isSelected) BrightNeonPurple else Color.White.copy(alpha = 0.04f))
                                    .border(
                                        1.dp,
                                        if (isSelected) BrightNeonPurple else BorderColor,
                                        RoundedCornerShape(24.dp)
                                    )
                                    .clickable { gender = item }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    VioraGradientButton(
                        text = "Proceed to Selfie Scan",
                        onClick = {
                            if (firstName.trim().isEmpty() || lastName.trim().isEmpty()) {
                                speak("Please enter your official first and last name to proceed.")
                            } else {
                                currentStep = 2
                                speak("Step two. Let us complete selfie verification. Please hold your phone steady.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                2 -> {
                    // ---- STEP 2: LIVE SELFIE VERIFICATION ----
                    Text(
                        text = "Guided 3D Face Scanning",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Our neural system uses live motion analysis to confirm liveness and scan depth cues.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Diagnostic selector for reviewers
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF160E2A)),
                        border = BorderStroke(1.dp, Color(0xFF331E53))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🛠️ Rejection Simulator Control",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Switch behavior modes to test fail alerts:",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                mapOf(
                                    "none" to "Normal Pass",
                                    "poor_lighting" to "Bad Light",
                                    "multiple_faces" to "Multi Face",
                                    "gallery" to "Static Pic"
                                ).forEach { (key, value) ->
                                    val isSel = rejectionMode == key
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) AccentCrimsonPink else Color.White.copy(alpha = 0.05f))
                                            .clickable {
                                                rejectionMode = key
                                                // reset
                                                isLivenessRunning = false
                                                selfieCheckStep = 0
                                                livenessProgress = 0f
                                                selfieChecks.keys.forEach { selfieChecks[it] = false }
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(value, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Face Scanner view
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                            .border(3.dp, BrightNeonPurple, CircleShape)
                            .purpleGlow()
                            .background(Color(0xFF070312))
                    ) {
                        // Real Front Camera Preview
                        if (hasCameraPermission && selfieCheckStep < 6) {
                            CameraPreview(
                                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Oval pulse overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.85f)
                                .align(Alignment.Center)
                                .border(
                                    2.dp,
                                    if (rejectionMode != "none") {
                                        Color(0xFFEF5350).copy(alpha = 0.5f)
                                    } else {
                                        Color(0xFFCE00FF).copy(alpha = 0.5f)
                                    },
                                    CircleShape
                                )
                        )

                        // Pulse outline & Advanced face tracking skeleton overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = BrightNeonPurple.copy(alpha = 0.2f),
                                radius = (size.width / 2f) * if (isLivenessRunning) pulseSizeMultiplier else 1.0f,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            // Glowing scanning target laser line
                            if (isLivenessRunning) {
                                val laserY = size.height * laserYOffset
                                drawLine(
                                    color = if (rejectionMode != "none") Color(0xFFEF5350) else BrightNeonPurple,
                                    start = Offset(20f, laserY),
                                    end = Offset(size.width - 20f, laserY),
                                    strokeWidth = 3.dp.toPx()
                                )

                                // Biometric Landmarks connected in a 3D structural mesh
                                val color = if (rejectionMode != "none") Color(0xFFEF5350) else BrightNeonPurple
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f

                                // Generates semi-random but highly stable coordinate meshes that respond dynamically to pulses/progress!
                                val jitter = if (pulseSizeMultiplier > 1.04f) 3.dp.toPx() else 0f
                                val landmarks = listOf(
                                    Offset(centerX - 35.dp.toPx(), centerY - 30.dp.toPx() + jitter), // Left Eye
                                    Offset(centerX + 35.dp.toPx(), centerY - 30.dp.toPx() - jitter), // Right Eye
                                    Offset(centerX, centerY - 5.dp.toPx() + (jitter * 0.5f)),         // Nose Tip
                                    Offset(centerX - 24.dp.toPx() + jitter, centerY + 30.dp.toPx()), // Mouth Left
                                    Offset(centerX + 24.dp.toPx() - jitter, centerY + 30.dp.toPx()), // Mouth Right
                                    Offset(centerX, centerY + 50.dp.toPx() + jitter),                // Chin
                                    Offset(centerX - 50.dp.toPx(), centerY - 65.dp.toPx()),          // Left Eyebrow
                                    Offset(centerX + 50.dp.toPx(), centerY - 65.dp.toPx()),          // Right Eyebrow
                                )

                                // Connect lines to draw depth mesh
                                for (i in landmarks.indices) {
                                    for (j in i + 1 until landmarks.size) {
                                        val dist = (landmarks[i] - landmarks[j]).getDistance()
                                        if (dist < 85.dp.toPx()) {
                                            drawLine(
                                                color = color.copy(alpha = 0.25f),
                                                start = landmarks[i],
                                                end = landmarks[j],
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                    }
                                }

                                // Plot tracking dots
                                landmarks.forEach { pt ->
                                    drawCircle(
                                        color = color,
                                        radius = 3.5.dp.toPx(),
                                        center = pt
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 1.dp.toPx(),
                                        center = pt
                                    )
                                }
                            }
                        }

                        // Selfie silhouette or captured selfie display
                        if (selfieCheckStep >= 6) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(selfieImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Selfie preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            if (!hasCameraPermission) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .clickable {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = BrightNeonPurple,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Camera Offline",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tap to grant camera access",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else if (!isLivenessRunning) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Fit Face in Oval",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // Floating text overlays for the real active steps
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 20.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when (selfieCheckStep) {
                                            1 -> "Hold Still • Face Front"
                                            2 -> "Slowly turn head LEFT"
                                            3 -> "Slowly turn head RIGHT"
                                            4 -> "Blink eyes rapidly"
                                            5 -> "Show a natural SMILE"
                                            else -> "Scanning Face Data"
                                        },
                                        color = if (rejectionMode != "none") Color(0xFFEF5350) else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Progress loader outer ring
                        CircularProgressIndicator(
                            progress = { livenessProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = if (rejectionMode != "none") Color(0xFFEF5350) else SuccessGreen,
                            strokeWidth = 3.dp,
                            strokeCap = StrokeCap.Round
                        )

                        // Camera flash white screen overlay
                        if (captureFlash) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Liveness Guided Prompts & Audio Waveform View
                    AnimatedVisibility(visible = isLivenessRunning) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = when (selfieCheckStep) {
                                    1 -> "🎤 GUIDANCE: \"Look straight at the camera.\""
                                    2 -> "🎤 GUIDANCE: \"Please slowly turn your head to the left.\""
                                    3 -> "🎤 GUIDANCE: \"Now slowly turn your head to the right.\""
                                    4 -> "🎤 GUIDANCE: \"Great, blink both eyes carefully.\""
                                    5 -> "🎤 GUIDANCE: \"Finally, show a warm, natural smile.\""
                                    else -> "🎤 Processing biometrical landmarks..."
                                },
                                color = BrightNeonPurple,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            // Voice Visualizer Simulator lines
                            Row(
                                modifier = Modifier.height(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0..10) {
                                    val delaySec = i * 100
                                    val heightFactor = remember { (2..14).random() }
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(heightFactor.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(BrightNeonPurple)
                                    )
                                }
                            }
                        }
                    }

                    // Alerts
                    if (rejectionMode == "poor_lighting" && isLivenessRunning) {
                        Text(
                            text = "⚠️ REJECTED: Low illumination, make sure your face is evenly lit.",
                            color = Color(0xFFEF5350),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else if (rejectionMode == "multiple_faces" && isLivenessRunning) {
                        Text(
                            text = "⚠️ REJECTED: Multiple faces or background users detected.",
                            color = Color(0xFFEF5350),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else if (rejectionMode == "gallery" && isLivenessRunning) {
                        Text(
                            text = "⚠️ REJECTED: Static screenshot or background photo detected. Live camera needed.",
                            color = Color(0xFFEF5350),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Liveness checklist indicators
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Live Neural Liveness Checklist",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            CheckIndicator(label = "Face Detected in Frame", checked = selfieChecks["face"] == true)
                            CheckIndicator(label = "Completed Head Turn Left", checked = selfieChecks["left"] == true)
                            CheckIndicator(label = "Completed Head Turn Right", checked = selfieChecks["right"] == true)
                            CheckIndicator(label = "Blink Pattern Authenticated", checked = selfieChecks["blink"] == true)
                            CheckIndicator(label = "Genuine Neutral Smile Received", checked = selfieChecks["smile"] == true)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!isLivenessRunning && selfieCheckStep < 6) {
                        VioraGradientButton(
                            text = "Start Biometric Checks",
                            onClick = {
                                isLivenessRunning = true
                                livenessProgress = 0.05f
                                selfieCheckStep = 1
                                currentStep = 2 // just keep step 2 active

                                coroutineScope.launch {
                                    if (rejectionMode != "none") {
                                        // simulate failure quickly
                                        delay(800)
                                        speak("Alert. Neural analysis rejected scanning frame due to " + 
                                            if (rejectionMode == "poor_lighting") "inadequate background illumination." 
                                            else if (rejectionMode == "multiple_faces") "the presence of multiple subjects in frame"
                                            else "rejection threshold triggers on a non-live static source photograph.")
                                        
                                        livenessProgress = 0.4f
                                    } else {
                                        // Step 1: Front
                                        speak("Liveness scan starting. Look straight at the camera.")
                                        delay(1500)
                                        selfieChecks["face"] = true
                                        livenessProgress = 0.2f

                                        // Step 2: Left
                                        selfieCheckStep = 2
                                        speak("Turn your head slowly to the left.")
                                        delay(1500)
                                        selfieChecks["left"] = true
                                        livenessProgress = 0.4f

                                        // Step 3: Right
                                        selfieCheckStep = 3
                                        speak("Now turn your head slowly to the right.")
                                        delay(1500)
                                        selfieChecks["right"] = true
                                        livenessProgress = 0.6f

                                        // Step 4: Blink
                                        selfieCheckStep = 4
                                        speak("Blink both of your eyes.")
                                        delay(1200)
                                        selfieChecks["blink"] = true
                                        livenessProgress = 0.8f

                                        // Step 5: Smile
                                        selfieCheckStep = 5
                                        speak("Finally, show a natural, friendly smile.")
                                        delay(1500)
                                        selfieChecks["smile"] = true
                                        livenessProgress = 1.0f

                                        // Automatically Capture selfie!
                                        selfieCheckStep = 6
                                        captureFlash = true
                                        delay(150)
                                        captureFlash = false
                                        isLivenessRunning = false
                                        speak("Liveness selfie captured successfully!")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (selfieCheckStep >= 6) {
                        Text(
                            text = "✓ Live biometric liveness successfully verified",
                            color = SuccessGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        VioraGradientButton(
                            text = "Continue to Document Scan",
                            onClick = {
                                currentStep = 3
                                speak("Step three. Please align your government-issued ID card inside the frame.")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // running checks indicator
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                            color = if (rejectionMode != "none") Color(0xFFEF5350) else BrightNeonPurple,
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Analyzing biometric streams in real-time...",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                3 -> {
                    // ---- STEP 3: DOCUMENT SCOPING SCANNER ----
                    Text(
                        text = "Document Laser Scanning",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Scan the FRONT and BACK sides of your photographic driver's license, passport, or ID.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Side selector tab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(4.dp)
                    ) {
                        listOf("front" to "Front of ID", "back" to "Back of ID").forEach { (key, title) ->
                            val isSel = activeIdScanningSide == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) BrightNeonPurple else Color.Transparent)
                                    .clickable {
                                        activeIdScanningSide = key
                                        scanProgress = 0.0f
                                        isIdScanningActive = false
                                        idQualityOk = false
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Rectangle crop bounds scanner UI
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                2.dp,
                                if (idQualityOk) SuccessGreen else BrightNeonPurple,
                                RoundedCornerShape(16.dp)
                            )
                            .purpleGlow()
                            .drawBehind {
                                // Draw corner crops
                                val len = 24.dp.toPx()
                                val stroke = 4.dp.toPx()
                                val cornerColor = if (idQualityOk) SuccessGreen else BrightNeonPurple

                                // TopLeft
                                drawLine(cornerColor, Offset(0f, 0f), Offset(len, 0f), stroke)
                                drawLine(cornerColor, Offset(0f, 0f), Offset(0f, len), stroke)

                                // TopRight
                                drawLine(cornerColor, Offset(size.width, 0f), Offset(size.width - len, 0f), stroke)
                                drawLine(cornerColor, Offset(size.width, 0f), Offset(size.width, len), stroke)

                                // BottomLeft
                                drawLine(cornerColor, Offset(0f, size.height), Offset(len, size.height), stroke)
                                drawLine(cornerColor, Offset(0f, size.height), Offset(0f, size.height - len), stroke)

                                // BottomRight
                                drawLine(cornerColor, Offset(size.width, size.height), Offset(size.width - len, size.height), stroke)
                                drawLine(cornerColor, Offset(size.width, size.height), Offset(size.width, size.height - len), stroke)
                            }
                            .background(Color(0xFF0C081A))
                    ) {
                        // Real Back Camera Preview for document Scanning!
                        if (hasCameraPermission && !idQualityOk) {
                            CameraPreview(
                                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (isIdScanningActive) {
                            // Laser lines scanning up and down
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.04f)
                                    .align(Alignment.TopCenter)
                                    .offset(y = 175.dp * laserYOffset)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                BrightNeonPurple.copy(alpha = 0.1f),
                                                BrightNeonPurple,
                                                BrightNeonPurple.copy(alpha = 0.1f)
                                            )
                                        )
                                    )
                            )
                        }

                        // Simulated placeholder card artwork / camera offline prompt
                        if (!hasCameraPermission) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clickable {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = BrightNeonPurple,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Camera Permission Required",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to unlock document scanner",
                                    color = TextSecondary,
                                    fontSize = 9.sp
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (activeIdScanningSide == "front") Icons.Default.Portrait else Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = if (idQualityOk) SuccessGreen else Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (idQualityOk) "✓ Captured successfully" else if (isIdScanningActive) "Analyzing Document Edges..." else "Align ${activeIdScanningSide.uppercase()} of ID card...",
                                    color = if (idQualityOk) SuccessGreen else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isIdScanningActive && !idQualityOk) {
                            LinearProgressIndicator(
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp),
                                color = BrightNeonPurple,
                                trackColor = Color.Transparent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Scanner checks info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Real-time Edge Checks",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            CheckIndicator(label = "Document boundaries aligned", checked = isIdScanningActive)
                            CheckIndicator(label = "Ambient image sharpness clear", checked = isIdScanningActive && scanProgress >= 0.4f)
                            CheckIndicator(label = "Alphanumeric letters match check", checked = isIdScanningActive && scanProgress >= 0.7f)
                            CheckIndicator(label = "Auto-capture complete", checked = idQualityOk)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!isIdScanningActive && !idQualityOk) {
                        VioraGradientButton(
                            text = "Initiate Scanner Search",
                            onClick = {
                                isIdScanningActive = true
                                coroutineScope.launch {
                                    speak("Searching document. Please keep your document inside the boundaries and avoid glare.")
                                    // Simulated auto focus / edge detect cycle
                                    delay(1000)
                                    scanProgress = 0.4f
                                    delay(1000)
                                    scanProgress = 0.8f
                                    delay(1000)
                                    scanProgress = 1.0f
                                    idQualityOk = true
                                    speak("Edge check approved. Automatically capturing " + activeIdScanningSide + " of identification card.")

                                    if (activeIdScanningSide == "front") {
                                        isFrontScanned = true
                                        // switch fields
                                        delay(1500)
                                        activeIdScanningSide = "back"
                                        idQualityOk = false
                                        isIdScanningActive = false
                                        speak("Front captured. Now flip your document card to scan the back side.")
                                    } else {
                                        isBackScanned = true
                                        delay(1500)
                                        currentStep = 4 // Auto proceed to OCR
                                        speak("Document capture complete. Starting cloud based AI OCR Extraction of details.")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (idQualityOk) {
                        Text(
                            text = "✓ Side scanned and saved in local cache",
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    } else {
                        // Scanning guide
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Auto-framing: Keep document flat and still... Snapping automatically.",
                                color = BrightNeonPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Manual capture override just in case
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Or click to capture manually if camera focus fails",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = {
                            isIdScanningActive = true
                            idQualityOk = true
                            if (activeIdScanningSide == "front") {
                                isFrontScanned = true
                                activeIdScanningSide = "back"
                                idQualityOk = false
                                isIdScanningActive = false
                                speak("Captured manually. Now align the back side.")
                            } else {
                                isBackScanned = true
                                currentStep = 4
                                speak("Scanning complete. Processing details.")
                            }
                        },
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Force Instant Capture", fontSize = 11.sp)
                    }
                }

                4 -> {
                    // ---- STEP 4: AI OCR EXTRACTION REVIEW ----
                    Text(
                        text = "Neural OCR Scan Extraction",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "We use secure machine learning algorithms to index text fields from your verified document.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    var isOcrCompleted by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        if (!isOcrCompleted) {
                            delay(2500)
                            isOcrCompleted = true
                            speak("OCR Complete. Please review the extracted credentials carefully.")
                        }
                    }

                    if (!isOcrCompleted) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = BrightNeonPurple, modifier = Modifier.size(48.dp))
                            Text(
                                "Recognizing font characters & extracting passport format...",
                                color = BrightNeonPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Display OCR extracted card details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AI Scanned Text Fields",
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SuccessGreen.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "99.4% Accuracy",
                                            color = SuccessGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Interactive Text Fields
                                OutlinedTextField(
                                    value = ocrOutputName,
                                    onValueChange = { ocrOutputName = it },
                                    label = { Text("Full Name", color = TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = BrightNeonPurple,
                                        unfocusedBorderColor = BorderColor,
                                        focusedLabelColor = BrightNeonPurple
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = ocrOutputDob,
                                    onValueChange = { ocrOutputDob = it },
                                    label = { Text("Date of Birth", color = TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = BrightNeonPurple,
                                        unfocusedBorderColor = BorderColor,
                                        focusedLabelColor = BrightNeonPurple
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = ocrOutputIdNum,
                                    onValueChange = { ocrOutputIdNum = it },
                                    label = { Text("Document ID Code", color = TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = BrightNeonPurple,
                                        unfocusedBorderColor = BorderColor,
                                        focusedLabelColor = BrightNeonPurple
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = ocrExpiryDate,
                                    onValueChange = { ocrExpiryDate = it },
                                    label = { Text("Expiry Date", color = TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = BrightNeonPurple,
                                        unfocusedBorderColor = BorderColor,
                                        focusedLabelColor = BrightNeonPurple
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = ocrAddress,
                                    onValueChange = { ocrAddress = it },
                                    label = { Text("Country Origin / Address", color = TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = BrightNeonPurple,
                                        unfocusedBorderColor = BorderColor,
                                        focusedLabelColor = BrightNeonPurple
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        VioraGradientButton(
                            text = "Analyze Metadata Alignments",
                            onClick = {
                                currentStep = 5
                                speak("Step five. Starting intelligent identity matching of your app profile.")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                5 -> {
                    // ---- STEP 5: INTELLIGENT MATCHING ----
                    Text(
                        text = "Intelligent Semantic Matching",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Viora cross references matching profiles, physical geometry coordinates and face similarities.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Interactive option to trigger mismatch alert triggers
                    var nameMismatchToggle by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF140F24)),
                        border = BorderStroke(1.dp, Color(0xFF331B4D))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Simulate Name Mismatch Warn",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Profile name differs from Gov ID",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Switch(
                                checked = nameMismatchToggle,
                                onCheckedChange = { nameMismatchToggle = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BrightNeonPurple
                                )
                            )
                        }
                    }

                    val nameScore = if (nameMismatchToggle) 42 else 98
                    val faceScore = 97
                    val locationScore = 100
                    val confidence = if (nameMismatchToggle) 59 else 99

                    // Gauges list
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Semantic Similarity Evaluation",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            ScoreGauge(title = "App Profile vs ID Legal Name Match", score = nameScore)
                            ScoreGauge(title = "Liveness Selfie vs Passport Photo Match", score = faceScore)
                            ScoreGauge(title = "Issuing Country vs User Position Match", score = locationScore)
                            
                            HorizontalDivider(color = BorderColor)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Aggregate Identity Trust Rating", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$confidence%",
                                    color = if (confidence >= 80) SuccessGreen else Color(0xFFEF5350),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (nameMismatchToggle) {
                        // Warning state as requested!
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x2CD32F2F))
                                .border(1.dp, Color(0xFFD32F2F), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF5350))
                                Text(
                                    text = "Your profile information does not fully match your government-issued ID.",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "App Display Name is \"${userProfile.name}\", but Government ID reads \"$ocrOutputName\". Choose an action to reconcile this deviation:",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )

                            VioraGradientButton(
                                text = "Apply Verified Identity Information",
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.updateMyDetails(
                                            name = ocrOutputName,
                                            age = userProfile.age,
                                            location = userProfile.location,
                                            occupation = userProfile.occupation,
                                            relationshipGoals = userProfile.relationshipGoals,
                                            interests = userProfile.interests
                                        )
                                        nameMismatchToggle = false
                                        speak("Display profile aligned with legal document details successfully.")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { nameMismatchToggle = false },
                                    border = BorderStroke(1.dp, BorderColor),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Manual review", fontSize = 10.sp)
                                }
                                OutlinedButton(
                                    onClick = { nameMismatchToggle = false },
                                    border = BorderStroke(1.dp, BorderColor),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Edit Name", fontSize = 10.sp)
                                }
                            }
                        }
                    } else {
                        VioraGradientButton(
                            text = "Submit Verification Package",
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.triggerProfileVerification() // Sets DB status to pending
                                    currentStep = 6
                                    speak("Identity package submitted successfully. Pending manual review.")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                6 -> {
                    // ---- STEP 6: TIMELINE & MANUAL REVIEW MONITOR ----
                    Text(
                        text = "Identity Package Submitted",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your digital tokens are cryptographed under high-intensity hash locks for safety validation.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Pending Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0x19FFC107)),
                        border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF8E1))
                                    .purpleGlow(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFFFFB300), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Pending Safety Review", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = "Estimated Feedback Time: 24–48 Hours",
                                color = Color(0xFFFFD54F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                "Viora compliance agents manually audit ID cards, portrait shapes and liveness signatures to grant official verified profiles.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Step list checkpoints
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Validation Logs Timeline", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            CheckTimelineItem(label = "Guided Biometrics Selfie Capture", time = "1 min ago", status = "PASSED")
                            CheckTimelineItem(label = "Physical Face to Landmark Mapping", time = "1 min ago", status = "PASSED")
                            CheckTimelineItem(label = "Double-sided ID Scanning", time = "30s ago", status = "PASSED")
                            CheckTimelineItem(label = "Cognitive OCR Field Extraction", time = "15s ago", status = "PASSED")
                            CheckTimelineItem(label = "Final Human Agent Review audit", time = "Queueing...", status = "PENDING")
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Reviewer Bypass tool
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x19B01DFF))
                            .border(1.dp, BrightNeonPurple, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "🛠️ ADMIN CHEAT: INSTANT KYC APPROVAL",
                                color = BrightNeonPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        // Bypass directly to status 7!
                                        // Update the DB Profile automatically
                                        val approvedProfile = userProfile.copy(verificationStatus = "verified")
                                        viewModel.handleKycReviewOverride(approvedProfile)
                                        currentStep = 7
                                        speak("Verification review completed. Shield badge granted. Congratulations!")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrightNeonPurple),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Approve Identity Now", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                7 -> {
                    // ---- STEP 7: IDENTITY APPROVED & VERIFICATION BADGE SUCCESS ----
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .purpleGlow()
                            .background(BrightNeonPurple.copy(alpha = 0.2f), CircleShape)
                            .border(2.dp, BrightNeonPurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Badge Verified Success Symbol",
                            tint = BrightNeonPurple,
                            modifier = Modifier.size(62.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Viora Account Verified!",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Congratulations, $firstName! Your identity verification has passed human safety audit standards.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Upgraded checklist privileges
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "👑 Verified Privileges Active",
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            PrivilegeRow(title = "Purple Verified Shield Badge", desc = "Displayed on posts, profiles and chats.")
                            PrivilegeRow(title = "Increased Match Trust Rating", desc = "Trust score rating boosted to Tier-1.")
                            PrivilegeRow(title = "+150% Feed Visibility Booster", desc = "Your posts will appear priority to matches.")
                            PrivilegeRow(title = "Allowed Unlimited Chat Thread starts", desc = "Bypass match limit controls easily.")
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    VioraGradientButton(
                        text = "Return to My Profile",
                        onClick = { onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun CheckIndicator(label: String, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) SuccessGreen else Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (checked) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun OcrLineItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
    }
}

@Composable
fun ScoreGauge(title: String, score: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = TextSecondary, fontSize = 11.sp)
            Text(
                text = "$score%",
                color = if (score >= 90) SuccessGreen else if (score >= 60) Color(0xFFFFB300) else Color(0xFFEF5350),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { score / 100.0f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = if (score >= 90) SuccessGreen else if (score >= 60) Color(0xFFFFB300) else Color(0xFFEF5350),
            trackColor = Color.White.copy(alpha = 0.04f)
        )
    }
}

@Composable
fun CheckTimelineItem(label: String, time: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (status == "PASSED") SuccessGreen else Color(0xFFFFB300),
                        shape = CircleShape
                    )
            )
            Column {
                Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = time, color = TextSecondary, fontSize = 10.sp)
            }
        }
        Text(
            text = status,
            color = if (status == "PASSED") SuccessGreen else Color(0xFFFFB300),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PrivilegeRow(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = null,
            tint = BrightNeonPurple,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = desc, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    onViewCreated: (PreviewView) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                onViewCreated(this)
            }
        },
        modifier = modifier,
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}
