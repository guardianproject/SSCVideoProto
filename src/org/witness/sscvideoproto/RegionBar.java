package org.witness.sscvideoproto;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

public class RegionBar extends View {
	
	public static final String LOGTAG = "REGIONBAR";
	
	public static final int COLOR_SELECTED = Color.YELLOW;
	public static final int COLOR_NOT_SELECTED = Color.BLUE;
	
	int backgroundColor = COLOR_NOT_SELECTED;
	
	int width = 0;
	int height = 0;
	
	ObscureRegion or = null;
	
	public RegionBar(Context context) {
		super(context);
		
		this.setBackgroundColor(backgroundColor);
	}
	
    @Override
    protected void onDraw(Canvas canvas) {
    	super.onDraw(canvas);
    	
    	Paint paint = new Paint();
        paint.setTextSize(12);
        paint.setColor(0xFF668800);
        paint.setStyle(Paint.Style.FILL);

        //canvas.drawRect(left, top, right, bottom, paint);
        //(float)startTime/(float)1000 + "," + (float)endTime/(float)1000
        //canvas.drawRect(10,10,width-10,height-10,paint);
        
        canvas.drawText("TEST", 100, 100, paint);
        
        //Log.v(LOGTAG,"Just drew " + width + " " + height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	width = widthMeasureSpec;
    	height = heightMeasureSpec;
    	
        this.setMeasuredDimension(widthMeasureSpec,heightMeasureSpec);     
    }	
}
