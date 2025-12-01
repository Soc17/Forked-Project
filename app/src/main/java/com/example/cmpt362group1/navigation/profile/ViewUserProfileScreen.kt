package com.example.cmpt362group1.navigation.profile

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cmpt362group1.Route
import com.example.cmpt362group1.auth.AuthViewModel
import com.example.cmpt362group1.database.*
import com.example.cmpt362group1.ui.dialogs.UserListDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewUserProfileScreen(
    userId: String,
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    eventViewModel: EventViewModel = viewModel(),
) {
    val currentUserId = remember { authViewModel.getUserId() }
    val scope = rememberCoroutineScope()

    // Load the viewed user
    LaunchedEffect(userId) {
        userViewModel.loadUser(userId)
    }

    // Load current user to check following status
    var currentUser by remember { mutableStateOf<User?>(null) }
    LaunchedEffect(currentUserId) {
        currentUserId?.let { uid ->
            scope.launch {
                userViewModel.repository.getUser(uid).collect { user ->
                    currentUser = user
                }
            }
        }
    }

    val userState by userViewModel.userState.collectAsState()

    when (val state = userState) {
        is UserUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        is UserUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${state.message}")
            }
            return
        }
        UserUiState.Idle -> {
            return
        }
        is UserUiState.Success -> {}
    }

    val viewedUser = (userState as UserUiState.Success).user
    val isFollowing = currentUser?.followingList?.contains(userId) ?: false

    // Dialog states
    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }
    var followersList by remember { mutableStateOf<List<User>>(emptyList()) }
    var followingList by remember { mutableStateOf<List<User>>(emptyList()) }

    // Load events
    val eventsState by eventViewModel.eventsState.collectAsState()
    val userEvents = remember(eventsState, viewedUser.eventsJoined) {
        when (eventsState) {
            is EventViewModel.EventsUiState.Success -> {
                val allEvents = (eventsState as EventViewModel.EventsUiState.Success).events
                allEvents
                    .filter { event -> viewedUser.eventsJoined.contains(event.id) }
                    .sortedWith(compareByDescending<Event> { it.startDate }
                        .thenByDescending { it.startTime })
            }
            else -> emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = viewedUser.username.ifEmpty { "username" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(viewedUser.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.LightGray, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        StatBlock(label = "Events", value = viewedUser.eventsJoined.size)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    followersList = userViewModel.getUsersByIds(viewedUser.followersList)
                                    showFollowersDialog = true
                                }
                            }
                        ) {
                            Text(
                                text = viewedUser.followers.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Followers",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    followingList = userViewModel.getUsersByIds(viewedUser.followingList)
                                    showFollowingDialog = true
                                }
                            }
                        ) {
                            Text(
                                text = viewedUser.following.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Following",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewedUser.displayName.ifEmpty {
                                if (viewedUser.firstName.isNotEmpty() || viewedUser.lastName.isNotEmpty()) {
                                    "${viewedUser.firstName} ${viewedUser.lastName}".trim()
                                } else {
                                    "Name"
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        if (viewedUser.pronouns.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = viewedUser.pronouns,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            item {
                if (viewedUser.description.isNotEmpty()) {
                    Text(
                        text = viewedUser.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }

            item {
                val context = LocalContext.current
                if (viewedUser.link.isNotEmpty()) {
                    Text(
                        text = viewedUser.link,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    if (!viewedUser.link.startsWith("http://") &&
                                        !viewedUser.link.startsWith("https://")
                                    ) {
                                        "https://${viewedUser.link}"
                                    } else {
                                        viewedUser.link
                                    }.toUri()
                                )
                                context.startActivity(intent)
                            }
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Follow/Unfollow button
            item {
                currentUserId?.let { currentUid ->
                    if (currentUid != userId) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isFollowing) {
                                OutlinedButton(
                                    onClick = {
                                        userViewModel.unfollowUser(currentUid, userId)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Unfollow", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        userViewModel.followUser(currentUid, userId)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Follow", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Events",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }

            // Events grid
            item {
                if (userEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No events yet!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.height((userEvents.size / 3 + 1) * 130.dp),
                        userScrollEnabled = false
                    ) {
                        items(userEvents) { event ->
                            ProfileEventGridItem(
                                event = event,
                                onClick = {
                                    navController.navigate("${Route.EventDetail.route}/${event.id}?readonly=true")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showFollowersDialog) {
        currentUserId?.let { currentUid ->
            UserListDialog(
                title = "Followers",
                users = followersList,
                currentUserId = currentUid,
                onDismiss = { showFollowersDialog = false },
                onUserClick = { clickedUserId ->
                    showFollowersDialog = false
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

    if (showFollowingDialog) {
        currentUserId?.let { currentUid ->
            UserListDialog(
                title = "Following",
                users = followingList,
                currentUserId = currentUid,
                onDismiss = { showFollowingDialog = false },
                onUserClick = { clickedUserId ->
                    showFollowingDialog = false
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
