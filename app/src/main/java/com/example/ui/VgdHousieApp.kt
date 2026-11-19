package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.domain.ExchangeCalculator
import com.example.domain.FounderConfig
import com.example.ui.components.BuyCoinsDialog
import com.example.ui.components.ConfettiSuccessModal
import com.example.ui.components.ConfirmExchangeDialog
import com.example.ui.components.HousieTutorialModal
import com.example.ui.components.WithdrawDiamondDialog
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.ExchangeScreen
import com.example.ui.screens.GameBoardScreen
import com.example.ui.screens.HomeRoomsScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.CoralError
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

object VgdRoutes {
    const val ROUTE_HOME = "home_rooms"
    const val ROUTE_WALLET = "wallet"
    const val ROUTE_EXCHANGE = "exchange"
    const val ROUTE_INBOX = "inbox"
    const val ROUTE_PROFILE = "profile"
    const val ROUTE_ADMIN = "admin_panel"
    const val ROUTE_GAME_BOARD = "game_board"
}

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val playerDestinations = listOf(
    NavDestinationItem(
        route = VgdRoutes.ROUTE_HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Rounded.Casino,
        unselectedIcon = Icons.Outlined.Casino,
        testTag = "nav_home"
    ),
    NavDestinationItem(
        route = VgdRoutes.ROUTE_WALLET,
        labelRes = R.string.nav_wallet,
        selectedIcon = Icons.Rounded.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "nav_wallet"
    ),
    NavDestinationItem(
        route = VgdRoutes.ROUTE_EXCHANGE,
        labelRes = R.string.nav_exchange,
        selectedIcon = Icons.Rounded.CurrencyExchange,
        unselectedIcon = Icons.Outlined.CurrencyExchange,
        testTag = "nav_exchange"
    ),
    NavDestinationItem(
        route = VgdRoutes.ROUTE_INBOX,
        labelRes = R.string.nav_inbox,
        selectedIcon = Icons.Rounded.Forum,
        unselectedIcon = Icons.Outlined.Forum,
        testTag = "nav_inbox"
    ),
    NavDestinationItem(
        route = VgdRoutes.ROUTE_PROFILE,
        labelRes = R.string.nav_profile,
        selectedIcon = Icons.Rounded.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_profile"
    )
)

private val founderDestination = NavDestinationItem(
    route = VgdRoutes.ROUTE_ADMIN,
    labelRes = R.string.nav_admin,
    selectedIcon = Icons.Rounded.AdminPanelSettings,
    unselectedIcon = Icons.Outlined.AdminPanelSettings,
    testTag = "nav_admin"
)

@Composable
fun VgdHousieApp(
    viewModel: WalletViewModel,
    gameViewModel: HousieGameViewModel,
    isFounder: Boolean = false,
    currentUserEmail: String = FounderConfig.FOUNDER_EMAIL,
    onSignOutClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isInGameBoard = currentDestination?.route == VgdRoutes.ROUTE_GAME_BOARD
    val snackbarHostState = remember { SnackbarHostState() }
    var showTutorial by remember { mutableStateOf(false) }

    val topDestinations = remember(isFounder) {
        if (isFounder) playerDestinations + founderDestination else playerDestinations
    }

    LaunchedEffect(uiState.feedbackMessage) {
        val message = uiState.feedbackMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeedbackMessage()
        }
    }

    fun navigateToTab(route: String) {
        if (route == VgdRoutes.ROUTE_ADMIN && !isFounder) return
        if (route == VgdRoutes.ROUTE_EXCHANGE) {
            viewModel.onExchangePageEntered()
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = ObsidianBg,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavRail && !isInGameBoard) {
                        NavigationBar(
                            containerColor = ObsidianSurface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            topDestinations.forEach { dest ->
                                val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                                val activeColor = when (dest.route) {
                                    VgdRoutes.ROUTE_HOME, VgdRoutes.ROUTE_ADMIN -> GoldCoin
                                    else -> CyanDiamond
                                }
                                val activeContainer = when (dest.route) {
                                    VgdRoutes.ROUTE_HOME, VgdRoutes.ROUTE_ADMIN -> GoldContainer
                                    else -> CyanContainer
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(dest.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = activeColor,
                                        selectedTextColor = activeColor,
                                        indicatorColor = activeContainer,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail && !isInGameBoard) {
                        NavigationRail(
                            containerColor = ObsidianSurface,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            topDestinations.forEach { dest ->
                                val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                                val activeColor = when (dest.route) {
                                    VgdRoutes.ROUTE_HOME, VgdRoutes.ROUTE_ADMIN -> GoldCoin
                                    else -> CyanDiamond
                                }
                                val activeContainer = when (dest.route) {
                                    VgdRoutes.ROUTE_HOME, VgdRoutes.ROUTE_ADMIN -> GoldContainer
                                    else -> CyanContainer
                                }
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = activeColor,
                                        selectedTextColor = activeColor,
                                        indicatorColor = activeContainer,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = VgdRoutes.ROUTE_HOME,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        composable(VgdRoutes.ROUTE_HOME) {
                            HomeRoomsScreen(
                                walletState = uiState,
                                isFounder = isFounder,
                                currentUserEmail = currentUserEmail,
                                onSignOutClick = onSignOutClick,
                                onJoinRoom = { room ->
                                    gameViewModel.joinRoom(room) {
                                        navController.navigate(VgdRoutes.ROUTE_GAME_BOARD) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onBuyCoinsClick = viewModel::openBuyCoinsDialog,
                                onExchangeClick = { navigateToTab(VgdRoutes.ROUTE_EXCHANGE) },
                                onOpenWalletClick = { navigateToTab(VgdRoutes.ROUTE_WALLET) },
                                onShowTutorial = { showTutorial = true },
                                onOpenProfileClick = { navigateToTab(VgdRoutes.ROUTE_PROFILE) },
                                onOpenInboxClick = { navigateToTab(VgdRoutes.ROUTE_INBOX) }
                            )
                        }

                        composable(VgdRoutes.ROUTE_GAME_BOARD) {
                            GameBoardScreen(
                                gameState = gameState,
                                walletState = uiState,
                                onClaimWin = gameViewModel::claimWin,
                                onDismissWinPopup = gameViewModel::dismissWinPopup,
                                onDismissWrongClaim = gameViewModel::dismissWrongClaim,
                                onPlayAgain = {
                                    gameViewModel.leaveGame()
                                    navController.popBackStack(VgdRoutes.ROUTE_HOME, inclusive = false)
                                },
                                onShowTutorial = { showTutorial = true }
                            )
                        }

                        composable(VgdRoutes.ROUTE_WALLET) {
                            WalletScreen(
                                uiState = uiState,
                                onBuyCoinsClick = viewModel::openBuyCoinsDialog,
                                onWithdrawDiamondClick = viewModel::openWithdrawDiamondDialog,
                                onExchangeDiamondClick = { navigateToTab(VgdRoutes.ROUTE_EXCHANGE) },
                                onPlayHousieClick = { navigateToTab(VgdRoutes.ROUTE_HOME) }
                            )
                        }

                        composable(VgdRoutes.ROUTE_EXCHANGE) {
                            ExchangeScreen(
                                uiState = uiState,
                                onDiamondsTextChanged = viewModel::onDiamondsTextChanged,
                                onSliderChanged = viewModel::onSliderChanged,
                                onStepDiamonds = viewModel::stepDiamonds,
                                onSelectPreset = viewModel::selectPresetDiamonds,
                                onExchangeNowClick = viewModel::requestExchangeConfirmation,
                                onBackToWallet = { navigateToTab(VgdRoutes.ROUTE_WALLET) }
                            )
                        }

                        composable(VgdRoutes.ROUTE_INBOX) {
                            InboxScreen(
                                uiState = uiState,
                                onSearchVirtualId = viewModel::searchPlayerByVirtualId,
                                onStartChatWithPeer = viewModel::openChatWithPeer,
                                onOpenThread = viewModel::openChatThread,
                                onCloseActiveChat = viewModel::closeActiveChat,
                                onSendMessage = viewModel::sendChatMessage
                            )
                        }

                        composable(VgdRoutes.ROUTE_PROFILE) {
                            ProfileScreen(
                                uiState = uiState,
                                currentUserEmail = currentUserEmail,
                                onSaveProfile = viewModel::updateProfile,
                                onSignOutClick = onSignOutClick
                            )
                        }

                        composable(VgdRoutes.ROUTE_ADMIN) {
                            if (isFounder) {
                                AdminPanelScreen(
                                    uiState = uiState,
                                    founderEmail = currentUserEmail,
                                    onAddBonusDiamonds = viewModel::addBonusDiamonds,
                                    onGenerateCurrency = viewModel::founderGenerateCurrency,
                                    onBroadcastCurrency = viewModel::founderBroadcastCurrency,
                                    onAdjustOrResetUser = viewModel::founderAdjustOrResetUser,
                                    onSetUserStatus = viewModel::founderSetUserStatus,
                                    onUpdatePlatformSettings = viewModel::founderUpdatePlatformSettings
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Founder Access Only",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = CoralError
                                        )
                                        Button(onClick = { navigateToTab(VgdRoutes.ROUTE_HOME) }) {
                                            Text("Back to Rooms")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showConfirmDialog) {
            ConfirmExchangeDialog(
                breakdown = uiState.breakdown,
                onConfirm = viewModel::confirmExchange,
                onDismiss = viewModel::dismissConfirmDialog
            )
        }

        val insufficientRoom = gameState.insufficientCoinsRoom
        if (insufficientRoom != null) {
            BuyCoinsDialog(
                customPrompt = "Need ${ExchangeCalculator.formatNumber(insufficientRoom.entryCoins)} Coins to join ${insufficientRoom.name}. Select a bundle:",
                onSelectBundle = { amount ->
                    gameViewModel.dismissInsufficientCoinsPrompt()
                    viewModel.buyCoins(amount)
                },
                onDismiss = gameViewModel::dismissInsufficientCoinsPrompt
            )
        } else {
            when (uiState.activeWalletDialog) {
                WalletActionDialog.BUY_COINS -> {
                    BuyCoinsDialog(
                        onSelectBundle = viewModel::buyCoins,
                        onDismiss = viewModel::dismissWalletDialog
                    )
                }
                WalletActionDialog.WITHDRAW_DIAMOND -> {
                    WithdrawDiamondDialog(
                        availableDiamonds = uiState.userDiamonds,
                        onWithdraw = viewModel::withdrawDiamonds,
                        onDismiss = viewModel::dismissWalletDialog
                    )
                }
                null -> Unit
            }
        }

        ConfettiSuccessModal(
            successBanner = uiState.exchangeSuccess,
            onDone = viewModel::dismissSuccessModal,
            onViewWallet = {
                viewModel.dismissSuccessModal()
                navigateToTab(VgdRoutes.ROUTE_WALLET)
            }
        )

        HousieTutorialModal(
            visible = showTutorial,
            onDismiss = { showTutorial = false }
        )
    }
}
