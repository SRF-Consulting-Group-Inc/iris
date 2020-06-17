/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2019-2020  SRF Consulting Group
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.GstException;
import org.freedesktop.gstreamer.GstObject;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.State;
import org.freedesktop.gstreamer.StateChangeReturn;
import org.freedesktop.gstreamer.elements.AppSink;

/**
 * Stream manager for a GStreamer stream.
 *
 * @author John L. Stanley - SRF Consulting Group
 */
public class VidStreamMgrGst extends VidStreamMgr {

	Element srcElem;
	VidComponentGst gstComponent;

	//-------------------------------------------

	/**
	 * Create a GStreamer stream manager.
	 * @param vp The VideoPanel to use. 
	 * @param vr The StreamReq to use.
	 */
    protected VidStreamMgrGst(VidPanel vp, VidStreamReq vr) {
		super(vp, vr);
//		Gst.init("StreamMgrGst");
//		setComponent(createScreenPanel());
	}

	//-------------------------------------------

	@Override
	/** Are we currently streaming? */
	public boolean isStreaming() {
		return (pipe != null);
	}

	@Override
	protected void doStartStream() {
		if (bStreaming)
			return;
//		if (camera == null) {
////			setStatusText(null);
//			return;
//		}
//		setStatusText(I18N.get("camera.stream.opening"));
		openStream();
	}

	@Override
	protected void doStopStream() {
	    if (pipe != null)
			closeStream();
	}

	//-------------------------------------------
	
	/** Does config appear to be OK for this video manager? */
	public static boolean isOkConfig(String config) {
		return (config.contains(" ! ") && isGstInstalled());
	}

	//-------------------------------------------
	
	/** Most recent streaming state.  State variable for event FSM. */
	private boolean bStreaming = false;

	private Pipeline pipe;
	
	//-------------------------------------------

	private AppSink appsink;
	private Bus     bus;
	private AppSinkListener appsinkListener;
	private BusListener     busListener;

	/** Open the video stream */
	private synchronized boolean openStream() {
		try {
			pipe = (Pipeline)Gst.parseLaunch(vreq.getConfig()+" ! appsink name=appsink");
			appsink = (AppSink) pipe.getElementByName("appsink");
			List<Element> elements = pipe.getElements();
			srcElem = elements.get(0);
//			dumpElement(srcElem, "Start");
			gstComponent = new VidComponentGst(appsink, this);
			appsinkListener = new AppSinkListener();
			appsink.connect((AppSink.NEW_SAMPLE) appsinkListener);
			appsink.connect((AppSink.EOS)        appsinkListener);
			bus = pipe.getBus();
			busListener = new BusListener();
			bus.connect((Bus.ERROR)  busListener);
			bus.connect((Bus.WARNING)busListener);
			bus.connect((Bus.INFO)   busListener);
			pipe.play();
			setComponent(gstComponent);
			return true;
		} catch (GstException e) {
			setErrorMsg(e, "Unknown GStreamer error");
			return false;
		} catch (Exception e) {
			setErrorMsg(e, "Unknown error");
			return false;
		}
	}

	/** Close the video stream */
	private synchronized void closeStream() {
		Pipeline p = pipe;
		if (p != null) {
			pipe = null;
			if (appsink == null)
				System.out.println("appsink is null");
			appsink.disconnect((AppSink.NEW_SAMPLE) appsinkListener);
			appsink.disconnect((AppSink.EOS)        appsinkListener);
			bus.disconnect((Bus.ERROR)  busListener);
			bus.disconnect((Bus.WARNING)busListener);
			bus.disconnect((Bus.INFO)   busListener);
			gstComponent.disconnectAll();
			gstComponent = null;
//			setComponent(null);
//			p.stop();
//			p.close();
			try {
				p.setState(State.NULL);
				p.getState();
				System.out.println("CloseStream finished");
			}
			catch (java.lang.IllegalStateException e) {
				e.printStackTrace();
			}
//			setStatus("");
		}
	}

	/** Listen for start and stop of video stream */
	private class AppSinkListener implements AppSink.NEW_SAMPLE, AppSink.EOS {

		@Override
		public FlowReturn newSample(AppSink elem) {
			if (bStreaming) {
				// if we've been dropped, stop the stream
				if (videoPanel.getStreamMgr() != VidStreamMgrGst.this)
					queueStopStream();
			}
			else {
				streamingStarted();
				bStreaming = true;
			}
			return FlowReturn.OK;
		}
	
		@Override
		public void eos(AppSink elem) {
			if (bStreaming) {
				streamingStopped();
				System.out.println("### EOS");
//				dumpElement(elem, "EOS");
//				dumpElement(srcElem, "End");
				bStreaming = false;
			}
		}
	}
	
	/** Listen for pipeline messages */
	private class BusListener implements Bus.ERROR, Bus.WARNING, Bus.INFO {

		/**
		 * Called when a {@link Pipeline} element posts an error message.
		 *
		 * @param source the element which posted the message.
		 * @param code a numeric code representing the error.
		 * @param message a string representation of the error.
		 */
		public void errorMessage(GstObject source, int code, String message) {
			System.out.println("### ERROR: "+code+", "+message);
			setErrorMsg(message);
		}
		
		/**
		 * Called when a {@link Pipeline} element posts an warning message.
		 *
		 * @param source the element which posted the message.
		 * @param code a numeric code representing the warning.
		 * @param message a string representation of the warning.
		 */
		public void warningMessage(GstObject source, int code, String message) {
			System.out.println("### WARNING: "+code+", "+message);
		}

		/**
		 * Called when a {@link Pipeline} element posts an informational
		 * message.
		 *
		 * @param source the element which posted the message.
		 * @param code a numeric code representing the informational message.
		 * @param message a string representation of the informational message.
		 */
		public void infoMessage(GstObject source, int code, String message) {
			System.out.println("### INFO: "+code+", "+message);
		}

	}

	public void dumpElement(Element elem, String title) {
		System.out.println("## "+ title);
		List<String> names = elem.listPropertyNames();
		System.out.println("### "+ names);
		for (String name : names) {
			System.out.println("###-- "+name+" = "+elem.get(name).toString());
		}
	}
	
	//-------------------------------------------
	
	private static Boolean bGstInstalled = null;
	
	private static File tmpDir;
	
	/** See if we have access to GStreamer library */
	public static boolean isGstInstalled() {
		if (bGstInstalled == null) {
			// try to load with whatever environment settings we have now
			bGstInstalled = initGst();
			
			// if we didn't get it and we're using WebStart, try to load from
			// a JAR from the server
			if (isRunningJavaWebStart()) {
				// TODO multi platform
				
				try {
					String path = Native.getWebStartLibraryPath("libgstbase-1.0-0.dll");
					System.out.println(path);
					if (path != null)
						NativeLibrary.addSearchPath("gstreamer", path);
				} catch (UnsatisfiedLinkError e) {
					e.printStackTrace();
				}
				
				try {
					File f = Native.extractFromResourcePath("gstbase");
					System.out.println(f.getAbsolutePath());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//				System.loadLibrary("");
				
				
				// load 
//				InputStream is = InputStream.class.getResourceAsStream(
//						"gstreamer-1.0-mingw-x86_64-1.16.2.jar");
//				ZipInputStream zis = new ZipInputStream(is);
//				ZipEntry ze;
//				
//				System.out.println("Reading zip file...");
//				try {
//					while ((ze = zis.getNextEntry()) != null) {
//						System.out.println(ze.getName());
//					}
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				
//				if (tmpDir == null ) {
//					tmpDir = createTempDirectory("gstreamer");
//					tmpDir.deleteOnExit();
//				}
				
				
				
//				try {
//					
//					File tmp = new File
//					Files.copy(is, tmpDir)
//				}
			}
		}
		return bGstInstalled;
	}
	
	/** Try to initialize GStreamer. */
	private static boolean initGst() {
		try {
			Gst.init("StreamMgrGst");
			System.out.println("GStreamer "+Gst.getVersionString()+" installed");
			return true;
		} catch (GstException|UnsatisfiedLinkError ex) {
			System.out.println("GStreamer not available: "+ex.getMessage());
			return false;
		}
	}
	
	/** Test if we're running via Java WebStart */
	private static boolean isRunningJavaWebStart() {
	    boolean hasJNLP = false;
	    try {
	      Class.forName("javax.jnlp.ServiceManager");
	      hasJNLP = true;
	      System.out.println("Running in WebStart");
	    } catch (ClassNotFoundException ex) {
	      hasJNLP = false;
	      System.out.println("NOT Running in WebStart");
	    }
	    return hasJNLP;
	}
	
	private static File createTempDirectory(String prefix) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File generatedDir = new File(tempDir, prefix + System.nanoTime());
        
        if (!generatedDir.mkdir())
            throw new IOException("Failed to create temp directory " +
            		generatedDir.getName());
        
        return generatedDir;
    }
}
