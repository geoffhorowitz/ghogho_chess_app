# Ghogho Chess MVP Walkthrough

## Overview
Successfully implemented the foundational Minimum Viable Product (MVP) of the Ghogho Chess application following the MVVM architecture and using Jetpack Compose in Kotlin.

## Changes Made
1. **Models (`ChessModels.kt` / `Board.kt`)**
   - Implemented standard models for `Piece`, `PieceType`, `PieceColor`, `Position`, and `Move`.
   - Built a `Board` class to initialize and house the core array state.
2. **Game Engine (`ChessEngine.kt`)**
   - Engineered pseudo-legal and legal move generation for all piece types.
   - Designed logic for capturing, preventing moves placing the king in check, and detecting Check/Checkmate/Draw states.
3. **AI Opponent (`MinimaxAI.kt`)**
   - Added Minimax algorithm driven logic with Alpha-Beta pruning spanning configurable search depths.
   - Added a straightforward Evaluation Function based purely on piece material valuation and subtle center-control bonuses.
4. **Jetpack Compose UI (`MainActivity.kt` / `ChessViewModel.kt`)**
   - Transformed `MainActivity` to mount an elegant UI with status display, an interactive chessboard via declarative composables, user controls, and an AdMob integrated Banner row.
   - Fused the engine logic with Jetpack Compose triggers through `ChessViewModel`, employing `StateFlow` and Coroutines for thread-safe asynchronous AI processing.
5. **Dependencies (`build.gradle.kts` / `local.properties`)**
   - Hooked up `play-services-ads`, and Compose ViewModels.
   - Pushed test identifiers securely up via `BuildConfig` reading from `local.properties` (gitignored).

## Items Skipped for MVP (Added to Future Subtasks)
As agreed, omitted implementations to speed up deployment:
- En Passant captures
- Castling rules
- Custom piece promotions (Queens automatically assumed)
- 50-move rule and Threefold Repetition.

## Validation Strategy
Because testing this requires visual confirmation and standard interaction inputs which the raw console cannot simulate:
1. Open up the `ChessApp` inside Android Studio.
2. Synchronize the Gradle project. 
3. Run the application through the Android emulator or a physical device.
4. Verify moving a few pawns/pieces locally, followed by turning ON the `VS CPU` switch and confirming the game automatically schedules and runs the Minimax framework smoothly.
