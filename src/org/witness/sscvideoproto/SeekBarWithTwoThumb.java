/*
 * Experiment with code from: 
 * http://www.quicknews4you.com/2011/05/08/android-seek-bar-with-two-thumb/
 */

package org.witness.sscvideoproto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

public class SeekBarWithTwoThumb extends ImageView {

	private String TAG = this.getClass().getSimpleName();
	private Bitmap thumb = BitmapFactory.decodeResource(getResources(),
			R.drawable.leftthumb);
	private int thumb1X, thumb2X;
	private int thumb1Value, thumb2Value;
	private int thumbY;
	private Paint paint = new Paint();
	private int selectedThumb;
	private int thumbHalfWidth;
	private SeekBarChangeListener scl;
	
	
	public SeekBarWithTwoThumb(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public SeekBarWithTwoThumb(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SeekBarWithTwoThumb(Context context) {
		super(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (getHeight() > 0)
			init();
	}

	private void init() {
		printLog("View Height =" + getHeight() + "\t\t Thumb Height :"
				+ thumb.getHeight());
		if (thumb.getHeight() > getHeight())
			getLayoutParams().height = thumb.getHeight();

		thumbY = (getHeight() / 2) - (thumb.getHeight() / 2);
		printLog("View Height =" + getHeight() + "\t\t Thumb Height :"
				+ thumb.getHeight() + "\t\t" + thumbY);
		
		thumbHalfWidth = thumb.getWidth()/2;
		thumb1X = thumbHalfWidth;
		thumb2X = getWidth()/2 ;
		invalidate();
	}
	public void setSeekBarChangeListener(SeekBarChangeListener scl){
		this.scl = scl;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawBitmap(thumb, thumb1X - thumbHalfWidth, thumbY,
				paint);
		canvas.drawBitmap(thumb, thumb2X - thumbHalfWidth, thumbY,
				paint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int mx = (int) event.getX();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (mx >= thumb1X - thumbHalfWidth
					&& mx <= thumb1X + thumbHalfWidth) {
				selectedThumb = 1;
				printLog("Select Thumb 1");
			} else if (mx >= thumb2X - thumbHalfWidth
					&& mx <= thumb2X + thumbHalfWidth) {
				selectedThumb = 2;
				printLog("Select Thumb 2");
			}
			break;
		case MotionEvent.ACTION_MOVE:
			printLog("Mouse Move : " + selectedThumb);

			if (selectedThumb == 1) {
				thumb1X = mx;
				printLog("Move Thumb 1");
			} else if (selectedThumb == 2) {
				thumb2X = mx;
				printLog("Move Thumb 2");
			}
			break;
		case MotionEvent.ACTION_UP:
			selectedThumb = 0;
			break;
		}
		
		if(thumb1X < 0)
			thumb1X = 0;
		
		if(thumb2X < 0)
			thumb2X = 0;
		
		if(thumb1X > getWidth() )
			thumb1X =getWidth() ;
		
		if(thumb2X > getWidth() )
			thumb2X =getWidth() ;
		
		invalidate();
		if(scl !=null){
			calculateThumbValue();
			scl.SeekBarValueChanged(thumb1Value,thumb2Value);
		}
		return true;
	}
	
	private void calculateThumbValue(){
		thumb1Value = (100*(thumb1X))/(getWidth());
		thumb2Value = (100*(thumb2X))/(getWidth());
	}
	private void printLog(String log){
		Log.i(TAG, log);
	}
	
	interface SeekBarChangeListener{
		void SeekBarValueChanged(int Thumb1Value,int Thumb2Value);
	}
	
	
}
