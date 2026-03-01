package com.ghogho.ghoghochess.ai

import com.ghogho.ghoghochess.engine.ChessEngine
import com.ghogho.ghoghochess.model.*
import kotlin.math.max
import kotlin.math.min

class MinimaxAI(private val myColor: PieceColor, private val difficultyDepth: Int = 4) {

    private val pieceValues = mapOf(
        PieceType.PAWN to 100,
        PieceType.KNIGHT to 320,
        PieceType.BISHOP to 330,
        PieceType.ROOK to 500,
        PieceType.QUEEN to 900,
        PieceType.KING to 20000
    )

    fun getBestMove(engine: ChessEngine): Move? {
        val validMoves = engine.getAllValidMoves(myColor)
        if (validMoves.isEmpty()) return null

        var bestMove: Move? = null
        var bestValue = Int.MIN_VALUE
        var alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE

        for (move in validMoves) {
            val engineCopy = copyEngine(engine)
            engineCopy.makeMove(move)
            
            val boardValue = minimax(engineCopy, difficultyDepth - 1, alpha, beta, false)
            
            if (boardValue > bestValue) {
                bestValue = boardValue
                bestMove = move
            }
            alpha = max(alpha, bestValue)
        }

        return bestMove ?: validMoves.firstOrNull()
    }

    private fun minimax(engine: ChessEngine, depth: Int, alpha: Int, beta: Int, isMaximizing: Boolean): Int {
        if (depth == 0 || engine.isCheckmate || engine.isDraw) {
            return evaluate(engine)
        }

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            val moves = engine.getAllValidMoves(myColor)
            for (move in moves) {
                val engineCopy = copyEngine(engine)
                engineCopy.makeMove(move)
                val eval = minimax(engineCopy, depth - 1, currentAlpha, currentBeta, false)
                maxEval = max(maxEval, eval)
                currentAlpha = max(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            val moves = engine.getAllValidMoves(myColor.opposite())
            for (move in moves) {
                val engineCopy = copyEngine(engine)
                engineCopy.makeMove(move)
                val eval = minimax(engineCopy, depth - 1, currentAlpha, currentBeta, true)
                minEval = min(minEval, eval)
                currentBeta = min(currentBeta, eval)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }

    private fun evaluate(engine: ChessEngine): Int {
        if (engine.isCheckmate) {
            return if (engine.currentTurn == myColor) -100000 else 100000
        }
        if (engine.isDraw) {
            return 0
        }

        var score = 0
        val board = engine.board

        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board.getPiece(Position(row, col))
                if (piece != null) {
                    val value = pieceValues[piece.type] ?: 0
                    if (piece.color == myColor) {
                        score += value
                    } else {
                        score -= value
                    }
                    
                    val centerBonus = if (row in 3..4 && col in 3..4) 10 else 0
                    if (piece.color == myColor) {
                        score += centerBonus
                    } else {
                        score -= centerBonus
                    }
                }
            }
        }

        return score
    }

    private fun copyEngine(engine: ChessEngine): ChessEngine {
        val newEngine = ChessEngine()
        newEngine.board = engine.board.copy()
        newEngine.currentTurn = engine.currentTurn
        newEngine.isCheck = engine.isCheck
        newEngine.isCheckmate = engine.isCheckmate
        newEngine.isDraw = engine.isDraw
        return newEngine
    }
}
