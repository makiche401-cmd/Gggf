package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.database.DatingProfileEntity
import com.example.data.database.PostEntity
import com.example.ui.components.FloatingHeartsBackground
import com.example.ui.components.VioraGradientButton
import com.example.ui.components.glassmorphic
import com.example.ui.components.purpleGlow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.ui.theme.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Shadow
import kotlin.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.viewmodel.VioraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: VioraViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val celebrationProfile by viewModel.matchedCelebrationProfile.collectAsState()
    val viewingPost by viewModel.viewingFullscreenPost.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    
    var showNotifSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkCharcoalBg,
        bottomBar = {
            // Elegant premium bar with custom purple glow indicators
            VioraBottomNavigation(
                selectedTab = currentTab,
                onTabSelect = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen router
            when (currentTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { viewModel.setTab(2) },
                    onOpenNotifications = {
                        viewModel.markNotificationsRead()
                        showNotifSheet = true
                    }
                )
                1 -> ExploreScreen(viewModel = viewModel)
                2 -> ChatScreen(viewModel = viewModel)
                3 -> PostScreen(viewModel = viewModel)
                4 -> ProfileScreen(viewModel = viewModel)
            }

            // Mutual match celebration modal popup overlay
            if (celebrationProfile != null) {
                MatchCelebrationDialog(
                    profile = celebrationProfile!!,
                    onSendMessage = {
                        viewModel.setActiveChat(celebrationProfile)
                        viewModel.dismissCelebration()
                        viewModel.setTab(2) // Navigate to chats
                    },
                    onDismiss = { viewModel.dismissCelebration() }
                )
            }

            // Fullscreen post detailed visual overlay
            if (viewingPost != null) {
                FullscreenPostOverlay(
                    post = viewingPost!!,
                    onDismiss = { viewModel.setViewingFullscreenPost(null) },
                    viewModel = viewModel
                )
            }
        }
    }

    // Notifications sliding panel drawer sheet
    if (showNotifSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotifSheet = false },
            containerColor = DarkSurfaceCard,
            dragHandle = { BottomSheetDefaults.DragHandle(color = BorderColor) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.7f)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notifications Log", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = { showNotifSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "", tint = Color.White)
                    }
                }

                if (notifications.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "", tint = TextSecondary, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No new alerts", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notifications) { notif ->
                            val icon = when (notif.type) {
                                "match" -> Icons.Default.Favorite
                                "chat" -> Icons.Default.Forum
                                "verification" -> Icons.Default.Verified
                                else -> Icons.Default.Notifications
                            }
                            val color = when (notif.type) {
                                "match" -> AccentCrimsonPink
                                "chat" -> BrightNeonPurple
                                "verification" -> SuccessGreen
                                else -> PrimaryPinkPurple
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassmorphic(backgroundColor = Color(0x1F140B1F), borderColor = BorderColor, borderRadius = 16.dp)
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(notif.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(notif.content, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// PREMIUM BOTTOM NAVIGATION TAB WRAPPER
// -----------------------------------------------------------------------------
@Composable
fun VioraBottomNavigation(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .background(Color(0xFF0C0714))
                .border(BorderStroke(0.5.dp, BorderColor))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                NavigationTabItem("Home", Icons.Default.Home, Icons.Outlined.Home, 0),
                NavigationTabItem("Explore", Icons.Default.Explore, Icons.Outlined.Explore, 1),
                NavigationTabItem("Chats", Icons.Default.Forum, Icons.Outlined.Forum, 2),
                NavigationTabItem("Post", Icons.Default.AddBox, Icons.Outlined.AddBox, 3),
                NavigationTabItem("Profile", Icons.Default.Person, Icons.Outlined.Person, 4)
            )

            tabs.forEach { tab ->
                val isSelected = selectedTab == tab.index
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "scale"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelect(tab.index) }
                        .testTag("nav_tab_item_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(height = if (isSelected) 40.dp else 24.dp, width = 50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0x33B01DFF) else Color.Transparent)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.activeIcon else tab.outlineIcon,
                            contentDescription = tab.name,
                            tint = if (isSelected) BrightNeonPurple else TextSecondary,
                            modifier = Modifier
                                .size(24.dp)
                                .purpleGlow(
                                    color = if (isSelected) BrightNeonPurple.copy(alpha = 0.4f) else Color.Transparent,
                                    radius = 8.dp
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.name,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

data class NavigationTabItem(
    val name: String,
    val activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val outlineIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val index: Int
)

// -----------------------------------------------------------------------------
// CELEBRATION DISCOVER POPUP OVERLAY
// -----------------------------------------------------------------------------
@Composable
fun MatchCelebrationDialog(
    profile: DatingProfileEntity,
    onSendMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            // Ambient particle background
            FloatingHeartsBackground(modifier = Modifier.fillMaxSize())

            // Gradient decorative ring overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .purpleGlow(radius = 350.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Text(
                        text = "It's a Match!",
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.purpleGlow()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You and ${profile.name} liked each other.",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Overlapping avatars
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Me avatar (Left)
                    Box(
                        modifier = Modifier
                            .offset(x = (-46).dp)
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFB01DFF), CircleShape)
                            .purpleGlow()
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=250")
                                .crossfade(true)
                                .build(),
                            contentDescription = "Me",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Floating pink heart in center
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(AccentCrimsonPink)
                            .border(2.dp, DarkCharcoalBg, CircleShape)
                            .purpleGlow()
                            .zIndex(10f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    // Match partner avatar (Right)
                    Box(
                        modifier = Modifier
                            .offset(x = 46.dp)
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFFF2E93), CircleShape)
                            .purpleGlow()
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(profile.profilePics.firstOrNull())
                                .crossfade(true)
                                .build(),
                            contentDescription = profile.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Call to actions (Send Message vs Keep Swiping)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VioraGradientButton(
                        text = "Send Message",
                        onClick = onSendMessage,
                        modifier = Modifier.fillMaxWidth().testTag("celebration_send_message_cta")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Keep Swiping",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .testTag("celebration_keep_swiping_cta")
                            .clickable { onDismiss() }
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// IMMERSIVE FULL-SCREEN POST IMAGE & MULTI-IMAGE GALLERY OVERLAY
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenPostOverlay(
    post: PostEntity,
    onDismiss: () -> Unit,
    viewModel: VioraViewModel
) {
    var isScrollAllMode by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }

    val currentImageIndex = if (post.imageUrls.isNotEmpty()) {
        selectedImageIndex.coerceIn(0, post.imageUrls.size - 1)
    } else {
        0
    }

    if (showEditDialog) {
        var editCaption by remember { mutableStateOf(post.caption) }
        var editPrivacy by remember { mutableStateOf(post.privacy) }
        var editCommentPermission by remember { mutableStateOf(post.commentPermission) }
        var showDeleteConfirm by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { showEditDialog = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF140D24),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Edit Post Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    // Caption editing
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Caption",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = editCaption,
                            onValueChange = { editCaption = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0C0717),
                                unfocusedContainerColor = Color(0xFF0C0717),
                                focusedBorderColor = BrightNeonPurple,
                                unfocusedBorderColor = BorderColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                    }

                    // Who can see (privacy) selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Who Can See (Privacy)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Everyone", "Followers", "Private").forEach { option ->
                                val isSelected = editPrivacy == option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) BrightNeonPurple else Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, if (isSelected) Color.Transparent else BorderColor, RoundedCornerShape(10.dp))
                                        .clickable { editPrivacy = option }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Who can comment permissions selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Who Can Comment",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Everyone", "Followers", "No One").forEach { option ->
                                val isSelected = editCommentPermission == option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) BrightNeonPurple else Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, if (isSelected) Color.Transparent else BorderColor, RoundedCornerShape(10.dp))
                                        .clickable { editCommentPermission = option }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))

                    // Buttons
                    if (showDeleteConfirm) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2C0F22), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFE91E63).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Delete this post forever? This cannot be undone.",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showDeleteConfirm = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text("Cancel", color = Color.White, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.deleteOwnPost(post.id)
                                        showDeleteConfirm = false
                                        showEditDialog = false
                                        onDismiss() // Close the fullscreen overlay entirely
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text("Yes, Delete", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text("Delete Post", color = Color(0xFFFF2E93), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showEditDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, BorderColor),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text("Cancel", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.updatePostDetails(
                                    postId = post.id,
                                    caption = editCaption,
                                    privacy = editPrivacy,
                                    commentPermission = editCommentPermission
                                )
                                showEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrightNeonPurple),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("Apply Changes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            val globalMute by viewModel.isMuted.collectAsState()
            var isPreparingVideo by remember { mutableStateOf(true) }
            val coroutineScope = rememberCoroutineScope()
            var showHeartState by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Determine layout types
                val isShortBg = post.backgroundColor != null && post.backgroundColor != "none" && post.caption.length <= 100 && post.imageUrls.isEmpty() && post.videoUrl == null

                // 1. Media Background (fillMaxSize)
                if (post.videoUrl != null) {
                    // Video View / PlayerView
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(post.id) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (!post.isLiked) {
                                            viewModel.togglePostLike(post)
                                        }
                                        coroutineScope.launch {
                                            showHeartState = true
                                            delay(600)
                                            showHeartState = false
                                        }
                                    },
                                    onTap = { viewModel.toggleMute() }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ZoomableContainer(key = post.id) {
                            Media3VideoPlayer(
                                videoUrl = post.videoUrl,
                                isMuted = globalMute,
                                onPlaybackStateChanged = { buffering ->
                                    isPreparingVideo = buffering
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (isPreparingVideo) {
                            CircularProgressIndicator(
                                color = BrightNeonPurple,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                } else if (isShortBg) {
                    // Ambient Text Card centered with full screen backdrop
                    val brush = when (post.backgroundColor) {
                        "cosmic_violet" -> Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF1A237E)))
                        "solar_flame" -> Brush.linearGradient(listOf(Color(0xFF880E4F), Color(0xFFE65100)))
                        "aurora_teal" -> Brush.linearGradient(listOf(Color(0xFF003020), Color(0xFF0C2B52)))
                        "cotton_candy" -> Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63)))
                        "royal_gold" -> Brush.linearGradient(listOf(Color(0xFF2E1A47), Color(0xFF8B6508)))
                        "crimson_dark" -> Brush.linearGradient(listOf(Color(0xFF3E0610), Color(0xFF150205)))
                        else -> Brush.linearGradient(listOf(Color(0xFF263238), Color(0xFF10081C)))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                                .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = post.caption,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (post.imageUrls.isNotEmpty()) {
                    // Photo Viewer (Carousel or Scroll All Mode)
                    if (isScrollAllMode && post.imageUrls.size > 1) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 100.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(post.imageUrls) { imgUrl ->
                                ZoomableContainer(key = imgUrl) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(imgUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Post Item Fullscreen",
                                        contentScale = ContentScale.Crop, // Aspect Fill Center Crop behavior
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(550.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(post.id) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (!post.isLiked) {
                                                viewModel.togglePostLike(post)
                                            }
                                            coroutineScope.launch {
                                                showHeartState = true
                                                delay(600)
                                                showHeartState = false
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = currentImageIndex,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(350))
                                },
                                label = "ImageTransition",
                                modifier = Modifier.fillMaxSize()
                            ) { index ->
                                ZoomableContainer(key = index) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(post.imageUrls[index])
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Post Image Fullscreen",
                                        contentScale = ContentScale.Crop, // Center Crop behavior
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            // Left and Right manual swipe/tap regions or overlay pager buttons
                            if (post.imageUrls.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            selectedImageIndex = if (selectedImageIndex > 0) {
                                                selectedImageIndex - 1
                                            } else {
                                                post.imageUrls.size - 1
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = "Prev",
                                            tint = Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            selectedImageIndex = if (selectedImageIndex < post.imageUrls.size - 1) {
                                                selectedImageIndex + 1
                                            } else {
                                                0
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Next",
                                            tint = Color.White
                                        )
                                    }
                                }

                                // Image Index tag
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .statusBarsPadding()
                                        .padding(top = 80.dp, end = 20.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${currentImageIndex + 1}/${post.imageUrls.size}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Gradients and Overlays for high-contrast text readability
                if (!isShortBg) {
                    // Top Shadow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                                )
                            )
                            .align(Alignment.TopCenter)
                    )

                    // Bottom Shadow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                )
                            )
                            .align(Alignment.BottomCenter)
                    )
                }

                // 3. Floating Close & Edit Header at the Top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-Left Brand / Subtitle indicator
                    Text(
                        text = if (post.videoUrl != null) "Viora Reel" else "Viora Post",
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        style = androidx.compose.ui.text.TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 3f))
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (post.authorId == "me") {
                            IconButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BrightNeonPurple.copy(alpha = 0.3f))
                                    .border(1.dp, BrightNeonPurple.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Edit Post Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 4. Double tap Like heartbeat animation
                AnimatedVisibility(
                    visible = showHeartState,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Double tap heart like",
                        tint = AccentCrimsonPink,
                        modifier = Modifier.size(110.dp)
                    )
                }

                // 5. Floating Bottom-Left details (Author & Caption)
                if (!isShortBg) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .navigationBarsPadding()
                            .padding(start = 20.dp, bottom = 24.dp, end = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(post.authorAvatar)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = post.authorName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                            Column {
                                Text(
                                    text = post.authorName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    style = androidx.compose.ui.text.TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 4f))
                                )
                                Text(
                                    text = post.authorLabel,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    style = androidx.compose.ui.text.TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 4f))
                                )
                            }
                        }

                        Text(
                            text = post.caption,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            lineHeight = 18.sp,
                            style = androidx.compose.ui.text.TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 4f)),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Privacy & Comment Permission tags
                        if (post.privacy != "Everyone" || post.commentPermission != "Everyone") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (post.privacy != "Everyone") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = BrightNeonPurple,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = post.privacy,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (post.commentPermission != "Everyone") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Comment,
                                            contentDescription = null,
                                            tint = BrightNeonPurple,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "Comments: ${post.commentPermission}",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Floating Bottom-Right actions stack
                if (!isShortBg) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 16.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Like Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.togglePostLike(post) },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (post.isLiked) AccentCrimsonPink else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "${post.likesCount}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                style = androidx.compose.ui.text.TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 4f))
                            )
                        }

                        // Comment Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { /* Comments dialog or display */ },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Comment,
                                    contentDescription = "Comments",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "${post.commentsCount}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                style = androidx.compose.ui.text.TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 4f))
                            )
                        }

                        // Views count display
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { /* Info */ },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Views",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "${post.viewsCount + 1}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                style = androidx.compose.ui.text.TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 4f))
                            )
                        }

                        // Video Volume Mute toggle or Multi-image Scroll All toggle
                        if (post.videoUrl != null) {
                            IconButton(
                                onClick = { viewModel.toggleMute() },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (globalMute) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = "Toggle Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        if (post.imageUrls.size > 1) {
                            IconButton(
                                onClick = { isScrollAllMode = !isScrollAllMode },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(if (isScrollAllMode) BrightNeonPurple else Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isScrollAllMode) Icons.Default.AspectRatio else Icons.Default.Menu,
                                    contentDescription = "Scroll Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// HELPER COMPONENTS FOR EDGE-TO-EDGE PINCH ZOOM & MEDIA3 VIDEO PLAYBACK
// -----------------------------------------------------------------------------

@OptIn(UnstableApi::class)
@Composable
fun Media3VideoPlayer(
    videoUrl: String,
    isMuted: Boolean,
    onPlaybackStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
        }
    }

    // Set up media item
    LaunchedEffect(videoUrl) {
        val mediaItem = MediaItem.fromUri(videoUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    // Track state changes to show buffering indicator
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                onPlaybackStateChanged(state == Player.STATE_BUFFERING)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Handle mute state changes
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1.0f
    }

    // Correct cleanup when video player goes out of recomposition
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier
    )
}

@Composable
fun ZoomableContainer(
    key: Any,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var scale by remember(key) { mutableStateOf(1f) }
    var offset by remember(key) { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset += offsetChange * scale
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .transformable(state = state)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
    ) {
        content()
    }
}

