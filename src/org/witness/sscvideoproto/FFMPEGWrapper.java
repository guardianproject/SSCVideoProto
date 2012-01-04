package org.witness.sscvideoproto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Vector;

import android.content.Context;
import android.media.CamcorderProfile;
import android.os.Environment;
import android.util.Log;

public class FFMPEGWrapper {

	public static final String PACKAGENAME = "org.witness.sscvideoproto";
	public static final String LOGTAG = "FFMPEGWRAPPER";
		
	String[] libraryAssets = {"ffmpeg"};
	
	Context context;

	public FFMPEGWrapper(Context _context) {
		context = _context;
		File libraryAssetsDirectory = new File("/data/data/" + PACKAGENAME);
		if (!libraryAssetsDirectory.exists()) {
			libraryAssetsDirectory.mkdirs();
		}
		moveLibraryAssets();
	}
	
	private void moveLibraryAssets() {
        for (int i = 0; i < libraryAssets.length; i++) {
			try {
				InputStream ffmpegInputStream = context.getAssets().open(libraryAssets[i]);
		        FileMover fm = new FileMover(ffmpegInputStream,"/data/data/"+PACKAGENAME+"/" + libraryAssets[i]);
		        fm.moveIt();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
        	// Should have better permissions than 777
        	String[] args = {"chmod", "777", "/data/data/"+PACKAGENAME+"/" + libraryAssets[i]};
        	execProcess(args);
        }
        
        // This is the directory so I can view it from the command line
    	String[] args = {"chmod", "777", "/data/data/"+PACKAGENAME+"/"};
    	execProcess(args);
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
	    	Process process = new ProcessBuilder(command).redirectErrorStream(true).start();         	
			
			OutputStream outputStream = process.getOutputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	
			String line;
			
			Log.v(LOGTAG,"***Starting Command***");
			while ((line = reader.readLine()) != null)
			{
				Log.v(LOGTAG,"***"+line+"***");
			}
			Log.v(LOGTAG,"***Ending Command***");

		    if (process != null) {
		    	process.destroy();        
		    }

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void processVideo(File redactSettingsFile, Vector<ObscureRegion> obscureRegions, File inputFile, File outputFile, int width, int height, int frameRate, int kbitRate) {
		writeRedactData(redactSettingsFile, obscureRegions);
		    	
    	String widthxheight = width + "x" + height;
    	
    	//ffmpeg -v 10 -y -i /sdcard/org.witness.sscvideoproto/videocapture1042744151.mp4 -vcodec libx264 -b 3000k -s 720x480 -r 30 -acodec copy -f mp4 -vf 'redact=/data/data/org.witness.sscvideoproto/redact_unsort.txt' /sdcard/org.witness.sscvideoproto/new.mp4
    	String[] ffmpegCommand = {"/data/data/"+PACKAGENAME+"/ffmpeg", "-v", "10", "-y", "-i", inputFile.getPath(), 
				"-vcodec", "libx264", "-b", kbitRate+"k", "-s", widthxheight, "-r", ""+frameRate,
				"-an",
				"-f", "mp4", outputFile.getPath()};
    	//"-vf" , "redact=" + Environment.getExternalStorageDirectory().getPath() + "/" + PACKAGENAME + "/redact_unsort.txt",

    	
    	// Need to make sure this will create a legitimate mp4 file
    	//"-acodec", "ac3", "-ac", "1", "-ar", "16000", "-ab", "32k",
    	//"-acodec", "copy",

    	/*
    	String[] ffmpegCommand = {"/data/data/"+PACKAGENAME+"/ffmpeg", "-v", "10", "-y", "-i", recordingFile.getPath(), 
    					"-vcodec", "libx264", "-b", "3000k", "-vpre", "baseline", "-s", "720x480", "-r", "30",
    					//"-vf", "drawbox=10:20:200:60:red@0.5",
    					"-vf" , "\"movie="+ overlayImage.getPath() +" [logo];[in][logo] overlay=0:0 [out]\"",
    					"-acodec", "copy",
    					"-f", "mp4", savePath.getPath()+"/output.mp4"};
    	*/
    	
    	execProcess(ffmpegCommand);
	    
	}
	
	private void writeRedactData(File redactSettingsFile, Vector<ObscureRegion> obscureRegions) {
		// Write out the finger data
		try {			
			FileWriter redactSettingsFileWriter = new FileWriter(redactSettingsFile);
			PrintWriter redactSettingsPrintWriter = new PrintWriter(redactSettingsFileWriter);
			
			for (int i = 0; i < obscureRegions.size(); i++) {
				ObscureRegion or = (ObscureRegion)obscureRegions.get(i);
				Log.v(LOGTAG,"Writing: " + or.toString());
				redactSettingsPrintWriter.println(or.toString());
			}
			redactSettingsPrintWriter.flush();
			redactSettingsPrintWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
