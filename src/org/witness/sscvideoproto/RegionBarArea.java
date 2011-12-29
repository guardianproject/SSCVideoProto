package org.witness.sscvideoproto;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class RegionBarArea extends LinearLayout {

	public RegionBarArea(Context context, AttributeSet attrs) {
		super(context, attrs);

		setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setWeightSum(1.0f);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.seekbar, this, true);
	}	
}
