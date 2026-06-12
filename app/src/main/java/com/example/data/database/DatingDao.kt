package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DatingDao {
    // -------------------------------------------------------------
    // User Profile Queries
    // -------------------------------------------------------------
    @Query("SELECT * FROM user_profile WHERE id = 'me' LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 'me' LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    // -------------------------------------------------------------
    // Swipeable/Dating Profiles
    // -------------------------------------------------------------
    @Query("SELECT * FROM dating_profile WHERE isLiked = 0 AND isDisliked = 0")
    fun getDiscoverableProfiles(): Flow<List<DatingProfileEntity>>

    @Query("SELECT * FROM dating_profile WHERE isMatched = 1")
    fun getMatchedProfiles(): Flow<List<DatingProfileEntity>>

    @Query("SELECT * FROM dating_profile")
    fun getAllProfilesFlow(): Flow<List<DatingProfileEntity>>

    @Query("SELECT * FROM dating_profile WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): DatingProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDatingProfiles(profiles: List<DatingProfileEntity>)

    @Update
    suspend fun updateProfile(profile: DatingProfileEntity)

    @Query("UPDATE dating_profile SET isLiked = :isLiked, isDisliked = :isDisliked, isMatched = :isMatched WHERE id = :id")
    suspend fun updateSwipeState(id: String, isLiked: Boolean, isDisliked: Boolean, isMatched: Boolean)

    @Query("UPDATE dating_profile SET isFollowing = :isFollowing WHERE id = :id")
    suspend fun updateFollowingState(id: String, isFollowing: Boolean)

    // -------------------------------------------------------------
    // Posts Queries
    // -------------------------------------------------------------
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getPostsFeed(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("UPDATE posts SET isLiked = :isLiked, likesCount = likesCount + :likesDiff WHERE id = :postId")
    suspend fun updatePostLike(postId: String, isLiked: Boolean, likesDiff: Int)

    @Query("UPDATE posts SET viewsCount = viewsCount + 1 WHERE id = :postId")
    suspend fun incrementPostViews(postId: String)

    @Query("UPDATE posts SET isBookmarked = :isBookmarked WHERE id = :postId")
    suspend fun updatePostBookmark(postId: String, isBookmarked: Boolean)

    @Query("UPDATE posts SET isReported = 1 WHERE id = :postId")
    suspend fun reportPost(postId: String)

    // -------------------------------------------------------------
    // Comments Queries
    // -------------------------------------------------------------
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("UPDATE comments SET isLikedByMe = :isLiked, likesCount = :likesCount WHERE id = :commentId")
    suspend fun updateCommentLike(commentId: String, isLiked: Boolean, likesCount: Int)

    // -------------------------------------------------------------
    // Messages Queries
    // -------------------------------------------------------------
    @Query("SELECT * FROM messages WHERE chatUserId = :chatUserId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatUserId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("UPDATE messages SET status = 'read' WHERE chatUserId = :chatUserId AND senderId != 'me' AND status != 'read'")
    suspend fun markMessagesAsRead(chatUserId: String)

    @Query("DELETE FROM messages WHERE chatUserId = :chatUserId")
    suspend fun clearMessagesForChat(chatUserId: String)

    // -------------------------------------------------------------
    // Offline Sync Queue Queries
    // -------------------------------------------------------------
    @Query("SELECT * FROM offline_sync_queue ORDER BY timestamp ASC")
    fun getPendingQueue(): Flow<List<OfflineSyncQueueEntity>>

    @Query("SELECT * FROM offline_sync_queue ORDER BY timestamp ASC")
    suspend fun getPendingQueueDirect(): List<OfflineSyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: OfflineSyncQueueEntity)

    @Query("DELETE FROM offline_sync_queue WHERE queueId = :id")
    suspend fun deleteQueueItemById(id: Long)
}
