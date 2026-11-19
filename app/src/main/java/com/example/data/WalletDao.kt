package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.domain.ExchangeCalculator
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet_state WHERE id = 1")
    fun observeWalletState(): Flow<WalletStateEntity?>

    @Query("SELECT * FROM wallet_state WHERE id = 1")
    suspend fun getWalletState(): WalletStateEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialWallet(wallet: WalletStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWallet(wallet: WalletStateEntity)

    @Query("SELECT * FROM exchange_records ORDER BY timestampMillis DESC, id DESC")
    fun observeExchangeRecords(): Flow<List<ExchangeRecordEntity>>

    @Insert
    suspend fun insertExchangeRecord(record: ExchangeRecordEntity)

    @Query("SELECT * FROM wallet_activities ORDER BY timestampMillis DESC, id DESC")
    fun observeWalletActivities(): Flow<List<WalletActivityEntity>>

    @Insert
    suspend fun insertWalletActivity(activity: WalletActivityEntity)

    @Transaction
    suspend fun ensureSeeded() {
        if (getWalletState() == null) {
            insertInitialWallet(WalletStateEntity())
        }
    }

    @Transaction
    suspend fun performExchange(
        diamonds: Long,
        grossCoins: Long,
        netUserCoins: Long,
        adminFeeCoins: Long,
        timestampMillis: Long
    ): Boolean {
        val current = getWalletState() ?: WalletStateEntity()
        if (diamonds < ExchangeCalculator.MIN_EXCHANGE_DIAMONDS || diamonds > current.diamonds) {
            return false
        }

        val updatedWallet = current.copy(
            diamonds = current.diamonds - diamonds,
            coins = current.coins + netUserCoins,
            adminFeeProfitCoins = current.adminFeeProfitCoins + adminFeeCoins,
            totalExchangesCount = current.totalExchangesCount + 1,
            totalDiamondsExchanged = current.totalDiamondsExchanged + diamonds,
            totalCoinsDistributed = current.totalCoinsDistributed + netUserCoins
        )
        updateWallet(updatedWallet)

        insertExchangeRecord(
            ExchangeRecordEntity(
                diamondsExchanged = diamonds,
                grossCoins = grossCoins,
                netCoinsReceived = netUserCoins,
                adminFeeCoins = adminFeeCoins,
                timestampMillis = timestampMillis
            )
        )
        return true
    }

    @Transaction
    suspend fun buyCoins(coinsAmount: Long, timestampMillis: Long) {
        if (coinsAmount <= 0L) return
        val current = getWalletState() ?: WalletStateEntity()
        updateWallet(current.copy(coins = current.coins + coinsAmount))
        insertWalletActivity(
            WalletActivityEntity(
                type = "BUY_COINS",
                title = "Purchased ${ExchangeCalculator.formatNumber(coinsAmount)} Coins",
                subtitle = "Added to VGD Housie Wallet",
                amountDeltaText = "+${ExchangeCalculator.formatNumber(coinsAmount)} Coins",
                timestampMillis = timestampMillis
            )
        )
    }

    @Transaction
    suspend fun withdrawDiamonds(
        diamondsAmount: Long,
        destination: String,
        timestampMillis: Long
    ): Boolean {
        val current = getWalletState() ?: WalletStateEntity()
        if (diamondsAmount <= 0L || diamondsAmount > current.diamonds) {
            return false
        }
        updateWallet(current.copy(diamonds = current.diamonds - diamondsAmount))
        insertWalletActivity(
            WalletActivityEntity(
                type = "WITHDRAW_DIAMOND",
                title = "Withdrew ${ExchangeCalculator.formatNumber(diamondsAmount)} Diamond",
                subtitle = "To $destination",
                amountDeltaText = "-${ExchangeCalculator.formatNumber(diamondsAmount)} Diamond",
                timestampMillis = timestampMillis
            )
        )
        return true
    }

    @Transaction
    suspend fun addBonusDiamonds(diamondsAmount: Long, timestampMillis: Long) {
        if (diamondsAmount <= 0L) return
        val current = getWalletState() ?: WalletStateEntity()
        updateWallet(current.copy(diamonds = current.diamonds + diamondsAmount))
        insertWalletActivity(
            WalletActivityEntity(
                type = "BONUS_DIAMONDS",
                title = "Admin Credited ${ExchangeCalculator.formatNumber(diamondsAmount)} Diamond",
                subtitle = "Promotional / Test Credit",
                amountDeltaText = "+${ExchangeCalculator.formatNumber(diamondsAmount)} Diamond",
                timestampMillis = timestampMillis
            )
        )
    }

    @Transaction
    suspend fun joinGameRoom(
        roomName: String,
        entryCoins: Long,
        timestampMillis: Long
    ): Boolean {
        val current = getWalletState() ?: WalletStateEntity()
        if (entryCoins <= 0L || current.coins < entryCoins) {
            return false
        }
        updateWallet(current.copy(coins = current.coins - entryCoins))
        insertWalletActivity(
            WalletActivityEntity(
                type = "ROOM_ENTRY",
                title = "Joined $roomName",
                subtitle = "Housie Entry Fee",
                amountDeltaText = "-${ExchangeCalculator.formatNumber(entryCoins)} Coins",
                timestampMillis = timestampMillis
            )
        )
        return true
    }

    @Transaction
    suspend fun awardGamePrize(
        roomName: String,
        claimName: String,
        prizeDiamonds: Long,
        timestampMillis: Long
    ) {
        if (prizeDiamonds <= 0L) return
        val current = getWalletState() ?: WalletStateEntity()
        updateWallet(current.copy(diamonds = current.diamonds + prizeDiamonds))
        insertWalletActivity(
            WalletActivityEntity(
                type = "GAME_WIN",
                title = "Won $claimName in $roomName",
                subtitle = "Housie Prize Credited",
                amountDeltaText = "+${ExchangeCalculator.formatNumber(prizeDiamonds)} Diamond",
                timestampMillis = timestampMillis
            )
        )
    }
}
