package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.VibeProfile
import com.example.ui.components.MediaContentView
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.VibeEmptyState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.HotMagenta
import com.example.ui.theme.MidnightBlack
import com.example.ui.viewmodel.VibeMainViewModel

@Composable
fun ReelsPlayerScreen(
    viewModel: VibeMainViewModel,
    activeProfile: VibeProfile,
    onBack: () -> Unit,
    onCreateReel: () -> Unit
) {
    val feedState by viewModel.feedPosts.collectAsStateWithLifecycle()
    val reels = ((feedState as? UiState.Success)?.data.orEmpty()).filter { it.isReel }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
    ) {
        if (reels.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                verticalArrangement = Arrangement.Center
            ) {
                VibeEmptyState(
                    icon = Icons.Default.VideoLibrary,
                    title = "No VIBE Reels Yet",
                    subtitle = "Create the first short reel with an audio track and electric visuals.",
                    actionLabel = "Create Reel",
                    onAction = onCreateReel
                )
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { reels.size })
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ReelPage(
                    viewModel = viewModel,
                    reel = reels[page],
                    activeProfile = activeProfile
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "VIBE Reels",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReelPage(
    viewModel: VibeMainViewModel,
    reel: Post,
    activeProfile: VibeProfile
) {
    val likes by viewModel.observePostLikes(reel.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val isLiked = likes.any { it.profileId == activeProfile.id }

    Box(modifier = Modifier.fillMaxSize()) {
        if (reel.mediaData.isNotBlank()) {
            MediaContentView(
                mediaData = reel.mediaData,
                contentDescription = reel.caption,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(ElectricPurple, MidnightBlack)))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, MidnightBlack.copy(alpha = 0.9f))
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(18.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(
                        avatarImage = reel.authorAvatar,
                        avatarEmoji = "🎬",
                        cardTheme = reel.authorCardTheme,
                        size = 40.dp,
                        hasStoryRing = true
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "@${reel.authorUsername}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = reel.caption,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                if (reel.audioTrack.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(reel.audioTrack, style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { viewModel.toggleLike(reel) }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like reel",
                        tint = if (isLiked) HotMagenta else Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text("${reel.likesCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(12.dp))
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = Color.White, modifier = Modifier.size(26.dp))
                Text("${reel.commentsCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(12.dp))
                IconButton(onClick = { viewModel.recordExternalShare(reel.id) }) {
                    Icon(Icons.Default.Share, contentDescription = "Share reel", tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Text("${reel.sharesCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
