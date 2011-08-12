package org.witness.sscvideoproto;

import android.app.Activity;
import android.os.Bundle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class ObscuraVideoCam extends Activity implements OnClickListener, MediaRecorder.OnInfoListener, 
MediaRecorder.OnErrorListener, SurfaceHolder.Callback, Camera.PreviewCallback 
{ 

	public static final String LOGTAG = "OBSCURAVIDEOCAM";
	public static final String PACKAGENAME = "org.witness.sscvideoproto";
	
	private MediaRecorder recorder;
	private SurfaceHolder holder;
	private CamcorderProfile camcorderProfile;
	private Camera camera;	
	
	boolean recording = false;
	boolean usecamera = true;
	boolean previewRunning = false;

	Button recordButton;
	
	Camera.Parameters p;
		
	ProcessVideo processVideo;
	
	File recordingFile;
	File savePath;
		
	String[] libraryAssets = {"ffmpeg",
			"libavcodec.so", "libavcodec.so.52", "libavcodec.so.52.99.1",
			"libavcore.so", "libavcore.so.0", "libavcore.so.0.16.0",
			"libavdevice.so", "libavdevice.so.52", "libavdevice.so.52.2.2",
			"libavfilter.so", "libavfilter.so.1", "libavfilter.so.1.69.0",
			"libavformat.so", "libavformat.so.52", "libavformat.so.52.88.0",
			"libavutil.so", "libavutil.so.50", "libavutil.so.50.34.0",
			"libswscale.so", "libswscale.so.0", "libswscale.so.0.12.0"
	};

	private void moveLibraryAssets() {
        for (int i = 0; i < libraryAssets.length; i++) {
			try {
				InputStream ffmpegInputStream = this.getAssets().open(libraryAssets[i]);
		        FileMover fm = new FileMover(ffmpegInputStream,"/data/data/"+PACKAGENAME+"/" + libraryAssets[i]);
		        fm.moveIt();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        
        Process process = null;
        
        try {
        	String[] args = {"/system/bin/chmod", "755", "/data/data/"+PACKAGENAME+"/ffmpeg"};
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
	
	private void createCleanSavePath() {
		savePath = new File(Environment.getExternalStorageDirectory().getPath() + "/"+PACKAGENAME+"/");
		savePath.mkdirs();
		
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
	
		cameraView.setClickable(true);
		cameraView.setOnClickListener(this);
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
		
		// This is all very sloppy
		if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.THREE_GPP) {
	    	try {
				recordingFile = File.createTempFile("videocapture", ".3gp", savePath);
				Log.v(LOGTAG,"Recording at: " + recordingFile.getAbsolutePath());
				recorder.setOutputFile(recordingFile.getAbsolutePath());
			} catch (IOException e) {
				Log.v(LOGTAG,"Couldn't create file");
				e.printStackTrace();
				finish();
			}
		} else if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.MPEG_4) {
	    	try {
	    		recordingFile = File.createTempFile("videocapture", ".mp4", savePath);
				Log.v(LOGTAG,"Recording at: " + recordingFile.getAbsolutePath());
				recorder.setOutputFile(recordingFile.getAbsolutePath());
			} catch (IOException e) {
				Log.v(LOGTAG,"Couldn't create file");
				e.printStackTrace();
				finish();
			}
		} else {
	    	try {
	    		recordingFile = File.createTempFile("videocapture", ".mp4", savePath);
				Log.v(LOGTAG,"Recording at: " + recordingFile.getAbsolutePath());
				recorder.setOutputFile(recordingFile.getAbsolutePath());
			} catch (IOException e) {
				Log.v(LOGTAG,"Couldn't create file");
				e.printStackTrace();
				finish();
			}
	
		}
		//recorder.setMaxDuration(50000); // 50 seconds
		//recorder.setMaxFileSize(5000000); // Approximately 5 megabytes
		
		try {
			recorder.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			finish();
		} catch (IOException e) {
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

			// Convert to video
			processVideo = new ProcessVideo();
			processVideo.execute();
			
			// Let's prepareRecorder so we can record again
			//prepareRecorder();
		} else {
			recording = true;
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
	    
	private class ProcessVideo extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {

	        Process ffmpegProcess = null;
	        
	        try {
	        		        	
	        	// ffmpeg -i VID_20110811_122115.3gp -f mov -vcodec mpeg1video -b 800k -s 640x480 -ab 64k -acodec aac -strict experimental -vf "color=red:320x240 [over]; [in][over] overlay [out]" output.mov
				//String[] args2 = {"/data/data/com.mobvcasting.ffmpegcommandlinetest/ffmpeg", "-y", "-i", "/data/data/com.mobvcasting.ffmpegcommandlinetest/", "-vcodec", "copy", "-acodec", "copy", "-f", "flv", "rtmp://192.168.43.176/live/thestream"};
				
	        	Log.v(LOGTAG,"In doInBackground:recordingFile: " + recordingFile.getPath());
	        	Log.v(LOGTAG,"In doInBackground:savePath: " + savePath.getPath());
	        	
	        	// Trying with a simple vflip filter, no overlaying yet
	        	String[] ffmpegCommand = {"/data/data/"+PACKAGENAME+"/ffmpeg", "-f", "mov", "-vcodec", "mpeg1video", "-b", "800k", "-s", "640x480", "-acodec", "aac", "-strict", "experimental", "-vf", "\"vflip\"", "-i", recordingFile.getPath(), savePath.getPath()+"/output.mov"};
	        	StringBuilder ffmpegCmmandSb = new StringBuilder();
	        	for (int i = 0; i < ffmpegCommand.length; i++) {
	        		ffmpegCmmandSb.append(ffmpegCommand[i]);
	        	}
	        	Log.v(LOGTAG, ffmpegCmmandSb.toString());
				ffmpegProcess = new ProcessBuilder(ffmpegCommand).redirectErrorStream(true).start();         	
				
				OutputStream ffmpegOutStream = ffmpegProcess.getOutputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));

				String line;
				
				Log.v(LOGTAG,"***Starting FFMPEG***");
				while ((line = reader.readLine()) != null)
				{
					Log.v(LOGTAG,"***"+line+"***");
				}
				Log.v(LOGTAG,"***Ending FFMPEG***");
	
	    
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
	        
	        if (ffmpegProcess != null) {
	        	ffmpegProcess.destroy();        
	        }
	        
	        return null;
		}
		
	     protected void onPostExecute(Void... result) {
	    	 Toast toast = Toast.makeText(ObscuraVideoCam.this, "Done Processing Video", Toast.LENGTH_LONG);
	    	 toast.show();
	     }
	}

	public void onInfo(MediaRecorder mr, int what, int extra) {
		
	}

	public void onError(MediaRecorder mr, int what, int extra) {
		
	}	
}