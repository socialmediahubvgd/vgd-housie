package com.example.domain

import kotlin.random.Random

enum class HousieClaimType(val label: String) {
    EARLY_5("Early 5"),
    FIRST_LINE("First Line"),
    SECOND_LINE("Second Line"),
    THIRD_LINE("Third Line"),
    FULL_HOUSE("Full House")
}

data class HousieRoom(
    val id: String,
    val name: String,
    val entryCoins: Long,
    val prizeDiamonds: Long,
    val prizeDisplay: String,
    val badge: String
)

data class HousieTicket(
    val rows: List<List<Int?>> // 3 rows x 9 columns, null = blank cell
) {
    val allNumbers: Set<Int> by lazy {
        rows.flatten().filterNotNull().toSet()
    }

    fun rowNumbers(rowIndex: Int): List<Int> {
        return rows.getOrElse(rowIndex) { emptyList() }.filterNotNull()
    }
}

object HousieEngine {

    val rooms: List<HousieRoom> = listOf(
        HousieRoom(
            id = "small",
            name = "Small Room",
            entryCoins = 500L,
            prizeDiamonds = 5_000L,
            prizeDisplay = "5,000",
            badge = "Starter"
        ),
        HousieRoom(
            id = "medium",
            name = "Medium Room",
            entryCoins = 2_000L,
            prizeDiamonds = 15_000L,
            prizeDisplay = "15,000",
            badge = "Popular"
        ),
        HousieRoom(
            id = "big",
            name = "Big Room",
            entryCoins = 10_000L,
            prizeDiamonds = 80_000L,
            prizeDisplay = "80,000",
            badge = "High Stakes"
        ),
        HousieRoom(
            id = "vip",
            name = "VIP Room",
            entryCoins = 50_000L,
            prizeDiamonds = 400_000L,
            prizeDisplay = "4,00,000",
            badge = "Royal VIP"
        )
    )

    fun prizeForClaim(room: HousieRoom, claimType: HousieClaimType): Long {
        return when (claimType) {
            HousieClaimType.FULL_HOUSE -> room.prizeDiamonds
            HousieClaimType.EARLY_5 -> (room.prizeDiamonds * 10L) / 100L
            HousieClaimType.FIRST_LINE,
            HousieClaimType.SECOND_LINE,
            HousieClaimType.THIRD_LINE -> (room.prizeDiamonds * 20L) / 100L
        }
    }

    fun generateTicket(random: Random = Random.Default): HousieTicket {
        val occupancy = Array(3) { BooleanArray(9) }

        // Step 1: Give every column 1 number distributed evenly (3 per row)
        val shuffledCols = (0..8).shuffled(random)
        for (i in 0 until 9) {
            val row = i / 3
            val col = shuffledCols[i]
            occupancy[row][col] = true
        }

        // Step 2: Add 2 more numbers per row so each row has exactly 5 numbers (15 total)
        for (row in 0 until 3) {
            val availableCols = (0..8).filter { col ->
                !occupancy[row][col] && (0 until 3).count { r -> occupancy[r][col] } < 3
            }.shuffled(random)
            availableCols.take(2).forEach { col ->
                occupancy[row][col] = true
            }
        }

        // Step 3: Populate each column with sorted distinct numbers in its decade range
        val grid = Array(3) { arrayOfNulls<Int>(9) }
        for (col in 0 until 9) {
            val range = when (col) {
                0 -> 1..9
                8 -> 80..90
                else -> (col * 10)..(col * 10 + 9)
            }
            val activeRows = (0 until 3).filter { r -> occupancy[r][col] }
            val picked = range.shuffled(random).take(activeRows.size).sorted()
            activeRows.forEachIndexed { idx, row ->
                grid[row][col] = picked[idx]
            }
        }

        return HousieTicket(rows = grid.map { it.toList() })
    }

    fun isClaimValid(
        ticket: HousieTicket,
        calledNumbers: Set<Int>,
        claimType: HousieClaimType
    ): Boolean {
        return when (claimType) {
            HousieClaimType.EARLY_5 -> ticket.allNumbers.count { it in calledNumbers } >= 5
            HousieClaimType.FIRST_LINE -> ticket.rowNumbers(0).let { it.isNotEmpty() && it.all { n -> n in calledNumbers } }
            HousieClaimType.SECOND_LINE -> ticket.rowNumbers(1).let { it.isNotEmpty() && it.all { n -> n in calledNumbers } }
            HousieClaimType.THIRD_LINE -> ticket.rowNumbers(2).let { it.isNotEmpty() && it.all { n -> n in calledNumbers } }
            HousieClaimType.FULL_HOUSE -> ticket.allNumbers.let { it.isNotEmpty() && it.all { n -> n in calledNumbers } }
        }
    }
}
