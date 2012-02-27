package org.witness.ssc.video;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class ObscuraApp extends Activity implements OnClickListener {
	    
	public final static String LOGTAG = "ObscuraVid";
		
	final static int VIDEO_RESULT = 0;
	final static int GALLERY_RESULT = 1;
	final static int VIDEO_EDITOR = 2;
	
	private Button chooseVideoButton, captureVideoButton;		
	
	private Uri uriVideoResult = null;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.mainmenu);
		
		chooseVideoButton = (Button) this.findViewById(R.id.ChooseVideoButton);
		chooseVideoButton.setOnClickListener(this);
		
		captureVideoButton = (Button) this.findViewById(R.id.CaptureVideoButton);
		captureVideoButton.setOnClickListener(this);
    }
    
	public void onClick(View v) 
	{
		if (v == chooseVideoButton) 
		{
			try
			{
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType("video/*"); //limit to video types for now
				startActivityForResult(intent, GALLERY_RESULT);
			}
			catch (Exception e)
			{
				Toast.makeText(this, "Unable to open Gallery app", Toast.LENGTH_LONG).show();
				Log.e(LOGTAG, "error loading gallery app to choose video: " + e.getMessage(), e);
			}
		} 
		else if (v == captureVideoButton) 
		{
			Intent intent = new Intent("android.media.action.VIDEO_CAMERA");

			//Intent intent = new Intent(this,VideoCam.class);
			startActivityForResult(intent, VIDEO_RESULT);
		} 
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) 
	{
		if (resultCode == RESULT_OK)
		{			
			if (requestCode == GALLERY_RESULT) 
			{
				if (intent != null)
				{
					uriVideoResult = intent.getData();
					Log.v(LOGTAG,"uriVideoResult: " + uriVideoResult.toString());
						
					if (uriVideoResult != null)
					{
						Intent passingIntent = new Intent(this,VideoEditor.class);
						passingIntent.setData(uriVideoResult);
						startActivityForResult(passingIntent,VIDEO_EDITOR);
					}
					else
					{
						Toast.makeText(this, "Unable to load video.", Toast.LENGTH_LONG).show();
					}
				}
				else
				{
					Toast.makeText(this, "Unable to load video.", Toast.LENGTH_LONG).show();
				}
			}
			else if (requestCode == VIDEO_RESULT)
			{
				uriVideoResult = intent.getData();

				if (uriVideoResult != null)
				{
					Intent passingIntent = new Intent(this,VideoEditor.class);
					passingIntent.setData(uriVideoResult);
					startActivityForResult(passingIntent,VIDEO_EDITOR);
				}
			}
		}		
	}	
}
