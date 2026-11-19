package com.example

import com.example.domain.ExchangeCalculator
import com.example.domain.HousieClaimType
import com.example.domain.HousieEngine
import com.example.domain.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExampleUnitTest {

    @Test
    fun `150 diamonds yields 100 gross, 10 admin fee, and 90 net coins`() {
        val result = ExchangeCalculator.calculate(150L, availableDiamonds = 5_000L)
        assertTrue(result.isValid)
        assertNull(result.validationError)
        assertEquals(100L, result.grossCoins)
        assertEquals(10L, result.adminFeeCoins)
        assertEquals(90L, result.netUserCoins)
    }

    @Test
    fun `1500 diamonds yields 1000 gross, 100 admin fee, and 900 net coins`() {
        val result = ExchangeCalculator.calculate(1_500L, availableDiamonds = 5_000L)
        assertTrue(result.isValid)
        assertEquals(1_000L, result.grossCoins)
        assertEquals(100L, result.adminFeeCoins)
        assertEquals(900L, result.netUserCoins)
    }

    @Test
    fun `5000 diamonds yields 3000 net coins and 333 admin fee`() {
        val result = ExchangeCalculator.calculate(5_000L, availableDiamonds = 5_000L)
        assertTrue(result.isValid)
        assertEquals(3_333L, result.grossCoins)
        assertEquals(333L, result.adminFeeCoins)
        assertEquals(3_000L, result.netUserCoins)
    }

    @Test
    fun `15000 diamonds yields 10000 gross, 1000 admin fee, and 9000 net coins`() {
        val result = ExchangeCalculator.calculate(15_000L, availableDiamonds = 20_000L)
        assertTrue(result.isValid)
        assertEquals(10_000L, result.grossCoins)
        assertEquals(1_000L, result.adminFeeCoins)
        assertEquals(9_000L, result.netUserCoins)
    }

    @Test
    fun `below minimum 150 diamonds is invalid`() {
        val result = ExchangeCalculator.calculate(149L, availableDiamonds = 5_000L)
        assertFalse(result.isValid)
        assertEquals(ValidationError.BELOW_MINIMUM, result.validationError)
    }

    @Test
    fun `exceeding available diamonds is invalid`() {
        val result = ExchangeCalculator.calculate(6_000L, availableDiamonds = 5_000L)
        assertFalse(result.isValid)
        assertEquals(ValidationError.INSUFFICIENT_BALANCE, result.validationError)
    }

    @Test
    fun `housie ticket always has 3 rows, 9 cols, 15 numbers, 5 per row`() {
        repeat(25) { seed ->
            val ticket = HousieEngine.generateTicket(Random(seed))
            assertEquals(3, ticket.rows.size)
            ticket.rows.forEach { row ->
                assertEquals(9, row.size)
                assertEquals(5, row.count { it != null })
            }
            assertEquals(15, ticket.allNumbers.size)
            assertTrue(ticket.allNumbers.all { it in 1..90 })
        }
    }

    @Test
    fun `housie claim validation verifies Early 5, Lines, and Full House accurately`() {
        val ticket = HousieEngine.generateTicket(Random(42))
        val row0 = ticket.rowNumbers(0).toSet()
        val row1 = ticket.rowNumbers(1).toSet()
        val all = ticket.allNumbers

        assertFalse(HousieEngine.isClaimValid(ticket, row0.take(4).toSet(), HousieClaimType.EARLY_5))
        assertTrue(HousieEngine.isClaimValid(ticket, row0, HousieClaimType.EARLY_5))
        assertTrue(HousieEngine.isClaimValid(ticket, row0, HousieClaimType.FIRST_LINE))
        assertFalse(HousieEngine.isClaimValid(ticket, row0, HousieClaimType.SECOND_LINE))
        assertTrue(HousieEngine.isClaimValid(ticket, row1, HousieClaimType.SECOND_LINE))
        assertFalse(HousieEngine.isClaimValid(ticket, row0 + row1, HousieClaimType.FULL_HOUSE))
        assertTrue(HousieEngine.isClaimValid(ticket, all, HousieClaimType.FULL_HOUSE))
    }

    @Test
    fun `housie rooms have expected entry coins and diamond prizes`() {
        assertEquals(4, HousieEngine.rooms.size)
        assertEquals(500L, HousieEngine.rooms[0].entryCoins)
        assertEquals(5_000L, HousieEngine.rooms[0].prizeDiamonds)
        assertEquals(2_000L, HousieEngine.rooms[1].entryCoins)
        assertEquals(15_000L, HousieEngine.rooms[1].prizeDiamonds)
        assertEquals(10_000L, HousieEngine.rooms[2].entryCoins)
        assertEquals(80_000L, HousieEngine.rooms[2].prizeDiamonds)
        assertEquals(50_000L, HousieEngine.rooms[3].entryCoins)
        assertEquals(400_000L, HousieEngine.rooms[3].prizeDiamonds)
    }

    @Test
    fun `founder panel access is strictly limited to verified vinayakglobaldream@gmailcom`() {
        assertTrue(com.example.domain.FounderConfig.isFounderEmail("vinayakglobaldream@gmail.com", isEmailVerified = true))
        assertTrue(com.example.domain.FounderConfig.isFounderEmail("  VinayakGlobalDream@gmail.com ", isEmailVerified = true))
        assertFalse(com.example.domain.FounderConfig.isFounderEmail("vinayakglobaldream@gmail.com", isEmailVerified = false))
        assertFalse(com.example.domain.FounderConfig.isFounderEmail("randomplayer@gmail.com", isEmailVerified = true))
        assertFalse(com.example.domain.FounderConfig.isFounderEmail(null, isEmailVerified = true))
    }

    @Test
    fun `virtual ID generator creates valid deterministic IDs and normalizes queries`() {
        val idA1 = com.example.domain.VirtualIdGenerator.fromUid("uid_player_alpha")
        val idA2 = com.example.domain.VirtualIdGenerator.fromUid("uid_player_alpha")
        val idB = com.example.domain.VirtualIdGenerator.fromUid("uid_player_beta")

        assertEquals(idA1, idA2)
        assertTrue(idA1 != idB)
        assertTrue(com.example.domain.VirtualIdGenerator.isValid(idA1))
        assertTrue(com.example.domain.VirtualIdGenerator.isValid(idB))

        assertEquals("VGD-RAHUL1", com.example.domain.VirtualIdGenerator.normalize(" rahul1 "))
        assertEquals("VGD-RAHUL1", com.example.domain.VirtualIdGenerator.normalize("vgd-rahul1"))
        assertTrue(com.example.domain.VirtualIdGenerator.isValid("VGD-RAHUL1"))
        assertFalse(com.example.domain.VirtualIdGenerator.isValid("VGD-12"))
        assertFalse(com.example.domain.VirtualIdGenerator.isValid("ABC-123456"))
    }
}
