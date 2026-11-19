package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanDiamond
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinBright
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HousieTutorialModal(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return

    // 0 = Demo Video, 1 = Complete Rules & Regulations
    var selectedTab by remember { mutableIntStateOf(0) }
    val modalScale = remember { Animatable(0.92f) }

    LaunchedEffect(Unit) {
        modalScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg.copy(alpha = 0.78f))
                .testTag("housie_tutorial_modal"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = ObsidianSurface,
                tonalElevation = 12.dp,
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .scale(modalScale.value)
                    .border(
                        width = 1.4.dp,
                        brush = Brush.linearGradient(
                            listOf(GoldCoin.copy(alpha = 0.8f), CyanDiamond.copy(alpha = 0.55f))
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(ObsidianCardElevated.copy(alpha = 0.85f), ObsidianSurface)
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GoldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Rounded.PlayCircle else Icons.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = GoldCoin,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "How to Play VGD Housie",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = GoldCoinBright
                                )
                                Text(
                                    text = "Live Demo Video & Official Rules",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("tutorial_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close tutorial",
                                tint = TextSecondary
                            )
                        }
                    }

                    // Unified 2-Tab Switcher: [Demo Video] | [Rules & Guide]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ObsidianBg)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GuideTabButton(
                            label = "▶ Demo Video",
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            testTag = "tutorial_tab_demo",
                            modifier = Modifier.weight(1f)
                        )
                        GuideTabButton(
                            label = "Rules & Claims",
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            testTag = "tutorial_tab_rules",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (selectedTab == 0) {
                            HousieDemoVideoPlayer()
                        } else {
                            UnifiedRulesAndRegulations()
                        }
                    }

                    // Footer Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (selectedTab == 0) {
                            Button(
                                onClick = { selectedTab = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ObsidianCardElevated,
                                    contentColor = CyanDiamond
                                ),
                                border = BorderStroke(1.dp, CyanDiamond.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("tutorial_read_rules_button")
                            ) {
                                Text("Read Rules", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldCoin,
                                contentColor = TextDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                                .testTag("tutorial_done_button")
                        ) {
                            Text(
                                text = "Got It, Let's Play!",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) GoldCoin else Color.Transparent,
        modifier = modifier
            .height(44.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) TextDark else TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UnifiedRulesAndRegulations() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.testTag("tutorial_rules_content")
    ) {
        Text(
            text = "Game Khelne Ka Sahi Tarika (Step-by-Step)",
            style = MaterialTheme.typography.titleMedium,
            color = GoldCoinBright
        )

        GuideRuleCard(
            badge = "1",
            badgeColor = GoldCoin,
            title = "Coins Se Room Join Karein",
            description = "Home screen par Small (500), Medium (2,000), Big (10,000) ya VIP (50,000) Room chunein. Coins kam hon toh 'Buy Coins' se add karein."
        )
        GuideRuleCard(
            badge = "2",
            badgeColor = CyanDiamond,
            title = "3 × 9 Housie Ticket & 4s Caller",
            description = "Har ticket me 3 rows aur 9 columns hote hain (kul 15 numbers, har row me 5). Har 4 second me 1–90 me se ek number aata hai aur ticket pe Green auto-cut ho jata hai."
        )
        GuideRuleCard(
            badge = "3",
            badgeColor = EmeraldSuccess,
            title = "5 Win Claims & Diamond Prizes",
            description = "• Early 5: Koi bhi 5 numbers cut (10% bonus)\n• First / Second / Third Line: Poori row ke 5 numbers cut (20% each)\n• Full House: Sabhi 15 numbers cut = Poora Room Prize + Game Over!"
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Rules & Regulations",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CoralContainer,
            border = BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = CoralError,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Wrong Claim Rule: Pattern poora Green hone se pehle claim button dabane par 'Wrong Claim' milega. Har pattern ek match me sirf ek baar claim ho sakta hai.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }

        GuideRuleCard(
            badge = "⇄",
            badgeColor = CyanDiamond,
            title = "Diamond → Coins Exchange Rule",
            description = "Jeete hue Diamonds ko Wallet → Exchange page pe Coins me badal sakte hain (150 💎 = 100 🪙, min 150 💎, 10% Admin Exchange Fee)."
        )
    }
}

@Composable
private fun GuideRuleCard(
    badge: String,
    badgeColor: Color,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianBg,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelLarge,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
