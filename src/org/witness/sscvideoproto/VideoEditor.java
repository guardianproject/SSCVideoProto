package org.witness.sscvideoproto;

import java.io.IOException;
import java.util.Vector;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.media.MediaPlayer;
import android.util.Log;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;

public class VideoEditor extends Activity implements 
						OnCompletionListener, OnErrorListener, OnInfoListener,
						OnBufferingUpdateListener, OnPreparedListener, OnSeekCompleteListener,
						OnVideoSizeChangedListener, SurfaceHolder.Callback,
						MediaController.MediaPlayerControl, OnTouchListener, OnClickListener {

	public static final String LOGTAG = "VIDEOEDITOR";
	
	Uri originalVideoUri;
	Uri savedVideoUri;
	
	Display currentDisplay;

	SurfaceView surfaceView;
	SurfaceHolder surfaceHolder;
	MediaPlayer mediaPlayer;	
	
	ProgressBar progressBar;

	int videoWidth = 0;
	int videoHeight = 0;
	
	ImageButton playPauseButton;
	ImageButton inPointImageButton;
	ImageButton outPointImageButton;
	
	private Vector obscureRegions = new Vector();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.videoeditor);
			
		// Passed in from ObscuraApp
		originalVideoUri = getIntent().getData();

		surfaceView = (SurfaceView) this.findViewById(R.id.SurfaceView);
		surfaceView.setOnTouchListener(this);
		surfaceHolder = surfaceView.getHolder();

		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnInfoListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnSeekCompleteListener(this);
		mediaPlayer.setOnVideoSizeChangedListener(this);
		mediaPlayer.setOnBufferingUpdateListener(this);

		try {
			mediaPlayer.setDataSource(pullPathFromUri(originalVideoUri));
		} catch (IllegalArgumentException e) {
			Log.v(LOGTAG, e.getMessage());
			finish();
		} catch (IllegalStateException e) {
			Log.v(LOGTAG, e.getMessage());
			finish();
		} catch (IOException e) {
			Log.v(LOGTAG, e.getMessage());
			finish();
		}
		
		progressBar = (ProgressBar) this.findViewById(R.id.ProgressBar);

		//progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progressbar));
		progressBar.setIndeterminate(false);
		progressBar.setSecondaryProgress(0);
		progressBar.setProgress(0);
		progressBar.setOnTouchListener(this);

		playPauseButton = (ImageButton) this.findViewById(R.id.PlayPauseImageButton);
		playPauseButton.setOnClickListener(this);
		
		inPointImageButton = (ImageButton) this.findViewById(R.id.InPointImageButton);
		inPointImageButton.setOnClickListener(this);
		outPointImageButton = (ImageButton) this.findViewById(R.id.OutPointImageButton);
		outPointImageButton.setOnClickListener(this);
		
		currentDisplay = getWindowManager().getDefaultDisplay();
		
		mHandler.postDelayed(updatePlayProgress, 1000);		
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(LOGTAG, "surfaceCreated Called");

		mediaPlayer.setDisplay(holder);

		try {
			mediaPlayer.prepareAsync();
		} catch (IllegalStateException e) {
			Log.v(LOGTAG, "IllegalStateException " + e.getMessage());
			finish();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.v(LOGTAG, "surfaceChanged Called");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(LOGTAG, "surfaceDestroyed Called");
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.v(LOGTAG, "onCompletion Called");
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int whatError, int extra) {
		Log.v(LOGTAG, "onError Called");
		if (whatError == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
			Log.v(LOGTAG, "Media Error, Server Died " + extra);
		} else if (whatError == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
			Log.v(LOGTAG, "Media Error, Error Unknown " + extra);
		}
		return false;
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int whatInfo, int extra) {
		if (whatInfo == MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING) {
			Log.v(LOGTAG, "Media Info, Media Info Bad Interleaving " + extra);
		} else if (whatInfo == MediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
			Log.v(LOGTAG, "Media Info, Media Info Not Seekable " + extra);
		} else if (whatInfo == MediaPlayer.MEDIA_INFO_UNKNOWN) {
			Log.v(LOGTAG, "Media Info, Media Info Unknown " + extra);
		} else if (whatInfo == MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING) {
			Log.v(LOGTAG, "MediaInfo, Media Info Video Track Lagging " + extra);
		} /*
		 * Android version 2.0 or higher else if (whatInfo ==
		 * MediaPlayer.MEDIA_INFO_METADATA_UPDATE) { Log.v(LOGTAG,
		 * "MediaInfo, Media Info Metadata Update " + extra); }
		 */
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.v(LOGTAG, "onPrepared Called");

		videoWidth = mp.getVideoWidth();
		videoHeight = mp.getVideoHeight();

		Log.v(LOGTAG, "Width: " + videoWidth);
		Log.v(LOGTAG, "Height: " + videoHeight);

		if (videoWidth > currentDisplay.getWidth() || videoHeight > currentDisplay.getHeight()) {
			float heightRatio = (float) videoHeight / (float) currentDisplay.getHeight();
			float widthRatio = (float) videoWidth / (float) currentDisplay.getWidth();

			if (heightRatio > 1 || widthRatio > 1) {
				if (heightRatio > widthRatio) {
					videoHeight = (int) Math.ceil((float) videoHeight / (float) heightRatio);
					videoWidth = (int) Math.ceil((float) videoWidth / (float) heightRatio);
				} else {
					videoHeight = (int) Math.ceil((float) videoHeight / (float) widthRatio);
					videoWidth = (int) Math.ceil((float) videoWidth / (float) widthRatio);
				}
			}
		}

		surfaceView.setLayoutParams(new FrameLayout.LayoutParams(videoWidth, videoHeight));
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		Log.v(LOGTAG, "onSeekComplete Called");
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		Log.v(LOGTAG, "onVideoSizeChanged Called");

		videoWidth = mp.getVideoWidth();
		videoHeight = mp.getVideoHeight();

		Log.v(LOGTAG, "Width: " + videoWidth);
		Log.v(LOGTAG, "Height: " + videoHeight);

		if (videoWidth > currentDisplay.getWidth() || videoHeight > currentDisplay.getHeight()) {
			float heightRatio = (float) videoHeight / (float) currentDisplay.getHeight();
			float widthRatio = (float) videoWidth / (float) currentDisplay.getWidth();

			if (heightRatio > 1 || widthRatio > 1) {
				if (heightRatio > widthRatio) {
					videoHeight = (int) Math.ceil((float) videoHeight / (float) heightRatio);
					videoWidth = (int) Math.ceil((float) videoWidth / (float) heightRatio);
				} else {
					videoHeight = (int) Math.ceil((float) videoHeight / (float) widthRatio);
					videoWidth = (int) Math.ceil((float) videoWidth / (float) widthRatio);
				}
			}
		}

		surfaceView.setLayoutParams(new LinearLayout.LayoutParams(videoWidth, videoHeight));
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int bufferedPercent) {
		Log.v(LOGTAG, "MediaPlayer Buffering: " + bufferedPercent + "%");
	}

	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getBufferPercentage() {
		return 0;
	}

	@Override
	public int getCurrentPosition() {
		return mediaPlayer.getCurrentPosition();
	}

	@Override
	public int getDuration() {
		Log.v(LOGTAG,"Calling our getDuration method");
		return mediaPlayer.getDuration();
	}

	@Override
	public boolean isPlaying() {
		Log.v(LOGTAG,"Calling our isPlaying method");
		return mediaPlayer.isPlaying();
	}

	@Override
	public void pause() {
		Log.v(LOGTAG,"Calling our pause method");
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
		}
	}

	@Override
	public void seekTo(int pos) {
		Log.v(LOGTAG,"Calling our seekTo method");
		mediaPlayer.seekTo(pos);
	}

	@Override
	public void start() {
		Log.v(LOGTAG,"Calling our start method");
		mediaPlayer.start();
	}
	
	private Handler mHandler = new Handler();
	private Runnable updatePlayProgress = new Runnable() {
	   public void run() {
		   if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			   progressBar.setProgress((int)(((float)mediaPlayer.getCurrentPosition()/(float)mediaPlayer.getDuration())*100));
		   }   
		   mHandler.postDelayed(this, 1000);
	   }
	};		
	
	/* TOUCH EVENTS FROM ImageEditor.java 
	ImageRegion currRegion = null;
	
	@Override
	public boolean onTouch(View v, MotionEvent event) 
	{
		if (currRegion != null && (mode == DRAG || currRegion.getBounds().contains(event.getX(), event.getY())))		
			return onTouchRegion(v, event, currRegion);	
		else
			return onTouchImage(v,event);
	}
	
	public ImageRegion findRegion (MotionEvent event)
	{
		ImageRegion result = null;
		Matrix iMatrix = new Matrix();
		matrix.invert(iMatrix);

		float[] points = {event.getX(), event.getY()};        	
    	iMatrix.mapPoints(points);
    	
		for (ImageRegion region : imageRegions)
		{

			if (region.getBounds().contains(points[0],points[1]))
			{
				result = region;
				
				break;
			}
			
		}
	
		
		return result;
	}
	
	public boolean onTouchRegion (View v, MotionEvent event, ImageRegion iRegion)
	{
		boolean handled = false;
		
		currRegion.setMatrix(matrix);
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				clearImageRegionsEditMode();
				currRegion.setSelected(true);	
				
				currRegion.setCornerMode(event.getX(),event.getY());
				
				mode = DRAG;
				handled = iRegion.onTouch(v, event);

			break;
			
			case MotionEvent.ACTION_UP:
				mode = NONE;
				handled = iRegion.onTouch(v, event);
				currRegion.setSelected(false);
				//if (handled)
					//currRegion = null;
			
			break;
			
			default:
				mode = DRAG;
				handled = iRegion.onTouch(v, event);
			
		}
		
		return handled;
		
		
	}
	
	public boolean onTouchImage(View v, MotionEvent event) 
	{
		boolean handled = false;
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				// Single Finger down
				mode = TAP;				
				ImageRegion newRegion = findRegion(event);
				
				if (newRegion != null)
				{
					currRegion = newRegion;
					return onTouchRegion(v,  event, currRegion);
				}
				else if (currRegion == null)
				{
					
					// 	Save the Start point. 
					startPoint.set(event.getX(), event.getY());
				}
				else
				{
					currRegion.setSelected(false);
					currRegion = null;

				}
				
				
				break;
				
			case MotionEvent.ACTION_POINTER_DOWN:
				// Two Fingers down

				// Don't do realtime preview while touching screen
				//doRealtimePreview = false;
				//updateDisplayImage();

				// Get the spacing of the fingers, 2 fingers
				float sx = event.getX(0) - event.getX(1);
				float sy = event.getY(0) - event.getY(1);
				startFingerSpacing = (float) Math.sqrt(sx * sx + sy * sy);

				//Log.d(ObscuraApp.TAG, "Start Finger Spacing=" + startFingerSpacing);
				
				// Get the midpoint
				float xsum = event.getX(0) + event.getX(1);
				float ysum = event.getY(0) + event.getY(1);
				startFingerSpacingMidPoint.set(xsum / 2, ysum / 2);
				
				mode = ZOOM;
				//Log.d(ObscuraApp.TAG, "mode=ZOOM");
				
				clearImageRegionsEditMode();
				
				break;
				
			case MotionEvent.ACTION_UP:
				// Single Finger Up
				
			    // Re-enable realtime preview
				doRealtimePreview = true;
				updateDisplayImage();
				
				//debug(ObscuraApp.TAG,"mode=NONE");

				break;
				
			case MotionEvent.ACTION_POINTER_UP:
				// Multiple Finger Up
				
			    // Re-enable realtime preview
				doRealtimePreview = true;
				updateDisplayImage();
				
				//Log.d(ObscuraApp.TAG, "mode=NONE");
				break;
				
			case MotionEvent.ACTION_MOVE:
				
				// Calculate distance moved
				float distance = (float) (Math.sqrt(Math.abs(startPoint.x - event.getX()) + Math.abs(startPoint.y - event.getY())));
				//debug(ObscuraApp.TAG,"Move Distance: " + distance);
				//debug(ObscuraApp.TAG,"Min Distance: " + minMoveDistance);
				
				// If greater than minMoveDistance, it is likely a drag or zoom
				if (distance > minMoveDistance) {
				
					if (mode == TAP || mode == DRAG) {
						mode = DRAG;
						
						matrix.postTranslate(event.getX() - startPoint.x, event.getY() - startPoint.y);
						imageView.setImageMatrix(matrix);
//						// Reset the start point
						startPoint.set(event.getX(), event.getY());
	
						putOnScreen();
						//redrawRegions();
						
						handled = true;
	
					} else if (mode == ZOOM) {
						
						// Save the current matrix so that if zoom goes to big, we can revert
						savedMatrix.set(matrix);
						
						// Get the spacing of the fingers, 2 fingers
						float ex = event.getX(0) - event.getX(1);
						float ey = event.getY(0) - event.getY(1);
						endFingerSpacing = (float) Math.sqrt(ex * ex + ey * ey);
	
						//Log.d(ObscuraApp.TAG, "End Finger Spacing=" + endFingerSpacing);
		
						// If we moved far enough
						if (endFingerSpacing > minMoveDistance) {
							
							// Ratio of spacing..  If it was 5 and goes to 10 the image is 2x as big
							float scale = endFingerSpacing / startFingerSpacing;
							// Scale from the midpoint
							matrix.postScale(scale, scale, startFingerSpacingMidPoint.x, startFingerSpacingMidPoint.y);
		
							// Make sure that the matrix isn't bigger than max scale/zoom
							float[] matrixValues = new float[9];
							matrix.getValues(matrixValues);
							
							if (matrixValues[0] > MAX_SCALE) {
								matrix.set(savedMatrix);
							}
							imageView.setImageMatrix(matrix);
							
							putOnScreen();
							//redrawRegions();
	
							// Reset Start Finger Spacing
							float esx = event.getX(0) - event.getX(1);
							float esy = event.getY(0) - event.getY(1);
							startFingerSpacing = (float) Math.sqrt(esx * esx + esy * esy);
							//Log.d(ObscuraApp.TAG, "New Start Finger Spacing=" + startFingerSpacing);
							
							// Reset the midpoint
							float x_sum = event.getX(0) + event.getX(1);
							float y_sum = event.getY(0) + event.getY(1);
							startFingerSpacingMidPoint.set(x_sum / 2, y_sum / 2);
							
							handled = true;
						}
					}
				}
				break;
		}


		return handled; // indicate event was handled
	}
	*/	

	ObscureRegion currRegion = null;

	int currentNumFingers = 0;
	
	public static final int NONE = 0;
	public static final int DRAG = 1;
	int mode = NONE;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == progressBar) {
			Log.v(LOGTAG,"" + event.getX() + " " + event.getX()/progressBar.getWidth());
			mediaPlayer.seekTo((int)(mediaPlayer.getDuration()*(float)(event.getX()/progressBar.getWidth())));
			return false;
		}
		else if (currRegion != null && (mode == DRAG || currRegion.getBounds().contains(event.getX(), event.getY())))		
		{
			boolean handled = false;
			return handled;
		}
		else 
		{
			boolean handled = false;

			float x = event.getX()/(float)currentDisplay.getWidth() * videoWidth;
			float y = event.getY()/(float)currentDisplay.getHeight() * videoHeight;

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
					// Single Finger down
					currentNumFingers = 1;
					
					ObscureRegion singleFingerRegion = new ObscureRegion(mediaPlayer.getCurrentPosition(),x,x);
					obscureRegions.add(singleFingerRegion);
					handled = true;
					
					break;
					
				case MotionEvent.ACTION_UP:
					// Single Finger Up
					currentNumFingers = 0;
					
						ObscureRegion singleFingerUpRegion = new ObscureRegion(mediaPlayer.getCurrentPosition(),x,y,x,y);
						obscureRegions.add(singleFingerUpRegion);
					
					break;
										
				case MotionEvent.ACTION_MOVE:
					// Calculate distance moved
					
					ObscureRegion oneFingerMoveRegion = new ObscureRegion(mediaPlayer.getCurrentPosition(),x,x);
					obscureRegions.add(oneFingerMoveRegion);
					
					handled = true;

					break;
			}

			return handled; // indicate event was handled	
		}		
	}
	
	@Override
	public void onClick(View v) {
		if (v == playPauseButton) {
			if (mediaPlayer.isPlaying()) {
				mediaPlayer.pause();
			} else {
				mediaPlayer.start();
			}
		}
	}	

	public String pullPathFromUri(Uri originalUri) {
    	String originalVideoFilePath = null;
    	String[] columnsToSelect = { MediaStore.Video.Media.DATA };
    	Cursor videoCursor = getContentResolver().query(originalUri, columnsToSelect, null, null, null );
    	if ( videoCursor != null && videoCursor.getCount() == 1 ) {
	        videoCursor.moveToFirst();
	        originalVideoFilePath = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Images.Media.DATA));
    	}

    	return originalVideoFilePath;
    }
}
