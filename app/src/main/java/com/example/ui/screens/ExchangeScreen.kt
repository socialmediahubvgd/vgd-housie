package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ExchangeCalculator
import com.example.domain.ValidationError
import com.example.ui.WalletUiState
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ExchangeScreen(
    uiState: WalletUiState,
    onDiamondsTextChanged: (String) -> Unit,
    onSliderChanged: (Float) -> Unit,
    onStepDiamonds: (Long) -> Unit,
    onSelectPreset: (Long) -> Unit,
    onExchangeNowClick: () -> Unit,
    onBackToWallet: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val breakdown = uiState.breakdown
    val maxSlider = uiState.userDiamonds.coerceAtLeast(ExchangeCalculator.MIN_EXCHANGE_DIAMONDS).toFloat()
    val currentSlider = uiState.selectedDiamonds
        .coerceIn(ExchangeCalculator.MIN_EXCHANGE_DIAMONDS, maxSlider.toLong())
        .toFloat()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("exchange_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onBackToWallet != null) {
                        IconButton(
                            onClick = onBackToWallet,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.nav_wallet),
                                tint = TextPrimary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.exchange_page_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.min_exchange_badge),
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanDiamond
                        )
                    }
                }
            }

            // Hero Summary Card: "💎 Mere Diamond: 5,000" & "🪙 Milenge Coins: 3,000"
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(listOf(CyanDiamond.copy(alpha = 0.6f), GoldCoin.copy(alpha = 0.6f)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CyanContainer, ObsidianSurfaceVariant, GoldContainer.copy(alpha = 0.7f))
                                )
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💎 Mere Diamond: ${ExchangeCalculator.formatNumber(uiState.userDiamonds)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = CyanDiamond,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("mere_diamond_text")
                            )
                        }

                        HorizontalDivider(color = ObsidianOutline.copy(alpha = 0.6f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🪙 Milenge Coins: ${ExchangeCalculator.formatNumber(breakdown.netUserCoins)}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = GoldCoin,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("milenge_coins_text")
                            )
                        }

                        Text(
                            text = "After 10% Exchange Fee (${ExchangeCalculator.formatNumber(breakdown.adminFeeCoins)} Coins to Admin)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Interactive Diamond Selector (Input + Slider + Stepper + / - + Quick Presets)
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.select_diamonds_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        // Numeric Input + Stepper - / +
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = { onStepDiamonds(-ExchangeCalculator.STEP_DIAMONDS) },
                                shape = RoundedCornerShape(14.dp),
                                color = ObsidianCardElevated,
                                border = BorderStroke(1.dp, ObsidianOutline),
                                modifier = Modifier
                                    .size(54.dp)
                                    .testTag("step_minus_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Remove,
                                        contentDescription = "Decrease Diamonds by 150",
                                        tint = CyanDiamond
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = uiState.diamondsInputText,
                                onValueChange = onDiamondsTextChanged,
                                label = { Text(stringResource(R.string.enter_diamonds_label)) },
                                suffix = { Text("💎", color = CyanDiamond) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = !breakdown.isValid,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanDiamond,
                                    unfocusedBorderColor = ObsidianOutline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = CyanDiamond
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("diamond_amount_input")
                            )

                            Surface(
                                onClick = { onStepDiamonds(ExchangeCalculator.STEP_DIAMONDS) },
                                shape = RoundedCornerShape(14.dp),
                                color = ObsidianCardElevated,
                                border = BorderStroke(1.dp, ObsidianOutline),
                                modifier = Modifier
                                    .size(54.dp)
                                    .testTag("step_plus_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Increase Diamonds by 150",
                                        tint = CyanDiamond
                                    )
                                }
                            }
                        }

                        if (!breakdown.isValid && breakdown.validationError != null) {
                            val errMsg = when (breakdown.validationError) {
                                ValidationError.BELOW_MINIMUM -> stringResource(R.string.err_min_diamond)
                                ValidationError.INSUFFICIENT_BALANCE -> stringResource(R.string.err_insufficient_diamonds)
                            }
                            Text(
                                text = errMsg,
                                style = MaterialTheme.typography.labelMedium,
                                color = CoralError,
                                modifier = Modifier.testTag("exchange_validation_error")
                            )
                        }

                        // Slider
                        Column {
                            Slider(
                                value = currentSlider,
                                onValueChange = onSliderChanged,
                                valueRange = ExchangeCalculator.MIN_EXCHANGE_DIAMONDS.toFloat()..maxSlider,
                                enabled = uiState.userDiamonds >= ExchangeCalculator.MIN_EXCHANGE_DIAMONDS,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanDiamond,
                                    activeTrackColor = CyanDiamond,
                                    inactiveTrackColor = ObsidianCardElevated
                                ),
                                modifier = Modifier.testTag("diamond_slider")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Min: 150 💎",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Max: ${ExchangeCalculator.formatNumber(uiState.userDiamonds)} 💎",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Quick Preset Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presets = buildList {
                                add(150L to "150")
                                add(1_500L to "1,500")
                                if (uiState.userDiamonds >= 15_000L) {
                                    add(15_000L to "15,000")
                                } else {
                                    add(5_000L to "5,000")
                                }
                                if (uiState.userDiamonds >= ExchangeCalculator.MIN_EXCHANGE_DIAMONDS) {
                                    add(uiState.userDiamonds to "Max")
                                }
                            }
                            presets.forEach { (presetAmount, presetLabel) ->
                                val selected = uiState.selectedDiamonds == presetAmount
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectPreset(presetAmount) },
                                    label = {
                                        Text(
                                            text = presetLabel,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanContainer,
                                        selectedLabelColor = CyanDiamond,
                                        containerColor = ObsidianCardElevated,
                                        labelColor = TextSecondary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Exchange Now CTA Button (right below selector for immediate visibility)
            item {
                val exchangePaused = !uiState.platformSettings.exchangeEnabled &&
                    uiState.currentUserAccount?.isFounder != true
                if (exchangePaused) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CoralError.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Diamond Exchange is temporarily paused by the Founder. Check back soon.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoralError,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Button(
                    onClick = onExchangeNowClick,
                    enabled = breakdown.isValid && !exchangePaused && !uiState.isSuspended,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanDiamond,
                        contentColor = TextDark,
                        disabledContainerColor = ObsidianCardElevated,
                        disabledContentColor = TextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("exchange_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CurrencyExchange,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = stringResource(R.string.btn_exchange_now),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Live Breakdown Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.live_breakdown_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        BreakdownRow(
                            label = stringResource(R.string.gross_coins_label),
                            value = "${ExchangeCalculator.formatNumber(breakdown.grossCoins)} Coins",
                            valueColor = TextPrimary
                        )
                        BreakdownRow(
                            label = stringResource(R.string.exchange_fee_label),
                            value = "-${ExchangeCalculator.formatNumber(breakdown.adminFeeCoins)} Coins",
                            valueColor = CoralError
                        )
                        HorizontalDivider(color = ObsidianOutline)
                        BreakdownRow(
                            label = stringResource(R.string.net_coins_label),
                            value = "${ExchangeCalculator.formatNumber(breakdown.netUserCoins)} Coins",
                            valueColor = EmeraldSuccess,
                            bold = true
                        )
                    }
                }
            }

            // Fixed Exchange Rate Table
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = GoldCoin,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.fixed_rates_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = GoldCoin
                            )
                        }
                        RateTierRow("💎 150 Diamond", "🪙 100 Coins (90 Net + 10 Fee)")
                        RateTierRow("💎 1,500 Diamond", "🪙 1,000 Coins (900 Net + 100 Fee)")
                        RateTierRow("💎 15,000 Diamond", "🪙 10,000 Coins (9,000 Net + 1,000 Fee)")
                        Text(
                            text = stringResource(R.string.fee_rule_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    valueColor: Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (bold) TextPrimary else TextSecondary
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun RateTierRow(left: String, right: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianCardElevated, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = left,
            style = MaterialTheme.typography.labelLarge,
            color = CyanDiamond
        )
        Text(
            text = right,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary
        )
    }
}
