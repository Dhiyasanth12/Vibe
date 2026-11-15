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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UiState
import com.example.data.model.VibeProfile
import com.example.ui.components.AnimatedVibeProfileCard
import com.example.ui.components.MediaContentView
import com.example.ui.components.MoodSelectorSheet
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.EmeraldCloseFriend
import com.example.ui.theme.HotMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.VibeCardThemes
import com.example.ui.viewmodel.VibeMainViewModel
import com.example.util.ImageUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: VibeMainViewModel,
    activeProfile: VibeProfile,
    allMyProfiles: List<VibeProfile>,
    loggedOutProfileIds: Set<String>,
    isDarkTheme: Boolean,
    onOpenSwitcher: () -> Unit,
    onOpenProfileManagement: () -> Unit,
    onOpenCreateProfile: () -> Unit,
    onOpenLoginForProfile: (String) -> Unit
) {
    val feedState by viewModel.feedPosts.collectAsStateWithLifecycle()
    val followingRelations by viewModel.followingRelations.collectAsStateWithLifecycle()

    var showEditSheet by remember { mutableStateOf(false) }
    var showMoodSheet by remember { mutableStateOf(false) }
    var showCloseFriendsSheet by remember { mutableStateOf(false) }

    val myPosts = remember(feedState, activeProfile.id) {
        ((feedState as? UiState.Success)?.data.orEmpty()).filter { it.profileId == activeProfile.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Account Switcher & Management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "@${activeProfile.username}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(
                        onClick = onOpenSwitcher,
                        modifier = Modifier.testTag("profile_switcher_button")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Switch VIBE account", tint = CyberCyan)
                    }
                    IconButton(
                        onClick = onOpenProfileManagement,
                        modifier = Modifier.testTag("open_profile_management_button")
                    ) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = "Manage profiles")
                    }
                }
            }
        }

        // Animated VIBE Identity Card
        item {
            AnimatedVibeProfileCard(
                profile = activeProfile,
                onSwitchAccountClick = onOpenSwitcher,
                onMoodClick = { showMoodSheet = true }
            )
        }

        // Quick Multi-Profile Carousel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your VIBE Identities (${allMyProfiles.size})", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onOpenCreateProfile) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Profile")
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(allMyProfiles, key = { it.id }) { p ->
                    val isActive = p.id == activeProfile.id
                    val isLoggedOut = p.id in loggedOutProfileIds
                    Surface(
                        color = if (isActive) ElectricPurple.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .border(
                                if (isActive) 1.5.dp else 0.dp,
                                if (isActive) CyberCyan else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                if (isLoggedOut) {
                                    onOpenLoginForProfile(p.username)
                                } else {
                                    viewModel.switchProfile(p)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileAvatar(p.avatarImage, p.avatarEmoji, p.cardTheme, size = 30.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isLoggedOut) "@${p.username} (Out)" else "@${p.username}",
                                style = MaterialTheme.typography.labelLarge,
                                color = when {
                                    isActive -> CyberCyan
                                    isLoggedOut -> HotMagenta
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }

        // Stats Row
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn("${myPosts.size}", "Posts")
                    StatColumn("${activeProfile.followersCount}", "Followers")
                    StatColumn("${activeProfile.followingCount}", "Following")
                    StatColumn("${activeProfile.streakDays}d", "Streak")
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showEditSheet = true },
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit Profile")
                }
                OutlinedButton(
                    onClick = { showCloseFriendsSheet = true },
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = EmeraldCloseFriend, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Close Friends")
                }
            }
        }

        // Controls: Theme + Anonymous Discovery
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = NeonViolet)
                            Spacer(Modifier.width(10.dp))
                            Text("Midnight Purple Dark Mode", style = MaterialTheme.typography.titleMedium)
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.toggleDarkTheme(it) },
                            modifier = Modifier.testTag("theme_mode_switch")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = CyberCyan)
                            Spacer(Modifier.width(10.dp))
                            Text("Anonymous Discovery Mode", style = MaterialTheme.typography.titleMedium)
                        }
                        Switch(
                            checked = activeProfile.anonymousDiscovery,
                            onCheckedChange = { viewModel.toggleAnonymousDiscovery(it) }
                        )
                    }
                }
            }
        }

        // Profile Posts
        item {
            Text("Posts by @${activeProfile.username}", style = MaterialTheme.typography.titleLarge)
        }

        if (myPosts.isEmpty()) {
            item {
                Text(
                    "No posts yet on this profile. Each VIBE profile has its own isolated feed & posts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(myPosts, key = { "prof_${it.id}" }) { post ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (post.mediaData.isNotBlank()) {
                            MediaContentView(
                                mediaData = post.mediaData,
                                contentDescription = post.caption,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(post.caption, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${post.likesCount} likes • ${post.commentsCount} comments",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showMoodSheet) {
        MoodSelectorSheet(
            currentMood = activeProfile.moodStatus,
            currentActivity = activeProfile.activityStatus,
            onSave = { m, e, a -> viewModel.updateMood(m, e, a) },
            onDismiss = { showMoodSheet = false }
        )
    }

    if (showEditSheet) {
        EditProfileSheet(
            profile = activeProfile,
            onSave = { name, bio, theme, avatarImg, emoji ->
                viewModel.updateProfile(activeProfile.id, name, bio, theme, avatarImg, emoji) {
                    showEditSheet = false
                }
            },
            onDismiss = { showEditSheet = false }
        )
    }

    if (showCloseFriendsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCloseFriendsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text("Close Friends Circle", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "People in your Close Friends circle see your emerald-ring stories and private posts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                if (followingRelations.isEmpty()) {
                    Text("Follow creators in Explore first to add them to Close Friends.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    followingRelations.forEach { rel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(rel.targetDisplayName.ifBlank { rel.targetUsername }, style = MaterialTheme.typography.titleMedium)
                                Text("@${rel.targetUsername}", style = MaterialTheme.typography.labelSmall, color = NeonViolet)
                            }
                            Switch(
                                checked = rel.isCloseFriend,
                                onCheckedChange = { viewModel.toggleCloseFriend(rel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    profile: VibeProfile,
    onSave: (String, String, String, String?, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(profile.displayName) }
    var bio by remember { mutableStateOf(profile.bio) }
    var theme by remember { mutableStateOf(profile.cardTheme) }
    var emoji by remember { mutableStateOf(profile.avatarEmoji) }
    var avatarImg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            avatarImg = ImageUtils.uriToCompressedDataUri(context, uri, 360, 70)
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Edit @${profile.username}", style = MaterialTheme.typography.headlineMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileAvatar(avatarImg ?: profile.avatarImage, emoji, theme, size = 60.dp)
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ) { Text("Change Photo") }
            }
            OutlinedTextField(value = name, onValueChange = { name = it.take(50) }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bio, onValueChange = { bio = it.take(240) }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                VibeCardThemes.forEach { t ->
                    val picked = theme == t.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(t.primary, t.secondary)))
                            .border(if (picked) 2.dp else 0.dp, CyberCyan, RoundedCornerShape(10.dp))
                            .clickable { theme = t.id },
                        contentAlignment = Alignment.Center
                    ) {
                        if (picked) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, bio, theme, avatarImg, emoji) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Save Changes") }
        }
    }
}

@Composable
fun ProfileManagementScreen(
    viewModel: VibeMainViewModel,
    phoneDisplay: String,
    profiles: List<VibeProfile>,
    loggedOutProfileIds: Set<String>,
    activeProfileId: String?,
    onBack: () -> Unit,
    onCreateProfileRequest: () -> Unit,
    onOpenLoginSheet: (String) -> Unit,
    onRecoverPasswordClick: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var deletingProfile by remember { mutableStateOf<VibeProfile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Accounts Center", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(
                    onClick = { onOpenLoginSheet("") },
                    modifier = Modifier.testTag("mgmt_login_existing_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = "Log into existing account", tint = NeonViolet)
                }
                IconButton(
                    onClick = onCreateProfileRequest,
                    modifier = Modifier.testTag("mgmt_add_profile_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create new profile", tint = CyberCyan)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Verified Mobile Identity", style = MaterialTheme.typography.labelLarge, color = CyberCyan)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            phoneDisplay.ifBlank { "Linked Google Identity" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${profiles.size} / 10 independent VIBE accounts under this mobile identity.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(profiles, key = { it.id }) { profile ->
                val isActive = profile.id == activeProfileId
                val isLoggedOut = profile.id in loggedOutProfileIds
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            when {
                                isActive -> CyberCyan
                                isLoggedOut -> HotMagenta.copy(alpha = 0.55f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            },
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatar(
                                profile.avatarImage,
                                profile.avatarEmoji,
                                profile.cardTheme,
                                size = 46.dp,
                                isOnline = !isLoggedOut && !profile.isDeactivated
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                val statusText = when {
                                    isLoggedOut -> "Logged Out"
                                    profile.isDeactivated -> "Deactivated"
                                    isActive -> "Active Now"
                                    else -> "Logged In • Ready"
                                }
                                Text(
                                    "@${profile.username} • $statusText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isLoggedOut || profile.isDeactivated) HotMagenta else CyberCyan
                                )
                            }
                            if (isLoggedOut) {
                                Button(
                                    onClick = { onOpenLoginSheet(profile.username) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("mgmt_login_${profile.username}")
                                ) {
                                    Text("Log In")
                                }
                            } else if (!isActive) {
                                Button(
                                    onClick = { viewModel.switchProfile(profile) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Switch")
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isLoggedOut) {
                                OutlinedButton(
                                    onClick = { onRecoverPasswordClick(profile.username) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Recover")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.logoutIndividualProfile(profile) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("mgmt_logout_${profile.username}")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Log Out")
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.toggleProfileDeactivated(profile) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (profile.isDeactivated) Icons.Default.PlayCircleOutline else Icons.Default.PauseCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (profile.isDeactivated) "Reactivate" else "Deactivate")
                            }
                        }

                        if (profiles.size > 1) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { deletingProfile = profile },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HotMagenta),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Delete @${profile.username}")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HotMagenta),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out Verified Device Identity")
                }
            }
        }
    }

    deletingProfile?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingProfile = null },
            title = { Text("Delete @${target.username}?") },
            text = { Text("This removes the profile and releases its @username permanently without affecting your other VIBE profiles.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProfile(target)
                        deletingProfile = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotMagenta)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingProfile = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CreateProfileDialog(
    isBusy: Boolean,
    onCheckUsername: suspend (String) -> Boolean?,
    onCreate: (username: String, displayName: String, bio: String, emoji: String, theme: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var checkingUsername by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var bio by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🌙") }
    var theme by remember { mutableStateOf("cyber_cyan") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(username) {
        if (username.length >= 3) {
            checkingUsername = true
            usernameAvailable = null
            delay(350)
            usernameAvailable = onCheckUsername(username)
            checkingUsername = false
        } else {
            checkingUsername = false
            usernameAvailable = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New VIBE Account") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Create another independent VIBE account with its own username & password under your verified mobile number.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(30)
                        errorMsg = null
                    },
                    label = { Text("Username") },
                    prefix = { Text("@") },
                    singleLine = true,
                    isError = usernameAvailable == false,
                    supportingText = {
                        when {
                            checkingUsername -> Text("Checking availability...", color = CyberCyan)
                            usernameAvailable == true -> Text("@$username is available", color = EmeraldCloseFriend)
                            usernameAvailable == false -> Text("@$username is already taken", color = HotMagenta)
                        }
                    },
                    modifier = Modifier.testTag("dialog_new_username")
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it.take(50)
                        errorMsg = null
                    },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.testTag("dialog_new_displayname")
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it.take(64)
                        errorMsg = null
                    },
                    label = { Text("Password (min 6 chars)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.testTag("dialog_new_password")
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it.take(64)
                        errorMsg = null
                    },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                    modifier = Modifier.testTag("dialog_confirm_password")
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(240) },
                    label = { Text("Bio") },
                    maxLines = 2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("🌙", "🎧", "🔥", "⚡", "✨").forEach { em ->
                        Surface(
                            color = if (emoji == em) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp).clickable { emoji = em }
                        ) { Box(contentAlignment = Alignment.Center) { Text(em, fontSize = 18.sp) } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    VibeCardThemes.forEach { t ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(t.primary, t.secondary)))
                                .border(if (theme == t.id) 2.dp else 0.dp, CyberCyan, RoundedCornerShape(8.dp))
                                .clickable { theme = t.id }
                        )
                    }
                }
                if (!errorMsg.isNullOrBlank()) {
                    Text(errorMsg.orEmpty(), color = HotMagenta, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        username.length < 3 -> errorMsg = "Username must be 3+ chars"
                        usernameAvailable == false -> errorMsg = "@$username is already taken"
                        displayName.isBlank() -> errorMsg = "Enter a display name"
                        password.length < 6 -> errorMsg = "Password must be at least 6 chars"
                        password != confirmPassword -> errorMsg = "Passwords do not match"
                        else -> onCreate(username, displayName, bio, emoji, theme, password)
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.testTag("dialog_confirm_create_profile")
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
