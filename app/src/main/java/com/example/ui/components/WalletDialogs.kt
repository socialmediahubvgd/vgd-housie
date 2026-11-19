package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ExchangeBreakdown
import com.example.domain.ExchangeCalculator
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ConfirmExchangeDialog(
    breakdown: ExchangeBreakdown,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val diamondText = ExchangeCalculator.formatNumber(breakdown.diamonds)
    val coinsText = ExchangeCalculator.formatNumber(breakdown.netUserCoins)
    val feeText = ExchangeCalculator.formatNumber(breakdown.adminFeeCoins)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = stringResource(R.string.confirm_exchange_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "$diamondText Diamond -> $coinsText Coins, Confirm?",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.testTag("confirm_exchange_message")
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianCardElevated,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Gross Value: ${ExchangeCalculator.formatNumber(breakdown.grossCoins)} Coins",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "10% Exchange Fee (Admin): $feeText Coins",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldCoin
                        )
                        Text(
                            text = "You Receive: $coinsText Coins",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanDiamond
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanDiamond,
                    contentColor = TextDark
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("confirm_exchange_button")
            ) {
                Text(
                    text = stringResource(R.string.confirm_action),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("cancel_exchange_button")
            ) {
                Text(
                    text = stringResource(R.string.cancel_action),
                    color = TextSecondary
                )
            }
        }
    )
}

@Composable
fun BuyCoinsDialog(
    customPrompt: String? = null,
    onSelectBundle: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val bundles = listOf(500L, 2_000L, 10_000L, 50_000L)
    var selectedBundle by remember { mutableLongStateOf(2_000L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = stringResource(R.string.buy_coins_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = customPrompt ?: stringResource(R.string.buy_coins_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (customPrompt != null) GoldCoin else TextSecondary,
                    modifier = Modifier.testTag("buy_coins_prompt")
                )
                Spacer(modifier = Modifier.height(4.dp))
                bundles.chunked(2).forEach { rowBundles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowBundles.forEach { amount ->
                            val isSelected = selectedBundle == amount
                            Surface(
                                onClick = { selectedBundle = amount },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) GoldContainer else ObsidianCardElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) GoldCoin else ObsidianOutline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "🪙 ${ExchangeCalculator.formatNumber(amount)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isSelected) GoldCoin else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectBundle(selectedBundle) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldCoin,
                    contentColor = TextDark
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("confirm_buy_coins_button")
            ) {
                Text("Add ${ExchangeCalculator.formatNumber(selectedBundle)} Coins", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.cancel_action), color = TextSecondary)
            }
        }
    )
}

@Composable
fun WithdrawDiamondDialog(
    availableDiamonds: Long,
    onWithdraw: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("500") }
    var destinationText by remember { mutableStateOf("") }
    val amount = amountText.toLongOrNull() ?: 0L
    val isValid = amount in 1..availableDiamonds && destinationText.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = stringResource(R.string.withdraw_diamond_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Available: 💎 ${ExchangeCalculator.formatNumber(availableDiamonds)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyanDiamond
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() }.take(9) },
                    label = { Text(stringResource(R.string.withdraw_amount_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amount > availableDiamonds,
                    supportingText = if (amount > availableDiamonds) {
                        { Text(stringResource(R.string.err_insufficient_diamonds)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = destinationText,
                    onValueChange = { destinationText = it.take(40) },
                    label = { Text(stringResource(R.string.withdraw_destination_label)) },
                    placeholder = { Text("player@upi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onWithdraw(amount, destinationText) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanDiamond,
                    contentColor = TextDark
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("confirm_withdraw_diamond_button")
            ) {
                Text(stringResource(R.string.withdraw_action), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.cancel_action), color = TextSecondary)
            }
        }
    )
}
