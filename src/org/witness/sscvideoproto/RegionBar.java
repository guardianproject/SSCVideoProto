package org.witness.sscvideoproto;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class RegionBar extends View {
	
	public static final int COLOR_SELECTED = Color.YELLOW;
	public static final int COLOR_NOT_SELECTED = Color.BLUE;
	
	int backgroundColor = COLOR_NOT_SELECTED;
	
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

        canvas.drawText("TEEEST", 100, 100, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.setMeasuredDimension(150,200);     
    }
	
}
