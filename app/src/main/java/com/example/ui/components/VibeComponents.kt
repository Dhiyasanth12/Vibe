package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.VibeMoodOptions
import com.example.data.model.VibeProfile
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.EmeraldCloseFriend
import com.example.ui.theme.HotMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SolarGold
import com.example.ui.theme.cardThemeForId
import com.example.util.ImageUtils

@Composable
fun ProfileAvatar(
    avatarImage: String,
    avatarEmoji: String,
    cardTheme: String,
    size: Dp = 48.dp,
    hasStoryRing: Boolean = false,
    isCloseFriendRing: Boolean = false,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier
) {
    val style = cardThemeForId(cardTheme)
    val ringBrush = when {
        isCloseFriendRing -> Brush.linearGradient(listOf(EmeraldCloseFriend, CyberCyan))
        hasStoryRing -> Brush.linearGradient(listOf(ElectricPurple, HotMagenta, CyberCyan))
        else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val decoded = remember(avatarImage) {
        if (avatarImage.startsWith("data:image")) ImageUtils.decodeDataUriToImageBitmap(avatarImage) else null
    }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hasStoryRing || isCloseFriendRing) {
                        Modifier
                            .border(2.5.dp, ringBrush, CircleShape)
                            .padding(3.5.dp)
                    } else Modifier
                )
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(style.primary, style.secondary))),
            contentAlignment = Alignment.Center
        ) {
            if (decoded != null) {
                Image(
                    bitmap = decoded,
                    contentDescription = "Profile avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = avatarEmoji.ifBlank { "⚡" },
                    fontSize = (size.value * 0.42f).sp
                )
            }
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((size.value * 0.26f).dp.coerceAtLeast(10.dp))
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(CyberCyan)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            )
        }
    }
}

@Composable
fun MediaContentView(
    mediaData: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    when {
        mediaData == "preset:hero" -> {
            Image(
                painter = painterResource(id = R.drawable.img_vibe_hero_backdrop),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        mediaData == "preset:stage" -> {
            Image(
                painter = painterResource(id = R.drawable.img_vibe_stage_cover),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        mediaData == "preset:reel" -> {
            Image(
                painter = painterResource(id = R.drawable.img_vibe_reel_art),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        mediaData.startsWith("data:image") -> {
            val bmp = remember(mediaData) { ImageUtils.decodeDataUriToImageBitmap(mediaData) }
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun AnimatedVibeProfileCard(
    profile: VibeProfile,
    modifier: Modifier = Modifier,
    onSwitchAccountClick: (() -> Unit)? = null,
    onMoodClick: (() -> Unit)? = null
) {
    val style = cardThemeForId(profile.cardTheme)
    val transition = rememberInfiniteTransition(label = "card_shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    val cardBrush = Brush.linearGradient(
        colors = listOf(
            style.primary.copy(alpha = 0.92f),
            style.secondary.copy(alpha = 0.85f),
            style.accent.copy(alpha = 0.35f)
        ),
        start = Offset(shimmerOffset * 0.2f, 0f),
        end = Offset(600f + shimmerOffset * 0.4f, 500f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cardBrush)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(
                        avatarImage = profile.avatarImage,
                        avatarEmoji = profile.avatarEmoji,
                        cardTheme = profile.cardTheme,
                        size = 64.dp,
                        isOnline = profile.isOnline,
                        hasStoryRing = true
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${profile.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        if (profile.anonymousDiscovery) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = Color.Black.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.VisibilityOff,
                                        contentDescription = "Anonymous discovery active",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Incognito Discovery",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberCyan
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = "Daily streak",
                            tint = SolarGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${profile.streakDays}d",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }

            if (profile.bio.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.then(
                        if (onMoodClick != null) Modifier.clickable { onMoodClick() } else Modifier
                    )
                ) {
                    Text(
                        text = "${profile.moodEmoji} ${profile.moodStatus} • ${profile.activityStatus}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onSwitchAccountClick != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .testTag("switch_profile_chip")
                            .clickable { onSwitchAccountClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Switch profile",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Switch",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcherSheet(
    profiles: List<VibeProfile>,
    loggedOutProfileIds: Set<String>,
    activeProfileId: String?,
    phoneDisplay: String,
    onSelectProfile: (VibeProfile) -> Unit,
    onLoginToProfile: (VibeProfile) -> Unit,
    onLogoutProfile: (VibeProfile) -> Unit,
    onAddExistingAccount: () -> Unit,
    onCreateNewProfile: () -> Unit,
    onManageProfiles: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "VIBE Accounts Center",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (phoneDisplay.isNotBlank()) {
                Text(
                    text = "Verified Mobile: $phoneDisplay • Switch, add or log out per account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    val isCurrent = profile.id == activeProfileId
                    val isLoggedOut = profile.id in loggedOutProfileIds
                    Surface(
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("switcher_item_${profile.username}")
                            .clickable {
                                onDismiss()
                                if (isLoggedOut) {
                                    onLoginToProfile(profile)
                                } else {
                                    onSelectProfile(profile)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileAvatar(
                                avatarImage = profile.avatarImage,
                                avatarEmoji = profile.avatarEmoji,
                                cardTheme = profile.cardTheme,
                                size = 46.dp,
                                isOnline = !profile.isDeactivated && !isLoggedOut
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isLoggedOut) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            color = HotMagenta.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                "Logged Out",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = HotMagenta,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (profile.isDeactivated) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                "Deactivated",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isLoggedOut) {
                                        "@${profile.username} • Tap to log in"
                                    } else {
                                        "@${profile.username} • ${profile.moodEmoji} ${profile.moodStatus}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isLoggedOut) {
                                TextButton(
                                    onClick = {
                                        onDismiss()
                                        onLoginToProfile(profile)
                                    },
                                    modifier = Modifier.testTag("switcher_login_${profile.username}")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Login,
                                        contentDescription = "Log in @${profile.username}",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Log In", color = CyberCyan)
                                }
                            } else {
                                if (isCurrent) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Active profile",
                                        tint = CyberCyan
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                IconButton(
                                    onClick = { onLogoutProfile(profile) },
                                    modifier = Modifier.testTag("switcher_logout_${profile.username}")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Log out @${profile.username}",
                                        tint = HotMagenta
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onCreateNewProfile()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_new_profile_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create Account", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onAddExistingAccount()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_existing_account_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Log In Account", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onManageProfiles()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("manage_profiles_button")
            ) {
                Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Manage All Accounts & Recovery")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodSelectorSheet(
    currentMood: String,
    currentActivity: String,
    onSave: (mood: String, emoji: String, activity: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMood by remember { mutableStateOf(VibeMoodOptions.firstOrNull { it.first == currentMood } ?: VibeMoodOptions.first()) }
    var activityInput by remember { mutableStateOf(currentActivity) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Set Your Vibe & Activity", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VibeMoodOptions.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { option ->
                            val isSelected = selectedMood.first == option.first
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedMood = option }
                            ) {
                                Text(
                                    text = "${option.second} ${option.first}",
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = activityInput,
                onValueChange = { activityInput = it.take(60) },
                label = { Text("Activity status (e.g. Listening to synthwave)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onSave(selectedMood.first, selectedMood.second, activityInput.ifBlank { "Online on VIBE" })
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Update Vibe")
            }
        }
    }
}

@Composable
fun VibeEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            shape = CircleShape,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = NeonViolet,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
