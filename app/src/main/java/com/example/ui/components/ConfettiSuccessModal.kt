package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ExchangeCalculator
import com.example.ui.ExchangeSuccessBanner
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class ConfettiShape {
    RECTANGLE,
    CIRCLE,
    DIAMOND
}

private data class ConfettiParticle(
    val startXFraction: Float,
    val startYFraction: Float,
    val velocityX: Float,
    val velocityY: Float,
    val gravity: Float,
    val sizeDp: Float,
    val initialRotation: Float,
    val rotationSpeed: Float,
    val color: Color,
    val shape: ConfettiShape
)

@Composable
fun ConfettiSuccessModal(
    successBanner: ExchangeSuccessBanner?,
    onDone: () -> Unit,
    onViewWallet: () -> Unit
) {
    AnimatedVisibility(
        visible = successBanner != null,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        if (successBanner != null) {
            val modalScale = remember(successBanner.eventId) { Animatable(0.68f) }
            val badgeScale = remember(successBanner.eventId) { Animatable(0.35f) }
            var elapsedSeconds by remember(successBanner.eventId) { mutableLongStateOf(0L) }
            val animClock = remember(successBanner.eventId) { Animatable(0f) }

            val confettiParticles = remember(successBanner.eventId) {
                val rng = Random(successBanner.eventId)
                val palette = listOf(
                    CyanDiamond,
                    GoldCoin,
                    EmeraldSuccess,
                    Color(0xFFF472B6),
                    Color(0xFFA78BFA),
                    Color(0xFFFFFFFF)
                )
                List(88) { index ->
                    val angle = rng.nextFloat() * Math.PI.toFloat() * 2f
                    val speed = 260f + rng.nextFloat() * 780f
                    ConfettiParticle(
                        startXFraction = 0.5f + (rng.nextFloat() - 0.5f) * 0.25f,
                        startYFraction = 0.42f + (rng.nextFloat() - 0.5f) * 0.12f,
                        velocityX = cos(angle) * speed * (0.7f + rng.nextFloat() * 0.6f),
                        velocityY = sin(angle) * speed - 460f - rng.nextFloat() * 340f,
                        gravity = 920f + rng.nextFloat() * 380f,
                        sizeDp = 6f + rng.nextFloat() * 8f,
                        initialRotation = rng.nextFloat() * 360f,
                        rotationSpeed = (rng.nextFloat() - 0.5f) * 520f,
                        color = palette[index % palette.size],
                        shape = ConfettiShape.entries[index % ConfettiShape.entries.size]
                    )
                }
            }

            LaunchedEffect(successBanner.eventId) {
                modalScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }

            LaunchedEffect(successBanner.eventId) {
                badgeScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.45f,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            LaunchedEffect(successBanner.eventId) {
                val startNanos = withFrameNanos { it }
                while (true) {
                    val frameNanos = withFrameNanos { it }
                    val sec = (frameNanos - startNanos) / 1_000_000_000f
                    elapsedSeconds = ((frameNanos - startNanos) / 1_000_000L)
                    animClock.snapTo(sec)
                    if (sec > 4.2f) break
                }
            }

            val pulseTransition = rememberInfiniteTransition(label = "success_pulse")
            val pulseScale by pulseTransition.animateFloat(
                initialValue = 0.94f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.84f))
                    .testTag("confetti_success_modal"),
                contentAlignment = Alignment.Center
            ) {
                // Full-screen 60fps Confetti Canvas Burst
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val t = animClock.value
                    val alphaFade = (1f - (t / 4.0f)).coerceIn(0f, 1f)
                    if (alphaFade > 0.01f) {
                        confettiParticles.forEach { p ->
                            val px = size.width * p.startXFraction + p.velocityX * t
                            val py = size.height * p.startYFraction + p.velocityY * t + 0.5f * p.gravity * t * t
                            val particleSize = p.sizeDp.dp.toPx()
                            val rotation = p.initialRotation + p.rotationSpeed * t

                            if (py < size.height + particleSize && px > -particleSize && px < size.width + particleSize) {
                                rotate(degrees = rotation, pivot = Offset(px, py)) {
                                    val c = p.color.copy(alpha = alphaFade)
                                    when (p.shape) {
                                        ConfettiShape.RECTANGLE -> {
                                            drawRect(
                                                color = c,
                                                topLeft = Offset(px - particleSize / 2f, py - particleSize * 0.3f),
                                                size = Size(particleSize, particleSize * 0.6f)
                                            )
                                        }
                                        ConfettiShape.CIRCLE -> {
                                            drawCircle(
                                                color = c,
                                                radius = particleSize * 0.42f,
                                                center = Offset(px, py)
                                            )
                                        }
                                        ConfettiShape.DIAMOND -> {
                                            val half = particleSize * 0.55f
                                            val path = Path().apply {
                                                moveTo(px, py - half)
                                                lineTo(px + half * 0.75f, py)
                                                lineTo(px, py + half)
                                                lineTo(px - half * 0.75f, py)
                                                close()
                                            }
                                            drawPath(path = path, color = c)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Spring-Animated Success Card
                Surface(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .scale(modalScale.value)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(CyanDiamond.copy(alpha = 0.7f), GoldCoin.copy(alpha = 0.7f))
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = ObsidianSurface,
                    tonalElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ObsidianCardElevated.copy(alpha = 0.9f),
                                        ObsidianSurface
                                    )
                                )
                            )
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            EmeraldSuccess.copy(alpha = 0.35f),
                                            EmeraldContainer.copy(alpha = 0.1f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .scale(badgeScale.value)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = stringResource(R.string.exchange_successful_title),
                                    tint = TextDark,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = stringResource(R.string.exchange_successful_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("exchange_success_title")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Coins credited instantly to your VGD Housie wallet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ObsidianCardElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "-${ExchangeCalculator.formatNumber(successBanner.diamondsExchanged)} 💎",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = CyanDiamond,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "→",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "+${ExchangeCalculator.formatNumber(successBanner.netCoinsReceived)} 🪙",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = GoldCoin,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "10% Admin Fee: ${ExchangeCalculator.formatNumber(successBanner.adminFeeCoins)} Coins",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onViewWallet,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("exchange_success_wallet_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Wallet", color = TextPrimary)
                            }
                            Button(
                                onClick = onDone,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("exchange_success_done_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldSuccess,
                                    contentColor = TextDark
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.done_action),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
