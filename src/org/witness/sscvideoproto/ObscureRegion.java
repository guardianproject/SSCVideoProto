package org.witness.sscvideoproto;

import android.graphics.RectF;
import android.util.Log;

public class ObscureRegion {

	public static final String LOGTAG = "OBSCUREREGION";

	public static final String DEFAULT_COLOR = "black";

	public static final long DEFAULT_LENGTH = 10; // Seconds
	
	public static final float DEFAULT_X_SIZE = 100;
	public static final float DEFAULT_Y_SIZE = 100;
		
	public float sx = 0;
	public float sy = 0;
	
	public float ex = 0;
	public float ey = 0;
		
	public long startTime = 0;
	public long endTime = 0;
	
	public ObscureRegion(long _startTime, long _endTime, float _sx, float _sy, float _ex, float _ey) {
		startTime = _startTime;
		endTime = _endTime;
		sx = _sx;
		sy = _sy;
		ex = _ex;
		ey = _ey;

		Log.v(LOGTAG,"new region: " + startTime + " " + " " + endTime + " " + sx + " " + sy + " " + ex + " " + ey);
	}

	public ObscureRegion(long _startTime, float _sx, float _sy, float _ex, float _ey) {
		this(_startTime, _startTime+DEFAULT_LENGTH, _sx, _sy, _ex, _ey);
	}

	public ObscureRegion(long _startTime, long _endTime, float _sx, float _sy) {
		this(_startTime, _endTime, _sx - DEFAULT_X_SIZE/2, _sy - DEFAULT_Y_SIZE/2, _sx + DEFAULT_X_SIZE/2, _sy + DEFAULT_Y_SIZE/2);
	}

	public ObscureRegion(long _startTime, float _sx, float _sy) {
		this(_startTime, _startTime+DEFAULT_LENGTH, _sx - DEFAULT_X_SIZE/2, _sy - DEFAULT_Y_SIZE/2, _sx + DEFAULT_X_SIZE/2, _sy + DEFAULT_Y_SIZE/2);
	}
	
	public RectF getRectF() {
		return new RectF(sx, sy, ex, ey);
	}
	
	public RectF getBounds() {
		return getRectF();
	}
	
	public boolean existsInTime(long time) {
		if (time < endTime && time >= startTime) {
			return true;
		}
		return false;
	}

	public String toString() {
		//left, right, top, bottom
		return "" + startTime + "," + endTime + "," + (int)sx + "," + (int)ex + "," + (int)sy + "," + (int)ey + "," + DEFAULT_COLOR;
	}
}