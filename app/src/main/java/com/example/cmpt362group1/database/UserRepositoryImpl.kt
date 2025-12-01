package com.example.cmpt362group1.database

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
    }

    override fun getUser(uid: String): Flow<User?> = callbackFlow {
        val listenerRegistration = firestore.collection(USERS_COLLECTION)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val user = snapshot?.toObject<User>()
                trySend(user)
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun createUser(user: User): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCreatedEvent(uid: String, eventId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update("eventsCreated", FieldValue.arrayUnion(eventId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addJoinedEvent(uid: String, eventId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update("eventsJoined", FieldValue.arrayUnion(eventId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeCreatedEvent(uid: String, eventId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update("eventsCreated", FieldValue.arrayRemove(eventId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeJoinedEvent(uid: String, eventId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update("eventsJoined", FieldValue.arrayRemove(eventId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUsersByIds(userIds: List<String>): Result<List<User>> {
        if (userIds.isEmpty()) return Result.success(emptyList())
        return try {
            val chunks = userIds.chunked(10)
            val allUsers = mutableListOf<User>()

            for (chunk in chunks) {
                val snapshot = firestore.collection(USERS_COLLECTION)
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .await()
                allUsers.addAll(snapshot.toObjects(User::class.java))
            }
            Result.success(allUsers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun followUser(currentUserId: String, userToFollowId: String): Result<Unit> {
        return try {
            // Add userToFollowId to current user's followingList
            firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .update(
                    mapOf(
                        "followingList" to FieldValue.arrayUnion(userToFollowId),
                        "following" to FieldValue.increment(1)
                    )
                )
                .await()

            // Add currentUserId to userToFollow's followersList
            firestore.collection(USERS_COLLECTION)
                .document(userToFollowId)
                .update(
                    mapOf(
                        "followersList" to FieldValue.arrayUnion(currentUserId),
                        "followers" to FieldValue.increment(1)
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowUser(currentUserId: String, userToUnfollowId: String): Result<Unit> {
        return try {
            // Remove userToUnfollowId from current user's followingList
            firestore.collection(USERS_COLLECTION)
                .document(currentUserId)
                .update(
                    mapOf(
                        "followingList" to FieldValue.arrayRemove(userToUnfollowId),
                        "following" to FieldValue.increment(-1)
                    )
                )
                .await()

            // Remove currentUserId from userToUnfollow's followersList
            firestore.collection(USERS_COLLECTION)
                .document(userToUnfollowId)
                .update(
                    mapOf(
                        "followersList" to FieldValue.arrayRemove(currentUserId),
                        "followers" to FieldValue.increment(-1)
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}