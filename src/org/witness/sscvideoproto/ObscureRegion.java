package org.witness.sscvideoproto;

import android.graphics.RectF;
import android.util.Log;

public class ObscureRegion {

	public static final String LOGTAG = "OBSCUREREGION";

	public static final float DEFAULT_X_SIZE = 100;
	public static final float DEFAULT_Y_SIZE = 100;
	
	private float calcDefaultXSize = DEFAULT_X_SIZE;
	private float calcDefaultYSize = DEFAULT_Y_SIZE;

	// Number of fingers
	public int numFingers = 1;
	
	// Finger 1
	public float sx = 0;
	public float sy = 0;
	
	// Finger 2
	public float ex = 0;
	public float ey = 0;
	
	// Time in ms
	public long time = 0;
	
	public ObscureRegion(long _time, float _sx, float _sy, float _ex, float _ey) {
		time = _time;
		numFingers = 2;
		sx = _sx;
		sy = _sy;
		ex = _ex;
		ey = _ey;

		Log.v(LOGTAG,"new region: " + time + " " + sx + " " + sy + " " + ex + " " + ey);
	}
	
	public ObscureRegion(long _time, float _sx, float _sy) {
		time = _time;
		numFingers = 1;
		sx = _sx - calcDefaultXSize/2;
		sy = _sy - calcDefaultYSize/2;
		ex = sx + calcDefaultXSize;
		ey = sy + calcDefaultYSize;
		
		Log.v(LOGTAG,"new region: " + time + " " + sx + " " + sy + " " + ex + " " + ey);
	}
	
	public RectF getRectF() {
		return new RectF(sx, sy, ex, ey);
	}
}