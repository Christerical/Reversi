/**
 * 
 */
package com.krisapps.reversi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * @author Chris
 *
 */
public class BoardView extends ImageView implements BoardAnimator {
	private class Flipper implements Runnable, BoardAnimator {
		private Handler mHandler = new Handler();
		private int mFlipping[];
		private boolean mActive = false;
		private final long sBeat = 100;
		// Rotation Angles 64, 38, 12, -12, -38, -64 
		private final int sPercent[] = 
			{ 100, 90, 62, 22, -22, -62, -90, -100 }; 
		
		
		public Flipper() {
			mFlipping = new int[64];
			for ( int i = 0; i < 64; i++ ) {
				mFlipping[i] = -1;
			}
		}
		
    	public void animateFlip(int x, int y) {
    		// Animate the flip by scaling and redrawing the piece to make it appear to turn over
    		mFlipping[x * 8 + y] = sPercent.length - 1;
    		if ( !mActive ) {
    			mHandler.postDelayed(this, sBeat);
    			mActive = true;
    		}
    	}
    	
    	public int getPercent(int x, int y) {
    		int flip = mFlipping[x * 8 + y];
    		return sPercent[flip];
    	}
    	
    	public boolean isFlipping(int x, int y) {
    		int flip = mFlipping[x * 8 + y];
    		return flip >= 0;
    	}
    	
    	public void run() {
    		mActive = false;

    		for ( int i = 0; i < 64; i++ ) {
    			if ( mFlipping[i] > 0 ) {
    				if ( --mFlipping[i] >= 0 ) {
    					mActive = true;
    				}
    			}
    		}

    		if ( mActive ) {
    			mHandler.postDelayed(this, sBeat);
    		}
    		
    		invalidate();
    	}
    }
	
	private BoardImages mImages;
	private Board mBoard;
	
	private boolean mShowLegal;

	Flipper mFlipper = new Flipper();

	/**
	 * @param context
	 */
	public BoardView(Context context) {
		super(context);
		mImages = new BoardImages(context);
		setImageBitmap(mImages.getBoard());
	}
	
	/**
	 * @param context
	 * @param attrs
	 */
	public BoardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mImages = new BoardImages(context);
		setImageBitmap(mImages.getBoard());
	}
	
	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public BoardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mImages = new BoardImages(context);
		setImageBitmap(mImages.getBoard());
	}
	
	public final void animateFlip(int x, int y) {
		mFlipper.animateFlip(x, y);
		
	}
	
	public final void draw() {
		invalidate();
	}
	
	public final int getBackgroundColor() {
		return mImages.getBackgroundColor();
	}
    
    public final int getSelectedBoard() {
		return mImages.getSelectedBoard();
	}
    
	public final int getSelectedPieces() {
		return mImages.getSelectedPieces();
	}

	public Point getSquare(float x, float y) {
		Point p = new Point(
				(int) (x * 8 / mImages.getBoard().getWidth()),
				(int) (y * 8 / mImages.getBoard().getHeight()));
		return p;
	}
	
	/**
	 * @return the showLegal
	 */
	public boolean isShowLegal() {
		return mShowLegal;
	}

	public void selectBoard(int menuId) {
		mImages.selectBoard(menuId);
		invalidate();
	}

	
	public void selectPieces(int menuId) {
		mImages.selectPieces(menuId);
		invalidate();
	}
	
	/**
	 * @param board the board to set
	 */
	public void setBoard(Board board) {
		mBoard = board;
	}

	/**
	 * @param mShowLegal the showLegal to set
	 */
	public void setShowLegal(boolean showLegal) {
		mShowLegal = showLegal;
	}

	/* (non-Javadoc)
	 * @see android.widget.ImageView#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		Matrix matrix = new Matrix();
		Bitmap board = mImages.getBoard();
		int h = board.getHeight();
		int w = board.getWidth();
		canvas.drawBitmap(board, matrix, null);

		Bitmap white = mImages.getWhite();
		int wh = white.getHeight();
		int ww = white.getWidth();
		
		Bitmap black = mImages.getBlack();
		int bh = black.getHeight();
		int bw = black.getWidth();
		
		Bitmap legal = (mBoard.getCurrentPlayer() == Board.WHITE) ? 
				mImages.getWhite(): mImages.getBlack();
		int lh = legal.getHeight();
		int lw = legal.getWidth();
		Paint legalPaint = new Paint();
		legalPaint.setAlpha(32);
		
		for ( int x = 0; x < 8; x++ )
			for ( int y = 0; y < 8; y++ ) {
				int yOffset, xOffset, yTile, xTile;
				
				yTile = y * h / 8;
				xTile = x * w / 8;

				// If showLegal is true and this square would be a valid move we show a partially transparent piece
				if ( mShowLegal && mBoard.isLegalMove(x,  y) ) {
					yOffset = (h / 8 - lh) / 2;
					xOffset = (w / 8 - lw) / 2;
					matrix.setTranslate(xTile + xOffset, yTile + yOffset);
					canvas.drawBitmap(legal, matrix, legalPaint);
				}
		
				int player = mBoard.getPlayer(x, y);
				boolean flipping = mFlipper.isFlipping(x, y);
				float flipPercent = 100;
				
				// Select the appropriate piece according to the current player, the contents of the square and
				// where we are during a flip, which changes the piece half way through the flip
				if ( player == Board.WHITE || player == Board.BLACK ) {
					if ( flipping ) {
						// negative flip percents tell us that the flip is less than half way and so is still showing the
						// old piece.
						flipPercent = mFlipper.getPercent(x, y);
						if (flipPercent < 0 ) {
							flipPercent = -flipPercent;
							player = -player;
						}
					}
				
					Bitmap piece;
					int ph, pw;
					
					if ( player == Board.WHITE ) {
						piece = white;
						ph = wh;
						pw = ww;
					} else {
						piece = black;
						ph = bh;
						pw = bw;
					}
				
					yOffset = (h / 8 - ph) / 2;
					xOffset = (w / 8 - pw) / 2;
					
					if ( flipping ) {
						float flip = flipPercent  / 100.0f;
						// Setup a scaling transformation to scale along a 45 degree line to create the illusion of a
						// rotation as the pieces are flipped.
						// This is the equivalent of rotate 45 degrees, scale along x axis and then rotate back 45 degrees.
						// The matrix is set up directly to avoid rounding errors.
						float values[] = { 
								0.5f + flip / 2, -0.5f + flip / 2, pw / 2.0f * (1 - flip), 
								-0.5f + flip / 2, 0.5f + flip / 2, pw / 2.0f * (1 - flip), 
								0.0f, 0.0f, 1.0f 
						};
						
						matrix.setValues(values);
						matrix.postTranslate(xTile + xOffset, yTile + yOffset);
					} else {
						// If there is no flipping it's just a simple translate to put the piece in the correct place.
						matrix.setTranslate(xTile + xOffset, yTile + yOffset);
					}
					
					
					canvas.drawBitmap(piece, matrix, null);
				}
			}
	}

	/* (non-Javadoc)
	 * @see android.widget.ImageView#onMeasure(int, int)
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Select the best size board for the available area and then set it as the ImageView's image
		mImages.selectBoardSize(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
		setImageBitmap(mImages.getBoard());
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
}
