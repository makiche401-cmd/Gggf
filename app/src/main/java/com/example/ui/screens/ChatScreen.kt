package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.random.Random
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import com.example.data.database.DatingProfileEntity
import com.example.data.database.MessageEntity
import com.example.data.database.PostEntity
import com.example.ui.components.InteractiveWaveform
import com.example.ui.components.glassmorphic
import com.example.ui.components.purpleGlow
import com.example.ui.theme.*
import com.example.ui.viewmodel.VioraViewModel

data class InboxWallpaper(val name: String, val url: String)

val INBOX_WALLPAPERS = listOf(
    // Original Favorites
    InboxWallpaper("Midnight Nebula", "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?auto=format&fit=crop&q=80&w=600"),
    InboxWallpaper("Emerald Glade", "https://images.unsplash.com/photo-1502082553048-f009c37129b9?auto=format&fit=crop&q=80&w=600"),
    
    // Love & Hearts Romance
    InboxWallpaper("Neon Love", "https://images.unsplash.com/photo-1518199266791-5375a83190b7?auto=format&fit=crop&q=80&w=600"),
    InboxWallpaper("Heart Spark", "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?auto=format&fit=crop&q=80&w=600"),
    InboxWallpaper("Sweet Glow", "https://images.unsplash.com/photo-1518241353330-0f7941c2d9b5?auto=format&fit=crop&q=80&w=600"),

    // City & Cyberpunk Nights
    InboxWallpaper("Cyberpunk Alley", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?auto=format&fit=crop&q=80&w=600"),
    InboxWallpaper("Tokyo Tower", "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&q=80&w=600"),
    InboxWallpaper("Manhattan Twilight", "https://images.unsplash.com/photo-1519501025264-65ba15a82390?auto=format&fit=crop&q=80&w=600"),

    // Sweet Couples
    InboxWallpaper("Starry Couple", "https://images.unsplash.com/photo-1537633552985-df8429e8048b?auto=format&fit=crop&q=80&w=600"),
    InboxWallpaper("Hold Hands", "https://images.unsplash.com/photo-1464746133101-a2c3f88e0dd9?auto=format&fit=crop&q=80&w=600"),
    InboxWallpaper("Sunset Kiss", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=600"),

    // Animation & Aesthetic Illustrations
    InboxWallpaper("Lofi Sky", "https://images.unsplash.com/photo-1494905998402-395d579af36f?auto=format&fit=crop&q=80&w=600"),
    InboxWallpaper("Pastel Dream", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&q=80&w=600"),
    InboxWallpaper("Anime Street", "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?auto=format&fit=crop&q=80&w=600")
)

@Composable
fun ChatScreen(viewModel: VioraViewModel) {
    val activeProfile by viewModel.activeChatProfile.collectAsState()
    val matches by viewModel.matchedProfiles.collectAsState()
    val allMessages by viewModel.allMessages.collectAsState()
    val allProfilesList by viewModel.allProfiles.collectAsState()
    val postsFeed by viewModel.posts.collectAsState()

    var viewedProfileDetail by remember { mutableStateOf<DatingProfileEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkCharcoalBg)
        ) {
            if (activeProfile == null) {
                // Screen 1: Matches and Dialog threads List (incorporating settings & spam)
                ChatsLobby(
                    viewModel = viewModel,
                    matches = matches,
                    allMessages = allMessages,
                    onSelectChat = { viewModel.setActiveChat(it) }
                )
            } else {
                // Screen 2: Detailed Private Chat window (with custom wallpapers / colors active)
                val profile = activeProfile!!
                val chatMessages = allMessages.filter { it.chatUserId == profile.id }
                ChatRoom(
                    viewModel = viewModel,
                    profile = profile,
                    messages = chatMessages,
                    onBack = { viewModel.setActiveChat(null) },
                    onSendMessage = { viewModel.sendChatMessage(text = it) },
                    onSendVoiceNote = { viewModel.sendChatMessage(text = "Voice note", voiceDuration = it) },
                    onSendImageMock = { viewModel.sendChatMessage(text = "Media Image Attachment", imageUrl = "https://images.unsplash.com/photo-1543807535-eceef0bc6599?auto=format&fit=crop&q=80&w=650") },
                    onHeaderClick = { viewedProfileDetail = profile }
                )
            }
        }

        // Full Screen Profile Detail overlay
        if (viewedProfileDetail != null) {
            val liveProfileState = allProfilesList.find { it.id == viewedProfileDetail!!.id } ?: viewedProfileDetail!!
            FullProfileDetailOverlay(
                profile = liveProfileState,
                posts = postsFeed.filter { it.authorId == liveProfileState.id },
                onDismiss = { viewedProfileDetail = null },
                onToggleFollow = { viewModel.toggleFollowUser(liveProfileState.id, liveProfileState.isFollowing) },
                onMessage = { viewedProfileDetail = null },
                onInvite = {},
                onSeeFollowers = {},
                onReport = {
                    viewModel.reportProfile(liveProfileState.id, "Reported via Profile View")
                    viewedProfileDetail = null
                },
                onBlock = {
                    viewModel.blockProfile(liveProfileState.id)
                    viewedProfileDetail = null
                },
                onPostClick = { viewModel.setViewingFullscreenPost(it) }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// CHATS LOBBY (LOBBY OF INBOX THREADS)
// -----------------------------------------------------------------------------
@Composable
fun ChatsLobby(
    viewModel: VioraViewModel,
    matches: List<DatingProfileEntity>,
    allMessages: List<MessageEntity>,
    onSelectChat: (DatingProfileEntity) -> Unit
) {
    var showSettingsPanel by remember { mutableStateOf(false) }
    var selectedInboxTab by remember { mutableStateOf(0) } // 0: Primary (Followers), 1: Spam Folder (Non-Followers)

    val whoCanChatSetting by viewModel.whoCanChatMe.collectAsState()
    val activeChatTheme by viewModel.chatTheme.collectAsState()
    val activeWallpaper by viewModel.chatWallpaperUrl.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    // 1. Process "Who can chat me" permissions overall
    val allowedMatches = matches.filter { profile ->
        when (whoCanChatSetting) {
            "Followers Only" -> profile.isFollowing
            "Verified Only" -> profile.verified
            else -> true // "Everyone"
        }
    }

    // 2. Classify into Primary (Followers) vs Spam Folder (Non-Followers)
    val primaryMatches = allowedMatches.filter { it.isFollowing }
    val spamMatches = allowedMatches.filter { !it.isFollowing }

    // Active displayed matches depending on selected folder tab
    val displayedMatches = if (selectedInboxTab == 0) primaryMatches else spamMatches

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Lobby Header with dynamic Settings Gear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Conversations",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                if (!isOnline) {
                    Text(
                        text = "Offline Mode • Local Messages Cached",
                        color = Color(0xFFEF5350),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = { showSettingsPanel = !showSettingsPanel },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (showSettingsPanel) PrimaryPinkPurple.copy(alpha = 0.3f) else Color(0xFF1E1233))
                    .testTag("chat_settings_toggle_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Inbox settings",
                    tint = if (showSettingsPanel) PrimaryPinkPurple else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Expanded/collapsible beautiful custom settings tray
        AnimatedVisibility(
            visible = showSettingsPanel,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ChatSettingsPanel(
                whoCanChat = whoCanChatSetting,
                onSetWhoCanChat = { viewModel.setWhoCanChatMe(it) },
                chatTheme = activeChatTheme,
                onSetChatTheme = { viewModel.setChatTheme(it) },
                activeWallpaper = activeWallpaper,
                onSetWallpaper = { viewModel.setChatWallpaperUrl(it) }
            )
        }

        // Category Filter Tabs: Standard Inbox vs Spam folder for non-followers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab 1: Primary Inbox
            val isPrimarySelected = selectedInboxTab == 0
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isPrimarySelected) PrimaryPinkPurple.copy(alpha = 0.25f) else Color.Transparent)
                    .border(1.dp, if (isPrimarySelected) PrimaryPinkPurple else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { selectedInboxTab = 0 }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "",
                    tint = if (isPrimarySelected) PrimaryPinkPurple else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Inbox",
                    color = if (isPrimarySelected) Color.White else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (primaryMatches.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PrimaryPinkPurple)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${primaryMatches.size}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tab 2: Non-Followers Spam requests Folder
            val isSpamSelected = selectedInboxTab == 1
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSpamSelected) Color(0xFFFF2E93).copy(alpha = 0.2f) else Color.Transparent)
                    .border(1.dp, if (isSpamSelected) Color(0xFFFF2E93) else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { selectedInboxTab = 1 }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReportProblem,
                    contentDescription = "",
                    tint = if (isSpamSelected) Color(0xFFFF2E93) else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Spam Folder",
                    color = if (isSpamSelected) Color.White else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (spamMatches.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFFF2E93))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${spamMatches.size}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal matches avatars list matching dynamic filter
        if (displayedMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (selectedInboxTab == 0) "No active matches in Primary. 💖" else "Spam folder for non-followers is clean! ✨",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
            ) {
                items(displayedMatches) { profile ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onSelectChat(profile) }
                            .testTag("inbox_match_avatar_${profile.id}")
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profile.profilePics.firstOrNull())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = profile.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (selectedInboxTab == 0) PrimaryPinkPurple else Color(0xFFFF2E93),
                                        shape = CircleShape
                                    )
                            )
                            if (profile.onlineStatus == "online") {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(OnlineGreen)
                                        .border(1.5.dp, DarkCharcoalBg, CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = profile.name.split(" ").firstOrNull() ?: "",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Message Thread list
        Text(
            text = if (selectedInboxTab == 0) "Active Chats" else "Filtered Spam Threads",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
        )

        if (displayedMatches.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (selectedInboxTab == 0) Icons.Default.Forum else Icons.Default.MarkChatRead,
                    contentDescription = "",
                    tint = TextSecondary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (selectedInboxTab == 0) "Your Inbox is empty" else "No Spam Messages",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (selectedInboxTab == 0) "Matches that follow you appear here." else "Chats from members who don't follow you land here.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(displayedMatches, key = { it.id }) { profile ->
                    val lastMessage = allMessages.filter { it.chatUserId == profile.id }.maxByOrNull { it.timestamp }
                    val isUnread = lastMessage != null && lastMessage.senderId != "me" && lastMessage.status != "read"
                    val displayContent = when (lastMessage?.type) {
                        "voice" -> "🎤 Voice note (${lastMessage.audioDurationSec}s)"
                        "image" -> "📷 Sent a photo attachment"
                        else -> lastMessage?.textContent ?: "Say hello! 👋 Connect now."
                    }
                    val timeStr = if (lastMessage != null) formatMessageTime(lastMessage.timestamp) else ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectChat(profile) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profile.profilePics.firstOrNull())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = profile.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isUnread) {
                                            Modifier.border(2.dp, if (selectedInboxTab == 0) BrightNeonPurple else Color(0xFFFF2E93), CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                            if (profile.onlineStatus == "online") {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(OnlineGreen)
                                        .border(2.dp, DarkCharcoalBg, CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.name,
                                        color = Color.White,
                                        fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    if (profile.verified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Verified, contentDescription = "", tint = BrightNeonPurple, modifier = Modifier.size(14.dp))
                                    }
                                }
                                if (lastMessage != null) {
                                    Text(
                                        text = timeStr,
                                        color = if (isUnread) BrightNeonPurple else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayContent,
                                    color = if (isUnread) Color.White else if (lastMessage?.senderId == "me") TextSecondary else BrightNeonPurple.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (lastMessage != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (lastMessage.senderId == "me") {
                                        when (lastMessage.status) {
                                            "sending" -> {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = "sending",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            "sent" -> {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "sent",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            "delivered" -> {
                                                Icon(
                                                    imageVector = Icons.Default.DoneAll,
                                                    contentDescription = "delivered",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            "read" -> {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(profile.profilePics.firstOrNull())
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = "seen",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                )
                                            }
                                        }
                                    } else if (isUnread) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(BrightNeonPurple)
                                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = BorderColor, thickness = 0.5.dp)
                }
            }
        }
    }
}


// -----------------------------------------------------------------------------
// CHAT SETTINGS PANEL
// -----------------------------------------------------------------------------
@Composable
fun ChatSettingsPanel(
    whoCanChat: String,
    onSetWhoCanChat: (String) -> Unit,
    chatTheme: String,
    onSetChatTheme: (String) -> Unit,
    activeWallpaper: String?,
    onSetWallpaper: (String?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191026)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section 1: Who Can Chat Me
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = "", tint = PrimaryPinkPurple, modifier = Modifier.size(16.dp))
                Text("Who can start chats with me:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Everyone", "Followers Only", "Verified Only").forEach { option ->
                    val isSelected = whoCanChat == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryPinkPurple else Color(0xFF0F071A))
                            .border(1.dp, if (isSelected) Color.White else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { onSetWhoCanChat(option) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 2: Chat Bubble Color Themes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Palette, contentDescription = "", tint = PrimaryPinkPurple, modifier = Modifier.size(16.dp))
                Text("Select Chat Bubble Gradient:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    Triple("Purple", Color(0xFFB01DFF), Color(0xFFFF2E93)),
                    Triple("Rose", Color(0xFFE91E63), Color(0xFFFF8A80)),
                    Triple("Teal", Color(0xFF009688), Color(0xFF00E676)),
                    Triple("Gold", Color(0xFFFFA000), Color(0xFFFFD54F)),
                    Triple("Charcoal", Color(0xFF37474F), Color(0xFF78909C))
                ).forEach { themeOption ->
                    val name = themeOption.first
                    val c1 = themeOption.second
                    val c2 = themeOption.third
                    val isSelected = chatTheme == name

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(c1, c2)))
                            .border(
                                width = if (isSelected) 2.5.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onSetChatTheme(name) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 3: Inbox Background Wallpaper (5 beautiful photos)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Wallpaper, contentDescription = "", tint = PrimaryPinkPurple, modifier = Modifier.size(16.dp))
                Text("Select Wallpaper (Dynamic Background):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    // Default Dark option
                    Box(
                        modifier = Modifier
                            .size(54.dp, 64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10081C))
                            .border(
                                width = if (activeWallpaper == null) 2.dp else 1.dp,
                                color = if (activeWallpaper == null) PrimaryPinkPurple else BorderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSetWallpaper(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Default\nDark",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                items(INBOX_WALLPAPERS) { wp: InboxWallpaper ->
                    val isSelected = activeWallpaper == wp.url
                    val displayedName = wp.name.split(" ").last()
                    Box(
                        modifier = Modifier
                            .size(54.dp, 64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) PrimaryPinkPurple else BorderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSetWallpaper(wp.url) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(wp.url)
                                .crossfade(true)
                                .build(),
                            contentDescription = wp.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                text = displayedName,
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


// -----------------------------------------------------------------------------
// CHAT ROOM (DETAILED CHAT PAGE)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatRoom(
    viewModel: VioraViewModel,
    profile: DatingProfileEntity,
    messages: List<MessageEntity>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendVoiceNote: (Int) -> Unit,
    onSendImageMock: () -> Unit,
    onHeaderClick: () -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var mockSelectedPhoto by remember { mutableStateOf(false) }

    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var activeMessageOptions by remember { mutableStateOf<MessageEntity?>(null) }
    var forwardingMessage by remember { mutableStateOf<MessageEntity?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val matches by viewModel.matchedProfiles.collectAsState()

    val sortedMessages = remember(messages) { messages.sortedByDescending { it.timestamp } }
    val latestReadMeId = remember(sortedMessages) {
        sortedMessages.filter { it.senderId == "me" && it.status == "read" }
            .maxByOrNull { it.timestamp }?.id
    }

    // Capture background wallpaper and color theme
    val activeWallpaperUrl by viewModel.chatWallpaperUrl.collectAsState()
    val activeThemeName by viewModel.chatTheme.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showReportDialogInChat by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
    ) {
        // Dynamic Live Wallpaper background rendering with auto overlay
        if (activeWallpaperUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(activeWallpaperUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Dynamic Background Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Glass opacity shade card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Chat Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back lobby", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onHeaderClick() }
                            .padding(end = 8.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profile.profilePics.firstOrNull())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = profile.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(OnlineGreen)
                                    .border(1.5.dp, DarkCharcoalBg, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(profile.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (profile.verified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, contentDescription = "", tint = BrightNeonPurple, modifier = Modifier.size(13.dp))
                                }
                            }
                            Text("End-to-end Encrypted 🔒", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status Badge icon
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2C163C))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(profile.relationshipGoals, color = BrightNeonPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    // 3-dots Menu Button
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(DarkSurfaceCard)
                                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Chats", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    viewModel.clearChatMessages(profile.id)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear Chats",
                                        tint = Color.White
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block User", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    viewModel.blockProfile(profile.id)
                                    onBack()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "Block User",
                                        tint = Color.Red
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report User", color = Color(0xFFFF9800)) },
                                onClick = {
                                    showMenu = false
                                    showReportDialogInChat = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Report User",
                                        tint = Color(0xFFFF9800)
                                    )
                                }
                            )
                        }
                    }

                    if (showReportDialogInChat) {
                        ReportReasonDialog(
                            profileName = profile.name,
                            onDismiss = { showReportDialogInChat = false },
                            onSubmitReport = { reason ->
                                viewModel.reportProfile(profile.id, reason)
                                showReportDialogInChat = false
                                onBack() // Navigate out of the chat
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderColor.copy(alpha = 0.6f), thickness = 0.5.dp)

            // Messaging Area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                reverseLayout = true
            ) {
                items(sortedMessages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == "me"
                    SwipeToReplyContainer(
                        onReply = {
                            replyingToMessage = msg
                            editingMessage = null
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                        ) {
                            if (msg.replyToText != null) {
                                Row(
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = if (isMe) 0.dp else 36.dp,
                                            end = if (isMe) 12.dp else 0.dp
                                        )
                                ) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .padding(bottom = 2.dp)
                                            .border(
                                                BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                                                RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                                contentDescription = null,
                                                tint = BrightNeonPurple.copy(alpha = 0.7f),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "${msg.replyToSender}: ",
                                                color = BrightNeonPurple,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = msg.replyToText ?: "",
                                                color = Color.LightGray.copy(alpha = 0.9f),
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .combinedClickable(
                                        onLongClick = { activeMessageOptions = msg },
                                        onClick = { /* No-op or status view */ }
                                    )
                            ) {
                                if (!isMe) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(profile.profilePics.firstOrNull())
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = profile.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .padding(end = 4.dp)
                                    )
                                }

                                // Dynamic styled Bubble component depending on user theme
                                when (msg.type) {
                                    "voice" -> VoiceNoteBubble(duration = msg.audioDurationSec, isMe = isMe, chatTheme = activeThemeName)
                                    "image" -> MediaBubble(url = msg.mediaUrl ?: "", isMe = isMe)
                                    else -> TextBubble(text = msg.textContent, isMe = isMe, chatTheme = activeThemeName)
                                }
                            }

                            // Status subtitle labels
                            Row(
                                modifier = Modifier.padding(top = 2.dp, start = if (isMe) 0.dp else 36.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(formatMessageTime(msg.timestamp), color = TextSecondary, fontSize = 9.sp)
                                if (isMe) {
                                    when (msg.status) {
                                        "sending" -> {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = "sending",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        "sent" -> {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "sent",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        "delivered" -> {
                                            Icon(
                                                imageVector = Icons.Default.DoneAll,
                                                contentDescription = "delivered",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        "read" -> {
                                            if (msg.id == latestReadMeId) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(profile.profilePics.firstOrNull())
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = "seen",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.DoneAll,
                                                    contentDescription = "read",
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(11.dp)
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

            // Attached photo simulation indicator
            if (mockSelectedPhoto) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF140B1F))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, contentDescription = "", tint = BrightNeonPurple, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ready to upload: couple_mood.png", color = Color.White, fontSize = 12.sp)
                    }
                    IconButton(onClick = { mockSelectedPhoto = false }) {
                        Icon(Icons.Default.Delete, contentDescription = "Discard", tint = AccentCrimsonPink)
                    }
                }
            }

            // Replying preview banner
            if (replyingToMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B0E2A))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = BrightNeonPurple, modifier = Modifier.size(16.dp))
                        Column {
                            Text(
                                text = "Replying to ${if (replyingToMessage!!.senderId == "me") "yourself" else profile.name}",
                                color = BrightNeonPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = replyingToMessage!!.textContent,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = { replyingToMessage = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel reply", tint = Color.LightGray)
                    }
                }
            }

            // Editing preview banner
            if (editingMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B0E2A))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = BrightNeonPurple, modifier = Modifier.size(16.dp))
                        Column {
                            Text(
                                text = "Editing Message",
                                color = BrightNeonPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = editingMessage!!.textContent,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = {
                        editingMessage = null
                        rawText = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel edit", tint = Color.LightGray)
                    }
                }
            }

            // Chat input tray
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF120822))
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (mockSelectedPhoto) {
                            onSendImageMock()
                            mockSelectedPhoto = false
                        } else {
                            mockSelectedPhoto = true
                        }
                    },
                    modifier = Modifier.testTag("chat_attach_button")
                ) {
                    Icon(
                        imageVector = if (mockSelectedPhoto) Icons.Default.CloudUpload else Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach photo",
                        tint = BrightNeonPurple
                    )
                }

                IconButton(onClick = { onSendVoiceNote(Random.nextInt(12) + 5) }) {
                    Icon(Icons.Default.Mic, contentDescription = "Send voice note", tint = BrightNeonPurple)
                }

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("Message...", color = TextSecondary, fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text")
                        .height(50.dp),
                    singleLine = true
                )

                IconButton(onClick = { rawText += "💜" }) {
                    Text("💜", fontSize = 20.sp)
                }

                IconButton(
                    onClick = {
                        if (rawText.isNotEmpty()) {
                            if (editingMessage != null) {
                                val updatedMsg = editingMessage!!.copy(textContent = rawText)
                                viewModel.updateMessage(updatedMsg)
                                editingMessage = null
                            } else {
                                viewModel.sendChatMessage(
                                    text = rawText,
                                    replyToId = replyingToMessage?.id,
                                    replyToText = replyingToMessage?.textContent,
                                    replyToSender = if (replyingToMessage?.senderId == "me") "You" else profile.name
                                )
                                replyingToMessage = null
                            }
                            rawText = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFB01DFF), Color(0xFFFF2E93))))
                        .testTag("chat_send_message_button")
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Send Message", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Overlay Action Dialogs
        if (activeMessageOptions != null) {
            val msg = activeMessageOptions!!
            val isMe = msg.senderId == "me"
            MessageActionsDialog(
                message = msg,
                isMe = isMe,
                onDismiss = { activeMessageOptions = null },
                onCopy = {
                    clipboardManager.setText(AnnotatedString(msg.textContent))
                    activeMessageOptions = null
                },
                onReply = {
                    replyingToMessage = msg
                    editingMessage = null
                    activeMessageOptions = null
                },
                onForward = {
                    forwardingMessage = msg
                    activeMessageOptions = null
                },
                onDelete = {
                    viewModel.deleteMessage(msg.id)
                    activeMessageOptions = null
                },
                onEdit = {
                    editingMessage = msg
                    rawText = msg.textContent
                    replyingToMessage = null
                    activeMessageOptions = null
                }
            )
        }

        if (forwardingMessage != null) {
            ForwardTargetSelectionDialog(
                matches = matches,
                onDismiss = { forwardingMessage = null },
                onSelectTarget = { targetProfile ->
                    viewModel.forwardMessage(forwardingMessage!!, targetProfile.id)
                    forwardingMessage = null
                }
            )
        }
    }
}


// Theme Brush compiler helper matching dynamic state selection
@Composable
fun getChatThemeBrush(themeName: String, isMe: Boolean): Brush {
    return if (isMe) {
        when (themeName) {
            "Rose" -> Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFFFF8A80)))
            "Teal" -> Brush.linearGradient(listOf(Color(0xFF009688), Color(0xFF00E676)))
            "Gold" -> Brush.linearGradient(listOf(Color(0xFFFFA000), Color(0xFFFFD54F)))
            "Charcoal" -> Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF78909C)))
            else -> Brush.linearGradient(listOf(Color(0xFFB01DFF), Color(0xFFFF2E93))) // Purple (Default)
        }
    } else {
        Brush.linearGradient(listOf(Color(0xFF221133), Color(0xFF140822)))
    }
}


// -----------------------------------------------------------------------------
// HELPER BUBBLE COMPONENT WIDGETS
// -----------------------------------------------------------------------------
@Composable
fun TextBubble(text: String, isMe: Boolean, chatTheme: String) {
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    val brush = getChatThemeBrush(chatTheme, isMe)

    Box(
        modifier = Modifier
            .clip(shape)
            .background(brush)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun VoiceNoteBubble(duration: Int, isMe: Boolean, chatTheme: String) {
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    val brush = getChatThemeBrush(chatTheme, isMe)
    var isPlaying by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(220.dp)
            .clip(shape)
            .background(brush)
            .clickable { isPlaying = !isPlaying }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                contentDescription = "Voice note status",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )

            InteractiveWaveform(
                modifier = Modifier.weight(1f),
                activePercent = if (isPlaying) 0.8f else 0.4f,
                barCount = 14
            )

            Text("${duration}s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MediaBubble(url: String, isMe: Boolean) {
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = Modifier
            .size(200.dp, 150.dp)
            .clip(shape)
            .border(1.dp, BorderColor, shape)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = "Incoming media",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / 60000
    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}m"
        mins < 1440 -> "${mins / 60}h"
        else -> "${mins / 1440}d"
    }
}

@Composable
fun SwipeToReplyContainer(
    onReply: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX)
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 120f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReply()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount).coerceIn(0f, 150f)
                    }
                )
            }
    ) {
        if (animatedOffsetX > 10f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E1945)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Reply",
                    tint = BrightNeonPurple,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Box(
            modifier = Modifier.offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
        ) {
            content()
        }
    }
}

@Composable
fun MessageActionsDialog(
    message: MessageEntity,
    isMe: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E2C),
        title = {
            Text(
                text = if (isMe) "Your Message" else "Message Options",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (message.type == "text") "\"${message.textContent}\"" else "[Media Attachment]",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (message.type == "text") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopy() }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.LightGray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Copy Text", color = Color.White, fontSize = 14.sp)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReply() }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply", tint = Color.LightGray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Reply to Message", color = Color.White, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onForward() }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = Color.LightGray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Forward Message", color = Color.White, fontSize = 14.sp)
                }

                if (isMe) {
                    if (message.type == "text") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEdit() }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Edit Message", color = Color.White, fontSize = 14.sp)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDelete() }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Unsend", tint = AccentCrimsonPink)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Unsend Message", color = AccentCrimsonPink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Deletes instantly with no alert", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDelete() }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Delete Message (For Me)", color = Color.Red, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = BrightNeonPurple)
            }
        }
    )
}

@Composable
fun ForwardTargetSelectionDialog(
    matches: List<DatingProfileEntity>,
    onDismiss: () -> Unit,
    onSelectTarget: (DatingProfileEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E2C),
        title = {
            Text("Forward to Match", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            if (matches.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No other active matches found to forward to.", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                ) {
                    items(matches) { match ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTarget(match) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(match.profilePics.firstOrNull())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = match.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(match.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BrightNeonPurple)
            }
        }
    )
}
