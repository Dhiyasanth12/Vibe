package com.example.ui.screens

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.UiState
import com.example.data.model.VibeProfile
import com.example.data.repository.VibeAiRepository
import com.example.data.repository.VibeRepository
import com.example.ui.components.MediaContentView
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.VibeEmptyState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.HotMagenta
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SolarGold
import com.example.ui.theme.VibeChatThemes
import com.example.ui.theme.chatThemeForId
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.VibeMainViewModel
import com.example.util.ImageUtils
import com.example.util.VoiceRecorderPlayer
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun ChatsListScreen(
    viewModel: VibeMainViewModel,
    activeProfile: VibeProfile,
    onOpenConversation: (String) -> Unit
) {
    val conversationsState by viewModel.conversations.collectAsStateWithLifecycle()
    val discoverState by viewModel.discoverProfiles.collectAsStateWithLifecycle()
    val myProfilesState by viewModel.myProfiles.collectAsStateWithLifecycle()

    var showNewChatSheet by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showVibeAiSheet by remember { mutableStateOf(false) }

    val allProfiles = ((discoverState as? UiState.Success)?.data.orEmpty() +
        (myProfilesState as? UiState.Success)?.data.orEmpty())
        .distinctBy { it.id }
        .filter { it.id != activeProfile.id && !it.isDeactivated }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("VIBE Chats", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("@${activeProfile.username} inbox", style = MaterialTheme.typography.labelSmall, color = NeonViolet)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = ElectricPurple.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .testTag("open_vibe_ai_chat_button")
                        .clickable { showVibeAiSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "VibeAI Assistant", tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("VibeAI", style = MaterialTheme.typography.labelLarge, color = CyberCyan)
                    }
                }
                IconButton(onClick = { showNewGroupDialog = true }) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "New group chat")
                }
                IconButton(
                    onClick = { showNewChatSheet = true },
                    modifier = Modifier.testTag("new_chat_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New chat")
                }
            }
        }

        when (val state = conversationsState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                VibeEmptyState(Icons.AutoMirrored.Filled.Chat, "Chat Error", state.message)
            }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    VibeEmptyState(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = "No Conversations Yet",
                        subtitle = "Start a real-time 1:1 or group chat with disappearing timers, voice notes, and VibeAI smart replies.",
                        actionLabel = "Start Chat",
                        onAction = { showNewChatSheet = true }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.data, key = { it.id }) { conv ->
                            val otherName = if (conv.isGroup) {
                                conv.groupName.ifBlank { "VIBE Group" }
                            } else {
                                conv.participantNames.entries
                                    .firstOrNull { it.key != activeProfile.id }?.value
                                    ?: activeProfile.displayName
                            }
                            val othersTyping = conv.typingProfileIds.any { it != activeProfile.id }
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .clickable { onOpenConversation(conv.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileAvatar(
                                        avatarImage = "",
                                        avatarEmoji = if (conv.isGroup) "👥" else "💬",
                                        cardTheme = if (conv.chatTheme == "neon_cyan") "cyber_cyan" else "electric_purple",
                                        size = 50.dp,
                                        isOnline = true
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(otherName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            if (conv.disappearingSeconds > 0) {
                                                Spacer(Modifier.width(6.dp))
                                                Icon(Icons.Default.Timer, contentDescription = "Disappearing", tint = CyberCyan, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Text(
                                            text = if (othersTyping) "typing..." else conv.lastMessage.ifBlank { "Tap to chat" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (othersTyping) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        color = SolarGold.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = SolarGold, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("${conv.streakCount}", style = MaterialTheme.typography.labelSmall, color = SolarGold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatSheet) {
        NewDirectChatSheet(
            profiles = allProfiles,
            selfProfile = activeProfile,
            onSelectProfile = { target ->
                showNewChatSheet = false
                viewModel.startDirectChat(target, onOpenConversation)
            },
            onDismiss = { showNewChatSheet = false }
        )
    }

    if (showNewGroupDialog) {
        CreateGroupDialog(
            profiles = allProfiles,
            onCreate = { name, members ->
                showNewGroupDialog = false
                viewModel.createGroupChat(members, name, onOpenConversation)
            },
            onDismiss = { showNewGroupDialog = false }
        )
    }

    if (showVibeAiSheet) {
        StandaloneVibeAiDialog(onDismiss = { showVibeAiSheet = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatRoomScreen(
    mainViewModel: VibeMainViewModel,
    activeProfile: VibeProfile,
    conversationId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val chatViewModel: ChatViewModel = viewModel(
        key = "${mainViewModel.currentUserId}_$conversationId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                ChatViewModel(VibeRepository(db), VibeAiRepository(), mainViewModel.currentUserId, conversationId)
            }
        }
    )

    val conversationsState by mainViewModel.conversations.collectAsStateWithLifecycle()
    val conversation = ((conversationsState as? UiState.Success)?.data.orEmpty())
        .firstOrNull { it.id == conversationId }
    val messagesState by chatViewModel.messages.collectAsStateWithLifecycle()
    val smartRepliesState by chatViewModel.smartRepliesState.collectAsStateWithLifecycle()
    val rewriteState by chatViewModel.rewriteState.collectAsStateWithLifecycle()
    val aiAssistantState by chatViewModel.aiAssistantState.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessageForActions by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimerMenu by remember { mutableStateOf(false) }
    var showAiAssistantSheet by remember { mutableStateOf(false) }
    var activeCallMode by remember { mutableStateOf<String?>(null) } // "voice" or "video"

    // Voice recorder & player
    val voiceHelper = remember(context) { VoiceRecorderPlayer(context) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var playingMsgId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            voiceHelper.stopRecording()
            voiceHelper.stopPlayback()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isRecordingVoice = voiceHelper.startRecording()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && conversation != null) {
            val encoded = ImageUtils.uriToCompressedDataUri(context, uri, 680, 68)
            if (!encoded.isNullOrBlank()) {
                chatViewModel.sendMessage(
                    conversation = conversation,
                    senderProfile = activeProfile,
                    text = inputText,
                    mediaType = "image",
                    mediaData = encoded
                )
                inputText = ""
            }
        }
    }

    // Apply AI rewrite if ready
    LaunchedEffect(rewriteState) {
        val s = rewriteState as? UiState.Success
        if (s != null && s.data.isNotBlank()) {
            inputText = s.data
        }
    }

    // Mark incoming messages read automatically
    val msgList = (messagesState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(msgList.size, activeProfile.id) {
        if (msgList.isNotEmpty()) {
            chatViewModel.markRead(msgList, activeProfile.id)
        }
    }

    // Typing indicator debounce
    LaunchedEffect(inputText, conversation?.id) {
        val conv = conversation ?: return@LaunchedEffect
        if (inputText.isNotBlank()) {
            chatViewModel.updateTyping(conv, activeProfile.id, true)
            delay(2500)
            chatViewModel.updateTyping(conv, activeProfile.id, false)
        } else {
            chatViewModel.updateTyping(conv, activeProfile.id, false)
        }
    }

    val chatTheme = chatThemeForId(conversation?.chatTheme ?: "midnight_purple")
    val listState = rememberLazyListState()
    LaunchedEffect(msgList.size) {
        if (msgList.isNotEmpty()) listState.animateScrollToItem(msgList.lastIndex)
    }

    val title = when {
        conversation == null -> "Chat"
        conversation.isGroup -> conversation.groupName.ifBlank { "Group Chat" }
        else -> conversation.participantNames.entries.firstOrNull { it.key != activeProfile.id }?.value
            ?: activeProfile.displayName
    }
    val othersTyping = conversation?.typingProfileIds?.any { it != activeProfile.id } == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(chatTheme.backdropTint)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Header
        Surface(color = Color.Black.copy(alpha = 0.35f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if ((conversation?.streakCount ?: 0L) > 0L) {
                            Spacer(Modifier.width(6.dp))
                            Text("🔥${conversation?.streakCount}", style = MaterialTheme.typography.labelLarge, color = SolarGold)
                        }
                    }
                    Text(
                        text = when {
                            othersTyping -> "typing..."
                            (conversation?.disappearingSeconds ?: 0L) > 0L -> "Online • Disappearing ${conversation?.disappearingSeconds}s"
                            else -> "Online • Encrypted Vibe"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                }
                IconButton(onClick = { activeCallMode = "voice" }) {
                    Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = Color.White)
                }
                IconButton(onClick = { activeCallMode = "video" }) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White)
                }
                IconButton(onClick = { showThemeDialog = true }) {
                    Icon(Icons.Default.ColorLens, contentDescription = "Chat Theme", tint = NeonViolet)
                }
                Box {
                    IconButton(onClick = { showTimerMenu = true }) {
                        Icon(Icons.Default.Timer, contentDescription = "Disappearing Timer", tint = CyberCyan)
                    }
                    DropdownMenu(expanded = showTimerMenu, onDismissRequest = { showTimerMenu = false }) {
                        listOf(0L to "Off", 30L to "30 seconds", 3600L to "1 hour", 86400L to "24 hours").forEach { (sec, label) ->
                            DropdownMenuItem(
                                text = { Text("Disappearing: $label") },
                                onClick = {
                                    chatViewModel.updateDisappearingTimer(sec)
                                    showTimerMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(msgList, key = { it.id }) { msg ->
                val isMine = msg.senderProfileId == activeProfile.id
                val readByOther = msg.readByProfileIds.any { it != activeProfile.id }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = if (isMine) 20.dp else 6.dp,
                                    bottomEnd = if (isMine) 6.dp else 20.dp
                                )
                            )
                            .background(
                                if (isMine) Brush.linearGradient(listOf(chatTheme.bubbleOwnStart, chatTheme.bubbleOwnEnd))
                                else Brush.linearGradient(listOf(Color(0xFF1F1A30), Color(0xFF28213D)))
                            )
                            .combinedClickable(
                                onClick = { selectedMessageForActions = msg },
                                onLongClick = { selectedMessageForActions = msg }
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            if (msg.forwardedFrom.isNotBlank()) {
                                Text(
                                    text = "Forwarded from ${msg.forwardedFrom}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            if (msg.replyToPreview.isNotBlank()) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = msg.replyToPreview,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(8.dp),
                                        maxLines = 2
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                            if (msg.mediaData.isNotBlank() && (msg.mediaType == "image" || msg.mediaType == "post_share")) {
                                MediaContentView(
                                    mediaData = msg.mediaData,
                                    contentDescription = "Chat image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            if (msg.mediaType == "voice") {
                                val isPlaying = playingMsgId == msg.id
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        if (isPlaying) {
                                            voiceHelper.stopPlayback()
                                            playingMsgId = null
                                        } else {
                                            playingMsgId = msg.id
                                            voiceHelper.playEncodedAudio(msg.mediaData) {
                                                playingMsgId = null
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Play voice note",
                                        tint = CyberCyan
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Voice Note • ${msg.voiceDurationSec}s",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            } else if (msg.text.isNotBlank()) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }

                            Row(
                                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (msg.reactions.isNotEmpty()) {
                                    Text(
                                        text = msg.reactions.values.distinct().joinToString(" "),
                                        fontSize = 13.sp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                if (isMine) {
                                    Icon(
                                        Icons.Default.DoneAll,
                                        contentDescription = if (readByOther) "Read" else "Sent",
                                        tint = if (readByOther) CyberCyan else Color.White.copy(alpha = 0.55f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // VibeAI Smart Replies & Tone Rewrite Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Surface(
                        color = ElectricPurple.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .testTag("ai_smart_replies_chip")
                            .clickable { chatViewModel.requestSmartReplies(activeProfile.moodStatus) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Smart Replies", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                        }
                    }
                }
                item {
                    Surface(
                        color = HotMagenta.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.clickable { showAiAssistantSheet = true }
                    ) {
                        Text(
                            "Ask VibeAI",
                            style = MaterialTheme.typography.labelSmall,
                            color = HotMagenta,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                if (inputText.isNotBlank()) {
                    listOf("Witty", "Poetic", "Bold").forEach { tone ->
                        item {
                            Surface(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable { chatViewModel.rewriteDraft(inputText, tone) }
                            ) {
                                Text(
                                    "✨ $tone",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                val suggestions = (smartRepliesState as? UiState.Success)?.data.orEmpty()
                items(suggestions) { suggestion ->
                    Surface(
                        color = CyberCyan.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.clickable {
                            conversation?.let {
                                chatViewModel.sendMessage(it, activeProfile, suggestion, replyTo = replyingTo)
                                replyingTo = null
                            }
                        }
                    ) {
                        Text(
                            suggestion,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (smartRepliesState is UiState.Loading || rewriteState is UiState.Loading) {
                Text("VibeAI thinking...", style = MaterialTheme.typography.labelSmall, color = CyberCyan, modifier = Modifier.padding(top = 4.dp))
            }
            if (smartRepliesState is UiState.Error) {
                Text(
                    (smartRepliesState as UiState.Error).message,
                    style = MaterialTheme.typography.labelSmall,
                    color = HotMagenta,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Reply Banner
        AnimatedVisibility(visible = replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElectricPurple.copy(alpha = 0.25f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Replying to ${replyingTo?.senderName}: ${replyingTo?.text?.take(45)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel reply", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            ) {
                Icon(Icons.Default.Image, contentDescription = "Send image", tint = NeonViolet)
            }
            IconButton(
                onClick = {
                    if (isRecordingVoice) {
                        val recorded = voiceHelper.stopRecordingAndEncode()
                        isRecordingVoice = false
                        if (recorded != null && conversation != null) {
                            chatViewModel.sendMessage(
                                conversation = conversation,
                                senderProfile = activeProfile,
                                text = "Voice note",
                                mediaType = "voice",
                                mediaData = recorded.first,
                                voiceDurationSec = recorded.second
                            )
                        }
                    } else {
                        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            isRecordingVoice = voiceHelper.startRecording()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            ) {
                Icon(
                    if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecordingVoice) "Stop & send voice note" else "Record voice note",
                    tint = if (isRecordingVoice) HotMagenta else CyberCyan
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it.take(2000) },
                placeholder = { Text(if (isRecordingVoice) "Recording audio..." else "Message on VIBE...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    val conv = conversation ?: return@IconButton
                    if (inputText.isNotBlank()) {
                        chatViewModel.sendMessage(conv, activeProfile, inputText, replyTo = replyingTo)
                        inputText = ""
                        replyingTo = null
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ElectricPurple)
                    .testTag("send_message_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }

    // Message Actions Sheet (React, Reply, Forward, Delete)
    selectedMessageForActions?.let { msg ->
        ModalBottomSheet(
            onDismissRequest = { selectedMessageForActions = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("❤️", "🔥", "😂", "😮", "💜").forEach { emoji ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    chatViewModel.toggleReaction(msg, activeProfile.id, emoji)
                                    selectedMessageForActions = null
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 22.sp) }
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        replyingTo = msg
                        selectedMessageForActions = null
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reply")
                }
                OutlinedButton(
                    onClick = {
                        forwardingMessage = msg
                        selectedMessageForActions = null
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Forward to Chat")
                }
                if (msg.senderProfileId == activeProfile.id) {
                    OutlinedButton(
                        onClick = {
                            chatViewModel.deleteMessage(msg.id)
                            selectedMessageForActions = null
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HotMagenta)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Unsend Message")
                    }
                }
            }
        }
    }

    // Forward Message Dialog
    forwardingMessage?.let { msg ->
        val convs = (conversationsState as? UiState.Success)?.data.orEmpty()
        AlertDialog(
            onDismissRequest = { forwardingMessage = null },
            title = { Text("Forward Message") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    convs.forEach { targetConv ->
                        val label = if (targetConv.isGroup) targetConv.groupName else targetConv.participantNames.values.joinToString(", ")
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    chatViewModel.sendMessage(
                                        conversation = targetConv,
                                        senderProfile = activeProfile,
                                        text = msg.text,
                                        mediaType = msg.mediaType,
                                        mediaData = msg.mediaData,
                                        forwardedFrom = msg.senderName
                                    )
                                    forwardingMessage = null
                                }
                        ) {
                            Text(label, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { forwardingMessage = null }) { Text("Close") }
            }
        )
    }

    // Custom Chat Theme Picker
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Chat Theme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VibeChatThemes.forEach { theme ->
                        Surface(
                            color = theme.backdropTint,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, theme.bubbleOwnEnd, RoundedCornerShape(14.dp))
                                .clickable {
                                    chatViewModel.updateChatTheme(theme.id)
                                    showThemeDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(theme.label, color = Color.White, style = MaterialTheme.typography.titleMedium)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(theme.bubbleOwnStart, theme.bubbleOwnEnd)))
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Done") }
            }
        )
    }

    // VibeAI Conversation Assistant Sheet
    if (showAiAssistantSheet) {
        var aiPrompt by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { showAiAssistantSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("VibeAI Conversation Assistant", style = MaterialTheme.typography.headlineMedium)
                }
                OutlinedTextField(
                    value = aiPrompt,
                    onValueChange = { aiPrompt = it },
                    label = { Text("Ask for an icebreaker, summary, or reply...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (aiPrompt.isNotBlank()) chatViewModel.askVibeAi(aiPrompt)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Ask VibeAI")
                }
                when (val s = aiAssistantState) {
                    is UiState.Loading -> CircularProgressIndicator()
                    is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                    is UiState.Success -> if (s.data.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(s.data, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = {
                                    inputText = s.data
                                    showAiAssistantSheet = false
                                }) {
                                    Text("Insert into Chat")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Voice & Video Call HUD
    activeCallMode?.let { mode ->
        VibeCallDialog(
            peerName = title,
            isVideo = mode == "video",
            onEndCall = { seconds ->
                val conv = conversation
                if (conv != null) {
                    chatViewModel.sendMessage(
                        conversation = conv,
                        senderProfile = activeProfile,
                        text = if (mode == "video") "📹 Video call (${seconds}s)" else "📞 Voice call (${seconds}s)",
                        mediaType = "call_event"
                    )
                }
                activeCallMode = null
            }
        )
    }
}

@Composable
private fun VibeCallDialog(
    peerName: String,
    isVideo: Boolean,
    onEndCall: (seconds: Int) -> Unit
) {
    var elapsed by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(isVideo) }
    var camOn by remember { mutableStateOf(isVideo) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsed++
        }
    }

    Dialog(
        onDismissRequest = { onEndCall(elapsed) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF2B124C), MidnightBlack)))
                .systemBarsPaddingCompat()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
                    Text(
                        if (isVideo) "VIBE Video Call" else "VIBE Voice Call",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan
                    )
                    Spacer(Modifier.height(14.dp))
                    ProfileAvatar("", if (camOn) "📹" else "🎧", "electric_purple", size = 104.dp, hasStoryRing = true)
                    Spacer(Modifier.height(16.dp))
                    Text(peerName, style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (elapsed < 2) "Ringing..." else String.format("%02d:%02d", elapsed / 60, elapsed % 60),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { muted = !muted },
                        modifier = Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Icon(if (muted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute", tint = Color.White)
                    }
                    if (isVideo) {
                        IconButton(
                            onClick = { camOn = !camOn },
                            modifier = Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))
                        ) {
                            Icon(if (camOn) Icons.Default.Videocam else Icons.Default.VideocamOff, contentDescription = "Camera", tint = Color.White)
                        }
                    }
                    IconButton(
                        onClick = { speaker = !speaker },
                        modifier = Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Speaker", tint = if (speaker) CyberCyan else Color.White)
                    }
                    IconButton(
                        onClick = { onEndCall(elapsed) },
                        modifier = Modifier.size(58.dp).clip(CircleShape).background(HotMagenta)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White)
                    }
                }
            }
        }
    }
}

private fun Modifier.systemBarsPaddingCompat(): Modifier = this.statusBarsPadding().navigationBarsPadding()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewDirectChatSheet(
    profiles: List<VibeProfile>,
    selfProfile: VibeProfile,
    onSelectProfile: (VibeProfile) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Start Real-Time Chat", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProfile(selfProfile) }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatar(selfProfile.avatarImage, selfProfile.avatarEmoji, selfProfile.cardTheme, size = 40.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("${selfProfile.displayName} (Saved Notes)", style = MaterialTheme.typography.titleMedium)
                                Text("@${selfProfile.username}", style = MaterialTheme.typography.labelSmall, color = NeonViolet)
                            }
                        }
                    }
                }
                items(profiles, key = { it.id }) { p ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProfile(p) }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatar(p.avatarImage, p.avatarEmoji, p.cardTheme, size = 40.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(p.displayName, style = MaterialTheme.typography.titleMedium)
                                Text("@${p.username}", style = MaterialTheme.typography.labelSmall, color = NeonViolet)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    profiles: List<VibeProfile>,
    onCreate: (String, List<VibeProfile>) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<VibeProfile>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group Chat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it.take(60) },
                    label = { Text("Group Name") },
                    singleLine = true
                )
                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(profiles, key = { it.id }) { p ->
                        val checked = selected.any { it.id == p.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selected.removeAll { it.id == p.id }
                                    else selected.add(p)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (checked) selected.removeAll { it.id == p.id }
                                    else selected.add(p)
                                }
                            )
                            Text("${p.displayName} (@${p.username})")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onCreate(groupName, selected.toList())
                    }
                }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandaloneVibeAiDialog(onDismiss: () -> Unit) {
    val aiRepo = remember { VibeAiRepository() }
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var triggerCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(triggerCount) {
        if (triggerCount > 0 && prompt.isNotBlank()) {
            isLoading = true
            errorMsg = null
            try {
                aiRepo.streamAssistant(prompt).collect { partial ->
                    answer = partial
                }
            } catch (e: Exception) {
                errorMsg = e.message ?: "VibeAI error"
            } finally {
                isLoading = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberCyan)
                Spacer(Modifier.width(8.dp))
                Text("VibeAI Studio Assistant", style = MaterialTheme.typography.headlineMedium)
            }
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Ask VibeAI anything...") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { if (prompt.isNotBlank()) triggerCount++ },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Generate")
            }
            if (!errorMsg.isNullOrBlank()) {
                Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            if (answer.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(answer, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(14.dp))
                }
            }
        }
    }
}
