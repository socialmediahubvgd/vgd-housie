package com.example.domain

import java.text.NumberFormat
import java.util.Locale

data class ExchangeBreakdown(
    val diamonds: Long,
    val grossCoins: Long,
    val adminFeeCoins: Long,
    val netUserCoins: Long,
    val isValid: Boolean,
    val validationError: ValidationError? = null
)

enum class ValidationError {
    BELOW_MINIMUM,
    INSUFFICIENT_BALANCE
}

object ExchangeCalculator {
    const val MIN_EXCHANGE_DIAMONDS = 150L
    const val STEP_DIAMONDS = 150L
    const val INITIAL_DIAMONDS = 5_000L
    const val FEE_PERCENT = 10

    /**
     * Fixed exchange rate:
     * - 150 Diamond = 100 Coins
     * - 1,500 Diamond = 1,000 Coins
     * - 15,000 Diamond = 10,000 Coins
     *
     * Exchange Fee: 10% of gross coins goes to Admin.
     * Example: 1,500 Diamond -> 1,000 gross Coins -> 100 Admin Fee + 900 User Coins.
     * Example: 5,000 Diamond -> 3,333 gross Coins -> 333 Admin Fee + 3,000 User Coins.
     */
    fun calculate(diamonds: Long, availableDiamonds: Long): ExchangeBreakdown {
        val safeDiamonds = diamonds.coerceAtLeast(0L)
        val grossCoins = (safeDiamonds * 2L) / 3L
        val adminFeeCoins = grossCoins / 10L
        val netUserCoins = grossCoins - adminFeeCoins

        val error = when {
            safeDiamonds < MIN_EXCHANGE_DIAMONDS -> ValidationError.BELOW_MINIMUM
            safeDiamonds > availableDiamonds -> ValidationError.INSUFFICIENT_BALANCE
            else -> null
        }

        return ExchangeBreakdown(
            diamonds = safeDiamonds,
            grossCoins = grossCoins,
            adminFeeCoins = adminFeeCoins,
            netUserCoins = netUserCoins,
            isValid = error == null,
            validationError = error
        )
    }

    fun defaultSelectionForBalance(availableDiamonds: Long): Long {
        return when {
            availableDiamonds >= INITIAL_DIAMONDS -> INITIAL_DIAMONDS
            availableDiamonds >= MIN_EXCHANGE_DIAMONDS -> availableDiamonds
            else -> MIN_EXCHANGE_DIAMONDS
        }
    }

    fun formatNumber(value: Long): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }
}
