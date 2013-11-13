package com.krisapps.reversi;

import java.util.Random;

import android.graphics.Point;
import android.util.Log;


public class AI {
	// DEBUG private static final int USE_BEST = 1000;
	private static final int USE_BEST = 900;
	protected int mLevel = 1;
	protected long mTimeLimit;
	private Random mRandom = new Random();
	private long mStartTime;
	private long mEndTime;
	private transient ProgressUpdater mUpdater;
	private Board mLevelBoard[];

	public AI(int level, int maxSeconds) {
		mLevel = level;
		mTimeLimit = maxSeconds * 1000;
		initialise();
	}

	/**
	 * @return the level
	 */
	public int getLevel() {
		return mLevel;
	}

	/**
	 * @return the timeLimit
	 */
	public int getMaxSeconds() {
		return (int) (mTimeLimit / 1000);
	}

	/**
	 * @param timeLimit the timeLimit to set
	 */
	public void setMaxSeconds(int maxSeconds) {
		mTimeLimit = maxSeconds * 1000;
	}

	/**
	 * @param level the level to set
	 */
	public void setLevel(int level) {
		mLevel = level;
		initialise();
	}

	public Point chooseMove(Board board, int player, ProgressUpdater updater) {
		mStartTime = System.currentTimeMillis();
		mEndTime = mTimeLimit + mStartTime;
		mUpdater = updater;
		
		// Log.i(ReversiActivity.TAG, "chooseMove: " + mLevel + ", " + mTimeLimit);
		// Debug.startMethodTracing("ai");
	    
		Point move = playerMoveTop(board, player);
		
		// Debug.stopMethodTracing();
		
		return move;
	}

	public void finish() {
		mEndTime = System.currentTimeMillis();
	}

	protected void initialise() {
		mLevelBoard = new Board[mLevel + 1];
		for ( int i = 0; i < mLevel + 1; i++ )
			mLevelBoard[i] = new Board();
	}

	/**
	 * Determine whether or not the new score is better than the old score. Use a bit of random variation to keep
	 * thinks a bit more interesting
	 * @param score
	 * @param bestScore
	 * @param length
	 * @return
	 */
	private final boolean isBest(int score, int bestScore, int length) {
		int diff = score - bestScore;
		int pChange;
		if ( diff > 0 ) {
			// The new score is better than the current best score so we should probably use it
			// The better the new score the more likely we are to use it
			pChange = 1000 - (1000 - USE_BEST) / diff; 
		} else {
			// The new score is equal or not so good so we might use it but the worse the score is the less likely
			// If the scores are the same, we still prefer the first ones we find because we check the better squares first
			pChange = USE_BEST / length / (1 - diff);
		}
		
		return mRandom.nextInt(1000) < pChange;
	}

	private int playerMove(int level, Board board, int player) {
		int bestScore = Integer.MIN_VALUE;
		Point[] moves = board.getLegalMoves(player);
		int length = moves.length;
		
		if ( length > 0 ) {
			Board nextBoard = mLevelBoard[level];
			// Board nextBoard = new Board();
			// Step through the legal moves and pick best one
			for ( Point move: moves ) {
				nextBoard.copyBoard(board);
				nextBoard.takeMove(player, move.x, move.y);
				int score = (level <= 0) ? 
						nextBoard.score(player): 
						-playerMove(level - 1, nextBoard, -player);
	
				// If we have run out of time we should simply break out and make do with the best score we have,
				// breaking here because the best value just returned is probably incomplete.
				long nowTime = System.currentTimeMillis();
				if ( nowTime > mEndTime )
					break;
		
				mUpdater.updateProgress((int) (100 * (nowTime - mStartTime)/mTimeLimit));
				
				// Apply strategy bonuses
				score += strategyBonus(move, board);
				
				if ( score > bestScore ) {
					bestScore = score;
				}
			}
		} else {
			// Game over! This very good if the player has won +600 and very bad if the player has lost -600
			// maximum score is normally 64 so +600 makes this better than any intermediate score.
			bestScore = board.score(player);
			if ( bestScore > 0 )
				bestScore += 600;
			else
				bestScore -= 600;
		}
		
		return bestScore;
	}

	private Point playerMoveTop(Board board, int player) {
		Point bestMove = null;
		int bestScore = Integer.MIN_VALUE;
	
		Point[] moves = board.getLegalMoves(player);
		int length = moves.length;
		// If there is only one legal move there is no point thinking about it! 
		if ( length == 1 )
			return moves[0];
		
		if (moves.length > 0) {
			// If we run out of time we'll need to return the first legal move.
			bestMove = moves[0];
			Board levelBoard = mLevelBoard[mLevel];
			// Step through the legal moves and pick best one
			for ( Point move: moves ) {
				// Make a copy of the board and take the move on the copy
				levelBoard.copyBoard(board);
				levelBoard.takeMove(player, move.x, move.y);
				
				// Recursively calculate the score for this move, assuming the computer and human make the best move
				// every turn.
				int score = -playerMove(mLevel - 1, levelBoard, -player);
				
				// If we have run out of time we should simply return with the best move so far, ignoring the current score
				// because it's calculation is incomplete. If we haven't even managed to complete analysis of the first move
				// we just return that!
				long nowTime = System.currentTimeMillis();
				if ( nowTime > mEndTime )
					return ( bestMove == null ) ? move: bestMove;
		
				mUpdater.updateProgress((int) (100 * (nowTime - mStartTime)/mTimeLimit));
				
				// Apply strategy bonuses
				score += strategyBonus(move, board);
				
				if ( bestScore == Integer.MIN_VALUE || isBest(score, bestScore, length) ) {
					bestScore = score;
					bestMove = move;
				}
				
			}
		}
		
		return bestMove;
	}
	/**
	 * Add strategy bonuses for good moves.
	 * 400 for a corner move "early" in the game
	 * 200 TODO for less good moves e.g.
	 *     edge moves that can't be immediately flipped
	 * 600 is used for end games so this will never outweigh a winning move
	 * TODO is 48 a good cut off? No idea really!
	 * @param move
	 * @param board
	 * @return
	 */
	private final int strategyBonus(Point move, Board board) {
		int moveCount = board.getMoveCount();
		int x = move.x;
		int y = move.y;
		
		// Corner moves are usually very good
		if ( moveCount < 48 ) {
			if ( x == 0 || x == 7 ) {
				// Corner move
				if ( y == 0 || y == 7 )
					return 400;
			
				// Edge move with no adjacent pieces
				/* TODO More than this or none! if ( board.getPlayer(x, y + 1) == Board.EMPTY &&
					 board.getPlayer(x, y - 1) == Board.EMPTY )
					return 200;*/
			}
			
			/* TODO More than this or none! if ( y == 0 || y == 7 ) {
				// Edge move with no adjacent pieces
				if ( board.getPlayer(x + 1, y) == Board.EMPTY &&
					 board.getPlayer(x - 1, y) == Board.EMPTY )
					return 200;
			}*/
		}
		
		return 0;
	}
}
