package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ChatMessage
import com.example.domain.ChatThread
import com.example.domain.VirtualIdGenerator
import com.example.domain.VirtualPlayerLookup
import com.example.ui.WalletUiState
import com.example.ui.components.PlayerAvatar
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.CoralError
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InboxScreen(
    uiState: WalletUiState,
    onSearchVirtualId: (String) -> Unit,
    onStartChatWithPeer: (VirtualPlayerLookup) -> Unit,
    onOpenThread: (ChatThread) -> Unit,
    onCloseActiveChat: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeThread = uiState.activeChatThread
    if (activeThread != null) {
        BackHandler { onCloseActiveChat() }
        DirectChatConversationView(
            thread = activeThread,
            messages = uiState.activeChatMessages,
            onBackClick = onCloseActiveChat,
            onSendMessage = onSendMessage,
            modifier = modifier
        )
    } else {
        InboxDirectoryView(
            uiState = uiState,
            onSearchVirtualId = onSearchVirtualId,
            onStartChatWithPeer = onStartChatWithPeer,
            onOpenThread = onOpenThread,
            modifier = modifier
        )
    }
}

@Composable
private fun InboxDirectoryView(
    uiState: WalletUiState,
    onSearchVirtualId: (String) -> Unit,
    onStartChatWithPeer: (VirtualPlayerLookup) -> Unit,
    onOpenThread: (ChatThread) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchInput by rememberSaveable { mutableStateOf("") }
    var copiedId by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val myVirtualId = uiState.currentUserAccount?.virtualId?.takeIf { it.isNotBlank() }
        ?: VirtualIdGenerator.fromUid(uiState.currentUserAccount?.userId.orEmpty())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("inbox_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("inbox_threads_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + My Virtual ID strip
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(
                        1.2.dp,
                        Brush.linearGradient(listOf(CyanDiamond.copy(alpha = 0.7f), GoldCoin.copy(alpha = 0.7f)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CyanContainer.copy(alpha = 0.7f), ObsidianSurfaceVariant, GoldContainer.copy(alpha = 0.55f))
                                )
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Forum,
                                contentDescription = null,
                                tint = CyanDiamond
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "1v1 Player Inbox",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Chat privately with any Housie player using their Virtual ID",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianSurface.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Badge,
                                        contentDescription = null,
                                        tint = GoldCoin,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Your ID: $myVirtualId",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = GoldCoinBright,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("inbox_my_virtual_id")
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(myVirtualId))
                                        copiedId = true
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy My Virtual ID",
                                        tint = if (copiedId) CyanDiamond else GoldCoin
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Find Player by Virtual ID Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonSearch,
                                contentDescription = null,
                                tint = GoldCoin
                            )
                            Text(
                                text = "Find Player by Virtual ID",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchInput,
                                onValueChange = { searchInput = it.uppercase().take(18) },
                                label = { Text("Enter Virtual ID") },
                                placeholder = { Text("e.g., VGD-RAHUL1") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanDiamond,
                                    unfocusedBorderColor = ObsidianOutline,
                                    focusedLabelColor = CyanDiamond,
                                    unfocusedLabelColor = TextSecondary,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = CyanDiamond
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("inbox_virtual_id_input")
                            )

                            Button(
                                onClick = { onSearchVirtualId(searchInput) },
                                enabled = searchInput.trim().isNotBlank() && !uiState.isSearchingPeer,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanDiamond,
                                    contentColor = TextDark
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("inbox_search_player_button")
                            ) {
                                if (uiState.isSearchingPeer) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = TextDark
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Find Player",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (!uiState.lookupError.isNullOrBlank()) {
                            Text(
                                text = uiState.lookupError,
                                style = MaterialTheme.typography.bodySmall,
                                color = CoralError,
                                modifier = Modifier.testTag("inbox_lookup_error")
                            )
                        }

                        val peer = uiState.lookedUpPeer
                        if (peer != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianCardElevated,
                                border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("inbox_found_player_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PlayerAvatar(
                                        photoBase64 = peer.photoBase64,
                                        displayName = peer.displayName,
                                        size = 48.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = peer.preferredName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = peer.virtualId,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GoldCoinBright
                                        )
                                    }
                                    Button(
                                        onClick = { onStartChatWithPeer(peer) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GoldCoin,
                                            contentColor = TextDark
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("inbox_start_chat_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text("Chat", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent Conversations Header
            item {
                Text(
                    text = "Direct Messages",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.chatThreads.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inbox_empty_state")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Chat,
                                contentDescription = null,
                                tint = CyanDiamond,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "No 1v1 Chats Yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Enter a friend's Virtual ID above (e.g., VGD-RAHUL1) to start a private conversation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.chatThreads,
                    key = { it.chatId }
                ) { thread ->
                    ChatThreadCard(
                        thread = thread,
                        onClick = { onOpenThread(thread) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatThreadCard(
    thread: ChatThread,
    onClick: () -> Unit
) {
    val timeLabel = remember(thread.lastMessageAtMillis) {
        if (thread.lastMessageAtMillis <= 0L) {
            ""
        } else {
            SimpleDateFormat("hh:mm a", Locale.US).format(Date(thread.lastMessageAtMillis))
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_thread_${thread.peerVirtualId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlayerAvatar(
                photoBase64 = thread.peerPhotoBase64,
                displayName = thread.peerName,
                size = 50.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.peerName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (timeLabel.isNotBlank()) {
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Text(
                    text = thread.peerVirtualId,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldCoin
                )
                Text(
                    text = thread.lastMessageText.ifBlank { "Tap to start chatting..." },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DirectChatConversationView(
    thread: ChatThread,
    messages: List<ChatMessage>,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageText by rememberSaveable(thread.chatId) { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("chat_conversation_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            // Top Chat Header Bar
            Surface(
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Inbox",
                            tint = GoldCoinBright
                        )
                    }
                    PlayerAvatar(
                        photoBase64 = thread.peerPhotoBase64,
                        displayName = thread.peerName,
                        size = 44.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = thread.peerName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Virtual ID: ${thread.peerVirtualId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanDiamond
                        )
                    }
                }
            }

            // Messages Feed
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("chat_messages_list"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Say hello to ${thread.peerName} (${thread.peerVirtualId})!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { msg ->
                        ChatBubble(message = msg)
                    }
                }
            }

            // Composer Bar
            Surface(
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it.take(1000) },
                        placeholder = { Text("Type a message...") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldCoin,
                            unfocusedBorderColor = ObsidianOutline,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = GoldCoinBright
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    IconButton(
                        onClick = {
                            val toSend = messageText.trim()
                            if (toSend.isNotBlank()) {
                                onSendMessage(toSend)
                                messageText = ""
                            }
                        },
                        enabled = messageText.trim().isNotBlank(),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                if (messageText.trim().isNotBlank()) GoldCoin else ObsidianCardElevated
                            )
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send Message",
                            tint = if (messageText.trim().isNotBlank()) TextDark else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val timeLabel = remember(message.timestampMillis) {
        SimpleDateFormat("hh:mm a", Locale.US).format(Date(message.timestampMillis))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromCurrentUser) 4.dp else 16.dp
            ),
            color = if (message.isFromCurrentUser) GoldContainer else ObsidianSurfaceVariant,
            border = BorderStroke(
                1.dp,
                if (message.isFromCurrentUser) GoldCoin.copy(alpha = 0.65f) else CyanDiamond.copy(alpha = 0.35f)
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!message.isFromCurrentUser) {
                    Text(
                        text = "${message.senderName} • ${message.senderVirtualId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanDiamond
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
