package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.database.UserProfileEntity
import com.example.ui.components.VioraGradientButton
import com.example.ui.components.VioraFlowRow
import com.example.ui.components.VioraInterestTag
import com.example.ui.components.glassmorphic
import com.example.ui.components.purpleGlow
import com.example.ui.theme.*
import com.example.ui.viewmodel.VioraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: VioraViewModel) {
    val userProfileState by viewModel.userProfile.collectAsState()
    val profiles by viewModel.allProfiles.collectAsState()
    val posts by viewModel.posts.collectAsState()

    val profile = userProfileState ?: UserProfileEntity()

    var isEditing by remember { mutableStateOf(false) }

    // Forms Editable parameters
    var nameEdit by remember(profile) { mutableStateOf(profile.name) }
    var ageEdit by remember(profile) { mutableStateOf(profile.age.toString()) }
    var bioEdit by remember(profile) { mutableStateOf(profile.bio) }
    var locationEdit by remember(profile) { mutableStateOf(profile.location) }
    var occupationEdit by remember(profile) { mutableStateOf(profile.occupation) }
    var relationshipGoalsEdit by remember(profile) { mutableStateOf(profile.relationshipGoals) }

    var educationEdit by remember(profile.education) { mutableStateOf(profile.education) }
    var religionEdit by remember(profile.religionSetting) { mutableStateOf(profile.religionSetting) }
    var drinkingEdit by remember(profile.drinkingStatus) { mutableStateOf(profile.drinkingStatus) }
    var smokingEdit by remember(profile.smokingStatus) { mutableStateOf(profile.smokingStatus) }
    var petsEdit by remember(profile.petsStatus) { mutableStateOf(profile.petsStatus) }
    var childrenEdit by remember(profile.childrenStatus) { mutableStateOf(profile.childrenStatus) }
    var lookingForEdit by remember(profile.lookingForSetting) { mutableStateOf(profile.lookingForSetting) }
    var languagesEdit by remember(profile.languageFilterText) { mutableStateOf(profile.languageFilterText) }
    var heightEdit by remember(profile.heightFilterSlider) { mutableStateOf(profile.heightFilterSlider.toInt().toString()) }
    var zodiacSignEdit by remember(profile.zodiacSign) { mutableStateOf(profile.zodiacSign) }

    var hasVideoIntro by remember { mutableStateOf(true) }
    var hasVoiceIntro by remember { mutableStateOf(true) }
    var showVoiceRecorderDialog by remember { mutableStateOf(false) }
    var activeFullPhotoUrl by remember { mutableStateOf<String?>(null) }
    var activeFullPhotoTitle by remember { mutableStateOf("") }

    // Privacy States
    val incognitoMode by viewModel.incognitoMode.collectAsState()
    val hideOnlinePresence by viewModel.hideOnlinePresence.collectAsState()
    val obfuscateProximity by viewModel.obfuscateProximity.collectAsState()
    val hideMyAge by viewModel.hideMyAge.collectAsState()
    val allowSearchIndexing by viewModel.allowSearchIndexing.collectAsState()

    var showKycWizard by remember { mutableStateOf(false) }
    var showDataLedgerDialog by remember { mutableStateOf(false) }
    var showWipeConfirmationDialog by remember { mutableStateOf(false) }
    var dataWipedSuccessfully by remember { mutableStateOf(false) }

    // Expandable accordion section state trackers
    var sectionProfileAttrExpanded by remember { mutableStateOf(false) }
    var sectionCustomizationExpanded by remember { mutableStateOf(false) }
    var sectionDiscoveryExpanded by remember { mutableStateOf(false) }
    var sectionSafetyExpanded by remember { mutableStateOf(false) }
    var sectionActivityCenterExpanded by remember { mutableStateOf(false) }
    var sectionChatExpanded by remember { mutableStateOf(false) }
    var sectionGroupsEventsExpanded by remember { mutableStateOf(false) }
    var sectionNotificationsExpanded by remember { mutableStateOf(false) }
    var sectionAccountExpanded by remember { mutableStateOf(false) }

    // Advanced Profile Attributes States
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Real Cover photo upload contract
    val coverPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val localFilePath = saveUriToInternalStorage(context, uri)
                if (localFilePath != null) {
                    viewModel.updateUserProfile(profile.copy(coverPhotoUrl = localFilePath))
                }
            }
        }
    )

    // Profile Photo Launcher
    val profilePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val localFilePath = saveUriToInternalStorage(context, uri)
                if (localFilePath != null) {
                    val currentPics = profile.profilePics.toMutableList()
                    if (currentPics.isEmpty()) {
                        currentPics.add(localFilePath)
                    } else {
                        currentPics[0] = localFilePath
                    }
                    viewModel.updateUserProfile(profile.copy(profilePics = currentPics))
                }
            }
        }
    )

    // Gallery Photo Launcher
    val galleryPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val localFilePath = saveUriToInternalStorage(context, uri)
                if (localFilePath != null) {
                    val currentPics = profile.profilePics.toMutableList()
                    if (currentPics.size < 6) {
                        currentPics.add(localFilePath)
                        viewModel.updateUserProfile(profile.copy(profilePics = currentPics))
                    }
                }
            }
        }
    )

    // Dynamic accent color parsing representing premium customized settings!
    val activeAccentColor = remember(profile.activeAccentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(profile.activeAccentColorHex))
        } catch (_: Exception) {
            PrimaryPinkPurple
        }
    }

    val coverPhotoUrl = profile.coverPhotoUrl
    val zodiacSign = profile.zodiacSign
    val drinkingStatus = profile.drinkingStatus
    val smokingStatus = profile.smokingStatus
    val fitnessLevel = profile.fitnessLevel
    val petsStatus = profile.petsStatus
    val childrenStatus = profile.childrenStatus
    val relationshipStatus = profile.relationshipStatus
    val lookingForSetting = profile.lookingForSetting

    // Customizable Inputs
    var pronounsInput by remember(profile.pronounsInput) { mutableStateOf(profile.pronounsInput) }
    var religionSetting by remember(profile.religionSetting) { mutableStateOf(profile.religionSetting) }
    var loveLanguageSetting by remember(profile.loveLanguageSetting) { mutableStateOf(profile.loveLanguageSetting) }
    var favoriteMusic by remember(profile.favoriteMusic) { mutableStateOf(profile.favoriteMusic) }
    var favoriteMovies by remember(profile.favoriteMovies) { mutableStateOf(profile.favoriteMovies) }
    var favoriteBooks by remember(profile.favoriteBooks) { mutableStateOf(profile.favoriteBooks) }
    var travelInterests by remember(profile.travelInterests) { mutableStateOf(profile.travelInterests) }
    var instagramLink by remember(profile.instagramLink) { mutableStateOf(profile.instagramLink) }
    var spotifyLink by remember(profile.spotifyLink) { mutableStateOf(profile.spotifyLink) }

    // Customization Settings
    val profileThemeName = profile.profileThemeName
    val profileFrameName = profile.profileFrameName
    val customEmojiStatus = profile.customEmojiStatus
    val customBadgeSelection = profile.customBadgeSelection
    val activeStickerSelection = profile.activeStickerSelection
    val animateProfileBgEffect = profile.animateProfileBgEffect

    // Discovery Settings
    val verifiedOnlyMode = profile.verifiedOnlyMode
    val recentlyActiveOnlyMode = profile.recentlyActiveOnlyMode
    val onlineNowOnlyMode = profile.onlineNowOnlyMode
    val newMembersOnlyMode = profile.newMembersOnlyMode
    val hideInactiveProfilesMode = profile.hideInactiveProfilesMode
    val distanceRadiusSlider = profile.distanceRadiusSlider
    var countryFilterText by remember(profile.countryFilterText) { mutableStateOf(profile.countryFilterText) }
    var cityFilterText by remember(profile.cityFilterText) { mutableStateOf(profile.cityFilterText) }
    var languageFilterText by remember(profile.languageFilterText) { mutableStateOf(profile.languageFilterText) }
    var educationFilterText by remember(profile.educationFilterText) { mutableStateOf(profile.educationFilterText) }
    var occupationFilterText by remember(profile.occupationFilterText) { mutableStateOf(profile.occupationFilterText) }
    val heightFilterSlider = profile.heightFilterSlider
    val interestMatchingSlider = profile.interestMatchingSlider

    // Chat Settings Toggles
    val messageRequestsEnabled = profile.messageRequestsEnabled
    val readReceiptsToggle = profile.readReceiptsToggle
    val typingIndicatorsToggle = profile.typingIndicatorsToggle
    val messageReactionsToggle = profile.messageReactionsToggle
    val autoDeleteMessagesLabel = profile.autoDeleteMessagesLabel
    val screenshotPreventionToggle = profile.screenshotPreventionToggle
    val chatBackgroundThemeName = profile.chatBackgroundThemeName
    val pinnedConversationsCount = profile.pinnedConversationsCount
    val archivedChatsCount = profile.archivedChatsCount

    // Safety Features Toggles
    var trustedContactNumber by remember(profile.trustedContactNumber) { mutableStateOf(profile.trustedContactNumber) }
    val safetyCheckInReminders = profile.safetyCheckInReminders
    val spamProtectionEnabled = profile.spamProtectionEnabled
    val botDetectionEnabled = profile.botDetectionEnabled
    val catfishDetectionEnabled = profile.catfishDetectionEnabled
    var showSafetyEducationDrawer by remember { mutableStateOf(false) }
    var showPanicTriggeredDialog by remember { mutableStateOf(false) }
    var quickBlockUserInput by remember { mutableStateOf("") }
    var quickReportUserInput by remember { mutableStateOf("") }

    // Activity Center Stats
    var isSpotlightActive = profile.customBadgeSelection == "Spotlight Mode Active"
    var showWhoViewedMePanel by remember { mutableStateOf(false) }
    var showWhoLikedMePanel by remember { mutableStateOf(false) }
    val profileViewsCounter = profile.profileViews
    val profileLikesReceived = profile.likesReceived

    val simulatedVisitorsHistory = remember(profiles) {
        if (profiles.isNotEmpty()) {
            profiles.take(4).mapIndexed { idx, p ->
                p.name to when(idx) {
                    0 -> "5 mins ago"
                    1 -> "2 hours ago"
                    2 -> "Yesterday"
                    else -> "3 days ago"
                }
            }
        } else {
            listOf(
                "Emma Watson" to "5 mins ago",
                "Sophia Martinez" to "2 hours ago",
                "Sharon" to "Yesterday",
                "Isabella Wilson" to "Yesterday"
            )
        }
    }
    // Boost simulation
    var boostSecondsRemaining by remember { mutableStateOf(0) }
    LaunchedEffect(boostSecondsRemaining) {
        if (boostSecondsRemaining > 0) {
            delay(1000)
            boostSecondsRemaining -= 1
        }
    }

    // Notifications Toggles (10 pieces)
    val notifyNewMatch = profile.notifyNewMatch
    val notifyNewMessage = profile.notifyNewMessage
    val notifyVisitorAlerts = profile.notifyVisitorAlerts
    val notifyVerificationStatus = profile.notifyVerificationStatus
    val notifyProfileViewAlerts = profile.notifyProfileViewAlerts
    val notifyMentionAlerts = profile.notifyMentionAlerts
    val notifySafetyAlerts = profile.notifySafetyAlerts
    val notifyGroupNotifications = profile.notifyGroupNotifications
    val notifyEventNotifications = profile.notifyEventNotifications
    val notifyMarketingPreferences = profile.notifyMarketingPreferences

    // Group & Events states
    val mockCreatedGroupsCount = profile.postsCount
    val speedDatingRsvpList = remember(profile.rsvpEventsCsv) {
        profile.rsvpEventsCsv.split(",").filter { it.isNotEmpty() }.toSet()
    }

    // Account features states
    var accountEmailText by remember(profile.accountEmailText) { mutableStateOf(profile.accountEmailText) }
    var accountPhoneText by remember(profile.accountPhoneText) { mutableStateOf(profile.accountPhoneText) }
    val isTwoFactorEnabled = profile.isTwoFactorEnabled
    var backupCodesGenerated by remember(profile.backupCodesCsv) {
        mutableStateOf(profile.backupCodesCsv.split(",").filter { it.isNotEmpty() })
    }
    val userDetailsLogs = remember(profile.userDetailsLogsCsv) {
        profile.userDetailsLogsCsv.split("|||").filter { it.isNotEmpty() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkCharcoalBg)
                .statusBarsPadding()
        ) {
            // Profile Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Profile",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                // Save or edit profile toggle
                IconButton(
                    onClick = {
                        if (isEditing) {
                            viewModel.updateMyDetails(
                                name = nameEdit,
                                age = ageEdit.toIntOrNull() ?: profile.age,
                                location = locationEdit,
                                occupation = occupationEdit,
                                relationshipGoals = relationshipGoalsEdit,
                                interests = profile.interests
                            )
                            viewModel.updateMyBio(bioEdit)
                        }
                        isEditing = !isEditing
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF140B1F))
                        .border(1.dp, BorderColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = activeAccentColor
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 90.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // 1. Cover Photo & Profile Stack Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    // Back Cover Wallpaper
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverPhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile cover backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                            .clickable {
                                activeFullPhotoUrl = coverPhotoUrl
                                activeFullPhotoTitle = "Edit Cover Backdrop"
                            }
                    )

                    // Edit Cover Photo floating button (gorgeous premium outline pill)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 28.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable {
                                coverPhotoLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Edit Cover",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Edit Cover Photo",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Floating User Avatar overlapping on the left
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp)
                            .offset(y = (-10).dp)
                            .size(125.dp)
                            .purpleGlow()
                            .border(
                                width = 3.dp,
                                color = BrightNeonPurple,
                                shape = CircleShape
                            )
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(if (profile.profilePics.firstOrNull() == "me_1" || profile.profilePics.isEmpty()) "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=350" else profile.profilePics.firstOrNull())
                                .crossfade(true)
                                .build(),
                            contentDescription = "My Portrait Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .clickable {
                                    activeFullPhotoUrl = if (profile.profilePics.firstOrNull() == "me_1" || profile.profilePics.isEmpty()) "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=350" else profile.profilePics.firstOrNull()
                                    activeFullPhotoTitle = "My Profile Portrait"
                                }
                        )

                        // Camera uploader badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF140D22))
                                .border(1.5.dp, BrightNeonPurple, CircleShape)
                                .clickable {
                                    profilePhotoLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Change profile picture",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // User Identity text side-by-side
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 164.dp, bottom = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                modifier = Modifier.purpleGlow()
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (profile.verificationStatus == "verified") {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8D3BFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Verified badge",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = "Premium Crown Badge",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(3.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${profile.age}, ${profile.location}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(OnlineGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Online now",
                                color = OnlineGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Edit/Save toggle button right next to name
                    Button(
                        onClick = {
                            if (isEditing) {
                                viewModel.updateUserProfile(
                                    profile.copy(
                                        name = nameEdit,
                                        age = ageEdit.toIntOrNull() ?: profile.age,
                                        location = locationEdit,
                                        occupation = occupationEdit,
                                        bio = bioEdit,
                                        relationshipGoals = relationshipGoalsEdit,
                                        education = educationEdit,
                                        zodiacSign = zodiacSignEdit,
                                        religionSetting = religionEdit,
                                        drinkingStatus = drinkingEdit,
                                        smokingStatus = smokingEdit,
                                        petsStatus = petsEdit,
                                        childrenStatus = childrenEdit,
                                        lookingForSetting = lookingForEdit,
                                        languageFilterText = languagesEdit,
                                        heightFilterSlider = heightEdit.toFloatOrNull() ?: profile.heightFilterSlider
                                    )
                                )
                            }
                            isEditing = !isEditing
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEditing) SuccessGreen else Color(0xFF1B0F2B)
                        ),
                        border = BorderStroke(1.dp, if (isEditing) SuccessGreen else BorderColor),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 24.dp, bottom = 12.dp)
                            .height(36.dp)
                    ) {
                        Text(
                            text = if (isEditing) "Save" else "Edit",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Beautiful Action Row (Photos, Video, Voice, Verification, Boost)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Action 1: Add Photos
                        ProfileActionItem(
                            icon = Icons.Default.PhotoLibrary,
                            label = "Add Photos",
                            status = "(${profile.profilePics.size}/6)",
                            statusColor = BrightNeonPurple,
                            onClick = {
                                galleryPhotoLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        )

                        // Quick Action 2: Add Video
                        ProfileActionItem(
                            icon = Icons.Default.VideoCameraFront,
                            label = "Add Video",
                            status = "(${if (hasVideoIntro) "1" else "0"}/1)",
                            statusColor = BrightNeonPurple,
                            onClick = {
                                hasVideoIntro = !hasVideoIntro
                            }
                        )

                        // Quick Action 3: Voice Intro
                        ProfileActionItem(
                            icon = Icons.Default.Audiotrack,
                            label = "Voice Intro",
                            status = "(${if (hasVoiceIntro) "1" else "0"}/1)",
                            statusColor = if (hasVoiceIntro) OnlineGreen else TextSecondary,
                            onClick = {
                                showVoiceRecorderDialog = true
                            }
                        )

                        // Quick Action 4: Verification
                        ProfileActionItem(
                            icon = Icons.Default.VerifiedUser,
                            label = "Verification",
                            status = if (profile.verificationStatus == "verified") "Verified" else "Verify",
                            statusColor = if (profile.verificationStatus == "verified") OnlineGreen else AccentCrimsonPink,
                            onClick = {
                                showKycWizard = true
                            }
                        )

                        // Quick Action 5: Boost
                        ProfileActionItem(
                            icon = Icons.Default.FlashOn,
                            label = "Boost",
                            status = if (boostSecondsRemaining > 0) "${boostSecondsRemaining / 60}m" else "Boost",
                            statusColor = if (boostSecondsRemaining > 0) Color(0xFFFFC107) else BrightNeonPurple,
                            onClick = {
                                boostSecondsRemaining = 1800 // Trigger 30m boost
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. Profile Completion Card (Dynamically calculated progress wheel and bar)
                val filledFieldsCount = remember(profile, nameEdit, bioEdit, locationEdit, occupationEdit, educationEdit) {
                    listOf(
                        profile.name.isNotEmpty(),
                        profile.bio.isNotEmpty(),
                        profile.location.isNotEmpty(),
                        profile.occupation.isNotEmpty(),
                        profile.education.isNotEmpty(),
                        profile.zodiacSign.isNotEmpty(),
                        profile.religionSetting.isNotEmpty(),
                        profile.drinkingStatus.isNotEmpty() && profile.drinkingStatus != "Never",
                        profile.smokingStatus.isNotEmpty() && profile.smokingStatus != "Never",
                        profile.petsStatus.isNotEmpty(),
                        profile.childrenStatus.isNotEmpty(),
                        profile.relationshipGoals.isNotEmpty()
                    ).count { it }
                }

                val completionPercentage = remember(filledFieldsCount) {
                    (40 + (filledFieldsCount * 5)).coerceAtMost(100)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Progress ring on left
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(64.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { completionPercentage / 100f },
                                color = BrightNeonPurple,
                                strokeWidth = 5.dp,
                                trackColor = Color(0xFF1F1430),
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "$completionPercentage%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Text content and expand button
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Profile Completion",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.purpleGlow()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Complete your profile to get more matches",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Small linear progress bar
                            LinearProgressIndicator(
                                progress = { completionPercentage / 100f },
                                color = BrightNeonPurple,
                                trackColor = Color(0xFF1F1430),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                isEditing = true // trigger inline editing mode!
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8D3BFF)),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Improve Profile", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // About Me Card (Centered, clean)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "About Me",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.purpleGlow()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isEditing) bioEdit else profile.bio.ifEmpty { "I love traveling, coffee, and good conversations. Looking for someone real to build something beautiful." },
                            color = TextPrimary.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Details / Interactive edit forms
                if (isEditing) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Edit Profile Details",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.purpleGlow()
                            )
                            
                            OutlinedTextField(
                                value = nameEdit,
                                onValueChange = { nameEdit = it },
                                label = { Text("Display Name", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = ageEdit,
                                    onValueChange = { ageEdit = it },
                                    label = { Text("Age", color = TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        focusedBorderColor = BrightNeonPurple,
                                        unfocusedBorderColor = BorderColor,
                                        focusedContainerColor = Color(0xFF10081C)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = heightEdit,
                                    onValueChange = { heightEdit = it },
                                    label = { Text("Height (cm)", color = TextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        focusedBorderColor = BrightNeonPurple,
                                        unfocusedBorderColor = BorderColor,
                                        focusedContainerColor = Color(0xFF10081C)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = locationEdit,
                                onValueChange = { locationEdit = it },
                                label = { Text("Location", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = occupationEdit,
                                onValueChange = { occupationEdit = it },
                                label = { Text("Occupation", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = educationEdit,
                                onValueChange = { educationEdit = it },
                                label = { Text("Education", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = bioEdit,
                                onValueChange = { bioEdit = it },
                                label = { Text("Bio / About Me", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 4
                            )

                            OutlinedTextField(
                                value = relationshipGoalsEdit,
                                onValueChange = { relationshipGoalsEdit = it },
                                label = { Text("Looking For", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = zodiacSignEdit,
                                onValueChange = { zodiacSignEdit = it },
                                label = { Text("Zodiac Sign", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = religionEdit,
                                onValueChange = { religionEdit = it },
                                label = { Text("Religion", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = languagesEdit,
                                onValueChange = { languagesEdit = it },
                                label = { Text("Languages", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = drinkingEdit,
                                onValueChange = { drinkingEdit = it },
                                label = { Text("Drinking", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = smokingEdit,
                                onValueChange = { smokingEdit = it },
                                label = { Text("Smoking", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = childrenEdit,
                                onValueChange = { childrenEdit = it },
                                label = { Text("Children Status", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = petsEdit,
                                onValueChange = { petsEdit = it },
                                label = { Text("Pets Status", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = BrightNeonPurple,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = Color(0xFF10081C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Identity Checkpoints Grid of 12 detailed options pulling from Room DB
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Identity Checkpoints",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 2.dp).purpleGlow()
                        )

                        // Grid Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileDetailCard(
                                    icon = Icons.Default.Favorite,
                                    label = "Looking For",
                                    value = profile.relationshipGoals.ifEmpty { "Long-term" }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileDetailCard(
                                    icon = Icons.Default.Height,
                                    label = "Height",
                                    value = "${profile.heightFilterSlider.toInt()} cm"
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileDetailCard(
                                    icon = Icons.Default.Work,
                                    label = "Occupation",
                                    value = profile.occupation.ifEmpty { "Marketing Manager" }
                                )
                            }
                        }

                        // Grid Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileDetailCard(
                                    icon = Icons.Default.School,
                                    label = "Education",
                                    value = profile.education.ifEmpty { "Bachelor's" }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileDetailCard(
                                    icon = Icons.Default.SelfImprovement,
                                    label = "Religion",
                                    value = profile.religionSetting.ifEmpty { "Christian" }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileDetailCard(
                                    icon = Icons.Default.Language,
                                    label = "Languages",
                                    value = profile.languageFilterText.ifEmpty { "English" }
                                )
                            }
                        }

                        // Grid Row 3
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileDetailCard(
                                    icon = Icons.Default.LocalBar,
                                    label = "Drinking",
                                    value = profile.drinkingStatus.ifEmpty { "Socially" }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileDetailCard(
                                    icon = Icons.Default.SmokingRooms,
                                    label = "Smoking",
                                    value = profile.smokingStatus.ifEmpty { "No" }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileDetailCard(
                                    icon = Icons.Default.ChildCare,
                                    label = "Children",
                                    value = profile.childrenStatus.ifEmpty { "No" }
                                )
                            }
                        }

                        // Grid Row 4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileDetailCard(
                                    icon = Icons.Default.WbSunny,
                                    label = "Zodiac Sign",
                                    value = profile.zodiacSign.ifEmpty { "Leo" }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileDetailCard(
                                    icon = Icons.Default.Category,
                                    label = "Interests",
                                    value = "${profile.interests.size}+"
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileDetailCard(
                                    icon = Icons.Default.Pets,
                                    label = "Pets",
                                    value = profile.petsStatus.ifEmpty { "Dog lover" }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Stats Dashboard Grid (Views, Visitors, Likes received, Trust Score)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .glassmorphic(borderRadius = 18.dp)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(metric = "$profileViewsCounter", label = "Profile Views")
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderColor))
                    StatItem(metric = "128", label = "Visitors")
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderColor))
                    StatItem(metric = "$profileLikesReceived", label = "Likes Recv")
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderColor))
                    StatItem(
                        metric = if (profile.verificationStatus == "verified") "High" else "Medium",
                        label = "Trust Score"
                    )
                }

                // -------------------------------------------------------------------------------------
                // THE ORGANIZED COLLAPSIBLE ACCORDION SETTINGS
                // -------------------------------------------------------------------------------------

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    
                    // SECTION 1: Advanced Profile Attributes & Lifestyle
                    ExpandableSection(
                        title = "Advanced Profile & Lifestyle",
                        icon = Icons.Default.Face,
                        expanded = sectionProfileAttrExpanded,
                        onToggle = { sectionProfileAttrExpanded = !sectionProfileAttrExpanded }
                    ) {
                        // Zodiac Sign
                        Text("Zodiac Sign", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                            val zodiacs = listOf("Libra ♎", "Scorpio ♏", "Gemini ♊", "Leo ♌")
                            zodiacs.forEach { z ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (zodiacSign == z) activeAccentColor else Color(0xFF140D24))
                                        .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.updateUserProfile(profile.copy(zodiacSign = z)) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(z, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }

                        // Religion / Spirit
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Religion & Beliefs", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        OutlinedTextField(
                            value = religionSetting,
                            onValueChange = { 
                                religionSetting = it
                                viewModel.updateUserProfile(profile.copy(religionSetting = it))
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )

                        // Lifestyle Row selectors
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Lifestyle Metrics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        LifestyleSelectorRow(label = "Drinking Status", value = drinkingStatus, options = listOf("Never", "Socially", "Regularly"), onChange = { viewModel.updateUserProfile(profile.copy(drinkingStatus = it)) })
                        LifestyleSelectorRow(label = "Smoking Status", value = smokingStatus, options = listOf("Never", "Vape", "Occasional"), onChange = { viewModel.updateUserProfile(profile.copy(smokingStatus = it)) })
                        LifestyleSelectorRow(label = "Fitness Level", value = fitnessLevel, options = listOf("Low", "Moderate", "Highly Active"), onChange = { viewModel.updateUserProfile(profile.copy(fitnessLevel = it)) })
                        LifestyleSelectorRow(label = "Pets & Animals", value = petsStatus, options = listOf("None", "Has Cat 🐱", "Has Dog 🐶"), onChange = { viewModel.updateUserProfile(profile.copy(petsStatus = it)) })
                        LifestyleSelectorRow(label = "Children", value = childrenStatus, options = listOf("None", "Has Kids", "Maybe future"), onChange = { viewModel.updateUserProfile(profile.copy(childrenStatus = it)) })
                        LifestyleSelectorRow(label = "Relationship", value = relationshipStatus, options = listOf("Single", "Separated", "In union"), onChange = { viewModel.updateUserProfile(profile.copy(relationshipStatus = it)) })
                        LifestyleSelectorRow(label = "Looking For", value = lookingForSetting, options = listOf("Dating", "Friendship", "Marriage"), onChange = { viewModel.updateUserProfile(profile.copy(lookingForSetting = it)) })

                        // Personality & Love language
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Personality Type & Love Language", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        OutlinedTextField(
                            value = loveLanguageSetting,
                            onValueChange = { 
                                loveLanguageSetting = it
                                viewModel.updateUserProfile(profile.copy(loveLanguageSetting = it))
                            },
                            label = { Text("Love Language", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )

                        // Multi-line interest texts
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Culture & Favorites", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        
                        OutlinedTextField(
                            value = favoriteMusic,
                            onValueChange = { 
                                favoriteMusic = it
                                viewModel.updateUserProfile(profile.copy(favoriteMusic = it))
                            },
                            label = { Text("Favorite Music Genres", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = favoriteMovies,
                            onValueChange = { 
                                favoriteMovies = it
                                viewModel.updateUserProfile(profile.copy(favoriteMovies = it))
                            },
                            label = { Text("Favorite Movies / Shows", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = favoriteBooks,
                            onValueChange = { 
                                favoriteBooks = it
                                viewModel.updateUserProfile(profile.copy(favoriteBooks = it))
                            },
                            label = { Text("Favorite Books / Writers", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = travelInterests,
                            onValueChange = { 
                                travelInterests = it
                                viewModel.updateUserProfile(profile.copy(travelInterests = it))
                            },
                            label = { Text("Travel Interests", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )

                        // Social media connections
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Linked Social Integrations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        OutlinedTextField(
                            value = instagramLink,
                            onValueChange = { 
                                instagramLink = it
                                viewModel.updateUserProfile(profile.copy(instagramLink = it))
                            },
                            label = { Text("Instagram handle", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = "", tint = activeAccentColor, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = spotifyLink,
                            onValueChange = { 
                                spotifyLink = it
                                viewModel.updateUserProfile(profile.copy(spotifyLink = it))
                            },
                            label = { Text("Spotify Playlist handle", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = "", tint = activeAccentColor, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }

                    // SECTION 2: Profile Customization & Aesthetics
                    ExpandableSection(
                        title = "Profile Customization",
                        icon = Icons.Default.Brush,
                        expanded = sectionCustomizationExpanded,
                        onToggle = { sectionCustomizationExpanded = !sectionCustomizationExpanded }
                    ) {
                        // Color palette presets
                        Text("Profile Base Theme Accent", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val colorsList = listOf(
                                "Violet Glow" to "#7B1FA2",
                                "Crimson Aura" to "#EF5350",
                                "Emerald Mint" to "#00E676",
                                "Elite Gold" to "#FFD700",
                                "Cosmic Blue" to "#0288D1"
                            )
                            colorsList.forEach { (name, hex) ->
                                val colorItem = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { PrimaryPinkPurple }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(colorItem)
                                        .border(2.dp, if (profile.activeAccentColorHex == hex) Color.White else Color.Transparent, CircleShape)
                                        .clickable {
                                            viewModel.updateUserProfile(profile.copy(
                                                activeAccentColorHex = hex,
                                                profileThemeName = name
                                            ))
                                        }
                                )
                            }
                        }
                        Text("Current Theme: $profileThemeName", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))

                        // Avatar Framing Prestige Settings
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Profile Custom Frames", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val frames = listOf("None", "Neon Sparkle", "Gold Elite", "Platinum Border")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            frames.forEach { frame ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (profileFrameName == frame) activeAccentColor else Color(0xFF140D24))
                                        .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.updateUserProfile(profile.copy(profileFrameName = frame)) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(frame, color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }

                        // Custom status emojis picker
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Profile Status Emoji", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            val emojis = listOf("✨", "🔥", "🎧", "☕", "🏕️", "🚀")
                            emojis.forEach { emo ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (customEmojiStatus == emo) activeAccentColor.copy(alpha = 0.4f) else Color(0xFF140D24))
                                        .border(0.5.dp, BorderColor, CircleShape)
                                        .clickable { viewModel.updateUserProfile(profile.copy(customEmojiStatus = emo)) }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emo, fontSize = 15.sp)
                                }
                            }
                        }

                        // Interest custom sticker selection
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Profile Interest Stickers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val stickers = listOf("✈️ Wanderlust", "🍿 Movie Buff", "🐾 Pet Lover", "🧘 Zen Mind")
                        VioraFlowRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            stickers.forEach { stick ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (activeStickerSelection == stick) activeAccentColor else Color(0xFF1C0D2C))
                                        .border(0.5.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.updateUserProfile(profile.copy(activeStickerSelection = stick)) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(stick, color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Animated Profile Effects", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Show custom decorative particles behind photo", color = TextSecondary, fontSize = 10.sp)
                            }
                            Switch(
                                checked = animateProfileBgEffect,
                                onCheckedChange = { viewModel.updateUserProfile(profile.copy(animateProfileBgEffect = it)) },
                                colors = SwitchDefaults.colors(checkedTrackColor = activeAccentColor)
                            )
                        }
                    }

                    // SECTION 3: Premium Discovery Preferences
                    ExpandableSection(
                        title = "Discovery Preferences",
                        icon = Icons.Default.Search,
                        expanded = sectionDiscoveryExpanded,
                        onToggle = { sectionDiscoveryExpanded = !sectionDiscoveryExpanded }
                    ) {
                        ProfileToggleRow(title = "Verified Profiles Only", desc = "Filter out unverified review accounts", checked = verifiedOnlyMode) { viewModel.updateUserProfile(profile.copy(verifiedOnlyMode = it)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileToggleRow(title = "Recently Active Only", desc = "Prioritize people active in the past 24h", checked = recentlyActiveOnlyMode) { viewModel.updateUserProfile(profile.copy(recentlyActiveOnlyMode = it)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileToggleRow(title = "Online Now Only", desc = "Show only candidates with active green indicator", checked = onlineNowOnlyMode) { viewModel.updateUserProfile(profile.copy(onlineNowOnlyMode = it)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileToggleRow(title = "New Members Only", desc = "Highlight candidates joined this week", checked = newMembersOnlyMode) { viewModel.updateUserProfile(profile.copy(newMembersOnlyMode = it)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileToggleRow(title = "Hide Inactive Accounts", desc = "Automatically suppress ghost profiles", checked = hideInactiveProfilesMode) { viewModel.updateUserProfile(profile.copy(hideInactiveProfilesMode = it)) }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Distance Radius: ${distanceRadiusSlider.toInt()} miles", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Slider(
                            value = distanceRadiusSlider,
                            onValueChange = { viewModel.updateUserProfile(profile.copy(distanceRadiusSlider = it)) },
                            valueRange = 5f..150f,
                            colors = SliderDefaults.colors(thumbColor = activeAccentColor, activeTrackColor = activeAccentColor)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Minimum Height Filter: ${heightFilterSlider.toInt()} cm", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Slider(
                            value = heightFilterSlider,
                            onValueChange = { viewModel.updateUserProfile(profile.copy(heightFilterSlider = it)) },
                            valueRange = 140f..210f,
                            colors = SliderDefaults.colors(thumbColor = activeAccentColor, activeTrackColor = activeAccentColor)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Required Interest Match Ratio: ${interestMatchingSlider.toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Slider(
                            value = interestMatchingSlider,
                            onValueChange = { viewModel.updateUserProfile(profile.copy(interestMatchingSlider = it)) },
                            valueRange = 20f..100f,
                            colors = SliderDefaults.colors(thumbColor = activeAccentColor, activeTrackColor = activeAccentColor)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Geographic Customization", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = countryFilterText,
                                onValueChange = { 
                                    countryFilterText = it
                                    viewModel.updateUserProfile(profile.copy(countryFilterText = it))
                                },
                                label = { Text("Country", color = TextSecondary, fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cityFilterText,
                                onValueChange = { 
                                    cityFilterText = it
                                    viewModel.updateUserProfile(profile.copy(cityFilterText = it))
                                },
                                label = { Text("City", color = TextSecondary, fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Linguistic, occupation filters
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = languageFilterText,
                            onValueChange = { 
                                languageFilterText = it
                                viewModel.updateUserProfile(profile.copy(languageFilterText = it))
                            },
                            label = { Text("Filter by spoken languages", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = educationFilterText,
                            onValueChange = { 
                                educationFilterText = it
                                viewModel.updateUserProfile(profile.copy(educationFilterText = it))
                            },
                            label = { Text("Minimum Education Level", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = occupationFilterText,
                            onValueChange = { 
                                occupationFilterText = it
                                viewModel.updateUserProfile(profile.copy(occupationFilterText = it))
                            },
                            label = { Text("Occupation / Industry Filter", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }

                    // SECTION 4: Chat Engine & Encryption Preferences
                    ExpandableSection(
                        title = "Chat Settings & Encryption",
                        icon = Icons.Default.Message,
                        expanded = sectionChatExpanded,
                        onToggle = { sectionChatExpanded = !sectionChatExpanded }
                    ) {
                        ProfileToggleRow(title = "Filter Message Requests", desc = "Route non-matches to Requests inbox", checked = messageRequestsEnabled) { viewModel.updateUserProfile(profile.copy(messageRequestsEnabled = it)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileToggleRow(title = "Read Receipts Active", desc = "Allow others to review when tags are read", checked = readReceiptsToggle) { viewModel.updateUserProfile(profile.copy(readReceiptsToggle = it)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileToggleRow(title = "Live Typing Indicators", desc = "Display bubbles while responding on chats", checked = typingIndicatorsToggle) { viewModel.updateUserProfile(profile.copy(typingIndicatorsToggle = it)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileToggleRow(title = "Enable Emoji Reactions", desc = "Double tap messages to assign reaction symbols", checked = messageReactionsToggle) { viewModel.updateUserProfile(profile.copy(messageReactionsToggle = it)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileToggleRow(title = "Strict Screenshot Prevention", desc = "Block conversation captures on system windows", checked = screenshotPreventionToggle) { viewModel.updateUserProfile(profile.copy(screenshotPreventionToggle = it)) }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Auto-Delete Chat Transcripts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                            val options = listOf("Never", "After 24h", "After 7 Days")
                            options.forEach { opt ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (autoDeleteMessagesLabel == opt) activeAccentColor else Color(0xFF140D24))
                                        .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.updateUserProfile(profile.copy(autoDeleteMessagesLabel = opt)) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(opt, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Preset Chat Background Wallpapers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val wallpapers = listOf("Starry Midnight", "Nebula Purple", "Desert Minimal", "Lofi Sunset")
                        VioraFlowRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            wallpapers.forEach { wall ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (chatBackgroundThemeName == wall) activeAccentColor else Color(0xFF1B0D2C))
                                        .border(0.5.dp, BorderColor, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.updateUserProfile(profile.copy(chatBackgroundThemeName = wall)) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(wall, color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pinned Chats Transcripts count: $pinnedConversationsCount", color = TextSecondary, fontSize = 11.sp)
                            Text("Archived count: $archivedChatsCount", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    // SECTION 5: Emergency Safety Vault & Proactive Shields
                    ExpandableSection(
                        title = "Emergency Safety & Trust Vault",
                        icon = Icons.Default.Shield,
                        expanded = sectionSafetyExpanded,
                        onToggle = { sectionSafetyExpanded = !sectionSafetyExpanded }
                    ) {
                        // EMERGENCY PANIC BUTTON CARD
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x33B22222)),
                            border = BorderStroke(1.dp, Color(0xFFEF5350)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = "", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("RED ALERT PANIC TRIGGER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Tap to instantly notify pre-selected Trusted Contacts with your location telemetry, activate safety checks, and flag local admins.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showPanicTriggeredDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("ACTIVATE PANIC SWITCH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        // Trusted Contacts input
                        Text("Trusted Contacts Numbers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        OutlinedTextField(
                            value = trustedContactNumber,
                            onValueChange = { 
                                trustedContactNumber = it
                                viewModel.updateUserProfile(profile.copy(trustedContactNumber = it))
                            },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "", tint = activeAccentColor, modifier = Modifier.size(14.dp)) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Defensive Anti-Bot & Catfishing AI Protection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "Safety Check-In Reminders", desc = "Auto-ping if an outdoors date runs late", checked = safetyCheckInReminders) { viewModel.updateUserProfile(profile.copy(safetyCheckInReminders = it)) }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "Proactive Anti-Spam Shield", desc = "Auto-mute rapid-fire identical copy pings", checked = spamProtectionEnabled) { viewModel.updateUserProfile(profile.copy(spamProtectionEnabled = it)) }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "System Bot Detection Engine", desc = "Block automated scripts & scrapers", checked = botDetectionEnabled) { viewModel.updateUserProfile(profile.copy(botDetectionEnabled = it)) }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "Real-time Catfish Guardian", desc = "Match user selfies with active landmarks", checked = catfishDetectionEnabled) { viewModel.updateUserProfile(profile.copy(catfishDetectionEnabled = it)) }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Facial Identity Match Score:", color = TextSecondary, fontSize = 11.sp)
                            Text("98% (Extremely Reliable)", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Block/Report
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Quick Block / Purge User", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = quickBlockUserInput,
                                onValueChange = { quickBlockUserInput = it },
                                label = { Text("User name/ID", color = TextSecondary, fontSize = 9.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (quickBlockUserInput.isNotEmpty()) {
                                        quickBlockUserInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1124)),
                                border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Block", color = Color(0xFFEF5350), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Safety Education Center
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showSafetyEducationDrawer = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF130922)),
                            border = BorderStroke(0.5.dp, activeAccentColor.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "", tint = activeAccentColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("OPEN SAFETY EDUCATION CENTER", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // SECTION 6: VIP Premium Features & Visibility Boost
                    ExpandableSection(
                        title = "VIP Premium & Visibility Hub",
                        icon = Icons.Default.Favorite,
                        expanded = sectionActivityCenterExpanded,
                        onToggle = { sectionActivityCenterExpanded = !sectionActivityCenterExpanded }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x22DAA520)),
                            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = "", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("VIORA PRO MEMBERSHIP ACTIVATED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Text("Enjoy elite visibility weight, priority support, and advanced search indices.", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }

                        // Unlimited Swipes switch
                        ProfileToggleRow(title = "Unlimited Daily Swipes", desc = "Swipe candidates without any rate limits", checked = true) {}
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileToggleRow(title = "Priority Support Line", desc = "24/7 dedicated support review queued first", checked = true) {}
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileToggleRow(title = "Anonymous Incognito Surfing", desc = "Surround other profiles silently without footprint", checked = incognitoMode) {
                            viewModel.toggleIncognitoMode(it)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Boost Trigger Button
                            Button(
                                onClick = { boostSecondsRemaining = 1800 }, // 30 minutes boost
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x337B1FA2)),
                                border = BorderStroke(1.dp, Color(0xFFC2185B).copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = "", tint = Color(0xFFE91E63), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Boost Profile", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Spotlight Trigger Button
                            val isSpotlightStyle = isSpotlightActive
                            Button(
                                onClick = { isSpotlightActive = !isSpotlightActive },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSpotlightStyle) activeAccentColor else Color(0xFF221133)),
                                border = BorderStroke(1.dp, activeAccentColor.copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "", tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isSpotlightStyle) "Spotlight Active" else "Trigger Spotlight", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        // See Who Viewed/Liked panels
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showWhoViewedMePanel = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF140D22)),
                                border = BorderStroke(0.5.dp, BorderColor),
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Who Viewed Me?", color = Color.White, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { showWhoLikedMePanel = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF140D22)),
                                border = BorderStroke(0.5.dp, BorderColor),
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Who Liked Me?", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    // SECTION 7: Visitors History & Recent Matches
                    ExpandableSection(
                        title = "Activity Visitors & Matches",
                        icon = Icons.Default.People,
                        expanded = sectionActivityCenterExpanded,
                        onToggle = { sectionActivityCenterExpanded = !sectionActivityCenterExpanded }
                    ) {
                        // Horizontal visitors list
                        Text("Profile Visitors History (Recently)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(simulatedVisitorsHistory) { (name, time) ->
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .background(Color(0xFF11071B), RoundedCornerShape(12.dp))
                                        .border(0.5.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color.Gray)
                                        ) {
                                            Icon(Icons.Default.Face, contentDescription = "", tint = activeAccentColor, modifier = Modifier.fillMaxSize().padding(4.dp))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                                        Text(time, color = TextSecondary, fontSize = 9.sp)
                                    }
                                }
                            }
                        }

                        // Quick telemetry log
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Active Verification & Safety Alerts Feed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0C071A))
                                .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(6.dp)
                        ) {
                            Column {
                                val logs = listOf(
                                    "[Ok] Anti-bot scanner checks passed",
                                    "[Shield] Catfish Guardian matched current selfie with landmarks",
                                    "[Info] Account sync finalized with main servers",
                                    "[Security] Screenshot prevention module updated"
                                )
                                logs.forEach { log ->
                                    Text(log, color = Color.Gray, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, modifier = Modifier.padding(bottom = 2.dp))
                                }
                            }
                        }
                    }

                    // SECTION 8: Group Spaces & Matchmaking Events
                    ExpandableSection(
                        title = "Groups & Local Events Hub",
                        icon = Icons.Default.Event,
                        expanded = sectionGroupsEventsExpanded,
                        onToggle = { sectionGroupsEventsExpanded = !sectionGroupsEventsExpanded }
                    ) {
                        // Create join groups
                        Text("Groups & Forums Spaces", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Joined public/private spaces: $mockCreatedGroupsCount active", color = TextSecondary, fontSize = 11.sp)
                            Button(
                                onClick = { viewModel.updateUserProfile(profile.copy(postsCount = mockCreatedGroupsCount + 1)) },
                                colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, activeAccentColor),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Join/Create Space", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Speed dating nearby events list
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Authentic Dating Meets & nearby RSVP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val nearbyEvents = listOf("Speed Dating LA Downtown", "Sunset Rooftop Mixer", "Bookworms Coffee Meetup")
                        
                        nearbyEvents.forEach { ev ->
                            val isRsvped = speedDatingRsvpList.contains(ev)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF140B22))
                                    .border(0.5.dp, BorderColor, RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ev, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Reminder alerts active via push", color = TextSecondary, fontSize = 9.sp)
                                }
                                Button(
                                    onClick = {
                                        val nextList = if (isRsvped) {
                                            speedDatingRsvpList - ev
                                        } else {
                                            speedDatingRsvpList + ev
                                        }
                                        viewModel.updateUserProfile(profile.copy(rsvpEventsCsv = nextList.joinToString(",")))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isRsvped) Color(0xFF1B5E20) else activeAccentColor),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Text(if (isRsvped) "Going ✓" else "RSVP Now", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // SECTION 9: Ultimate 10-Point Push Notification Toggles
                    ExpandableSection(
                        title = "Push Alert & Mention Hub",
                        icon = Icons.Default.Notifications,
                        expanded = sectionNotificationsExpanded,
                        onToggle = { sectionNotificationsExpanded = !sectionNotificationsExpanded }
                    ) {
                        ProfileToggleRow(title = "New Match Alerts", desc = "Vibrate when a Mutual Match activates", checked = notifyNewMatch) { viewModel.updateUserProfile(profile.copy(notifyNewMatch = it)) }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "New Message Pings", desc = "Live alert for incoming transcripts", checked = notifyNewMessage) { viewModel.updateUserProfile(profile.copy(notifyNewMessage = it)) }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "Visitor Alerts", desc = "Ping immediately when someone visits profile", checked = notifyVisitorAlerts) { viewModel.updateUserProfile(profile.copy(notifyVisitorAlerts = it)) }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "Verification Status Updates", desc = "Hear when KYC selfies reviews finalize", checked = notifyVerificationStatus) { viewModel.updateUserProfile(profile.copy(notifyVerificationStatus = it)) }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "Profile View Alerts", desc = "Notify immediately on every search swipe view", checked = notifyProfileViewAlerts) { viewModel.updateUserProfile(profile.copy(notifyProfileViewAlerts = it)) }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "Mention Alerts", desc = "Ping if you are mentioned on forum channels", checked = notifyMentionAlerts) { viewModel.updateUserProfile(profile.copy(notifyMentionAlerts = it)) }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "Emergency Safety Alerts", desc = "Immediate broadcasts regarding user safety", checked = notifySafetyAlerts) { viewModel.updateUserProfile(profile.copy(notifySafetyAlerts = it)) }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "Group Announcements", desc = "Vibrate when group admins post announcements", checked = notifyGroupNotifications) { viewModel.updateUserProfile(profile.copy(notifyGroupNotifications = it)) }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "Event Reminders", desc = "Notify 15 mins before RSVPed meetups start", checked = notifyEventNotifications) { viewModel.updateUserProfile(profile.copy(notifyEventNotifications = it)) }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProfileToggleRow(title = "Exclusive Marketing Deals", desc = "Hear about premium credit discounts", checked = notifyMarketingPreferences) { viewModel.updateUserProfile(profile.copy(notifyMarketingPreferences = it)) }
                    }

                    // SECTION 10: Multi-Factor Account Security & GDPR Data
                    ExpandableSection(
                        title = "Account Security, Recovery & CCPA",
                        icon = Icons.Default.Lock,
                        expanded = sectionAccountExpanded,
                        onToggle = { sectionAccountExpanded = !sectionAccountExpanded }
                    ) {
                        // Change Email
                        Text("Registered Credentials", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = accountEmailText,
                            onValueChange = { 
                                accountEmailText = it
                                viewModel.updateUserProfile(profile.copy(accountEmailText = it))
                            },
                            label = { Text("Account Email", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "", tint = activeAccentColor, modifier = Modifier.size(14.dp)) }
                        )

                        OutlinedTextField(
                            value = accountPhoneText,
                            onValueChange = { 
                                accountPhoneText = it
                                viewModel.updateUserProfile(profile.copy(accountPhoneText = it))
                            },
                            label = { Text("Mobile Number", color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedBorderColor = activeAccentColor, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "", tint = activeAccentColor, modifier = Modifier.size(14.dp)) }
                        )

                        // 2FA Switch
                        Spacer(modifier = Modifier.height(10.dp))
                        ProfileToggleRow(title = "Two-Factor Authentication", desc = "Require OTP verification block during app entrance", checked = isTwoFactorEnabled) { viewModel.updateUserProfile(profile.copy(isTwoFactorEnabled = it)) }

                        // Backup codes
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Vault Recovery Backup Codes", color = Color.White, fontSize = 11.sp)
                            Button(
                                onClick = {
                                    val codesList = listOf("VIO-8891-AA", "VIO-1102-ZX", "VIO-5520-KL", "VIO-9912-QW")
                                    viewModel.updateUserProfile(profile.copy(backupCodesCsv = codesList.joinToString(",")))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF130922)),
                                border = BorderStroke(0.5.dp, BorderColor),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Generate Codes", color = Color.White, fontSize = 9.sp)
                            }
                        }
                        if (backupCodesGenerated.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = backupCodesGenerated.joinToString(separator = "  |  "),
                                    color = Color(0xFFFFD54F),
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }

                        // Download data ledgers CCPA
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Encrypted Local Ledger", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Under GDPR Article 15/CCPA, inspect and download complete local transcripts.", color = TextSecondary, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showDataLedgerDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF140D2D)),
                                border = BorderStroke(1.dp, activeAccentColor.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = "", tint = activeAccentColor, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Inspect Data", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { showWipeConfirmationDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF5350)),
                                border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = "", tint = Color(0xFFEF5350), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Wipe Footprint", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Deactivate Delete account
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { /* Deactivate mock */ },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Deactivate Account", fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = { showWipeConfirmationDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Delete Account", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Logout Action Rows
                Text(
                    text = "Logout of Viora?",
                    color = AccentCrimsonPink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .testTag("profile_logout_action")
                        .clickable { viewModel.handleLogout() }
                        .padding(16.dp)
                )
            }
        }

        // PANEL 1: Kyc live selfie wizard review frame
        if (showKycWizard) {
            KycVerificationScreen(
                viewModel = viewModel,
                userProfile = profile,
                onDismiss = { showKycWizard = false }
            )
        }

        // PANEL 2: Personal profile local ledger CCPA inspector
        if (showDataLedgerDialog) {
            AlertDialog(
                onDismissRequest = { showDataLedgerDialog = false },
                containerColor = DarkSurfaceCard,
                tonalElevation = 6.dp,
                icon = { Icon(Icons.Default.Security, contentDescription = "", tint = activeAccentColor, modifier = Modifier.size(24.dp)) },
                title = { Text("Regulatory GDPR Ledger Audit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "This transcript lists the complete details stored locally under GDPR compliance laws for Alex.",
                            color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0C071A))
                                .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp))
                                .horizontalScroll(rememberScrollState())
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = """
                                {
                                  "subject": "${profile.id}",
                                  "identity": {
                                    "display_name": "${profile.name}",
                                    "age": ${if (hideMyAge) "\"Protected\"" else profile.age.toString()},
                                    "pronouns": "$pronounsInput",
                                    "zodiac": "$zodiacSign",
                                    "religion": "$religionSetting",
                                    "lifestyle": {
                                      "drinking": "$drinkingStatus",
                                      "smoking": "$smokingStatus",
                                      "fitness": "$fitnessLevel",
                                      "pets": "$petsStatus",
                                      "children": "$childrenStatus"
                                    }
                                  },
                                  "privacy_ledger": {
                                    "incognito_search": $incognitoMode,
                                    "stealth_indicator": $hideOnlinePresence,
                                    "obfuscate_proximity": $obfuscateProximity,
                                    "search_indexing": $allowSearchIndexing
                                  }
                                }
                                """.trimIndent(),
                                color = SuccessGreen,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDataLedgerDialog = false }) {
                        Text("Dismiss", color = activeAccentColor)
                    }
                }
            )
        }

        // PANEL 3: Local storage footprint CCPA wipe
        if (showWipeConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showWipeConfirmationDialog = false },
                containerColor = DarkSurfaceCard,
                icon = { Icon(Icons.Default.Warning, contentDescription = "", tint = Color(0xFFEF5350), modifier = Modifier.size(24.dp)) },
                title = { Text("Regulatory GDPR Data Wipe?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = { Text("This will completely sanitize all cached conversations, preferences, and local credentials. Proceed?", color = TextSecondary, fontSize = 12.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            showWipeConfirmationDialog = false
                            dataWipedSuccessfully = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Sanitize All", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeConfirmationDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            )
        }

        // PANEL 4: Wipe success acknowledgment
        if (dataWipedSuccessfully) {
            AlertDialog(
                onDismissRequest = { dataWipedSuccessfully = false },
                containerColor = DarkSurfaceCard,
                icon = { Icon(Icons.Default.Check, contentDescription = "", tint = Color(0xFF00E676), modifier = Modifier.size(24.dp)) },
                title = { Text("Data Purge Finalized", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = { Text("All local database records, credentials, and image caches have been completely sanitized.", color = TextSecondary, fontSize = 12.sp) },
                confirmButton = {
                    Button(onClick = { dataWipedSuccessfully = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)), shape = RoundedCornerShape(6.dp)) {
                        Text("Acknowledge", color = Color.White)
                    }
                }
            )
        }

        // PANEL 5: Red Warning Panic Active Overlay
        if (showPanicTriggeredDialog) {
            AlertDialog(
                onDismissRequest = { showPanicTriggeredDialog = false },
                containerColor = Color(0xFF1D090E),
                icon = { Icon(Icons.Default.Warning, contentDescription = "", tint = Color(0xFFEF5350), modifier = Modifier.size(28.dp)) },
                title = { Text("RED EMERGENCY PANIC BROADCAST", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "An alert has been generated and dispatched successfully to your Trusted Contacts:",
                            color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(8.dp)
                        ) {
                            Text(
                                "Distress Dispatch: Urgent. Help needed at live geo coordinates shared: $trustedContactNumber",
                                color = Color(0xFFEF5350), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showPanicTriggeredDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Disable Alarm", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // PANEL 6: Safety Education Center Slider Drawer
        if (showSafetyEducationDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showSafetyEducationDrawer = false },
                containerColor = DarkSurfaceCard,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Safety Education Center", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Proactive self-defense & secure meetups guide", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    SafetyTipItem(title = "1. Meet in Populated Public Spaces Only", desc = "Always coordinate initial speed dates in high-density areas with cameras & escape lines available.")
                    SafetyTipItem(title = "2. Inform Trusted Contacts", desc = "Register phone triggers and location coordinates with close circle contacts before initiating speed meetups.")
                    SafetyTipItem(title = "3. Check Verification Certificates", desc = "Verify the other user shows active Guard checkmarks or Trust levels over 90% before sharing contact details.")
                    SafetyTipItem(title = "4. Trust Your Gut", desc = "If anyone acts suspicious or attempts bypass scripts, immediately hit the block/report trigger button under the Safety Vault.")

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showSafetyEducationDrawer = false },
                        colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Acknowledge Guide", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // PANEL 7: Who Viewed Me Info Overlay
        if (showWhoViewedMePanel) {
            AlertDialog(
                onDismissRequest = { showWhoViewedMePanel = false },
                containerColor = DarkSurfaceCard,
                title = { Text("Recent Views History Feed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("The following members swiped past or viewed your active discovery profile:", color = TextSecondary, fontSize = 12.sp)
                        simulatedVisitorsHistory.forEach { (name, time) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Face, contentDescription = "", tint = activeAccentColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(time, color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showWhoViewedMePanel = false }, colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor)) {
                        Text("Dismiss", color = Color.White)
                    }
                }
            )
        }

        // PANEL 8: Who Liked Me Info Overlay
        if (showWhoLikedMePanel) {
            AlertDialog(
                onDismissRequest = { showWhoLikedMePanel = false },
                containerColor = DarkSurfaceCard,
                title = { Text("Dating Likes Received Feed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("These people swiped right on your profile! Match with them inside matches tab.", color = TextSecondary, fontSize = 12.sp)
                        val likes = listOf("Emma Watson", "Sophia Martinez", "Sharon")
                        likes.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = "", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showWhoLikedMePanel = false }, colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor)) {
                        Text("Dismiss", color = Color.White)
                    }
                }
            )
        }

        // PANEL 9: Active Full Image Zoom overlay with close button and smooth backdrop
        if (activeFullPhotoUrl != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { activeFullPhotoUrl = null }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { activeFullPhotoUrl = null },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = activeFullPhotoTitle,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 16.dp).purpleGlow()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(activeFullPhotoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Full zoom image preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(400.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, BrightNeonPurple, RoundedCornerShape(16.dp))
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        Button(
                            onClick = { activeFullPhotoUrl = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF140D24)),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("CLOSE PREVIEW", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // PANEL 10: Pulsing Live Voice Intro dialog
        if (showVoiceRecorderDialog) {
            var isRecordingActive by remember { mutableStateOf(false) }
            val transition = rememberInfiniteTransition()
            val pulseScale by transition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulseScale"
            )

            AlertDialog(
                onDismissRequest = { showVoiceRecorderDialog = false },
                containerColor = Color(0xFF160920),
                modifier = Modifier.border(1.dp, BrightNeonPurple, RoundedCornerShape(26.dp)),
                title = {
                    Text(
                        "Voice Greeting Recorder",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().purpleGlow()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isRecordingActive) "🔴 Recording your 30s Intro..." else "Elevate your matchmaking prospects with a real audio handshake.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Red pulsing circle record button
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecordingActive) Color(0xFFEF5350).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = if (isRecordingActive) (2.dp * pulseScale) else 1.dp,
                                    color = if (isRecordingActive) Color(0xFFEF5350) else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    isRecordingActive = !isRecordingActive
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecordingActive) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Record Trigger",
                                tint = if (isRecordingActive) Color(0xFFEF5350) else Color.White,
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer {
                                        if (isRecordingActive) {
                                            scaleX = pulseScale
                                            scaleY = pulseScale
                                        }
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isRecordingActive) {
                            Text(
                                "Recording active - speak clearly. Tap button when finished.",
                                color = Color(0xFFEF5350),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "Press target circle to begin recording.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            hasVoiceIntro = true
                            showVoiceRecorderDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrightNeonPurple),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save Intro", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVoiceRecorderDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            )
        }
    }
}

@Composable
fun StatItem(metric: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = metric,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ExpandableSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (expanded) PrimaryPinkPurple.copy(alpha = 0.8f) else BorderColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF140B24))
                            .border(0.5.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = PrimaryPinkPurple, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (expanded) {
                HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), thickness = 0.5.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ProfileToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(desc, color = TextSecondary, fontSize = 10.sp, lineHeight = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryPinkPurple,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun LifestyleSelectorRow(
    label: String,
    value: String,
    options: List<String>,
    onChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
            options.forEach { opt ->
                val selected = value == opt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) PrimaryPinkPurple else Color(0xFF140D24))
                        .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp))
                        .clickable { onChange(opt) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(opt, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SafetyTipItem(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

fun saveUriToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream: java.io.InputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "cover_photo_${System.currentTimeMillis()}.jpg"
        val file = java.io.File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun ProfileActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    status: String,
    statusColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
            .width(60.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF140D24))
                .border(0.5.dp, BorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = BrightNeonPurple,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            text = status,
            color = statusColor,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ProfileDetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140D22)),
        border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = BrightNeonPurple,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 9.sp,
                maxLines = 1,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
