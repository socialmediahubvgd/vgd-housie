package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ExchangeCalculator
import com.example.domain.FounderAuditEntry
import com.example.domain.FounderConfig
import com.example.domain.ManagedUserAccount
import com.example.domain.PlatformExchangeLedgerItem
import com.example.domain.PlatformSettings
import com.example.ui.WalletUiState
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
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

private enum class FounderTab(val label: String, val tag: String) {
    OVERVIEW("Overview", "founder_tab_overview"),
    GENERATOR("Coin Gen", "founder_tab_generator"),
    USERS("Users", "founder_tab_users"),
    CONTROLS("Controls", "founder_tab_controls")
}

@Composable
fun AdminPanelScreen(
    uiState: WalletUiState,
    founderEmail: String,
    onAddBonusDiamonds: (Long) -> Unit,
    onGenerateCurrency: (targetUserId: String, coins: Long, diamonds: Long, reason: String) -> Unit,
    onBroadcastCurrency: (coins: Long, diamonds: Long, reason: String) -> Unit,
    onAdjustOrResetUser: (targetUserId: String, resetToZero: Boolean, coinsDeduct: Long, diamondsDeduct: Long, reason: String) -> Unit,
    onSetUserStatus: (targetUserId: String, newStatus: String) -> Unit,
    onUpdatePlatformSettings: (settings: PlatformSettings, actionType: String, summary: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(FounderTab.OVERVIEW) }
    var preselectedTargetUserId by remember { mutableStateOf<String?>(null) }
    var userPendingReset by remember { mutableStateOf<ManagedUserAccount?>(null) }

    var userSearchQuery by remember { mutableStateOf("") }
    var userStatusFilter by remember { mutableStateOf("ALL") }
    val filteredUsers = remember(uiState.managedUsers, userSearchQuery, userStatusFilter) {
        uiState.managedUsers.filter { user ->
            val matchesQuery = userSearchQuery.isBlank() ||
                user.displayName.contains(userSearchQuery, ignoreCase = true) ||
                user.email.contains(userSearchQuery, ignoreCase = true)
            val matchesStatus = when (userStatusFilter) {
                "ACTIVE" -> !user.isSuspended
                "SUSPENDED" -> user.isSuspended
                else -> true
            }
            matchesQuery && matchesStatus
        }
    }

    val settings = uiState.platformSettings
    var exchangeEnabled by remember(settings) { mutableStateOf(settings.exchangeEnabled) }
    var maintenanceMode by remember(settings) { mutableStateOf(settings.maintenanceMode) }
    var bannerText by remember(settings) { mutableStateOf(settings.announcementBanner) }
    var smallEntry by remember(settings) { mutableStateOf(settings.smallEntryCoins.toString()) }
    var smallPrize by remember(settings) { mutableStateOf(settings.smallPrizeDiamonds.toString()) }
    var medEntry by remember(settings) { mutableStateOf(settings.mediumEntryCoins.toString()) }
    var medPrize by remember(settings) { mutableStateOf(settings.mediumPrizeDiamonds.toString()) }
    var bigEntry by remember(settings) { mutableStateOf(settings.bigEntryCoins.toString()) }
    var bigPrize by remember(settings) { mutableStateOf(settings.bigPrizeDiamonds.toString()) }
    var vipEntry by remember(settings) { mutableStateOf(settings.vipEntryCoins.toString()) }
    var vipPrize by remember(settings) { mutableStateOf(settings.vipPrizeDiamonds.toString()) }

    val ledgerItems = remember(uiState.globalExchangeLedger, uiState.exchangeRecords, founderEmail) {
        if (uiState.globalExchangeLedger.isNotEmpty()) {
            uiState.globalExchangeLedger
        } else {
            uiState.exchangeRecords.map { rec ->
                PlatformExchangeLedgerItem(
                    id = rec.id.toString(),
                    userId = uiState.currentUserAccount?.userId ?: "founder",
                    userEmail = founderEmail,
                    userName = uiState.currentUserAccount?.displayName ?: "Founder",
                    diamondsExchanged = rec.diamondsExchanged,
                    grossCoins = rec.grossCoins,
                    netCoinsReceived = rec.netCoinsReceived,
                    adminFeeCoins = rec.adminFeeCoins,
                    timestampMillis = rec.timestampMillis
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("founder_panel_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .testTag("founder_panel_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Founder Exclusive Header
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.2.dp, GoldCoin.copy(alpha = 0.65f)),
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
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = "Verified Founder",
                                    tint = GoldCoinBright,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "FOUNDER COMMAND PANEL",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldCoinBright,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = stringResource(R.string.admin_panel_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Exclusive access: $founderEmail",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanDiamond,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("founder_email_badge")
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GoldCoin),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AdminPanelSettings,
                                contentDescription = stringResource(R.string.admin_panel_title),
                                tint = TextDark,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // Segmented 4-Tab Bar inside Founder Panel
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FounderTab.entries.forEach { tab ->
                        val active = selectedTab == tab
                        FilterChip(
                            selected = active,
                            onClick = { selectedTab = tab },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (tab) {
                                        FounderTab.OVERVIEW -> Icons.Rounded.Assessment
                                        FounderTab.GENERATOR -> Icons.Rounded.MonetizationOn
                                        FounderTab.USERS -> Icons.Rounded.Group
                                        FounderTab.CONTROLS -> Icons.Rounded.Tune
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldCoin,
                                selectedLabelColor = TextDark,
                                selectedLeadingIconColor = TextDark,
                                containerColor = ObsidianSurface,
                                labelColor = TextSecondary,
                                iconColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = active,
                                borderColor = ObsidianOutline,
                                selectedBorderColor = GoldCoin
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag(tab.tag)
                        )
                    }
                }
            }

            when (selectedTab) {
                FounderTab.OVERVIEW -> {
                    // Hero 10% Exchange Fee Profit Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.2.dp, GoldCoin.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(GoldContainer, ObsidianSurfaceVariant)
                                        )
                                    )
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MonetizationOn,
                                        contentDescription = null,
                                        tint = GoldCoin
                                    )
                                    Text(
                                        text = stringResource(R.string.admin_profit_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = "🪙 ${ExchangeCalculator.formatNumber(uiState.adminFeeProfitCoins)} Coins",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = GoldCoin,
                                    modifier = Modifier.testTag("admin_profit_coins_text")
                                )
                                Text(
                                    text = "10% platform commission across all player Diamond → Coins exchanges",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Platform KPI Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminStatTile(
                                label = stringResource(R.string.total_exchanges_label),
                                value = "${uiState.totalExchangesCount}",
                                accentColor = EmeraldSuccess,
                                testTag = "admin_total_exchanges_text",
                                modifier = Modifier.weight(1f)
                            )
                            AdminStatTile(
                                label = stringResource(R.string.total_diamonds_exchanged_label),
                                value = "${ExchangeCalculator.formatNumber(uiState.totalDiamondsExchanged)} 💎",
                                accentColor = CyanDiamond,
                                testTag = "admin_total_diamonds_text",
                                modifier = Modifier.weight(1f)
                            )
                            AdminStatTile(
                                label = "Total Users",
                                value = "${uiState.managedUsers.size.coerceAtLeast(1)}",
                                accentColor = GoldCoin,
                                testTag = "admin_total_users_text",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Founder Mint Counters & Quick Actions
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, ObsidianOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Quick Founder Self-Credit",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Generated by Founder: ${ExchangeCalculator.formatNumber(uiState.platformSettings.totalCoinsGeneratedByFounder)} 🪙  •  ${ExchangeCalculator.formatNumber(uiState.platformSettings.totalDiamondsGeneratedByFounder)} 💎",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onAddBonusDiamonds(1_500L) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("admin_add_1500_diamonds_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "+1,500 💎",
                                            color = CyanDiamond,
                                            maxLines = 1
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onAddBonusDiamonds(15_000L) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("admin_add_15000_diamonds_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "+15,000 💎",
                                            color = CyanDiamond,
                                            maxLines = 1
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            val uid = uiState.currentUserAccount?.userId
                                            if (!uid.isNullOrBlank()) {
                                                onGenerateCurrency(uid, 50_000L, 0L, "Quick Founder Coin Mint")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GoldCoin,
                                            contentColor = TextDark
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("admin_add_50000_coins_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "+50K 🪙",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Global Exchange Records Header
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
                                    imageVector = Icons.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    tint = GoldCoin,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.admin_records_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ObsidianCardElevated
                            ) {
                                Text(
                                    text = "${ledgerItems.size} txns",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldCoin,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (ledgerItems.isEmpty()) {
                        item {
                            EmptyFounderCard(
                                icon = Icons.Rounded.Assessment,
                                message = stringResource(R.string.empty_admin_records)
                            )
                        }
                    } else {
                        items(
                            items = ledgerItems,
                            key = { "ledger_${it.id}_${it.timestampMillis}" }
                        ) { entry ->
                            AdminExchangeRecordCard(entry = entry)
                        }
                    }
                }

                FounderTab.GENERATOR -> {
                    item {
                        CoinDiamondGeneratorSection(
                            users = uiState.managedUsers,
                            currentFounderUid = uiState.currentUserAccount?.userId ?: "",
                            initialTargetUserId = preselectedTargetUserId,
                            onGenerateForUser = onGenerateCurrency,
                            onBroadcastAll = onBroadcastCurrency,
                            onDeductFromUser = { uid, coins, diamonds, reason ->
                                onAdjustOrResetUser(uid, false, coins, diamonds, reason)
                            }
                        )
                    }

                    item {
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
                                text = "Founder Audit Trail",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (uiState.founderAuditLogs.isEmpty()) {
                        item {
                            EmptyFounderCard(
                                icon = Icons.Rounded.History,
                                message = "No Founder coin/diamond mints or control changes recorded yet."
                            )
                        }
                    } else {
                        items(
                            items = uiState.founderAuditLogs,
                            key = { "audit_${it.id}_${it.timestampMillis}" }
                        ) { log ->
                            FounderAuditCard(entry = log)
                        }
                    }
                }

                FounderTab.USERS -> {
                    item(key = "users_filter_header") {
                        UserManagementFilterHeader(
                            users = uiState.managedUsers,
                            searchQuery = userSearchQuery,
                            onSearchQueryChange = { userSearchQuery = it },
                            statusFilter = userStatusFilter,
                            onStatusFilterChange = { userStatusFilter = it }
                        )
                    }

                    if (filteredUsers.isEmpty()) {
                        item(key = "users_empty") {
                            EmptyFounderCard(
                                icon = Icons.Rounded.Group,
                                message = "No matching user accounts found."
                            )
                        }
                    } else {
                        items(
                            items = filteredUsers,
                            key = { "user_${it.userId}" }
                        ) { user ->
                            ManagedUserCard(
                                user = user,
                                isSelfFounder = user.userId == (uiState.currentUserAccount?.userId ?: ""),
                                onOpenGeneratorForUser = {
                                    preselectedTargetUserId = user.userId
                                    selectedTab = FounderTab.GENERATOR
                                },
                                onToggleStatus = {
                                    val nextStatus = if (user.isSuspended) {
                                        FounderConfig.STATUS_ACTIVE
                                    } else {
                                        FounderConfig.STATUS_SUSPENDED
                                    }
                                    onSetUserStatus(user.userId, nextStatus)
                                },
                                onRequestResetUser = {
                                    userPendingReset = user
                                }
                            )
                        }
                    }
                }

                FounderTab.CONTROLS -> {
                    item(key = "controls_system_card") {
                        SystemControlsCard(
                            exchangeEnabled = exchangeEnabled,
                            onToggleExchange = { next ->
                                exchangeEnabled = next
                                onUpdatePlatformSettings(
                                    settings.copy(exchangeEnabled = next),
                                    "APP_TOGGLE",
                                    "Set Diamond Exchange to ${if (next) "ENABLED" else "PAUSED"}"
                                )
                            },
                            maintenanceMode = maintenanceMode,
                            onToggleMaintenance = { next ->
                                maintenanceMode = next
                                onUpdatePlatformSettings(
                                    settings.copy(maintenanceMode = next),
                                    "APP_TOGGLE",
                                    "Set Maintenance Mode to ${if (next) "ON" else "OFF"}"
                                )
                            },
                            bannerText = bannerText,
                            onBannerTextChange = { bannerText = it }
                        )
                    }

                    item(key = "controls_room_pricing") {
                        RoomPricingEditorCard(
                            smallEntry = smallEntry,
                            onSmallEntryChange = { smallEntry = it },
                            smallPrize = smallPrize,
                            onSmallPrizeChange = { smallPrize = it },
                            medEntry = medEntry,
                            onMedEntryChange = { medEntry = it },
                            medPrize = medPrize,
                            onMedPrizeChange = { medPrize = it },
                            bigEntry = bigEntry,
                            onBigEntryChange = { bigEntry = it },
                            bigPrize = bigPrize,
                            onBigPrizeChange = { bigPrize = it },
                            vipEntry = vipEntry,
                            onVipEntryChange = { vipEntry = it },
                            vipPrize = vipPrize,
                            onVipPrizeChange = { vipPrize = it }
                        )
                    }

                    item(key = "controls_save_actions") {
                        ControlsSaveActionsRow(
                            onSaveAll = {
                                val next = settings.copy(
                                    exchangeEnabled = exchangeEnabled,
                                    maintenanceMode = maintenanceMode,
                                    announcementBanner = bannerText.trim(),
                                    smallEntryCoins = smallEntry.toLongOrNull() ?: settings.smallEntryCoins,
                                    smallPrizeDiamonds = smallPrize.toLongOrNull() ?: settings.smallPrizeDiamonds,
                                    mediumEntryCoins = medEntry.toLongOrNull() ?: settings.mediumEntryCoins,
                                    mediumPrizeDiamonds = medPrize.toLongOrNull() ?: settings.mediumPrizeDiamonds,
                                    bigEntryCoins = bigEntry.toLongOrNull() ?: settings.bigEntryCoins,
                                    bigPrizeDiamonds = bigPrize.toLongOrNull() ?: settings.bigPrizeDiamonds,
                                    vipEntryCoins = vipEntry.toLongOrNull() ?: settings.vipEntryCoins,
                                    vipPrizeDiamonds = vipPrize.toLongOrNull() ?: settings.vipPrizeDiamonds
                                )
                                onUpdatePlatformSettings(next, "ROOM_CONFIG", "Updated platform announcement & room pricing")
                            },
                            onResetDefaults = {
                                val defaults = PlatformSettings(
                                    totalCoinsGeneratedByFounder = settings.totalCoinsGeneratedByFounder,
                                    totalDiamondsGeneratedByFounder = settings.totalDiamondsGeneratedByFounder
                                )
                                onUpdatePlatformSettings(defaults, "ROOM_CONFIG", "Reset room pricing & controls to defaults")
                            }
                        )
                    }
                }
            }
        }

        val resetTarget = userPendingReset
        if (resetTarget != null) {
            AlertDialog(
                onDismissRequest = { userPendingReset = null },
                containerColor = ObsidianSurface,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                title = { Text("Reset User Wallet?") },
                text = {
                    Text("Reset Coins and Diamonds to 0 for ${resetTarget.displayName} (${resetTarget.email})? This action is logged in Founder Audit Trail.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onAdjustOrResetUser(resetTarget.userId, true, 0L, 0L, "Founder Reset Wallet")
                            userPendingReset = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralError, contentColor = TextPrimary)
                    ) {
                        Text("Reset to 0", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userPendingReset = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun CoinDiamondGeneratorSection(
    users: List<ManagedUserAccount>,
    currentFounderUid: String,
    initialTargetUserId: String?,
    onGenerateForUser: (String, Long, Long, String) -> Unit,
    onBroadcastAll: (Long, Long, String) -> Unit,
    onDeductFromUser: (String, Long, Long, String) -> Unit
) {
    var broadcastAll by remember { mutableStateOf(false) }
    var selectedUserId by remember(users, initialTargetUserId) {
        mutableStateOf(
            initialTargetUserId?.takeIf { id -> users.any { it.userId == id } }
                ?: users.firstOrNull { it.userId == currentFounderUid }?.userId
                ?: users.firstOrNull()?.userId
                ?: currentFounderUid
        )
    }
    var coinsInput by remember { mutableStateOf("5000") }
    var diamondsInput by remember { mutableStateOf("0") }
    var reasonInput by remember { mutableStateOf("Founder Bonus Credit") }

    val selectedUser = users.firstOrNull { it.userId == selectedUserId }
    val coinsAmt = coinsInput.toLongOrNull() ?: 0L
    val diamondsAmt = diamondsInput.toLongOrNull() ?: 0L

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("coin_generator_section")
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
                    imageVector = Icons.Rounded.MonetizationOn,
                    contentDescription = null,
                    tint = GoldCoin
                )
                Column {
                    Text(
                        text = "Coin & Diamond Generator",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldCoinBright,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mint Coins or Diamonds to any user, yourself, or broadcast to all active players",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Target Mode Toggle: Specific User vs Broadcast All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !broadcastAll,
                    onClick = { broadcastAll = false },
                    label = { Text("Single User") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanContainer,
                        selectedLabelColor = CyanDiamond
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = broadcastAll,
                    onClick = { broadcastAll = true },
                    label = { Text("All Active Users") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldContainer,
                        selectedLabelColor = GoldCoin
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            if (!broadcastAll && users.isNotEmpty()) {
                Text(
                    text = "Select Target Account",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    users.forEach { user ->
                        val isSelected = user.userId == selectedUserId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedUserId = user.userId },
                            label = {
                                val badge = if (user.isFounder) " (Founder)" else ""
                                Text(
                                    text = "${user.displayName}$badge",
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldCoin,
                                selectedLabelColor = TextDark,
                                containerColor = ObsidianCardElevated,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }
                if (selectedUser != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianCardElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedUser.email.ifBlank { selectedUser.userId },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "🪙 ${ExchangeCalculator.formatNumber(selectedUser.coins)}  •  💎 ${ExchangeCalculator.formatNumber(selectedUser.diamonds)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldCoin
                            )
                        }
                    }
                }
            }

            // Inputs for Coins & Diamonds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = coinsInput,
                    onValueChange = { coinsInput = it.filter { ch -> ch.isDigit() }.take(8) },
                    label = { Text("Coins (🪙)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldCoin,
                        unfocusedBorderColor = ObsidianOutline,
                        focusedTextColor = GoldCoin,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = GoldCoin
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("generator_coins_input")
                )
                OutlinedTextField(
                    value = diamondsInput,
                    onValueChange = { diamondsInput = it.filter { ch -> ch.isDigit() }.take(8) },
                    label = { Text("Diamonds (💎)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanDiamond,
                        unfocusedBorderColor = ObsidianOutline,
                        focusedTextColor = CyanDiamond,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = CyanDiamond
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("generator_diamonds_input")
                )
            }

            // Quick Coin Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1_000L to "+1K 🪙", 5_000L to "+5K 🪙", 25_000L to "+25K 🪙", 100_000L to "+100K 🪙").forEach { (amt, label) ->
                    OutlinedButton(
                        onClick = { coinsInput = amt.toString() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(text = label, color = GoldCoin, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            OutlinedTextField(
                value = reasonInput,
                onValueChange = { reasonInput = it.take(120) },
                label = { Text("Credit Note / Reason") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldCoin,
                    unfocusedBorderColor = ObsidianOutline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (coinsAmt > 0L || diamondsAmt > 0L) {
                            if (broadcastAll) {
                                onBroadcastAll(coinsAmt, diamondsAmt, reasonInput)
                            } else if (selectedUserId.isNotBlank()) {
                                onGenerateForUser(selectedUserId, coinsAmt, diamondsAmt, reasonInput)
                            }
                        }
                    },
                    enabled = (coinsAmt > 0L || diamondsAmt > 0L) && (broadcastAll || selectedUserId.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldCoin,
                        contentColor = TextDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("generator_credit_button")
                ) {
                    Text(
                        text = if (broadcastAll) "Broadcast Generate" else "Generate & Credit",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!broadcastAll && selectedUserId.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            if (coinsAmt > 0L || diamondsAmt > 0L) {
                                onDeductFromUser(selectedUserId, coinsAmt, diamondsAmt, reasonInput)
                            }
                        },
                        enabled = coinsAmt > 0L || diamondsAmt > 0L,
                        border = BorderStroke(1.dp, CoralError.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text(text = "Deduct", color = CoralError)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserManagementFilterHeader(
    users: List<ManagedUserAccount>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    statusFilter: String,
    onStatusFilterChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_management_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search users by name or email...") },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Search, contentDescription = null, tint = GoldCoin)
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldCoin,
                unfocusedBorderColor = ObsidianOutline,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_search_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "ALL" to "All (${users.size})",
                "ACTIVE" to "Active (${users.count { !it.isSuspended }})",
                "SUSPENDED" to "Suspended (${users.count { it.isSuspended }})"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = statusFilter == key,
                    onClick = { onStatusFilterChange(key) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldContainer,
                        selectedLabelColor = GoldCoin,
                        containerColor = ObsidianSurface,
                        labelColor = TextSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun ManagedUserCard(
    user: ManagedUserAccount,
    isSelfFounder: Boolean,
    onOpenGeneratorForUser: () -> Unit,
    onToggleStatus: () -> Unit,
    onRequestResetUser: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(
            1.dp,
            if (user.isSuspended) CoralError.copy(alpha = 0.55f) else ObsidianOutline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("managed_user_card_${user.userId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (user.isFounder) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = GoldContainer
                            ) {
                                Text(
                                    text = "FOUNDER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldCoinBright,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = user.email.ifBlank { user.userId },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (user.isSuspended) CoralError.copy(alpha = 0.16f) else EmeraldContainer
                ) {
                    Text(
                        text = if (user.isSuspended) "SUSPENDED" else "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (user.isSuspended) CoralError else EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🪙 ${ExchangeCalculator.formatNumber(user.coins)} Coins",
                    style = MaterialTheme.typography.titleSmall,
                    color = GoldCoin
                )
                Text(
                    text = "💎 ${ExchangeCalculator.formatNumber(user.diamonds)} Diamond",
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanDiamond
                )
                Text(
                    text = "Wins: ${user.gamesWon}/${user.gamesPlayed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenGeneratorForUser,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldCoin,
                        contentColor = TextDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("+Coins / 💎", fontWeight = FontWeight.Bold)
                }
                if (!isSelfFounder) {
                    OutlinedButton(
                        onClick = onToggleStatus,
                        border = BorderStroke(
                            1.dp,
                            if (user.isSuspended) EmeraldSuccess else CoralError.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("toggle_user_status_${user.userId}")
                    ) {
                        Icon(
                            imageVector = if (user.isSuspended) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                            contentDescription = null,
                            tint = if (user.isSuspended) EmeraldSuccess else CoralError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = if (user.isSuspended) "Activate" else "Suspend",
                            color = if (user.isSuspended) EmeraldSuccess else CoralError
                        )
                    }
                    OutlinedButton(
                        onClick = onRequestResetUser,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset Wallet",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemControlsCard(
    exchangeEnabled: Boolean,
    onToggleExchange: (Boolean) -> Unit,
    maintenanceMode: Boolean,
    onToggleMaintenance: (Boolean) -> Unit,
    bannerText: String,
    onBannerTextChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_controls_section")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Settings, contentDescription = null, tint = GoldCoin)
                Text(
                    text = "Application Controls",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Diamond → Coins Exchange",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = if (exchangeEnabled) "Active for all players" else "Paused by Founder",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (exchangeEnabled) EmeraldSuccess else CoralError
                    )
                }
                Switch(
                    checked = exchangeEnabled,
                    onCheckedChange = onToggleExchange,
                    colors = SwitchDefaults.colors(checkedThumbColor = TextDark, checkedTrackColor = EmeraldSuccess),
                    modifier = Modifier.testTag("toggle_exchange_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Maintenance Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = if (maintenanceMode) "Players blocked; Founder keeps full access" else "App live for all players",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (maintenanceMode) CoralError else TextSecondary
                    )
                }
                Switch(
                    checked = maintenanceMode,
                    onCheckedChange = onToggleMaintenance,
                    colors = SwitchDefaults.colors(checkedThumbColor = TextDark, checkedTrackColor = CoralError),
                    modifier = Modifier.testTag("toggle_maintenance_switch")
                )
            }

            OutlinedTextField(
                value = bannerText,
                onValueChange = { onBannerTextChange(it.take(300)) },
                label = { Text("Live Lobby Announcement Banner") },
                placeholder = { Text("e.g. Mega VIP Housie Tonight at 9 PM!") },
                leadingIcon = { Icon(Icons.Rounded.Campaign, contentDescription = null, tint = GoldCoin) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldCoin,
                    unfocusedBorderColor = ObsidianOutline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("announcement_banner_input")
            )
        }
    }
}

@Composable
private fun RoomPricingEditorCard(
    smallEntry: String,
    onSmallEntryChange: (String) -> Unit,
    smallPrize: String,
    onSmallPrizeChange: (String) -> Unit,
    medEntry: String,
    onMedEntryChange: (String) -> Unit,
    medPrize: String,
    onMedPrizeChange: (String) -> Unit,
    bigEntry: String,
    onBigEntryChange: (String) -> Unit,
    bigPrize: String,
    onBigPrizeChange: (String) -> Unit,
    vipEntry: String,
    onVipEntryChange: (String) -> Unit,
    vipPrize: String,
    onVipPrizeChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, CyanDiamond.copy(alpha = 0.4f)),
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
                Icon(imageVector = Icons.Rounded.CurrencyExchange, contentDescription = null, tint = CyanDiamond)
                Text(
                    text = "Housie Room Entry & Prize Manager",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanDiamond,
                    fontWeight = FontWeight.Bold
                )
            }

            RoomPricingRow("Small Room", smallEntry, onSmallEntryChange, smallPrize, onSmallPrizeChange)
            RoomPricingRow("Medium Room", medEntry, onMedEntryChange, medPrize, onMedPrizeChange)
            RoomPricingRow("Big Room", bigEntry, onBigEntryChange, bigPrize, onBigPrizeChange)
            RoomPricingRow("VIP Room", vipEntry, onVipEntryChange, vipPrize, onVipPrizeChange)
        }
    }
}

@Composable
private fun ControlsSaveActionsRow(
    onSaveAll: () -> Unit,
    onResetDefaults: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSaveAll,
                colors = ButtonDefaults.buttonColors(containerColor = GoldCoin, contentColor = TextDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("save_platform_settings_button")
            ) {
                Text("Save All Controls", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onResetDefaults,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Text("Reset", color = TextSecondary)
            }
        }
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun RoomPricingRow(
    roomLabel: String,
    entryValue: String,
    onEntryChange: (String) -> Unit,
    prizeValue: String,
    onPrizeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = roomLabel,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = entryValue,
                onValueChange = { onEntryChange(it.filter { ch -> ch.isDigit() }.take(8)) },
                label = { Text("Entry 🪙") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldCoin,
                    unfocusedBorderColor = ObsidianOutline,
                    focusedTextColor = GoldCoin,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = prizeValue,
                onValueChange = { onPrizeChange(it.filter { ch -> ch.isDigit() }.take(8)) },
                label = { Text("Prize 💎") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanDiamond,
                    unfocusedBorderColor = ObsidianOutline,
                    focusedTextColor = CyanDiamond,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AdminStatTile(
    label: String,
    value: String,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(testTag)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun AdminExchangeRecordCard(entry: PlatformExchangeLedgerItem) {
    val diamondsFmt = ExchangeCalculator.formatNumber(entry.diamondsExchanged)
    val netCoinsFmt = ExchangeCalculator.formatNumber(entry.netCoinsReceived)
    val grossCoinsFmt = ExchangeCalculator.formatNumber(entry.grossCoins)
    val feeFmt = ExchangeCalculator.formatNumber(entry.adminFeeCoins)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_record_item_${entry.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Exchanged $diamondsFmt Diamond -> $netCoinsFmt Coins",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    if (entry.userEmail.isNotBlank()) {
                        Text(
                            text = "${entry.userName} (${entry.userEmail})",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanDiamond,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldContainer
                ) {
                    Text(
                        text = "+$feeFmt 🪙 Profit",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gross: $grossCoinsFmt Coins • Fee: 10%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = formatTimestamp(entry.timestampMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun FounderAuditCard(entry: FounderAuditEntry) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = entry.actionType,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldCoin,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTimestamp(entry.timestampMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Text(
                text = entry.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Text(
                text = "Target: ${entry.targetEmail}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun EmptyFounderCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
