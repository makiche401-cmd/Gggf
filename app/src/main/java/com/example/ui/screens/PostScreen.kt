package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Movie
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
import com.example.data.database.PostEntity
import com.example.ui.components.VioraGradientButton
import com.example.ui.components.glassmorphic
import com.example.ui.components.purpleGlow
import com.example.ui.theme.*
import com.example.ui.viewmodel.VioraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PostBgOption(
    val id: String,
    val name: String,
    val brush: Brush?,
    val solidColor: Color = Color.Transparent,
    val isSolid: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(viewModel: VioraViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val posts by viewModel.posts.collectAsState()
    val ownPosts = posts.filter { it.authorId == "me" }

    // Navigation state inside screen: Photo vs Video Post
    var postTypeTab by remember { mutableStateOf("Photo") } // "Photo", "Video"

    var caption by remember { mutableStateOf("") }
    var privacySetting by remember { mutableStateOf("Everyone") }
    var commentSetting by remember { mutableStateOf("Everyone") }
    
    // -------------------------------------------------------------------------
    // PHOTO POST STATE
    // -------------------------------------------------------------------------
    val attachedImageUris = remember { mutableStateListOf<String>() }
    var isCompressingPhotos by remember { mutableStateOf(false) }
    var selectedBgId by remember { mutableStateOf("none") }
    var showColorPicker by remember { mutableStateOf(false) }
    val maxBgCharLimit = 100

    val postBgOptions = remember {
        listOf(
            PostBgOption(id = "none", name = "Default Dark", brush = null, solidColor = Color(0xFF10081C), isSolid = true),
            PostBgOption(id = "cosmic_violet", name = "Cosmic Violet", brush = Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF1A237E)))),
            PostBgOption(id = "solar_flame", name = "Solar Flame", brush = Brush.linearGradient(listOf(Color(0xFF880E4F), Color(0xFFE65100)))),
            PostBgOption(id = "aurora_teal", name = "Aurora Teal", brush = Brush.linearGradient(listOf(Color(0xFF003020), Color(0xFF0C2B52)))),
            PostBgOption(id = "cotton_candy", name = "Cotton Candy", brush = Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63)))),
            PostBgOption(id = "royal_gold", name = "Royal Gold", brush = Brush.linearGradient(listOf(Color(0xFF2E1A47), Color(0xFF8B6508)))),
            PostBgOption(id = "charcoal_solid", name = "Charcoal", brush = null, solidColor = Color(0xFF263238), isSolid = true),
            PostBgOption(id = "crimson_dark", name = "Crimson Dark", brush = Brush.linearGradient(listOf(Color(0xFF3E0610), Color(0xFF150205))))
        )
    }
    val currentBgOption = postBgOptions.find { it.id == selectedBgId } ?: postBgOptions.first()
    val canUseBgColor = selectedBgId != "none" && attachedImageUris.isEmpty() && caption.length <= maxBgCharLimit

    val presetImagesList = listOf(
        "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&q=80&w=400",
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&q=80&w=400",
        "https://images.unsplash.com/photo-1516280440614-37939bbacd6a?auto=format&fit=crop&q=80&w=400",
        "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?auto=format&fit=crop&q=80&w=400"
    )

    val photoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val spaceLeft = 3 - attachedImageUris.size
            if (spaceLeft <= 0) {
                Toast.makeText(context, "Maximum 3 photos can be attached.", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val toAdd = uris.take(spaceLeft)
            toAdd.forEach { uri -> attachedImageUris.add(uri.toString()) }
        }
    }

    // -------------------------------------------------------------------------
    // VIDEO POST STATE & METADATA SELECTION
    // -------------------------------------------------------------------------
    var selectedVideoUri by remember { mutableStateOf<String?>(null) }
    var selectedVideoDuration by remember { mutableStateOf(0) } // in seconds
    var generatedThumbnailPath by remember { mutableStateOf<String?>(null) }
    var videoFormatError by remember { mutableStateOf<String?>(null) }

    // Video Trimming parameters
    var trimStartSec by remember { mutableStateOf(0) }
    var trimEndSec by remember { mutableStateOf(60) }

    // Upload & Compression Status Progress
    var isUploadingVideo by remember { mutableStateOf(false) }
    var uploadProgressPercentage by remember { mutableStateOf(0) }
    var uploadProgressStatusMsg by remember { mutableStateOf("") }
    var simulatedCompressedSizeMb by remember { mutableStateOf(0.0) }

    val presetVideosList = listOf(
        Pair("Skate Sunset (Preset)", "https://assets.mixkit.co/videos/preview/mixkit-young-woman-skating-at-sunset-41712-large.mp4"),
        Pair("Dance Neon (Preset)", "https://assets.mixkit.co/videos/preview/mixkit-woman-dancing-in-front-of-wall-with-neon-lights-39875-large.mp4"),
        Pair("Ambient Loop (Preset)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
    )

    // Function to generate dynamic thumbnail on IO flow
    suspend fun generateThumbnailFromUri(uriStr: String): String? = withContext(Dispatchers.IO) {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                retriever.setDataSource(context, Uri.parse(uriStr))
            } else {
                retriever.setDataSource(uriStr, HashMap())
            }
            val bitmap = retriever.getFrameAtTime(500000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: retriever.frameAtTime
            if (bitmap != null) {
                val cacheFile = File(context.cacheDir, "vid_gen_thumb_${System.currentTimeMillis()}.jpg")
                val fos = FileOutputStream(cacheFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, fos)
                fos.flush()
                fos.close()
                bitmap.recycle()
                return@withContext cacheFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) { e.printStackTrace() }
        }
        return@withContext null
    }

    val videoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val type = context.contentResolver.getType(uri) ?: ""
            val extension = uri.path?.substringAfterLast('.', "")?.lowercase() ?: ""
            val isSupported = type.contains("mp4") || type.contains("quicktime") || type.contains("webm") ||
                    extension in listOf("mp4", "mov", "webm") || extension.isEmpty() // accept loose paths safely

            if (!isSupported) {
                videoFormatError = "Format not supported! Supported files: MP4, MOV, WEBM only."
                selectedVideoUri = null
                selectedVideoDuration = 0
                generatedThumbnailPath = null
                return@rememberLauncherForActivityResult
            }

            videoFormatError = null
            selectedVideoUri = uri.toString()

            // Analyze duration in coroutine
            coroutineScope.launch {
                var retriever: MediaMetadataRetriever? = null
                try {
                    retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val durationMsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val ms = durationMsStr?.toLongOrNull() ?: 12000L
                    selectedVideoDuration = (ms / 1000).toInt()
                } catch (e: java.lang.Exception) {
                    selectedVideoDuration = 15 // Mock sensible default
                } finally {
                    try { retriever?.release() } catch (ex: Exception) {}
                }
                trimStartSec = 0
                trimEndSec = minOf(60, selectedVideoDuration)

                // Generate real preview thumbnail!
                val path = generateThumbnailFromUri(uri.toString())
                generatedThumbnailPath = path ?: "https://images.unsplash.com/photo-1516280440614-37939bbacd6a?auto=format&fit=crop&q=80&w=200"
            }
        }
    }

    // Modal inline Caption Editor popup dialog
    var postToEditId by remember { mutableStateOf<String?>(null) }
    var postToEditNewCaption by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Main Screen Header Label
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(
                    text = "Viora Creator Hub",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = "Share photos or vertical video reels in high definitions",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Segment Switcher: [Photo Post | Video Post]
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(32.dp))
                    .background(Color(0xFF0F081B)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Photo", "Video").forEach { tab ->
                    val isSelected = postTypeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(32.dp))
                            .background(if (isSelected) PrimaryPinkPurple else Color.Transparent)
                            .clickable { postTypeTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (tab == "Photo") Icons.Default.AddPhotoAlternate else Icons.Default.MovieFilter,
                                contentDescription = "",
                                tint = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (tab == "Photo") "Photo Moment" else "Video Reel",
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Creator Canvas: Main Input Box
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .glassmorphic(borderRadius = 22.dp)
                    .padding(20.dp)
            ) {
                // 1. Write caption input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Caption & Inspiration",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    
                    if (postTypeTab == "Photo") {
                        IconButton(
                            onClick = { showColorPicker = !showColorPicker },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (showColorPicker) PrimaryPinkPurple.copy(alpha = 0.3f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ColorLens,
                                contentDescription = "Choose Background Color",
                                tint = if (selectedBgId != "none") PrimaryPinkPurple else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Color picker block (only for Photo Moment style posts)
                if (postTypeTab == "Photo") {
                    AnimatedVisibility(
                        visible = showColorPicker,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Text(
                                text = "Choose theme (applied if character limit <= $maxBgCharLimit):",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(postBgOptions) { option ->
                                    val isSelected = selectedBgId == option.id
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color.White else BorderColor,
                                                shape = CircleShape
                                            )
                                            .then(
                                                if (option.brush != null) {
                                                    Modifier.background(option.brush)
                                                } else {
                                                    Modifier.background(option.solidColor)
                                                }
                                            )
                                            .clickable {
                                                selectedBgId = option.id
                                                if (attachedImageUris.isNotEmpty()) {
                                                    Toast.makeText(context, "Colors apply to text posts only.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .testTag("color_pill_${option.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (option.id == "none") {
                                            Icon(Icons.Default.NotInterested, contentDescription = "", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(15.dp))
                                        } else if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = "", tint = Color.White, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Input card text frame
                val useBg = canUseBgColor && postTypeTab == "Photo"
                val textContainerModifier = if (useBg) {
                    if (currentBgOption.brush != null) Modifier.background(currentBgOption.brush) else Modifier.background(currentBgOption.solidColor)
                } else {
                    Modifier.background(Color(0xFF10071C))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, if (useBg) Color.White.copy(alpha = 0.4f) else BorderColor, RoundedCornerShape(14.dp))
                        .then(textContainerModifier)
                ) {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = {
                            Text(
                                text = if (postTypeTab == "Video") "Write a description for your dynamic Reel video post..." else "What's on your mind? Create a fresh story post...",
                                color = if (useBg) Color.White.copy(alpha = 0.7f) else TextSecondary,
                                fontSize = if (useBg) 15.sp else 13.sp,
                                textAlign = if (useBg) TextAlign.Center else TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.White,
                            fontSize = if (useBg) 16.sp else 13.5.sp,
                            fontWeight = if (useBg) FontWeight.Bold else FontWeight.Normal,
                            textAlign = if (useBg) TextAlign.Center else TextAlign.Start
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("create_post_caption_input")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Characters: ${caption.length}" + (if (selectedBgId != "none" && postTypeTab == "Photo") " / $maxBgCharLimit" else ""),
                        color = if (selectedBgId != "none" && caption.length > maxBgCharLimit && postTypeTab == "Photo") Color(0xFFFF2E93) else TextSecondary,
                        fontSize = 11.sp
                    )
                    
                    if (useBg) {
                        Text("✨ Color Theme Active", color = Color(0xFF00FF87), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 2. Tab Specific Attachment Workstations
                if (postTypeTab == "Photo") {
                    // PHOTO WORKSTATION
                    if (attachedImageUris.isNotEmpty()) {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Attached Photos (${attachedImageUris.size}/3):", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                TextButton(onClick = { attachedImageUris.clear() }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF2E93))) {
                                    Text("Clear", fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                attachedImageUris.forEachIndexed { idx, path ->
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current).data(path).crossfade(true).build(),
                                            contentDescription = "",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.7f))
                                                .clickable { attachedImageUris.removeAt(idx) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text("Attach photos to your post:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF160E21))
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (attachedImageUris.size >= 3) {
                                        Toast.makeText(context, "Already selected maximum of 3 photos.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        photoGalleryLauncher.launch("image/*")
                                    }
                                }
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = "", tint = PrimaryPinkPurple, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Gallery", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(BorderColor))

                        presetImagesList.forEach { url ->
                            val isAttached = attachedImageUris.contains(url)
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(width = if (isAttached) 2.dp else 0.5.dp, color = if (isAttached) PrimaryPinkPurple else BorderColor, shape = RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isAttached) {
                                            attachedImageUris.remove(url)
                                        } else {
                                            if (attachedImageUris.size >= 3) {
                                                Toast.makeText(context, "Maximum 3 photos can be attached.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                attachedImageUris.add(url)
                                            }
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                                    contentDescription = "",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isAttached) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // VIDEO WORKSTATION
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Source Video File (MP4, MOV, WEBM):", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Selected video detail card
                        if (selectedVideoUri != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF140A20))
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Real or mock video thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (generatedThumbnailPath != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current).data(generatedThumbnailPath).crossfade(true).build(),
                                            contentDescription = "Thumb preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.Movie, contentDescription = "", tint = PrimaryPinkPurple, modifier = Modifier.size(24.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Video Source Selected", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = if (selectedVideoUri!!.contains("mixkit") || selectedVideoUri!!.contains("gtv")) {
                                            "Source: Preset Draft Video"
                                        } else {
                                            "Source: ${selectedVideoUri!!.takeLast(24)}"
                                        },
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text("Duration: ${selectedVideoDuration}s", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Color.White, containerColor = Color(0x3F9C27B0))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(SuccessGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Valid Format", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        selectedVideoUri = null
                                        selectedVideoDuration = 0
                                        generatedThumbnailPath = null
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color(0xFFFF2E93), modifier = Modifier.size(16.dp))
                                }
                            }

                            // VIDEO TRIMMING CONTROLLER (Max 60 Seconds rule compliance)
                            Spacer(modifier = Modifier.height(14.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF13091D))
                                    .border(1.dp, BorderColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.ContentCut, contentDescription = "", tint = PrimaryPinkPurple, modifier = Modifier.size(16.dp))
                                        Text("Precision Video Trimmer (Max 60s)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Text(
                                        text = "${trimEndSec - trimStartSec}s selected",
                                        color = if ((trimEndSec - trimStartSec) > 60) Color(0xFFFF2E93) else Color(0xFF00FF87),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = "Videos exceeding 60s will be block-rejected unless trimmed down to a 60-second window before publishing.",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("SPLIT START", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { if (trimStartSec > 0) trimStartSec-- },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "", tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                            Text("${trimStartSec}s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
                                            IconButton(
                                                onClick = { if (trimStartSec < trimEndSec) trimStartSec++ },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.AddCircleOutline, contentDescription = "", tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }

                                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderColor))

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("SPLIT END", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { if (trimEndSec > trimStartSec) trimEndSec-- },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "", tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                            Text("${trimEndSec}s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
                                            IconButton(
                                                onClick = { if (trimEndSec < selectedVideoDuration) trimEndSec++ },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.AddCircleOutline, contentDescription = "", tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }

                                if ((trimEndSec - trimStartSec) > 60) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF3E0A1A))
                                            .border(0.5.dp, Color(0xFFFF2E93), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "❌ Rejected: Trimmed length exceeds 60s. Trim end time down to support publishing.",
                                            color = Color(0xFFFF489A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ContentCut, contentDescription = "", tint = Color(0xFF00FF87), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Trim window fits criteria (<= 60s)", color = Color(0xFF00FF87), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            // Selector segment
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(115.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF160B21))
                                        .border(2.dp, Brush.linearGradient(listOf(PrimaryPinkPurple, BrightNeonPurple)), RoundedCornerShape(12.dp))
                                        .clickable {
                                            videoGalleryLauncher.launch("video/*")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(Icons.Outlined.Movie, contentDescription = "", tint = PrimaryPinkPurple, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Upload Video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("MP4, MOV, WEBM", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }

                                // Preset videos container to test video uploads easily
                                Column(
                                    modifier = Modifier
                                        .weight(1.7f)
                                        .height(115.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F0816))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Text("Test with Quick Presets:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(presetVideosList) { pair ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x1F9C27B0))
                                                    .border(0.5.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        selectedVideoUri = pair.second
                                                        selectedVideoDuration = 24
                                                        trimStartSec = 0
                                                        trimEndSec = 24
                                                        coroutineScope.launch {
                                                            val path = generateThumbnailFromUri(pair.second)
                                                            generatedThumbnailPath = path ?: "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&q=80&w=200"
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(pair.first, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    Icon(Icons.Default.PlayCircleOutline, contentDescription = "", tint = PrimaryPinkPurple, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (videoFormatError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(videoFormatError!!, color = Color(0xFFFF2E93), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Post general settings configuration
                Text(text = "Permissions & Audience", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF160E21))
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Privacy settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = PrimaryPinkPurple, modifier = Modifier.size(16.dp))
                            Text("Audience limit", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("Everyone", "Followers", "Private").forEach { option ->
                                val selected = privacySetting == option
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) PrimaryPinkPurple else Color.White.copy(alpha = 0.05f))
                                        .clickable { privacySetting = option }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(option, color = if (selected) Color.White else TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))

                    // Comment permissions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, tint = PrimaryPinkPurple, modifier = Modifier.size(16.dp))
                            Text("Commenting style", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("Everyone", "Followers", "No One").forEach { option ->
                                val selected = commentSetting == option
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) PrimaryPinkPurple else Color.White.copy(alpha = 0.05f))
                                        .clickable { commentSetting = option }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(option, color = if (selected) Color.White else TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Upload Progress Simulator Indicator (Active state)
                if (isUploadingVideo) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Uploading video post...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("$uploadProgressPercentage%", color = PrimaryPinkPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgressPercentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = PrimaryPinkPurple,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                } else if (isCompressingPhotos) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = PrimaryPinkPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uploading...", color = Color.White, fontSize = 12.sp)
                    }
                } else {
                    val canRelease = if (postTypeTab == "Photo") {
                        caption.trim().isNotEmpty() || attachedImageUris.isNotEmpty()
                    } else {
                        selectedVideoUri != null && (trimEndSec - trimStartSec) <= 60
                    }

                    VioraGradientButton(
                        text = if (postTypeTab == "Photo") "Release Photo Moment" else "Release Video Post",
                        onClick = {
                            if (!canRelease) {
                                if (postTypeTab == "Video" && selectedVideoUri != null && (trimEndSec - trimStartSec) > 60) {
                                    Toast.makeText(context, "Rejecting video: Exceeds 60 seconds limit.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please select media assets or compose a text status.", Toast.LENGTH_SHORT).show()
                                }
                                return@VioraGradientButton
                            }

                            if (postTypeTab == "Photo") {
                                isCompressingPhotos = true
                                coroutineScope.launch {
                                    val compressedList = mutableListOf<String>()
                                    attachedImageUris.forEachIndexed { index, path ->
                                        if (path.startsWith("content://") || path.startsWith("file://")) {
                                            val compressedPath = viewModel.compressAndSaveImage(context, Uri.parse(path), index)
                                            compressedList.add(compressedPath)
                                        } else {
                                            compressedList.add(path)
                                        }
                                    }

                                    viewModel.createPost(
                                        caption = caption,
                                        imageUrls = compressedList,
                                        backgroundColor = if (canUseBgColor) selectedBgId else null,
                                        privacy = privacySetting,
                                        commentPermission = commentSetting
                                    )

                                    isCompressingPhotos = false
                                    Toast.makeText(context, "Moments published successfully!", Toast.LENGTH_SHORT).show()

                                    // Reset Photo Canvas state
                                    caption = ""
                                    attachedImageUris.clear()
                                    selectedBgId = "none"
                                }
                            } else {
                                // Dynamic Video compression & auto generation progress indicator block
                                isUploadingVideo = true
                                uploadProgressPercentage = 0
                                simulatedCompressedSizeMb = 2.0 + (java.lang.Math.random() * 2.8)
                                coroutineScope.launch {
                                    uploadProgressStatusMsg = "Block Checking file format limits..."
                                    delay(800)
                                    uploadProgressPercentage = 22
                                    uploadProgressStatusMsg = "Generating thumbnail from frames..."
                                    delay(900)
                                    uploadProgressPercentage = 48
                                    uploadProgressStatusMsg = "Downscaling resolution to 480p..."
                                    delay(1000)
                                    uploadProgressPercentage = 75
                                    uploadProgressStatusMsg = "Compressing stream down to targeted MBs..."
                                    delay(900)
                                    uploadProgressPercentage = 95
                                    uploadProgressStatusMsg = "Registering video metadata parameters..."
                                    delay(600)
                                    uploadProgressPercentage = 100
                                    
                                    val finalThumb = generatedThumbnailPath ?: "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&q=80&w=200"
                                    viewModel.createPost(
                                        caption = caption,
                                        imageUrls = emptyList(),
                                        backgroundColor = null,
                                        privacy = privacySetting,
                                        commentPermission = commentSetting,
                                        videoUrl = selectedVideoUri,
                                        thumbnailUrl = finalThumb,
                                        videoDuration = (trimEndSec - trimStartSec)
                                    )

                                    isUploadingVideo = false
                                    Toast.makeText(context, "Video Reel published successfully!", Toast.LENGTH_SHORT).show()

                                    // Reset dynamic state
                                    caption = ""
                                    selectedVideoUri = null
                                    selectedVideoDuration = 0
                                    generatedThumbnailPath = null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_create_post_submit"
                    )
                }
            }
        }

        // Shared posts management list (Your creation files)
        if (ownPosts.isNotEmpty()) {
            item {
                Text(
                    text = "Your Published Feed moments",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 10.dp)
                )
            }

            items(ownPosts, key = { it.id }) { post ->
                val matchingBg = postBgOptions.find { it.id == post.backgroundColor }
                val isShortWithBg = post.backgroundColor != null && post.backgroundColor != "none" && post.caption.length <= maxBgCharLimit && post.imageUrls.isEmpty() && post.videoUrl == null

                val postBgModifier = if (isShortWithBg && matchingBg != null) {
                    if (matchingBg.brush != null) Modifier.background(matchingBg.brush) else Modifier.background(matchingBg.solidColor)
                } else {
                    Modifier.background(Color(0xFF140B22))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .then(postBgModifier)
                        .border(0.5.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setViewingFullscreenPost(post) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail display (Photo list vs. Custom Video thumbnail)
                        if (post.videoUrl != null) {
                            Box(modifier = Modifier.size(54.dp)) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(post.thumbnailUrl).crossfade(true).build(),
                                    contentDescription = "",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(2.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "", tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        } else if (post.imageUrls.isNotEmpty()) {
                            Box(modifier = Modifier.size(54.dp)) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(post.imageUrls.first()).crossfade(true).build(),
                                    contentDescription = "",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                )
                                if (post.imageUrls.size > 1) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+${post.imageUrls.size - 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            var isExpanded by remember { mutableStateOf(false) }
                            val limit = 65
                            val isTooLong = post.caption.length > limit

                            Text(
                                text = if (isTooLong && !isExpanded) "${post.caption.take(limit)}..." else post.caption,
                                color = Color.White,
                                fontSize = if (isShortWithBg) 14.sp else 12.5.sp,
                                fontWeight = if (isShortWithBg) FontWeight.Bold else FontWeight.SemiBold
                            )

                            if (post.videoUrl != null) {
                                Text(
                                    text = "🎥 Duration: ${post.videoDuration}s • Views: ${post.viewsCount}",
                                    color = Color(0xFF00FF87),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            } else if (isShortWithBg) {
                                Text(
                                    text = "🎨 Theme: ${matchingBg?.name ?: "Solid color"}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            if (isTooLong) {
                                Text(
                                    text = if (isExpanded) "Read Less" else "Read More",
                                    color = PrimaryPinkPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { isExpanded = !isExpanded }
                                        .padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Likes: ${post.likesCount} • Comments: ${post.commentsCount}",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Caption editing & Deletion management triggers
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Caption editing button
                        IconButton(
                            onClick = {
                                postToEditId = post.id
                                postToEditNewCaption = post.caption
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Caption", tint = Color.White, modifier = Modifier.size(15.dp))
                        }

                        // Deletion button
                        IconButton(
                            onClick = { viewModel.deleteOwnPost(post.id) },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Post", tint = Color(0xFFFF2E93), modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }

    // Modal Caption Editor dialog (Allow users to edit captions after posting)
    if (postToEditId != null) {
        val editingPost = posts.find { it.id == postToEditId }
        Dialog(
            onDismissRequest = { postToEditId = null }
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Edit Post Caption",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = postToEditNewCaption,
                        onValueChange = { postToEditNewCaption = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPinkPurple,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { postToEditId = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (editingPost != null) {
                                    viewModel.updatePostDetails(
                                        postId = editingPost.id,
                                        caption = postToEditNewCaption,
                                        privacy = editingPost.privacy,
                                        commentPermission = editingPost.commentPermission
                                    )
                                    Toast.makeText(context, "Caption updated successfully!", Toast.LENGTH_SHORT).show()
                                    postToEditId = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPinkPurple),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
