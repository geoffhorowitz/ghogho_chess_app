package com.ghogho.ghoghochess.engine

import com.ghogho.ghoghochess.model.*

class ChessEngine {
    var board = Board.initial()
    var currentTurn = PieceColor.WHITE
    
    var isCheck = false
    var isCheckmate = false
    var isDraw = false // By no valid moves (stalemate)
    
    data class EngineState(
        val board: Board,
        val currentTurn: PieceColor,
        val isCheck: Boolean,
        val isCheckmate: Boolean,
        val isDraw: Boolean
    )

    private val history = mutableListOf<EngineState>()
    
    fun reset() {
        board = Board.initial()
        currentTurn = PieceColor.WHITE
        isCheck = false
        isCheckmate = false
        isDraw = false
        history.clear()
    }

    fun getValidMoves(from: Position): List<Move> {
        val piece = board.getPiece(from) ?: return emptyList()
        if (piece.color != currentTurn) return emptyList()

        val pseudoLegalMoves = generatePseudoLegalMoves(from, piece, board)
        
        // Filter out moves that leave own king in check
        return pseudoLegalMoves.filter { move ->
            val boardCopy = board.copy()
            applyMove(boardCopy, move)
            !isKingInCheck(currentTurn, boardCopy)
        }
    }
    
    fun getAllValidMoves(color: PieceColor = currentTurn): List<Move> {
        val allValidMoves = mutableListOf<Move>()
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = board.getPiece(pos)
                if (piece != null && piece.color == color) {
                    allValidMoves.addAll(getValidMoves(pos))
                }
            }
        }
        return allValidMoves
    }
    
    fun makeMove(move: Move) {
        // Save state before move
        history.add(
            EngineState(
                board = board.copy(),
                currentTurn = currentTurn,
                isCheck = isCheck,
                isCheckmate = isCheckmate,
                isDraw = isDraw
            )
        )
        
        applyMove(board, move)
        currentTurn = currentTurn.opposite()
        updateGameState()
    }

    fun undoLastMove(): Boolean {
        if (history.isEmpty()) return false
        
        val lastState = history.removeLast()
        board = lastState.board
        currentTurn = lastState.currentTurn
        isCheck = lastState.isCheck
        isCheckmate = lastState.isCheckmate
        isDraw = lastState.isDraw
        
        return true
    }
    
    private fun updateGameState() {
        isCheck = isKingInCheck(currentTurn, board)
        val validMoves = getAllValidMoves(currentTurn)
        
        if (validMoves.isEmpty()) {
            if (isCheck) {
                isCheckmate = true
            } else {
                isDraw = true
            }
        }
    }
    
    private fun applyMove(boardState: Board, move: Move) {
        boardState.setPiece(move.from, null)
        
        val movedPiece = move.pieceMoved.copy(hasMoved = true)
        
        if (move.isPromotion && move.promotedTo != null) {
            boardState.setPiece(move.to, movedPiece.copy(type = move.promotedTo))
        } else {
            boardState.setPiece(move.to, movedPiece)
        }
    }
    
    private fun isKingInCheck(color: PieceColor, boardState: Board): Boolean {
        // Find king
        var kingPos: Position? = null
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = boardState.getPiece(Position(row, col))
                if (piece != null && piece.type == PieceType.KING && piece.color == color) {
                    kingPos = Position(row, col)
                    break
                }
            }
            if (kingPos != null) break
        }
        
        if (kingPos == null) return false // Should not happen in real games
        
        // See if any opponent piece can attack kingPos
        val opponentColor = color.opposite()
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = boardState.getPiece(pos)
                if (piece != null && piece.color == opponentColor) {
                    val oppMoves = generatePseudoLegalMoves(pos, piece, boardState)
                    if (oppMoves.any { it.to == kingPos }) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun generatePseudoLegalMoves(from: Position, piece: Piece, boardState: Board): List<Move> {
        return when (piece.type) {
            PieceType.PAWN -> generatePawnMoves(from, piece, boardState)
            PieceType.KNIGHT -> generateKnightMoves(from, piece, boardState)
            PieceType.BISHOP -> generateSlidingMoves(from, piece, boardState, arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)))
            PieceType.ROOK -> generateSlidingMoves(from, piece, boardState, arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)))
            PieceType.QUEEN -> generateSlidingMoves(from, piece, boardState, arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1), intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)))
            PieceType.KING -> generateKingMoves(from, piece, boardState)
        }
    }
    
    private fun generateSlidingMoves(from: Position, piece: Piece, boardState: Board, directions: Array<IntArray>): List<Move> {
        val moves = mutableListOf<Move>()
        for (dir in directions) {
            var currRow = from.row + dir[0]
            var currCol = from.col + dir[1]
            while (currRow in 0..7 && currCol in 0..7) {
                val to = Position(currRow, currCol)
                val targetPiece = boardState.getPiece(to)
                
                if (targetPiece == null) {
                    moves.add(Move(from, to, piece))
                } else {
                    if (targetPiece.color != piece.color) {
                        moves.add(Move(from, to, piece, targetPiece))
                    }
                    break
                }
                
                currRow += dir[0]
                currCol += dir[1]
            }
        }
        return moves
    }
    
    private fun generateKnightMoves(from: Position, piece: Piece, boardState: Board): List<Move> {
        val moves = mutableListOf<Move>()
        val offsets = arrayOf(
            intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(-2, 1), intArrayOf(-2, -1),
            intArrayOf(1, 2), intArrayOf(1, -2), intArrayOf(-1, 2), intArrayOf(-1, -2)
        )
        
        for (offset in offsets) {
            val toRow = from.row + offset[0]
            val toCol = from.col + offset[1]
            if (toRow in 0..7 && toCol in 0..7) {
                val to = Position(toRow, toCol)
                val targetPiece = boardState.getPiece(to)
                if (targetPiece == null || targetPiece.color != piece.color) {
                    moves.add(Move(from, to, piece, targetPiece))
                }
            }
        }
        return moves
    }
    
    private fun generateKingMoves(from: Position, piece: Piece, boardState: Board): List<Move> {
        val moves = mutableListOf<Move>()
        val offsets = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
            intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)
        )
        
        for (offset in offsets) {
            val toRow = from.row + offset[0]
            val toCol = from.col + offset[1]
            if (toRow in 0..7 && toCol in 0..7) {
                val to = Position(toRow, toCol)
                val targetPiece = boardState.getPiece(to)
                if (targetPiece == null || targetPiece.color != piece.color) {
                    moves.add(Move(from, to, piece, targetPiece))
                }
            }
        }
        return moves
    }
    
    private fun generatePawnMoves(from: Position, piece: Piece, boardState: Board): List<Move> {
        val moves = mutableListOf<Move>()
        val direction = if (piece.color == PieceColor.WHITE) -1 else 1
        val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
        val promoteRow = if (piece.color == PieceColor.WHITE) 0 else 7
        
        // Forward 1
        val oneStepRow = from.row + direction
        if (oneStepRow in 0..7) {
            val oneStepPos = Position(oneStepRow, from.col)
            if (boardState.getPiece(oneStepPos) == null) {
                val isPromo = oneStepRow == promoteRow
                moves.add(Move(from, oneStepPos, piece, isPromotion = isPromo, promotedTo = if (isPromo) PieceType.QUEEN else null))
                
                // Forward 2
                if (from.row == startRow) {
                    val twoStepRow = from.row + 2 * direction
                    val twoStepPos = Position(twoStepRow, from.col)
                    if (boardState.getPiece(twoStepPos) == null) {
                        moves.add(Move(from, twoStepPos, piece))
                    }
                }
            }
        }
        
        // Captures
        for (colOffset in intArrayOf(-1, 1)) {
            val toCol = from.col + colOffset
            if (oneStepRow in 0..7 && toCol in 0..7) {
                val capturePos = Position(oneStepRow, toCol)
                val targetPiece = boardState.getPiece(capturePos)
                if (targetPiece != null && targetPiece.color != piece.color) {
                    val isPromo = oneStepRow == promoteRow
                    moves.add(Move(from, capturePos, piece, targetPiece, isPromotion = isPromo, promotedTo = if (isPromo) PieceType.QUEEN else null))
                }
            }
        }
        
        return moves
    }
}
