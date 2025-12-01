package com.example.cmpt362group1.database

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class UserViewModel(
    private val repository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _userState = MutableStateFlow<UserUiState>(UserUiState.Idle)
    val userState: StateFlow<UserUiState> = _userState.asStateFlow()

    private val _operationState = MutableStateFlow<OperationUiState>(OperationUiState.Idle)
    val operationState: StateFlow<OperationUiState> = _operationState.asStateFlow()

    fun loadUser(uid: String) {
        viewModelScope.launch {
            _userState.value = UserUiState.Loading

            repository.getUser(uid)
                .catch { e ->
                    Log.e("INFO UserViewModel", "Error loading user", e)
                    _userState.value = UserUiState.Error(e.message ?: "Unknown error")
                }
                .collect { user ->
                    _userState.value = if (user != null) {
                        UserUiState.Success(user)
                    } else {
                        UserUiState.Error("User not found")
                    }
                }
        }
    }

    fun createNewUser(user: FirebaseUser) {
        val newUser = User(
            id = user.uid,  // Add this - was missing
            email = user.email ?: "",
            displayName = user.displayName ?: "",
            photoUrl = user.photoUrl?.toString() ?: "",
        )

        createUser(newUser)
    }

    fun createUser(user: User) {
        viewModelScope.launch {
            _operationState.value = OperationUiState.Loading

            val result = repository.createUser(user)

            _operationState.value = if (result.isSuccess) {
                Log.d("INFO UserViewModel", "User created: ${user.id}")
                OperationUiState.Success("User created successfully")
            } else {
                Log.e("INFO UserViewModel", "Failed to create user", result.exceptionOrNull())
                OperationUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to create user"
                )
            }
        }
    }

    fun updateUser(uid: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _operationState.value = OperationUiState.Loading

            val result = repository.updateUser(uid, updates)

            _operationState.value = if (result.isSuccess) {
                Log.d("INFO UserViewModel", "User updated: $uid")
                OperationUiState.Success("User updated successfully")
            } else {
                Log.e("INFO UserViewModel", "Failed to update user", result.exceptionOrNull())
                OperationUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to update user"
                )
            }
        }
    }

    fun deleteUser(uid: String) {
        viewModelScope.launch {
            _operationState.value = OperationUiState.Loading

            val result = repository.deleteUser(uid)

            _operationState.value = if (result.isSuccess) {
                Log.d("INFO UserViewModel", "User deleted: $uid")
                OperationUiState.Success("User deleted successfully")
            } else {
                Log.e("INFO UserViewModel", "Failed to delete user", result.exceptionOrNull())
                OperationUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to delete user"
                )
            }
        }
    }

    fun addCreatedEvent(uid: String, eventId: String) {
        viewModelScope.launch {
            val result = repository.addCreatedEvent(uid, eventId)

            if (result.isFailure) {
                Log.e("INFO UserViewModel", "Failed to add created event", result.exceptionOrNull())
            } else {
                Log.d("INFO UserViewModel", "Added created event: $eventId to user: $uid")
            }
        }
    }

    fun addJoinedEvent(uid: String, eventId: String) {
        viewModelScope.launch {
            val result = repository.addJoinedEvent(uid, eventId)

            if (result.isFailure) {
                Log.e("INFO UserViewModel", "Failed to add joined event", result.exceptionOrNull())
            } else {
                Log.d("INFO UserViewModel", "Added joined event: $eventId to user: $uid")
            }
        }
    }

    fun removeCreatedEvent(uid: String, eventId: String) {
        viewModelScope.launch {
            val result = repository.removeCreatedEvent(uid, eventId)

            if (result.isFailure) {
                Log.e("INFO UserViewModel", "Failed to remove created event", result.exceptionOrNull())
            } else {
                Log.d("INFO UserViewModel", "Removed created event: $eventId from user: $uid")
            }
        }
    }

    fun removeJoinedEvent(uid: String, eventId: String) {
        viewModelScope.launch {
            val result = repository.removeJoinedEvent(uid, eventId)

            if (result.isFailure) {
                Log.e("INFO UserViewModel", "Failed to remove joined event", result.exceptionOrNull())
            } else {
                Log.d("INFO UserViewModel", "Removed joined event: $eventId from user: $uid")
            }
        }
    }

    fun followUser(currentUserId: String, userToFollowId: String) {
        viewModelScope.launch {
            val result = repository.followUser(currentUserId, userToFollowId)

            if (result.isFailure) {
                Log.e("INFO UserViewModel", "Failed to follow user", result.exceptionOrNull())
            } else {
                Log.d("INFO UserViewModel", "User $currentUserId followed user $userToFollowId")
            }
        }
    }

    fun unfollowUser(currentUserId: String, userToUnfollowId: String) {
        viewModelScope.launch {
            val result = repository.unfollowUser(currentUserId, userToUnfollowId)

            if (result.isFailure) {
                Log.e("INFO UserViewModel", "Failed to unfollow user", result.exceptionOrNull())
            } else {
                Log.d("INFO UserViewModel", "User $currentUserId unfollowed user $userToUnfollowId")
            }
        }
    }

    suspend fun getUsersByIds(userIds: List<String>): List<User> {
        val result = repository.getUsersByIds(userIds)
        return if (result.isSuccess) {
            result.getOrNull() ?: emptyList()
        } else {
            Log.e("INFO UserViewModel", "Failed to get users by IDs", result.exceptionOrNull())
            emptyList()
        }
    }

    fun resetOperationState() {
        _operationState.value = OperationUiState.Idle
    }
}

sealed class UserUiState {
    data object Idle : UserUiState()
    data object Loading : UserUiState()
    data class Success(val user: User) : UserUiState()
    data class Error(val message: String) : UserUiState()
}

sealed class OperationUiState {
    data object Idle : OperationUiState()
    data object Loading : OperationUiState()
    data class Success(val message: String) : OperationUiState()
    data class Error(val message: String) : OperationUiState()
}