package org.witness.sscvideoproto;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Vector;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.Toast;

public class ObscuraVideoCam extends Activity implements OnTouchListener, OnClickListener, MediaRecorder.OnInfoListener, 
															MediaRecorder.OnErrorListener, SurfaceHolder.Callback, Camera.PreviewCallback 
{ 
	public static final String LOGTAG = "OBSCURAVIDEOCAM";
	public static final String PACKAGENAME = "org.witness.sscvideoproto";
	
	public static final String DEFAULT_COLOR = "black";
	
	public static final float DEFAULT_X_SIZE = 100;
	public static final float DEFAULT_Y_SIZE = 100;
	
	private MediaRecorder recorder;
	private SurfaceHolder holder;
	private CamcorderProfile camcorderProfile;
	private Camera camera;	
	
	// Vector of ObscureRegion objects
	private Vector obscureRegions = new Vector();
	
	boolean recording = false;
	boolean usecamera = true;
	boolean previewRunning = false;
		
	long recordStartTime = 0;
	
	Button recordButton;
	
	Camera.Parameters p;
		
	ProcessVideo processVideo;
	
	File recordingFile;
	File savePath;
	
	File redactSettingsFile;
	PrintWriter redactSettingsPrintWriter;
	
	File overlayImage; 

	String[] libraryAssets = {"ffmpeg"};

	ProgressDialog progressDialog;
	
	private void moveLibraryAssets() {
		
        Process process = null;

        for (int i = 0; i < libraryAssets.length; i++) {
			try {
				InputStream ffmpegInputStream = this.getAssets().open(libraryAssets[i]);
		        FileMover fm = new FileMover(ffmpegInputStream,"/data/data/"+PACKAGENAME+"/" + libraryAssets[i]);
		        fm.moveIt();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
	        try {
	        	String[] args = {"/system/bin/chmod", "777", "/data/data/"+PACKAGENAME+"/" + libraryAssets[i]};
	        	process = new ProcessBuilder(args).start();        	
	        	try {
					process.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	        	process.destroy();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
        }
        
        try {
        	//String[] args = {"/system/bin/chmod", "777", "/data/data/"+PACKAGENAME+"/ffmpeg"};
        	String[] args = {"/system/bin/chmod", "777", "/data/data/"+PACKAGENAME+"/"};
        	process = new ProcessBuilder(args).start();        	
        	try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	process.destroy();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*
    	// Get Some Help
    	String[] ffmpegHelp = {"/data/data/"+PACKAGENAME+"/ffmpeg", "-?"};
    	execProcess(ffmpegHelp);
    	
    	String[] ffmpegMoreHelp = {"/data/data/"+PACKAGENAME+"/ffmpeg", "-filters"};
    	execProcess(ffmpegMoreHelp);

    	String[] ffmpegMoreHelpAgain = {"/data/data/"+PACKAGENAME+"/ffmpeg", "-codecs"};
    	execProcess(ffmpegMoreHelpAgain);
    	
    	String[] ffmpegMoreHelpAgainMore = {"/data/data/"+PACKAGENAME+"/ffmpeg", "-formats"};
    	execProcess(ffmpegMoreHelpAgainMore);
    	// Finish Getting Help
    	 */
	}
	
	private void execProcess(String[] command) {
        try {
	
	    	StringBuilder commandSb = new StringBuilder();
	    	for (int i = 0; i < command.length; i++) {
	    		if (i > 0) {
	    			commandSb.append(" ");
	    		}
	    		commandSb.append(command[i]);
	    	}
	    	Log.v(LOGTAG, commandSb.toString());
	    	Process prrocess = new ProcessBuilder(command).redirectErrorStream(true).start();         	
			
			OutputStream outputStream = prrocess.getOutputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(prrocess.getInputStream()));
	
			String line;
			
			Log.v(LOGTAG,"***Starting Command***");
			while ((line = reader.readLine()) != null)
			{
				Log.v(LOGTAG,"***"+line+"***");
			}
			Log.v(LOGTAG,"***Ending Command***");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void createCleanSavePath() {
		savePath = new File(Environment.getExternalStorageDirectory().getPath() + "/"+PACKAGENAME+"/");
		savePath.mkdirs();
		
		createOverlayImage();
		
		Log.v(LOGTAG,"savePath:" + savePath.getPath());
		if (savePath.exists()) {
			Log.v(LOGTAG,"savePath exists!");
		} else {
			Log.v(LOGTAG,"savePath DOES NOT exist!");
		}
		
		File[] existingFiles = savePath.listFiles();
		if (existingFiles != null) {
			for (int i = 0; i < existingFiles.length; i++) {
				existingFiles[i].delete();
			}
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		moveLibraryAssets();
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	
		camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
	
		setContentView(R.layout.main);
		
		recordButton = (Button) this.findViewById(R.id.RecordButton);
		recordButton.setOnClickListener(this);		
		
		SurfaceView cameraView = (SurfaceView) findViewById(R.id.CameraView);
		holder = cameraView.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			
		cameraView.setOnTouchListener(this);
	}
	
	private void prepareRecorder() {
	    recorder = new MediaRecorder();
		recorder.setPreviewDisplay(holder.getSurface());
		
		if (usecamera) {
			camera.unlock();
			recorder.setCamera(camera);
		}
		
		recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
	
		recorder.setProfile(camcorderProfile);
	
		createCleanSavePath();
		
		try {
			
			// This is all very sloppy
			if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.THREE_GPP) {
				recordingFile = File.createTempFile("videocapture", ".3gp", savePath);
				Log.v(LOGTAG,"Recording at: " + recordingFile.getAbsolutePath());
				recorder.setOutputFile(recordingFile.getAbsolutePath());
			} else if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.MPEG_4) {
	    		recordingFile = File.createTempFile("videocapture", ".mp4", savePath);
				Log.v(LOGTAG,"Recording at: " + recordingFile.getAbsolutePath());
				recorder.setOutputFile(recordingFile.getAbsolutePath());
			} else {
	    		recordingFile = File.createTempFile("videocapture", ".mp4", savePath);
				Log.v(LOGTAG,"Recording at: " + recordingFile.getAbsolutePath());
				recorder.setOutputFile(recordingFile.getAbsolutePath());
			}
		//recorder.setMaxDuration(50000); // 50 seconds
		//recorder.setMaxFileSize(5000000); // Approximately 5 megabytes
		
			recorder.prepare();
		} catch (IOException e) {
			Log.v(LOGTAG,"Couldn't create file");
			e.printStackTrace();
			finish();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			finish();
		}
	}

	public void onClick(View v) {
		if (recording) {
			recorder.stop();
			if (usecamera) {
				try {
					camera.reconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}			
			// recorder.release();
			recording = false;
			Log.v(LOGTAG, "Recording Stopped");

			// Write out the finger data
			try {
				//Environment.getExternalStorageDirectory().getPath() + "/" + PACKAGENAME + "/redact_unsort.txt"
				redactSettingsFile = new File(Environment.getExternalStorageDirectory().getPath()+"/"+PACKAGENAME+"/redact_unsort.txt");
				
				FileWriter redactSettingsFileWriter = new FileWriter(redactSettingsFile);
				redactSettingsPrintWriter = new PrintWriter(redactSettingsFileWriter);
				
				for (int i = 0; i < obscureRegions.size(); i++) {
					ObscureRegion or = (ObscureRegion)obscureRegions.get(i);
					float ftime = (float)or.time/(float)1000;
					float etime = ftime + 1;
					if (i < obscureRegions.size() - 1) {
						etime = (float)((ObscureRegion)obscureRegions.get(i+1)).time/(float)1000;
					}
					//left, right, top, bottom
					String orString = "" + ftime + ","+etime+"," + (int)or.sx + "," + (int)or.ex + "," + (int)or.sy + "," + (int)or.ey + "," + DEFAULT_COLOR;
					Log.v(LOGTAG,"Writing: " + orString);
					redactSettingsPrintWriter.println(orString);
				}
				redactSettingsPrintWriter.flush();
				redactSettingsPrintWriter.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
			
			progressDialog = ProgressDialog.show(ObscuraVideoCam.this, "", 
                    "Loading. Please wait...", true);
			
			// Convert to video
			processVideo = new ProcessVideo();
			processVideo.execute();
							
			// Let's prepareRecorder so we can record again
			//prepareRecorder(); // We lose file name here
		} else {
			recording = true;
			recordStartTime = SystemClock.uptimeMillis();
			recorder.start();
			Log.v(LOGTAG, "Recording Started");
		}
	}
	
	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(LOGTAG, "surfaceCreated");
		
		if (usecamera) {
			camera = Camera.open();
			
			try {
				camera.setPreviewDisplay(holder);
				camera.startPreview();
				previewRunning = true;
			}
			catch (IOException e) {
				Log.e(LOGTAG,e.getMessage());
				e.printStackTrace();
			}	
		}		
		
	}
	
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.v(LOGTAG, "surfaceChanged");
	
		if (!recording && usecamera) {
			if (previewRunning){
				camera.stopPreview();
			}
	
			try {
				Camera.Parameters p = camera.getParameters();
	
				 p.setPreviewSize(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
			     p.setPreviewFrameRate(camcorderProfile.videoFrameRate);
				
				camera.setParameters(p);
				
				camera.setPreviewDisplay(holder);
				camera.startPreview();
				previewRunning = true;
			}
			catch (IOException e) {
				Log.e(LOGTAG,e.getMessage());
				e.printStackTrace();
			}	

			prepareRecorder();	
		}
	}
	
	
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(LOGTAG, "surfaceDestroyed");
		if (recording) {
			recorder.stop();
			recording = false;
		}
		recorder.release();
		if (usecamera) {
			previewRunning = false;
			camera.lock();
			camera.release();
		}
		finish();
	}

	public void onPreviewFrame(byte[] b, Camera c) {
		
	}
	
    @Override
    public void onConfigurationChanged(Configuration conf) 
    {
        super.onConfigurationChanged(conf);
    }	
	    
    private void createOverlayImage() {
		try {
			overlayImage = new File(savePath,"overlay.jpg");
			
	    	Bitmap overlayBitmap = Bitmap.createBitmap(720, 480, Bitmap.Config.RGB_565);
	    	Canvas obscuredCanvas = new Canvas(overlayBitmap);
	    	Paint obscuredPaint = new Paint();   
	    	Matrix obscuredMatrix = new Matrix();
	        
	    	obscuredCanvas.drawOval(new RectF(10,10,100,100), obscuredPaint);
	    	
	    	OutputStream overlayImageFileOS = new FileOutputStream(overlayImage);
			overlayBitmap.compress(CompressFormat.JPEG, 90, overlayImageFileOS);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    }
    
	private class ProcessVideo extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {

			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
			wl.acquire();
	        
			Process ffmpegProcess = null;
	        try {
	        		        					
	        	Log.v(LOGTAG,"In doInBackground:recordingFile: " + recordingFile.getPath());
	        	Log.v(LOGTAG,"In doInBackground:savePath: " + savePath.getPath());
	        		        	
	        	//ffmpeg -v 10 -y -i /sdcard/org.witness.sscvideoproto/videocapture1042744151.mp4 -vcodec libx264 -b 3000k -s 720x480 -r 30 -acodec copy -f mp4 -vf 'redact=/data/data/org.witness.sscvideoproto/redact_unsort.txt' /sdcard/org.witness.sscvideoproto/new.mp4
	        	String[] ffmpegCommand = {"/data/data/"+PACKAGENAME+"/ffmpeg", "-v", "10", "-y", "-i", recordingFile.getPath(), 
    					"-vcodec", "libx264", "-b", "3000k", "-s", "720x480", "-r", "30",
    					"-vf" , "redact=" + Environment.getExternalStorageDirectory().getPath() + "/" + PACKAGENAME + "/redact_unsort.txt",
    					"-acodec", "copy",
    					"-f", "mp4", savePath.getPath()+"/output.mp4"};

	        	/*
	        	String[] ffmpegCommand = {"/data/data/"+PACKAGENAME+"/ffmpeg", "-v", "10", "-y", "-i", recordingFile.getPath(), 
	        					"-vcodec", "libx264", "-b", "3000k", "-vpre", "baseline", "-s", "720x480", "-r", "30",
	        					//"-vf", "drawbox=10:20:200:60:red@0.5",
	        					"-vf" , "\"movie="+ overlayImage.getPath() +" [logo];[in][logo] overlay=0:0 [out]\"",
	        					"-acodec", "copy",
	        					"-f", "mp4", savePath.getPath()+"/output.mp4"};
	        	*/
	        	
	        	StringBuilder ffmpegCommandSb = new StringBuilder();
	        	for (int i = 0; i < ffmpegCommand.length; i++) {
	        		if (i > 0) {
	        			ffmpegCommandSb.append(" ");
	        		}
	        		ffmpegCommandSb.append(ffmpegCommand[i]);
	        	}
	        	Log.v(LOGTAG, ffmpegCommandSb.toString());
				ffmpegProcess = new ProcessBuilder(ffmpegCommand).redirectErrorStream(true).start();         	
				
				OutputStream ffmpegOutStream = ffmpegProcess.getOutputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));

				String line;
				
				int count = 0;
				Log.v(LOGTAG,"***Starting FFMPEG***");
				while ((line = reader.readLine()) != null)
				{
					Log.v(LOGTAG,"***"+line+"***");
					count++;
		            publishProgress(count);
				}
				Log.v(LOGTAG,"***Ending FFMPEG***");
	
	    
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
	        
	        if (ffmpegProcess != null) {
	        	ffmpegProcess.destroy();        
	        }
	        
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

	    	 progressDialog.cancel();
	    	 
	    	 Intent intent = new Intent(android.content.Intent.ACTION_VIEW); 
	    	 Uri data = Uri.parse(savePath.getPath()+"/output.mp4");
	    	 intent.setDataAndType(data, "video/mp4"); 
	    	 startActivity(intent);
	    	 
	    	 /*
        	Intent share = new Intent(Intent.ACTION_SEND);
        	share.setType("video/mp4");
        	share.putExtra(Intent.EXTRA_STREAM, Uri.parse(savePath.getPath()+"/output.mp4"));
        	startActivity(Intent.createChooser(share, "Share Video"));    	
			*/
	     }
	}

	public void onInfo(MediaRecorder mr, int what, int extra) {
		
	}

	public void onError(MediaRecorder mr, int what, int extra) {
		
	}
	
	//PrintWriter redactSettingsPrintWriter
	
	int currentNumFingers = 0;
	public boolean onTouch(View v, MotionEvent event) {
		boolean handled = false;
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				// Single Finger down
				currentNumFingers = 1;
				
				if (recording) {
					ObscureRegion singleFingerRegion = new ObscureRegion(SystemClock.uptimeMillis() - recordStartTime,event.getX(),event.getY());
					obscureRegions.add(singleFingerRegion);
				}
				handled = true;
				
				break;
				
			case MotionEvent.ACTION_POINTER_DOWN:
				// Two Fingers down
				currentNumFingers = 2;
				
				if (recording) {
					ObscureRegion twoFingerRegion = new ObscureRegion(SystemClock.uptimeMillis() - recordStartTime,event.getX(0),event.getY(0),event.getX(1),event.getY(1));
					obscureRegions.add(twoFingerRegion);
				}
				handled = true;
				
				break;
				
			case MotionEvent.ACTION_UP:
				// Single Finger Up
				currentNumFingers = 0;
				
				if (recording) {
					ObscureRegion singleFingerUpRegion = new ObscureRegion(SystemClock.uptimeMillis() - recordStartTime,event.getX(0),event.getY(0),event.getX(0),event.getY(0));
					obscureRegions.add(singleFingerUpRegion);
				}
				
				break;
				
			case MotionEvent.ACTION_POINTER_UP:
				// Multiple Finger Up
				currentNumFingers = 0;
				
				if (recording) {
					ObscureRegion twoFingerUpRegion = new ObscureRegion(SystemClock.uptimeMillis() - recordStartTime,event.getX(0),event.getY(0),event.getX(0),event.getY(0));
					obscureRegions.add(twoFingerUpRegion);
				}
				
				break;
				
			case MotionEvent.ACTION_MOVE:
				// Calculate distance moved
				
				if (recording) {
					if (currentNumFingers == 2) {
						ObscureRegion twoFingerMoveRegion = new ObscureRegion(SystemClock.uptimeMillis() - recordStartTime,event.getX(0),event.getY(0),event.getX(1),event.getY(1));
						obscureRegions.add(twoFingerMoveRegion);
					} else if (currentNumFingers == 1) {
						ObscureRegion oneFingerMoveRegion = new ObscureRegion(SystemClock.uptimeMillis() - recordStartTime,event.getX(0),event.getY(0));
						obscureRegions.add(oneFingerMoveRegion);
					}
				}
				
				handled = true;

				break;
		}

		return handled; // indicate event was handled	
	}	
	
	class ObscureRegion {
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
			sx = _sx - DEFAULT_X_SIZE/2;
			sy = _sy - DEFAULT_Y_SIZE/2;
			ex = sx + DEFAULT_X_SIZE;
			ey = sy + DEFAULT_Y_SIZE;
			
			Log.v(LOGTAG,"new region: " + time + " " + sx + " " + sy + " " + ex + " " + ey);
		}
		
		public RectF getRectF() {
			return new RectF(sx, sy, ex, ey);
		}
	}
}