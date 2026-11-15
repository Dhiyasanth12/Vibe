package com.example

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.UiState
import com.example.data.repository.ActiveProfileStore
import com.example.data.repository.VibeAiRepository
import com.example.data.repository.VibeRepository
import com.example.ui.components.ProfileSwitcherSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatRoomScreen
import com.example.ui.screens.ChatsListScreen
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.CreateProfileDialog
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.MobileAndProfileSetupScreen
import com.example.ui.screens.MobilePasswordRecoveryDialog
import com.example.ui.screens.ProfileAccountsLoginScreen
import com.example.ui.screens.ProfileManagementScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReelsPlayerScreen
import com.example.ui.screens.UsernamePasswordLoginSheet
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.VibeTheme
import com.example.ui.viewmodel.VibeMainViewModel
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object VibeRoutes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val CREATE = "create"
    const val CHATS = "chats"
    const val PROFILE = "profile"
    const val REELS = "reels"
    const val MANAGEMENT = "profile_management"
    const val CHAT_ROOM = "chat_room/{conversationId}"
    fun chatRoom(conversationId: String) = "chat_room/$conversationId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before UI/ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            VibeRootApp()
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun VibeRootApp(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        VibeTheme(darkTheme = true) {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            VibeAuthenticatedShell(currentUserId = user.uid)
        }
    }
}

@Composable
fun VibeAuthenticatedShell(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: VibeMainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                VibeMainViewModel(
                    repository = VibeRepository(db),
                    profileStore = ActiveProfileStore(app),
                    aiRepository = VibeAiRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val accountState by viewModel.userAccount.collectAsStateWithLifecycle()
    val myProfilesState by viewModel.myProfiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val loggedOutProfileIds by viewModel.loggedOutProfileIds.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var loginSheetUsername by remember { mutableStateOf<String?>(null) }
    var recoveryUsername by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val performSignOut: () -> Unit = {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("VibeAuth", "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                }
            }
        }
    }

    VibeTheme(darkTheme = isDarkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val profilesUi = myProfilesState) {
                is UiState.Loading -> {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(profilesUi.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                is UiState.Success -> {
                    val profiles = profilesUi.data
                    val account = (accountState as? UiState.Success)?.data
                    val profile = activeProfile
                    val phoneDisplay = if (account != null && account.phoneNumber.isNotBlank()) {
                        "${account.phoneCountryCode} ${account.phoneNumber}"
                    } else ""

                    if (profiles.isEmpty()) {
                        MobileAndProfileSetupScreen(
                            existingAccount = account,
                            isBusy = isBusy,
                            onCheckUsername = { viewModel.checkUsernameAvailability(it) },
                            onSavePhone = { code, num, verified ->
                                viewModel.savePhoneAccountAndContinue(code, num, verified)
                            },
                            onCreateFirstProfile = { username, displayName, bio, emoji, avatar, theme, mood, moodEmoji, password ->
                                viewModel.createProfile(
                                    username = username,
                                    displayName = displayName,
                                    bio = bio,
                                    avatarEmoji = emoji,
                                    avatarImage = avatar,
                                    cardTheme = theme,
                                    moodStatus = mood,
                                    moodEmoji = moodEmoji,
                                    password = password
                                )
                            },
                            onSignOut = performSignOut
                        )
                    } else if (profile == null) {
                        ProfileAccountsLoginScreen(
                            profiles = profiles,
                            phoneDisplay = phoneDisplay,
                            isBusy = isBusy,
                            onLoginWithPassword = { username, password, onSuccess, onError ->
                                viewModel.loginWithUsernameAndPassword(username, password, onSuccess, onError)
                            },
                            onRecoverPasswordClick = { prefill -> recoveryUsername = prefill },
                            onCreateNewProfileClick = { showCreateProfileDialog = true },
                            onSignOutRoot = performSignOut
                        )
                    } else {
                        val navController = rememberNavController()
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = backStackEntry?.destination?.route

                        var showSwitcherSheet by remember { mutableStateOf(false) }
                        var createInitialMode by remember { mutableStateOf("Post") }

                    val showBottomBar = currentRoute in setOf(
                        VibeRoutes.HOME,
                        VibeRoutes.EXPLORE,
                        VibeRoutes.CREATE,
                        VibeRoutes.CHATS,
                        VibeRoutes.PROFILE
                    )

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    NavigationBarItem(
                                        selected = currentRoute == VibeRoutes.HOME,
                                        onClick = {
                                            navController.navigate(VibeRoutes.HOME) {
                                                popUpTo(VibeRoutes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                if (currentRoute == VibeRoutes.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                                contentDescription = "Home"
                                            )
                                        },
                                        label = { Text("Home") },
                                        modifier = Modifier.testTag("nav_home"),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = CyberCyan,
                                            indicatorColor = ElectricPurple.copy(alpha = 0.25f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == VibeRoutes.EXPLORE,
                                        onClick = {
                                            navController.navigate(VibeRoutes.EXPLORE) {
                                                popUpTo(VibeRoutes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                if (currentRoute == VibeRoutes.EXPLORE) Icons.Filled.Explore else Icons.Outlined.Explore,
                                                contentDescription = "Explore"
                                            )
                                        },
                                        label = { Text("Explore") },
                                        modifier = Modifier.testTag("nav_explore"),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = CyberCyan,
                                            indicatorColor = ElectricPurple.copy(alpha = 0.25f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == VibeRoutes.CREATE,
                                        onClick = {
                                            createInitialMode = "Post"
                                            navController.navigate(VibeRoutes.CREATE) {
                                                popUpTo(VibeRoutes.HOME) { saveState = true }
                                                launchSingleTop = true
                                            }
                                        },
                                        icon = {
                                            Surface(
                                                color = ElectricPurple,
                                                shape = CircleShape,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Filled.Add, contentDescription = "Create", tint = Color.White)
                                                }
                                            }
                                        },
                                        label = { Text("Create") },
                                        modifier = Modifier.testTag("nav_create")
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == VibeRoutes.CHATS,
                                        onClick = {
                                            navController.navigate(VibeRoutes.CHATS) {
                                                popUpTo(VibeRoutes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                if (currentRoute == VibeRoutes.CHATS) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                                                contentDescription = "Chats"
                                            )
                                        },
                                        label = { Text("Chats") },
                                        modifier = Modifier.testTag("nav_chats"),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = CyberCyan,
                                            indicatorColor = ElectricPurple.copy(alpha = 0.25f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == VibeRoutes.PROFILE,
                                        onClick = {
                                            navController.navigate(VibeRoutes.PROFILE) {
                                                popUpTo(VibeRoutes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                if (currentRoute == VibeRoutes.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                                contentDescription = "Profile"
                                            )
                                        },
                                        label = { Text("Profile") },
                                        modifier = Modifier.testTag("nav_profile"),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = CyberCyan,
                                            indicatorColor = ElectricPurple.copy(alpha = 0.25f)
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = VibeRoutes.HOME,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable(VibeRoutes.HOME) {
                                HomeFeedScreen(
                                    viewModel = viewModel,
                                    activeProfile = profile,
                                    onOpenSwitcher = { showSwitcherSheet = true },
                                    onOpenCreateStory = {
                                        createInitialMode = "Story"
                                        navController.navigate(VibeRoutes.CREATE)
                                    },
                                    onOpenCreatePost = {
                                        createInitialMode = "Post"
                                        navController.navigate(VibeRoutes.CREATE)
                                    },
                                    onOpenReels = { navController.navigate(VibeRoutes.REELS) },
                                    onOpenAudioRooms = { navController.navigate(VibeRoutes.EXPLORE) },
                                    onOpenConversation = { convId ->
                                        navController.navigate(VibeRoutes.chatRoom(convId))
                                    }
                                )
                            }
                            composable(VibeRoutes.EXPLORE) {
                                ExploreScreen(
                                    viewModel = viewModel,
                                    activeProfile = profile,
                                    onOpenConversation = { convId ->
                                        navController.navigate(VibeRoutes.chatRoom(convId))
                                    }
                                )
                            }
                            composable(VibeRoutes.CREATE) {
                                CreatePostScreen(
                                    viewModel = viewModel,
                                    activeProfile = profile,
                                    initialMode = createInitialMode,
                                    onPublished = {
                                        navController.navigate(VibeRoutes.HOME) {
                                            popUpTo(VibeRoutes.HOME) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(VibeRoutes.CHATS) {
                                ChatsListScreen(
                                    viewModel = viewModel,
                                    activeProfile = profile,
                                    onOpenConversation = { convId ->
                                        navController.navigate(VibeRoutes.chatRoom(convId))
                                    }
                                )
                            }
                            composable(VibeRoutes.PROFILE) {
                                ProfileScreen(
                                    viewModel = viewModel,
                                    activeProfile = profile,
                                    allMyProfiles = profiles,
                                    loggedOutProfileIds = loggedOutProfileIds,
                                    isDarkTheme = isDarkTheme,
                                    onOpenSwitcher = { showSwitcherSheet = true },
                                    onOpenProfileManagement = { navController.navigate(VibeRoutes.MANAGEMENT) },
                                    onOpenCreateProfile = { showCreateProfileDialog = true },
                                    onOpenLoginForProfile = { uname -> loginSheetUsername = uname }
                                )
                            }
                            composable(VibeRoutes.REELS) {
                                ReelsPlayerScreen(
                                    viewModel = viewModel,
                                    activeProfile = profile,
                                    onBack = { navController.popBackStack() },
                                    onCreateReel = {
                                        createInitialMode = "Reel"
                                        navController.navigate(VibeRoutes.CREATE)
                                    }
                                )
                            }
                            composable(VibeRoutes.MANAGEMENT) {
                                ProfileManagementScreen(
                                    viewModel = viewModel,
                                    phoneDisplay = phoneDisplay,
                                    profiles = profiles,
                                    loggedOutProfileIds = loggedOutProfileIds,
                                    activeProfileId = profile.id,
                                    onBack = { navController.popBackStack() },
                                    onCreateProfileRequest = { showCreateProfileDialog = true },
                                    onOpenLoginSheet = { uname -> loginSheetUsername = uname },
                                    onRecoverPasswordClick = { uname -> recoveryUsername = uname },
                                    onSignOut = performSignOut
                                )
                            }
                            composable(
                                route = VibeRoutes.CHAT_ROOM,
                                arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
                            ) { entry ->
                                val convId = entry.arguments?.getString("conversationId").orEmpty()
                                ChatRoomScreen(
                                    mainViewModel = viewModel,
                                    activeProfile = profile,
                                    conversationId = convId,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                        if (showSwitcherSheet) {
                            ProfileSwitcherSheet(
                                profiles = profiles,
                                loggedOutProfileIds = loggedOutProfileIds,
                                activeProfileId = profile.id,
                                phoneDisplay = phoneDisplay,
                                onSelectProfile = { viewModel.switchProfile(it) },
                                onLoginToProfile = { loginSheetUsername = it.username },
                                onLogoutProfile = { viewModel.logoutIndividualProfile(it) },
                                onAddExistingAccount = { loginSheetUsername = "" },
                                onCreateNewProfile = { showCreateProfileDialog = true },
                                onManageProfiles = { navController.navigate(VibeRoutes.MANAGEMENT) },
                                onDismiss = { showSwitcherSheet = false }
                            )
                        }
                    }

                    if (showCreateProfileDialog) {
                        CreateProfileDialog(
                            isBusy = isBusy,
                            onCheckUsername = { viewModel.checkUsernameAvailability(it) },
                            onCreate = { username, displayName, bio, emoji, theme, password ->
                                viewModel.createProfile(
                                    username = username,
                                    displayName = displayName,
                                    bio = bio,
                                    avatarEmoji = emoji,
                                    avatarImage = "",
                                    cardTheme = theme,
                                    moodStatus = "Vibing",
                                    moodEmoji = "🎧",
                                    password = password
                                ) {
                                    showCreateProfileDialog = false
                                }
                            },
                            onDismiss = { showCreateProfileDialog = false }
                        )
                    }

                    loginSheetUsername?.let { targetUsername ->
                        UsernamePasswordLoginSheet(
                            initialUsername = targetUsername,
                            isBusy = isBusy,
                            onLogin = { username, password, onSuccess, onError ->
                                viewModel.loginWithUsernameAndPassword(username, password, onSuccess, onError)
                            },
                            onForgotPassword = { prefill -> recoveryUsername = prefill },
                            onDismiss = { loginSheetUsername = null }
                        )
                    }

                    recoveryUsername?.let { targetUsername ->
                        MobilePasswordRecoveryDialog(
                            initialUsername = targetUsername,
                            phoneHint = phoneDisplay,
                            isBusy = isBusy,
                            onRecover = { username, newPassword, mobileDigits, onSuccess, onError ->
                                viewModel.recoverPasswordWithMobile(username, newPassword, mobileDigits, onSuccess, onError)
                            },
                            onDismiss = { recoveryUsername = null }
                        )
                    }
                }
            }

            if (activeProfile == null) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
