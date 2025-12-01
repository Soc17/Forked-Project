package com.example.cmpt362group1.database

import kotlinx.coroutines.flow.Flow

interface EventRepository {
    val collection: String

    fun getEvent(id: String): Flow<Event?>

    fun getAllEvents(): Flow<List<Event?>>

    suspend fun addEvent(event: Event): Result<String>

    suspend fun updateEvent(eventId: String, updatedEvent: Event): Result<Unit>

    suspend fun deleteEvent(eventId: String): Result<Unit>

    suspend fun clearEvents(): Result<Unit>

    fun getComments(eventId: String): Flow<List<Comment>>

    suspend fun postComment(eventId: String, comment: Comment): Result<Unit>

    fun getParticipantsCount(eventId: String): Flow<Int>

    suspend fun getParticipantIds(eventId: String): Result<List<String>>

    fun getCheckIns(eventId: String): Flow<CheckInData>

    suspend fun checkIn(eventId: String, userId: String): Result<Unit>

    suspend fun cancelCheckIn(eventId: String, userId: String): Result<Unit>

    suspend fun deleteComment(eventId: String, commentId: String): Result<Unit>

    suspend fun banUser(eventId: String, userId: String): Result<Unit>
    suspend fun unbanUser(eventId: String, userId: String): Result<Unit>
}

data class CheckInData(
    val count: Int,
    val userIds: List<String>
)