package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinBright
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private data class DemoFrame(
    val calledNumber: Int?,
    val cutCount: Int,
    val badgeText: String,
    val isWarning: Boolean,
    val captionTitle: String,
    val captionBody: String
)

private val demoTicketGrid: List<List<Int?>> = listOf(
    listOf(4, null, 23, null, 41, 56, null, 72, null),
    listOf(null, 15, null, 34, 48, null, 63, null, 85),
    listOf(9, 19, null, 38, null, null, 69, null, 90)
)

// Ordered sequence of the 15 ticket numbers as called in the demo video
private val demoCallSequence = listOf(4, 23, 41, 15, 56, 72, 34, 48, 63, 85, 9, 19, 38, 69, 90)

private val demoTimeline: List<DemoFrame> = listOf(
    DemoFrame(
        calledNumber = null,
        cutCount = 0,
        badgeText = "Step 1 • Join Room (500 🪙)",
        isWarning = false,
        captionTitle = "Room Join & 3×9 Ticket",
        captionBody = "Coins se Room join karte hi aapko 3 Row × 9 Column ka 15-number Housie ticket milta hai."
    ),
    DemoFrame(
        calledNumber = 4,
        cutCount = 1,
        badgeText = "Caller: 4 • Auto-Cut!",
        isWarning = false,
        captionTitle = "Har 4 Second me Number Caller",
        captionBody = "Upar Caller har 4 sec me 1–90 me se number bolta hai. Match hone par ticket pe Green cut ho jata hai!"
    ),
    DemoFrame(
        calledNumber = 23,
        cutCount = 2,
        badgeText = "Caller: 23 • 2 Cut",
        isWarning = false,
        captionTitle = "Numbers Auto-Cut Ho Rahe Hain",
        captionBody = "Aapko manually tap nahi karna — aaya hua number apne aap Green aur strikethrough ho jata hai."
    ),
    DemoFrame(
        calledNumber = 41,
        cutCount = 3,
        badgeText = "⚠️ Premature Tap = Wrong Claim",
        isWarning = true,
        captionTitle = "Wrong Claim Regulation",
        captionBody = "Agar 5 numbers cut hone se pehle 'Early 5' dabaya toh 'Wrong Claim' aayega. Pattern poora hone dein!"
    ),
    DemoFrame(
        calledNumber = 56,
        cutCount = 5,
        badgeText = "Early 5 Claimed! +500 💎",
        isWarning = false,
        captionTitle = "Early 5 Win Claim",
        captionBody = "Ticket par koi bhi 5 numbers Green hote hi 'Early 5' claim karein aur turant Diamond jeetein!"
    ),
    DemoFrame(
        calledNumber = 72,
        cutCount = 6,
        badgeText = "First Line Claimed! +1,000 💎",
        isWarning = false,
        captionTitle = "First / Second / Third Line",
        captionBody = "Row 1 ke sabhi 5 numbers (4, 23, 41, 56, 72) cut ho gaye — ab 'First Line' claim karke Diamond paayein."
    ),
    DemoFrame(
        calledNumber = 85,
        cutCount = 10,
        badgeText = "Second Line Claimed! +1,000 💎",
        isWarning = false,
        captionTitle = "Second Line Complete",
        captionBody = "Isi tarah Row 2 poori hone par 'Second Line' aur Row 3 poori hone par 'Third Line' claim kar sakte hain."
    ),
    DemoFrame(
        calledNumber = 90,
        cutCount = 15,
        badgeText = "FULL HOUSE! +5,000 💎 🏆",
        isWarning = false,
        captionTitle = "Full House = Grand Prize & Game Over",
        captionBody = "Sabhi 15 numbers Green hote hi 'Full House' claim karein! Poora Diamond Prize Wallet me add hoga."
    )
)

@Composable
fun HousieDemoVideoPlayer(
    modifier: Modifier = Modifier
) {
    var frameIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(2_200L)
            frameIndex = if (frameIndex < demoTimeline.lastIndex) frameIndex + 1 else 0
        }
    }

    val currentFrame = demoTimeline[frameIndex]
    val cutSet = remember(currentFrame.cutCount) {
        demoCallSequence.take(currentFrame.cutCount).toSet()
    }
    val progress = frameIndex.toFloat() / demoTimeline.lastIndex.coerceAtLeast(1)
    val currentSec = frameIndex * 3
    val totalSec = demoTimeline.lastIndex * 3

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cinema Video Frame
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ObsidianBg,
            border = BorderStroke(1.2.dp, GoldCoin.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("demo_video_player")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(ObsidianSurfaceVariant, ObsidianBg)
                        )
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Video Overlay Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = GoldContainer
                    ) {
                        Text(
                            text = if (isPlaying) "● LIVE DEMO" else "❚❚ PAUSED",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldCoinBright,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Animated Mini Caller Ball
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Caller:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GoldCoin),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentFrame.calledNumber?.toString() ?: "--",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = String.format("00:%02d / 00:%02d", currentSec, totalSec),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                // Mini 3x9 Housie Ticket Simulation
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    demoTicketGrid.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            row.forEach { num ->
                                val isCut = num != null && num in cutSet
                                val cellColor by animateColorAsState(
                                    targetValue = when {
                                        num == null -> ObsidianSurface.copy(alpha = 0.65f)
                                        isCut -> EmeraldContainer
                                        else -> ObsidianCardElevated
                                    },
                                    animationSpec = tween(280),
                                    label = "demo_cell_color"
                                )
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = cellColor,
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            num == null -> ObsidianOutline.copy(alpha = 0.25f)
                                            isCut -> EmeraldSuccess
                                            else -> ObsidianOutline
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (num != null) {
                                            Text(
                                                text = num.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCut) EmeraldSuccess else TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = if (isCut) TextDecoration.LineThrough else TextDecoration.None
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Milestone / Action Overlay Banner inside Video
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (currentFrame.isWarning) CoralContainer else EmeraldContainer.copy(alpha = 0.85f),
                    border = BorderStroke(
                        1.dp,
                        if (currentFrame.isWarning) CoralError else EmeraldSuccess
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentFrame.badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (currentFrame.isWarning) CoralError else EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Video Transport Controls (Play/Pause + Scrubber + Replay)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ObsidianBg,
            border = BorderStroke(1.dp, ObsidianOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("demo_video_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause demo" else "Play demo",
                        tint = GoldCoin
                    )
                }

                Slider(
                    value = progress,
                    onValueChange = { value ->
                        isPlaying = false
                        frameIndex = (value * demoTimeline.lastIndex).toInt().coerceIn(0, demoTimeline.lastIndex)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = GoldCoin,
                        activeTrackColor = GoldCoin,
                        inactiveTrackColor = ObsidianCardElevated
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        frameIndex = 0
                        isPlaying = true
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("demo_video_replay")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay,
                        contentDescription = "Replay demo",
                        tint = CyanDiamond
                    )
                }
            }
        }

        // Scene Quick-Jump Chapters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val chapters = listOf(0 to "Ticket", 1 to "Auto-Cut", 4 to "Claims", 7 to "Full House")
            chapters.forEach { (targetIdx, label) ->
                val active = frameIndex == targetIdx
                FilterChip(
                    selected = active,
                    onClick = {
                        isPlaying = false
                        frameIndex = targetIdx
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldContainer,
                        selectedLabelColor = GoldCoinBright,
                        containerColor = ObsidianCardElevated,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Synchronized Narration / Subtitle Box
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CyanContainer.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, CyanDiamond.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currentFrame.captionTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanDiamond,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("demo_caption_title")
                )
                Text(
                    text = currentFrame.captionBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }
    }
}
