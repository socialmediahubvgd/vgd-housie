package com.example.data.firestore

import androidx.annotation.Keep
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserAccountDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val virtualId: String = "",
    val email: String = "",
    val displayName: String = "Player",
    val nickname: String = "",
    val gender: String = "",
    val dob: String = "",
    val photoBase64: String = "",
    val role: String = FounderConfig.ROLE_PLAYER,
    val accountStatus: String = FounderConfig.STATUS_ACTIVE,
    val diamonds: Long = ExchangeCalculator.INITIAL_DIAMONDS,
    val coins: Long = 0L,
    val totalExchangesCount: Long = 0L,
    val totalDiamondsExchanged: Long = 0L,
    val totalCoinsDistributed: Long = 0L,
    val gamesPlayed: Long = 0L,
    val gamesWon: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): ManagedUserAccount {
        val resolvedUid = userId.ifBlank { id }
        val resolvedVirtualId = virtualId.ifBlank { VirtualIdGenerator.fromUid(resolvedUid) }
        return ManagedUserAccount(
            userId = resolvedUid,
            email = email,
            displayName = displayName.ifBlank { email.substringBefore("@").ifBlank { "Player" } },
            role = role,
            accountStatus = accountStatus,
            diamonds = diamonds,
            coins = coins,
            totalExchangesCount = totalExchangesCount.toInt(),
            totalDiamondsExchanged = totalDiamondsExchanged,
            totalCoinsDistributed = totalCoinsDistributed,
            gamesPlayed = gamesPlayed.toInt(),
            gamesWon = gamesWon.toInt(),
            virtualId = resolvedVirtualId,
            nickname = nickname,
            gender = gender,
            dob = dob,
            photoBase64 = photoBase64
        )
    }
}

@Keep
@IgnoreExtraProperties
data class UserExchangeDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val diamondsExchanged: Long = 0L,
    val grossCoins: Long = 0L,
    val netCoinsReceived: Long = 0L,
    val adminFeeCoins: Long = 0L,
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserActivityDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "",
    val title: String = "",
    val subtitle: String = "",
    val amountDeltaText: String = "",
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlatformExchangeLedgerDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val diamondsExchanged: Long = 0L,
    val grossCoins: Long = 0L,
    val netCoinsReceived: Long = 0L,
    val adminFeeCoins: Long = 0L,
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toDomain(): PlatformExchangeLedgerItem = PlatformExchangeLedgerItem(
        id = id,
        userId = userId,
        userEmail = userEmail,
        userName = userName,
        diamondsExchanged = diamondsExchanged,
        grossCoins = grossCoins,
        netCoinsReceived = netCoinsReceived,
        adminFeeCoins = adminFeeCoins,
        timestampMillis = timestampMillis
    )
}

@Keep
@IgnoreExtraProperties
data class PlatformConfigDoc(
    @DocumentId val id: String = "",
    val configId: String = "platform",
    val exchangeEnabled: Boolean = true,
    val maintenanceMode: Boolean = false,
    val announcementBanner: String = "",
    val smallEntryCoins: Long = 500L,
    val smallPrizeDiamonds: Long = 5_000L,
    val mediumEntryCoins: Long = 2_000L,
    val mediumPrizeDiamonds: Long = 15_000L,
    val bigEntryCoins: Long = 10_000L,
    val bigPrizeDiamonds: Long = 80_000L,
    val vipEntryCoins: Long = 50_000L,
    val vipPrizeDiamonds: Long = 400_000L,
    val totalCoinsGeneratedByFounder: Long = 0L,
    val totalDiamondsGeneratedByFounder: Long = 0L,
    val updatedByEmail: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): PlatformSettings = PlatformSettings(
        exchangeEnabled = exchangeEnabled,
        maintenanceMode = maintenanceMode,
        announcementBanner = announcementBanner,
        smallEntryCoins = smallEntryCoins,
        smallPrizeDiamonds = smallPrizeDiamonds,
        mediumEntryCoins = mediumEntryCoins,
        mediumPrizeDiamonds = mediumPrizeDiamonds,
        bigEntryCoins = bigEntryCoins,
        bigPrizeDiamonds = bigPrizeDiamonds,
        vipEntryCoins = vipEntryCoins,
        vipPrizeDiamonds = vipPrizeDiamonds,
        totalCoinsGeneratedByFounder = totalCoinsGeneratedByFounder,
        totalDiamondsGeneratedByFounder = totalDiamondsGeneratedByFounder
    )
}

@Keep
@IgnoreExtraProperties
data class FounderAuditLogDoc(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val actionType: String = "",
    val targetUserId: String = "",
    val targetEmail: String = "",
    val summary: String = "",
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toDomain(): FounderAuditEntry = FounderAuditEntry(
        id = id,
        actorEmail = actorEmail,
        actionType = actionType,
        targetEmail = targetEmail,
        summary = summary,
        timestampMillis = timestampMillis
    )
}

@Keep
@IgnoreExtraProperties
data class VirtualIdDirectoryDoc(
    @DocumentId val id: String = "",
    val virtualId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val nickname: String = "",
    val gender: String = "",
    val photoBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): VirtualPlayerLookup = VirtualPlayerLookup(
        virtualId = virtualId.ifBlank { id },
        userId = userId,
        displayName = displayName.ifBlank { "Player" },
        nickname = nickname,
        gender = gender,
        photoBase64 = photoBase64
    )
}

@Keep
@IgnoreExtraProperties
data class ChatThreadDoc(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val user1Id: String = "",
    val user1VirtualId: String = "",
    val user1Name: String = "",
    val user1Photo: String = "",
    val user2Id: String = "",
    val user2VirtualId: String = "",
    val user2Name: String = "",
    val user2Photo: String = "",
    val lastMessageText: String = "",
    val lastSenderId: String = "",
    val lastMessageAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(currentUid: String): ChatThread {
        val currentIsUser1 = currentUid == user1Id
        return ChatThread(
            chatId = id,
            participantIds = participantIds,
            peerUserId = if (currentIsUser1) user2Id else user1Id,
            peerVirtualId = if (currentIsUser1) user2VirtualId else user1VirtualId,
            peerName = if (currentIsUser1) user2Name else user1Name,
            peerPhotoBase64 = if (currentIsUser1) user2Photo else user1Photo,
            lastMessageText = lastMessageText,
            lastSenderId = lastSenderId,
            lastMessageAtMillis = lastMessageAtMillis
        )
    }
}

@Keep
@IgnoreExtraProperties
data class ChatMessageDoc(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderVirtualId: String = "",
    val senderName: String = "",
    val text: String = "",
    val participantIds: List<String> = emptyList(),
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toDomain(currentUid: String): ChatMessage = ChatMessage(
        id = id,
        chatId = chatId,
        senderId = senderId,
        senderVirtualId = senderVirtualId,
        senderName = senderName,
        text = text,
        timestampMillis = timestampMillis,
        isFromCurrentUser = senderId == currentUid
    )
}
