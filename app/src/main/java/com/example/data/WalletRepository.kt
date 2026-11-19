package com.example.data

import com.example.domain.ChatMessage
import com.example.domain.ChatThread
import com.example.domain.ExchangeCalculator
import com.example.domain.FounderAuditEntry
import com.example.domain.FounderConfig
import com.example.domain.ManagedUserAccount
import com.example.domain.PlatformExchangeLedgerItem
import com.example.domain.PlatformSettings
import com.example.domain.VirtualIdGenerator
import com.example.domain.VirtualPlayerLookup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

class WalletRepository(
    private val walletDao: WalletDao,
    private val sessionUserId: String = "local_user",
    private val sessionEmail: String = FounderConfig.FOUNDER_EMAIL,
    private val sessionName: String = "VGD Player",
    private val isFounderSession: Boolean = true
) : HousieDataRepository {

    private data class LocalProfileMeta(
        val virtualId: String,
        val displayName: String,
        val nickname: String = "",
        val gender: String = "",
        val dob: String = "",
        val photoBase64: String = ""
    )

    private val localProfileMeta = MutableStateFlow(
        LocalProfileMeta(
            virtualId = VirtualIdGenerator.fromUid(sessionUserId),
            displayName = sessionName
        )
    )

    private val localPlatformSettings = MutableStateFlow(PlatformSettings())
    private val localManagedUsers = MutableStateFlow(
        listOf(
            ManagedUserAccount(
                userId = "player_demo_1",
                email = "rahul.housie@gmail.com",
                displayName = "Rahul Sharma",
                role = FounderConfig.ROLE_PLAYER,
                accountStatus = FounderConfig.STATUS_ACTIVE,
                diamonds = 2_500L,
                coins = 1_200L,
                totalExchangesCount = 2,
                totalDiamondsExchanged = 1_500L,
                totalCoinsDistributed = 900L,
                gamesPlayed = 5,
                gamesWon = 1,
                virtualId = "VGD-RAHUL1",
                nickname = "LuckyRahul",
                gender = "Male",
                dob = "14/08/1998"
            ),
            ManagedUserAccount(
                userId = "player_demo_2",
                email = "priya.vgd@gmail.com",
                displayName = "Priya Verma",
                role = FounderConfig.ROLE_PLAYER,
                accountStatus = FounderConfig.STATUS_ACTIVE,
                diamonds = 7_500L,
                coins = 4_000L,
                totalExchangesCount = 1,
                totalDiamondsExchanged = 1_500L,
                totalCoinsDistributed = 900L,
                gamesPlayed = 8,
                gamesWon = 2,
                virtualId = "VGD-PRIYA2",
                nickname = "QueenPriya",
                gender = "Female",
                dob = "22/03/2000"
            )
        )
    )
    private val localAuditLogs = MutableStateFlow<List<FounderAuditEntry>>(emptyList())
    private val localChatThreads = MutableStateFlow<List<ChatThread>>(emptyList())
    private val localChatMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    override val walletState: Flow<WalletStateEntity> = walletDao.observeWalletState()
        .map { it ?: WalletStateEntity() }

    override val exchangeRecords: Flow<List<ExchangeRecordEntity>> = walletDao.observeExchangeRecords()

    override val walletActivities: Flow<List<WalletActivityEntity>> = walletDao.observeWalletActivities()

    override val currentUserAccount: Flow<ManagedUserAccount?> = combine(
        walletState,
        localProfileMeta
    ) { wallet, meta ->
        ManagedUserAccount(
            userId = sessionUserId,
            email = sessionEmail,
            displayName = meta.displayName,
            role = if (isFounderSession) FounderConfig.ROLE_FOUNDER else FounderConfig.ROLE_PLAYER,
            accountStatus = FounderConfig.STATUS_ACTIVE,
            diamonds = wallet.diamonds,
            coins = wallet.coins,
            totalExchangesCount = wallet.totalExchangesCount,
            totalDiamondsExchanged = wallet.totalDiamondsExchanged,
            totalCoinsDistributed = wallet.totalCoinsDistributed,
            gamesPlayed = 0,
            gamesWon = 0,
            virtualId = meta.virtualId,
            nickname = meta.nickname,
            gender = meta.gender,
            dob = meta.dob,
            photoBase64 = meta.photoBase64
        )
    }

    override val platformSettings: Flow<PlatformSettings> = localPlatformSettings

    override val chatThreads: Flow<List<ChatThread>> = localChatThreads.map { list ->
        list.sortedByDescending { it.lastMessageAtMillis }
    }

    override fun observeChatMessages(chatId: String): Flow<List<ChatMessage>> =
        localChatMessages.map { map ->
            (map[chatId] ?: emptyList()).sortedBy { it.timestampMillis }
        }

    override val allUsersForFounder: Flow<List<ManagedUserAccount>> =
        if (!isFounderSession) {
            MutableStateFlow(emptyList())
        } else {
            combine(currentUserAccount, localManagedUsers) { self, others ->
                listOfNotNull(self) + others
            }
        }

    override val globalExchangeLedger: Flow<List<PlatformExchangeLedgerItem>> =
        if (!isFounderSession) {
            MutableStateFlow(emptyList())
        } else {
            exchangeRecords.map { list ->
                list.map { rec ->
                    PlatformExchangeLedgerItem(
                        id = rec.id.toString(),
                        userId = sessionUserId,
                        userEmail = sessionEmail,
                        userName = sessionName,
                        diamondsExchanged = rec.diamondsExchanged,
                        grossCoins = rec.grossCoins,
                        netCoinsReceived = rec.netCoinsReceived,
                        adminFeeCoins = rec.adminFeeCoins,
                        timestampMillis = rec.timestampMillis
                    )
                }
            }
        }

    override val founderAuditLogs: Flow<List<FounderAuditEntry>> =
        if (!isFounderSession) MutableStateFlow(emptyList()) else localAuditLogs

    override suspend fun initialize() {
        walletDao.ensureSeeded()
    }

    override suspend fun executeExchange(diamonds: Long): ExchangeExecutionResult {
        val current = walletDao.getWalletState() ?: WalletStateEntity()
        val breakdown = ExchangeCalculator.calculate(diamonds, current.diamonds)
        if (!breakdown.isValid) {
            return ExchangeExecutionResult.Failure(breakdown.validationError)
        }
        val committed = walletDao.performExchange(
            diamonds = breakdown.diamonds,
            grossCoins = breakdown.grossCoins,
            netUserCoins = breakdown.netUserCoins,
            adminFeeCoins = breakdown.adminFeeCoins,
            timestampMillis = System.currentTimeMillis()
        )
        return if (committed) {
            ExchangeExecutionResult.Success(
                diamondsExchanged = breakdown.diamonds,
                netCoinsReceived = breakdown.netUserCoins,
                adminFeeCoins = breakdown.adminFeeCoins
            )
        } else {
            ExchangeExecutionResult.Failure(null)
        }
    }

    override suspend fun buyCoins(coinsAmount: Long): Result<Unit> {
        walletDao.buyCoins(coinsAmount, System.currentTimeMillis())
        return Result.success(Unit)
    }

    override suspend fun withdrawDiamonds(diamondsAmount: Long, destination: String): Result<Boolean> {
        return Result.success(
            walletDao.withdrawDiamonds(diamondsAmount, destination, System.currentTimeMillis())
        )
    }

    override suspend fun addBonusDiamonds(diamondsAmount: Long): Result<Unit> {
        walletDao.addBonusDiamonds(diamondsAmount, System.currentTimeMillis())
        return Result.success(Unit)
    }

    override suspend fun joinGameRoom(roomName: String, entryCoins: Long): Result<Boolean> {
        return Result.success(
            walletDao.joinGameRoom(roomName, entryCoins, System.currentTimeMillis())
        )
    }

    override suspend fun awardGamePrize(roomName: String, claimName: String, prizeDiamonds: Long): Result<Unit> {
        walletDao.awardGamePrize(roomName, claimName, prizeDiamonds, System.currentTimeMillis())
        return Result.success(Unit)
    }

    override suspend fun updateUserProfile(
        displayName: String,
        nickname: String,
        gender: String,
        dob: String,
        photoBase64: String
    ): Result<Unit> {
        val cleanName = displayName.trim().take(100)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be blank"))
        }
        localProfileMeta.update {
            it.copy(
                displayName = cleanName,
                nickname = nickname.trim().take(40),
                gender = gender.trim().take(30),
                dob = dob.trim().take(20),
                photoBase64 = photoBase64.take(150_000)
            )
        }
        return Result.success(Unit)
    }

    override suspend fun lookupPlayerByVirtualId(rawVirtualId: String): Result<VirtualPlayerLookup?> {
        val normalized = VirtualIdGenerator.normalize(rawVirtualId)
        if (!VirtualIdGenerator.isValid(normalized)) {
            return Result.failure(IllegalArgumentException("Enter a valid Virtual ID (e.g., VGD-RAHUL1)"))
        }
        val selfMeta = localProfileMeta.value
        if (selfMeta.virtualId.equals(normalized, ignoreCase = true)) {
            return Result.success(
                VirtualPlayerLookup(
                    virtualId = selfMeta.virtualId,
                    userId = sessionUserId,
                    displayName = selfMeta.displayName,
                    nickname = selfMeta.nickname,
                    gender = selfMeta.gender,
                    photoBase64 = selfMeta.photoBase64
                )
            )
        }
        val matched = localManagedUsers.value.firstOrNull {
            it.virtualId.equals(normalized, ignoreCase = true)
        }
        return Result.success(
            matched?.let {
                VirtualPlayerLookup(
                    virtualId = it.virtualId,
                    userId = it.userId,
                    displayName = it.displayName,
                    nickname = it.nickname,
                    gender = it.gender,
                    photoBase64 = it.photoBase64
                )
            }
        )
    }

    override suspend fun openOrCreateDirectChat(peer: VirtualPlayerLookup): Result<ChatThread> {
        if (peer.userId == sessionUserId) {
            return Result.failure(IllegalArgumentException("Choose another player's Virtual ID to start a 1v1 chat"))
        }
        val user1 = minOf(sessionUserId, peer.userId)
        val user2 = maxOf(sessionUserId, peer.userId)
        val chatId = "${user1}_${user2}"
        val existing = localChatThreads.value.firstOrNull { it.chatId == chatId }
        if (existing != null) return Result.success(existing)

        val created = ChatThread(
            chatId = chatId,
            participantIds = listOf(user1, user2),
            peerUserId = peer.userId,
            peerVirtualId = peer.virtualId,
            peerName = peer.displayName,
            peerPhotoBase64 = peer.photoBase64
        )
        localChatThreads.update { listOf(created) + it }
        return Result.success(created)
    }

    override suspend fun sendDirectMessage(
        chatId: String,
        peer: VirtualPlayerLookup?,
        text: String
    ): Result<Unit> {
        val cleanText = text.trim().take(1000)
        if (cleanText.isBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty"))
        }
        val selfMeta = localProfileMeta.value
        val nowMs = System.currentTimeMillis()
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = sessionUserId,
            senderVirtualId = selfMeta.virtualId,
            senderName = selfMeta.displayName,
            text = cleanText,
            timestampMillis = nowMs,
            isFromCurrentUser = true
        )
        localChatMessages.update { currentMap ->
            val updatedList = (currentMap[chatId] ?: emptyList()) + msg
            currentMap + (chatId to updatedList)
        }
        localChatThreads.update { threads ->
            val found = threads.any { it.chatId == chatId }
            if (found) {
                threads.map { thread ->
                    if (thread.chatId == chatId) {
                        thread.copy(
                            lastMessageText = cleanText,
                            lastSenderId = sessionUserId,
                            lastMessageAtMillis = nowMs
                        )
                    } else thread
                }
            } else if (peer != null) {
                val user1 = minOf(sessionUserId, peer.userId)
                val user2 = maxOf(sessionUserId, peer.userId)
                listOf(
                    ChatThread(
                        chatId = chatId,
                        participantIds = listOf(user1, user2),
                        peerUserId = peer.userId,
                        peerVirtualId = peer.virtualId,
                        peerName = peer.displayName,
                        peerPhotoBase64 = peer.photoBase64,
                        lastMessageText = cleanText,
                        lastSenderId = sessionUserId,
                        lastMessageAtMillis = nowMs
                    )
                ) + threads
            } else {
                threads
            }
        }
        return Result.success(Unit)
    }

    override suspend fun founderGenerateCurrency(
        targetUserId: String,
        coinsToAdd: Long,
        diamondsToAdd: Long,
        reason: String
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        if (targetUserId == sessionUserId) {
            if (coinsToAdd > 0L) walletDao.buyCoins(coinsToAdd, now)
            if (diamondsToAdd > 0L) walletDao.addBonusDiamonds(diamondsToAdd, now)
        } else {
            localManagedUsers.update { list ->
                list.map { user ->
                    if (user.userId == targetUserId) {
                        user.copy(
                            coins = user.coins + coinsToAdd,
                            diamonds = user.diamonds + diamondsToAdd
                        )
                    } else user
                }
            }
        }
        localPlatformSettings.update {
            it.copy(
                totalCoinsGeneratedByFounder = it.totalCoinsGeneratedByFounder + coinsToAdd,
                totalDiamondsGeneratedByFounder = it.totalDiamondsGeneratedByFounder + diamondsToAdd
            )
        }
        localAuditLogs.update { logs ->
            listOf(
                FounderAuditEntry(
                    id = UUID.randomUUID().toString(),
                    actorEmail = sessionEmail,
                    actionType = if (coinsToAdd > 0L) "MINT_COINS" else "MINT_DIAMONDS",
                    targetEmail = targetUserId,
                    summary = "Generated +$coinsToAdd Coins, +$diamondsToAdd Diamond ($reason)",
                    timestampMillis = now
                )
            ) + logs
        }
        return Result.success(Unit)
    }

    override suspend fun founderBroadcastCurrency(
        coinsToAdd: Long,
        diamondsToAdd: Long,
        reason: String
    ): Result<Int> {
        val now = System.currentTimeMillis()
        if (coinsToAdd > 0L) walletDao.buyCoins(coinsToAdd, now)
        if (diamondsToAdd > 0L) walletDao.addBonusDiamonds(diamondsToAdd, now)
        var count = 1
        localManagedUsers.update { list ->
            list.map { user ->
                if (!user.isSuspended) {
                    count++
                    user.copy(coins = user.coins + coinsToAdd, diamonds = user.diamonds + diamondsToAdd)
                } else user
            }
        }
        localPlatformSettings.update {
            it.copy(
                totalCoinsGeneratedByFounder = it.totalCoinsGeneratedByFounder + coinsToAdd * count,
                totalDiamondsGeneratedByFounder = it.totalDiamondsGeneratedByFounder + diamondsToAdd * count
            )
        }
        localAuditLogs.update { logs ->
            listOf(
                FounderAuditEntry(
                    id = UUID.randomUUID().toString(),
                    actorEmail = sessionEmail,
                    actionType = "BROADCAST_MINT",
                    targetEmail = "$count users",
                    summary = "Broadcast +$coinsToAdd Coins, +$diamondsToAdd Diamond ($reason)",
                    timestampMillis = now
                )
            ) + logs
        }
        return Result.success(count)
    }

    override suspend fun founderAdjustOrResetUser(
        targetUserId: String,
        resetToZero: Boolean,
        coinsToDeduct: Long,
        diamondsToDeduct: Long,
        reason: String
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        if (targetUserId == sessionUserId) {
            val current = walletDao.getWalletState() ?: WalletStateEntity()
            walletDao.updateWallet(
                current.copy(
                    coins = if (resetToZero) 0L else (current.coins - coinsToDeduct).coerceAtLeast(0L),
                    diamonds = if (resetToZero) 0L else (current.diamonds - diamondsToDeduct).coerceAtLeast(0L)
                )
            )
        } else {
            localManagedUsers.update { list ->
                list.map { user ->
                    if (user.userId == targetUserId) {
                        user.copy(
                            coins = if (resetToZero) 0L else (user.coins - coinsToDeduct).coerceAtLeast(0L),
                            diamonds = if (resetToZero) 0L else (user.diamonds - diamondsToDeduct).coerceAtLeast(0L)
                        )
                    } else user
                }
            }
        }
        localAuditLogs.update { logs ->
            listOf(
                FounderAuditEntry(
                    id = UUID.randomUUID().toString(),
                    actorEmail = sessionEmail,
                    actionType = "WALLET_ADJUST",
                    targetEmail = targetUserId,
                    summary = if (resetToZero) "Reset wallet to 0" else "Deducted -$coinsToDeduct Coins, -$diamondsToDeduct Diamond",
                    timestampMillis = now
                )
            ) + logs
        }
        return Result.success(Unit)
    }

    override suspend fun founderSetUserStatus(targetUserId: String, newStatus: String): Result<Unit> {
        localManagedUsers.update { list ->
            list.map { user ->
                if (user.userId == targetUserId) user.copy(accountStatus = newStatus) else user
            }
        }
        return Result.success(Unit)
    }

    override suspend fun founderUpdatePlatformSettings(
        settings: PlatformSettings,
        actionType: String,
        summary: String
    ): Result<Unit> {
        localPlatformSettings.value = settings
        localAuditLogs.update { logs ->
            listOf(
                FounderAuditEntry(
                    id = UUID.randomUUID().toString(),
                    actorEmail = sessionEmail,
                    actionType = actionType,
                    targetEmail = "Platform",
                    summary = summary,
                    timestampMillis = System.currentTimeMillis()
                )
            ) + logs
        }
        return Result.success(Unit)
    }
}

sealed class ExchangeExecutionResult {
    data class Success(
        val diamondsExchanged: Long,
        val netCoinsReceived: Long,
        val adminFeeCoins: Long
    ) : ExchangeExecutionResult()

    data class Failure(val error: com.example.domain.ValidationError?) : ExchangeExecutionResult()
}
