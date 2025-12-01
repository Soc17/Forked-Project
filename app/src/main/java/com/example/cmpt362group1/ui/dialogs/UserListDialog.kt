package com.example.cmpt362group1.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.cmpt362group1.database.User
import com.example.cmpt362group1.ui.theme.MonoBlack
import com.example.cmpt362group1.ui.theme.MonoDarkGray
import com.example.cmpt362group1.ui.theme.MonoLightGray
import com.example.cmpt362group1.ui.theme.PrimaryBlue

@Composable
fun UserListDialog(
    title: String,
    users: List<User>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit,
    onFollowClick: ((String) -> Unit)? = null,
    onUnfollowClick: ((String) -> Unit)? = null,
    isFollowingUser: (String) -> Boolean = { false },
    showFollowButton: Boolean = true
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MonoBlack
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MonoBlack
                        )
                    }
                }
                HorizontalDivider()

                // Content
                if (users.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No users found.",
                            color = MonoDarkGray
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                        itemsIndexed(users) { _, user ->
                            UserListItem(
                                user = user,
                                currentUserId = currentUserId,
                                onUserClick = { onUserClick(user.id) },
                                onFollowClick = if (onFollowClick != null) {
                                    { onFollowClick(user.id) }
                                } else null,
                                onUnfollowClick = if (onUnfollowClick != null) {
                                    { onUnfollowClick(user.id) }
                                } else null,
                                isFollowing = isFollowingUser(user.id),
                                showFollowButton = showFollowButton && user.id != currentUserId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(
    user: User,
    currentUserId: String,
    onUserClick: () -> Unit,
    onFollowClick: (() -> Unit)?,
    onUnfollowClick: (() -> Unit)?,
    isFollowing: Boolean,
    showFollowButton: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Profile photo
            AsyncImage(
                model = user.photoUrl.ifBlank { "https://via.placeholder.com/40" },
                contentDescription = "Profile photo",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = user.displayName.ifBlank { "User ${user.id.take(8)}" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MonoBlack
                )
                if (user.username.isNotBlank()) {
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MonoDarkGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Follow/Unfollow button
        if (showFollowButton) {
            if (isFollowing) {
                OutlinedButton(
                    onClick = { onUnfollowClick?.invoke() },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    border = BorderStroke(1.dp, MonoLightGray)
                ) {
                    Text(
                        text = "Unfollow",
                        fontSize = 12.sp,
                        color = MonoDarkGray
                    )
                }
            } else {
                Button(
                    onClick = { onFollowClick?.invoke() },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    )
                ) {
                    Text(
                        text = "Follow",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
