package com.krisapps.reversi;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

// Button that removes itself if there is not enough room
public class GonerButton extends Button {

	public GonerButton(Context context) {
		super(context);
	}

	public GonerButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GonerButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/* (non-Javadoc)
	 * @see android.widget.TextView#onMeasure(int, int)
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int h = MeasureSpec.getSize(heightMeasureSpec);
		int mh = getSuggestedMinimumHeight(); 
		
		// If there is not enough room for this button remove it!
		
		if ( mh > h ) {
			setVisibility(View.GONE);
		} else {
			setVisibility(View.VISIBLE);
		}
		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

}
