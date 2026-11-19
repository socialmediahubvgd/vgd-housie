package com.example

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.VgdDatabase
import com.example.data.WalletRepository
import com.example.domain.FounderConfig
import com.example.ui.HousieGameViewModel
import com.example.ui.VgdHousieApp
import com.example.ui.WalletViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.theme.VgdHousieTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: VgdDatabase

    @Before
    fun setUpDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VgdDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDownDb() {
        db.close()
    }

    @Test
    fun `app name matches VGD Housie`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("VGD Housie", context.getString(R.string.app_name))
    }

    @Test
    fun `unauthenticated launch shows Google Login and Sign Up portal`() {
        composeRule.setContent {
            VgdHousieTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("auth_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("auth_mode_login").assertIsDisplayed()
        composeRule.onNodeWithTag("auth_mode_signup").assertIsDisplayed()
        composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in with Google").assertIsDisplayed()

        // Switch to Sign Up mode -> shows optional Player Name input and Sign up with Google CTA
        composeRule.onNodeWithTag("auth_mode_signup").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("signup_display_name_input").assertIsDisplayed()
        composeRule.onNodeWithTag("signup_display_name_input").performTextReplacement("Lucky Vinayak")
        composeRule.onNodeWithTag("google_sign_up_button").assertIsDisplayed()
        composeRule.onNodeWithText("Sign up with Google").assertIsDisplayed()

        // Switch back via bottom link -> returns to Login mode
        composeRule.onNodeWithTag("auth_mode_switch_link").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun `regular player session hides Founder Panel completely and allows playing and exchanging`() {
        val repo = WalletRepository(
            walletDao = db.walletDao(),
            sessionUserId = "player_1",
            sessionEmail = "player@example.com",
            sessionName = "Regular Player",
            isFounderSession = false
        )
        val walletVm = WalletViewModel(repo)
        val gameVm = HousieGameViewModel(repo, playerDisplayName = "Regular Player")

        composeRule.setContent {
            VgdHousieTheme {
                VgdHousieApp(
                    viewModel = walletVm,
                    gameViewModel = gameVm,
                    isFounder = false,
                    currentUserEmail = "player@example.com"
                )
            }
        }
        composeRule.waitForIdle()

        // Admin/Founder tab MUST NOT exist for non-founder players
        composeRule.onNodeWithTag("nav_admin").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_wallet").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_exchange").assertIsDisplayed()

        // Home screen shows Wallet summary + 4 Game Rooms
        composeRule.onNodeWithTag("home_wallet_coins").assertTextContains("0")
        composeRule.onNodeWithTag("home_wallet_diamonds").assertTextContains("5,000")
        composeRule.onNodeWithTag("room_card_small").assertIsDisplayed()

        // Trying to join Small Room with 0 Coins opens Buy Coins dialog
        composeRule.onNodeWithTag("join_room_small").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("buy_coins_prompt").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("confirm_buy_coins_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("buy_coins_prompt").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("home_wallet_coins").assertTextContains("2,000")

        // Join Small Room -> opens Game Board & verifies Wrong Claim
        composeRule.onNodeWithTag("join_room_small").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("game_board_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("housie_ticket_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("claim_early_5").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wrong_claim_title").assertTextContains("Wrong Claim")
        composeRule.onNodeWithTag("dismiss_wrong_claim_button").performClick()
        composeRule.onNodeWithTag("game_back_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("home_wallet_coins").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_wallet_coins").assertTextContains("1,500")
    }

    @Test
    fun `founder email vinayakglobaldream@gmailcom unlocks full Founder Panel, Coin Generator, User Management, and Controls`() {
        val repo = WalletRepository(
            walletDao = db.walletDao(),
            sessionUserId = "founder_uid",
            sessionEmail = FounderConfig.FOUNDER_EMAIL,
            sessionName = "Vinayak Founder",
            isFounderSession = true
        )
        val walletVm = WalletViewModel(repo)
        val gameVm = HousieGameViewModel(repo, playerDisplayName = "Vinayak Founder")

        composeRule.setContent {
            VgdHousieTheme {
                VgdHousieApp(
                    viewModel = walletVm,
                    gameViewModel = gameVm,
                    isFounder = true,
                    currentUserEmail = FounderConfig.FOUNDER_EMAIL
                )
            }
        }
        composeRule.waitForIdle()

        // Exchange 1,500 Diamond -> 900 Coins (+100 Admin Fee)
        composeRule.onNodeWithTag("nav_exchange").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mere_diamond_text").assertTextContains("💎 Mere Diamond: 5,000")
        composeRule.onNodeWithTag("diamond_amount_input").performTextReplacement("1500")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("milenge_coins_text").assertTextContains("🪙 Milenge Coins: 900")
        composeRule.onNodeWithTag("exchange_lazy_column")
            .performScrollToNode(hasTestTag("exchange_now_button"))
        composeRule.onNodeWithTag("exchange_now_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_exchange_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("confetti_success_modal").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("exchange_success_done_button").performClick()

        // Founder tab IS visible exclusively for Founder
        composeRule.onNodeWithTag("nav_admin").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("founder_panel_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("founder_email_badge").assertTextContains(FounderConfig.FOUNDER_EMAIL, substring = true)
        composeRule.onNodeWithTag("admin_total_exchanges_text").assertTextContains("1")
        composeRule.onNodeWithTag("admin_profit_coins_text").assertTextContains("100", substring = true)

        // Open Coin & Diamond Generator tab and credit Coins to Founder wallet
        composeRule.onNodeWithTag("founder_tab_generator").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("coin_generator_section").assertIsDisplayed()
        composeRule.onNodeWithTag("generator_coins_input").performTextReplacement("5000")
        composeRule.onNodeWithTag("generator_credit_button").performClick()
        composeRule.waitForIdle()

        // Open User Management tab and suspend a player
        composeRule.onNodeWithTag("founder_tab_users").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("user_management_section").assertIsDisplayed()
        composeRule.onNodeWithTag("founder_panel_list")
            .performScrollToNode(hasTestTag("toggle_user_status_player_demo_1"))
        composeRule.onNodeWithTag("toggle_user_status_player_demo_1").performClick()
        composeRule.waitForIdle()

        // Open App & Room Controls tab and verify controls
        composeRule.onNodeWithTag("founder_tab_controls").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_controls_section").assertIsDisplayed()
        composeRule.onNodeWithTag("announcement_banner_input").performTextReplacement("Mega Housie Live!")
        composeRule.onNodeWithTag("founder_panel_list")
            .performScrollToNode(hasTestTag("save_platform_settings_button"))
        composeRule.onNodeWithTag("save_platform_settings_button").performClick()
        composeRule.waitForIdle()

        // Verify announcement & minted Coins reflected on Home
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("live_announcement_banner").assertTextContains("Mega Housie Live!", substring = true)
        composeRule.onNodeWithTag("home_wallet_coins").assertTextContains("5,900")
    }

    @Test
    fun `tutorial overlay modal shows demo video player, rules tab, and closes cleanly`() {
        val repo = WalletRepository(walletDao = db.walletDao(), isFounderSession = false)
        val walletVm = WalletViewModel(repo)
        val gameVm = HousieGameViewModel(repo)

        composeRule.setContent {
            VgdHousieTheme {
                VgdHousieApp(
                    viewModel = walletVm,
                    gameViewModel = gameVm,
                    isFounder = false,
                    currentUserEmail = "player@example.com"
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("how_to_play_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("housie_tutorial_modal").assertIsDisplayed()
        composeRule.onNodeWithTag("demo_video_player").assertIsDisplayed()
        composeRule.onNodeWithTag("demo_video_play_pause").assertIsDisplayed()
        composeRule.onNodeWithTag("demo_caption_title").assertIsDisplayed()

        composeRule.onNodeWithTag("tutorial_tab_rules").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tutorial_rules_content").assertIsDisplayed()

        composeRule.onNodeWithTag("tutorial_done_button").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodesWithTag("housie_tutorial_modal").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun `profile editing updates name, nickname, gender, dob and 1v1 inbox finds player by Virtual ID`() {
        val repo = WalletRepository(
            walletDao = db.walletDao(),
            sessionUserId = "player_social_test",
            sessionEmail = "social@example.com",
            sessionName = "Ananya",
            isFounderSession = false
        )
        val walletVm = WalletViewModel(repo)
        val gameVm = HousieGameViewModel(repo, playerDisplayName = "Ananya")

        composeRule.setContent {
            VgdHousieTheme {
                VgdHousieApp(
                    viewModel = walletVm,
                    gameViewModel = gameVm,
                    isFounder = false,
                    currentUserEmail = "social@example.com"
                )
            }
        }
        composeRule.waitForIdle()

        // Navigate to Profile tab & verify Virtual ID badge + editing fields
        composeRule.onNodeWithTag("nav_profile").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("profile_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("profile_virtual_id_text").assertTextContains("VGD-", substring = true)
        composeRule.onNodeWithTag("profile_name_input").performTextReplacement("Ananya Roy")
        composeRule.onNodeWithTag("profile_nickname_input").performTextReplacement("QueenHousie")
        composeRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("profile_gender_Female"))
        composeRule.onNodeWithTag("profile_gender_Female").performClick()
        composeRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("profile_dob_input"))
        composeRule.onNodeWithTag("profile_dob_input").performTextReplacement("12/05/1999")
        composeRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("save_profile_button"))
        composeRule.onNodeWithTag("save_profile_button").performClick()
        composeRule.waitForIdle()

        // Switch to Inbox tab, search peer VGD-RAHUL1, open 1v1 chat, and send a message
        composeRule.onNodeWithTag("nav_inbox").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("inbox_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("inbox_virtual_id_input").performTextReplacement("VGD-RAHUL1")
        composeRule.onNodeWithTag("inbox_search_player_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("inbox_found_player_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("inbox_found_player_card").assertIsDisplayed()
        composeRule.onNodeWithText("Rahul Sharma", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("inbox_start_chat_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("chat_conversation_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("chat_message_input").performTextReplacement("Hey Rahul, ready for Housie?")
        composeRule.onNodeWithTag("chat_send_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("chat_messages_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Hey Rahul, ready for Housie?").assertIsDisplayed()

        // Navigate back to Inbox directory and verify thread preview
        composeRule.onNodeWithTag("chat_back_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("chat_thread_VGD-RAHUL1").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("chat_thread_VGD-RAHUL1")
            .assertTextContains("Hey Rahul, ready for Housie?", substring = true)
    }
}
