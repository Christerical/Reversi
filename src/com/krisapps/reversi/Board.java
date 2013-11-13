/**
 * 
 */
package com.krisapps.reversi;

import java.io.Serializable;
import java.util.ArrayList;

import android.graphics.Point;

/**
 * @author Chris Harrison
 *
 */
public class Board implements Serializable {
	private static final long serialVersionUID = 1;
	private int mBoard[];
	private int mCurrentPlayer = WHITE;
	private int mMoveCount = 0;
	private SerializablePoint mMove;
	private Board mPreviousBoard;
	
	private transient BoardAnimator mFlipper;

	public final static int WHITE =  1;
	public final static int EMPTY =  0;
	public final static int BLACK = -1;

	private final static Point sdXY[] = {
		new Point(-1, -1), new Point( 0, -1), new Point(+1, -1),
		new Point(-1,  0),                    new Point(+1,  0),
		new Point(-1, +1), new Point( 0, +1), new Point(+1, +1)	
	};
	
    private final static Point sXYOrder[];
    
    static {
		ArrayList<Point> first = new ArrayList<Point>();
		ArrayList<Point> second = new ArrayList<Point>();
		ArrayList<Point> third = new ArrayList<Point>();
		ArrayList<Point> fourth = new ArrayList<Point>();
		ArrayList<Point> fifth = new ArrayList<Point>();
		
		for ( int i = 0; i < 8; i++ ) {
			for ( int j = 0; j < 8; j++ ) {
				if ( i == 0 || i == 8 - 1 ) {
					if ( j == 0 || j == 8 - 1 )
						first.add(new Point(i, j));
					else
						second.add(new Point(i, j));
				} else if ( i == 1 || i == 8 - 2 ){
					if ( j ==1 || j == 8 - 2 )
						fifth.add(new Point(i, j));
					else
						fourth.add(new Point(i, j));
				} else {
					third.add(new Point(i, j));
				}
			}
		}
		
		first.addAll(second);
		first.addAll(third);
		first.addAll(fourth);
		first.addAll(fifth);
	
		sXYOrder = first.toArray(new Point[8 * 8]);
    }
    
	public Board() {
		initialise(null);
	}
	
	public Board(BoardAnimator flipper) {
		initialise(flipper);
	}
	

	/**
	 * @return the previousBoard
	 */
	public Board getPreviousBoard() {
		return mPreviousBoard;
	}

	/**
	 * @param previousBoard the previousBoard to set
	 */
	public void setPreviousBoard(Board previousBoard) {
		mPreviousBoard = previousBoard;
	}

	/**
	 * Overwrite this board with a copy of the supplied board. This method is used instead of clone() to reduce
	 * reallocations.
	
	 * @param board
	 */
	public void copyBoard(Board board) {
		mCurrentPlayer = board.mCurrentPlayer;
		mMoveCount = board.mMoveCount;
		mMove = board.mMove;
		
		System.arraycopy(board.mBoard, 0, mBoard, 0, 64);
	}
	
	public int countLegalMoves(int player) {
		int count = 0;
		
		for ( Point p: sXYOrder ) {
			if ( isLegalMove(player, p.x, p.y) ) {
				count++;
			}
		}
	
		return count;
	}
	
	public int getCurrentPlayer() {
		return mCurrentPlayer;
	}
	
	public boolean isCurrentPlayer(int player) {
		return mCurrentPlayer == player;
	}
	
	/**
	 * @return the move
	 */
	public Point getMove() {
		return mMove;
	}

	public boolean isGameOver() {
		// Game is over if there are no more legal moves
		Point moves[] = getLegalMoves();
		return moves.length == 0;
	}
	
	public boolean isLegalMove(int x, int y) {
		return isLegalMove(mCurrentPlayer, x, y);
	}
	
	public boolean isLegalMove(int player, int x, int y) {
		// A move is legal if an adjacent square in any of eight directions contains an enemy piece
		// and after one or more enemy pieces in that direction there is a friendly piece.
		
		if ( player == mCurrentPlayer &&
			 0 <= x && x < 8 && 0 <= y && y < 8 &&
			 mBoard[x * 8 + y] == EMPTY &&
			 willFlip(player, x, y))
			return true;
		else
			return false;
	}
	
	/**
	 * Return an array of all the legal moves for the current player
	 * @return
	 */
	private Point[] getLegalMoves() {
		return getLegalMoves(mCurrentPlayer);
	}
	
	/**
	 * Return an array of all the legal moves for the player
	 * 
	 * Iterate through the moves in best move order so AI considers best moves first.
	 * @param player
	 * @return
	 */
	public Point[] getLegalMoves(int player) {
		int count = 0;
		ArrayList<Point> moves = new ArrayList<Point>();
		
		for ( Point p: sXYOrder ) {
			int x = p.x;
			int y = p.y;
			if ( isLegalMove(player, x, y) ) {
				count++;
				moves.add(new Point(x, y));
			}
		}
		
		return moves.toArray(new Point[count]);
	}
	
	public int score(int player) {
		int score = 0;
		
		for ( int b: mBoard ) {
			score += b;
		}

		return (player == WHITE) ? score: -score;
	}
	
	public void setAnimator(BoardAnimator flipper) {
		mFlipper = flipper;
	}
	
	public final int getPlayer(int x, int y) {
		return mBoard[x*8 + y];
	}
	
	public boolean takeMove(int player, int x, int y) {
		if ( isLegalMove(player, x, y)) {
			mBoard[x * 8 + y] = player;
			mMoveCount++;
			flipPieces(player, x, y);
			mCurrentPlayer = -player;
			mMove = new SerializablePoint(x, y);
			return true;
		}
		
		return false;
	}
	
	public boolean willFlip(int player, int x, int y) {
		// Check all eight surrounding squares for an enemy piece. These points are held as offsets in a static array
		// for simplicity and efficiency
		 
		for ( Point d: sdXY ) {
			int dx = d.x;
			int dy = d.y;
			int xdx = x + dx;
			int ydy = y + dy;
			if ( 0 <= xdx && xdx < 8 && 0 <= ydy && ydy < 8 &&  mBoard[xdx * 8 + ydy] == -player) {
				// An enemy piece is found check that it followed by zero or more enemy pieces
				// and then a friendly piece.
				for ( int xi = xdx + dx, yi = ydy + dy; ; xi += dx, yi += dy ) {
					if (!(0 <= xi && xi < 8 && 0 <= yi && yi < 8))
						break;
					int square = mBoard[xi * 8 + yi];
					if ( square == player )
						return true;
					if ( square == EMPTY )
						break;
				}
			}
		}
		
		return false;
	}
	
	// (re)allocate the array that stores the board
	private void allocateBoard() {
		mBoard = new int[64];
	}

	private void flipPieces(int player, int x, int y) {
		// Check all eight surrounding squares for an enemy piece. 
		// If one is found check that it followed by zero or more enemy pieces
		// and then a friendly piece.
		
		for ( Point d: sdXY ) {
			int dx = d.x;
			int dy = d.y;
			int xdx = x + dx;
			int ydy = y + dy;
			if ( 0 <= xdx && xdx < 8 && 0 <= ydy && ydy < 8 &&
				 mBoard[xdx * 8 + ydy] == -player) {
				for ( int i = 2; ; i++ ) {
					int xi = x + dx * i;
					int yi = y + dy * i;
					if (!(0 <= xi && xi < 8 && 0 <= yi && yi < 8))
						break;
					int square = mBoard[xi * 8 + yi];
					if (square == player) {
						// Found a chain to flip so flip it
						for ( int j = 1; j < i; j++) {
							int xj = x + dx * j;
							int yj = y + dy * j;
							mBoard[xj * 8 + yj] = player;
							if ( mFlipper != null ) {
								mFlipper.animateFlip(xj, yj);
							}
						}
						break;
					} else if (square == EMPTY)
						break;
				}

			}
		}
	}
	
	private void initialise(BoardAnimator flipper) {
		mFlipper = flipper;
		mMoveCount = 0;
		allocateBoard();
		initialiseBoard();
	}
	
	/**
	 * @return the moveCount
	 */
	public int getMoveCount() {
		return mMoveCount;
	}

	// Set the board to the starting position
	private void initialiseBoard() {
		// Add starting pieces to the board.
		mBoard[3*8 + 3] = WHITE;
		mBoard[3*8 + 4] = BLACK;
		mBoard[4*8 + 3] = BLACK;
		mBoard[4*8 + 4] = WHITE;
	}
}
