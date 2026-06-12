package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.FilterList
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.database.DatingProfileEntity
import com.example.data.database.PostEntity
import com.example.ui.components.VioraGradientButton
import com.example.ui.components.VioraFlowRow
import com.example.ui.components.VioraInterestTag
import com.example.ui.components.glassmorphic
import com.example.ui.components.purpleGlow
import com.example.ui.theme.*
import com.example.ui.viewmodel.VioraViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(viewModel: VioraViewModel) {
    val allProfilesList by viewModel.allProfiles.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    val selectedInterests by viewModel.selectedInterests.collectAsState()
    val ageRange by viewModel.ageRange.collectAsState()
    val distanceFilter by viewModel.distanceFilter.collectAsState()
    val postsFeed by viewModel.posts.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }

    // Navigation and transient modal states
    var selectedProfileForDetail by remember { mutableStateOf<DatingProfileEntity?>(null) }
    var showReportModalForProfile by remember { mutableStateOf<DatingProfileEntity?>(null) }
    var showFollowersModalForProfile by remember { mutableStateOf<DatingProfileEntity?>(null) }
    var showInviteModalForProfile by remember { mutableStateOf<DatingProfileEntity?>(null) }

    // Client-side filtration matching the screenshot criteria
    val filteredProfiles = allProfilesList.filter { profile ->
        if (profile.isDisliked) return@filter false

        val matchesSearch = profile.name.contains(searchFilter, ignoreCase = true) ||
                profile.bio.contains(searchFilter, ignoreCase = true) ||
                profile.occupation.contains(searchFilter, ignoreCase = true)
        val matchesInterests = selectedInterests.isEmpty() || profile.interests.any { it in selectedInterests }
        val matchesAge = profile.age.toFloat() in ageRange
        val matchesDistance = profile.distanceKm.toFloat() <= distanceFilter

        val matchesCategory = when (selectedCategory) {
            "Nearby" -> profile.distanceKm <= 10.0
            "Online" -> profile.onlineStatus == "online"
            "New" -> profile.age <= 24
            "Popular" -> profile.verified
            else -> true
        }

        matchesSearch && matchesInterests && matchesAge && matchesDistance && matchesCategory
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkCharcoalBg)
                .statusBarsPadding()
        ) {
            // Header: Title with gradient and sparkles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Explore",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("explore_title")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "✦",
                            color = BrightNeonPurple,
                            fontSize = 24.sp,
                            modifier = Modifier.purpleGlow()
                        )
                    }
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF140B1F))
                            .border(1.dp, BorderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = "Filter Parameters",
                            tint = BrightNeonPurple
                        )
                    }
                }
                Text(
                    text = "Discover amazing people around you 💜",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Search by name, interest or keyword text box
            OutlinedTextField(
                value = searchFilter,
                onValueChange = { viewModel.setSearchFilter(it) },
                placeholder = { Text("Search by name, interest or keyword...", color = TextSecondary, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = BrightNeonPurple) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = BrightNeonPurple,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = DarkSurfaceCard,
                    unfocusedContainerColor = DarkSurfaceCard
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .testTag("search_profiles_input"),
                singleLine = true
            )

            // Category selector row from screenshot ("All", "Nearby", "Online", "New", "Popular")
            val categories = listOf("All", "Nearby", "Online", "New", "Popular")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    val backgroundBrush = if (isSelected) {
                        Brush.horizontalGradient(listOf(BrightNeonPurple, PrimaryPinkPurple))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0xFF130920), Color(0xFF130920)))
                    }
                    val borderStroke = if (isSelected) 0.dp else 1.dp
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(backgroundBrush)
                            .then(
                                if (isSelected) Modifier else Modifier.border(
                                    1.dp,
                                    BorderColor,
                                    RoundedCornerShape(20.dp)
                                )
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val icon = when (category) {
                            "All" -> Icons.Default.GridOn
                            "Nearby" -> Icons.Default.LocationOn
                            "Online" -> Icons.Default.Circle
                            "New" -> Icons.Default.Star
                            "Popular" -> Icons.Default.Whatshot
                            else -> Icons.Default.Circle
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = category,
                            tint = if (category == "Online" && !isSelected) Color.Green else if (isSelected) Color.White else BrightNeonPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Grid of Profiles (2 columns, scrollable)
            if (filteredProfiles.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0x1FBD1DFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = BrightNeonPurple,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No matches found",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try adjusting your search criteria or modifying categories.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProfiles, key = { it.id }) { profile ->
                        ExploreGridCard(
                            profile = profile,
                            onCardClick = { selectedProfileForDetail = profile },
                            onToggleFollow = { viewModel.toggleFollowUser(profile.id, profile.isFollowing) },
                            onBlock = { viewModel.blockProfile(profile.id) },
                            onReport = { showReportModalForProfile = profile }
                        )
                    }
                }
            }
        }

        // Full Screen Profile Detail overlay
        AnimatedVisibility(
            visible = selectedProfileForDetail != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            selectedProfileForDetail?.let { profile ->
                // Ensure real-time following update reflects instantly in modal overlay by looking up state from allProfilesList reference
                val liveProfileState = allProfilesList.find { it.id == profile.id } ?: profile
                FullProfileDetailOverlay(
                    profile = liveProfileState,
                    posts = postsFeed.filter { it.authorId == profile.id },
                    onDismiss = { selectedProfileForDetail = null },
                    onToggleFollow = { viewModel.toggleFollowUser(profile.id, liveProfileState.isFollowing) },
                    onMessage = {
                        viewModel.setActiveChat(profile)
                        selectedProfileForDetail = null
                        viewModel.setTab(2) // Jump to ChatScreen tab
                    },
                    onInvite = { showInviteModalForProfile = profile },
                    onSeeFollowers = { showFollowersModalForProfile = profile },
                    onReport = { showReportModalForProfile = profile },
                    onBlock = {
                        viewModel.blockProfile(profile.id)
                        selectedProfileForDetail = null
                    },
                    onPostClick = { viewModel.setViewingFullscreenPost(it) }
                )
            }
        }

        // Report Modals
        if (showReportModalForProfile != null) {
            ReportReasonDialog(
                profileName = showReportModalForProfile!!.name,
                onDismiss = { showReportModalForProfile = null },
                onSubmitReport = { reason ->
                    viewModel.reportProfile(showReportModalForProfile!!.id, reason)
                    showReportModalForProfile = null
                    // Close the detail screen if it was open for the same user
                    if (selectedProfileForDetail?.id == showReportModalForProfile?.id) {
                        selectedProfileForDetail = null
                    }
                }
            )
        }

        // Invite Modals
        if (showInviteModalForProfile != null) {
            InviteOthersDialog(
                profileName = showInviteModalForProfile!!.name,
                potentialInvitees = allProfilesList.filter { it.id != showInviteModalForProfile!!.id && !it.isDisliked },
                onDismiss = { showInviteModalForProfile = null },
                onSendInvite = { invitee ->
                    viewModel.blockProfile(invitee.id)
                    showInviteModalForProfile = null
                }
            )
        }

        // Followers modal list
        if (showFollowersModalForProfile != null) {
            FollowersListDialog(
                profileName = showFollowersModalForProfile!!.name,
                onDismiss = { showFollowersModalForProfile = null }
            )
        }

        // Discovery range Slider Sheet
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = DarkSurfaceCard,
                dragHandle = { BottomSheetDefaults.DragHandle(color = BorderColor) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(0.8f)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Discovery filters", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Age preference", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "${ageRange.start.toInt()} - ${ageRange.endInclusive.toInt()} years",
                            color = BrightNeonPurple,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    RangeSlider(
                        value = ageRange,
                        onValueChange = { viewModel.setAgeRange(it) },
                        valueRange = 18f..60f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = PrimaryPinkPurple,
                            inactiveTrackColor = BorderColor,
                            thumbColor = BrightNeonPurple
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Maximum distance", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "${distanceFilter.toInt()} km",
                            color = BrightNeonPurple,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = distanceFilter,
                        onValueChange = { viewModel.setDistanceFilter(it) },
                        valueRange = 1f..50f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = PrimaryPinkPurple,
                            inactiveTrackColor = BorderColor,
                            thumbColor = BrightNeonPurple
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Interests and hobbies", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    val interestsList = listOf("Travel", "Technology", "Music", "Movies", "Gaming", "Fitness", "Food", "Photography", "Design", "Sports", "Art", "Fashion", "Yoga", "Beach", "Dancing")
                    VioraFlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        interestsList.forEach { interest ->
                            val isSelected = interest in selectedInterests
                            VioraInterestTag(
                                text = interest,
                                isSelected = isSelected,
                                onClick = { viewModel.toggleInterestFilter(interest) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    VioraGradientButton(
                        text = "Apply Filters",
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Explore Grid Card item (screenshot design)
// -----------------------------------------------------------------------------
@Composable
fun ExploreGridCard(
    profile: DatingProfileEntity,
    onCardClick: () -> Unit,
    onToggleFollow: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
            .testTag("explore_profile_card_${profile.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140B22)),
        border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Profile image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profile.profilePics.firstOrNull())
                    .crossfade(true)
                    .build(),
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Horizontal Vignette Shade
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            // Top Status Pill (Online / Recently Active / New Here) & 3 dots trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Online/Status badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when (profile.onlineStatus) {
                        "online" -> "Online"
                        "away" -> "Recently Active"
                        else -> "New Here"
                    }
                    val statusColor = when (profile.onlineStatus) {
                        "online" -> Color.Green
                        "away" -> Color.Yellow
                        else -> BrightNeonPurple
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3 dots overlay
                Box {
                    IconButton(
                        onClick = { expandedDropdown = true },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options Menu",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.background(DarkSurfaceCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Block", color = Color.White) },
                            onClick = {
                                expandedDropdown = false
                                onBlock()
                            },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = AccentCrimsonPink) }
                        )
                        DropdownMenuItem(
                            text = { Text("Report Profile", color = Color.White) },
                            onClick = {
                                expandedDropdown = false
                                onReport()
                            },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = BrightNeonPurple) }
                        )
                    }
                }
            }

            // Bottom Profile Brief Information
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Name & Verified Badge & Age
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = profile.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile.verified) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified badge",
                            tint = Color(0xFFA832FF),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• ${profile.age}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Location with simple map pin and distance info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Distance marker",
                        tint = AccentCrimsonPink,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${profile.location.substringBefore(",")} • ${profile.distanceKm.toInt()}km away",
                        color = TextSecondary.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Interests Pills row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    profile.interests.take(2).forEach { interest ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x3D130920))
                                .border(0.5.dp, BorderColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = interest, color = BrightNeonPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Circular Action buttons (Dislike 'X' / Follow 'Heart')
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close ('X') block outline button
                    IconButton(
                        onClick = { onBlock() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Pass Profile",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Follow Love icon toggle button
                    val iconColor = if (profile.isFollowing) Color.White else BrightNeonPurple
                    val btnBg = if (profile.isFollowing) {
                        Brush.horizontalGradient(listOf(Color(0xFFB01DFF), Color(0xFFFF2E93)))
                    } else {
                        Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.4f)))
                    }
                    IconButton(
                        onClick = onToggleFollow,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(btnBg)
                            .then(
                                if (profile.isFollowing) Modifier else Modifier.border(
                                    0.5.dp,
                                    BrightNeonPurple.copy(alpha = 0.5f),
                                    CircleShape
                                )
                            )
                    ) {
                        Icon(
                            imageVector = if (profile.isFollowing) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Follow toggle",
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Helpers & Sub-dialogs for Profile Viewer Customizations
// -----------------------------------------------------------------------------
fun getCoverPhotoForProfile(profile: DatingProfileEntity): String {
    if (profile.profilePics.size > 1) {
        return profile.profilePics[1]
    }
    // Return a beautiful consistent Unsplash premium backdrop depending on the profile ID
    val presetCovers = listOf(
        "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&q=80&w=600", // Violet aurora
        "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&q=80&w=600", // Dark mesh gradient
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&q=80&w=600", // Soft purple waves
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&q=80&w=600", // Sunset beach
        "https://images.unsplash.com/photo-1475924156734-496f6cac6ec1?auto=format&fit=crop&q=80&w=600"  // Sunny horizon
    )
    val index = Math.abs(profile.id.hashCode()) % presetCovers.size
    return presetCovers[index]
}

@Composable
fun FullPhotoViewerDialog(
    imageUrl: String?,
    title: String,
    onDismiss: () -> Unit
) {
    if (imageUrl == null) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable { onDismiss() }
        ) {
            // Immersive Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Full Screen Visual",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .align(Alignment.Center)
            )

            // Header Overlay info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.purpleGlow()
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Viewer",
                        tint = Color.White
                    )
                }
            }

            // Bottom Exit Instruction
            Text(
                text = "Tap anywhere to return",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Interactive Full Profile Screen Overlay (Detailed slide-up panel)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullProfileDetailOverlay(
    profile: DatingProfileEntity,
    posts: List<PostEntity>,
    onDismiss: () -> Unit,
    onToggleFollow: () -> Unit,
    onMessage: () -> Unit,
    onInvite: () -> Unit,
    onSeeFollowers: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onPostClick: (PostEntity) -> Unit
) {
    var expandedSettings by remember { mutableStateOf(false) }
    var activeFullPhotoUrl by remember { mutableStateOf<String?>(null) }
    var activeFullPhotoTitle by remember { mutableStateOf("") }

    val coverUrl = getCoverPhotoForProfile(profile)
    val avatarUrl = profile.profilePics.firstOrNull()

    if (activeFullPhotoUrl != null) {
        FullPhotoViewerDialog(
            imageUrl = activeFullPhotoUrl,
            title = activeFullPhotoTitle,
            onDismiss = { activeFullPhotoUrl = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Image Cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${profile.name} Cover Backdrop",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        activeFullPhotoUrl = coverUrl
                        activeFullPhotoTitle = "${profile.name}'s Cover Photo"
                    }
            )

            // Dark linear gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                DarkCharcoalBg
                            )
                        )
                    )
            )

            // Tap prompt on cover backdrop
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 18.dp, end = 72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom cover icon",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tap Cover",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Navigation and Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return List", tint = Color.White)
                }

                // Settings Inside
                Box {
                    IconButton(
                        onClick = { expandedSettings = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings Options", tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = expandedSettings,
                        onDismissRequest = { expandedSettings = false },
                        modifier = Modifier.background(DarkSurfaceCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Invite others to follow", color = Color.White) },
                            onClick = {
                                expandedSettings = false
                                onInvite()
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = BrightNeonPurple) }
                        )
                        DropdownMenuItem(
                            text = { Text("See followers list", color = Color.White) },
                            onClick = {
                                expandedSettings = false
                                onSeeFollowers()
                            },
                            leadingIcon = { Icon(Icons.Default.People, contentDescription = null, tint = BrightNeonPurple) }
                        )
                        DropdownMenuItem(
                            text = { Text("Report profile", color = Color.White) },
                            onClick = {
                                expandedSettings = false
                                onReport()
                            },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = BrightNeonPurple) }
                        )
                        DropdownMenuItem(
                            text = { Text("Block profile", color = Color.White) },
                            onClick = {
                                expandedSettings = false
                                onBlock()
                            },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = AccentCrimsonPink) }
                        )
                    }
                }
            }

            // Quick display of profile avatar, name, age, and location
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overlapping clickable circular profile avatar
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF140B22))
                        .border(3.dp, BrightNeonPurple, CircleShape)
                        .clickable {
                            activeFullPhotoUrl = avatarUrl
                            activeFullPhotoTitle = "${profile.name}'s Profile Photo"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${profile.name} Profile Pic",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Zoom overlay indicator badge on avatar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(BrightNeonPurple)
                            .border(1.5.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Metadata details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name,
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.purpleGlow()
                        )
                        if (profile.verified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified badge",
                                tint = Color(0xFFA832FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Age: ${profile.age}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.5f))
                        )
                        Text(
                            text = "Tap photo to zoom 🔍",
                            color = BrightNeonPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "pin",
                            tint = AccentCrimsonPink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile.location} • ${profile.distanceKm.toInt()} km away",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Detailed Content Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Stats Row: Followers, Following, Posts counts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceCard)
                    .border(0.5.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSeeFollowers() }
                ) {
                    Text(text = "1.4k", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Followers", color = TextSecondary, fontSize = 11.sp)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderColor))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "340", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Following", color = TextSecondary, fontSize = 11.sp)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderColor))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${posts.size}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Posts", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Row: Follow Toggle & Message
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Follow Outline Button
                val followText = if (profile.isFollowing) "Following" else "Follow"
                val followBorder = if (profile.isFollowing) BorderColor else BrightNeonPurple
                Button(
                    onClick = onToggleFollow,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (profile.isFollowing) Color.Transparent else Color(0x2BBD1DFF)
                    ),
                    border = BorderStroke(1.dp, followBorder),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = if (profile.isFollowing) Icons.Default.Check else Icons.Default.Favorite,
                        contentDescription = "follow action icon",
                        tint = if (profile.isFollowing) Color.White else BrightNeonPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = followText, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Chat direct message buttons
                Button(
                    onClick = onMessage,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .purpleGlow(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(BrightNeonPurple, PrimaryPinkPurple)))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChatBubble, contentDescription = "message", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Message", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Biography Section
            Text(
                text = "Biography",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.bio,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Job occupation & School
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF140B1F))
                    .padding(12.dp)
            ) {
                Icon(Icons.Default.Work, contentDescription = "Work icon", tint = BrightNeonPurple, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = profile.occupation, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = profile.education, color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Beautiful flow row values
            Text(
                text = "Interests & Hobbies",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            VioraFlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profile.interests.forEach { interest ->
                    VioraInterestTag(text = interest)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Visual Posts Grid Section
            Text(
                text = "Recent Posts",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No posts from this user yet", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(posts) { post ->
                        val hasImages = post.imageUrls.isNotEmpty()
                        val matchingBgBrush = when (post.backgroundColor) {
                            "cosmic_violet" -> Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF1A237E)))
                            "solar_flame" -> Brush.linearGradient(listOf(Color(0xFF880E4F), Color(0xFFE65100)))
                            "aurora_teal" -> Brush.linearGradient(listOf(Color(0xFF003020), Color(0xFF0C2B52)))
                            "cotton_candy" -> Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63)))
                            "royal_gold" -> Brush.linearGradient(listOf(Color(0xFF2E1A47), Color(0xFF8B6508)))
                            "crimson_dark" -> Brush.linearGradient(listOf(Color(0xFF3E0610), Color(0xFF150205)))
                            else -> null
                        }
                        val matchingBgSolidColor = if (post.backgroundColor == "charcoal_solid") Color(0xFF263238) else Color(0xFF10081C)
                        
                        val boxBackgroundModifier = if (!hasImages) {
                            if (matchingBgBrush != null) {
                                Modifier.background(matchingBgBrush)
                            } else {
                                Modifier.background(matchingBgSolidColor)
                            }
                        } else {
                            Modifier
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPostClick(post) },
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(boxBackgroundModifier)
                            ) {
                                if (hasImages) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(post.imageUrls.firstOrNull())
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.8f)
                                                )
                                            )
                                        )
                                        .padding(6.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Text(
                                        text = post.caption,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (post.imageUrls.size > 1) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "+${post.imageUrls.size - 1}",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// -----------------------------------------------------------------------------
// Report Reason Modal popup Dialog
// -----------------------------------------------------------------------------
@Composable
fun ReportReasonDialog(
    profileName: String,
    onDismiss: () -> Unit,
    onSubmitReport: (String) -> Unit
) {
    val reportOptions = listOf(
        "Fake visual content or profile",
        "Spam, bot, or inappropriate chat flow",
        "Underage user",
        "Harassment, hate language, or bullying",
        "Other safety reason"
    )
    var selectedReason by remember { mutableStateOf(reportOptions[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header block
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = BrightNeonPurple,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Report ${profileName}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Please select a reason below. This helps us manually audit profiles and enforce safety rules.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Options list selection buttons
                reportOptions.forEach { option ->
                    val isChosen = selectedReason == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isChosen) Color(0x33BBD1FF) else Color.Transparent)
                            .clickable { selectedReason = option }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isChosen,
                            onClick = { selectedReason = option },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BrightNeonPurple,
                                unselectedColor = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CTA action sheet buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = { onSubmitReport(selectedReason) },
                        modifier = Modifier
                            .weight(1f)
                            .purpleGlow(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(BrightNeonPurple, PrimaryPinkPurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Submit", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Invite Others modal dialog popup
// -----------------------------------------------------------------------------
@Composable
fun InviteOthersDialog(
    profileName: String,
    potentialInvitees: List<DatingProfileEntity>,
    onDismiss: () -> Unit,
    onSendInvite: (DatingProfileEntity) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Invite connection to follow",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Invite other members to look at ${profileName}'s profile and explore digital creator links.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (potentialInvitees.isEmpty()) {
                    Text(
                        "No other members available to invite",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    potentialInvitees.take(4).forEach { invitee ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF140B1F))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = invitee.profilePics.firstOrNull(),
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(invitee.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onSendInvite(invitee) },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrightNeonPurple)
                            ) {
                                Text("Invite", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// See Followers List Modal dialog popup
// -----------------------------------------------------------------------------
@Composable
fun FollowersListDialog(
    profileName: String,
    onDismiss: () -> Unit
) {
    val mockFollowers = listOf(
        Pair("Sharon", "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=150"),
        Pair("Amina", "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=150"),
        Pair("John", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=150"),
        Pair("Michael", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=150")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "${profileName}'s Followers",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                mockFollowers.forEach { follower ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = follower.second,
                                contentDescription = "",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(follower.first, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        // Followers toggle status indicators
                        var isFollowingBack by remember { mutableStateOf(false) }
                        val btnText = if (isFollowingBack) "Following" else "Follow Back"
                        val btnColor = if (isFollowingBack) Color.Transparent else BrightNeonPurple
                        val btnBorder = if (isFollowingBack) BorderStroke(1.dp, BorderColor) else null

                        Button(
                            onClick = { isFollowingBack = !isFollowingBack },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                            border = btnBorder
                        ) {
                            Text(btnText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                VioraGradientButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
