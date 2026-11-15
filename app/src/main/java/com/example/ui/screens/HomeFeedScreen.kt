package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Conversation
import com.example.data.model.FollowRelation
import com.example.data.model.Post
import com.example.data.model.ProfileNotification
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.model.VibeProfile
import com.example.ui.components.MediaContentView
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.VibeEmptyState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.EmeraldCloseFriend
import com.example.ui.theme.HotMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SolarGold
import com.example.ui.theme.cardThemeForId
import com.example.ui.viewmodel.VibeMainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    viewModel: VibeMainViewModel,
    activeProfile: VibeProfile,
    onOpenSwitcher: () -> Unit,
    onOpenCreateStory: () -> Unit,
    onOpenCreatePost: () -> Unit,
    onOpenReels: () -> Unit,
    onOpenAudioRooms: () -> Unit,
    onOpenConversation: (String) -> Unit
) {
    val context = LocalContext.current
    val feedState by viewModel.feedPosts.collectAsStateWithLifecycle()
    val storiesState by viewModel.activeStories.collectAsStateWithLifecycle()
    val discoverState by viewModel.discoverProfiles.collectAsStateWithLifecycle()
    val followingRelations by viewModel.followingRelations.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notifications.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversations.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("For You") }
    var viewingStories by remember { mutableStateOf<List<Story>?>(null) }
    var commentsPost by remember { mutableStateOf<Post?>(null) }
    var sharePost by remember { mutableStateOf<Post?>(null) }
    var showNotifications by remember { mutableStateOf(false) }

    val followingIds = remember(followingRelations) { followingRelations.map { it.targetProfileId }.toSet() }
    val closeFriendIds = remember(followingRelations) {
        followingRelations.filter { it.isCloseFriend }.map { it.targetProfileId }.toSet()
    }
    val unreadNotifs = (notificationsState as? UiState.Success)?.data?.count { !it.isRead } ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Brand & Identity Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "VIBE",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.width(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .testTag("home_profile_switcher_pill")
                        .clickable { onOpenSwitcher() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@${activeProfile.username}",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonViolet,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Switch VIBE profile",
                            tint = NeonViolet,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = ElectricPurple.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .testTag("home_audio_rooms_button")
                        .clickable { onOpenAudioRooms() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Live Audio Rooms", tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Live", style = MaterialTheme.typography.labelLarge, color = CyberCyan)
                    }
                }
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = { showNotifications = true },
                    modifier = Modifier.testTag("home_notifications_button")
                ) {
                    Box {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        if (unreadNotifs > 0) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(HotMagenta)
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 24-Hour Disappearing Stories Row
            item {
                val stories = (storiesState as? UiState.Success)?.data.orEmpty()
                val grouped = remember(stories) { stories.groupBy { it.profileId } }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .testTag("add_story_button")
                                .clickable { onOpenCreateStory() }
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                ProfileAvatar(
                                    avatarImage = activeProfile.avatarImage,
                                    avatarEmoji = activeProfile.avatarEmoji,
                                    cardTheme = activeProfile.cardTheme,
                                    size = 64.dp,
                                    hasStoryRing = grouped.containsKey(activeProfile.id)
                                )
                                Surface(
                                    color = ElectricPurple,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Story", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Your Story", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }

                    items(grouped.entries.toList(), key = { it.key }) { (profileId, userStories) ->
                        val first = userStories.first()
                        val isCF = userStories.any { it.audience == "close_friends" }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                viewingStories = userStories
                                viewModel.recordStoryView(first)
                            }
                        ) {
                            ProfileAvatar(
                                avatarImage = first.authorAvatar,
                                avatarEmoji = "⚡",
                                cardTheme = "electric_purple",
                                size = 64.dp,
                                hasStoryRing = true,
                                isCloseFriendRing = isCF
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (profileId == activeProfile.id) "You" else first.authorUsername,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(66.dp)
                            )
                        }
                    }
                }
            }

            // Friend Activity & Mood Strip
            item {
                val friends = (discoverState as? UiState.Success)?.data.orEmpty()
                    .filter { it.id != activeProfile.id && !it.anonymousDiscovery }
                    .take(10)
                if (friends.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(friends, key = { it.id }) { friend ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable {
                                    viewModel.startDirectChat(friend, onOpenConversation)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(CyberCyan)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "@${friend.username}: ${friend.moodEmoji} ${friend.moodStatus}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Feed Filter Chips + Reels Entry
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("For You", "Following", "Close Friends").forEach { tab ->
                        FilterChip(
                            selected = selectedFilter == tab,
                            onClick = { selectedFilter = tab },
                            label = { Text(tab) }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = HotMagenta.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .testTag("open_reels_button")
                            .clickable { onOpenReels() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "Watch Reels", tint = HotMagenta, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reels", style = MaterialTheme.typography.labelLarge, color = HotMagenta)
                        }
                    }
                }
            }

            // Feed Posts
            when (val state = feedState) {
                is UiState.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        VibeEmptyState(
                            icon = Icons.Default.LocalFireDepartment,
                            title = "Could not load feed",
                            subtitle = state.message
                        )
                    }
                }
                is UiState.Success -> {
                    val filteredPosts = state.data.filter { post ->
                        when (selectedFilter) {
                            "Following" -> post.profileId == activeProfile.id || post.profileId in followingIds
                            "Close Friends" -> post.visibility == "close_friends" && (post.profileId == activeProfile.id || post.profileId in closeFriendIds)
                            else -> post.visibility == "public" || post.profileId == activeProfile.id || post.profileId in closeFriendIds
                        }
                    }
                    if (filteredPosts.isEmpty()) {
                        item {
                            VibeEmptyState(
                                icon = Icons.Default.LocalFireDepartment,
                                title = "Your VIBE Feed Awaits",
                                subtitle = "Drop your first photo, short reel, or vibe update to light up the feed.",
                                actionLabel = "Create Post",
                                onAction = onOpenCreatePost
                            )
                        }
                    } else {
                        items(filteredPosts, key = { it.id }) { post ->
                            FeedPostCard(
                                viewModel = viewModel,
                                post = post,
                                activeProfile = activeProfile,
                                onCommentClick = { commentsPost = post },
                                onShareClick = { sharePost = post },
                                onDeleteClick = if (post.profileId == activeProfile.id) {
                                    { viewModel.deletePost(post) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }

    // Story Viewer Dialog
    viewingStories?.let { storyList ->
        StoryViewerDialog(
            stories = storyList,
            onDismiss = { viewingStories = null },
            onReply = { story ->
                viewingStories = null
                val target = (discoverState as? UiState.Success)?.data?.firstOrNull { it.id == story.profileId }
                if (target != null) {
                    viewModel.startDirectChat(target, onOpenConversation)
                }
            }
        )
    }

    // Comments Bottom Sheet
    commentsPost?.let { post ->
        CommentsBottomSheet(
            viewModel = viewModel,
            post = post,
            onDismiss = { commentsPost = null }
        )
    }

    // Share Post Sheet
    sharePost?.let { post ->
        val convs = (conversationsState as? UiState.Success)?.data.orEmpty()
        SharePostBottomSheet(
            post = post,
            conversations = convs,
            onSendToChat = { conv ->
                viewModel.sharePostToChat(post, conv)
                sharePost = null
            },
            onExternalShare = {
                viewModel.recordExternalShare(post.id)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, "@${post.authorUsername} on VIBE: ${post.caption}")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share VIBE post"))
                sharePost = null
            },
            onDismiss = { sharePost = null }
        )
    }

    // Notifications Sheet (Isolated per active profile)
    if (showNotifications) {
        val list = (notificationsState as? UiState.Success)?.data.orEmpty()
        NotificationsBottomSheet(
            profileUsername = activeProfile.username,
            notifications = list,
            onMarkRead = { viewModel.markNotificationRead(it) },
            onDismiss = { showNotifications = false }
        )
    }
}

@Composable
fun FeedPostCard(
    viewModel: VibeMainViewModel,
    post: Post,
    activeProfile: VibeProfile,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: (() -> Unit)?
) {
    val likes by viewModel.observePostLikes(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val isLiked = remember(likes, activeProfile.id) { likes.any { it.profileId == activeProfile.id } }
    var showBurstHeart by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (showBurstHeart) 1.3f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "heart_burst"
    )
    LaunchedEffect(showBurstHeart) {
        if (showBurstHeart) {
            delay(650)
            showBurstHeart = false
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(
                        avatarImage = post.authorAvatar,
                        avatarEmoji = "⚡",
                        cardTheme = post.authorCardTheme,
                        size = 42.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (post.visibility == "close_friends") {
                                Spacer(Modifier.width(6.dp))
                                Surface(color = EmeraldCloseFriend.copy(alpha = 0.2f), shape = RoundedCornerShape(50)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = EmeraldCloseFriend, modifier = Modifier.size(11.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Close Friends", style = MaterialTheme.typography.labelSmall, color = EmeraldCloseFriend)
                                    }
                                }
                            }
                        }
                        Text(
                            text = "@${post.authorUsername}" + if (post.isReel) " • Reel" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete post",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (post.mediaData.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .pointerInput(post.id, isLiked) {
                            detectTapGestures(
                                onDoubleTap = {
                                    showBurstHeart = true
                                    if (!isLiked) viewModel.toggleLike(post)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    MediaContentView(
                        mediaData = post.mediaData,
                        contentDescription = post.caption,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (post.isReel) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("REEL", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = showBurstHeart) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = HotMagenta,
                            modifier = Modifier
                                .size(76.dp)
                                .scale(heartScale)
                        )
                    }
                }
            }

            if (post.caption.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (post.hashtags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.hashtags.take(4).forEach { tag ->
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonViolet
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.toggleLike(post) }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like post",
                            tint = if (isLiked) HotMagenta else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("${post.likesCount}", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(12.dp))
                    IconButton(onClick = onCommentClick) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("${post.commentsCount}", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(12.dp))
                    IconButton(onClick = onShareClick) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share post",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("${post.sharesCount}", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun StoryViewerDialog(
    stories: List<Story>,
    onDismiss: () -> Unit,
    onReply: (Story) -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    val current = stories.getOrNull(index) ?: return
    val theme = cardThemeForId(current.bgTheme)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(theme.primary, Color(0xFF07060B))))
        ) {
            if (current.mediaData.isNotBlank()) {
                MediaContentView(
                    mediaData = current.mediaData,
                    contentDescription = current.caption,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }

            // Left/Right tap navigation
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { if (index > 0) index-- else onDismiss() }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { if (index < stories.lastIndex) index++ else onDismiss() }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stories.indices.forEach { i ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (i <= index) Color.White else Color.White.copy(alpha = 0.35f))
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatar(
                                avatarImage = current.authorAvatar,
                                avatarEmoji = "⚡",
                                cardTheme = current.bgTheme,
                                size = 40.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "@${current.authorUsername}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (current.audience == "close_friends") "Close Friends • 24h" else "24h Story • ${current.viewsCount} views",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close story", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (current.caption.isNotBlank()) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = current.caption,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    Button(
                        onClick = { onReply(current) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reply in VIBE Chat")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsBottomSheet(
    viewModel: VibeMainViewModel,
    post: Post,
    onDismiss: () -> Unit
) {
    val comments by viewModel.observePostComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var commentText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Comments (${comments.size})", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(comments, key = { it.id }) { comment ->
                    Row(verticalAlignment = Alignment.Top) {
                        ProfileAvatar(
                            avatarImage = comment.authorAvatar,
                            avatarEmoji = "💬",
                            cardTheme = "electric_purple",
                            size = 34.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("@${comment.authorUsername}", style = MaterialTheme.typography.labelLarge, color = NeonViolet)
                            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it.take(500) },
                    placeholder = { Text("Add a vibe comment...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.addComment(post, commentText)
                            commentText = ""
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment", tint = ElectricPurple)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharePostBottomSheet(
    post: Post,
    conversations: List<Conversation>,
    onSendToChat: (Conversation) -> Unit,
    onExternalShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Share Vibe", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onExternalShare,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share Outside VIBE")
            }
            if (conversations.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Send to VIBE Chat", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                conversations.take(6).forEach { conv ->
                    val label = if (conv.isGroup) conv.groupName.ifBlank { "Group Chat" }
                    else conv.participantNames.values.joinToString(", ")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSendToChat(conv) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = CyberCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsBottomSheet(
    profileUsername: String,
    notifications: List<ProfileNotification>,
    onMarkRead: (ProfileNotification) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Notifications • @$profileUsername", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            if (notifications.isEmpty()) {
                Text(
                    "No activity for @$profileUsername yet. Each VIBE profile keeps its notifications strictly separate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        Surface(
                            color = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMarkRead(notif) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    when (notif.type) {
                                        "like" -> "❤️"
                                        "comment" -> "💬"
                                        "follow" -> "⚡"
                                        else -> "✨"
                                    },
                                    fontSize = 20.sp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(notif.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
