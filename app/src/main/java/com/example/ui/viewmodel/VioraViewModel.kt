package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.DatingRepository
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VioraViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = DatingRepository(application, database.datingDao())

    // -------------------------------------------------------------
    // Reactive Streams from Repository
    // -------------------------------------------------------------
    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val discoverableProfiles: StateFlow<List<DatingProfileEntity>> = repository.discoverableProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matchedProfiles: StateFlow<List<DatingProfileEntity>> = repository.matchedProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val posts: StateFlow<List<PostEntity>> = repository.postsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProfiles: StateFlow<List<DatingProfileEntity>> = repository.allProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<MessageEntity>> = repository.allMessagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<DatingRepository.VioraNotification>> = repository.notificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val sharedPrefs = application.getSharedPreferences("viora_prefs", Context.MODE_PRIVATE)

    // -------------------------------------------------------------
    // Connectivity & Automated Offline Sync States
    // -------------------------------------------------------------
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline

    private val _isSlowConnection = MutableStateFlow(sharedPrefs.getBoolean("simulated_slow", false))
    val isSlowConnection: StateFlow<Boolean> = _isSlowConnection

    private val _isNetworkFetchingPosts = MutableStateFlow(false)
    val isNetworkFetchingPosts: StateFlow<Boolean> = _isNetworkFetchingPosts

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    val pendingQueue: StateFlow<List<OfflineSyncQueueEntity>> = repository.pendingQueueFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun performActualNetworkCheck(): Boolean = withContext(Dispatchers.IO) {
        if (sharedPrefs.getBoolean("simulated_offline", false)) {
            return@withContext false
        }
        try {
            val timeoutMs = 1500
            val socket = java.net.Socket()
            val socketAddress = java.net.InetSocketAddress("8.8.8.8", 53)
            socket.connect(socketAddress, timeoutMs)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun toggleSimulatedOffline() {
        val current = sharedPrefs.getBoolean("simulated_offline", false)
        sharedPrefs.edit().putBoolean("simulated_offline", !current).apply()
        viewModelScope.launch {
            _isOnline.value = performActualNetworkCheck()
        }
    }

    fun toggleSimulatedSlow() {
        val current = _isSlowConnection.value
        sharedPrefs.edit().putBoolean("simulated_slow", !current).apply()
        _isSlowConnection.value = !current
    }

    fun triggerManualPostsFetch() {
        viewModelScope.launch {
            _isNetworkFetchingPosts.value = true
            val delayDuration = if (_isSlowConnection.value) 4000L else 1500L
            kotlinx.coroutines.delay(delayDuration)
            _isNetworkFetchingPosts.value = false
        }
    }

    fun preloadNextTenPosts() {
        viewModelScope.launch {
            if (_isNetworkFetchingPosts.value) return@launch
            
            _isNetworkFetchingPosts.value = true
            val delayDuration = if (_isSlowConnection.value) 3000L else 1000L
            kotlinx.coroutines.delay(delayDuration)
            
            val currentPosts = posts.value
            val currentSize = currentPosts.size
            if (currentSize >= 40) {
                // Limit to 40 posts to protect performance/DB bloat
                _isNetworkFetchingPosts.value = false
                return@launch
            }
            
            val additionalPosts = mutableListOf<PostEntity>()
            val startId = currentSize + 1
            
            val captions = listOf(
                "Sunrise reflections over the lake 🌅 Peace is not a place, it's a state of mind.",
                "Fresh sourdough crust detail! 🍞 Baking is 10% science, 90% waiting and praying.",
                "Wandering the old concrete pathways in Neo-Tokyo. Spontaneous travel hits different. 🗺️🎒",
                "New workspace setup complete. Minimalist desk, organic wood texture, glowing mechanical board. 💻🎨",
                "Weekend playlist curation in progress. Dropping deep organic house and jazz grooves! 🎷🎵",
                "Midnight research into cosmic architecture and gravity lattices. 🌌🔭 Learn something new every night.",
                "Tasting artisanal single-origin estate coffee today. Notes of peach and cherry! ☕🍒",
                "Biking along the coastal highway at 6 AM. Cold morning breeze keeps the mind crisp. 🚴‍♂️🌊",
                "Sketched out a minimal mobile app wireframe today with extreme whitespace ✏️📏 Layout balance is key.",
                "Strolling through the botanical greenhouse. Tropical plants reaching for the glass ceiling. 🌿🌵"
            )
            
            val authors = listOf(
                "Sophia Martinez" to "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200",
                "James Carter" to "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=200",
                "Isabella Wilson" to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=200",
                "Olivia Anderson" to "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=200",
                "Sharon" to "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=200"
            )
            
            val images = listOf(
                "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&q=80&w=600",
                "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&q=80&w=600",
                "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&q=80&w=600",
                "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?auto=format&fit=crop&q=80&w=600",
                "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=600",
                "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&q=80&w=600",
                "https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&q=80&w=600",
                "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&q=80&w=600",
                "https://images.unsplash.com/photo-1512486130939-2c4f79935e4f?auto=format&fit=crop&q=80&w=600",
                "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?auto=format&fit=crop&q=80&w=600"
            )
            
            for (i in 0 until 10) {
                val idx = (startId + i)
                val author = authors[idx % authors.size]
                val img = images[idx % images.size]
                val cap = captions[idx % captions.size]
                
                additionalPosts.add(
                    PostEntity(
                        id = "post_preloaded_$idx",
                        authorId = "profile_${author.first.lowercase().replace(" ", "_")}",
                        authorName = author.first,
                        authorAvatar = author.second,
                        authorLabel = "Los Angeles • Preloaded ${idx - 3}m ago",
                        caption = cap,
                        imageUrls = listOf(img),
                        likesCount = (40..300).random(),
                        commentsCount = (5..45).random(),
                        isLiked = false,
                        isSaved = false,
                        timestamp = System.currentTimeMillis() - idx * 1000L * 60 * 15
                    )
                )
            }
            
            repository.datingDao.insertPosts(additionalPosts)
            _isNetworkFetchingPosts.value = false
        }
    }

    private suspend fun processSyncQueue() {
        if (_isSyncing.value) return
        val list = repository.getPendingQueueDirect()
        if (list.isEmpty()) return

        _isSyncing.value = true
        try {
            for (item in list) {
                when (item.actionType) {
                    "CREATE_POST" -> {
                        val csvImages = if (item.payloadString2.isEmpty()) emptyList() else item.payloadString2.split(",")
                        repository.createNewPost(
                            caption = item.payloadString1,
                            imageUrls = csvImages,
                            backgroundColor = if (item.payloadString3.isEmpty() || item.payloadString3 == "none") null else item.payloadString3,
                            privacy = item.payloadString4,
                            commentPermission = item.payloadString4
                        )
                        repository.deletePost(item.itemId)
                    }
                    "TOGGLE_POST_LIKE" -> {
                        repository.togglePostLike(item.itemId, item.payloadString1.toBoolean())
                    }
                    "ADD_COMMENT" -> {
                        repository.addComment(item.itemId, item.payloadString1, if (item.payloadString3.isEmpty()) null else item.payloadString3)
                    }
                    "SEND_MESSAGE" -> {
                        repository.sendChatMessage(
                            userId = item.itemId,
                            content = item.payloadString1,
                            voiceDuration = item.payloadLong.toInt(),
                            imageUrl = if (item.payloadString2.isEmpty()) null else item.payloadString2,
                            replyToId = if (item.payloadString3.isEmpty()) null else item.payloadString3,
                            replyToText = item.payloadString3,
                            replyToSender = item.payloadString3
                        )
                        repository.deleteMessage(item.itemId)
                    }
                    "TOGGLE_COMMENT_LIKE" -> {
                        repository.toggleCommentLike(item.itemId, item.payloadString1.toBoolean(), item.payloadLong.toInt())
                    }
                }
                repository.dequeueItemById(item.queueId)
            }
            repository.insertNotification(
                title = "Offline Actions Synced! ✨",
                content = "You are back online! Your posts, comments, likes, and messages have synced flawlessly.",
                type = "sync"
            )
        } catch (e: Exception) {
            // Silently swallow errors
        } finally {
            _isSyncing.value = false
        }
    }

    init {
        viewModelScope.launch {
            while (true) {
                val hasInternet = performActualNetworkCheck()
                _isOnline.value = hasInternet
                kotlinx.coroutines.delay(3000)
            }
        }
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(4000)
                if (_isOnline.value && !_isSyncing.value) {
                    processSyncQueue()
                }
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            if (performActualNetworkCheck()) {
                triggerManualPostsFetch()
            }
        }
    }

    // -------------------------------------------------------------
    // UI Transient State
    // -------------------------------------------------------------
    private val _currentScreen = MutableStateFlow(
        if (sharedPrefs.getBoolean("is_logged_in", false)) "MAIN" else "WELCOME"
    ) // WELCOME, HOW_IT_WORKS, MESSAGING_GUIDE, LOGIN, SIGN_UP, FORGOT_PASSWORD, MAIN
    val currentScreen: StateFlow<String> = _currentScreen

    private val _authEmail = MutableStateFlow("")
    val authEmail: StateFlow<String> = _authEmail

    private val _authPassword = MutableStateFlow("")
    val authPassword: StateFlow<String> = _authPassword

    private val _authConfirmPassword = MutableStateFlow("")
    val authConfirmPassword: StateFlow<String> = _authConfirmPassword

    private val _termsAccepted = MutableStateFlow(false)
    val termsAccepted: StateFlow<Boolean> = _termsAccepted

    private val _rememberMe = MutableStateFlow(true)
    val rememberMe: StateFlow<Boolean> = _rememberMe

    // Verification Code
    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode

    // Verification Flow Modal
    private val _showOtpDialog = MutableStateFlow(false)
    val showOtpDialog: StateFlow<Boolean> = _showOtpDialog

    // App Navigation Bar
    private val _currentTab = MutableStateFlow(0) // 0: Home, 1: Explore, 2: Chats, 3: Posts, 4: Profile
    val currentTab: StateFlow<Int> = _currentTab

    // Chat Details
    private val _activeChatProfile = MutableStateFlow<DatingProfileEntity?>(null)
    val activeChatProfile: StateFlow<DatingProfileEntity?> = _activeChatProfile

    // Comment Details on Specific Post
    private val _commentingPost = MutableStateFlow<PostEntity?>(null)
    val commentingPost: StateFlow<PostEntity?> = _commentingPost

    // Fullscreen Post Detail State
    private val _viewingFullscreenPost = MutableStateFlow<PostEntity?>(null)
    val viewingFullscreenPost: StateFlow<PostEntity?> = _viewingFullscreenPost

    fun setViewingFullscreenPost(post: PostEntity?) {
        _viewingFullscreenPost.value = post
    }

    // Swipe Celebration Overlay
    private val _matchedCelebrationProfile = MutableStateFlow<DatingProfileEntity?>(null)
    val matchedCelebrationProfile: StateFlow<DatingProfileEntity?> = _matchedCelebrationProfile

    // Filters & Discover Settings
    private val _searchFilter = MutableStateFlow("")
    val searchFilter: StateFlow<String> = _searchFilter

    private val _selectedInterests = MutableStateFlow<List<String>>(emptyList())
    val selectedInterests: StateFlow<List<String>> = _selectedInterests

    private val _ageRange = MutableStateFlow(18f..40f)
    val ageRange: StateFlow<ClosedFloatingPointRange<Float>> = _ageRange

    private val _distanceFilter = MutableStateFlow(30f)
    val distanceFilter: StateFlow<Float> = _distanceFilter

    // Chat Settings State
    private val _whoCanChatMe = MutableStateFlow("Everyone") // Everyone, Followers Only, Verified Profiles Only
    val whoCanChatMe: StateFlow<String> = _whoCanChatMe

    private val _chatWallpaperUrl = MutableStateFlow<String?>(null)
    val chatWallpaperUrl: StateFlow<String?> = _chatWallpaperUrl

    private val _chatTheme = MutableStateFlow("Purple") // Purple, Rose, Teal, Gold, Charcoal
    val chatTheme: StateFlow<String> = _chatTheme

    fun setWhoCanChatMe(option: String) {
        _whoCanChatMe.value = option
    }

    fun setChatWallpaperUrl(url: String?) {
        _chatWallpaperUrl.value = url
    }

    fun setChatTheme(theme: String) {
        _chatTheme.value = theme
    }

    // App-wide Data Privacy Settings
    private val _incognitoMode = MutableStateFlow(false)
    val incognitoMode: StateFlow<Boolean> = _incognitoMode

    private val _hideOnlinePresence = MutableStateFlow(false)
    val hideOnlinePresence: StateFlow<Boolean> = _hideOnlinePresence

    private val _obfuscateProximity = MutableStateFlow(false)
    val obfuscateProximity: StateFlow<Boolean> = _obfuscateProximity

    private val _hideMyAge = MutableStateFlow(false)
    val hideMyAge: StateFlow<Boolean> = _hideMyAge

    private val _allowSearchIndexing = MutableStateFlow(true)
    val allowSearchIndexing: StateFlow<Boolean> = _allowSearchIndexing

    fun toggleIncognitoMode(enabled: Boolean) {
        _incognitoMode.value = enabled
    }

    fun toggleHideOnlinePresence(enabled: Boolean) {
        _hideOnlinePresence.value = enabled
    }

    fun toggleObfuscateProximity(enabled: Boolean) {
        _obfuscateProximity.value = enabled
    }

    fun toggleHideMyAge(enabled: Boolean) {
        _hideMyAge.value = enabled
    }

    fun toggleAllowSearchIndexing(enabled: Boolean) {
        _allowSearchIndexing.value = enabled
    }

    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun setAuthEmail(email: String) {
        _authEmail.value = email
    }

    fun setAuthPassword(password: String) {
        _authPassword.value = password
    }

    fun setAuthConfirmPassword(password: String) {
        _authConfirmPassword.value = password
    }

    fun setTermsAccepted(accepted: Boolean) {
        _termsAccepted.value = accepted
    }

    fun setRememberMe(remember: Boolean) {
        _rememberMe.value = remember
    }

    fun setOtpCode(code: String) {
        _otpCode.value = code
    }

    fun setShowOtpDialog(show: Boolean) {
        _showOtpDialog.value = show
    }

    fun setTab(tab: Int) {
        _currentTab.value = tab
    }

    fun setActiveChat(profile: DatingProfileEntity?) {
        _activeChatProfile.value = profile
        if (profile != null) {
            viewModelScope.launch {
                repository.markChatAsRead(profile.id)
            }
        }
    }

    fun setCommentingPost(post: PostEntity?) {
        _commentingPost.value = post
    }

    fun dismissCelebration() {
        _matchedCelebrationProfile.value = null
    }

    fun setSearchFilter(query: String) {
        _searchFilter.value = query
    }

    fun toggleInterestFilter(interest: String) {
        val current = _selectedInterests.value.toMutableList()
        if (current.contains(interest)) {
            current.remove(interest)
        } else {
            current.add(interest)
        }
        _selectedInterests.value = current
    }

    fun setAgeRange(range: ClosedFloatingPointRange<Float>) {
        _ageRange.value = range
    }

    fun setDistanceFilter(distance: Float) {
        _distanceFilter.value = distance
    }

    // -------------------------------------------------------------
    // Core Actions called from UI Screens
    // -------------------------------------------------------------
    fun handleLogin() {
        if (_authEmail.value.isNotEmpty() && _authPassword.value.isNotEmpty()) {
            _currentScreen.value = "MAIN"
            sharedPrefs.edit().putBoolean("is_logged_in", true).apply()
            repository.insertNotification("Welcome Back!", "Good evening! Find your perfect match. 💜", "verification")
        }
    }

    fun handleSignUp() {
        if (_authEmail.value.isNotEmpty() && _authPassword.value.isNotEmpty() && _authPassword.value == _authConfirmPassword.value) {
            _showOtpDialog.value = true
        }
    }

    fun verifyOtp() {
        if (_otpCode.value.length >= 4) {
            _showOtpDialog.value = false
            _currentScreen.value = "MAIN"
            sharedPrefs.edit().putBoolean("is_logged_in", true).apply()
            repository.insertNotification("Sign Up Successful!", "Welcome to Viora! We review accounts manually to keep our community safe and verified. ✨", "verification")
        }
    }

    fun handleForgotPassword() {
        if (_authEmail.value.isNotEmpty()) {
            _showOtpDialog.value = true
        }
    }

    fun swipeLike(profile: DatingProfileEntity) {
        viewModelScope.launch {
            val updated = repository.updateSwipe(profile.id, liked = true, disliked = false)
            if (updated?.isMatched == true) {
                // Trigger match celebration dialog overlay
                _matchedCelebrationProfile.value = updated
            }
        }
    }

    fun swipePass(profile: DatingProfileEntity) {
        viewModelScope.launch {
            repository.updateSwipe(profile.id, liked = false, disliked = true)
        }
    }

    fun toggleFollowUser(userId: String, isFollowing: Boolean) {
        viewModelScope.launch {
            repository.setFollowing(userId, !isFollowing)
        }
    }

    fun blockProfile(userId: String) {
        viewModelScope.launch {
            repository.updateSwipe(userId, liked = false, disliked = true)
            repository.insertNotification("Profile Blocked", "You have blocked this profile. they will no longer appear in your suggestions.", "alert")
            if (_activeChatProfile.value?.id == userId) {
                _activeChatProfile.value = null
            }
        }
    }

    fun reportProfile(userId: String, reason: String) {
        viewModelScope.launch {
            repository.updateSwipe(userId, liked = false, disliked = true)
            repository.insertNotification("Safety Report Received", "We have received your report for '$reason'. This profile has been blocked and our safety team is reviewing it.", "alert")
            if (_activeChatProfile.value?.id == userId) {
                _activeChatProfile.value = null
            }
        }
    }

    fun clearChatMessages(userId: String) {
        viewModelScope.launch {
            repository.clearChatMessages(userId)
        }
    }

    fun sendChatMessage(
        text: String,
        voiceDuration: Int = 0,
        imageUrl: String? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSender: String? = null
    ) {
        val active = _activeChatProfile.value ?: return
        viewModelScope.launch {
            if (_isOnline.value) {
                repository.sendChatMessage(
                    userId = active.id,
                    content = text,
                    voiceDuration = voiceDuration,
                    imageUrl = imageUrl,
                    replyToId = replyToId,
                    replyToText = replyToText,
                    replyToSender = replyToSender
                )
            } else {
                val tempId = "temp_msg_${System.currentTimeMillis()}"
                val optimisticMsg = MessageEntity(
                    id = tempId,
                    chatUserId = active.id,
                    senderId = "me",
                    textContent = text,
                    mediaUrl = imageUrl,
                    audioDurationSec = voiceDuration,
                    type = if (voiceDuration > 0) "voice" else if (imageUrl != null) "image" else "text",
                    timestamp = System.currentTimeMillis(),
                    status = "sending",
                    replyToId = replyToId,
                    replyToText = replyToText,
                    replyToSender = replyToSender
                )
                repository.datingDao.insertMessage(optimisticMsg)

                repository.enqueueItem(
                    OfflineSyncQueueEntity(
                        actionType = "SEND_MESSAGE",
                        itemId = active.id,
                        payloadString1 = text,
                        payloadString2 = imageUrl ?: "",
                        payloadString3 = replyToId ?: "",
                        payloadLong = voiceDuration.toLong(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun updateMessage(message: MessageEntity) {
        viewModelScope.launch {
            repository.updateMessage(message)
        }
    }

    fun forwardMessage(message: MessageEntity, targetUserId: String) {
        viewModelScope.launch {
            if (_isOnline.value) {
                repository.sendChatMessage(
                    userId = targetUserId,
                    content = message.textContent,
                    voiceDuration = message.audioDurationSec,
                    imageUrl = message.mediaUrl
                )
            } else {
                val tempId = "temp_msg_${System.currentTimeMillis()}"
                val optimisticMsg = MessageEntity(
                    id = tempId,
                    chatUserId = targetUserId,
                    senderId = "me",
                    textContent = message.textContent,
                    mediaUrl = message.mediaUrl,
                    audioDurationSec = message.audioDurationSec,
                    type = if (message.audioDurationSec > 0) "voice" else if (message.mediaUrl != null) "image" else "text",
                    timestamp = System.currentTimeMillis(),
                    status = "sending"
                )
                repository.datingDao.insertMessage(optimisticMsg)

                repository.enqueueItem(
                    OfflineSyncQueueEntity(
                        actionType = "SEND_MESSAGE",
                        itemId = targetUserId,
                        payloadString1 = message.textContent,
                        payloadString2 = message.mediaUrl ?: "",
                        payloadLong = message.audioDurationSec.toLong(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun togglePostLike(post: PostEntity) {
        viewModelScope.launch {
            if (_isOnline.value) {
                repository.togglePostLike(post.id, post.isLiked)
            } else {
                val diff = if (post.isLiked) -1 else 1
                repository.datingDao.updatePostLike(post.id, !post.isLiked, diff)

                repository.enqueueItem(
                    OfflineSyncQueueEntity(
                        actionType = "TOGGLE_POST_LIKE",
                        itemId = post.id,
                        payloadString1 = (!post.isLiked).toString(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun createComment(postId: String, text: String, parentId: String? = null) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            if (_isOnline.value) {
                repository.addComment(postId, text, parentId)
            } else {
                val tempCommentId = "temp_cmt_${System.currentTimeMillis()}"
                val user = repository.datingDao.getUserProfileDirect() ?: UserProfileEntity()
                repository.datingDao.insertComment(
                    CommentEntity(
                        id = tempCommentId,
                        postId = postId,
                        authorName = user.name,
                        authorAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200",
                        text = text,
                        timestamp = System.currentTimeMillis(),
                        parentId = parentId,
                        likesCount = 0,
                        isLikedByMe = false
                    )
                )

                repository.enqueueItem(
                    OfflineSyncQueueEntity(
                        actionType = "ADD_COMMENT",
                        itemId = postId,
                        payloadString1 = text,
                        payloadString3 = parentId ?: "",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun toggleCommentLike(commentId: String, isLiked: Boolean, currentLikes: Int) {
        viewModelScope.launch {
            if (_isOnline.value) {
                repository.toggleCommentLike(commentId, isLiked, currentLikes)
            } else {
                val newIsLiked = !isLiked
                val newLikesCount = if (newIsLiked) currentLikes + 1 else maxOf(0, currentLikes - 1)
                repository.datingDao.updateCommentLike(commentId, newIsLiked, newLikesCount)

                repository.enqueueItem(
                    OfflineSyncQueueEntity(
                        actionType = "TOGGLE_COMMENT_LIKE",
                        itemId = commentId,
                        payloadString1 = newIsLiked.toString(),
                        payloadLong = currentLikes.toLong(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun compressAndSaveImage(context: Context, uri: Uri, index: Int): String = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext uri.toString()
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var inSampleSize = 1
            val reqWidth = 800
            val reqHeight = 800
            val height = options.outHeight
            val width = options.outWidth
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val secondInputStream = contentResolver.openInputStream(uri) ?: return@withContext uri.toString()
            val bitmap = BitmapFactory.decodeStream(secondInputStream, null, decodeOptions)
            secondInputStream.close()

            if (bitmap == null) return@withContext uri.toString()

            val cacheFile = File(context.cacheDir, "compressed_post_pic_${System.currentTimeMillis()}_$index.jpg")
            val fos = FileOutputStream(cacheFile)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 45, fos)
            fos.flush()
            fos.close()

            bitmap.recycle()

            cacheFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            uri.toString()
        }
    }

    val isMuted = kotlinx.coroutines.flow.MutableStateFlow(false)

    fun toggleMute() {
        isMuted.value = !isMuted.value
    }

    fun incrementViews(postId: String) {
        viewModelScope.launch {
            repository.incrementPostViews(postId)
        }
    }

    fun toggleBookmark(post: PostEntity) {
        viewModelScope.launch {
            val updatedBookmark = !post.isBookmarked
            repository.updatePostBookmark(post.id, updatedBookmark)
            _viewingFullscreenPost.value?.let { currentViewing ->
                if (currentViewing.id == post.id) {
                    _viewingFullscreenPost.value = currentViewing.copy(isBookmarked = updatedBookmark)
                }
            }
        }
    }

    fun reportPost(postId: String) {
        viewModelScope.launch {
            repository.reportPost(postId)
            repository.insertNotification("Post Highlighted for Safety", "Thank you! This post has been reported and our safety team is reviewing it.", "alert")
            _viewingFullscreenPost.value?.let { currentViewing ->
                if (currentViewing.id == postId) {
                    _viewingFullscreenPost.value = null
                }
            }
        }
    }

    fun createPost(
        caption: String,
        imageUrls: List<String>,
        backgroundColor: String? = null,
        privacy: String = "Everyone",
        commentPermission: String = "Everyone",
        videoUrl: String? = null,
        thumbnailUrl: String? = null,
        videoDuration: Int = 0
    ) {
        if (caption.trim().isEmpty() && imageUrls.isEmpty() && videoUrl == null) return
        viewModelScope.launch {
            if (_isOnline.value) {
                repository.createNewPost(caption, imageUrls, backgroundColor, privacy, commentPermission, videoUrl, thumbnailUrl, videoDuration)
            } else {
                val tempPostId = "temp_post_${System.currentTimeMillis()}"
                val user = repository.datingDao.getUserProfileDirect() ?: UserProfileEntity()
                repository.datingDao.insertPost(
                    PostEntity(
                        id = tempPostId,
                        authorId = "me",
                        authorName = user.name,
                        authorAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200",
                        authorLabel = "Saving offline...",
                        caption = caption,
                        imageUrls = imageUrls,
                        likesCount = 0,
                        commentsCount = 0,
                        isLiked = false,
                        isSaved = false,
                        timestamp = System.currentTimeMillis(),
                        backgroundColor = backgroundColor,
                        privacy = privacy,
                        commentPermission = commentPermission,
                        videoUrl = videoUrl,
                        thumbnailUrl = thumbnailUrl,
                        videoDuration = videoDuration
                    )
                )

                repository.enqueueItem(
                    OfflineSyncQueueEntity(
                        actionType = "CREATE_POST",
                        itemId = tempPostId,
                        payloadString1 = caption,
                        payloadString2 = imageUrls.joinToString(","),
                        payloadString3 = backgroundColor ?: "none",
                        payloadString4 = privacy,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updatePostDetails(
        postId: String,
        caption: String,
        privacy: String,
        commentPermission: String
    ) {
        viewModelScope.launch {
            repository.updatePostDetails(postId, caption, privacy, commentPermission)
            _viewingFullscreenPost.value?.let { currentViewing ->
                if (currentViewing.id == postId) {
                    _viewingFullscreenPost.value = currentViewing.copy(
                        caption = caption,
                        privacy = privacy,
                        commentPermission = commentPermission
                    )
                }
            }
        }
    }

    fun deleteOwnPost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>> {
        return repository.getCommentsForPost(postId)
    }

    fun triggerProfileVerification() {
        viewModelScope.launch {
            repository.triggerVerificationReview()
        }
    }

    fun handleKycReviewOverride(approvedProfile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveUserProfile(approvedProfile)
        }
    }

    fun updateMyBio(newBio: String) {
        viewModelScope.launch {
            val me = repository.userProfileFlow.first() ?: UserProfileEntity()
            repository.saveUserProfile(me.copy(bio = newBio))
        }
    }

    fun updateUserProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
        }
    }

    fun updateMyDetails(name: String, age: Int, location: String, occupation: String, relationshipGoals: String, interests: List<String>) {
        viewModelScope.launch {
            val me = repository.userProfileFlow.first() ?: UserProfileEntity()
            repository.saveUserProfile(
                me.copy(
                    name = name,
                    age = age,
                    location = location,
                    occupation = occupation,
                    relationshipGoals = relationshipGoals,
                    interests = interests
                )
            )
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }

    fun handleLogout() {
        sharedPrefs.edit().putBoolean("is_logged_in", false).apply()
        _currentScreen.value = "WELCOME"
        _currentTab.value = 0
        _authEmail.value = ""
        _authPassword.value = ""
        _authConfirmPassword.value = ""
    }
}
