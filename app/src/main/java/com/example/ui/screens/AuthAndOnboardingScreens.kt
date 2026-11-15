package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.CountryCodeOption
import com.example.data.model.SupportedCountryCodes
import com.example.data.model.UserAccount
import com.example.data.model.VibeMoodOptions
import com.example.data.model.VibeProfile
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.EmeraldCloseFriend
import com.example.ui.theme.HotMagenta
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.VibeCardThemes
import com.example.util.ImageUtils
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

private const val AUTH_TAG = "VibeAuth"

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPhoneOtpPanel by remember { mutableStateOf(false) }

    // Firebase Phone Auth state
    var selectedCountry by remember { mutableStateOf(SupportedCountryCodes.first()) }
    var phoneDigits by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var phoneStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_vibe_hero_backdrop),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.55f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MidnightBlack.copy(alpha = 0.35f),
                            MidnightBlack.copy(alpha = 0.88f),
                            MidnightBlack
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = ElectricPurple.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .size(84.dp)
                            .border(1.5.dp, NeonViolet.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "V",
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "VIBE",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Next-gen social network, real-time encrypted-feel chats, stories, audio rooms & multi-profile switching.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    Surface(
                        color = Color(0xFF141022).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "One Identity • Multiple VIBE Profiles",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CyberCyan
                                )
                            }

                            Spacer(Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    errorMessage = null
                                    isLoading = true
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isLoading = false
                                            onAuthSuccess()
                                        },
                                        onAuthError = { msg ->
                                            isLoading = false
                                            errorMessage = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = { isLoading = false }
                                    )
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("google_sign_in_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Continue with Google",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            TextButton(
                                onClick = { showPhoneOtpPanel = !showPhoneOtpPanel },
                                modifier = Modifier.testTag("toggle_phone_auth_button")
                            ) {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = NeonViolet
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (showPhoneOtpPanel) "Hide Mobile OTP Login" else "Sign in with Mobile Number OTP (+91)",
                                    color = NeonViolet
                                )
                            }

                            AnimatedVisibility(visible = showPhoneOtpPanel) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    PhoneOtpEntryBlock(
                                        selectedCountry = selectedCountry,
                                        onSelectCountry = { selectedCountry = it },
                                        phoneDigits = phoneDigits,
                                        onPhoneChange = { phoneDigits = it },
                                        otpCode = otpCode,
                                        onOtpChange = { otpCode = it },
                                        verificationId = verificationId,
                                        isBusy = isLoading,
                                        statusText = phoneStatus,
                                        onSendOtp = {
                                            val activity = context.findActivity()
                                            if (activity == null) {
                                                errorMessage = "Unable to resolve active screen for OTP verification."
                                                return@PhoneOtpEntryBlock
                                            }
                                            val clean = phoneDigits.filter(Char::isDigit)
                                            val e164 = "${selectedCountry.dialCode}$clean"
                                            if (clean.length < 7) {
                                                errorMessage = "Enter a valid mobile number"
                                                return@PhoneOtpEntryBlock
                                            }
                                            isLoading = true
                                            errorMessage = null
                                            phoneStatus = "Sending OTP to $e164..."
                                            try {
                                                val options = PhoneAuthOptions.newBuilder(Firebase.auth)
                                                    .setPhoneNumber(e164)
                                                    .setTimeout(60L, TimeUnit.SECONDS)
                                                    .setActivity(activity)
                                                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                                            scope.launch {
                                                                try {
                                                                    Firebase.auth.signInWithCredential(credential).await()
                                                                    isLoading = false
                                                                    onAuthSuccess()
                                                                } catch (e: Exception) {
                                                                    isLoading = false
                                                                    errorMessage = e.localizedMessage
                                                                }
                                                            }
                                                        }

                                                        override fun onVerificationFailed(e: FirebaseException) {
                                                            isLoading = false
                                                            phoneStatus = null
                                                            errorMessage = formatPhoneAuthError(e)
                                                        }

                                                        override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                                                            isLoading = false
                                                            verificationId = verId
                                                            phoneStatus = "OTP sent to $e164"
                                                        }
                                                    })
                                                    .build()
                                                PhoneAuthProvider.verifyPhoneNumber(options)
                                            } catch (e: Exception) {
                                                isLoading = false
                                                phoneStatus = null
                                                errorMessage = formatPhoneAuthError(e)
                                            }
                                        },
                                        onVerifyOtp = {
                                            val verId = verificationId ?: return@PhoneOtpEntryBlock
                                            if (otpCode.length < 6) {
                                                errorMessage = "Enter 6-digit OTP"
                                                return@PhoneOtpEntryBlock
                                            }
                                            isLoading = true
                                            errorMessage = null
                                            val cred = PhoneAuthProvider.getCredential(verId, otpCode.trim())
                                            scope.launch {
                                                try {
                                                    Firebase.auth.signInWithCredential(cred).await()
                                                    isLoading = false
                                                    onAuthSuccess()
                                                } catch (e: Exception) {
                                                    isLoading = false
                                                    errorMessage = e.localizedMessage ?: "Invalid OTP"
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            if (!errorMessage.isNullOrBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFFF8FA3),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Secured by Firebase Authentication & App Check",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MobileAndProfileSetupScreen(
    existingAccount: UserAccount?,
    isBusy: Boolean,
    onCheckUsername: suspend (String) -> Boolean?,
    onSavePhone: (countryCode: String, phone: String, verified: Boolean) -> Unit,
    onCreateFirstProfile: (
        username: String,
        displayName: String,
        bio: String,
        emoji: String,
        avatarImage: String,
        cardTheme: String,
        mood: String,
        moodEmoji: String,
        password: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var phoneStepDone by remember(existingAccount) {
        mutableStateOf(existingAccount != null && existingAccount.phoneNumber.isNotBlank())
    }
    var selectedCountry by remember { mutableStateOf(SupportedCountryCodes.first()) }
    var phoneDigits by remember { mutableStateOf(existingAccount?.phoneNumber.orEmpty()) }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var phoneBusy by remember { mutableStateOf(false) }
    var phoneMessage by remember { mutableStateOf<String?>(null) }

    // Profile creation fields
    var username by remember { mutableStateOf("") }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var checkingUsername by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var bio by remember { mutableStateOf("Vibing on next-gen social ⚡") }
    var selectedEmoji by remember { mutableStateOf("⚡") }
    var avatarImageData by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf("electric_purple") }
    var selectedMood by remember { mutableStateOf(VibeMoodOptions.first()) }
    var localError by remember { mutableStateOf<String?>(null) }

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

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            avatarImageData = ImageUtils.uriToCompressedDataUri(context, uri, 360, 70).orEmpty()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (!phoneStepDone) "Step 1 of 2 • Mobile Identity" else "Step 2 of 2 • First VIBE Profile",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan
                )
                TextButton(onClick = onSignOut) {
                    Text("Sign Out", color = HotMagenta)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!phoneStepDone) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Link Your Mobile Number",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "One verified mobile number unlocks unlimited VIBE profiles without logging out. Default region: India (+91).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))

                        PhoneOtpEntryBlock(
                            selectedCountry = selectedCountry,
                            onSelectCountry = { selectedCountry = it },
                            phoneDigits = phoneDigits,
                            onPhoneChange = { phoneDigits = it },
                            otpCode = otpCode,
                            onOtpChange = { otpCode = it },
                            verificationId = verificationId,
                            isBusy = phoneBusy,
                            statusText = phoneMessage,
                            onSendOtp = {
                                val activity = context.findActivity()
                                if (activity == null) {
                                    localError = "Unable to resolve active screen for OTP verification."
                                    return@PhoneOtpEntryBlock
                                }
                                val clean = phoneDigits.filter(Char::isDigit)
                                if (clean.length < 7) {
                                    localError = "Enter a valid mobile number"
                                    return@PhoneOtpEntryBlock
                                }
                                localError = null
                                phoneBusy = true
                                val e164 = "${selectedCountry.dialCode}$clean"
                                phoneMessage = "Sending SMS OTP to $e164..."
                                try {
                                    val options = PhoneAuthOptions.newBuilder(Firebase.auth)
                                        .setPhoneNumber(e164)
                                        .setTimeout(60L, TimeUnit.SECONDS)
                                        .setActivity(activity)
                                        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                                scope.launch {
                                                    try {
                                                        Firebase.auth.currentUser?.linkWithCredential(credential)?.await()
                                                        phoneBusy = false
                                                        onSavePhone(selectedCountry.dialCode, clean, true)
                                                        phoneStepDone = true
                                                    } catch (_: Exception) {
                                                        phoneBusy = false
                                                        onSavePhone(selectedCountry.dialCode, clean, true)
                                                        phoneStepDone = true
                                                    }
                                                }
                                            }

                                            override fun onVerificationFailed(e: FirebaseException) {
                                                phoneBusy = false
                                                phoneMessage = formatPhoneAuthError(e) + " Tap 'Save ${selectedCountry.dialCode} Number & Continue' below to keep going."
                                            }

                                            override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                                                phoneBusy = false
                                                verificationId = verId
                                                phoneMessage = "OTP sent to $e164"
                                            }
                                        })
                                        .build()
                                    PhoneAuthProvider.verifyPhoneNumber(options)
                                } catch (e: Exception) {
                                    phoneBusy = false
                                    phoneMessage = formatPhoneAuthError(e) + " Tap 'Save ${selectedCountry.dialCode} Number & Continue' below to keep going."
                                }
                            },
                            onVerifyOtp = {
                                val verId = verificationId ?: return@PhoneOtpEntryBlock
                                val clean = phoneDigits.filter(Char::isDigit)
                                phoneBusy = true
                                val cred = PhoneAuthProvider.getCredential(verId, otpCode.trim())
                                scope.launch {
                                    try {
                                        Firebase.auth.currentUser?.linkWithCredential(cred)?.await()
                                        phoneBusy = false
                                        onSavePhone(selectedCountry.dialCode, clean, true)
                                        phoneStepDone = true
                                    } catch (e: Exception) {
                                        phoneBusy = false
                                        localError = e.localizedMessage ?: "OTP verification failed"
                                    }
                                }
                            }
                        )

                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val clean = phoneDigits.filter(Char::isDigit)
                                if (clean.length < 7) {
                                    localError = "Enter at least 7 digits for your mobile number"
                                    return@OutlinedButton
                                }
                                localError = null
                                onSavePhone(selectedCountry.dialCode, clean, false)
                                phoneStepDone = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_phone_continue_button")
                        ) {
                            Text("Save ${selectedCountry.dialCode} Number & Continue")
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Craft Your VIBE Persona",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "You can create additional independent profiles anytime from your Profile Switcher.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))
                        Box(contentAlignment = Alignment.BottomEnd) {
                            ProfileAvatar(
                                avatarImage = avatarImageData,
                                avatarEmoji = selectedEmoji,
                                cardTheme = selectedTheme,
                                size = 88.dp,
                                hasStoryRing = true
                            )
                            Surface(
                                color = ElectricPurple,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable {
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = "Choose profile photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        val emojis = listOf("⚡", "🌙", "🎧", "🔥", "🦋", "✨", "🚀", "💎")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            emojis.forEach { em ->
                                Surface(
                                    color = if (selectedEmoji == em && avatarImageData.isBlank()) ElectricPurple else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable {
                                            avatarImageData = ""
                                            selectedEmoji = em
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) { Text(em, fontSize = 18.sp) }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(30) },
                            label = { Text("Unique Handle (e.g. arjun_vibes)") },
                            prefix = { Text("@") },
                            singleLine = true,
                            isError = usernameAvailable == false,
                            supportingText = {
                                when {
                                    username.isNotBlank() && username.length < 3 ->
                                        Text("At least 3 characters (a-z, 0-9, _, .)")
                                    checkingUsername ->
                                        Text("Checking availability...", color = CyberCyan)
                                    usernameAvailable == true ->
                                        Text("@$username is available", color = EmeraldCloseFriend)
                                    usernameAvailable == false ->
                                        Text("@$username is already taken", color = HotMagenta)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_username_input")
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it.take(50) },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_displayname_input")
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it.take(64) },
                            label = { Text("Account Password (min 6 chars)") },
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_password_input")
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it.take(64) },
                            label = { Text("Confirm Password") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_confirm_password_input")
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it.take(240) },
                            label = { Text("Bio") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Animated Card Theme",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VibeCardThemes.forEach { theme ->
                                val picked = selectedTheme == theme.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(listOf(theme.primary, theme.secondary)))
                                        .border(
                                            width = if (picked) 2.5.dp else 1.dp,
                                            color = if (picked) CyberCyan else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedTheme = theme.id },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (picked) {
                                        Icon(Icons.Default.Check, contentDescription = theme.label, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = {
                                when {
                                    username.length < 3 ->
                                        localError = "Enter a unique username (3+ characters)"
                                    usernameAvailable == false ->
                                        localError = "@$username is already taken. Choose another handle."
                                    displayName.isBlank() ->
                                        localError = "Enter your display name"
                                    password.length < 6 ->
                                        localError = "Create a password of at least 6 characters"
                                    password != confirmPassword ->
                                        localError = "Passwords do not match"
                                    else -> {
                                        localError = null
                                        onCreateFirstProfile(
                                            username,
                                            displayName,
                                            bio,
                                            selectedEmoji,
                                            avatarImageData,
                                            selectedTheme,
                                            selectedMood.first,
                                            selectedMood.second,
                                            password
                                        )
                                    }
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("create_first_profile_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Create Account & Enter VIBE")
                            }
                        }
                    }
                }
            }

            if (!localError.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = localError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ProfileAccountsLoginScreen(
    profiles: List<VibeProfile>,
    phoneDisplay: String,
    isBusy: Boolean,
    onLoginWithPassword: (username: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onRecoverPasswordClick: (prefillUsername: String) -> Unit,
    onCreateNewProfileClick: () -> Unit,
    onSignOutRoot: () -> Unit
) {
    var username by remember(profiles) { mutableStateOf(profiles.firstOrNull()?.username.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("VIBE Accounts", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = onSignOutRoot,
                    modifier = Modifier.testTag("sign_out_root_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = HotMagenta, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Switch Device ID", color = HotMagenta)
                }
            }

            if (phoneDisplay.isNotBlank()) {
                Surface(
                    color = ElectricPurple.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Verified Mobile: $phoneDisplay", style = MaterialTheme.typography.labelLarge, color = CyberCyan)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (profiles.isNotEmpty()) {
                Text(
                    "Select an Account to Log In",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(profiles, key = { it.id }) { p ->
                        val picked = p.username.equals(username, ignoreCase = true)
                        Surface(
                            color = if (picked) ElectricPurple.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .border(
                                    width = if (picked) 1.5.dp else 1.dp,
                                    color = if (picked) CyberCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    username = p.username
                                    errorMessage = null
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ProfileAvatar(p.avatarImage, p.avatarEmoji, p.cardTheme, size = 52.dp)
                                Spacer(Modifier.height(6.dp))
                                Text(p.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                Text("@${p.username}", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Log in with Username & Password",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Enter your VIBE handle and account password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(30)
                            errorMessage = null
                        },
                        label = { Text("Username") },
                        prefix = { Text("@") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_login_username")
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it.take(64)
                            errorMessage = null
                        },
                        label = { Text("Password") },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_login_password")
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage.orEmpty(), color = HotMagenta, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                errorMessage = "Enter both username and password"
                                return@Button
                            }
                            onLoginWithPassword(
                                username,
                                password,
                                { errorMessage = null },
                                { err -> errorMessage = err }
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("account_login_submit")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Log In")
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = { onRecoverPasswordClick(username) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Forgot password? Recover with Mobile", color = NeonViolet)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onCreateNewProfileClick,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_another_profile_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create New VIBE Account")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernamePasswordLoginSheet(
    initialUsername: String,
    isBusy: Boolean,
    onLogin: (username: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onForgotPassword: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember(initialUsername) { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

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
            Text("Log into VIBE Account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Enter the username and password for the VIBE account under your verified mobile identity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(30)
                    errorText = null
                },
                label = { Text("Username") },
                prefix = { Text("@") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheet_login_username")
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it.take(64)
                    errorText = null
                },
                label = { Text("Password") },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheet_login_password")
            )
            if (!errorText.isNullOrBlank()) {
                Text(errorText.orEmpty(), color = HotMagenta, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        errorText = "Enter username and password"
                        return@Button
                    }
                    onLogin(
                        username,
                        password,
                        { onDismiss() },
                        { err -> errorText = err }
                    )
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sheet_login_submit")
            ) {
                Text("Log In")
            }
            TextButton(
                onClick = {
                    onDismiss()
                    onForgotPassword(username)
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Forgot password? Recover via Mobile", color = NeonViolet)
            }
        }
    }
}

@Composable
fun MobilePasswordRecoveryDialog(
    initialUsername: String,
    phoneHint: String,
    isBusy: Boolean,
    onRecover: (username: String, newPassword: String, mobileDigits: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember(initialUsername) { mutableStateOf(initialUsername) }
    var mobileDigits by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recover Account Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (phoneHint.isNotBlank()) {
                        "Verify your registered mobile number ($phoneHint) to reset the password for your VIBE profile."
                    } else {
                        "Confirm your registered mobile number to reset your VIBE account password."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(30)
                        errorText = null
                    },
                    label = { Text("Username") },
                    prefix = { Text("@") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobileDigits,
                    onValueChange = {
                        mobileDigits = it.filter(Char::isDigit).take(15)
                        errorText = null
                    },
                    label = { Text("Registered Mobile Number") },
                    placeholder = { Text("Enter mobile digits") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it.take(64)
                        errorText = null
                    },
                    label = { Text("New Password (min 6 chars)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it.take(64)
                        errorText = null
                    },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!errorText.isNullOrBlank()) {
                    Text(errorText.orEmpty(), color = HotMagenta, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        username.length < 3 -> errorText = "Enter valid username"
                        mobileDigits.length < 6 -> errorText = "Enter your registered mobile number"
                        newPassword.length < 6 -> errorText = "New password must be at least 6 characters"
                        newPassword != confirmPassword -> errorText = "Passwords do not match"
                        else -> onRecover(
                            username,
                            newPassword,
                            mobileDigits,
                            { onDismiss() },
                            { err -> errorText = err }
                        )
                    }
                },
                enabled = !isBusy
            ) {
                Text("Reset & Log In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PhoneOtpEntryBlock(
    selectedCountry: CountryCodeOption,
    onSelectCountry: (CountryCodeOption) -> Unit,
    phoneDigits: String,
    onPhoneChange: (String) -> Unit,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    verificationId: String?,
    isBusy: Boolean,
    statusText: String?,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(56.dp)
                    .clickable { expanded = true }
                    .testTag("country_code_selector")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${selectedCountry.flag} ${selectedCountry.dialCode}", style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select country code")
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SupportedCountryCodes.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("${option.flag} ${option.name} (${option.dialCode})") },
                        onClick = {
                            onSelectCountry(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = phoneDigits,
            onValueChange = { onPhoneChange(it.filter(Char::isDigit).take(15)) },
            label = { Text("Mobile Number") },
            placeholder = { Text("9876543210") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
                .weight(1f)
                .testTag("phone_number_input")
        )
    }

    Spacer(Modifier.height(10.dp))
    Button(
        onClick = onSendOtp,
        enabled = !isBusy,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("send_otp_button")
    ) {
        Text(if (verificationId == null) "Send Firebase OTP" else "Resend OTP")
    }

    if (verificationId != null) {
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = otpCode,
            onValueChange = { onOtpChange(it.filter(Char::isDigit).take(6)) },
            label = { Text("6-Digit OTP Code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("otp_code_input")
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onVerifyOtp,
            enabled = !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("verify_otp_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MidnightBlack)
        ) {
            Text("Verify OTP", fontWeight = FontWeight.Bold)
        }
    }

    if (!statusText.isNullOrBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = CyberCyan
        )
    }
}

fun attemptAutoSignIn(
    context: android.content.Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: android.content.Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Unable to find host Activity for Google Sign-In")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun formatPhoneAuthError(e: Exception): String {
    val raw = e.localizedMessage.orEmpty()
    val isConfigError = raw.contains("recaptcha", ignoreCase = true) ||
        raw.contains("siteKey", ignoreCase = true) ||
        raw.contains("app verification", ignoreCase = true) ||
        raw.contains("not authorized", ignoreCase = true) ||
        raw.contains("operation is not allowed", ignoreCase = true) ||
        raw.contains("missing", ignoreCase = true)
    return if (isConfigError) {
        "Live SMS OTP requires enabling Phone Sign-In & reCAPTCHA Enterprise / Play Integrity in Firebase Console. Use Continue with Google above for instant access."
    } else {
        "Firebase Phone Auth: ${raw.ifBlank { "Verification unavailable; use Continue with Google." }}"
    }
}
