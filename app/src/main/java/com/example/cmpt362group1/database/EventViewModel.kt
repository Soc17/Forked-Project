package com.example.cmpt362group1.database

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class EventViewModel(
    private val repository: EventRepository = EventRepositoryImpl(),
) : ViewModel() {

    private val _eventsState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val eventsState: StateFlow<EventsUiState> = _eventsState.asStateFlow()

    private val _eventState = MutableStateFlow<EventUiState>(EventUiState.Idle)
    val eventState: StateFlow<EventUiState> = _eventState.asStateFlow()

    private val _operationState = MutableStateFlow<OperationUiState>(OperationUiState.Idle)
    val operationState: StateFlow<OperationUiState> = _operationState.asStateFlow()

    private val _commentsState =
        MutableStateFlow<CommentsUiState>(CommentsUiState.Loading)
    val commentsState: StateFlow<CommentsUiState> = _commentsState.asStateFlow()

    private val _participantsCount = MutableStateFlow(0)
    val participantsCount: StateFlow<Int> = _participantsCount.asStateFlow()

    private val _arrivedCount = MutableStateFlow(0)
    val arrivedCount: StateFlow<Int> = _arrivedCount.asStateFlow()

    private val _isCurrentUserCheckedIn = MutableStateFlow(false)
    val isCurrentUserCheckedIn: StateFlow<Boolean> = _isCurrentUserCheckedIn.asStateFlow()

    private val _bannedUsersList = MutableStateFlow<List<User>>(emptyList())
    val bannedUsersList: StateFlow<List<User>> = _bannedUsersList.asStateFlow()

    init {
        loadAllEvents()
    }

    fun loadAllEvents() {
        viewModelScope.launch {
            _eventsState.value = EventsUiState.Loading
            Log.d("INFO", "LOADING EVENTS")
            repository.getAllEvents()
                .catch { e ->
                    Log.d("INFO", "Failed to load events", e)
                    _eventsState.value =
                        EventsUiState.Error(e.message ?: "Unknown error occurred")
                }
                .collect { events ->
                    Log.d("INFO", "Events loaded $events")
                    _eventsState.value =
                        EventsUiState.Success(events.filterNotNull())
                }
        }
    }

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _eventState.value = EventUiState.Loading

            repository.getEvent(eventId)
                .catch { e ->
                    Log.e("EventViewModel", "Error loading event", e)
                    _eventState.value = EventUiState.Error(e.message ?: "Unknown error occurred")
                }
                .collect { event ->
                    _eventState.value = if (event != null) {
                        EventUiState.Success(event)
                    } else {
                        EventUiState.Error("Event not found")
                    }
                }
        }
    }

    fun saveEvent(
        event: Event,
        onSuccess: (String) -> Unit = {},
        onError: (Throwable?) -> Unit = {}
    ) {
        viewModelScope.launch {
            _operationState.value = OperationUiState.Loading

            Log.d("EventViewModel", "Saving event: $event")

            val result = repository.addEvent(event)

            if (result.isSuccess) {
                val eventId = result.getOrNull()!!

                _operationState.value = OperationUiState.Success("Event saved successfully")
                Log.d("EventViewModel", "Event saved with ID: $eventId")

                onSuccess(eventId)
            } else {
                val error = result.exceptionOrNull()
                _operationState.value = OperationUiState.Error(
                    error?.message ?: "Failed to save event"
                )

                onError(error)
            }
        }
    }

    fun updateEvent(
        eventId: String,
        updatedEvent: Event,
        onSuccess: () -> Unit = {},
        onError: (Throwable?) -> Unit = {}
    ) {
        viewModelScope.launch {
            _operationState.value = OperationUiState.Loading

            val result = repository.updateEvent(eventId, updatedEvent)

            if (result.isSuccess) {
                Log.d("EventViewModel", "Event updated: $eventId")
                _operationState.value =
                    OperationUiState.Success("Event updated successfully")
                onSuccess()
            } else {
                val error = result.exceptionOrNull()
                Log.e("EventViewModel", "Failed to update event", error)
                _operationState.value =
                    OperationUiState.Error(error?.message ?: "Failed to update event")
                onError(error)
            }
        }
    }


    fun addEvent(event: Event) {
        viewModelScope.launch {
            _operationState.value = OperationUiState.Loading

            val result = repository.addEvent(event)

            _operationState.value = if (result.isSuccess) {
                OperationUiState.Success("Event added successfully")
            } else {
                OperationUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to add event"
                )
            }
        }
    }

    fun clearAllEvents() {
        viewModelScope.launch {
            _operationState.value = OperationUiState.Loading

            val result = repository.clearEvents()

            if (result.isSuccess) {
                _operationState.value = OperationUiState.Success("All events cleared")
            } else {
                val error = result.exceptionOrNull()
                _operationState.value = OperationUiState.Error(
                    error?.message ?: "Failed to clear events"
                )
            }
        }
    }

    fun startComments(eventId: String) {
        viewModelScope.launch {
            _commentsState.value = CommentsUiState.Loading

            repository.getComments(eventId)
                .catch { e ->
                    Log.e("EventViewModel", "Error listening comments", e)
                    _commentsState.value =
                        CommentsUiState.Error(e.message ?: "Failed to load comments")
                }
                .collect { comments ->
                    _commentsState.value = CommentsUiState.Success(comments)
                }
        }
    }

    fun postComment(
        eventId: String,
        userId: String,
        userName: String,
        text: String,
        parentId: String? = null
    ) {
        viewModelScope.launch {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return@launch

            val comment = Comment(
                eventId = eventId,
                userId = userId,
                userName = userName,
                text = trimmed,
                parentId = parentId
            )

            val result = repository.postComment(eventId, comment)

            if (result.isSuccess) {
                Log.d("EventViewModel", "Comment added")
            } else {
                Log.e("EventViewModel", "Failed to add comment", result.exceptionOrNull())
            }
        }
    }

    fun startParticipants(eventId: String) {
        viewModelScope.launch {
            repository.getParticipantsCount(eventId)
                .catch { e ->
                    Log.e("EventViewModel", "Error listening participants", e)
                    _participantsCount.value = 0
                }
                .collect { count ->
                    _participantsCount.value = count
                }
        }
    }

    suspend fun getParticipantIds(eventId: String): List<String> {
        val result = repository.getParticipantIds(eventId)
        return if (result.isSuccess) {
            result.getOrNull() ?: emptyList()
        } else {
            Log.e("EventViewModel", "Failed to get participant IDs", result.exceptionOrNull())
            emptyList()
        }
    }

    fun startCheckIns(eventId: String, currentUserId: String?) {
        viewModelScope.launch {
            _arrivedCount.value = 0
            _isCurrentUserCheckedIn.value = false

            repository.getCheckIns(eventId)
                .catch { e ->
                    Log.e("EventViewModel", "Error listening check-ins", e)
                    _arrivedCount.value = 0
                    _isCurrentUserCheckedIn.value = false
                }
                .collect { checkInData ->
                    _arrivedCount.value = checkInData.count

                    if (currentUserId != null) {
                        _isCurrentUserCheckedIn.value = checkInData.userIds.contains(currentUserId)
                    } else {
                        _isCurrentUserCheckedIn.value = false
                    }
                }
        }
    }

    fun manualCheckIn(eventId: String, userId: String) {
        viewModelScope.launch {
            val result = repository.checkIn(eventId, userId)

            if (result.isSuccess) {
                Log.d("EventViewModel", "Manual check-in success")
            } else {
                Log.e("EventViewModel", "Manual check-in failed", result.exceptionOrNull())
            }
        }
    }

    fun cancelManualCheckIn(eventId: String, userId: String) {
        viewModelScope.launch {
            val result = repository.cancelCheckIn(eventId, userId)

            if (result.isSuccess) {
                Log.d("EventViewModel", "Manual check-in cancelled")
            } else {
                Log.e("EventViewModel", "Cancel manual check-in failed", result.exceptionOrNull())
            }
        }
    }

    fun deleteEvent(
        eventId: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _operationState.value = OperationUiState.Loading

            val result = repository.deleteEvent(eventId)

            if (result.isSuccess) {
                Log.d("EventViewModel", "Event deleted: $eventId")
                _operationState.value = OperationUiState.Success("Event deleted")
                onResult(true)
            } else {
                val error = result.exceptionOrNull()
                Log.e("EventViewModel", "Failed to delete event", error)
                _operationState.value =
                    OperationUiState.Error(error?.message ?: "Failed to delete event")
                onResult(false)
            }
        }
    }

    sealed class EventsUiState {
        data object Loading : EventsUiState()
        data class Success(val events: List<Event>) : EventsUiState()
        data class Error(val message: String) : EventsUiState()
    }

    sealed class EventUiState {
        data object Idle : EventUiState()
        data object Loading : EventUiState()
        data class Success(val event: Event) : EventUiState()
        data class Error(val message: String) : EventUiState()
    }

    sealed class OperationUiState {
        data object Idle : OperationUiState()
        data object Loading : OperationUiState()
        data class Success(val message: String) : OperationUiState()
        data class Error(val message: String) : OperationUiState()
    }

    sealed class CommentsUiState {
        data object Loading : CommentsUiState()
        data class Success(val comments: List<Comment>) : CommentsUiState()
        data class Error(val message: String) : CommentsUiState()
    }

    fun deleteComment(eventId: String, commentId: String) {
        viewModelScope.launch {
            val result = repository.deleteComment(eventId, commentId)

            if (result.isSuccess) {
                Log.d("EventViewModel", "Comment deleted successfully")
            } else {
                Log.e("EventViewModel", "Failed to delete comment", result.exceptionOrNull())
            }
        }
    }

    fun banUser(eventId: String, userIdToBan: String) {
        viewModelScope.launch {
            val result = repository.banUser(eventId, userIdToBan)

            if (result.isSuccess) {
                Log.d("EventViewModel", "User $userIdToBan banned from event $eventId")
                loadEvent(eventId)
            } else {
                Log.e("EventViewModel", "Failed to ban user", result.exceptionOrNull())
            }
        }
    }

    fun unbanUser(eventId: String, userIdToUnban: String) {
        viewModelScope.launch {
            val result = repository.unbanUser(eventId, userIdToUnban)
            if (result.isSuccess) {
                Log.d("EventViewModel", "User unbanned")
                loadEvent(eventId)
            }
        }
    }

    fun loadBannedUsersDetails(userIds: List<String>) {
        viewModelScope.launch {
            if (userIds.isEmpty()) {
                _bannedUsersList.value = emptyList()
            } else {
                val userRepo = UserRepositoryImpl()
                val result = userRepo.getUsersByIds(userIds)
                if (result.isSuccess) {
                    _bannedUsersList.value = result.getOrNull() ?: emptyList()
                }
            }
        }
    }
}