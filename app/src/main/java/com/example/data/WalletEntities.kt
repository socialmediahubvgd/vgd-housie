package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.ExchangeCalculator

@Entity(tableName = "wallet_state")
data class WalletStateEntity(
    @PrimaryKey val id: Int = 1,
    val diamonds: Long = ExchangeCalculator.INITIAL_DIAMONDS,
    val coins: Long = 0L,
    val adminFeeProfitCoins: Long = 0L,
    val totalExchangesCount: Int = 0,
    val totalDiamondsExchanged: Long = 0L,
    val totalCoinsDistributed: Long = 0L
)

@Entity(tableName = "exchange_records")
data class ExchangeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val diamondsExchanged: Long,
    val grossCoins: Long,
    val netCoinsReceived: Long,
    val adminFeeCoins: Long,
    val timestampMillis: Long
)

@Entity(tableName = "wallet_activities")
data class WalletActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String, // BUY_COINS, WITHDRAW_DIAMOND, BONUS_DIAMONDS
    val title: String,
    val subtitle: String,
    val amountDeltaText: String,
    val timestampMillis: Long
)
