package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ExchangeCalculator
import com.example.domain.HousieRoom
import com.example.domain.VirtualIdGenerator
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

@Composable
fun HomeRoomsScreen(
    walletState: WalletUiState,
    isFounder: Boolean,
    currentUserEmail: String,
    onSignOutClick: () -> Unit,
    onJoinRoom: (HousieRoom) -> Unit,
    onBuyCoinsClick: () -> Unit,
    onExchangeClick: () -> Unit,
    onOpenWalletClick: () -> Unit,
    onShowTutorial: () -> Unit,
    onOpenProfileClick: () -> Unit = {},
    onOpenInboxClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rooms = remember(walletState.platformSettings) {
        walletState.platformSettings.toRooms()
    }
    val maintenanceLocked = walletState.platformSettings.maintenanceMode && !isFounder
    val accountLocked = walletState.isSuspended && !isFounder
    val joinBlocked = maintenanceLocked || accountLocked
    val currentAccount = walletState.currentUserAccount
    val myVirtualId = currentAccount?.virtualId?.takeIf { it.isNotBlank() }
        ?: VirtualIdGenerator.fromUid(currentAccount?.userId.orEmpty())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("home_rooms_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Wallet + Session Header Card on Home Page
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        1.2.dp,
                        Brush.linearGradient(listOf(GoldCoin.copy(alpha = 0.75f), CyanDiamond.copy(alpha = 0.5f)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GoldContainer.copy(alpha = 0.85f), ObsidianSurfaceVariant, CyanContainer.copy(alpha = 0.6f))
                                )
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    onClick = onOpenProfileClick,
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    modifier = Modifier.testTag("home_profile_avatar_button")
                                ) {
                                    PlayerAvatar(
                                        photoBase64 = currentAccount?.photoBase64.orEmpty(),
                                        displayName = currentAccount?.displayName ?: "Player",
                                        size = 48.dp,
                                        borderColor = if (isFounder) GoldCoinBright else CyanDiamond
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentAccount?.displayName?.takeIf { it.isNotBlank() }
                                            ?: stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = GoldCoinBright,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        onClick = onOpenProfileClick,
                                        shape = RoundedCornerShape(50),
                                        color = ObsidianSurface.copy(alpha = 0.75f),
                                        border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.45f)),
                                        modifier = Modifier.testTag("home_virtual_id_chip")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isFounder) {
                                                Icon(
                                                    imageVector = Icons.Rounded.VerifiedUser,
                                                    contentDescription = "Founder",
                                                    tint = GoldCoinBright,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                            Text(
                                                text = "ID: $myVirtualId • Edit",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isFounder) GoldCoinBright else CyanDiamond,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = onShowTutorial,
                                    shape = RoundedCornerShape(50),
                                    color = ObsidianCardElevated,
                                    border = BorderStroke(1.dp, CyanDiamond.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("how_to_play_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                            contentDescription = "How to Play & Demo Video",
                                            tint = CyanDiamond,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Rules",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CyanDiamond
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onOpenWalletClick,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(ObsidianCardElevated)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountBalanceWallet,
                                        contentDescription = stringResource(R.string.nav_wallet),
                                        tint = GoldCoin
                                    )
                                }
                                IconButton(
                                    onClick = onSignOutClick,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(ObsidianCardElevated)
                                        .testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                                        contentDescription = "Sign Out",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianSurface.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "🪙 Coins",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = ExchangeCalculator.formatNumber(walletState.userCoins),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = GoldCoin,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("home_wallet_coins")
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianSurface.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, CyanDiamond.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "💎 Diamond",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = ExchangeCalculator.formatNumber(walletState.userDiamonds),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = CyanDiamond,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("home_wallet_diamonds")
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onBuyCoinsClick,
                                enabled = !joinBlocked,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldCoin,
                                    contentColor = TextDark
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_buy_coins_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = stringResource(R.string.btn_buy_coins),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = onExchangeClick,
                                border = BorderStroke(1.dp, CyanDiamond.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_exchange_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CurrencyExchange,
                                    contentDescription = null,
                                    tint = CyanDiamond,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "Exchange",
                                    color = CyanDiamond,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Live Founder Announcement Banner
            if (walletState.platformSettings.announcementBanner.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = GoldContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {}
                            .testTag("live_announcement_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Campaign,
                                contentDescription = "Announcement",
                                tint = GoldCoinBright
                            )
                            Text(
                                text = walletState.platformSettings.announcementBanner,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Suspended or Maintenance Alert
            if (accountLocked || maintenanceLocked) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CoralError.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, CoralError.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = CoralError
                            )
                            Text(
                                text = if (accountLocked) {
                                    "Your account has been suspended by the Founder. Room joins and exchanges are locked."
                                } else {
                                    "VGD Housie is currently under Founder Maintenance Mode. Rooms will reopen shortly."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoralError
                            )
                        }
                    }
                }
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.home_rooms_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.home_rooms_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Casino,
                            contentDescription = null,
                            tint = GoldCoin
                        )
                    }
                }
            }

            // 4 Game Rooms (live pricing from PlatformSettings)
            items(
                items = rooms,
                key = { it.id }
            ) { room ->
                GameRoomCard(
                    room = room,
                    canAfford = walletState.userCoins >= room.entryCoins,
                    enabled = !joinBlocked,
                    onJoinClick = { onJoinRoom(room) }
                )
            }
        }
    }
}

@Composable
private fun GameRoomCard(
    room: HousieRoom,
    canAfford: Boolean,
    enabled: Boolean,
    onJoinClick: () -> Unit
) {
    val isVip = room.id == "vip"
    val accent = if (isVip) GoldCoinBright else GoldCoin
    val borderColor = if (isVip) GoldCoin.copy(alpha = 0.75f) else ObsidianOutline

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        border = BorderStroke(if (isVip) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_card_${room.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isVip) {
                        Brush.horizontalGradient(listOf(GoldContainer.copy(alpha = 0.65f), ObsidianSurface))
                    } else {
                        Brush.horizontalGradient(listOf(ObsidianSurfaceVariant.copy(alpha = 0.6f), ObsidianSurface))
                    }
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = room.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isVip) GoldContainer else ObsidianCardElevated
                    ) {
                        Text(
                            text = room.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "Entry: ${ExchangeCalculator.formatNumber(room.entryCoins)} Coins 🪙",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (canAfford) GoldCoin else Color(0xFFFBBF24).copy(alpha = 0.85f)
                )
                Text(
                    text = "Prize: ${room.prizeDisplay} Diamond 💎",
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanDiamond,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Button(
                onClick = onJoinClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldCoin,
                    contentColor = TextDark
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("join_room_${room.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = stringResource(R.string.btn_join_room),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
