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
            color = Color(0xFF07030A) // Deeper night black backdrop
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header Area with Profile Details and Close/Edit Accent Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(post.authorAvatar)
                                .crossfade(true)
                                .build(),
                            contentDescription = post.authorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.dp, BorderColor, CircleShape)
                        )
                        Column {
                            Text(
                                text = post.authorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = post.authorLabel,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (post.authorId == "me") {
                            IconButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BrightNeonPurple.copy(alpha = 0.2f))
                                    .border(1.dp, BrightNeonPurple.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Edit Post Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Elegant Circular glass-style close button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
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

                // Main visual canvas segment
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val isShortBg = post.backgroundColor != null && post.backgroundColor != "none" && post.caption.length <= 100 && post.imageUrls.isEmpty() && post.videoUrl == null

                    if (post.videoUrl != null) {
                        // Immersive multi-ratio HD video player segment
                        var isPreparingVideo by remember { mutableStateOf(true) }
                        var mediaPlayerInstance by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                        val globalMute by viewModel.isMuted.collectAsState()

                        // Double tap like animation triggers
                        var showHeartState by remember { mutableStateOf(false) }
                        val coroutineScope = rememberCoroutineScope()

                        var calculatedRatio by remember(post.id) { mutableStateOf(9f / 16f) }
                        var detectedPlatformStr by remember(post.id) { mutableStateOf("TikTok Reel (9:16)") }
                        var videoResolutionLabel by remember(post.id) { mutableStateOf("1080x1920") }

                        LaunchedEffect(post.videoUrl) {
                            if (post.videoUrl.contains("skating") || post.videoUrl.contains("dancing") || post.videoUrl.contains("vertical") || post.videoUrl.contains("reel") || post.videoUrl.contains("tiktok")) {
                                calculatedRatio = 0.5625f
                                detectedPlatformStr = "TikTok Reel"
                                videoResolutionLabel = "1080x1920 (Portrait)"
                            } else if (post.videoUrl.contains("joyrides") || post.videoUrl.contains("bunny") || post.videoUrl.contains("landscape")) {
                                calculatedRatio = 1.777f
                                detectedPlatformStr = "YouTube Wide"
                                videoResolutionLabel = "1920x1080 (Landscape)"
                            } else {
                                calculatedRatio = 1.0f
                                detectedPlatformStr = "Facebook Square"
                                videoResolutionLabel = "1080x1080 (Square)"
                            }
                        }

                        // Views count incrementing once on entrance
                        LaunchedEffect(post.id) {
                            viewModel.incrementViews(post.id)
                        }

                        DisposableEffect(post.id) {
                            onDispose {
                                mediaPlayerInstance?.stop()
                                mediaPlayerInstance?.release()
                                mediaPlayerInstance = null
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { viewModel.toggleMute() }
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
                            // Loading skeleton / progress
                            if (isPreparingVideo) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.7f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = BrightNeonPurple, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text("Auto-detecting ratio & buffering stream...", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                }
                            }

                            // Dynamic widescreen or vertical safe container to avoid stretching
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .fillMaxHeight(0.92f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                // Native Video View renderer wrapping with dynamic aspect ratio constraints
                                androidx.compose.ui.viewinterop.AndroidView(
                                    factory = { ctx ->
                                        android.widget.VideoView(ctx).apply {
                                            setVideoURI(android.net.Uri.parse(post.videoUrl))
                                            setOnPreparedListener { mp ->
                                                mediaPlayerInstance = mp
                                                val w = mp.videoWidth
                                                val h = mp.videoHeight
                                                if (w > 0 && h > 0) {
                                                    calculatedRatio = w.toFloat() / h.toFloat()
                                                    val category = if (calculatedRatio < 0.8f) {
                                                        "TikTok Reel"
                                                    } else if (calculatedRatio > 1.3f) {
                                                        "YouTube Wide"
                                                    } else {
                                                        "Facebook Square"
                                                    }
                                                    detectedPlatformStr = category
                                                    videoResolutionLabel = "${w}x${h} HD"
                                                }
                                                mp.isLooping = true
                                                val currentVol = if (globalMute) 0f else 1f
                                                mp.setVolume(currentVol, currentVol)
                                                mp.start()
                                                isPreparingVideo = false
                                            }
                                            setOnErrorListener { _, _, _ ->
                                                isPreparingVideo = false
                                                true
                                            }
                                        }
                                    },
                                    update = { vv ->
                                        mediaPlayerInstance?.let { mp ->
                                            val currentVol = if (globalMute) 0f else 1f
                                            mp.setVolume(currentVol, currentVol)
                                        }
                                    },
                                    modifier = Modifier
                                        .aspectRatio(calculatedRatio)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                            }

                            // Heart double-tap animation
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showHeartState,
                                enter = scaleIn() + fadeIn(),
                                exit = scaleOut() + fadeOut()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Double tap heart like",
                                    tint = AccentCrimsonPink,
                                    modifier = Modifier.size(100.dp)
                                )
                            }

                            // Floating Mute Button & Views count
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(24.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text("${post.viewsCount + 1} views", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color.White.copy(alpha = 0.3f)))
                                Icon(
                                    imageVector = if (globalMute) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = "Volume State Toggle",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable { viewModel.toggleMute() }
                                )
                            }
                        }
                    } else if (isShortBg) {
                        // Ambient text post overlay canvas
                        val brush = when (post.backgroundColor) {
                            "cosmic_violet" -> Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF1A237E)))
                            "solar_flame" -> Brush.linearGradient(listOf(Color(0xFF880E4F), Color(0xFFE65100)))
                            "aurora_teal" -> Brush.linearGradient(listOf(Color(0xFF003020), Color(0xFF0C2B52)))
                            "cotton_candy" -> Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63)))
                            "royal_gold" -> Brush.linearGradient(listOf(Color(0xFF2E1A47), Color(0xFF8B6508)))
                            "crimson_dark" -> Brush.linearGradient(listOf(Color(0xFF3E0610), Color(0xFF150205)))
                            else -> null
                        }
                        val solidColor = if (post.backgroundColor == "charcoal_solid") Color(0xFF263238) else Color(0xFF10081C)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(brush ?: Brush.linearGradient(listOf(solidColor, solidColor)))
                                .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.caption,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else if (post.imageUrls.isNotEmpty()) {
                        // Image viewer
                        if (isScrollAllMode && post.imageUrls.size > 1) {
                            // Immersive toggle layout: Allow user to vertically scroll ALL high-quality images sequentially
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                        Text(
                                            text = "Multi-Image Gallery (${post.imageUrls.size} items)",
                                            color = BrightNeonPurple,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = post.caption,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                items(post.imageUrls) { imgUrl ->
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(imgUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Post Item Fullscreen",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 480.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Standard full width interactive Carousel Slider
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(post.imageUrls[currentImageIndex])
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Post Image Fullscreen",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.88f)
                                )

                                // Overlay pager button controllers
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
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
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
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Next",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    // Image numeric tag
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(16.dp)
                                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
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
                }

                // Interactive Bottom Detail panel (Actions, Stats & Captions)
                val isShortBg = post.backgroundColor != null && post.backgroundColor != "none" && post.caption.length <= 100 && post.imageUrls.isEmpty()
                if (!isShortBg) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)))
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (!(isScrollAllMode && post.imageUrls.size > 1)) {
                            Text(
                                text = post.caption,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Audience/Security Label tags
                        if (post.privacy != "Everyone" || post.commentPermission != "Everyone") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (post.privacy != "Everyone") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = BrightNeonPurple,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Visibility: ${post.privacy}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                if (post.commentPermission != "Everyone") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Comment,
                                            contentDescription = null,
                                            tint = BrightNeonPurple,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Comments: ${post.commentPermission}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Post metrics displays
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Liked badge",
                                        tint = if (post.isLiked) AccentCrimsonPink else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "${post.likesCount}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Comment,
                                        contentDescription = "Comments label",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "${post.commentsCount}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Dynamic action toggle triggered on multi-image posts
                            if (post.imageUrls.size > 1) {
                                Button(
                                    onClick = { isScrollAllMode = !isScrollAllMode },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isScrollAllMode) BrightNeonPurple else Color.White.copy(alpha = 0.12f)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    border = BorderStroke(1.dp, if (isScrollAllMode) Color.Transparent else Color.White.copy(alpha = 0.18f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isScrollAllMode) Icons.Default.AspectRatio else Icons.Default.Menu,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isScrollAllMode) "Show Cover Slider" else "See More (Scroll All)",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
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

