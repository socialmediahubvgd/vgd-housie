package com.example.domain

object FounderConfig {
    const val FOUNDER_EMAIL = "vinayakglobaldream@gmail.com"
    const val ROLE_FOUNDER = "founder"
    const val ROLE_PLAYER = "player"
    const val STATUS_ACTIVE = "active"
    const val STATUS_SUSPENDED = "suspended"

    fun isFounderEmail(email: String?, isEmailVerified: Boolean): Boolean {
        if (!isEmailVerified) return false
        return email?.trim()?.equals(FOUNDER_EMAIL, ignoreCase = true) == true
    }
}

data class ManagedUserAccount(
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String,
    val accountStatus: String,
    val diamonds: Long,
    val coins: Long,
    val totalExchangesCount: Int,
    val totalDiamondsExchanged: Long,
    val totalCoinsDistributed: Long,
    val gamesPlayed: Int,
    val gamesWon: Int,
    val virtualId: String = VirtualIdGenerator.fromUid(userId),
    val nickname: String = "",
    val gender: String = "",
    val dob: String = "",
    val photoBase64: String = ""
) {
    val isFounder: Boolean get() = role == FounderConfig.ROLE_FOUNDER
    val isSuspended: Boolean get() = accountStatus == FounderConfig.STATUS_SUSPENDED
}

data class PlatformExchangeLedgerItem(
    val id: String,
    val userId: String,
    val userEmail: String,
    val userName: String,
    val diamondsExchanged: Long,
    val grossCoins: Long,
    val netCoinsReceived: Long,
    val adminFeeCoins: Long,
    val timestampMillis: Long
)

data class FounderAuditEntry(
    val id: String,
    val actorEmail: String,
    val actionType: String,
    val targetEmail: String,
    val summary: String,
    val timestampMillis: Long
)

data class PlatformSettings(
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
    val totalDiamondsGeneratedByFounder: Long = 0L
) {
    fun toRooms(): List<HousieRoom> = listOf(
        HousieRoom(
            id = "small",
            name = "Small Room",
            entryCoins = smallEntryCoins,
            prizeDiamonds = smallPrizeDiamonds,
            prizeDisplay = ExchangeCalculator.formatNumber(smallPrizeDiamonds),
            badge = "Starter"
        ),
        HousieRoom(
            id = "medium",
            name = "Medium Room",
            entryCoins = mediumEntryCoins,
            prizeDiamonds = mediumPrizeDiamonds,
            prizeDisplay = ExchangeCalculator.formatNumber(mediumPrizeDiamonds),
            badge = "Popular"
        ),
        HousieRoom(
            id = "big",
            name = "Big Room",
            entryCoins = bigEntryCoins,
            prizeDiamonds = bigPrizeDiamonds,
            prizeDisplay = ExchangeCalculator.formatNumber(bigPrizeDiamonds),
            badge = "High Stakes"
        ),
        HousieRoom(
            id = "vip",
            name = "VIP Room",
            entryCoins = vipEntryCoins,
            prizeDiamonds = vipPrizeDiamonds,
            prizeDisplay = if (vipPrizeDiamonds == 400_000L) "4,00,000" else ExchangeCalculator.formatNumber(vipPrizeDiamonds),
            badge = "Royal VIP"
        )
    )
}
