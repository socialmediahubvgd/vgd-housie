package com.example.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinBright
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private enum class AuthPortalMode {
    LOGIN,
    SIGN_UP
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var authMode by remember { mutableStateOf(AuthPortalMode.LOGIN) }
    var signUpDisplayName by remember { mutableStateOf("") }
    var isCheckingAutoSignIn by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAutoSignIn = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingAutoSignIn = false
            },
            scope = scope
        )
    }

    fun launchGoogleAuth(customName: String?) {
        focusManager.clearFocus()
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
            onAuthCancelled = {
                isLoading = false
            },
            customDisplayName = customName
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianBg, ObsidianSurfaceVariant, ObsidianBg)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .testTag("auth_screen"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ObsidianSurface,
            border = BorderStroke(
                1.2.dp,
                Brush.linearGradient(
                    listOf(GoldCoin.copy(alpha = 0.75f), CyanDiamond.copy(alpha = 0.55f))
                )
            ),
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                GoldContainer.copy(alpha = 0.32f),
                                ObsidianSurface,
                                CyanContainer.copy(alpha = 0.24f)
                            )
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Brand Emblem
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(GoldCoin.copy(alpha = 0.38f), GoldContainer)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Casino,
                        contentDescription = stringResource(R.string.app_name),
                        tint = GoldCoinBright,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = GoldCoinBright,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Royal Housie & Cloud Wallet",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanDiamond
                    )
                }

                // Segmented Login / Sign Up Switcher
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianCardElevated,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AuthModeTabButton(
                            selected = authMode == AuthPortalMode.LOGIN,
                            label = "Login",
                            icon = Icons.AutoMirrored.Rounded.Login,
                            testTag = "auth_mode_login",
                            onClick = {
                                errorMessage = null
                                authMode = AuthPortalMode.LOGIN
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AuthModeTabButton(
                            selected = authMode == AuthPortalMode.SIGN_UP,
                            label = "Sign Up",
                            icon = Icons.Rounded.PersonAdd,
                            testTag = "auth_mode_signup",
                            onClick = {
                                errorMessage = null
                                authMode = AuthPortalMode.SIGN_UP
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AnimatedContent(
                    targetState = authMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "AuthModeContent"
                ) { mode ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (mode == AuthPortalMode.LOGIN) {
                            Text(
                                text = "Apne Google / Gmail account se turant Login karein — aapke Diamonds, Coins aur Game History cloud par safe hain.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            AuthFeatureCard(
                                items = listOf(
                                    Icons.Rounded.CloudSync to "Instant Cloud Sync — Wallet & Exchange history hamesha safe",
                                    Icons.Rounded.Security to "Founder Panel sirf verified vinayakglobaldream@gmail.com ke liye unlock hoga"
                                )
                            )
                        } else {
                            Text(
                                text = "Naya VGD Housie account banayein aur turant 5,000 💎 Welcome Bonus paayein!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = signUpDisplayName,
                                onValueChange = { signUpDisplayName = it.take(40) },
                                label = { Text("Player Name (Optional)") },
                                placeholder = { Text("e.g. Royal Player") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Badge,
                                        contentDescription = null,
                                        tint = GoldCoin
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { launchGoogleAuth(signUpDisplayName) }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldCoin,
                                    unfocusedBorderColor = ObsidianOutline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = GoldCoin,
                                    unfocusedLabelColor = TextSecondary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_display_name_input")
                            )

                            AuthFeatureCard(
                                items = listOf(
                                    Icons.Rounded.Diamond to "5,000 💎 Free Welcome Diamonds on new Google registration",
                                    Icons.Rounded.MailOutline to "Agle step me apni Gmail ID choose karein — bina extra password ke"
                                )
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CoralError.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, CoralError.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = CoralError,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                val isSignUp = authMode == AuthPortalMode.SIGN_UP
                Button(
                    onClick = {
                        launchGoogleAuth(if (isSignUp) signUpDisplayName else null)
                    },
                    enabled = !isLoading && !isCheckingAutoSignIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSignUp) CyanDiamond else GoldCoin,
                        contentColor = TextDark
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag(if (isSignUp) "google_sign_up_button" else "google_sign_in_button")
                ) {
                    if (isLoading || isCheckingAutoSignIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = TextDark,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = if (isCheckingAutoSignIn) {
                                "Checking Google session..."
                            } else if (isSignUp) {
                                "Creating account..."
                            } else {
                                "Signing in..."
                            },
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = if (isSignUp) "Sign up with Google" else "Sign in with Google",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = if (isSignUp) "Sign up with Google" else "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    onClick = {
                        errorMessage = null
                        authMode = if (isSignUp) AuthPortalMode.LOGIN else AuthPortalMode.SIGN_UP
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("auth_mode_switch_link")
                ) {
                    Text(
                        text = if (isSignUp) {
                            "Already have an account? Login with Google"
                        } else {
                            "New player? Create account with Google"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldCoinBright
                    )
                }

                // Clear backend capability disclosure
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianCardElevated.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, ObsidianOutline.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_provider_notice")
                ) {
                    Text(
                        text = "Secured by Google Sign-In: Apne kisi bhi Gmail / Google account se direct Login ya Sign Up karein (separate Email + Password auth Firebase Console me manually enable karna hota hai).",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthModeTabButton(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) GoldCoin else ObsidianCardElevated,
        modifier = modifier
            .height(48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) TextDark else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) TextDark else TextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AuthFeatureCard(
    items: List<Pair<ImageVector, String>>
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianCardElevated,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEachIndexed { index, (icon, text) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (index == 0) CyanDiamond else GoldCoin,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (index == 0) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}
