package com.ghogho.ghoghochess.model

class Board(
    val squares: Array<Array<Piece?>> = Array(8) { Array(8) { null } }
) {
    fun getPiece(position: Position): Piece? {
        if (!position.isValid()) return null
        return squares[position.row][position.col]
    }

    fun setPiece(position: Position, piece: Piece?) {
        if (position.isValid()) {
            squares[position.row][position.col] = piece
        }
    }

    fun copy(): Board {
        val newSquares = Array(8) { row ->
            Array(8) { col ->
                squares[row][col]?.copy()
            }
        }
        return Board(newSquares)
    }

    companion object {
        fun initial(): Board {
            val board = Board()
            
            // Set up pawns
            for (col in 0..7) {
                board.setPiece(Position(1, col), Piece(PieceType.PAWN, PieceColor.BLACK))
                board.setPiece(Position(6, col), Piece(PieceType.PAWN, PieceColor.WHITE))
            }
            
            // Set up other pieces
            val backRow = listOf(
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
            )
            
            for (col in 0..7) {
                board.setPiece(Position(0, col), Piece(backRow[col], PieceColor.BLACK))
                board.setPiece(Position(7, col), Piece(backRow[col], PieceColor.WHITE))
            }
            
            return board
        }
    }
}
