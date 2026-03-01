package com.ghogho.ghoghochess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ghogho.ghoghochess.model.PieceColor
import com.ghogho.ghoghochess.model.Position
import com.ghogho.ghoghochess.viewmodel.ChessViewModel
import com.ghogho.ghoghochess.viewmodel.GameState
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {

    private val viewModel: ChessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob
        MobileAds.initialize(this) {}

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        AdmobBanner()
                    }
                ) { innerPadding ->
                    val state by viewModel.state.collectAsState()
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        StatusBar(state)
                        Spacer(modifier = Modifier.height(16.dp))
                        ChessBoard(
                            state = state,
                            onSquareClick = { row, col -> viewModel.onSquareClicked(row, col) }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Controls(
                            state = state,
                            onRestart = { viewModel.restartGame() },
                            onUndo = { viewModel.undoMove() },
                            onToggleVsCpu = { viewModel.toggleVsCpu() },
                            onCycleDifficulty = { viewModel.cycleDifficulty() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdmobBanner() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun StatusBar(state: GameState) {
    val statusText = when {
        state.isCheckmate -> if (state.currentTurn == PieceColor.WHITE) "Black Wins!" else "White Wins!"
        state.isDraw -> "Game Drawn!"
        state.isCheck -> "${state.currentTurn} is in CHECK!"
        else -> "${state.currentTurn}'s Turn"
    }

    val textColor = when {
        state.isCheckmate || state.isDraw -> Color.Red
        state.isCheck -> Color(0xFFFFA500) // Orange
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.isCpuThinking) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            text = statusText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ChessBoard(state: GameState, onSquareClick: (Int, Int) -> Unit) {
    val lightSquare = Color(0xFFF0D9B5)
    val darkSquare = Color(0xFFB58863)
    val highlightColor = Color(0x66FFFF00) // Yellow semi-transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.Gray)
            .padding(4.dp)
    ) {
        for (row in 0..7) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0..7) {
                    val isLight = (row + col) % 2 == 0
                    val baseColor = if (isLight) lightSquare else darkSquare
                    
                    val piece = state.board.getPiece(Position(row, col))
                    val isSelected = state.selectedPosition == Position(row, col)
                    val isValidMove = state.validMovesForSelected.any { it.to == Position(row, col) }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(baseColor)
                            .clickable { onSquareClick(row, col) }
                            .then(
                                if (isSelected) Modifier.background(highlightColor) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (piece != null) {
                            Text(
                                text = piece.toSymbol(),
                                fontSize = 36.sp,
                                color = Color.Black // Let unicode outline differentiation work natively
                            )
                        }
                        
                        if (isValidMove) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x8800FF00))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Controls(
    state: GameState,
    onRestart: () -> Unit,
    onUndo: () -> Unit,
    onToggleVsCpu: () -> Unit,
    onCycleDifficulty: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onRestart) {
                Text("Restart Game")
            }
            
            Button(
                onClick = onUndo,
                enabled = !state.isCpuThinking
            ) {
                Text("Undo")
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("VS CPU")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = state.vsCpu,
                    onCheckedChange = { onToggleVsCpu() }
                )
            }
        }
        
        if (state.vsCpu) {
            Spacer(modifier = Modifier.height(16.dp))
            val diffText = when (state.cpuDifficulty) {
                2 -> "EASY"
                4 -> "MEDIUM"
                6 -> "HARD"
                else -> "UNKNOWN"
            }
            OutlinedButton(onClick = onCycleDifficulty) {
                Text("Difficulty: $diffText")
            }
        }
    }
}