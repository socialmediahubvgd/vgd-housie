package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ExchangeExecutionResult
import com.example.data.ExchangeRecordEntity
import com.example.data.HousieDataRepository
import com.example.data.WalletActivityEntity
import com.example.data.WalletStateEntity
import com.example.domain.ChatMessage
import com.example.domain.ChatThread
import com.example.domain.ExchangeBreakdown
import com.example.domain.ExchangeCalculator
import com.example.domain.FounderAuditEntry
import com.example.domain.ManagedUserAccount
import com.example.domain.PlatformExchangeLedgerItem
import com.example.domain.PlatformSettings
import com.example.domain.VirtualPlayerLookup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExchangeSuccessBanner(
    val diamondsExchanged: Long,
    val netCoinsReceived: Long,
    val adminFeeCoins: Long,
    val eventId: Long = System.currentTimeMillis()
)

enum class WalletActionDialog {
    BUY_COINS,
    WITHDRAW_DIAMOND
}

data class WalletUiState(
    val userDiamonds: Long = ExchangeCalculator.INITIAL_DIAMONDS,
    val userCoins: Long = 0L,
    val adminFeeProfitCoins: Long = 0L,
    val totalExchangesCount: Int = 0,
    val totalDiamondsExchanged: Long = 0L,
    val totalCoinsDistributed: Long = 0L,
    val exchangeRecords: List<ExchangeRecordEntity> = emptyList(),
    val walletActivities: List<WalletActivityEntity> = emptyList(),
    val currentUserAccount: ManagedUserAccount? = null,
    val platformSettings: PlatformSettings = PlatformSettings(),
    val managedUsers: List<ManagedUserAccount> = emptyList(),
    val globalExchangeLedger: List<PlatformExchangeLedgerItem> = emptyList(),
    val founderAuditLogs: List<FounderAuditEntry> = emptyList(),
    val chatThreads: List<ChatThread> = emptyList(),
    val activeChatThread: ChatThread? = null,
    val activeChatMessages: List<ChatMessage> = emptyList(),
    val lookedUpPeer: VirtualPlayerLookup? = null,
    val lookupError: String? = null,
    val isSearchingPeer: Boolean = false,
    val isSavingProfile: Boolean = false,
    val selectedDiamonds: Long = ExchangeCalculator.INITIAL_DIAMONDS,
    val diamondsInputText: String = ExchangeCalculator.INITIAL_DIAMONDS.toString(),
    val breakdown: ExchangeBreakdown = ExchangeCalculator.calculate(
        ExchangeCalculator.INITIAL_DIAMONDS,
        ExchangeCalculator.INITIAL_DIAMONDS
    ),
    val showConfirmDialog: Boolean = false,
    val exchangeSuccess: ExchangeSuccessBanner? = null,
    val activeWalletDialog: WalletActionDialog? = null,
    val feedbackMessage: String? = null,
    val isSuspended: Boolean = false
)

private data class LocalExchangeControls(
    val selectedDiamonds: Long = ExchangeCalculator.INITIAL_DIAMONDS,
    val diamondsInputText: String = ExchangeCalculator.INITIAL_DIAMONDS.toString(),
    val hasUserEditedSelection: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val exchangeSuccess: ExchangeSuccessBanner? = null,
    val activeWalletDialog: WalletActionDialog? = null,
    val feedbackMessage: String? = null,
    val activeChatThread: ChatThread? = null,
    val lookedUpPeer: VirtualPlayerLookup? = null,
    val lookupError: String? = null,
    val isSearchingPeer: Boolean = false,
    val isSavingProfile: Boolean = false
)

private data class CoreWalletData(
    val wallet: WalletStateEntity = WalletStateEntity(),
    val records: List<ExchangeRecordEntity> = emptyList(),
    val activities: List<WalletActivityEntity> = emptyList(),
    val account: ManagedUserAccount? = null,
    val settings: PlatformSettings = PlatformSettings()
)

private data class FounderManagementData(
    val users: List<ManagedUserAccount> = emptyList(),
    val ledger: List<PlatformExchangeLedgerItem> = emptyList(),
    val auditLogs: List<FounderAuditEntry> = emptyList()
)

private data class SocialChatData(
    val threads: List<ChatThread> = emptyList(),
    val messages: List<ChatMessage> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModel(
    private val repository: HousieDataRepository
) : ViewModel() {

    private val localControls = MutableStateFlow(LocalExchangeControls())

    private val coreData = combine(
        repository.walletState,
        repository.exchangeRecords,
        repository.walletActivities,
        repository.currentUserAccount,
        repository.platformSettings
    ) { wallet, records, activities, account, settings ->
        CoreWalletData(
            wallet = wallet,
            records = records,
            activities = activities,
            account = account,
            settings = settings
        )
    }.catch { error ->
        if (error is CancellationException) throw error
        localControls.update { it.copy(feedbackMessage = error.localizedMessage ?: "Failed to sync wallet") }
        emit(CoreWalletData())
    }

    private val founderData = combine(
        repository.allUsersForFounder,
        repository.globalExchangeLedger,
        repository.founderAuditLogs
    ) { users, ledger, logs ->
        FounderManagementData(users = users, ledger = ledger, auditLogs = logs)
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(FounderManagementData())
    }

    private val activeMessagesFlow = localControls
        .map { it.activeChatThread?.chatId.orEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { chatId ->
            if (chatId.isBlank()) flowOf(emptyList()) else repository.observeChatMessages(chatId)
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }

    private val socialData = combine(
        repository.chatThreads.catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        },
        activeMessagesFlow
    ) { threads, messages ->
        SocialChatData(threads = threads, messages = messages)
    }

    val uiState: StateFlow<WalletUiState> = combine(
        coreData,
        founderData,
        socialData,
        localControls
    ) { core, founder, social, controls ->
        val wallet = core.wallet
        val effectiveSelected = if (!controls.hasUserEditedSelection) {
            ExchangeCalculator.defaultSelectionForBalance(wallet.diamonds)
        } else {
            controls.selectedDiamonds
        }
        val effectiveText = if (!controls.hasUserEditedSelection) {
            effectiveSelected.toString()
        } else {
            controls.diamondsInputText
        }
        val breakdown = ExchangeCalculator.calculate(effectiveSelected, wallet.diamonds)

        val currentThreadId = controls.activeChatThread?.chatId
        val liveActiveThread = if (currentThreadId != null) {
            social.threads.firstOrNull { it.chatId == currentThreadId } ?: controls.activeChatThread
        } else {
            null
        }

        WalletUiState(
            userDiamonds = wallet.diamonds,
            userCoins = wallet.coins,
            adminFeeProfitCoins = wallet.adminFeeProfitCoins,
            totalExchangesCount = wallet.totalExchangesCount,
            totalDiamondsExchanged = wallet.totalDiamondsExchanged,
            totalCoinsDistributed = wallet.totalCoinsDistributed,
            exchangeRecords = core.records,
            walletActivities = core.activities,
            currentUserAccount = core.account,
            platformSettings = core.settings,
            managedUsers = founder.users,
            globalExchangeLedger = founder.ledger,
            founderAuditLogs = founder.auditLogs,
            chatThreads = social.threads,
            activeChatThread = liveActiveThread,
            activeChatMessages = social.messages,
            lookedUpPeer = controls.lookedUpPeer,
            lookupError = controls.lookupError,
            isSearchingPeer = controls.isSearchingPeer,
            isSavingProfile = controls.isSavingProfile,
            selectedDiamonds = effectiveSelected,
            diamondsInputText = effectiveText,
            breakdown = breakdown,
            showConfirmDialog = controls.showConfirmDialog,
            exchangeSuccess = controls.exchangeSuccess,
            activeWalletDialog = controls.activeWalletDialog,
            feedbackMessage = controls.feedbackMessage,
            isSuspended = core.account?.isSuspended == true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletUiState()
    )

    init {
        viewModelScope.launch {
            repository.initialize()
        }
    }

    fun onExchangePageEntered() {
        val currentDiamonds = uiState.value.userDiamonds
        val defaultSelection = ExchangeCalculator.defaultSelectionForBalance(currentDiamonds)
        localControls.update {
            it.copy(
                selectedDiamonds = defaultSelection,
                diamondsInputText = defaultSelection.toString(),
                hasUserEditedSelection = false
            )
        }
    }

    fun onDiamondsTextChanged(rawInput: String) {
        val digitsOnly = rawInput.filter { it.isDigit() }.take(9)
        val parsed = digitsOnly.toLongOrNull() ?: 0L
        localControls.update {
            it.copy(
                selectedDiamonds = parsed,
                diamondsInputText = digitsOnly,
                hasUserEditedSelection = true
            )
        }
    }

    fun onSliderChanged(sliderValue: Float) {
        val maxAvailable = uiState.value.userDiamonds.coerceAtLeast(ExchangeCalculator.MIN_EXCHANGE_DIAMONDS)
        val rounded = sliderValue.toLong().coerceIn(ExchangeCalculator.MIN_EXCHANGE_DIAMONDS, maxAvailable)
        localControls.update {
            it.copy(
                selectedDiamonds = rounded,
                diamondsInputText = rounded.toString(),
                hasUserEditedSelection = true
            )
        }
    }

    fun stepDiamonds(delta: Long) {
        val current = uiState.value.selectedDiamonds
        val maxAvailable = uiState.value.userDiamonds.coerceAtLeast(ExchangeCalculator.MIN_EXCHANGE_DIAMONDS)
        val next = (current + delta).coerceIn(ExchangeCalculator.MIN_EXCHANGE_DIAMONDS, maxAvailable)
        localControls.update {
            it.copy(
                selectedDiamonds = next,
                diamondsInputText = next.toString(),
                hasUserEditedSelection = true
            )
        }
    }

    fun selectPresetDiamonds(amount: Long) {
        val clamped = amount.coerceAtLeast(0L)
        localControls.update {
            it.copy(
                selectedDiamonds = clamped,
                diamondsInputText = clamped.toString(),
                hasUserEditedSelection = true
            )
        }
    }

    fun requestExchangeConfirmation() {
        if (!uiState.value.platformSettings.exchangeEnabled && uiState.value.currentUserAccount?.isFounder != true) {
            localControls.update { it.copy(feedbackMessage = "Diamond Exchange is temporarily paused by Founder.") }
            return
        }
        if (uiState.value.breakdown.isValid) {
            localControls.update { it.copy(showConfirmDialog = true) }
        }
    }

    fun dismissConfirmDialog() {
        localControls.update { it.copy(showConfirmDialog = false) }
    }

    fun confirmExchange() {
        val selected = uiState.value.selectedDiamonds
        localControls.update { it.copy(showConfirmDialog = false) }
        viewModelScope.launch {
            when (val result = repository.executeExchange(selected)) {
                is ExchangeExecutionResult.Success -> {
                    val remainingDiamonds = (uiState.value.userDiamonds - result.diamondsExchanged).coerceAtLeast(0L)
                    val nextDefault = ExchangeCalculator.defaultSelectionForBalance(remainingDiamonds)
                    localControls.update {
                        it.copy(
                            selectedDiamonds = nextDefault,
                            diamondsInputText = nextDefault.toString(),
                            hasUserEditedSelection = false,
                            exchangeSuccess = ExchangeSuccessBanner(
                                diamondsExchanged = result.diamondsExchanged,
                                netCoinsReceived = result.netCoinsReceived,
                                adminFeeCoins = result.adminFeeCoins
                            )
                        )
                    }
                }
                is ExchangeExecutionResult.Failure -> {
                    localControls.update {
                        it.copy(feedbackMessage = "Unable to complete exchange. Check your balance and minimum 150 Diamond.")
                    }
                }
            }
        }
    }

    fun dismissSuccessModal() {
        localControls.update { it.copy(exchangeSuccess = null) }
    }

    fun openBuyCoinsDialog() {
        localControls.update { it.copy(activeWalletDialog = WalletActionDialog.BUY_COINS) }
    }

    fun openWithdrawDiamondDialog() {
        localControls.update { it.copy(activeWalletDialog = WalletActionDialog.WITHDRAW_DIAMOND) }
    }

    fun dismissWalletDialog() {
        localControls.update { it.copy(activeWalletDialog = null) }
    }

    fun buyCoins(amount: Long) {
        localControls.update { it.copy(activeWalletDialog = null) }
        viewModelScope.launch {
            val res = repository.buyCoins(amount)
            localControls.update {
                it.copy(
                    feedbackMessage = if (res.isSuccess) {
                        "Added ${ExchangeCalculator.formatNumber(amount)} Coins to Wallet"
                    } else {
                        res.exceptionOrNull()?.localizedMessage ?: "Failed to purchase coins"
                    }
                )
            }
        }
    }

    fun withdrawDiamonds(amount: Long, destination: String) {
        localControls.update { it.copy(activeWalletDialog = null) }
        viewModelScope.launch {
            val res = repository.withdrawDiamonds(amount, destination.ifBlank { "Primary UPI" })
            localControls.update {
                it.copy(
                    feedbackMessage = when {
                        res.isFailure -> res.exceptionOrNull()?.localizedMessage ?: "Withdrawal failed"
                        res.getOrNull() == true -> "Withdrew ${ExchangeCalculator.formatNumber(amount)} Diamond"
                        else -> "Insufficient Diamond balance for withdrawal"
                    }
                )
            }
        }
    }

    fun addBonusDiamonds(amount: Long) {
        viewModelScope.launch {
            val res = repository.addBonusDiamonds(amount)
            localControls.update {
                it.copy(
                    feedbackMessage = if (res.isSuccess) {
                        "Added +${ExchangeCalculator.formatNumber(amount)} Diamond to Wallet"
                    } else {
                        res.exceptionOrNull()?.localizedMessage ?: "Credit failed"
                    }
                )
            }
        }
    }

    fun founderGenerateCurrency(
        targetUserId: String,
        coinsToAdd: Long,
        diamondsToAdd: Long,
        reason: String
    ) {
        viewModelScope.launch {
            val res = repository.founderGenerateCurrency(targetUserId, coinsToAdd, diamondsToAdd, reason)
            localControls.update {
                it.copy(
                    feedbackMessage = if (res.isSuccess) {
                        "Generated & credited successfully!"
                    } else {
                        res.exceptionOrNull()?.localizedMessage ?: "Generation failed"
                    }
                )
            }
        }
    }

    fun founderBroadcastCurrency(coinsToAdd: Long, diamondsToAdd: Long, reason: String) {
        viewModelScope.launch {
            val res = repository.founderBroadcastCurrency(coinsToAdd, diamondsToAdd, reason)
            localControls.update {
                it.copy(
                    feedbackMessage = if (res.isSuccess) {
                        "Broadcast credited to ${res.getOrNull() ?: 0} active users!"
                    } else {
                        res.exceptionOrNull()?.localizedMessage ?: "Broadcast failed"
                    }
                )
            }
        }
    }

    fun founderAdjustOrResetUser(
        targetUserId: String,
        resetToZero: Boolean,
        coinsToDeduct: Long,
        diamondsToDeduct: Long,
        reason: String
    ) {
        viewModelScope.launch {
            val res = repository.founderAdjustOrResetUser(
                targetUserId = targetUserId,
                resetToZero = resetToZero,
                coinsToDeduct = coinsToDeduct,
                diamondsToDeduct = diamondsToDeduct,
                reason = reason
            )
            localControls.update {
                it.copy(
                    feedbackMessage = if (res.isSuccess) {
                        if (resetToZero) "User wallet reset to 0" else "User wallet adjusted"
                    } else {
                        res.exceptionOrNull()?.localizedMessage ?: "Adjustment failed"
                    }
                )
            }
        }
    }

    fun founderSetUserStatus(targetUserId: String, newStatus: String) {
        viewModelScope.launch {
            val res = repository.founderSetUserStatus(targetUserId, newStatus)
            localControls.update {
                it.copy(
                    feedbackMessage = if (res.isSuccess) {
                        "User status updated to ${newStatus.uppercase()}"
                    } else {
                        res.exceptionOrNull()?.localizedMessage ?: "Failed to update user status"
                    }
                )
            }
        }
    }

    fun founderUpdatePlatformSettings(
        settings: PlatformSettings,
        actionType: String,
        summary: String
    ) {
        viewModelScope.launch {
            val res = repository.founderUpdatePlatformSettings(settings, actionType, summary)
            localControls.update {
                it.copy(
                    feedbackMessage = if (res.isSuccess) {
                        "Platform settings saved & synced live!"
                    } else {
                        res.exceptionOrNull()?.localizedMessage ?: "Failed to update platform settings"
                    }
                )
            }
        }
    }

    fun updateProfile(
        displayName: String,
        nickname: String,
        gender: String,
        dob: String,
        photoBase64: String
    ) {
        viewModelScope.launch {
            localControls.update { it.copy(isSavingProfile = true) }
            val res = repository.updateUserProfile(
                displayName = displayName,
                nickname = nickname,
                gender = gender,
                dob = dob,
                photoBase64 = photoBase64
            )
            localControls.update {
                it.copy(
                    isSavingProfile = false,
                    feedbackMessage = if (res.isSuccess) {
                        "Profile updated successfully!"
                    } else {
                        res.exceptionOrNull()?.localizedMessage ?: "Failed to save profile"
                    }
                )
            }
        }
    }

    fun searchPlayerByVirtualId(rawVirtualId: String) {
        viewModelScope.launch {
            localControls.update {
                it.copy(
                    isSearchingPeer = true,
                    lookupError = null,
                    lookedUpPeer = null
                )
            }
            val res = repository.lookupPlayerByVirtualId(rawVirtualId)
            val currentUid = uiState.value.currentUserAccount?.userId
            res.fold(
                onSuccess = { found ->
                    when {
                        found == null -> localControls.update {
                            it.copy(
                                isSearchingPeer = false,
                                lookedUpPeer = null,
                                lookupError = "No player found with that Virtual ID"
                            )
                        }
                        found.userId == currentUid -> localControls.update {
                            it.copy(
                                isSearchingPeer = false,
                                lookedUpPeer = null,
                                lookupError = "That is your own Virtual ID! Enter a friend's Virtual ID."
                            )
                        }
                        else -> localControls.update {
                            it.copy(
                                isSearchingPeer = false,
                                lookedUpPeer = found,
                                lookupError = null
                            )
                        }
                    }
                },
                onFailure = { err ->
                    localControls.update {
                        it.copy(
                            isSearchingPeer = false,
                            lookedUpPeer = null,
                            lookupError = err.localizedMessage ?: "Search failed"
                        )
                    }
                }
            )
        }
    }

    fun clearPeerLookup() {
        localControls.update { it.copy(lookedUpPeer = null, lookupError = null, isSearchingPeer = false) }
    }

    fun openChatWithPeer(peer: VirtualPlayerLookup) {
        viewModelScope.launch {
            val res = repository.openOrCreateDirectChat(peer)
            res.fold(
                onSuccess = { thread ->
                    localControls.update {
                        it.copy(
                            activeChatThread = thread,
                            lookedUpPeer = null,
                            lookupError = null
                        )
                    }
                },
                onFailure = { err ->
                    localControls.update {
                        it.copy(feedbackMessage = err.localizedMessage ?: "Unable to open 1v1 chat")
                    }
                }
            )
        }
    }

    fun openChatThread(thread: ChatThread) {
        localControls.update { it.copy(activeChatThread = thread) }
    }

    fun closeActiveChat() {
        localControls.update { it.copy(activeChatThread = null) }
    }

    fun sendChatMessage(text: String) {
        val thread = uiState.value.activeChatThread ?: return
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        val peer = VirtualPlayerLookup(
            virtualId = thread.peerVirtualId,
            userId = thread.peerUserId,
            displayName = thread.peerName,
            photoBase64 = thread.peerPhotoBase64
        )
        viewModelScope.launch {
            val res = repository.sendDirectMessage(thread.chatId, peer, cleanText)
            if (res.isFailure) {
                localControls.update {
                    it.copy(feedbackMessage = res.exceptionOrNull()?.localizedMessage ?: "Message failed to send")
                }
            }
        }
    }

    fun clearFeedbackMessage() {
        localControls.update { it.copy(feedbackMessage = null) }
    }

    class Factory(private val repository: HousieDataRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WalletViewModel(repository) as T
        }
    }
}
