package com.krisapps.reversi;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Stack;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

public class ReversiActivity extends Activity implements OnTouchListener {
	public final static String TAG = "Reversi";
	private final static int DIALOG_HELP = 1;
	private final static int DIALOG_DIFFICULTY = 2;
	/*private final static int DIALOG_LICENSE = 3;*/
	private String SAVE_GAME_FILENAME = "reversi_game";
	private View mTopView;
	private BoardView mBoardView;
	private TextView mStatusView;
	private TextView mLastMoveView;
	private TextView mScoreView;
	private ProgressBar mProgress;
	/*private int mProgressStatus;
*/
	private SeekBar mLevelBar;
	private SeekBar mTimeBar;
	
	
    private Board mBoard;
    Stack<Board> mBoardHistory = new Stack<Board>();
    ArrayList<Point> mMoveHistory = new ArrayList<Point>();
    private AI mAI = new AI(1, 2);
    private int mHumanPlayer;
    private final static long sPause = 2000; // Minimum think time is 2s
    private String mStatusText;
    
    private boolean mThinking = false;
    private boolean mNewGamePending = false;
    private boolean mSwapPlayerPending = false;
    private boolean mUndoPending = false;
    private boolean mCreateAIPending = false;
    private int mPendingAILevel;
    private int mPendingAILimit;
    
    private final static int MESSAGE_PROGRESS = 1;
    private final static int MESSAGE_DRAW = 2;
    
    /*private TextView mCheckLicenseText;
    private Button mCheckLicenseButton;
    private LicenseCheckerCallback mLicenseCheckerCallback;
    private LicenseChecker mChecker;
    private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApUjlRNHVrS7bAsLPFVVbdh/bLI1hUFyqbJaBnmNDakb+NQJaYKktfB63fWmGuQUSj1Q0QpNpNuKutrR87IlBTFtEW77lb+3Q4mRZSgR+H2OWVRWcCinLlRrP3A2Nl5s2nvfM5ifWoYc05sf2q+KMKdjgd7a4OC02g6zsCgUpSY78rl9Cy97DUb2PNwY0VueJ232pNvQUUoZwHf+rPpcqJqlTaeNeeBfTsQGnh+FhgBIf5PaOsVv7hrtko+52t8Gv/k1oDCenlBGLyOzdyYNrih8h4/229+zmj6H9UxGI+SqbOmqlR1amSMg316UlJOhxX8/Kzo9c0f/doxIJwrm/VQIDAQAB";
    private static final byte[] SALT;
    private boolean mAllowed;
    
    static {
    	Random random = new Random();
    	
    	SALT = new byte[20];
    	random.nextBytes(SALT);
    }
*/
    /*private class CheckerCallback implements LicenseCheckerCallback {
        public void allow() {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            
            mAllowed = true;
            Log.i(TAG, "CheckerCallback.allow");
        }

        public void dontAllow() {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            
            Log.i(TAG, "CheckerCallback.dontAllow");
            mAllowed = false;
            showDialog(DIALOG_LICENSE);
        }

        public void applicationError(ApplicationErrorCode errorCode) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            
            mAllowed = true;
            Log.i(TAG, "CheckerCallback.applicationError " + errorCode);
        }
    }
*/
        private Handler.Callback mCallback = new Handler.Callback() {
        	public boolean handleMessage (Message msg) {
    		switch ( msg.what ) {
    		case MESSAGE_PROGRESS:
        		mProgress.setVisibility(View.VISIBLE);
                mProgress.setProgress(msg.arg1);
        	return true;
    		case MESSAGE_DRAW:
    			drawAndroidMove((Point)msg.obj);
    			return true;
    		}
    		
    		return false;
    	}
    };
    
    private Handler mHandler = new Handler(mCallback);
    
    private ProgressUpdater mProgressUpdater = new ProgressUpdater() {
    	public void updateProgress(int status) {
    		Message msg = Message.obtain(mHandler, MESSAGE_PROGRESS, status, 0);
    		mHandler.sendMessage(msg);
        }
    };
    
    private Runnable mThinker = new Runnable() {
    	public void run() {
    		long startTime = System.currentTimeMillis();
    		Point androidMove = mAI.chooseMove(mBoard, -mHumanPlayer, mProgressUpdater);
    		long duration = System.currentTimeMillis() - startTime;
    		
    		// If the AI is too fast, delay the display of the computers move a bit to give the user a chance
    		// to see their move.
    		Message msg = Message.obtain(mHandler, MESSAGE_DRAW,androidMove);
    		
    		if ( duration >= sPause )
    			mHandler.sendMessage(msg);
    		else
    			mHandler.sendMessageDelayed(msg, sPause - duration);
    	}
    };
    
    private void drawAndroidMove(Point androidMove) {
    	// Log.i(TAG, "drawAndroidMove " + androidMove + ", " + mThinking + ", " + this);
    	mProgress.setVisibility(View.INVISIBLE);

    	mThinking = false;

    	// Check for changes that were made during the computer's move. We are in the main thread here so there are
    	// no synchronization issues.
    	if ( mNewGamePending ) {
    		newGame();
    		return;
    	}

    	if ( mSwapPlayerPending ) {
    		swapPlayers();
    		return;
    	}

    	if ( mUndoPending ) {
    		undo();
    		return;
    	}

    	// Log.i(TAG, "drawAndroidMove: " + androidMove + ", " + mBoard);
    	if ( androidMove != null ) {
    		mBoard.takeMove(-mHumanPlayer, androidMove.x, androidMove.y);
    	}

    	if ( mBoard.countLegalMoves(mHumanPlayer) <= 0 ) {
    		updateStatusText(null);
    	} else {
    		updateStatusText(androidMove);
    	}

    	if ( mCreateAIPending ) {
    		createAI(mPendingAILevel, mPendingAILimit);
    	}

    	mBoardView.draw();			
    }
    
    private Runnable mAlertClearer = new Runnable() {
    	public void run() {
    		mStatusView.setText(mStatusText);
    	}
    };
    
    public void onClickHelp(View view) {
		showDialog(DIALOG_HELP);
	}
	
	public void onClickNewGame(View view) {
		newGameDialog();
	}
 
	public void onClickQuit(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.d_quit_question)
		       .setIcon(R.drawable.ic_quit)
		       .setTitle(R.string.d_quit_title)
		       .setCancelable(false)
		       .setPositiveButton(R.string.d_yes, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   ReversiActivity.this.finish();
		        	   dialog.dismiss();
		           }
		       })
		       .setNegativeButton(R.string.d_no, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void onClickUndo(View view) {
		undo();
	}

	/** 
	 * Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// Log.i(TAG, "onCreate " + mThinking);
        super.onCreate(savedInstanceState);
        
        /*String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);*/
        
        /* Check that this user is licensed to use this application */
        /*mLicenseCheckerCallback = new CheckerCallback();
        mChecker = new LicenseChecker(
            this, new ServerManagedPolicy(this,
                new AESObfuscator(SALT, getPackageName(), deviceId)),
            BASE64_PUBLIC_KEY  // Your public licensing key.
            );
        mChecker.checkAccess(mLicenseCheckerCallback);
        */
        setContentView(R.layout.main);
    	mTopView = findViewById(R.id.contentView);
    	mBoardView = (BoardView) findViewById(R.id.boardView);
        mStatusView = (TextView) findViewById(R.id.statusView);
        mLastMoveView = (TextView) findViewById(R.id.lastMoveView);
        mScoreView = (TextView) findViewById(R.id.scoreView);
        mProgress = (ProgressBar) findViewById(R.id.progressView);
        
    	mBoard = new Board(mBoardView);
        
        if ( savedInstanceState == null ) {
        	SharedPreferences settings = getPreferences(MODE_PRIVATE);
        	mBoardView.setShowLegal(settings.getBoolean("show_legal", true));
        	mBoardView.selectPieces(settings.getInt("pieces", R.id.cp_plain));
        	mBoardView.selectBoard(settings.getInt("board", R.id.cb_basic));
        	createAI(settings.getInt("AILevel", 1), settings.getInt("AILimit", 2));
        	mHumanPlayer = settings.getBoolean("player", true) ? Board.WHITE: Board.BLACK;
        	
        	try {
    			FileInputStream fis = openFileInput(SAVE_GAME_FILENAME);
    			ObjectInputStream ois = new ObjectInputStream(fis);
    			mBoard = (Board) ois.readObject();
    			mBoardHistory = (Stack<Board>) ois.readObject();
    			fis.close();
    		} catch (Exception e) {
    			// If Board fails we will start with a blank board and no history
    			// If history fails we won't lose the game just the ability to undo 
    			// Log.i(...);
    		}
    	} else {
        	// Copy the serialized Board so we are guaranteed to have a new instance. This is because there could still
        	// be old threads kicking about that may update the old instance of Board and we don't want them to!
        	try {
				mBoard.copyBoard((Board) savedInstanceState.getSerializable("key_board"));
				mBoardHistory = (Stack<Board>) savedInstanceState.getSerializable("key_history");
			} catch (Exception e) {
    			// If Board fails we will start with a blank board and no history
    			// If history fails we won't lose the game just the ability to undo 
				// Log.i(...);
			}
        
        	mBoard.setAnimator(mBoardView);
        	mBoardView.setShowLegal(savedInstanceState.getBoolean("show_legal", true));
        	mBoardView.selectPieces(savedInstanceState.getInt("pieces", R.id.cp_basic));
        	mBoardView.selectBoard(savedInstanceState.getInt("board", R.id.cb_basic));
        	createAI(savedInstanceState.getInt("AILevel", 1), savedInstanceState.getInt("AILimit", 2));
        	mHumanPlayer = savedInstanceState.getBoolean("player", true) ? Board.WHITE: Board.BLACK;
        }
        
    	updateStatusText(mBoard.getMove());
    	mTopView.setBackgroundColor(mBoardView.getBackgroundColor());
        
        mBoardView.setBoard(mBoard);
        mBoardView.setOnTouchListener(this);
        
        if ( !mBoard.isCurrentPlayer(mHumanPlayer) )
			makeEnemyMove();
		
        mBoardView.draw();		
    }

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    
	    SharedPreferences settings = getPreferences(MODE_PRIVATE);
    	MenuItem item = menu.findItem(R.id.show_legal);
	    item.setChecked(settings.getBoolean("show_legal", true));
    	item.setTitle(item.isChecked() ? R.string.hide_legal: R.string.show_legal);
    	
    	menu.findItem(mBoardView.getSelectedPieces()).setChecked(true);
    	menu.findItem(mBoardView.getSelectedBoard()).setChecked(true);
    	
    	return true;
	}
	
	/**
	 *  (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		int menuId = item.getItemId();
	    switch (menuId) {
	    case R.id.start_new:
	    	newGameDialog();
	    	return true;
	    case R.id.swap_players:
	    	swapPlayersDialog();
	    	return true;
	    case R.id.difficulty:
	    	showDialog(DIALOG_DIFFICULTY);
	    	return true;
	    case R.id.cp_basic:
	    case R.id.cp_plain:
		case R.id.cp_hearts:
		case R.id.cp_tudor:
		case R.id.cp_wwii:
		case R.id.cp_shield:
			mBoardView.selectPieces(menuId);
	    	item.setChecked(!item.isChecked());
	    	return true;
		case R.id.cb_basic:
		case R.id.cb_red:
		case R.id.cb_marble:
			mBoardView.selectBoard(menuId);
			mTopView.setBackgroundColor(mBoardView.getBackgroundColor());
	        item.setChecked(!item.isChecked());
	    	return true;
	    case R.id.show_legal:
	    	showLegal(item);
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	/*private boolean allowed() {
		// allow four moves if unlicensed
		return mAllowed || mBoard.getMoveCount() < 5;
	}
	*/
	/* (non-Javadoc)
	 * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
	 */
	public boolean onTouch(View view, MotionEvent event) {
		int action = event.getAction();
		float xPos = event.getX();
		float yPos = event.getY();
		Point move = mBoardView.getSquare(xPos, yPos);
		
		if ( action == MotionEvent.ACTION_DOWN ) {
			/*if ( !allowed() ) {
				showDialog(DIALOG_LICENSE);
			} else */ if ( mBoard.isLegalMove(mHumanPlayer, move.x, move.y) ) {
				Board oldBoard = new Board();
				oldBoard.copyBoard(mBoard);
				mBoardHistory.push(oldBoard);
				mBoard.takeMove(mHumanPlayer, move.x, move.y);
				updateStatusText(move);
				mBoardView.draw();
				if ( !mBoard.isGameOver() ) {
					makeEnemyMove();
				} else {
					updateStatusText(null);
				}			
			} else {
				if ( mBoard.isCurrentPlayer(mHumanPlayer) )
					updateStatusAlert("You can't go there!" );
				else
					updateStatusAlert("It's not your move!" );
			}
	    }
	
		return true;
	}
	
	private void createAI(int level, int time) {
		if ( !mThinking ) {
			mCreateAIPending = false;
			mAI.setLevel(level);
			mAI.setMaxSeconds(time);
		} else {
			// No need for finish because we're only changing the AI
			mCreateAIPending = true;
			mPendingAILevel = level;
			mPendingAILimit = time;
		}
	}
	
	private	void makeEnemyMove() {
		if ( !mBoard.isGameOver() ) {
			mThinking = true;
    		Thread thinker = new Thread(mThinker);
    		thinker.start();
		}
	}
	
	private void newGame() {
		if ( !mThinking ) {
			mBoard = new Board(mBoardView);
			mBoardView.setBoard(mBoard);
			mBoardHistory.clear();
			updateStatusText(null);
			mBoardView.draw();
			
			mNewGamePending = false;

			if ( !mBoard.isCurrentPlayer(mHumanPlayer) )
				makeEnemyMove();
		} else {
			mAI.finish();
			mNewGamePending = true;
		}
	}

	private void newGameDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.d_new_question)
		       .setIcon(R.drawable.ic_start_new)
               .setTitle(R.string.d_new_title)
               .setCancelable(false)
		       .setPositiveButton(R.string.d_yes, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   ReversiActivity.this.newGame();
		        	   dialog.dismiss();
		           }
		       })
		       .setNegativeButton(R.string.d_no, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void showLegal(MenuItem item) {
		item.setChecked(!item.isChecked());
    	item.setTitle(item.isChecked() ? R.string.hide_legal: R.string.show_legal);
    	mBoardView.setShowLegal(item.isChecked());
    	mBoardView.draw();
    }

	private void swapPlayers() {
		if ( !mThinking ) {
			mHumanPlayer = -mHumanPlayer;
			mSwapPlayerPending = false;
			newGame();
		} else {
			mAI.finish();
			mSwapPlayerPending = true;
		}
	}

	private void swapPlayersDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.d_swap_question)
		       .setIcon(R.drawable.ic_swap_players)
               .setTitle(R.string.d_swap_title)
               .setCancelable(false)
		       .setPositiveButton(R.string.d_yes, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   ReversiActivity.this.swapPlayers();
		        	   dialog.dismiss();
		           }
		       })
		       .setNegativeButton(R.string.d_no, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void undo() {
		if ( !mBoardHistory.empty() ) {
			if ( !mThinking ) {
				mUndoPending = false;
				mBoard = mBoardHistory.pop();
				mBoardView.setBoard(mBoard);
				updateStatusText(mBoard.getMove());

				mBoardView.draw();
			} else {
				mAI.finish();
				mUndoPending = true;
			}
		}
	 }

	private void updateStatusAlert(String message) {
		mStatusView.setText(message);	
		mHandler.postDelayed(mAlertClearer, 1000);	
	}

	private void updateStatusText(Point move) {
		int score = mBoard.score(mHumanPlayer);
		String statusText;
		String lastText;
		String scoreText;	
		
		if ( !mBoard.isGameOver() ) {
			statusText = getString(mBoard.isCurrentPlayer(mHumanPlayer) ? R.string.your_move: R.string.computers_move);
			lastText = ( move == null ) ? "": getString(R.string.last_move, "(" + (move.x  + 1) + "," + (move.y + 1) + ")");
			scoreText =	getString(R.string.current_score, "" + score);
		} else {
			statusText = getString(R.string.game_over);
			if ( score > 0 ) {
				lastText = getString(R.string.you_win); 
				scoreText =	getString(R.string.final_score, "" + score);
			} else if ( score == 0 ) {
				lastText = getString(R.string.draw);
				scoreText =	"";
			} else {
				lastText = getString(R.string.you_lose);
				scoreText =	getString(R.string.final_score, "" + score);
			}
		}

		updateStatusText(statusText, lastText, scoreText);
	}

	private void updateStatusText(String message, String move, String score) {
		// Log.i(TAG, "updateStatusText: " + message + ", " + move + ", " + score + ", " + Thread.currentThread().getId());
		mStatusText = message;
		mStatusView.setText(message);	
		mLastMoveView.setText(move);	
		mScoreView.setText(score);	
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		
		switch ( id ) {
		case DIALOG_HELP: {
			LayoutInflater factory = LayoutInflater.from(this);
            final View contentView = factory.inflate(R.layout.help_dialog, null);
            dialog = new AlertDialog.Builder(ReversiActivity.this)
                .setIcon(R.drawable.ic_help)
                .setTitle(R.string.d_help_title)
                .setView(contentView)
                .setPositiveButton(R.string.b_close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	dismissDialog(DIALOG_HELP);
                    }
                })
                .create();
            
            WebView wv = (WebView) contentView.findViewById(R.id.help_content);
			wv.loadUrl("file:///android_asset/help.html");
			return dialog;
		}
		case DIALOG_DIFFICULTY:	{
            LayoutInflater factory = LayoutInflater.from(this);
            final View contentView = factory.inflate(R.layout.difficulty_dialog, null);
            mLevelBar = (SeekBar) contentView.findViewById(R.id.level_seekbar);
			mLevelBar.setProgress(mAI.getLevel() - 1);
			mTimeBar = (SeekBar) contentView.findViewById(R.id.time_seekbar); 
			mTimeBar.setProgress(mAI.getMaxSeconds() - 2);
			            
            dialog = new AlertDialog.Builder(ReversiActivity.this)
                .setIcon(R.drawable.ic_difficulty)
                .setTitle(R.string.d_difficulty_title)
                .setView(contentView)
                .setPositiveButton(R.string.b_ok, new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int whichButton) {
                    	createAI(mLevelBar.getProgress() + 1, mTimeBar.getProgress() + 2);
    					dismissDialog(DIALOG_DIFFICULTY);
                    }
                })
                .setNegativeButton(R.string.b_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                		dismissDialog(DIALOG_DIFFICULTY);
                    }
                })
                .create();
            
            return dialog;
		}
		/*case DIALOG_LICENSE:
	        return new AlertDialog.Builder(this)
            .setTitle(R.string.unlicensed_dialog_title)
            .setMessage(R.string.unlicensed_dialog_body)
            .setPositiveButton(R.string.buy_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "http://market.android.com/details?id=" + getPackageName()));
                    startActivity(marketIntent);
                }
            })
            .setNegativeButton(R.string.quit_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .create();

*/		default:
			return super.onCreateDialog(id);	
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		// Log.i(TAG, "onSaveInstanceState " + mThinking);
        outState.putSerializable("key_board", mBoard);
		outState.putSerializable("key_history", mBoardHistory);
		outState.putBoolean("show_legal", mBoardView.isShowLegal());
		outState.putInt("pieces", mBoardView.getSelectedPieces());
		outState.putInt("board", mBoardView.getSelectedBoard());
		outState.putInt("AILevel", mAI.getLevel());
		outState.putInt("AILimit", mAI.getMaxSeconds());
		outState.putBoolean("player", (mHumanPlayer == Board.WHITE));
    }
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
	
		editor.putBoolean("show_legal", mBoardView.isShowLegal());
		editor.putInt("AILevel", mAI.getLevel());
		editor.putInt("AILimit", mAI.getMaxSeconds());
		editor.putInt("pieces", mBoardView.getSelectedPieces());
		editor.putInt("board", mBoardView.getSelectedBoard());
		editor.putBoolean("player", (mHumanPlayer == Board.WHITE));

		editor.commit();    	
		
		try {
			FileOutputStream fos = openFileOutput(SAVE_GAME_FILENAME,
					Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(mBoard);
			oos.writeObject(mBoardHistory);
			fos.close();
		} catch (Exception e) {
			// Log.i(...);
		}
	}

}