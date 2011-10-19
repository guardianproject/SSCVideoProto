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

	int currentNumFingers = 0;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == progressBar) {
			Log.v(LOGTAG,"" + event.getX() + " " + event.getX()/progressBar.getWidth());
			mediaPlayer.seekTo((int)(mediaPlayer.getDuration()*(float)(event.getX()/progressBar.getWidth())));
			return false;
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
