package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UiState
import com.example.data.model.VibeProfile
import com.example.ui.components.MediaContentView
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.VibeCardThemes
import com.example.ui.viewmodel.VibeMainViewModel
import com.example.util.ImageUtils

@Composable
fun CreatePostScreen(
    viewModel: VibeMainViewModel,
    activeProfile: VibeProfile,
    initialMode: String = "Post",
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val aiCaptionState by viewModel.aiCaptionState.collectAsStateWithLifecycle()

    var mode by remember(initialMode) { mutableStateOf(initialMode) } // "Post", "Story", "Reel"
    var caption by remember { mutableStateOf("") }
    var aiTopic by remember { mutableStateOf("") }
    var mediaData by remember { mutableStateOf("preset:hero") }
    var visibility by remember { mutableStateOf("public") }
    var audioTrack by remember { mutableStateOf("Midnight Synthwave • VIBE Original") }
    var bgTheme by remember { mutableStateOf(activeProfile.cardTheme) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val compressed = ImageUtils.uriToCompressedDataUri(context, uri, 720, 68)
            if (!compressed.isNullOrBlank()) {
                mediaData = compressed
            }
        }
    }

    LaunchedEffect(aiCaptionState) {
        val success = aiCaptionState as? UiState.Success
        if (success != null && success.data.isNotBlank()) {
            caption = success.data
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Create as @${activeProfile.username}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Post", "Story", "Reel").forEach { item ->
                FilterChip(
                    selected = mode == item,
                    onClick = { mode = item },
                    label = { Text(if (item == "Story") "24h Story" else item) },
                    modifier = Modifier.testTag("create_mode_$item")
                )
            }
        }

        // Media Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (mediaData.isNotBlank()) {
                MediaContentView(
                    mediaData = mediaData,
                    contentDescription = "Selected media preview",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Text-only Vibe", style = MaterialTheme.typography.titleMedium)
            }
        }

        // Zero-permission Photo Picker + Curated Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("pick_photo_button")
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Gallery Photo")
            }
            OutlinedButton(
                onClick = {
                    mediaData = when (mediaData) {
                        "preset:hero" -> "preset:stage"
                        "preset:stage" -> "preset:reel"
                        "preset:reel" -> ""
                        else -> "preset:hero"
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Cycle VIBE Art")
            }
        }

        // VibeAI Caption Generator
        Surface(
            color = ElectricPurple.copy(alpha = 0.14f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ElectricPurple.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("VibeAI Caption & Hashtag Studio", style = MaterialTheme.typography.titleMedium, color = CyberCyan)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = aiTopic,
                        onValueChange = { aiTopic = it },
                        placeholder = { Text("Topic (e.g. rooftop sunset)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (aiTopic.isNotBlank()) viewModel.generateAiCaption(aiTopic)
                        },
                        enabled = aiCaptionState !is UiState.Loading,
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (aiCaptionState is UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Generate")
                        }
                    }
                }
                if (aiCaptionState is UiState.Error) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        (aiCaptionState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it.take(1000) },
            label = { Text("Caption & #hashtags") },
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_caption_input")
        )

        if (mode == "Reel") {
            OutlinedTextField(
                value = audioTrack,
                onValueChange = { audioTrack = it.take(100) },
                label = { Text("Reel Audio Track") },
                leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (mode == "Story") {
            Text("Story Gradient Ring", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                VibeCardThemes.forEach { theme ->
                    val picked = bgTheme == theme.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(theme.primary, theme.secondary)))
                            .border(
                                if (picked) 2.dp else 0.dp,
                                if (picked) CyberCyan else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { bgTheme = theme.id }
                    )
                }
            }
        }

        // Audience / Visibility
        Text("Audience", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val publicVal = if (mode == "Story") "all" else "public"
            FilterChip(
                selected = visibility == "public" || visibility == "all",
                onClick = { visibility = publicVal },
                label = { Text("Everyone") }
            )
            FilterChip(
                selected = visibility == "close_friends",
                onClick = { visibility = "close_friends" },
                label = { Text("Close Friends Only") }
            )
        }

        Button(
            onClick = {
                val safeCaption = caption.ifBlank { "Fresh drop on VIBE #vibe" }
                when (mode) {
                    "Story" -> {
                        val aud = if (visibility == "close_friends") "close_friends" else "all"
                        viewModel.createStory(safeCaption, mediaData, bgTheme, aud, onPublished)
                    }
                    "Reel" -> {
                        viewModel.createPost(
                            caption = safeCaption,
                            mediaType = "reel",
                            mediaData = mediaData.ifBlank { "preset:reel" },
                            visibility = if (visibility == "close_friends") "close_friends" else "public",
                            isReel = true,
                            audioTrack = audioTrack,
                            onSuccess = onPublished
                        )
                    }
                    else -> {
                        viewModel.createPost(
                            caption = safeCaption,
                            mediaType = if (mediaData.isNotBlank()) "photo" else "text",
                            mediaData = mediaData,
                            visibility = if (visibility == "close_friends") "close_friends" else "public",
                            isReel = false,
                            audioTrack = "",
                            onSuccess = onPublished
                        )
                    }
                }
            },
            enabled = !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("publish_content_button")
        ) {
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Publish $mode")
            }
        }
    }
}
