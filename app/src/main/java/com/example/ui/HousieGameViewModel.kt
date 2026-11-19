package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.HousieDataRepository
import com.example.domain.ExchangeCalculator
import com.example.domain.HousieClaimType
import com.example.domain.HousieEngine
import com.example.domain.HousieRoom
import com.example.domain.HousieTicket
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WinPopupEvent(
    val claimType: HousieClaimType,
    val prizeDiamonds: Long,
    val formattedPrize: String
)

data class GameUiState(
    val activeRoom: HousieRoom? = null,
    val ticket: HousieTicket? = null,
    val calledNumbers: List<Int> = emptyList(),
    val claimedTypes: Set<HousieClaimType> = emptySet(),
    val winPopup: WinPopupEvent? = null,
    val wrongClaimVisible: Boolean = false,
    val isGameOver: Boolean = false,
    val winnerName: String? = null,
    val totalDiamondsWonInMatch: Long = 0L,
    val insufficientCoinsRoom: HousieRoom? = null
) {
    val currentNumber: Int? get() = calledNumbers.lastOrNull()
    val lastFiveNumbers: List<Int> get() = calledNumbers.takeLast(5).reversed()
    val calledSet: Set<Int> get() = calledNumbers.toSet()
    val matchedCount: Int get() = ticket?.allNumbers?.count { it in calledSet } ?: 0
}

class HousieGameViewModel(
    private val repository: HousieDataRepository,
    private val playerDisplayName: String = "You (VGD Player)",
    private val callIntervalMs: Long = 4_000L
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameUiState())
    val gameState: StateFlow<GameUiState> = _gameState.asStateFlow()

    private var callerJob: Job? = null
    private var shuffledPool: List<Int> = emptyList()
    private var poolIndex: Int = 0

    fun joinRoom(room: HousieRoom, onJoined: () -> Unit) {
        viewModelScope.launch {
            val result = repository.joinGameRoom(room.name, room.entryCoins)
            val joined = result.getOrNull() == true
            if (!joined) {
                _gameState.update { it.copy(insufficientCoinsRoom = room) }
                return@launch
            }
            startNewMatch(room)
            onJoined()
        }
    }

    fun dismissInsufficientCoinsPrompt() {
        _gameState.update { it.copy(insufficientCoinsRoom = null) }
    }

    private fun startNewMatch(room: HousieRoom) {
        callerJob?.cancel()
        shuffledPool = (1..90).shuffled()
        poolIndex = 0
        val ticket = HousieEngine.generateTicket()
        val firstNumber = shuffledPool.firstOrNull()
        if (firstNumber != null) {
            poolIndex = 1
        }

        _gameState.value = GameUiState(
            activeRoom = room,
            ticket = ticket,
            calledNumbers = if (firstNumber != null) listOf(firstNumber) else emptyList(),
            claimedTypes = emptySet(),
            winPopup = null,
            wrongClaimVisible = false,
            isGameOver = false,
            winnerName = null,
            totalDiamondsWonInMatch = 0L,
            insufficientCoinsRoom = null
        )

        callerJob = viewModelScope.launch {
            while (poolIndex < shuffledPool.size) {
                delay(callIntervalMs)
                val current = _gameState.value
                if (current.isGameOver || current.activeRoom == null) break
                val nextNum = shuffledPool[poolIndex]
                poolIndex++
                _gameState.update { state ->
                    state.copy(calledNumbers = state.calledNumbers + nextNum)
                }
            }
        }
    }

    fun claimWin(claimType: HousieClaimType) {
        val state = _gameState.value
        val room = state.activeRoom ?: return
        val ticket = state.ticket ?: return
        if (state.isGameOver || claimType in state.claimedTypes) return

        val valid = HousieEngine.isClaimValid(ticket, state.calledSet, claimType)
        if (!valid) {
            _gameState.update { it.copy(wrongClaimVisible = true) }
            return
        }

        val prize = HousieEngine.prizeForClaim(room, claimType)
        val formattedPrize = if (claimType == HousieClaimType.FULL_HOUSE) {
            room.prizeDisplay
        } else {
            ExchangeCalculator.formatNumber(prize)
        }

        val isFullHouse = claimType == HousieClaimType.FULL_HOUSE
        if (isFullHouse) {
            callerJob?.cancel()
        }

        _gameState.update { current ->
            current.copy(
                claimedTypes = current.claimedTypes + claimType,
                winPopup = WinPopupEvent(
                    claimType = claimType,
                    prizeDiamonds = prize,
                    formattedPrize = formattedPrize
                ),
                isGameOver = isFullHouse || current.isGameOver,
                winnerName = if (isFullHouse) playerDisplayName else current.winnerName,
                totalDiamondsWonInMatch = current.totalDiamondsWonInMatch + prize
            )
        }

        viewModelScope.launch {
            repository.awardGamePrize(room.name, claimType.label, prize)
        }
    }

    fun dismissWinPopup() {
        _gameState.update { it.copy(winPopup = null) }
    }

    fun dismissWrongClaim() {
        _gameState.update { it.copy(wrongClaimVisible = false) }
    }

    fun leaveGame() {
        callerJob?.cancel()
        _gameState.value = GameUiState()
    }

    override fun onCleared() {
        super.onCleared()
        callerJob?.cancel()
    }

    class Factory(
        private val repository: HousieDataRepository,
        private val playerDisplayName: String = "You (VGD Player)"
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HousieGameViewModel(repository, playerDisplayName) as T
        }
    }
}
