package com.example.data

import com.example.domain.ChatMessage
import com.example.domain.ChatThread
import com.example.domain.FounderAuditEntry
import com.example.domain.ManagedUserAccount
import com.example.domain.PlatformExchangeLedgerItem
import com.example.domain.PlatformSettings
import com.example.domain.VirtualPlayerLookup
import kotlinx.coroutines.flow.Flow

interface HousieDataRepository {
    val walletState: Flow<WalletStateEntity>
    val exchangeRecords: Flow<List<ExchangeRecordEntity>>
    val walletActivities: Flow<List<WalletActivityEntity>>
    val currentUserAccount: Flow<ManagedUserAccount?>
    val platformSettings: Flow<PlatformSettings>
    val allUsersForFounder: Flow<List<ManagedUserAccount>>
    val globalExchangeLedger: Flow<List<PlatformExchangeLedgerItem>>
    val founderAuditLogs: Flow<List<FounderAuditEntry>>
    val chatThreads: Flow<List<ChatThread>>

    fun observeChatMessages(chatId: String): Flow<List<ChatMessage>>

    suspend fun initialize()
    suspend fun executeExchange(diamonds: Long): ExchangeExecutionResult
    suspend fun buyCoins(coinsAmount: Long): Result<Unit>
    suspend fun withdrawDiamonds(diamondsAmount: Long, destination: String): Result<Boolean>
    suspend fun addBonusDiamonds(diamondsAmount: Long): Result<Unit>
    suspend fun joinGameRoom(roomName: String, entryCoins: Long): Result<Boolean>
    suspend fun awardGamePrize(roomName: String, claimName: String, prizeDiamonds: Long): Result<Unit>

    // Profile editing & Virtual ID directory
    suspend fun updateUserProfile(
        displayName: String,
        nickname: String,
        gender: String,
        dob: String,
        photoBase64: String
    ): Result<Unit>

    // 1v1 Chat Inbox
    suspend fun lookupPlayerByVirtualId(rawVirtualId: String): Result<VirtualPlayerLookup?>
    suspend fun openOrCreateDirectChat(peer: VirtualPlayerLookup): Result<ChatThread>
    suspend fun sendDirectMessage(chatId: String, peer: VirtualPlayerLookup?, text: String): Result<Unit>

    // Founder / Admin full control operations
    suspend fun founderGenerateCurrency(
        targetUserId: String,
        coinsToAdd: Long,
        diamondsToAdd: Long,
        reason: String
    ): Result<Unit>

    suspend fun founderBroadcastCurrency(
        coinsToAdd: Long,
        diamondsToAdd: Long,
        reason: String
    ): Result<Int>

    suspend fun founderAdjustOrResetUser(
        targetUserId: String,
        resetToZero: Boolean,
        coinsToDeduct: Long,
        diamondsToDeduct: Long,
        reason: String
    ): Result<Unit>

    suspend fun founderSetUserStatus(
        targetUserId: String,
        newStatus: String
    ): Result<Unit>

    suspend fun founderUpdatePlatformSettings(
        settings: PlatformSettings,
        actionType: String,
        summary: String
    ): Result<Unit>
}
