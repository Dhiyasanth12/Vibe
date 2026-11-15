package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AudioRoom
import com.example.data.model.UiState
import com.example.data.model.VibeProfile
import com.example.ui.components.AnimatedVibeProfileCard
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.HotMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.viewmodel.VibeMainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: VibeMainViewModel,
    activeProfile: VibeProfile,
    onOpenConversation: (String) -> Unit
) {
    val discoverState by viewModel.discoverProfiles.collectAsStateWithLifecycle()
    val feedState by viewModel.feedPosts.collectAsStateWithLifecycle()
    val audioRoomsState by viewModel.audioRooms.collectAsStateWithLifecycle()
    val followingRelations by viewModel.followingRelations.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var activeRoom by remember { mutableStateOf<AudioRoom?>(null) }
    var inspectingProfile by remember { mutableStateOf<VibeProfile?>(null) }

    val followingIds = remember(followingRelations) { followingRelations.map { it.targetProfileId }.toSet() }
    val allPosts = (feedState as? UiState.Success)?.data.orEmpty()
    val trendingTags = remember(allPosts) {
        allPosts.flatMap { it.hashtags }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .ifEmpty { listOf("vibe", "midnight", "music", "creators", "synthwave") }
            .take(8)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Explore VIBE",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search @usernames or #hashtags...") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input")
            )
        }

        // Anonymous Discovery Mode Banner
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = CyberCyan)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Anonymous Discovery Mode", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (activeProfile.anonymousDiscovery) "Your handle & activity are masked in discovery"
                                else "Visible to creators while browsing",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = activeProfile.anonymousDiscovery,
                        onCheckedChange = { viewModel.toggleAnonymousDiscovery(it) },
                        modifier = Modifier.testTag("anonymous_discovery_switch")
                    )
                }
            }
        }

        // Trending Hashtags
        item {
            Text("Trending Hashtags", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(trendingTags) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { selectedTag = if (selectedTag == tag) null else tag },
                        label = { Text("#$tag") }
                    )
                }
            }
        }

        // Live Audio Hangout Rooms
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = CyberCyan)
                    Spacer(Modifier.width(6.dp))
                    Text("Live Audio Rooms", style = MaterialTheme.typography.titleLarge)
                }
                TextButton(
                    onClick = { showCreateRoomDialog = true },
                    modifier = Modifier.testTag("start_audio_room_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Go Live")
                }
            }

            val rooms = (audioRoomsState as? UiState.Success)?.data.orEmpty()
            if (rooms.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                        .clickable { showCreateRoomDialog = true }
                ) {
                    Box(modifier = Modifier.height(120.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_vibe_stage_cover),
                            contentDescription = "Start a live audio room",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.45f
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Host a Live Audio Hangout", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Text("Invite friends to speak, raise hands & drop reactions", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(rooms, key = { it.id }) { room ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .width(240.dp)
                                .border(1.dp, ElectricPurple.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .clickable {
                                    viewModel.joinRoom(room)
                                    activeRoom = room
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(color = HotMagenta.copy(alpha = 0.2f), shape = RoundedCornerShape(50)) {
                                        Text(
                                            "LIVE • #${room.topicTag}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HotMagenta,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(room.latestReaction, fontSize = 18.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(room.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Host @${room.hostUsername} • ${room.listenerCount} listening",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // Trending Creators
        item {
            Text("Trending Creators", style = MaterialTheme.typography.titleLarge)
        }

        val creators = (discoverState as? UiState.Success)?.data.orEmpty()
            .filter { it.id != activeProfile.id }
            .filter { profile ->
                val q = searchQuery.trim().lowercase().removePrefix("@").removePrefix("#")
                if (q.isBlank()) true
                else profile.username.contains(q, true) || profile.displayName.contains(q, true) || profile.bio.contains(q, true)
            }

        if (creators.isEmpty()) {
            item {
                Text(
                    "No matching creators found yet — invite friends or create another VIBE persona!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(creators, key = { it.id }) { creator ->
                val isFollowing = creator.id in followingIds
                val displayHandle = if (creator.anonymousDiscovery) "incognito_vibe" else creator.username
                val displayTitle = if (creator.anonymousDiscovery) "Anonymous Explorer" else creator.displayName
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable { inspectingProfile = creator }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(
                            avatarImage = if (creator.anonymousDiscovery) "" else creator.avatarImage,
                            avatarEmoji = if (creator.anonymousDiscovery) "🎭" else creator.avatarEmoji,
                            cardTheme = creator.cardTheme,
                            size = 52.dp,
                            isOnline = creator.isOnline
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("@$displayHandle • ${creator.followersCount} followers", style = MaterialTheme.typography.labelSmall, color = NeonViolet)
                            if (creator.bio.isNotBlank() && !creator.anonymousDiscovery) {
                                Text(creator.bio, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = { viewModel.startDirectChat(creator, onOpenConversation) }) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message creator", tint = CyberCyan)
                            }
                            Button(
                                onClick = { viewModel.toggleFollow(creator, isFollowing) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else ElectricPurple
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(if (isFollowing) "Following" else "Follow", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        // Filtered Hashtag Posts Preview
        if (selectedTag != null || searchQuery.startsWith("#")) {
            val tagFilter = (selectedTag ?: searchQuery.removePrefix("#")).lowercase()
            val matchedPosts = allPosts.filter { p ->
                p.hashtags.any { it.contains(tagFilter) } || p.caption.lowercase().contains("#$tagFilter")
            }
            if (matchedPosts.isNotEmpty()) {
                item {
                    Text("Posts tagged #$tagFilter", style = MaterialTheme.typography.titleLarge)
                }
                items(matchedPosts, key = { "explore_${it.id}" }) { post ->
                    FeedPostCard(
                        viewModel = viewModel,
                        post = post,
                        activeProfile = activeProfile,
                        onCommentClick = {},
                        onShareClick = {},
                        onDeleteClick = null
                    )
                }
            }
        }
    }

    if (showCreateRoomDialog) {
        var title by remember { mutableStateOf("") }
        var topic by remember { mutableStateOf("LateNightVibes") }
        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            title = { Text("Start Live Audio Room") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(100) },
                        label = { Text("Room Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it.take(30) },
                        label = { Text("Topic Tag") },
                        prefix = { Text("#") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.createAudioRoom(title, topic.ifBlank { "vibe" })
                            showCreateRoomDialog = false
                        }
                    }
                ) { Text("Go Live") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoomDialog = false }) { Text("Cancel") }
            }
        )
    }

    activeRoom?.let { initialRoom ->
        val liveRooms = (audioRoomsState as? UiState.Success)?.data.orEmpty()
        val liveRoom = liveRooms.firstOrNull { it.id == initialRoom.id } ?: initialRoom
        AudioRoomSheet(
            room = liveRoom,
            activeProfile = activeProfile,
            onRaiseHand = { viewModel.toggleRaiseHand(liveRoom) },
            onPromoteSpeaker = { handle -> viewModel.promoteSpeaker(liveRoom, handle) },
            onReaction = { emoji -> viewModel.sendRoomReaction(liveRoom, emoji) },
            onEndRoom = {
                viewModel.endAudioRoom(liveRoom)
                activeRoom = null
            },
            onLeave = {
                viewModel.leaveRoom(liveRoom)
                activeRoom = null
            }
        )
    }

    inspectingProfile?.let { creator ->
        ModalBottomSheet(
            onDismissRequest = { inspectingProfile = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                AnimatedVibeProfileCard(profile = creator)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    val isFollowing = creator.id in followingIds
                    Button(
                        onClick = { viewModel.toggleFollow(creator, isFollowing) },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(if (isFollowing) "Unfollow" else "Follow")
                    }
                    OutlinedButton(
                        onClick = {
                            inspectingProfile = null
                            viewModel.startDirectChat(creator, onOpenConversation)
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Message")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioRoomSheet(
    room: AudioRoom,
    activeProfile: VibeProfile,
    onRaiseHand: () -> Unit,
    onPromoteSpeaker: (String) -> Unit,
    onReaction: (String) -> Unit,
    onEndRoom: () -> Unit,
    onLeave: () -> Unit
) {
    val isHost = room.hostProfileId == activeProfile.id
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onLeave, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = HotMagenta.copy(alpha = 0.2f), shape = RoundedCornerShape(50)) {
                    Text(
                        "LIVE AUDIO • #${room.topicTag}",
                        color = HotMagenta,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(room.latestReaction, fontSize = 26.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(room.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${room.listenerCount} vibing in room", style = MaterialTheme.typography.bodyMedium, color = CyberCyan)

            Spacer(Modifier.height(16.dp))
            Text("On Stage", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(room.speakerNames) { speaker ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(ElectricPurple, CyberCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = speaker, tint = Color.White)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(speaker, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (room.raisedHands.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("Raised Hands", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(room.raisedHands) { handle ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.clickable(enabled = isHost) { onPromoteSpeaker(handle) }
                        ) {
                            Text(
                                text = "✋ $handle" + if (isHost) " (Tap to invite)" else "",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("🔥", "💜", "👏", "🎧", "✨").forEach { emoji ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(46.dp)
                            .clickable { onReaction(emoji) }
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 20.sp) }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onRaiseHand,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.PanTool, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Raise Hand")
                }
                Button(
                    onClick = if (isHost) onEndRoom else onLeave,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isHost) HotMagenta else ElectricPurple)
                ) {
                    Text(if (isHost) "End Room" else "Leave Quietly")
                }
            }
        }
    }
}
