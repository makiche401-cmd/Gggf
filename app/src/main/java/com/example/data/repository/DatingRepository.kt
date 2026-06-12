package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DatingRepository(private val context: Context, val datingDao: DatingDao) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        // Run backing seed task
        repositoryScope.launch {
            seedDatabaseIfEmpty()
        }
    }

    // Live Flow properties
    val userProfileFlow: Flow<UserProfileEntity?> = datingDao.getUserProfile()
    val discoverableProfilesFlow: Flow<List<DatingProfileEntity>> = datingDao.getDiscoverableProfiles()
    val matchedProfilesFlow: Flow<List<DatingProfileEntity>> = datingDao.getMatchedProfiles()
    val postsFlow: Flow<List<PostEntity>> = datingDao.getPostsFeed()
    val allProfilesFlow: Flow<List<DatingProfileEntity>> = datingDao.getAllProfilesFlow()
    val allMessagesFlow: Flow<List<MessageEntity>> = datingDao.getAllMessagesFlow()
    val pendingQueueFlow: Flow<List<OfflineSyncQueueEntity>> = datingDao.getPendingQueue()

    suspend fun getPendingQueueDirect(): List<OfflineSyncQueueEntity> = datingDao.getPendingQueueDirect()
    suspend fun enqueueItem(item: OfflineSyncQueueEntity) = datingDao.insertQueueItem(item)
    suspend fun dequeueItemById(id: Long) = datingDao.deleteQueueItemById(id)

    fun getMessagesForChat(userId: String): Flow<List<MessageEntity>> = datingDao.getMessagesForChat(userId)
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>> = datingDao.getCommentsForPost(postId)

    // User actions
    suspend fun saveUserProfile(profile: UserProfileEntity) {
        datingDao.insertUserProfile(profile)
    }

    suspend fun updateSwipe(id: String, liked: Boolean, disliked: Boolean): DatingProfileEntity? {
        val profile = datingDao.getProfileById(id) ?: return null
        // 80% chance of high compatibility match is auto triggered on like
        val isMatch = liked && (profile.name in listOf("Sophia Martinez", "Sharon", "Amina", "Cynthia"))
        datingDao.updateSwipeState(id, liked, disliked, isMatch)

        if (isMatch) {
            // Seed welcome auto-response message in the chat!
            val updatedProfile = profile.copy(isLiked = true, isMatched = true)
            datingDao.updateProfile(updatedProfile)

            // Dynamic welcome greeting from the matched partner
            val matchGreeting = when (profile.name) {
                "Sophia Martinez" -> "Hey! Your profile caught my eye. Coffee on me? ☕✨"
                "Sharon" -> "Hey there! I saw you are a coffee lover too! Excellent taste. 😊"
                "Amina" -> "Hello! Love your background in design. Let's build something beautiful!"
                else -> "Hey! It's a match. Let's chat and connect! 🎉"
            }

            datingDao.insertMessage(
                MessageEntity(
                    id = "msg_match_welcome_${id}",
                    chatUserId = id,
                    senderId = id,
                    textContent = matchGreeting,
                    timestamp = System.currentTimeMillis() - 5000,
                    status = "read"
                )
            )

            // Trigger notification
            insertNotification(
                title = "New Match!",
                content = "You and ${profile.name} matched! Send them a message.",
                type = "match"
            )
        }
        return datingDao.getProfileById(id)
    }

    suspend fun triggerVerificationReview() {
        val currentProfile = datingDao.getUserProfileDirect() ?: UserProfileEntity()
        datingDao.insertUserProfile(currentProfile.copy(verificationStatus = "pending"))

        // Simulate manual background process after 3 seconds, approving it!
        repositoryScope.launch {
            kotlinx.coroutines.delay(3000)
            val direct = datingDao.getUserProfileDirect()
            if (direct?.verificationStatus == "pending") {
                datingDao.insertUserProfile(direct.copy(verificationStatus = "verified"))
                insertNotification(
                    title = "Profile Verified",
                    content = "Your manual review is complete! You now have a purple verification badge. ✨",
                    type = "verification"
                )
            }
        }
    }

    suspend fun togglePostLike(postId: String, currentState: Boolean) {
        val diff = if (currentState) -1 else 1
        datingDao.updatePostLike(postId, !currentState, diff)
    }

    suspend fun setFollowing(id: String, follow: Boolean) {
        datingDao.updateFollowingState(id, follow)
    }

    suspend fun addComment(postId: String, text: String, parentId: String? = null) {
        val user = datingDao.getUserProfileDirect() ?: UserProfileEntity()
        val randomId = "comment_${System.currentTimeMillis()}"
        datingDao.insertComment(
            CommentEntity(
                id = randomId,
                postId = postId,
                authorName = user.name,
                authorAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200", // Me avatar
                text = text,
                timestamp = System.currentTimeMillis(),
                parentId = parentId,
                likesCount = 0,
                isLikedByMe = false
            )
        )
    }

    suspend fun toggleCommentLike(commentId: String, isLiked: Boolean, currentLikes: Int) {
        val newIsLiked = !isLiked
        val newLikesCount = if (newIsLiked) currentLikes + 1 else maxOf(0, currentLikes - 1)
        datingDao.updateCommentLike(commentId, newIsLiked, newLikesCount)
    }

    suspend fun createNewPost(
        caption: String,
        imageUrls: List<String>,
        backgroundColor: String? = null,
        privacy: String = "Everyone",
        commentPermission: String = "Everyone",
        videoUrl: String? = null,
        thumbnailUrl: String? = null,
        videoDuration: Int = 0
    ) {
        val user = datingDao.getUserProfileDirect() ?: UserProfileEntity()
        val postId = "post_${System.currentTimeMillis()}"
        datingDao.insertPost(
            PostEntity(
                id = postId,
                authorId = "me",
                authorName = user.name,
                authorAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200",
                authorLabel = "Just now",
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
    }

    suspend fun incrementPostViews(postId: String) {
        datingDao.incrementPostViews(postId)
    }

    suspend fun updatePostBookmark(postId: String, isBookmarked: Boolean) {
        datingDao.updatePostBookmark(postId, isBookmarked)
    }

    suspend fun reportPost(postId: String) {
        datingDao.reportPost(postId)
    }

    suspend fun updatePostDetails(
        postId: String,
        caption: String,
        privacy: String,
        commentPermission: String
    ) {
        val post = datingDao.getPostById(postId)
        if (post != null) {
            datingDao.insertPost(
                post.copy(
                    caption = caption,
                    privacy = privacy,
                    commentPermission = commentPermission
                )
            )
        }
    }

    suspend fun deletePost(postId: String) {
        datingDao.deletePostById(postId)
    }

    suspend fun sendChatMessage(
        userId: String,
        content: String,
        voiceDuration: Int = 0,
        imageUrl: String? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSender: String? = null
    ) {
        val messageId = "msg_user_send_${System.currentTimeMillis()}"
        val type = when {
            voiceDuration > 0 -> "voice"
            imageUrl != null -> "image"
            else -> "text"
        }
        val initialMsg = MessageEntity(
            id = messageId,
            chatUserId = userId,
            senderId = "me",
            textContent = content,
            mediaUrl = imageUrl,
            audioDurationSec = voiceDuration,
            type = type,
            timestamp = System.currentTimeMillis(),
            status = "sending",
            replyToId = replyToId,
            replyToText = replyToText,
            replyToSender = replyToSender
        )
        datingDao.insertMessage(initialMsg)

        // Mock automated instant response queue simulating real time match engagement
        repositoryScope.launch {
            // 1. Sent instantly after 250ms
            kotlinx.coroutines.delay(250)
            datingDao.insertMessage(initialMsg.copy(status = "sent"))

            // 2. Delivered after 400ms
            kotlinx.coroutines.delay(400)
            datingDao.insertMessage(initialMsg.copy(status = "delivered"))

            // 3. Read (Seen) after 600ms
            kotlinx.coroutines.delay(600)
            datingDao.insertMessage(initialMsg.copy(status = "read"))

            // Let partner start typing response
            kotlinx.coroutines.delay(1000)
            // Auto response matching user context
            val partnerResponse = when {
                voiceDuration > 0 -> "Oh, I love your voice! Sounds so nice. Tell me more about what you do!"
                content.contains("hello", true) || content.contains("hey", true) || content.contains("hi", true) -> "Hey! How is your week going? 🌸"
                content.contains("coffee", true) -> "Yes please! I know a fantastic little coffee spot with the best matcha and espresso. When are you free?"
                else -> "That is so cool! Let's definitely catch up soon. What are you looking for on here? 😊"
            }

            val responseId = "msg_partner_reply_${System.currentTimeMillis()}"
            datingDao.insertMessage(
                MessageEntity(
                    id = responseId,
                    chatUserId = userId,
                    senderId = userId,
                    textContent = partnerResponse,
                    timestamp = System.currentTimeMillis(),
                    status = "read"
                )
            )

            // Notification update
            val profile = datingDao.getProfileById(userId)
            insertNotification(
                title = "New message from ${profile?.name ?: "Viora Match"}",
                content = partnerResponse,
                type = "chat"
            )
        }
    }

    suspend fun markChatAsRead(userId: String) {
        datingDao.markMessagesAsRead(userId)
    }

    suspend fun clearChatMessages(userId: String) {
        datingDao.clearMessagesForChat(userId)
    }

    suspend fun deleteMessage(messageId: String) {
        datingDao.deleteMessageById(messageId)
    }

    suspend fun updateMessage(message: MessageEntity) {
        datingDao.updateMessage(message)
    }

    // Dynamic Notifications state support
    private val _notifications = kotlinx.coroutines.flow.MutableStateFlow<List<VioraNotification>>(emptyList())
    val notificationsFlow: Flow<List<VioraNotification>> = _notifications

    data class VioraNotification(
        val id: String,
        val title: String,
        val content: String,
        val timestamp: Long = System.currentTimeMillis(),
        val read: Boolean = false,
        val type: String // "match", "chat", "like", "comment", "verification"
    )

    fun insertNotification(title: String, content: String, type: String) {
        val newNotif = VioraNotification(
            id = "notif_${System.currentTimeMillis()}",
            title = title,
            content = content,
            type = type
        )
        val currentList = _notifications.value.toMutableList()
        currentList.add(0, newNotif)
        _notifications.value = currentList
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(read = true) }
    }

    // SQLite data generator
    private suspend fun seedDatabaseIfEmpty() {
        // Create user profile if missing
        val existingUserProfile = datingDao.getUserProfileDirect()
        if (existingUserProfile == null) {
            datingDao.insertUserProfile(UserProfileEntity())
        }

        // Seed or update our specific candidates (Sophia, James, Isabella, Olivia, and others)
        val seededProfiles = listOf(
            DatingProfileEntity(
                id = "profile_sophia",
                name = "Sophia Martinez",
                age = 24,
                gender = "Female",
                location = "Los Angeles, USA",
                distanceKm = 2.0,
                occupation = "Digital Creator",
                education = "USC School of Arts",
                bio = "Coffee lover ☕ | Adventure seeker 🌍\nLooking for genuine connections ✨\nLet's trade music albums and explore vinyl shops around Sunset Boulevard!",
                relationshipGoals = "Long-term match",
                profilePics = listOf(
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=600",
                    "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=600"
                ),
                interests = listOf("Travel", "Music", "Coffee", "Photography"),
                onlineStatus = "online",
                verified = true,
                likesCount = 256
            ),
            DatingProfileEntity(
                id = "profile_james",
                name = "James Carter",
                age = 26,
                gender = "Male",
                location = "San Diego, USA",
                distanceKm = 5.0,
                occupation = "Photographer & Film Director",
                education = "San Diego State University",
                bio = "Capturing light and shadows 📷 | Outdoors enthusiast 🥾\nLet's get lost in the mountains or discuss cinema history over cold brews.",
                relationshipGoals = "Casual dating",
                profilePics = listOf(
                    "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=600"
                ),
                interests = listOf("Photography", "Hiking", "Movies", "Travel"),
                onlineStatus = "online",
                verified = true,
                likesCount = 189
            ),
            DatingProfileEntity(
                id = "profile_isabella",
                name = "Isabella Wilson",
                age = 22,
                gender = "Female",
                location = "New York, USA",
                distanceKm = 7.0,
                occupation = "Fashion Stylist",
                education = "FIT NY",
                bio = "Design is in the details 🎨 | Obsessed with runway styles and rooftop yoga. 🧘‍♀️ Let's explore modern art galleries!",
                relationshipGoals = "Long-term relationship",
                profilePics = listOf(
                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=600"
                ),
                interests = listOf("Art", "Fashion", "Yoga", "Music"),
                onlineStatus = "online",
                verified = true,
                likesCount = 224
            ),
            DatingProfileEntity(
                id = "profile_olivia",
                name = "Olivia Anderson",
                age = 23,
                gender = "Female",
                location = "Miami, USA",
                distanceKm = 8.0,
                occupation = "Dance Choreographer",
                education = "Miami Arts",
                bio = "Salty air and sandy hair 🏖️ | Professional dancer with a heart of gold. Let's cook some pasta and dance under the starlight!",
                relationshipGoals = "Date to marry",
                profilePics = listOf(
                    "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=600"
                ),
                interests = listOf("Beach", "Food", "Dancing", "Travel"),
                onlineStatus = "online",
                verified = true,
                likesCount = 143
            ),
            DatingProfileEntity(
                id = "profile_sharon",
                name = "Sharon",
                age = 24,
                gender = "Female",
                location = "Nairobi, Kenya",
                distanceKm = 14.5,
                occupation = "Consultant & Foodie",
                education = "UON",
                bio = "Coffee lover ☕ | Good vibes only ✨\n#GoodVibes #CoffeeLover #Travel\nSpontaneous road trips are my favorite love language.",
                relationshipGoals = "Date to marry",
                profilePics = listOf(
                    "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=600"
                ),
                interests = listOf("Travel", "Music", "Coffee", "Food"),
                onlineStatus = "away",
                verified = false,
                likesCount = 128
            ),
            DatingProfileEntity(
                id = "profile_amina",
                name = "Amina",
                age = 23,
                gender = "Female",
                location = "Bel Air, CA",
                distanceKm = 4.2,
                occupation = "Data Scientist",
                education = "Stanford University",
                bio = "Coding neural networks during the daytime, writing poetry and catching deep jazz shows at night. 🎷",
                relationshipGoals = "Open-minded dating",
                profilePics = listOf(
                    "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=600"
                ),
                interests = listOf("Technology", "Music", "Movies", "Gaming"),
                onlineStatus = "online",
                verified = true,
                likesCount = 94
            )
        )
        datingDao.insertDatingProfiles(seededProfiles)

        // Seed or update Posts Feed
        val seededPosts = listOf(
            PostEntity(
                id = "post_sophia_1",
                authorId = "profile_sophia",
                authorName = "Sophia Martinez",
                authorAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200",
                authorLabel = "Los Angeles, USA • 2km away",
                caption = "Catching beautiful california skate loops at sunset 🛹🌅 Let's trade music playlists!",
                imageUrls = emptyList(),
                likesCount = 256,
                commentsCount = 32,
                isLiked = false,
                isSaved = false,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 30,
                videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-young-woman-skating-at-sunset-41712-large.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=600",
                videoDuration = 24,
                viewsCount = 1420
            ),
            PostEntity(
                id = "post_james_1",
                authorId = "profile_james",
                authorName = "James Carter",
                authorAvatar = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=200",
                authorLabel = "San Diego, USA • 5km away",
                caption = "Nature captures and camera tests on a spontaneous joyride! 🚗 Adventure awaits.",
                imageUrls = emptyList(),
                likesCount = 189,
                commentsCount = 12,
                isLiked = false,
                isSaved = false,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 3,
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=600",
                videoDuration = 15,
                viewsCount = 890
            ),
            PostEntity(
                id = "post_isabella_1",
                authorId = "profile_isabella",
                authorName = "Isabella Wilson",
                authorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=200",
                authorLabel = "New York, USA • 7km away",
                caption = "Designing a new layout concept today 🎨 Aesthetic spacing, deep colors, and modern shapes. Art heals the soul!",
                imageUrls = listOf("https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=600"),
                likesCount = 224,
                commentsCount = 18,
                isLiked = false,
                isSaved = false,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5
            ),
            PostEntity(
                id = "post_olivia_1",
                authorId = "profile_olivia",
                authorName = "Olivia Anderson",
                authorAvatar = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=200",
                authorLabel = "Miami, USA • 8km away",
                caption = "Late night neon dance choreography rehearsal 🕺💜 Loving this dynamic lighting!",
                imageUrls = emptyList(),
                likesCount = 143,
                commentsCount = 9,
                isLiked = false,
                isSaved = false,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 10,
                videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-woman-dancing-in-front-of-wall-with-neon-lights-39875-large.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=600",
                videoDuration = 18,
                viewsCount = 1120
            )
        )
        datingDao.insertPosts(seededPosts)

        // Seed some starter comments
        datingDao.insertComment(
            CommentEntity(
                id = "first_comment",
                postId = "post_sophia_1",
                authorName = "Michael",
                authorAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=200",
                text = "This photo style looks incredible Sophia! ✨ What camera is that?",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 10
            )
        )

        // Seed some starter notifications
        insertNotification(
            title = "Welcome to Viora!",
            content = "Complete your bio and start browsing to discover real connections. ✨",
            type = "verification"
        )
    }
}
