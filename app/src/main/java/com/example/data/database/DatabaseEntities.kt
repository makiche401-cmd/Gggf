package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "me",
    val name: String = "Sarah Johnson",
    val age: Int = 24,
    val gender: String = "Female",
    val location: String = "New York, USA",
    val occupation: String = "Marketing Manager",
    val education: String = "Bachelor's",
    val bio: String = "I love traveling, coffee, and good conversations. Looking for someone real to build something beautiful.",
    val relationshipGoals: String = "Long-term",
    val profilePics: List<String> = listOf("me_1"),
    val interests: List<String> = listOf("Coffee", "Travel", "Art", "Books", "Yoga", "Wine Tasting", "Cooking", "Photography"),
    val verificationStatus: String = "verified", // "unverified", "pending", "verified"
    val profileViews: Int = 325,
    val likesReceived: Int = 256,
    val matchesCount: Int = 12,
    val postsCount: Int = 3,

    // Cover Photo & Advanced Profile Details
    val coverPhotoUrl: String = "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&q=80&w=600",
    val pronounsInput: String = "She/They",
    val zodiacSign: String = "Libra ♎",
    val religionSetting: String = "Spiritual, Open-Minded",
    val drinkingStatus: String = "Socially",
    val smokingStatus: String = "Never",
    val fitnessLevel: String = "Highly Active",
    val petsStatus: String = "Has Dog 🐶",
    val childrenStatus: String = "Maybe in the future",
    val relationshipStatus: String = "Single & Looking",
    val lookingForSetting: String = "Dating & Marriage",
    val loveLanguageSetting: String = "Quality Time & Words of Affirmation",
    val favoriteMusic: String = "Indie Electronic, Jazz Hop, Lofi Synth",
    val favoriteMovies: String = "Sci-Fi, Cyberpunk 2049, Indie Rom-coms",
    val favoriteBooks: String = "Sapiens, Meditations, Designing for Delight",
    val travelInterests: String = "Kyoto, Swiss Alps Resorts, coastal roadtrips",
    val instagramLink: String = "alex_martinez",
    val spotifyLink: String = "alex.spins",

    // Customization Settings
    val profileThemeName: String = "Premium Violet Glow",
    val activeAccentColorHex: String = "#8D3BFF",
    val profileFrameName: String = "None",
    val customEmojiStatus: String = "✨",
    val customBadgeSelection: String = "Spotlight Member",
    val activeStickerSelection: String = "✈️ Wanderlust",
    val animateProfileBgEffect: Boolean = true,

    // Discovery Settings
    val verifiedOnlyMode: Boolean = false,
    val recentlyActiveOnlyMode: Boolean = true,
    val onlineNowOnlyMode: Boolean = false,
    val newMembersOnlyMode: Boolean = false,
    val hideInactiveProfilesMode: Boolean = true,
    val distanceRadiusSlider: Float = 45f,
    val countryFilterText: String = "United States",
    val cityFilterText: String = "Los Angeles",
    val languageFilterText: String = "English, Spanish",
    val educationFilterText: String = "Master's Degree",
    val occupationFilterText: String = "Product Lead",
    val heightFilterSlider: Float = 168f,
    val interestMatchingSlider: Float = 85f,

    // Chat Settings
    val messageRequestsEnabled: Boolean = true,
    val readReceiptsToggle: Boolean = true,
    val typingIndicatorsToggle: Boolean = true,
    val messageReactionsToggle: Boolean = true,
    val autoDeleteMessagesLabel: String = "Never",
    val screenshotPreventionToggle: Boolean = false,
    val chatBackgroundThemeName: String = "Starry Midnight",
    val pinnedConversationsCount: Int = 2,
    val archivedChatsCount: Int = 4,

    // Safety Settings
    val trustedContactNumber: String = "+1 (310) 555-0199",
    val safetyCheckInReminders: Boolean = true,
    val spamProtectionEnabled: Boolean = true,
    val botDetectionEnabled: Boolean = true,
    val catfishDetectionEnabled: Boolean = true,

    // Privacy Settings
    val incognitoMode: Boolean = false,
    val hideOnlinePresence: Boolean = false,
    val obfuscateProximity: Boolean = false,
    val hideMyAge: Boolean = false,
    val allowSearchIndexing: Boolean = true,

    // Notification toggles
    val notifyNewMatch: Boolean = true,
    val notifyNewMessage: Boolean = true,
    val notifyVisitorAlerts: Boolean = true,
    val notifyVerificationStatus: Boolean = true,
    val notifyProfileViewAlerts: Boolean = false,
    val notifyMentionAlerts: Boolean = true,
    val notifySafetyAlerts: Boolean = true,
    val notifyGroupNotifications: Boolean = true,
    val notifyEventNotifications: Boolean = true,
    val notifyMarketingPreferences: Boolean = false,

    // Account & Security features
    val accountEmailText: String = "alex.designer@viora.io",
    val accountPhoneText: String = "+1 (310) 555-9821",
    val isTwoFactorEnabled: Boolean = false,
    val rsvpEventsCsv: String = "Speed Dating LA,Mixer Night",
    val backupCodesCsv: String = "V09Y-A2BE,H98A-E7FF,C102-K99D,OP72-99EA",
    val userDetailsLogsCsv: String = "Login from Google Pixel 8 Pro - LA, USA [Success]|||KYC selfie-verification level 1 passed [Certified]|||GDPR local database check: ok"
)

@Entity(tableName = "dating_profile")
data class DatingProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val gender: String,
    val location: String,
    val distanceKm: Double,
    val occupation: String,
    val education: String,
    val bio: String,
    val relationshipGoals: String,
    val profilePics: List<String>,
    val interests: List<String>,
    val isFollowing: Boolean = false,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isMatched: Boolean = false,
    val onlineStatus: String = "offline", // "online", "away", "offline"
    val verified: Boolean = false,
    val likesCount: Int = 0,
    val streak: Int = 0
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val authorLabel: String, // "2km away" or "2h ago" etc.
    val caption: String,
    val imageUrls: List<String>,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val backgroundColor: String? = null,
    val privacy: String = "Everyone",
    val commentPermission: String = "Everyone",
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val viewsCount: Int = 0,
    val videoDuration: Int = 0,
    val isBookmarked: Boolean = false,
    val isReported: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatUserId: String, // The user ID we are chatting with
    val senderId: String,   // "me" or other userId
    val textContent: String,
    val mediaUrl: String? = null,
    val audioDurationSec: Int = 0,
    val type: String = "text", // "text", "voice", "image", "gif"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sent", // "sent", "delivered", "read"
    val reaction: String? = null,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorName: String,
    val authorAvatar: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val parentId: String? = null,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false
)

@Entity(tableName = "offline_sync_queue")
data class OfflineSyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0,
    val actionType: String, // "CREATE_POST", "TOGGLE_POST_LIKE", "ADD_COMMENT", "SEND_MESSAGE", "TOGGLE_COMMENT_LIKE"
    val itemId: String,     // Target/created item unique ID
    val payloadString1: String = "", // caption, comment text, message content
    val payloadString2: String = "", // CSV images, mediaUrl
    val payloadString3: String = "", // extra configs like replyToId, privacy, etc.
    val payloadString4: String = "", // extra configs like commentPermission, replyToSender, etc.
    val payloadLong: Long = 0,       // voice duration, timestamp, counts
    val timestamp: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split("|||")
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        if (list == null) return ""
        return list.joinToString("|||")
    }
}
