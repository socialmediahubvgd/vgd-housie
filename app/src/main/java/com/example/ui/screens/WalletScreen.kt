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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ExchangeRecordEntity
import com.example.data.WalletActivityEntity
import com.example.domain.ExchangeCalculator
import com.example.ui.WalletUiState
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldCoin
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
fun WalletScreen(
    uiState: WalletUiState,
    onBuyCoinsClick: () -> Unit,
    onWithdrawDiamondClick: () -> Unit,
    onExchangeDiamondClick: () -> Unit,
    onPlayHousieClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Branded Wallet Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.wallet_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ObsidianCardElevated,
                        border = BorderStroke(1.dp, ObsidianOutline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = GoldCoin,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Active Vault",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldCoin
                            )
                        }
                    }
                }
            }

            // Dual Balance Cards (Diamonds & Coins)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BalanceCard(
                        emoji = "💎",
                        label = stringResource(R.string.my_diamonds_label),
                        amount = ExchangeCalculator.formatNumber(uiState.userDiamonds),
                        accentColor = CyanDiamond,
                        gradientColors = listOf(CyanContainer, ObsidianSurfaceVariant),
                        icon = Icons.Rounded.Diamond,
                        tag = "wallet_diamond_balance",
                        modifier = Modifier.weight(1f)
                    )
                    BalanceCard(
                        emoji = "🪙",
                        label = stringResource(R.string.my_coins_label),
                        amount = ExchangeCalculator.formatNumber(uiState.userCoins),
                        accentColor = GoldCoin,
                        gradientColors = listOf(GoldContainer, ObsidianSurfaceVariant),
                        icon = Icons.Rounded.MonetizationOn,
                        tag = "wallet_coin_balance",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3 Wallet Action Buttons: [Buy Coins] [Withdraw Diamond] [Exchange Diamond]
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Wallet Actions",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WalletActionButton(
                            label = stringResource(R.string.btn_buy_coins),
                            icon = Icons.Rounded.AddCard,
                            containerColor = ObsidianCardElevated,
                            contentColor = GoldCoin,
                            borderColor = GoldCoin.copy(alpha = 0.4f),
                            onClick = onBuyCoinsClick,
                            testTag = "wallet_buy_coins_button",
                            modifier = Modifier.weight(1f)
                        )
                        WalletActionButton(
                            label = stringResource(R.string.btn_withdraw_diamond),
                            icon = Icons.Rounded.ArrowOutward,
                            containerColor = ObsidianCardElevated,
                            contentColor = TextPrimary,
                            borderColor = ObsidianOutline,
                            onClick = onWithdrawDiamondClick,
                            testTag = "wallet_withdraw_diamond_button",
                            modifier = Modifier.weight(1f)
                        )
                        WalletActionButton(
                            label = stringResource(R.string.btn_exchange_diamond),
                            icon = Icons.Rounded.CurrencyExchange,
                            containerColor = CyanDiamond,
                            contentColor = TextDark,
                            borderColor = CyanDiamond,
                            onClick = onExchangeDiamondClick,
                            testTag = "wallet_exchange_diamond_button",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Play VGD Housie Quick Launch Card
            item {
                Surface(
                    onClick = onPlayHousieClick,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.2.dp, GoldCoin.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_play_housie_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GoldContainer, ObsidianSurfaceVariant)
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GoldCoin),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Casino,
                                    contentDescription = stringResource(R.string.btn_play_housie),
                                    tint = TextDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Play VGD Housie Game",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GoldCoin,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "1–90 Tambola • Free Practice & Coin Rooms • Win 💎 Diamonds",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GoldCoin
                        ) {
                            Text(
                                text = "PLAY",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Fixed Rate Quick Promo Strip
            item {
                Surface(
                    onClick = onExchangeDiamondClick,
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, CyanDiamond.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Diamond → Coins Fixed Rate",
                                style = MaterialTheme.typography.titleSmall,
                                color = CyanDiamond
                            )
                            Text(
                                text = "150 💎 = 100 🪙  •  Min 150 💎  •  10% Fee",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = stringResource(R.string.btn_exchange_diamond),
                            tint = CyanDiamond
                        )
                    }
                }
            }

            // Separate Exchange History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = CyanDiamond,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.exchange_history_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ObsidianCardElevated
                    ) {
                        Text(
                            text = "${uiState.exchangeRecords.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanDiamond,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (uiState.exchangeRecords.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(CyanContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CurrencyExchange,
                                    contentDescription = null,
                                    tint = CyanDiamond,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.empty_exchange_history_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.empty_exchange_history_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onExchangeDiamondClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanDiamond,
                                    contentColor = TextDark
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_exchange_diamond),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                items(
                    items = uiState.exchangeRecords,
                    key = { "exchange_${it.id}" }
                ) { record ->
                    ExchangeHistoryCard(record = record)
                }
            }

            // Secondary Wallet Activity (Buy Coins / Withdrawals / Admin Bonus)
            if (uiState.walletActivities.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.wallet_activity_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                }
                items(
                    items = uiState.walletActivities,
                    key = { "activity_${it.id}" }
                ) { activity ->
                    WalletActivityRow(activity = activity)
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(
    emoji: String,
    label: String,
    amount: String,
    accentColor: Color,
    gradientColors: List<Color>,
    icon: ImageVector,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(colors = gradientColors))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$emoji $label",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.displaySmall,
                color = accentColor,
                modifier = Modifier.testTag(tag)
            )
        }
    }
}

@Composable
private fun WalletActionButton(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(76.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExchangeHistoryCard(record: ExchangeRecordEntity) {
    val diamondsFmt = ExchangeCalculator.formatNumber(record.diamondsExchanged)
    val coinsFmt = ExchangeCalculator.formatNumber(record.netCoinsReceived)
    val feeFmt = ExchangeCalculator.formatNumber(record.adminFeeCoins)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exchange_history_item_${record.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CurrencyExchange,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Exchanged $diamondsFmt Diamond -> $coinsFmt Coins",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = formatTimestamp(record.timestampMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GoldContainer
            ) {
                Text(
                    text = "Fee: $feeFmt 🪙",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldCoin,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun WalletActivityRow(activity: WalletActivityEntity) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "${activity.subtitle} • ${formatTimestamp(activity.timestampMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = activity.amountDeltaText,
                style = MaterialTheme.typography.labelLarge,
                color = if (activity.amountDeltaText.startsWith("+")) EmeraldSuccess else CyanDiamond
            )
        }
    }
}

internal fun formatTimestamp(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
    return formatter.format(Date(millis))
}
