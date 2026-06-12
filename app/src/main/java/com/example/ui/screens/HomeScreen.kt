package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.database.CommentEntity
import com.example.data.database.DatingProfileEntity
import com.example.data.database.PostEntity
import com.example.ui.components.glassmorphic
import com.example.ui.components.purpleGlow
import com.example.ui.theme.*
import com.example.ui.viewmodel.VioraViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VioraViewModel,
    onNavigateToChat: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    val posts by viewModel.posts.collectAsState()
    val profiles by viewModel.allProfiles.collectAsState()
    val commentsPost by viewModel.commentingPost.collectAsState()
    val myProfileState by viewModel.userProfile.collectAsState()
    val isMyProfileVerified = myProfileState?.verificationStatus == "verified"

    val isOnline by viewModel.isOnline.collectAsState()
    val isSlowConnection by viewModel.isSlowConnection.collectAsState()
    val isFetchingPosts by viewModel.isNetworkFetchingPosts.collectAsState()
    val pendingQueue by viewModel.pendingQueue.collectAsState()

    var feedFilter by remember { mutableStateOf("For You") } // "For You", "Following", "Liked You"
    var videoRatioFilter by remember { mutableStateOf("All") } // "All", "9:16", "16:9", "1:1"
    var searchQuery by remember { mutableStateOf("") }

    val lazyListState = rememberLazyListState()

    val shouldPreload = remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            
            // Trigger preloading if last visible item reaches totalItemsCount - 10
            totalItemsCount > 2 && lastVisibleItemIndex >= totalItemsCount - 10
        }
    }

    LaunchedEffect(shouldPreload.value) {
        if (shouldPreload.value && isOnline) {
            viewModel.preloadNextTenPosts()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
    ) {
        // 1. Customized Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Good evening,",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Alex 👋",
                        color = BrightNeonPurple,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Find your perfect match 💜",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Notification / Story Quick Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF140B1F))
                        .border(1.dp, BorderColor, CircleShape)
                ) {
                    BadgedBox(
                        badge = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(AccentCrimsonPink, CircleShape)
                            )
                        }
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = Color.White)
                    }
                }
            }
        }

        // 1.5. Interactive Search Bar with Auto-Suggestions
        val allPossibleSuggestions = listOf(
            // Categories & Filters
            "Nearby", "Online", "New", "Popular", "Following", "Liked You", "For You",
            // Interests
            "Technology", "Design", "Music", "Photography", "Travel", "Art", "Books", "Coffee", "Fitness",
            // Names of authors
            "Sophia Martinez", "James Carter", "Isabella Wilson", "Olivia Anderson", "Sharon", "Amina"
        )
        
        val activeSuggestions = remember(searchQuery) {
            if (searchQuery.isBlank()) {
                listOf("Nearby", "Online", "Music", "Travel") // Default recommended categories
            } else {
                allPossibleSuggestions.filter {
                    it.contains(searchQuery, ignoreCase = true) && !it.equals(searchQuery, ignoreCase = true)
                }.take(5)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .testTag("home_search_input"),
                placeholder = {
                    Text(
                        text = "Search categories, posts, interests...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = BrightNeonPurple
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF140B24),
                    unfocusedContainerColor = Color(0xFF140B24),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
        }

        if (activeSuggestions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text(
                        text = if (searchQuery.isBlank()) "Suggestions:" else "Matches:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                items(activeSuggestions) { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF231435))
                            .border(0.5.dp, BrightNeonPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { searchQuery = suggestion }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("suggestion_${suggestion.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val icon = when {
                                listOf("Nearby", "Online", "New", "Popular", "Following", "Liked You", "For You").contains(suggestion) -> Icons.Default.Category
                                listOf("Sophia Martinez", "James Carter", "Isabella Wilson", "Olivia Anderson", "Sharon", "Amina").contains(suggestion) -> Icons.Default.Person
                                else -> Icons.Default.LocalFireDepartment
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = BrightNeonPurple,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = suggestion,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Connection Controller and Interconnectivity Dashboards
        NetworkSyncDashboard(
            viewModel = viewModel,
            isOnline = isOnline,
            isSlowConnection = isSlowConnection,
            pendingQueueSize = pendingQueue.size
        )

        if (!isOnline) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF330E14))
                    .border(1.dp, Color(0xFFC62828), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Enable Internet to see posts & post",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Offline Mode is active. New posts, comments, and likes will queue locally and sync silently when connection returns.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // 2. Main Social Column
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Horizontal stories card
            item {
                StoriesRow(profiles = profiles, onStoryClick = {
                    viewModel.setActiveChat(it)
                    viewModel.setTab(2) // Jump to Chat
                    onNavigateToChat()
                })
            }

            // Custom Filter Tabs matching screenshots (For You, Following, Liked You)
            item {
                HomeFilterPills(
                    selectedFilter = feedFilter,
                    onFilterChange = { feedFilter = it }
                )
            }

            // Video Feed selection
            val activePosts = posts.filter { !it.isReported }
            val filteredPosts = if (feedFilter == "Videos") {
                val videoPosts = activePosts.filter { it.videoUrl != null }
                if (searchQuery.isNotEmpty()) {
                    videoPosts.filter { post ->
                        post.authorName.contains(searchQuery, ignoreCase = true) || 
                        post.caption.contains(searchQuery, ignoreCase = true)
                    }
                } else {
                    videoPosts
                }
            } else if (searchQuery.isNotEmpty()) {
                activePosts.filter { post ->
                    val matchingProfile = profiles.find { it.id == post.authorId }
                    
                    val matchesAuthor = post.authorName.contains(searchQuery, ignoreCase = true)
                    val matchesCaption = post.caption.contains(searchQuery, ignoreCase = true)
                    val matchesInterests = matchingProfile?.interests?.any { it.contains(searchQuery, ignoreCase = true) } == true
                    val matchesCategory = when (searchQuery.lowercase().trim()) {
                        "nearby" -> matchingProfile?.distanceKm != null && matchingProfile.distanceKm < 5.0
                        "online" -> matchingProfile?.onlineStatus == "online"
                        "new" -> post.authorLabel.contains("ago", ignoreCase = true) || post.authorLabel.contains("now", ignoreCase = true)
                        "popular" -> post.likesCount > 10
                        "following" -> matchingProfile?.isFollowing == true
                        "liked you" -> post.authorName in listOf("Sophia Martinez")
                        "tiktok", "reels", "reel", "9:16", "vertical" -> post.videoUrl != null && (post.videoUrl.contains("skating") || post.videoUrl.contains("dancing") || post.videoUrl.contains("vertical") || post.videoUrl.contains("tiktok") || post.videoUrl.contains("reel"))
                        "youtube", "landscape", "widescreen", "16:9" -> post.videoUrl != null && (post.videoUrl.contains("joyrides") || post.videoUrl.contains("bunny") || post.videoUrl.contains("landscape") || post.videoUrl.contains("youtube"))
                        else -> false
                    }
                    
                    matchesAuthor || matchesCaption || matchesInterests || matchesCategory
                }
            } else {
                when (feedFilter) {
                    "Following" -> activePosts.filter { it.authorName in listOf("Sharon") }
                    "Liked You" -> activePosts.filter { it.authorName in listOf("Sophia Martinez") }
                    else -> activePosts
                }
            }

            if (isFetchingPosts) {
                items(3) {
                    PostSkeletonCard()
                }
            } else if (filteredPosts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Empty feed",
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No matching posts yet.", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    val matchingProfile = profiles.find { it.id == post.authorId }
                    PostFeedCard(
                        post = post,
                        profile = matchingProfile,
                        isSlowConnection = isSlowConnection,
                        isMeVerified = isMyProfileVerified,
                        viewModel = viewModel,
                        onLike = { viewModel.togglePostLike(post) },
                        onCommentClick = { viewModel.setCommentingPost(post) },
                        onFollowToggle = {
                            matchingProfile?.let {
                                viewModel.toggleFollowUser(it.id, it.isFollowing)
                            }
                        },
                        onPostClick = { viewModel.setViewingFullscreenPost(post) }
                    )
                }
            }
        }
    }

    // 3. Comments Drawer Sheet
    if (commentsPost != null) {
        val post = commentsPost!!
        val commentsFlow = viewModel.getCommentsForPost(post.id).collectAsState(initial = emptyList())
        val comments = commentsFlow.value

        var commentText by remember { mutableStateOf("") }
        var replyingToComment by remember { mutableStateOf<CommentEntity?>(null) }

        ModalBottomSheet(
            onDismissRequest = { 
                viewModel.setCommentingPost(null)
                replyingToComment = null
            },
            containerColor = DarkSurfaceCard,
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = BorderColor) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Comments (${comments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val parentComments = remember(comments) { comments.filter { it.parentId == null } }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    parentComments.forEach { parent ->
                        // 1. Top-Level Parent Comment
                        item(key = parent.id) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize(),
                                verticalAlignment = Alignment.Top
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(parent.authorAvatar)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = parent.authorName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(parent.authorName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        
                                        val mAgo = (System.currentTimeMillis() - parent.timestamp) / 60000
                                        val timeStr = if (mAgo < 1) "just now" else if (mAgo < 60) "${mAgo}m ago" else "${mAgo / 60}h ago"
                                        Text(timeStr, color = TextSecondary, fontSize = 11.sp)
                                    }
                                    
                                    Text(parent.text, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                                    
                                    // Actions strip
                                    Row(
                                        modifier = Modifier.padding(top = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Reply",
                                            color = BrightNeonPurple,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .clickable { replyingToComment = parent }
                                                .padding(end = 16.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.weight(1f))
                                        
                                        // Comment Liking click area
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                viewModel.toggleCommentLike(parent.id, parent.isLikedByMe, parent.likesCount)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (parent.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Like parent comment",
                                                tint = if (parent.isLikedByMe) AccentCrimsonPink else TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            if (parent.likesCount > 0) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = parent.likesCount.toString(),
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Inline Indented Replies (Threaded)
                        val replies = comments.filter { it.parentId == parent.id }
                        items(replies, key = { it.id }) { reply ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 28.dp)
                                    .animateContentSize(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Thread vertical connection guide bar
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(36.dp)
                                        .background(BorderColor.copy(alpha = 0.5f))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(reply.authorAvatar)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = reply.authorName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(reply.authorName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                        
                                        val mAgo = (System.currentTimeMillis() - reply.timestamp) / 60000
                                        val timeStr = if (mAgo < 1) "just now" else if (mAgo < 60) "${mAgo}m ago" else "${mAgo / 60}h ago"
                                        Text(timeStr, color = TextSecondary, fontSize = 11.sp)
                                    }
                                    
                                    Text(reply.text, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                                    
                                    Row(
                                        modifier = Modifier.padding(top = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        
                                        // Like Action Button for Reply
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                viewModel.toggleCommentLike(reply.id, reply.isLikedByMe, reply.likesCount)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (reply.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Like active reply",
                                                tint = if (reply.isLikedByMe) AccentCrimsonPink else TextSecondary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            if (reply.likesCount > 0) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = reply.likesCount.toString(),
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Reply Indicator
                AnimatedVisibility(
                    visible = replyingToComment != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    replyingToComment?.let { target ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .background(Color(0xFF1E112E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Replying to @${target.authorName}",
                                color = AccentCrimsonPink,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { replyingToComment = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply target",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { 
                            Text(
                                text = if (replyingToComment != null) "Reply to @${replyingToComment!!.authorName}..." else "Write comment...", 
                                color = TextSecondary
                            ) 
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrightNeonPurple,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color(0xFF10081C),
                            unfocusedContainerColor = Color(0xFF10081C)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            if (commentText.trim().isNotEmpty()) {
                                viewModel.createComment(post.id, commentText, parentId = replyingToComment?.id)
                                commentText = ""
                                replyingToComment = null
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryPinkPurple)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Stories row widget
// -----------------------------------------------------------------------------
@Composable
fun StoriesRow(
    profiles: List<DatingProfileEntity>,
    onStoryClick: (DatingProfileEntity) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "Your Story" button
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF140B1F))
                            .border(1.dp, BorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add story", tint = PrimaryPinkPurple, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Your Story", color = TextSecondary, fontSize = 12.sp)
            }
        }

        // Dynamic stories matching screenshots
        items(profiles, key = { it.id }) { profile ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onStoryClick(profile) }
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    // Avatar styled with glowing neon ring
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                brush = Brush.sweepGradient(listOf(Color(0xFFB01DFF), Color(0xFFFF2E93))),
                                shape = CircleShape
                            )
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(profile.profilePics.firstOrNull())
                                .crossfade(true)
                                .build(),
                            contentDescription = profile.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    // Online indicator dot
                    if (profile.onlineStatus == "online") {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(OnlineGreen)
                                .border(2.dp, DarkCharcoalBg, CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = profile.name.split(" ").firstOrNull() ?: "",
                    color = Color.White,
                    fontSize = 12.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Category Filters (For You, Following, Liked You)
// -----------------------------------------------------------------------------
@Composable
fun HomeFilterPills(
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .background(Color(0xFF140B1F), RoundedCornerShape(24.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filters = listOf("For You", "Following", "Liked You", "Videos")
        filters.forEach { filter ->
            val isSelected = selectedFilter == filter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) PrimaryPinkPurple else Color.Transparent)
                    .clickable { onFilterChange(filter) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// High-Fidelity Social feed Card
// -----------------------------------------------------------------------------
@Composable
fun PostFeedCard(
    post: PostEntity,
    profile: DatingProfileEntity?,
    isSlowConnection: Boolean = false,
    isMeVerified: Boolean = false,
    viewModel: VioraViewModel,
    onLike: () -> Unit,
    onCommentClick: () -> Unit,
    onFollowToggle: () -> Unit,
    onPostClick: () -> Unit
) {
    val context = LocalContext.current
    val isShortWithBg = post.backgroundColor != null && post.backgroundColor != "none" && post.caption.length <= 100 && post.imageUrls.isEmpty() && post.videoUrl == null

    var simulatedLoadDelayComplete by remember(post.id) { mutableStateOf(false) }

    if (isSlowConnection) {
        LaunchedEffect(post.id) {
            simulatedLoadDelayComplete = false
            kotlinx.coroutines.delay(2800)
            simulatedLoadDelayComplete = true
        }
    } else {
        simulatedLoadDelayComplete = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_shimmer")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val cardBackgroundModifier = if (isShortWithBg) {
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
        if (brush != null) {
            Modifier.background(brush)
        } else {
            Modifier.background(solidColor)
        }
    } else {
        Modifier.background(DarkSurfaceCard)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(22.dp))
            .then(cardBackgroundModifier)
            .testTag("post_card_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (isShortWithBg) Color.White.copy(alpha = 0.3f) else BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (simulatedLoadDelayComplete) {
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
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f * pulseAlpha))
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (profile?.verified == true || (post.authorId == "me" && isMeVerified)) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Verified, contentDescription = "Verified badge", tint = BrightNeonPurple, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = post.authorLabel,
                            color = if (isShortWithBg) Color.White.copy(alpha = 0.8f) else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Follow button
                    if (profile != null) {
                        val isFollowing = profile.isFollowing
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isFollowing) Color(0xFF231233) else PrimaryPinkPurple)
                                .clickable { onFollowToggle() }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Safe safety and deletions dropdown options
                    var showDropdown by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showDropdown = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Security tools", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            modifier = Modifier.background(Color(0xFF160E21))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report safe guidelines break", color = Color.White, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Report, contentDescription = "", tint = Color(0xFFFF2E93), modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showDropdown = false
                                    viewModel.reportPost(post.id)
                                    android.widget.Toast.makeText(context, "Guideline infraction reported safely! This post will be hidden.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                            if (post.authorId == "me") {
                                DropdownMenuItem(
                                    text = { Text("Delete my post card", color = Color(0xFFFF2E93), fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "", tint = Color(0xFFFF2E93), modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        showDropdown = false
                                        viewModel.deleteOwnPost(post.id)
                                        android.widget.Toast.makeText(context, "Post deleted successfully.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Central Image / MULTI-IMAGE CAROUSEL / VIDEO PLAYER
            if (post.imageUrls.isNotEmpty()) {
                var selectedImageIndex by remember { mutableStateOf(0) }
                val currentImageIndex = selectedImageIndex.coerceIn(0, post.imageUrls.size - 1)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPostClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (simulatedLoadDelayComplete) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(post.imageUrls[currentImageIndex])
                                .crossfade(true)
                                .build(),
                            contentDescription = "Post Artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF1F1138),
                                            Color(0xFF140B25)
                                        )
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f * pulseAlpha)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Loading image skeleton",
                                        tint = BrightNeonPurple.copy(alpha = 0.5f * pulseAlpha),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.08f * pulseAlpha))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.05f * pulseAlpha))
                                )
                            }
                        }
                    }

                    // Navigation Chevrons if there are multiple images
                    if (post.imageUrls.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Arrow
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable {
                                        selectedImageIndex = if (selectedImageIndex > 0) {
                                            selectedImageIndex - 1
                                        } else {
                                            post.imageUrls.size - 1
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Image",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Right Arrow
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable {
                                        selectedImageIndex = if (selectedImageIndex < post.imageUrls.size - 1) {
                                            selectedImageIndex + 1
                                        } else {
                                            0
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next Image",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Top-right count indicator (e.g. "1/3")
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${currentImageIndex + 1}/${post.imageUrls.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Interest Tag Pills overlaid beautifully
                    if (profile != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            profile.interests.take(2).forEach { interest ->
                                val absHash = if (interest.hashCode() < 0) -interest.hashCode() else interest.hashCode()
                                val glowColor = if (absHash % 2 == 0) BrightNeonPurple else AccentCrimsonPink
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xE612081E), RoundedCornerShape(12.dp))
                                        .border(0.8.dp, glowColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = when (interest) {
                                        "Photography" -> Icons.Default.CameraAlt
                                        "Travel" -> Icons.Default.AirplanemodeActive
                                        "Music" -> Icons.Default.MusicNote
                                        "Coffee" -> Icons.Default.Coffee
                                        else -> Icons.Default.Star
                                    }
                                    Icon(icon, contentDescription = interest, tint = glowColor, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(interest, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else if (post.videoUrl != null) {
                // Inline Video Reel Component with standard autoplay & ratio auto-detection
                var isPreparingVideo by remember { mutableStateOf(true) }
                var mediaPlayerInstance by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                val globalMute by viewModel.isMuted.collectAsState()

                // Double tap overlay heart animations
                var showHeartState by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                // Aspect ratio dynamic memory
                var calculatedRatio by remember(post.id) { mutableStateOf(16f / 9f) }
                var detectedPlatformStr by remember(post.id) { mutableStateOf("YouTube Standard (16:9)") }
                var videoResolutionLabel by remember(post.id) { mutableStateOf("1920x1080 (FHD)") }

                // Heuristic detection initially to prevent layout jumps
                LaunchedEffect(post.videoUrl) {
                    if (post.videoUrl.contains("skating") || post.videoUrl.contains("dancing") || post.videoUrl.contains("vertical") || post.videoUrl.contains("reel") || post.videoUrl.contains("tiktok")) {
                        calculatedRatio = 0.5625f // 9:16
                        detectedPlatformStr = "TikTok Reel (9:16)"
                        videoResolutionLabel = "1080x1920 (Portrait)"
                    } else if (post.videoUrl.contains("joyrides") || post.videoUrl.contains("bunny") || post.videoUrl.contains("landscape")) {
                        calculatedRatio = 1.777f // 16:9
                        detectedPlatformStr = "YouTube Widescreen (16:9)"
                        videoResolutionLabel = "1920x1080 (Landscape)"
                    } else {
                        calculatedRatio = 1.0f // Square
                        detectedPlatformStr = "Facebook / Insta Square (1:1)"
                        videoResolutionLabel = "1080x1080 (Square)"
                    }
                }

                // Register visual impression count
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

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    val containerWidth = maxWidth
                    // Responsive heights: Vertical cap 380dp for scrolling feeds, otherwise calculate
                    val capHeight = 360.dp
                    val rawHeight = containerWidth / calculatedRatio
                    val finalHeight = if (rawHeight > capHeight) capHeight else rawHeight

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(finalHeight)
                            .clickable { onPostClick() }
                            .pointerInput(post.id) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (!post.isLiked) {
                                            onLike()
                                        }
                                        coroutineScope.launch {
                                            showHeartState = true
                                            delay(600)
                                            showHeartState = false
                                        }
                                    },
                                    onTap = { onPostClick() }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Buffering Shimmer Skeleton
                        if (isPreparingVideo) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = BrightNeonPurple, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Detecting aspect ratio & streaming...", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                            }
                        }

                        // Native Video View Embedded
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
                                                "TikTok Reel (9:16)"
                                            } else if (calculatedRatio > 1.3f) {
                                                "YouTube Wide (16:9)"
                                            } else {
                                                "Facebook Square (1:1)"
                                            }
                                            detectedPlatformStr = category
                                            videoResolutionLabel = "${w}x${h} HD"
                                        }
                                        mp.isLooping = true
                                        val currentVol = if (globalMute) 0f else 1f
                                        mp.setVolume(currentVol, currentVol)
                                        mp.start() // Instantly triggers autoplay!
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
                            modifier = Modifier.fillMaxSize()
                        )

                    // Double tap heart scale overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showHeartState,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "",
                            tint = AccentCrimsonPink,
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    // Floating overlays: View Count and Mute Button
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text("${post.viewsCount} views", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.width(1.dp).height(10.dp).background(Color.White.copy(alpha = 0.3f)))
                        Icon(
                            imageVector = if (globalMute) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute button",
                            tint = Color.White,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { viewModel.toggleMute() }
                        )
                    }

                    // Reel label duration indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Reel • ${post.videoDuration}s", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Description / Caption Label
            if (isShortWithBg) {
                // Centered large colorful status post style
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPostClick() }
                        .padding(vertical = 32.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.caption,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // Description Label with Read More for lengthy text
                var isExpanded by remember { mutableStateOf(false) }
                val captionLimit = 140
                val isLengthy = post.caption.length > captionLimit

                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = if (isLengthy && !isExpanded) {
                            "${post.caption.take(captionLimit)}..."
                        } else {
                            post.caption
                        },
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.animateContentSize()
                    )
                    if (isLengthy) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isExpanded) "Read Less" else "Read More",
                            color = BrightNeonPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { isExpanded = !isExpanded }
                        )
                    }
                }
            }

            // Likes/Comments action row matching style in screenshots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Actions (Likes, comments, bookmark, share)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLike() }
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like icon",
                            tint = if (post.isLiked) AccentCrimsonPink else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${post.likesCount}", color = Color.White, fontSize = 13.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onCommentClick() }
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comment outline", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${post.commentsCount}", color = Color.White, fontSize = 13.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            viewModel.toggleBookmark(post)
                            val word = if (post.isBookmarked) "Removed from bookmarks" else "Saved to bookmarks!"
                            android.widget.Toast.makeText(context, word, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = if (post.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark outline",
                            tint = if (post.isBookmarked) Color(0xFFFFEB3B) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            android.widget.Toast.makeText(context, "Reel post link copied to clipboard! Share the joy.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share outline",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Hot neon "Like" card pill to toggle Swipe match trigger directly!
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (post.isLiked) AccentCrimsonPink else Color(0x33B01DFF))
                        .clickable { onLike() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = "Like badge", tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (post.isLiked) "Liked" else "Like", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PostSkeletonCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.08f))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.08f))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.04f))
        )
    }
}

@Composable
fun NetworkSyncDashboard(
    viewModel: VioraViewModel,
    isOnline: Boolean,
    isSlowConnection: Boolean,
    pendingQueueSize: Int
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF160E2A))
            .border(1.dp, if (isOnline) Color(0xFF321E53) else Color(0xFF6B1B3C), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (!isOnline) Color(0xFFD32F2F) else if (isSlowConnection) Color(0xFFFFA000) else Color(0xFF388E3C),
                            shape = CircleShape
                        )
                )

                Text(
                    text = if (!isOnline) "Offline Mode (Cached Only)" else if (isSlowConnection) "Slow Connection Mode" else "Viora Online",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (pendingQueueSize > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE91E63))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${pendingQueueSize} queued to sync",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isExpanded) "Hide Options" else "Network Diagnostics",
                    color = BrightNeonPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BrightNeonPurple,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isOnline) Color(0xFF50132B) else Color(0xFF1E1537))
                            .clickable { viewModel.toggleSimulatedOffline() }
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Force Offline",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (!isOnline) "WiFi connected but no internet" else "Tap to trigger offline cache saving",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSlowConnection) Color(0xFF553D0E) else Color(0xFF1E1537))
                            .clickable { viewModel.toggleSimulatedSlow() }
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Slow Connection",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isSlowConnection) "Text first prioritised" else "Tap to prioritize text over graphics",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sync checks internet silently in bg.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Button(
                        onClick = { viewModel.triggerManualPostsFetch() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrightNeonPurple),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp),
                        enabled = isOnline
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refetch Feed", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
