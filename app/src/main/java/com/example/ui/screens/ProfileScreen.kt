package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.GenderOptions
import com.example.domain.VirtualIdGenerator
import com.example.ui.WalletUiState
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.encodeImageUriToCompactBase64
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: WalletUiState,
    currentUserEmail: String,
    onSaveProfile: (displayName: String, nickname: String, gender: String, dob: String, photoBase64: String) -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account = uiState.currentUserAccount
    val virtualId = account?.virtualId?.takeIf { it.isNotBlank() }
        ?: VirtualIdGenerator.fromUid(account?.userId.orEmpty())

    var syncedAccountKey by remember { mutableStateOf("") }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var nicknameInput by rememberSaveable { mutableStateOf("") }
    var genderInput by rememberSaveable { mutableStateOf("") }
    var dobInput by rememberSaveable { mutableStateOf("") }
    var photoBase64 by rememberSaveable { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var copiedBanner by remember { mutableStateOf(false) }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(
        account?.userId,
        account?.displayName,
        account?.nickname,
        account?.gender,
        account?.dob,
        account?.photoBase64
    ) {
        if (account != null) {
            val signature = "${account.userId}|${account.displayName}|${account.nickname}|${account.gender}|${account.dob}|${account.photoBase64.length}"
            if (signature != syncedAccountKey) {
                nameInput = account.displayName
                nicknameInput = account.nickname
                genderInput = account.gender
                dobInput = account.dob
                photoBase64 = account.photoBase64
                syncedAccountKey = signature
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessingPhoto = true
                val encoded = encodeImageUriToCompactBase64(context, uri)
                if (!encoded.isNullOrBlank()) {
                    photoBase64 = encoded
                }
                isProcessingPhoto = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("profile_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("profile_lazy_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Virtual ID Identity Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        1.2.dp,
                        Brush.linearGradient(listOf(GoldCoin.copy(alpha = 0.8f), CyanDiamond.copy(alpha = 0.6f)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GoldContainer.copy(alpha = 0.85f), ObsidianSurfaceVariant, CyanContainer.copy(alpha = 0.65f))
                                )
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            PlayerAvatar(
                                photoBase64 = photoBase64,
                                displayName = nameInput.ifBlank { account?.displayName ?: "Player" },
                                size = 76.dp,
                                borderColor = GoldCoinBright
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nameInput.ifBlank { account?.displayName ?: "VGD Player" },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (nicknameInput.isNotBlank()) {
                                    Text(
                                        text = "@$nicknameInput",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GoldCoinBright
                                    )
                                }
                                Text(
                                    text = currentUserEmail.ifBlank { account?.email.orEmpty() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Photo Picker Actions (Zero-Permission Android Photo Picker)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isProcessingPhoto,
                                border = BorderStroke(1.dp, CyanDiamond.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("profile_pick_photo_button")
                            ) {
                                if (isProcessingPhoto) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = CyanDiamond
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.PhotoLibrary,
                                        contentDescription = "Choose Profile Photo",
                                        tint = CyanDiamond,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = if (photoBase64.isBlank()) "Upload Photo" else "Change Photo",
                                    color = CyanDiamond,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (photoBase64.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { photoBase64 = "" },
                                    border = BorderStroke(1.dp, CoralError.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("profile_remove_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Remove Photo",
                                        tint = CoralError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Remove", color = CoralError)
                                }
                            }
                        }

                        // Unique Virtual ID Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ObsidianSurface.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "MY UNIQUE VIRTUAL ID (SHARE FOR 1v1 CHAT)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Badge,
                                            contentDescription = null,
                                            tint = GoldCoin,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = virtualId,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = GoldCoinBright,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.testTag("profile_virtual_id_text")
                                        )
                                    }
                                }
                                Surface(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(virtualId))
                                        copiedBanner = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (copiedBanner) EmeraldSuccess.copy(alpha = 0.2f) else GoldContainer,
                                    border = BorderStroke(
                                        1.dp,
                                        if (copiedBanner) EmeraldSuccess else GoldCoin.copy(alpha = 0.7f)
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("copy_virtual_id_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (copiedBanner) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy Virtual ID",
                                            tint = if (copiedBanner) EmeraldSuccess else GoldCoinBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (copiedBanner) "Copied" else "Copy ID",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (copiedBanner) EmeraldSuccess else GoldCoinBright
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Profile Editing Form
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = GoldCoin
                            )
                            Text(
                                text = "Edit Player Profile",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldCoin,
                            unfocusedBorderColor = ObsidianOutline,
                            focusedLabelColor = GoldCoinBright,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = GoldCoinBright
                        )

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it.take(100) },
                            label = { Text("Full Name") },
                            placeholder = { Text("Enter your display name") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = GoldCoin)
                            },
                            singleLine = true,
                            colors = fieldColors,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input")
                        )

                        OutlinedTextField(
                            value = nicknameInput,
                            onValueChange = { nicknameInput = it.take(40) },
                            label = { Text("Nickname / Housie Tag") },
                            placeholder = { Text("e.g., KingOfHousie") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Tag, contentDescription = null, tint = CyanDiamond)
                            },
                            singleLine = true,
                            colors = fieldColors,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_nickname_input")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Gender",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GenderOptions.entries.forEach { option ->
                                    val selected = genderInput.equals(option, ignoreCase = true)
                                    FilterChip(
                                        selected = selected,
                                        onClick = { genderInput = option },
                                        label = { Text(option) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = ObsidianCardElevated,
                                            labelColor = TextSecondary,
                                            selectedContainerColor = GoldContainer,
                                            selectedLabelColor = GoldCoinBright
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            borderColor = ObsidianOutline,
                                            selectedBorderColor = GoldCoin
                                        ),
                                        modifier = Modifier.testTag("profile_gender_$option")
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = dobInput,
                            onValueChange = { dobInput = it.take(20) },
                            label = { Text("Date of Birth (DD/MM/YYYY)") },
                            placeholder = { Text("DD/MM/YYYY") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Cake, contentDescription = null, tint = GoldCoin)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.testTag("profile_dob_picker_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Cake,
                                        contentDescription = "Select Date of Birth",
                                        tint = CyanDiamond
                                    )
                                }
                            },
                            singleLine = true,
                            colors = fieldColors,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_dob_input")
                        )

                        Button(
                            onClick = {
                                onSaveProfile(nameInput, nicknameInput, genderInput, dobInput, photoBase64)
                            },
                            enabled = nameInput.trim().isNotBlank() && !uiState.isSavingProfile,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldCoin,
                                contentColor = TextDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_profile_button")
                        ) {
                            if (uiState.isSavingProfile) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = TextDark
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "Save Profile",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Account Session Sign Out
            item {
                OutlinedButton(
                    onClick = onSignOutClick,
                    border = BorderStroke(1.dp, CoralError.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        tint = CoralError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Sign Out",
                        color = CoralError,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            dobInput = formatter.format(Date(selectedMillis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", color = GoldCoinBright)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
