/**
 * 
 */
package com.krisapps.reversi;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * @author Chris
 *
 */
public class BoardImages {
	private Bitmap mBoard;
	private Bitmap mBlack;
	private Bitmap mWhite;
	private Context mCtx;
	private int mSelectedPieces = 0;
	private int mSelectedSize = 0;
	
	private static final int sWhite[][] = {
		{ R.drawable.p30_white_wood, R.drawable.p30_white_plain, R.drawable.p30_white_heart, R.drawable.p30_white_tudor, R.drawable.p30_white_axis, R.drawable.p30_white_shield },
		{ R.drawable.p40_white_wood, R.drawable.p40_white_plain, R.drawable.p40_white_heart, R.drawable.p40_white_tudor, R.drawable.p40_white_axis, R.drawable.p40_white_shield },
		{ R.drawable.p50_white_wood, R.drawable.p50_white_plain, R.drawable.p50_white_heart, R.drawable.p50_white_tudor, R.drawable.p50_white_axis, R.drawable.p50_white_shield },
		{ R.drawable.p60_white_wood, R.drawable.p60_white_plain, R.drawable.p60_white_heart, R.drawable.p60_white_tudor, R.drawable.p60_white_axis, R.drawable.p60_white_shield },
		{ R.drawable.p75_white_wood, R.drawable.p75_white_plain, R.drawable.p75_white_heart, R.drawable.p75_white_tudor, R.drawable.p75_white_axis, R.drawable.p75_white_shield } };
	private static final int sBlack[][] = {
		{ R.drawable.p30_black_wood, R.drawable.p30_black_plain, R.drawable.p30_black_heart, R.drawable.p30_black_tudor, R.drawable.p30_black_axis, R.drawable.p30_black_shield },
		{ R.drawable.p40_black_wood, R.drawable.p40_black_plain, R.drawable.p40_black_heart, R.drawable.p40_black_tudor, R.drawable.p40_black_axis, R.drawable.p40_black_shield },
		{ R.drawable.p50_black_wood, R.drawable.p50_black_plain, R.drawable.p50_black_heart, R.drawable.p50_black_tudor, R.drawable.p50_black_axis, R.drawable.p50_black_shield },
		{ R.drawable.p60_black_wood, R.drawable.p60_black_plain, R.drawable.p60_black_heart, R.drawable.p60_black_tudor, R.drawable.p60_black_axis, R.drawable.p60_black_shield },
		{ R.drawable.p75_black_wood, R.drawable.p75_black_plain, R.drawable.p75_black_heart, R.drawable.p75_black_tudor, R.drawable.p75_black_axis, R.drawable.p75_black_shield } };
	private static final int sPiecesId[] = 
		{ R.id.cp_basic, R.id.cp_plain, R.id.cp_hearts, R.id.cp_tudor, R.id.cp_wwii, R.id.cp_shield };
	private int mSelectedBoard = 0;
	private static final int sBoard[][] = {
		{ R.drawable.p240_board_green, R.drawable.p240_board_marble, R.drawable.p240_board_red },
		{ R.drawable.p320_board_green, R.drawable.p320_board_marble, R.drawable.p320_board_red },
		{ R.drawable.p400_board_green, R.drawable.p400_board_marble, R.drawable.p400_board_red },
		{ R.drawable.p480_board_green, R.drawable.p480_board_marble, R.drawable.p480_board_red },
		{ R.drawable.p600_board_green, R.drawable.p600_board_marble, R.drawable.p600_board_red } };

	private static final int sBoardId[] = 
		{ R.id.cb_basic, R.id.cb_marble, R.id.cb_red };
	private static final int mBackgroundColor[] =
		{ 0xff264c26, 0xff4c4c3e, 0xff4c004c };

	BoardImages(Context ctx) {
		mCtx = ctx;
		selectImages();
	}

	public int getBackgroundColor() {
		return mBackgroundColor[mSelectedBoard];
	}
	
	/**
	 * @return the black piece
	 */
	public Bitmap getBlack() {
		return mBlack;
	}
	
	/**
	 * @return the Board
	 */
	public Bitmap getBoard() {
		return mBoard;
	}
	
	public int getSelectedBoard() {
		return sBoardId[mSelectedBoard];
	}
	
	public int getSelectedPieces() {
		return sPiecesId[mSelectedPieces];
	}
	
	/**
	 * @return the white piece
	 */
	public Bitmap getWhite() {
		return mWhite;
	}
	
	public void selectBoard(int menuId) {
		for ( int i = 0; i < sBoardId.length; i++ ) {
			if ( menuId == sBoardId[i]) {
				mSelectedBoard = i;
			}
		}
		
		selectImages();
	}
	/**
	 * Selects a board and associated set of pieces to best fit the available screen size. 
	 * 
	 * @param w
	 * @param h
	 */
	public void selectBoardSize(int w, int h) {
		int size = ( w < h ) ? w: h;
		Resources res = mCtx.getResources();
		/* If no boards are small enough, mSelectedSize will be negative and give the amount of space available. This will 
		 * then be used to scale down the smallest available board.
		 */
		mSelectedSize = -size;
		for ( int i = 0; i < sBoard.length; i++ ) {
			Bitmap board = BitmapFactory.decodeResource(res, sBoard[i][0]);
			// TODO why don't these values correspond to the actual image size
			int bsize = board.getWidth();
			
			if ( bsize <= size ) {
				mSelectedSize = i;
			} else {
				break;
			}
		}
		
		selectImages();
	}

	public void selectPieces(int menuId) {
		for ( int i = 0; i < sPiecesId.length; i++ ) {
			if ( menuId == sPiecesId[i]) {
				mSelectedPieces = i;
			}
		}
		
		selectImages();
	}
	
	private void selectImages() {
		Resources res = mCtx.getResources();

		if ( mSelectedSize >= 0 ) {
			mBoard = BitmapFactory.decodeResource(res, sBoard[mSelectedSize][mSelectedBoard]);
			mBlack = BitmapFactory.decodeResource(res, sBlack[mSelectedSize][mSelectedPieces]);
			mWhite = BitmapFactory.decodeResource(res, sWhite[mSelectedSize][mSelectedPieces]);
		} else {
			// None of the supplied boards are small enough for this device so we make 
			// the best of a bad job and scale them down
			int pieceSize = -mSelectedSize / 8;
			int boardSize = pieceSize * 8;
			
			mBoard = BitmapFactory.decodeResource(res, sBoard[0][mSelectedBoard]);
			mBlack = BitmapFactory.decodeResource(res, sBlack[0][mSelectedPieces]);
			mWhite = BitmapFactory.decodeResource(res, sWhite[0][mSelectedPieces]);
			
			mBoard = Bitmap.createScaledBitmap(mBoard, boardSize, boardSize, false);
			mBlack = Bitmap.createScaledBitmap(mBlack, pieceSize, pieceSize, false);
			mWhite = Bitmap.createScaledBitmap(mWhite, pieceSize, pieceSize, false);
		}
	}
}
