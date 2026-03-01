package com.ghogho.ghoghochess.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghogho.ghoghochess.ai.MinimaxAI
import com.ghogho.ghoghochess.engine.ChessEngine
import com.ghogho.ghoghochess.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameState(
    val board: Board = Board.initial(),
    val currentTurn: PieceColor = PieceColor.WHITE,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isDraw: Boolean = false,
    val selectedPosition: Position? = null,
    val validMovesForSelected: List<Move> = emptyList(),
    val vsCpu: Boolean = false,
    val cpuDifficulty: Int = 4, // 2=Easy, 4=Medium, 6=Hard
    val isCpuThinking: Boolean = false
)

class ChessViewModel : ViewModel() {
    private val engine = ChessEngine()
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    fun onSquareClicked(row: Int, col: Int) {
        val currentState = _state.value
        if (currentState.isCheckmate || currentState.isDraw) return
        if (currentState.vsCpu && currentState.currentTurn == PieceColor.BLACK) return // CPU's turn

        val clickedPos = Position(row, col)

        if (currentState.selectedPosition != null) {
            val move = currentState.validMovesForSelected.find { it.to == clickedPos }
            if (move != null) {
                engine.makeMove(move)
                updateState()
                
                if (_state.value.vsCpu && !engine.isCheckmate && !engine.isDraw) {
                    makeCpuMove()
                }
                return
            }
        }

        val piece = engine.board.getPiece(clickedPos)
        if (piece != null && piece.color == currentState.currentTurn) {
            val validMoves = engine.getValidMoves(clickedPos)
            _state.value = currentState.copy(
                selectedPosition = clickedPos,
                validMovesForSelected = validMoves
            )
        } else {
            _state.value = currentState.copy(
                selectedPosition = null,
                validMovesForSelected = emptyList()
            )
        }
    }

    private fun updateState() {
        val currentState = _state.value
        _state.value = currentState.copy(
            board = engine.board.copy(),
            currentTurn = engine.currentTurn,
            isCheck = engine.isCheck,
            isCheckmate = engine.isCheckmate,
            isDraw = engine.isDraw,
            selectedPosition = null,
            validMovesForSelected = emptyList(),
            isCpuThinking = false
        )
    }

    private fun makeCpuMove() {
        val currentState = _state.value
        _state.value = currentState.copy(isCpuThinking = true)
        
        viewModelScope.launch(Dispatchers.Default) {
            val ai = MinimaxAI(PieceColor.BLACK, currentState.cpuDifficulty)
            delay(500) // Artificial delay
            val bestMove = ai.getBestMove(engine)
            
            if (bestMove != null) {
                launch(Dispatchers.Main) {
                    engine.makeMove(bestMove)
                    updateState()
                }
            } else {
                launch(Dispatchers.Main) {
                    updateState()
                }
            }
        }
    }

    fun restartGame() {
        engine.reset()
        updateState()
    }

    fun toggleVsCpu() {
        val currentState = _state.value
        _state.value = currentState.copy(vsCpu = !currentState.vsCpu)
        if (!currentState.vsCpu && engine.currentTurn == PieceColor.BLACK && !engine.isCheckmate && !engine.isDraw) {
            makeCpuMove()
        }
    }

    fun cycleDifficulty() {
        val currentState = _state.value
        val newDiff = when (currentState.cpuDifficulty) {
            2 -> 4
            4 -> 6
            else -> 2
        }
        _state.value = currentState.copy(cpuDifficulty = newDiff)
    }

    fun undoMove() {
        val currentState = _state.value
        if (currentState.isCpuThinking) return

        if (engine.undoLastMove()) {
            // In vsCpu mode, if it's the CPU's turn after one undo, undo again
            if (currentState.vsCpu && engine.currentTurn == PieceColor.BLACK) {
                engine.undoLastMove()
            }
            updateState()
        }
    }
}
