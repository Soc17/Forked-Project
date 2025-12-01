package com.example.cmpt362group1.database

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class EventRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EventRepository {
    override val collection: String
        get() = "events"

    override fun getEvent(id: String): Flow<Event?> = callbackFlow {
        val listenerRegistration = firestore.collection(collection)
            .document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val event = snapshot?.toObject<Event>()
                trySend(event)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun getAllEvents(): Flow<List<Event?>> = callbackFlow {
        val listenerRegistration = firestore.collection(collection)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot
                    ?.documents
                    ?.mapNotNull {
                        it.toObject<Event>()
                    }
                    ?: emptyList()
                trySend(events)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun addEvent(event: Event): Result<String> {
        return try {
            val documentRef = firestore.collection(collection).document()
            val eventTagged= event.copy(id = documentRef.id)
            documentRef.set(eventTagged).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEvent(eventId: String, updatedEvent: Event): Result<Unit> {
        return try {
            firestore.collection(collection)
                .document(eventId)
                .set(updatedEvent.copy(id = eventId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            firestore.collection(collection)
                .document(eventId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearEvents(): Result<Unit> {
        return try {
            val snapshot = firestore.collection(collection).get().await()
            val batch = firestore.batch()
            for (document in snapshot.documents) {
                batch.delete(document.reference)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getComments(eventId: String): Flow<List<Comment>> = callbackFlow {
        val listenerRegistration = firestore.collection(collection)
            .document(eventId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                trySend(comments)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun postComment(eventId: String, comment: Comment): Result<Unit> {
        return try {
            firestore.collection(collection)
                .document(eventId)
                .collection("comments")
                .add(comment)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getParticipantsCount(eventId: String): Flow<Int> = callbackFlow {
        val listenerRegistration = firestore.collection("users")
            .whereArrayContains("eventsJoined", eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }

                trySend(snapshot?.size() ?: 0)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun getParticipantIds(eventId: String): Result<List<String>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereArrayContains("eventsJoined", eventId)
                .get()
                .await()

            val participantIds = snapshot.documents.map { it.id }
            Result.success(participantIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCheckIns(eventId: String): Flow<CheckInData> = callbackFlow {
        val listenerRegistration = firestore.collection(collection)
            .document(eventId)
            .collection("checkins")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(CheckInData(0, emptyList()))
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents ?: emptyList()
                val userIds = docs.mapNotNull { it.getString("userId") }
                trySend(CheckInData(docs.size, userIds))
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun checkIn(eventId: String, userId: String): Result<Unit> {
        return try {
            val data = mapOf(
                "userId" to userId,
                "method" to "manual",
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection(collection)
                .document(eventId)
                .collection("checkins")
                .document(userId)
                .set(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelCheckIn(eventId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection(collection)
                .document(eventId)
                .collection("checkins")
                .document(userId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(eventId: String, commentId: String): Result<Unit> {
        return try {
            firestore.collection("events").document(eventId)
                .collection("comments").document(commentId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun banUser(eventId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection("events").document(eventId)
                .update("bannedUserIds", FieldValue.arrayUnion(userId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unbanUser(eventId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection("events").document(eventId)
                .update("bannedUserIds", FieldValue.arrayRemove(userId)) // 使用 arrayRemove
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}