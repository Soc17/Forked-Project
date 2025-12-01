package com.example.cmpt362group1.event.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cmpt362group1.auth.AuthViewModel
import com.example.cmpt362group1.database.Comment
import com.example.cmpt362group1.database.Event
import com.example.cmpt362group1.database.EventViewModel
import com.example.cmpt362group1.database.ImageViewModel
import com.example.cmpt362group1.database.User
import com.example.cmpt362group1.database.UserUiState
import com.example.cmpt362group1.database.UserViewModel
import com.example.cmpt362group1.navigation.explore.weather.WeatherRepository
import com.example.cmpt362group1.navigation.explore.weather.WeatherResult
import com.example.cmpt362group1.ui.dialogs.UserListDialog
import com.example.cmpt362group1.Route
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.navigation.NavHostController

private val MonoBlack = Color(0xFF121212)
private val MonoDarkGray = Color(0xFF424242)
private val MonoLightGray = Color(0xFFE0E0E0)
private val MonoCardGray = Color(0xFFF0F0F0)
private val LightRed = Color(0xFFE57373)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    onEditEvent: (String) -> Unit = {},
    allowEditDelete: Boolean = true,
    navController: NavHostController? = null,
    eventViewModel: EventViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    imageViewModel: ImageViewModel = viewModel(),  // delete event
) {
    val eventState by eventViewModel.eventState.collectAsState()
    val userState by userViewModel.userState.collectAsState()
    val commentsState by eventViewModel.commentsState.collectAsState()
    val participantsCount by eventViewModel.participantsCount.collectAsState()
    val arrivedCount by eventViewModel.arrivedCount.collectAsState()
    val isCurrentUserCheckedIn by eventViewModel.isCurrentUserCheckedIn.collectAsState()
    val bannedUsersList by eventViewModel.bannedUsersList.collectAsState()

    LaunchedEffect(eventId) {
        eventViewModel.loadEvent(eventId)
        eventViewModel.startComments(eventId)
        eventViewModel.startParticipants(eventId)
    }

    var currentUserId by remember { mutableStateOf<String?>(null) }
    var eventImages by remember { mutableStateOf<ArrayList<String>>(ArrayList()) }  // for deletion

    LaunchedEffect(Unit) {
        val uid = authViewModel.getUserId()
        currentUserId = uid
        if (uid != null) {
            userViewModel.loadUser(uid)
        }
    }

    LaunchedEffect(eventId, currentUserId) {
        eventViewModel.startCheckIns(eventId, currentUserId)
    }

    var isJoined by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userState, eventId) {
        val uid = authViewModel.getUserId()
        if (uid != null && userState is UserUiState.Success) {
            val user = (userState as UserUiState.Success).user
            isJoined = user.eventsJoined.contains(eventId)
            currentUserName = user.displayName.ifBlank { null }
        }
    }

    val comments: List<Comment> = when (commentsState) {
        is EventViewModel.CommentsUiState.Success -> (
                commentsState as EventViewModel.CommentsUiState.Success).comments
        else -> emptyList()
    }
    val commentsLoading = commentsState is EventViewModel.CommentsUiState.Loading
    val commentsError = (commentsState as? EventViewModel.CommentsUiState.Error)?.message

    var commentText by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<Comment?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var eventIdPendingDelete by remember { mutableStateOf<String?>(null) }
    var showBannedListDialog by remember { mutableStateOf(false) }
    var showParticipantsDialog by remember { mutableStateOf(false) }
    var participantsList by remember { mutableStateOf<List<User>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val currentUser = if (userState is UserUiState.Success) {
        (userState as UserUiState.Success).user
    } else null

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Event Detail",
                    color = MonoBlack,
                    fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = MonoBlack)
                    }
                },
                actions = {
                    val event = (eventState as? EventViewModel.EventUiState.Success)?.event
                    val isHost = currentUserId != null && event?.createdBy == currentUserId
                    if (isHost) {
                        IconButton(onClick = {
                            showBannedListDialog = true
                            event?.let { eventViewModel.loadBannedUsersDetails(it.bannedUserIds) }
                        }) {
                            Icon(Icons.Default.Settings,
                                "Manage Blocked",
                                tint = MonoBlack)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        when (eventState) {
            is EventViewModel.EventUiState.Idle,
            is EventViewModel.EventUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MonoBlack)
                }
            }
            is EventViewModel.EventUiState.Error -> {
                val message = (eventState as EventViewModel.EventUiState.Error).message
                Box(Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center) {
                    Text(message, color = MonoBlack)
                }
            }
            is EventViewModel.EventUiState.Success -> {
                val event = (eventState as EventViewModel.EventUiState.Success).event
                val isHost = currentUserId != null && event.createdBy == currentUserId
                val listState = rememberLazyListState()
                val isBanned = currentUserId != null && event.bannedUserIds.contains(currentUserId)
                eventImages = event.imageUrls

                LaunchedEffect(event.bannedUserIds) {
                    if (showBannedListDialog) {
                        eventViewModel.loadBannedUsersDetails(event.bannedUserIds)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        EventDetailScrollableContent(
                            event = event,
                            isHost = isHost,
                            allowEditDelete = allowEditDelete,
                            isJoined = isJoined,
                            onToggleJoin = {
                                val uid = authViewModel.getUserId()
                                if (uid != null) {
                                    if (isJoined) userViewModel.removeJoinedEvent(
                                        uid,
                                        event.id)
                                    else userViewModel.addJoinedEvent(
                                        uid,
                                        event.id)
                                    isJoined = !isJoined
                                }
                            },
                            comments = comments,
                            commentsLoading = commentsLoading,
                            commentsError = commentsError,
                            participantsCount = participantsCount,
                            arrivedCount = arrivedCount,
                            isCurrentUserCheckedIn = isCurrentUserCheckedIn,
                            onToggleManualCheckIn = { currentlyCheckedIn ->
                                val uid = authViewModel.getUserId()
                                if (uid != null) {
                                    if (currentlyCheckedIn) eventViewModel.cancelManualCheckIn(
                                        event.id,
                                        uid)
                                    else eventViewModel.manualCheckIn(
                                        event.id,
                                        uid)
                                }
                            },
                            onReplyClick = { replyTo = it },
                            onDeleteComment = { eventViewModel.deleteComment(
                                event.id,
                                it) },
                            onBanUser = { uid -> eventViewModel.banUser(
                                event.id,
                                uid) },
                            onUnbanUser = { uid -> eventViewModel.unbanUser(
                                event.id,
                                uid) },
                            bannedUserIds = event.bannedUserIds,
                            listState = listState,
                            onEditEvent = { onEditEvent(event.id) },
                            onDeleteEvent = {
                                eventIdPendingDelete = event.id
                                showDeleteConfirm = true
                            },
                            onParticipantsClick = {
                                scope.launch {
                                    val participantIds = eventViewModel.getParticipantIds(event.id)
                                    participantsList = userViewModel.getUsersByIds(participantIds)
                                    showParticipantsDialog = true
                                }
                            }
                        )

                        val showScrollHint by remember { derivedStateOf {
                            listState.canScrollForward } }
                        this@Column.AnimatedVisibility(
                            visible = showScrollHint,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.95f))
                                        )
                                    ),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                            }
                        }
                    }

                    if (replyTo != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Replying to ${replyTo!!.userName.ifBlank { "Anon" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MonoDarkGray
                            )
                            TextButton(onClick = { replyTo = null }) {
                                Text("Cancel", color = MonoBlack)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    CommentInputBar(
                        commentText = commentText,
                        onCommentTextChange = { commentText = it },
                        enabled = currentUserName != null && commentText.isNotBlank() && !isBanned,
                        isBanned = isBanned,
                        onSend = {
                            val text = commentText.trim()
                            if (text.isNotEmpty()) {
                                val uid = authViewModel.getUserId()
                                if (uid != null) {
                                    val name = currentUserName ?: "Anonymous"
                                    eventViewModel.postComment(event.id, uid, name, text, replyTo?.id)
                                }
                                commentText = ""
                                replyTo = null
                            }
                        }
                    )
                }

                if (showBannedListDialog) {
                    BlockedUsersDialog(bannedUsersList, { showBannedListDialog = false }, { eventViewModel.unbanUser(event.id, it) })
                }

                if (showParticipantsDialog && navController != null) {
                    currentUserId?.let { currentUid ->
                        UserListDialog(
                            title = "Participants",
                            users = participantsList,
                            currentUserId = currentUid,
                            onDismiss = { showParticipantsDialog = false },
                            onUserClick = { clickedUserId ->
                                showParticipantsDialog = false
                                if (clickedUserId != currentUid) {
                                    navController.navigate("${Route.ViewUserProfile.route}/$clickedUserId")
                                }
                            },
                            onFollowClick = { userToFollowId ->
                                userViewModel.followUser(currentUid, userToFollowId)
                            },
                            onUnfollowClick = { userToUnfollowId ->
                                userViewModel.unfollowUser(currentUid, userToUnfollowId)
                            },
                            isFollowingUser = { checkUserId ->
                                currentUser?.followingList?.contains(checkUserId) ?: false
                            },
                            showFollowButton = true
                        )
                    }
                }
            }
        }

        if (showDeleteConfirm && eventIdPendingDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = Color.White,
                title = { Text("Delete event?", color = MonoBlack) },
                text = { Text("Cannot be undone.", color = MonoDarkGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = eventIdPendingDelete!!
                            showDeleteConfirm = false
                            eventViewModel.deleteEvent(id) { success ->
                                if (success) {
                                    userViewModel.removeCreatedEvent(currentUserId!!, id)
                                    userViewModel.removeJoinedEvent(currentUserId!!, id)
                                    imageViewModel.deleteImages(eventImages)
                                    onNavigateBack()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MonoBlack)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = MonoBlack) }
                }
            )
        }
    }
}

@Composable
private fun EventImageCarousel(
    legacyImageUrl: String,
    imageUrls: List<String>,
    modifier: Modifier = Modifier
) {
    val displayImages = remember(legacyImageUrl, imageUrls) {
        if (imageUrls.isNotEmpty()) imageUrls
        else if (legacyImageUrl.isNotBlank()) listOf(legacyImageUrl)
        else emptyList()
    }

    if (displayImages.isNotEmpty()) {
        val pageCount = displayImages.size
        val infiniteLoop = pageCount > 1

        val initialPage = if (infiniteLoop) (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % pageCount)
        else 0
        val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { if (infiniteLoop)
            Int.MAX_VALUE else pageCount })

        val coroutineScope = rememberCoroutineScope()
        val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

        if (infiniteLoop) {
            LaunchedEffect(pagerState, isDragged) {
                if (!isDragged) {
                    while (true) {
                        delay(4000L)
                        pagerState.animateScrollToPage(
                            pagerState.currentPage + 1,
                            animationSpec = tween(durationMillis = 1000,
                                easing = FastOutSlowInEasing)
                        )
                    }
                }
            }
        }

        Column(modifier = modifier) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val actualIndex = page % pageCount
                    EventHeaderImagePlaceholder(displayImages[actualIndex],
                        Modifier.fillMaxSize())
                }

                if (infiniteLoop) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Filled.KeyboardArrowLeft, "Prev",
                            tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Filled.KeyboardArrowRight, "Next",
                            tint = Color.White)
                    }
                }
            }

            if (infiniteLoop) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentRealPage = pagerState.currentPage % pageCount
                    repeat(pageCount) { iteration ->
                        val color = if (currentRealPage == iteration) MonoBlack else Color.LightGray
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MonoLightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("No Image Available", color = Color.Gray,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EventDetailScrollableContent(
    event: Event,
    isHost: Boolean,
    allowEditDelete: Boolean,
    isJoined: Boolean,
    onToggleJoin: () -> Unit,
    comments: List<Comment>,
    commentsLoading: Boolean,
    commentsError: String?,
    participantsCount: Int,
    arrivedCount: Int,
    isCurrentUserCheckedIn: Boolean,
    onToggleManualCheckIn: (Boolean) -> Unit,
    onReplyClick: (Comment) -> Unit,
    onDeleteComment: (String) -> Unit,
    onBanUser: (String) -> Unit,
    onUnbanUser: (String) -> Unit,
    bannedUserIds: List<String>,
    listState: LazyListState,
    onEditEvent: () -> Unit,
    onDeleteEvent: () -> Unit,
    onParticipantsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        item {
            Text(event.title, style = MaterialTheme.typography.headlineMedium,
                color = MonoBlack, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            EventImageCarousel(event.imageUrl, event.imageUrls,
                Modifier.fillMaxWidth().height(240.dp))
            Spacer(Modifier.height(16.dp))

            if (isHost && allowEditDelete) {
                HostSummaryPanel(participantsCount, arrivedCount, onEditEvent,
                    onDeleteEvent)
            }

            Text("Participants: $participantsCount",
                style = MaterialTheme.typography.bodyLarge,
                color = MonoBlack,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onParticipantsClick() })

            Spacer(Modifier.height(8.dp))
            Text(event.location,
                style = MaterialTheme.typography.bodyMedium,
                color = MonoDarkGray)
            Text("${event.startDate} ${event.startTime}  -  ${event.endDate} ${event.endTime}",
                style = MaterialTheme.typography.bodyMedium, color = MonoDarkGray)

            if (event.description.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("Description", style = MaterialTheme.typography.titleMedium,
                    color = MonoBlack, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(event.description, style = MaterialTheme.typography.bodyMedium,
                    color = MonoBlack)
            }

            if (event.dressCode.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("Dress Code: ${event.dressCode}",
                    style = MaterialTheme.typography.bodyMedium, color = MonoBlack)
            }

            Spacer(Modifier.height(20.dp))
            event.latitude?.let { lat -> event.longitude?.let {
                lon -> WeatherInfoPanel(lat, lon, event.startDate, event.startTime) } }
            Spacer(Modifier.height(20.dp))

            if (!isHost) {
                Button(
                    onClick = onToggleJoin,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MonoBlack,
                        contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isJoined) "Remove from Planner" else "Add to Planner",
                        fontWeight = FontWeight.Bold)
                }
            }

            if (isJoined) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onToggleManualCheckIn(isCurrentUserCheckedIn) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MonoBlack),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MonoBlack)
                ) {
                    Text(if (isCurrentUserCheckedIn) "Cancel Check-in"
                    else "Manual Check-in")
                }
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MonoLightGray)
            Spacer(Modifier.height(12.dp))
            Text("Comments", style = MaterialTheme.typography.titleMedium,
                color = MonoBlack, fontWeight = FontWeight.Bold)
        }

        when {
            commentsLoading -> { item { Text("Loading...", color = MonoDarkGray) } }
            commentsError != null -> { item { Text(commentsError, color = Color.Red) } }
            comments.isEmpty() -> { item { Text("No comments yet.", color = MonoDarkGray) } }
            else -> {
                val commentMap = comments.associateBy { it.id }
                itemsIndexed(comments, key = { _, c -> c.id }) { index, c ->
                    val parent = c.parentId?.let { commentMap[it] }
                    val isHostComment = c.userId == event.createdBy
                    val isUserBanned = bannedUserIds.contains(c.userId)

                    CommentRow(
                        comment = c,
                        parent = parent,
                        isHostComment = isHostComment,
                        isCurrentUserHost = isHost,
                        isUserBanned = isUserBanned,
                        onReplyClick = onReplyClick,
                        onDeleteClick = { onDeleteComment(c.id) },
                        onBanUser = { onBanUser(c.userId) },
                        onUnbanUser = { onUnbanUser(c.userId) }
                    )

                    if (index < comments.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp),
                            thickness = 0.5.dp, color = MonoLightGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: Comment,
    parent: Comment?,
    isHostComment: Boolean,
    isCurrentUserHost: Boolean,
    isUserBanned: Boolean,
    onReplyClick: (Comment) -> Unit,
    onDeleteClick: () -> Unit,
    onBanUser: () -> Unit,
    onUnbanUser: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        parent?.let {
            Row(modifier = Modifier.padding(bottom = 6.dp)) {
                Box(modifier = Modifier.width(2.dp).height(20.dp).background(Color.Gray))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(it.userName.ifBlank { "Anon" },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray)
                    Text(it.text, maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray)
                }
            }
        }

        val nameText = comment.userName.ifBlank { "Anonymous" }
        val displayName = if (isHostComment) "$nameText (Host)" else nameText

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isHostComment) MonoBlack else MonoDarkGray,
                    fontWeight = if (isHostComment) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(Modifier.height(2.dp))
                Text(comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MonoBlack)
            }

            if (isCurrentUserHost) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .align(Alignment.CenterVertically)
                        .clickable { onDeleteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MonoDarkGray
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Reply",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.clickable { onReplyClick(comment) }
            )

            if (isCurrentUserHost && !isHostComment) {
                Spacer(Modifier.width(16.dp))
                if (isUserBanned) {
                    Text("Unblock",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightRed,
                        modifier = Modifier.clickable { onUnbanUser() })
                } else {
                    Text("Block",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightRed,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.clickable { onBanUser() })
                }
            }
        }
    }
}

@Composable
fun BlockedUsersDialog(blockedUsers: List<User>, onDismiss: () -> Unit, onUnblock: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Blocked Users",
                        style = MaterialTheme.typography.titleMedium,
                        color = MonoBlack)
                    IconButton(onClick = onDismiss) { Icon(
                        Icons.Default.Close,
                        "Close", tint = MonoBlack) }
                }
                HorizontalDivider()
                if (blockedUsers.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center) {
                        Text("No blocked users.", color = MonoDarkGray)
                    }
                } else {
                    LazyColumn(Modifier.padding(top = 8.dp)) {
                        itemsIndexed(blockedUsers) { _, user ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(user.displayName.ifBlank {
                                    "User ${user.id.take(4)}" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MonoBlack)
                                OutlinedButton(
                                    onClick = { onUnblock(user.id) },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MonoLightGray)
                                ) { Text("Unblock", fontSize = 12.sp, color = MonoDarkGray) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HostSummaryPanel(participantsCount: Int, arrivedCount: Int, onEditClick: () ->
Unit, onDeleteClick: () -> Unit) {
    val notArrived = (participantsCount - arrivedCount).coerceAtLeast(0)
    Surface(shape = RoundedCornerShape(12.dp), color = MonoCardGray,
        modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Host View", style = MaterialTheme.typography.titleMedium, color = MonoBlack)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HostStat("Total", participantsCount.toString(), MonoBlack)
                HostStat("Arrived", arrivedCount.toString(), MonoBlack)
                HostStat("Pending", notArrived.toString(), MonoDarkGray)
            }
            HorizontalDivider(color = Color.LightGray)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onEditClick,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Edit Event", color = MonoBlack, fontWeight = FontWeight.Normal)
                }
                TextButton(
                    onClick = onDeleteClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Delete Event", color = LightRed, fontWeight = FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun HostStat(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor)
    }
}

@Composable
private fun CommentInputBar(commentText: String,
                            onCommentTextChange: (String) -> Unit,
                            enabled: Boolean, isBanned: Boolean,
                            onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, MonoLightGray, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = commentText,
            onValueChange = onCommentTextChange,
            enabled = !isBanned,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = MonoBlack
            ),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp)
                .padding(start = 12.dp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (commentText.isEmpty()) {
                        Text(
                            text = if (isBanned) "Restricted." else "Comment...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = enabled,
            modifier = Modifier
                .background(if (enabled) MonoBlack else Color.LightGray, CircleShape)
                .size(32.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Send",
                tint = Color.White,
                modifier = Modifier
                    .graphicsLayer { rotationZ = 90f }
                    .size(16.dp)
            )
        }
    }
}

@Composable
fun WeatherInfoPanel(lat: Double, lon: Double, date: String, time: String) {
    var weatherData by remember { mutableStateOf<WeatherResult?>(null) }
    var weatherError by remember { mutableStateOf<String?>(null) }
    val repository = remember { WeatherRepository() }

    LaunchedEffect(Unit) {
        try {
            val inputFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")
            val eventDateTime = LocalDateTime.parse("$date $time", inputFormatter)

            repository.getWeatherForDateTime(
                lat,
                lon,
                eventDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00")),
                onSuccess = { weatherData = it },
                onError = { weatherError = it }
            )
        } catch (e: Exception) {
            weatherError = "Check back closer to date."
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Expected Weather",
            style = MaterialTheme.typography.titleMedium,
            color = MonoBlack,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        when {
            weatherData != null -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${weatherData!!.temperature.toInt()}°C",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MonoBlack,
                        fontWeight = FontWeight.Bold
                    )

                    val symbol = com.example.cmpt362group1.navigation.explore.weather.WeatherHelper
                        .getWeatherSymbol(weatherData!!.condition.lowercase())
                    Text(text = symbol, fontSize = 28.sp)

                    Text(
                        text = weatherData!!.condition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MonoDarkGray
                    )
                }
            }
            weatherError != null -> {
                Text(
                    text = "Weather info unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MonoDarkGray
                )
                Text(
                    text = weatherError ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MonoBlack
                    )
                    Text(
                        text = "Loading forecast...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MonoDarkGray
                    )
                }
            }
        }
    }
}

@Composable
private fun EventHeaderImagePlaceholder(url: String, modifier: Modifier) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context).data(url).crossfade(true).build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}