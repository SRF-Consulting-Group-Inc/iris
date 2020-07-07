/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2015  SRF Consulting Group
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package us.mn.state.dot.tms.client.camera;

import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FileTransferClient;

import us.mn.state.dot.sched.Job;
import us.mn.state.dot.sched.Scheduler;
import us.mn.state.dot.tms.Camera;
import us.mn.state.dot.tms.Encoding;
import us.mn.state.dot.tms.utils.I18N;
import static us.mn.state.dot.tms.client.widget.Widgets.UI;

/**
 * A video stream which reads still images from an FTP server.
 *
 * @author Michael Janson
 */
public class FTPStream implements VideoStream {

	/** Default timeout for direct URL Connections */
	static protected final int TIMEOUT_DIRECT = 10 * 1000;
	
	/** Invalid Username or Password */
	static private final String INVALID_USERNAME_PASSWORD = "430";
	
	/** Valid Username, Passwork Required */
	static private final String INVALID_PASSWORD = "331";
	
	/** File Not Found or Insufficient Access */
	static private final String FILE_NOT_FOUND = "550";
	
	/** Not logged in */
	static private final String NOT_LOGGED_IN = "530";

	/** Label to display video stream */
	private final JLabel screen = new JLabel();

	/** Requested video size */
	private final Dimension size;

	/** Input stream to read */
	private InputStream stream;

	/** Flag to continue running stream */
	private boolean running = true;

	/** Stream error message */
	private String error_msg = null;
	
	private FileTransferClient ftpClient;
	
	private String ftpUsername;
	
	private String ftpPassword;
	
	private String ip;
	
	private int port;
	
	private String ftpPath;
	
	private boolean same_filename;
	
	private String baseUrl;
	
	private String ftp_filename;
		

	/** Set the stream error message */
	protected void setErrorMsg(String e) {
		if(error_msg == null)
			error_msg = e;
	}

	/** Create a new JPEG stream 
	 * @throws IOException */
	public FTPStream(Scheduler s, VideoRequest req, Camera c)
		throws IOException
	{
		size = UI.dimension(req.getSize().width, req.getSize().height);
		ftpClient = new FileTransferClient();

		
		/** Get ip and port from encoder field */
		baseUrl = c.getEncoder();
		String[] params = baseUrl.split(":");
		ip = params[0];
		
		/** Default port is 21 if no port is provided */
		if (params.length > 1){
			port = Integer.parseInt(params[1]);
		}
		else{
			port = 21;
		}
		
		/** Get base directory of images, provide root (/) if null or blank */
		ftpPath = c.getFtpPath();
		if (ftpPath == null || ftpPath.trim().equals("")){
			ftpPath = "/";
		}	
		
		/** Get filename if static image path */
		same_filename = c.getSameFilename();
		if (same_filename){
			ftp_filename = c.getFtpFilename();
		}
		
		/** Use anonymous with blank password if username field is left blank */
		ftpUsername = c.getFtpUsername();
		ftpPassword = c.getFtpPassword();
		if (ftpUsername == null || ftpUsername.trim().equals("")){
			ftpUsername = "anonomymous";
			ftpPassword = "";
		}
			
		/** Get the FTP Image */
		getFtpImage();
		
		/** Create thread to read image based on refresh interval */
		/** @throws IOException */
		final Job job = new Job(Calendar.MINUTE, c.getRefInterval()){
			public void perform() throws IOException{
				if(running){
					getFtpImage();
				}	
			}
			public boolean isRepeating() {
				return running;
			}
		};
		s.addJob(job);
		
	}
	
	protected FTPFile getMaxLastModified(FTPFile[] ftpFiles){
		return Collections.max(Arrays.asList(ftpFiles),new FTPComparator());
	}

	
	/** Get the FTP Image */
	/** @throws IOException */
	private void getFtpImage() throws IOException{
		/** Try connection 5 times before throwing exception */
		for(int i=0, n=5; i<n; i++){	
			try{			
				/** Connect to FTP server and change to image directory */
				if (ftpClient.isConnected() == false)
					connectFTP();				
				/** Get either most recent image or filename provided */
				getFTPFile();				
				/** Create URL Connection inputstream */		
				stream = ftpClient.downloadStream(ftp_filename);
				/** Read inputstream */
				readStream();				
				/** Close inputstream */
				stream.close();
				break;
			}catch(IOException e){
				if (i == 2){
					throw new IOException(I18N.get(getFtpStatus()));
				}
			}catch(Exception e){
				throw new IOException(I18N.get(getFtpStatus()));
			}
			finally {
				if (stream != null)
					stream.close();
			}
		}
	}
	
	/** Connect to FTP server using credentials provided and change to image directory */
	/** @throws IOException 
	 * @throws FTPException */
	private void connectFTP() throws IOException, FTPException{
		/** Connect to FTP server */
		try {
			ftpClient.setRemoteHost(ip);
			ftpClient.setRemotePort(port);
			ftpClient.setUserName(ftpUsername);
			ftpClient.setPassword(ftpPassword);
			ftpClient.connect();
			ftpClient.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
			ftpClient.setContentType(FTPTransferType.BINARY);
			/** Change to base directory */
			ftpClient.changeDirectory(ftpPath);
		} catch (FTPException e) {
			e.printStackTrace();
			throw new FTPException(e.getMessage());
		}
	}
	
	/** Is the image name constant or does it increment 
	If it increments, grab most recent file, otherwise
	just use filename provided */
	/** @throws IOException */
	private void getFTPFile() throws IOException{
		if (!same_filename){
			try {
				ftp_filename = getMaxLastModified(ftpClient.directoryList()).getName();
			} catch (FTPException | ParseException e) {
				e.printStackTrace();
			}
		}
	}

	/** Read a video stream */
	private void readStream() {
		try {
			byte[] idata = getImage();
			screen.setIcon(createIcon(idata));
		}
		catch(IOException e) {
			setErrorMsg(e.getMessage());
			screen.setIcon(null);
			running = false;
		}
	}

	/** Get the next image in the jpeg stream */
	/** @throws IOException */
	protected byte[] getImage() throws IOException {
	    byte[] image = new byte[0];
	    byte[] buff = new byte[1024];
	    int k = -1;
	    while((k = stream.read(buff, 0, buff.length)) > -1) {
	        byte[] tbuff = new byte[image.length + k]; // temp buffer size = bytes already read + bytes last read
	        System.arraycopy(image, 0, tbuff, 0, image.length); // copy previous bytes
	        System.arraycopy(buff, 0, tbuff, image.length, k);  // copy current lot
	        image = tbuff; // call the temp buffer as your result buff
	    }
	    return image;
	}

	/** Create an image icon from image data */
	protected ImageIcon createIcon(byte[] idata) {
		ImageIcon icon = new ImageIcon(idata);
		if(icon.getIconWidth() == size.width &&
		   icon.getIconHeight() == size.height)
			return icon;
		Image im = icon.getImage().getScaledInstance(size.width,
			size.height, Image.SCALE_FAST);
		return new ImageIcon(im);
	}

	/** Get a component for displaying the video stream */
	public JComponent getComponent() {
		return screen;
	}

	/** Get the status of the stream */
	public String getStatus() {
		String e = error_msg;
		return (e != null) ? e : Encoding.FTP.toString();
	}

	/** Test if the video is playing */
	public boolean isPlaying() {
		return running;
	}

	/** Dispose of the video stream */
	public void dispose() {
		running = false;
		try {
			if (stream != null){
				stream.close();
			}
		}
		catch(IOException e) {
			setErrorMsg(e.getMessage());
		}
		screen.setIcon(null);
	}
	
	/** Get status of FTP correspondence */
	public String getFtpStatus(){
		if(ftpClient.getLastReply() != null){
			String code = ftpClient.getLastReply().getReplyCode();
			switch (code){
			case INVALID_USERNAME_PASSWORD:
				return "camera.ftp.errorform1";
			case INVALID_PASSWORD:
				return "camera.ftp.errorform2";
			case FILE_NOT_FOUND:
				return "camera.ftp.errorform3";
			case NOT_LOGGED_IN:
				return "camera.ftp.errorform4";
			default:
				return "camera.ftp.errorform5";
			}
		}else return "camera.ftp.errorform5";
	}
}
