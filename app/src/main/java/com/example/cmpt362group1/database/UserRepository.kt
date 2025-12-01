package com.example.cmpt362group1.database

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(uid: String): Flow<User?>

    suspend fun createUser(user: User): Result<Unit>

    suspend fun updateUser(uid: String, updates: Map<String, Any>): Result<Unit>

    suspend fun deleteUser(uid: String): Result<Unit>

    suspend fun addCreatedEvent(uid: String, eventId: String): Result<Unit>

    suspend fun addJoinedEvent(uid: String, eventId: String): Result<Unit>

    suspend fun removeCreatedEvent(uid: String, eventId: String): Result<Unit>

    suspend fun removeJoinedEvent(uid: String, eventId: String): Result<Unit>

    suspend fun getUsersByIds(userIds: List<String>): Result<List<User>>

    suspend fun followUser(currentUserId: String, userToFollowId: String): Result<Unit>

    suspend fun unfollowUser(currentUserId: String, userToUnfollowId: String): Result<Unit>
}