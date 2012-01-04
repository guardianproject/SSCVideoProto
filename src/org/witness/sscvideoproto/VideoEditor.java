/*
 * 
 * To Do:
 * 
 * Handles on SeekBar - Not quite right
 * Editing region, not quite right
 * RegionBarArea will be graphical display of region tracks, no editing, just selecting
 * 
 */

package org.witness.sscvideoproto;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.witness.sscvideoproto.InOutPlayheadSeekBar.InOutPlayheadSeekBarChangeListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.LinearLayout.LayoutParams;

public class VideoEditor extends Activity implements
						OnCompletionListener, OnErrorListener, OnInfoListener,
						OnBufferingUpdateListener, OnPreparedListener, OnSeekCompleteListener,
						OnVideoSizeChangedListener, SurfaceHolder.Callback,
						MediaController.MediaPlayerControl, OnTouchListener, OnClickListener,
						InOutPlayheadSeekBarChangeListener {

	public static final String LOGTAG = "VIDEOEDITOR";
	public static final String PACKAGENAME = "org.witness.sscvideoproto";

	public static final int SHARE = 1;

    private final static float REGION_CORNER_SIZE = 26;
	
	ProgressDialog progressDialog;

	Uri originalVideoUri;
	
	File savePath;
	File saveFile;
	File recordingFile;
	
	Display currentDisplay;

	SurfaceView surfaceView;
	SurfaceHolder surfaceHolder;
	MediaPlayer mediaPlayer;	
	
	ImageView regionsView;
	Bitmap obscuredBmp;
    Canvas obscuredCanvas;
	Paint obscuredPaint;
	Paint selectedPaint;
	
	Bitmap bitmapCornerUL;
	Bitmap bitmapCornerUR;
	Bitmap bitmapCornerLL;
	Bitmap bitmapCornerLR;
	
	InOutPlayheadSeekBar progressBar;
	RegionBarArea regionBarArea;
	
	int videoWidth = 0;
	int videoHeight = 0;
	
	ImageButton playPauseButton;
	
	private Vector<ObscureRegion> obscureRegions = new Vector<ObscureRegion>();
	private ObscureRegion tempRegion;
	
	ProcessVideo processVideo;

	FFMPEGWrapper ffmpeg;
	File redactSettingsFile;
	
	private Handler mHandler = new Handler();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.videoeditor);
		
		regionsView = (ImageView) this.findViewById(R.id.VideoEditorImageView);
			
		createCleanSavePath();

		// Passed in from ObscuraApp
		originalVideoUri = getIntent().getData();
		recordingFile = new File(pullPathFromUri(originalVideoUri));

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
		
		progressBar = (InOutPlayheadSeekBar) this.findViewById(R.id.InOutPlayheadSeekBar);

		progressBar.setIndeterminate(false);
		progressBar.setSecondaryProgress(0);
		progressBar.setProgress(0);
		progressBar.setInOutPlayheadSeekBarChangeListener(this);
		progressBar.setThumbsInactive();
		progressBar.setOnTouchListener(this);

		playPauseButton = (ImageButton) this.findViewById(R.id.PlayPauseImageButton);
		playPauseButton.setOnClickListener(this);
		
		currentDisplay = getWindowManager().getDefaultDisplay();
				
		redactSettingsFile = new File(Environment.getExternalStorageDirectory().getPath()+"/"+PACKAGENAME+"/redact_unsort.txt");
		ffmpeg = new FFMPEGWrapper(this.getBaseContext());
		
		regionBarArea = (RegionBarArea) this.findViewById(R.id.RegionBarArea);
		regionBarArea.obscureRegions = obscureRegions;
		
		obscuredPaint = new Paint();   
        obscuredPaint.setColor(Color.WHITE);
	    obscuredPaint.setStyle(Style.STROKE);
	    obscuredPaint.setStrokeWidth(10f);
	    
	    selectedPaint = new Paint();
	    selectedPaint.setColor(Color.GREEN);
	    selectedPaint.setStyle(Style.STROKE);
	    selectedPaint.setStrokeWidth(10f);
	    
		bitmapCornerUL = BitmapFactory.decodeResource(getResources(), R.drawable.edit_region_corner_ul);
		bitmapCornerUR = BitmapFactory.decodeResource(getResources(), R.drawable.edit_region_corner_ur);
		bitmapCornerLL = BitmapFactory.decodeResource(getResources(), R.drawable.edit_region_corner_ll);
		bitmapCornerLR = BitmapFactory.decodeResource(getResources(), R.drawable.edit_region_corner_lr);
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
		} else if (whatInfo == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) { 
			Log.v(LOGTAG, "MediaInfo, Media Info Metadata Update " + extra); 
		}
		
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
		
		mHandler.postDelayed(updatePlayProgress, 100);		
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
	
	private Runnable updatePlayProgress = new Runnable() {
	   public void run() {
		   if (mediaPlayer != null) {
			   if (mediaPlayer.isPlaying()) {
				   progressBar.setProgress((int)(((float)mediaPlayer.getCurrentPosition()/(float)mediaPlayer.getDuration())*100));
			   }   
			   updateRegionDisplay();
		   }
		   mHandler.postDelayed(this, 100);
	   }
	};		
	
	private void updateRegionDisplay() {

		//Log.v(LOGTAG,"Position: " + mediaPlayer.getCurrentPosition());
		
		validateRegionView();
		clearRects();
				
		for (ObscureRegion region:obscureRegions) {
			if (region.existsInTime(mediaPlayer.getCurrentPosition())) {
				// Draw this region
				//Log.v(LOGTAG,mediaPlayer.getCurrentPosition() + " Drawing a region: " + region.getBounds().left + " " + region.getBounds().top + " " + region.getBounds().right + " " + region.getBounds().bottom);
				if (region != tempRegion) {
					displayRegion(region,false);
				}
			}
		}
		
		if (tempRegion != null && tempRegion.existsInTime(mediaPlayer.getCurrentPosition())) {
			displayRegion(tempRegion,true);
			//displayRect(tempRegion.getBounds(), selectedPaint);
		}
		
		regionsView.invalidate();
		//seekBar.invalidate();
	}
	
	private void validateRegionView() {
		if (obscuredBmp == null) {
			Log.v(LOGTAG,"obscuredBmp is null, creating it now");
			obscuredBmp = Bitmap.createBitmap(regionsView.getWidth(), regionsView.getHeight(), Bitmap.Config.ARGB_8888);
			obscuredCanvas = new Canvas(obscuredBmp); 
		    regionsView.setImageBitmap(obscuredBmp);			
		}
	}
	
	private void displayRegion(ObscureRegion region, boolean selected) {
					    	    	
    	if (selected) {

    		RectF paintingRect = new RectF();
        	paintingRect.set(region.getBounds());
        	paintingRect.inset(10,10);
        	
        	obscuredPaint.setStrokeWidth(10f);
    		obscuredPaint.setColor(Color.GREEN);
        	
    		obscuredCanvas.drawRect(paintingRect, obscuredPaint);
    		
        	obscuredCanvas.drawBitmap(bitmapCornerUL, paintingRect.left-REGION_CORNER_SIZE, paintingRect.top-REGION_CORNER_SIZE, obscuredPaint);
    		obscuredCanvas.drawBitmap(bitmapCornerLL, paintingRect.left-REGION_CORNER_SIZE, paintingRect.bottom-(REGION_CORNER_SIZE/2), obscuredPaint);
    		obscuredCanvas.drawBitmap(bitmapCornerUR, paintingRect.right-(REGION_CORNER_SIZE/2), paintingRect.top-REGION_CORNER_SIZE, obscuredPaint);
    		obscuredCanvas.drawBitmap(bitmapCornerLR, paintingRect.right-(REGION_CORNER_SIZE/2), paintingRect.bottom-(REGION_CORNER_SIZE/2), obscuredPaint);
    	    
    	} else {
    		obscuredPaint.setColor(Color.YELLOW);
    		obscuredCanvas.drawRect(region.getBounds(), obscuredPaint);
    	}
	}
	
	private void clearRects() {
		Paint clearPaint = new Paint();
		clearPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
		obscuredCanvas.drawPaint(clearPaint);
	}

	int currentNumFingers = 0;
	int regionCornerMode = 0;
	
	public static final int NONE = 0;
	public static final int DRAG = 1;
	//int mode = NONE;

	public ObscureRegion findRegion(MotionEvent event) 
	{
		ObscureRegion returnRegion = null;
		
		for (ObscureRegion region : obscureRegions)
		{
			if (region.getBounds().contains(event.getX(),event.getY()))
			{
				returnRegion = region;
				break;
			}
		}			
		return returnRegion;
	}
	
	/*
	long startTime = 0;
	float startX = 0;
	float startY = 0;
	*/

	boolean showMenu = false;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		boolean handled = false;

		if (v == progressBar) {
			// It's the progress bar/scrubber
			
			Log.v(LOGTAG,"" + event.getX() + " " + event.getX()/progressBar.getWidth());
			Log.v(LOGTAG,"Seeking To: " + (int)(mediaPlayer.getDuration()*(float)(event.getX()/progressBar.getWidth())));
			mediaPlayer.seekTo((int)(mediaPlayer.getDuration()*(float)(event.getX()/progressBar.getWidth())));
			Log.v(LOGTAG,"MediaPlayer Position: " + mediaPlayer.getCurrentPosition());
			// Attempt to get the player to update it's view - NOT WORKING
			if (!mediaPlayer.isPlaying()) {
				mediaPlayer.start();
				mediaPlayer.pause();
			}
			handled = false; // The progress bar doesn't get it if we have true here
		}
		else
		{
			// Region Related
			float x = event.getX()/(float)currentDisplay.getWidth() * videoWidth;
			float y = event.getY()/(float)currentDisplay.getHeight() * videoHeight;

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:

					// Single Finger down
					currentNumFingers = 1;
					
					// If we have a region in creation/editing and we touch within it
					if (tempRegion != null && tempRegion.getRectF().contains(x, y)) {

						// Should display menu, unless they move
						showMenu = true;
						
						// Are we on a corner?
						regionCornerMode = getRegionCornerMode(tempRegion, x, y);
						
						Log.v(LOGTAG,"Touched tempRegion");
																		
					} else {
					
						showMenu = false;
						
						tempRegion = findRegion(event);
						
						if (tempRegion != null)
						{
							// Display menu unless they move
							showMenu = true;
							
							// Are we on a corner?
							regionCornerMode = getRegionCornerMode(tempRegion, x, y);
							
							// Show in and out points
							progressBar.setThumbsActive((int)(tempRegion.startTime/mediaPlayer.getDuration()*100), (int)(tempRegion.endTime/mediaPlayer.getDuration()*100));
							
							// They are interacting with the active region
							Log.v(LOGTAG,"Touched an existing region");
						}
						else 
						{
							
							tempRegion = new ObscureRegion(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(),mediaPlayer.getDuration(),x,y);
							obscureRegions.add(tempRegion);
							
							Log.v(LOGTAG,"Creating a new tempRegion");
							
							Log.v(LOGTAG,"startTime: " + tempRegion.startTime + " duration: " + mediaPlayer.getDuration() + " math startTime/duration*100: " + (int)((float)tempRegion.startTime/(float)mediaPlayer.getDuration()*100));
							
							// Show in and out points
							progressBar.setThumbsActive((int)((double)tempRegion.startTime/(double)mediaPlayer.getDuration()*100), (int)((double)tempRegion.endTime/(double)mediaPlayer.getDuration()*100));

						}
					}

					handled = true;

					break;
					
				case MotionEvent.ACTION_UP:
					// Single Finger Up
					currentNumFingers = 0;
					
					if (showMenu) {
						
						Log.v(LOGTAG,"Touch Up: Show Menu - Really finalizing tempRegion");
						
						// Should show the menu, stopping region for now
						tempRegion.endTime = mediaPlayer.getCurrentPosition();
						
						tempRegion = null;
						
						// Hide in and out points
						progressBar.setThumbsInactive();
						
						showMenu = false;
					}
					
					break;
										
				case MotionEvent.ACTION_MOVE:
					// Calculate distance moved
					showMenu = false;
					
					if (tempRegion != null && mediaPlayer.getCurrentPosition() > tempRegion.startTime) {
						Log.v(LOGTAG,"Moving a tempRegion");
						
						long previousEndTime = tempRegion.endTime;
						tempRegion.endTime = mediaPlayer.getCurrentPosition();
						
						ObscureRegion lastRegion = tempRegion;
						tempRegion = null;
						
						if (regionCornerMode != CORNER_NONE) {
				
							//moveRegion(float _sx, float _sy, float _ex, float _ey)
							// Create new region with moved coordinates
							if (regionCornerMode == CORNER_UPPER_LEFT) {
								tempRegion = new ObscureRegion(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(),previousEndTime,x,y,lastRegion.ex,lastRegion.ey);
								obscureRegions.add(tempRegion);
							} else if (regionCornerMode == CORNER_LOWER_LEFT) {
								tempRegion = new ObscureRegion(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(),previousEndTime,x,lastRegion.sy,lastRegion.ex,y);
								obscureRegions.add(tempRegion);
							} else if (regionCornerMode == CORNER_UPPER_RIGHT) {
								tempRegion = new ObscureRegion(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(),previousEndTime,lastRegion.sx,y,x,lastRegion.ey);
								obscureRegions.add(tempRegion);
							} else if (regionCornerMode == CORNER_LOWER_RIGHT) {
								tempRegion = new ObscureRegion(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(),previousEndTime,lastRegion.sx,lastRegion.sy,x,y);
								obscureRegions.add(tempRegion);
							}
						} else {		
							// No Corner
							tempRegion = new ObscureRegion(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(),previousEndTime,x,y);
							obscureRegions.add(tempRegion);
						}
						
						if (tempRegion != null) {
							// Show in and out points
							progressBar.setThumbsActive((int)(tempRegion.startTime/mediaPlayer.getDuration()*100), (int)(tempRegion.endTime/mediaPlayer.getDuration()*100));
						}
						
					} else if (tempRegion != null) {
						Log.v(LOGTAG,"Moving tempRegion start time");
						
						if (regionCornerMode != CORNER_NONE) {
							
							// Just move region, we are at begin time
							if (regionCornerMode == CORNER_UPPER_LEFT) {
								tempRegion.moveRegion(x,y,tempRegion.ex,tempRegion.ey);
							} else if (regionCornerMode == CORNER_LOWER_LEFT) {
								tempRegion.moveRegion(x,tempRegion.sy,tempRegion.ex,y);
							} else if (regionCornerMode == CORNER_UPPER_RIGHT) {
								tempRegion.moveRegion(tempRegion.sx,y,x,tempRegion.ey);
							} else if (regionCornerMode == CORNER_LOWER_RIGHT) {
								tempRegion.moveRegion(tempRegion.sx,tempRegion.sy,x,y);
							}
						} else {		
							// No Corner
							tempRegion.moveRegion(x, y);
						}
						
						// Show in and out points
						progressBar.setThumbsActive((int)(tempRegion.startTime/mediaPlayer.getDuration()*100), (int)(tempRegion.endTime/mediaPlayer.getDuration()*100));

					}
					
					handled = true;
					break;
			}
		}
		return handled; // indicate event was handled	
	}
	

	public static final int CORNER_NONE = 0;
	public static final int CORNER_UPPER_LEFT = 1;
	public static final int CORNER_LOWER_LEFT = 2;
	public static final int CORNER_UPPER_RIGHT = 3;
	public static final int CORNER_LOWER_RIGHT = 4;
	
	public int getRegionCornerMode(ObscureRegion region, float x, float y)
	{    			
    	if (Math.abs(region.getBounds().left-x)<REGION_CORNER_SIZE
    			&& Math.abs(region.getBounds().top-y)<REGION_CORNER_SIZE)
    	{
    		Log.v(LOGTAG,"CORNER_UPPER_LEFT");
    		return CORNER_UPPER_LEFT;
    	}
    	else if (Math.abs(region.getBounds().left-x)<REGION_CORNER_SIZE
    			&& Math.abs(region.getBounds().bottom-y)<REGION_CORNER_SIZE)
    	{
    		Log.v(LOGTAG,"CORNER_LOWER_LEFT");
    		return CORNER_LOWER_LEFT;
		}
    	else if (Math.abs(region.getBounds().right-x)<REGION_CORNER_SIZE
    			&& Math.abs(region.getBounds().top-y)<REGION_CORNER_SIZE)
    	{
        		Log.v(LOGTAG,"CORNER_UPPER_RIGHT");
    			return CORNER_UPPER_RIGHT;
		}
    	else if (Math.abs(region.getBounds().right-x)<REGION_CORNER_SIZE
        			&& Math.abs(region.getBounds().bottom-y)<REGION_CORNER_SIZE)
    	{
    		Log.v(LOGTAG,"CORNER_LOWER_RIGHT");
    		return CORNER_LOWER_RIGHT;
    	}
    	
		Log.v(LOGTAG,"CORNER_NONE");    	
    	return CORNER_NONE;
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
	
	private void createCleanSavePath() {
		savePath = new File(Environment.getExternalStorageDirectory().getPath() + "/"+PACKAGENAME+"/");
		savePath.mkdirs();
		
		Log.v(LOGTAG,"savePath:" + savePath.getPath());
		if (savePath.exists()) {
			Log.v(LOGTAG,"savePath exists!");
		} else {
			Log.v(LOGTAG,"savePath DOES NOT exist!");
		}
		
		try {
			saveFile = File.createTempFile("output", ".mp4", savePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public final static int PLAY = 1;
	public final static int STOP = 2;
	public final static int PROCESS = 3;
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		
		String processString = "Process Video";
		
    	MenuItem processMenuItem = menu.add(Menu.NONE, PROCESS, Menu.NONE, processString);
    	processMenuItem.setIcon(R.drawable.ic_menu_about);
    	
    	return true;
	}
	
    public boolean onOptionsItemSelected(MenuItem item) {	
        switch (item.getItemId()) {
        	case PROCESS:
        		processVideo();
        		return true;
        		
        	default:
        		
        		return false;
        }
    }

    private void processVideo() {
    	
    	progressDialog = ProgressDialog.show(this, "", "Processing. Please wait...", true);
    	
		// Convert to video
		processVideo = new ProcessVideo();
		processVideo.execute();
    }
    
	private class ProcessVideo extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {	

			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
			wl.acquire();
	        
			// Could make some high/low quality presets	
			ffmpeg.processVideo(redactSettingsFile, obscureRegions, recordingFile, saveFile, mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight(), 15, 300);
	        
	        wl.release();
		     
	        return null;
		}
		
		@Override
	    protected void onProgressUpdate(Integer... progress) {
			Log.v(LOGTAG,"Progress: " + progress[0]);
	    }
		
		@Override
	    protected void onPostExecute(Void result) {
	    	 Log.v(LOGTAG,"***ON POST EXECUTE***");
	    	 showPlayShareDialog();
	     }
	}
	
	private void showPlayShareDialog() {
		progressDialog.cancel();

		AlertDialog.Builder builder = new AlertDialog.Builder(VideoEditor.this);
		builder.setMessage("Play or Share?")
			.setCancelable(true)
			.setPositiveButton("Play", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					playVideo();
				}
			})
			.setNegativeButton("Share", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	            	shareVideo();
	            }
		    });
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void playVideo() {
    	Intent intent = new Intent(android.content.Intent.ACTION_VIEW); 
   	 	Uri data = Uri.parse(saveFile.getPath());
   	 	intent.setDataAndType(data, "video/mp4"); 
   	 	startActivityForResult(intent,PLAY);
	}
	
	private void shareVideo() {
    	Intent share = new Intent(Intent.ACTION_SEND);
    	share.setType("video/mp4");
    	share.putExtra(Intent.EXTRA_STREAM, Uri.parse(saveFile.getPath()));
    	startActivityForResult(Intent.createChooser(share, "Share Video"),SHARE);     
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		showPlayShareDialog();
	}

	@Override
	public void inOutValuesChanged(int thumbInValue, int thumbOutValue) {
		if (tempRegion != null) {
			tempRegion.startTime = thumbInValue;
			tempRegion.endTime = thumbOutValue;
		}
	}	
}
