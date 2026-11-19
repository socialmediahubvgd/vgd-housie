package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.ExchangeCalculator
import com.example.domain.HousieClaimType
import com.example.domain.HousieTicket
import com.example.ui.GameUiState
import com.example.ui.WalletUiState
import com.example.ui.WinPopupEvent
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.EmeraldContainer
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameBoardScreen(
    gameState: GameUiState,
    walletState: WalletUiState,
    onClaimWin: (HousieClaimType) -> Unit,
    onDismissWinPopup: () -> Unit,
    onDismissWrongClaim: () -> Unit,
    onPlayAgain: () -> Unit,
    onShowTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onPlayAgain()
    }

    val room = gameState.activeRoom
    val ticket = gameState.ticket

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
                .testTag("game_board_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar: Room Name + Back + Live Diamond Wallet
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onPlayAgain,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("game_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back_to_rooms),
                                tint = TextPrimary
                            )
                        }
                        Column {
                            Text(
                                text = room?.name ?: "Housie Room",
                                style = MaterialTheme.typography.titleLarge,
                                color = GoldCoinBright
                            )
                            Text(
                                text = "Prize: ${room?.prizeDisplay ?: "0"} Diamond 💎",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanDiamond
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onShowTutorial,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("game_rules_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                contentDescription = "How to Play",
                                tint = GoldCoin
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ObsidianCardElevated,
                            border = BorderStroke(1.dp, CyanDiamond.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "💎 ${ExchangeCalculator.formatNumber(walletState.userDiamonds)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyanDiamond,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("game_wallet_diamonds")
                            )
                        }
                    }
                }
            }

            // Big Number Caller Card (1-90 every 4 seconds)
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.2.dp, GoldCoin.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GoldContainer.copy(alpha = 0.85f), ObsidianSurfaceVariant)
                                )
                            )
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.number_caller_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldCoinBright
                            )
                            Text(
                                text = if (gameState.isGameOver) {
                                    "Match Finished"
                                } else {
                                    stringResource(R.string.calling_every_4s)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Called: ${gameState.calledNumbers.size}/90",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Ticket: ${gameState.matchedCount}/15",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Pulsing Golden Caller Ball
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(GoldCoinBright, GoldCoin)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = gameState.currentNumber,
                                transitionSpec = {
                                    (scaleIn(
                                        initialScale = 0.5f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    ) + fadeIn()) togetherWith fadeOut()
                                },
                                label = "caller_number"
                            ) { number ->
                                Text(
                                    text = number?.toString() ?: "--",
                                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
                                    color = TextDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("caller_current_number")
                                )
                            }
                        }
                    }
                }
            }

            // 3 Row x 9 Column Housie Ticket (15 Numbers, Auto-cut Green)
            if (ticket != null) {
                item {
                    HousieTicketGrid(
                        ticket = ticket,
                        calledNumbers = gameState.calledSet
                    )
                }
            }

            // Last 5 Numbers List
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.last_5_numbers_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("last_5_numbers_row"),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val recent = gameState.lastFiveNumbers
                            if (recent.isEmpty()) {
                                Text(
                                    text = "Waiting for first number...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            } else {
                                recent.forEachIndexed { idx, num ->
                                    val isLatest = idx == 0
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(if (isLatest) GoldCoin else ObsidianCardElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = num.toString(),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isLatest) TextDark else TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Game Over Card when Full House is completed
            if (gameState.isGameOver) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.5.dp, GoldCoin),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("game_over_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(listOf(GoldContainer.copy(alpha = 0.7f), ObsidianSurface))
                                )
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = stringResource(R.string.game_over_title),
                                tint = GoldCoin,
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = stringResource(R.string.game_over_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = GoldCoinBright,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Winner: ${gameState.winnerName ?: "You"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.testTag("game_winner_name")
                            )
                            Text(
                                text = "Total Won: ${ExchangeCalculator.formatNumber(gameState.totalDiamondsWonInMatch)} Diamond 💎",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyanDiamond
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onPlayAgain,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldCoin,
                                    contentColor = TextDark
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("play_again_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = stringResource(R.string.play_again_action),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 5 Claim Buttons: Early 5, First Line, Second Line, Third Line, Full House
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.claim_prizes_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HousieClaimType.entries.forEach { claimType ->
                                val alreadyClaimed = claimType in gameState.claimedTypes
                                val isFullHouse = claimType == HousieClaimType.FULL_HOUSE
                                Button(
                                    onClick = { onClaimWin(claimType) },
                                    enabled = !alreadyClaimed && !gameState.isGameOver,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when {
                                            alreadyClaimed -> EmeraldContainer
                                            isFullHouse -> GoldCoin
                                            else -> ObsidianCardElevated
                                        },
                                        contentColor = when {
                                            alreadyClaimed -> EmeraldSuccess
                                            isFullHouse -> TextDark
                                            else -> GoldCoinBright
                                        },
                                        disabledContainerColor = if (alreadyClaimed) EmeraldContainer else ObsidianCardElevated,
                                        disabledContentColor = if (alreadyClaimed) EmeraldSuccess else TextSecondary
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            alreadyClaimed -> EmeraldSuccess
                                            isFullHouse -> GoldCoin
                                            else -> GoldCoin.copy(alpha = 0.4f)
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("claim_${claimType.name.lowercase()}")
                                ) {
                                    if (alreadyClaimed) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                    }
                                    Text(
                                        text = if (alreadyClaimed) "${claimType.label} ✓" else claimType.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Winner Popup: "You Won 15,000 Diamond 💎"
        if (gameState.winPopup != null) {
            WinClaimPopupDialog(
                event = gameState.winPopup,
                isFullHouse = gameState.winPopup.claimType == HousieClaimType.FULL_HOUSE,
                onDismiss = onDismissWinPopup,
                onPlayAgain = onPlayAgain
            )
        }

        // Wrong Claim Popup
        if (gameState.wrongClaimVisible) {
            AlertDialog(
                onDismissRequest = onDismissWrongClaim,
                containerColor = ObsidianSurface,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = CoralError,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.wrong_claim_message),
                        style = MaterialTheme.typography.titleLarge,
                        color = CoralError,
                        modifier = Modifier.testTag("wrong_claim_title")
                    )
                },
                text = {
                    Text(
                        text = "Ticket numbers do not complete this claim yet. Keep playing!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onDismissWrongClaim,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralContainer,
                            contentColor = CoralError
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("dismiss_wrong_claim_button")
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun HousieTicketGrid(
    ticket: HousieTicket,
    calledNumbers: Set<Int>
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.2.dp, GoldCoin.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("housie_ticket_grid")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Housie Ticket (3 × 9)",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldCoinBright
                )
                Text(
                    text = "15 Numbers",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            ticket.rows.forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEachIndexed { colIdx, number ->
                        val isCut = number != null && number in calledNumbers
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                number == null -> ObsidianBg.copy(alpha = 0.7f)
                                isCut -> EmeraldContainer
                                else -> ObsidianCardElevated
                            },
                            border = BorderStroke(
                                width = if (isCut) 1.5.dp else 1.dp,
                                color = when {
                                    number == null -> ObsidianOutline.copy(alpha = 0.35f)
                                    isCut -> EmeraldSuccess
                                    else -> ObsidianOutline
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .let { mod ->
                                    if (number != null) {
                                        mod.testTag("ticket_cell_${rowIdx}_${colIdx}_${if (isCut) "cut" else "open"}")
                                    } else {
                                        mod
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (number != null) {
                                    Text(
                                        text = number.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isCut) EmeraldSuccess else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = if (isCut) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WinClaimPopupDialog(
    event: WinPopupEvent,
    isFullHouse: Boolean,
    onDismiss: () -> Unit,
    onPlayAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(GoldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = GoldCoin,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "You Won ${event.formattedPrize} Diamond 💎",
                style = MaterialTheme.typography.headlineSmall,
                color = GoldCoinBright,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("win_popup_title")
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${event.claimType.label} Verified!",
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Diamonds have been added directly to your VGD Housie Wallet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = if (isFullHouse) onPlayAgain else onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldCoin,
                    contentColor = TextDark
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("win_popup_confirm_button")
            ) {
                Text(
                    text = if (isFullHouse) stringResource(R.string.play_again_action) else "Continue",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = if (isFullHouse) {
            {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianCardElevated,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("View Board")
                }
            }
        } else null
    )
}
