/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2002-2017  Minnesota Department of Transportation
 * Copyright (C) 2014-2015  AHMCT, University of California
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.elements.AppSink;

import us.mn.state.dot.sched.Job;
import us.mn.state.dot.sched.Scheduler;
import us.mn.state.dot.tms.Camera;
import us.mn.state.dot.tms.Encoding;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.UserProperty;
import us.mn.state.dot.tms.client.camera_test.SimpleVideoComponent;
import us.mn.state.dot.tms.client.widget.AbstractForm;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.Icons;
import us.mn.state.dot.tms.client.widget.SmartDesktop;
import us.mn.state.dot.tms.utils.I18N;
import us.mn.state.dot.tms.utils.OSUtils;
import static us.mn.state.dot.tms.client.widget.Widgets.UI;

/**
 * A JPanel that can display a video stream. It includes a status label.
 *
 * @author Timothy Johnson
 * @author Douglas Lau
 * @author Travis Swanston
 */
public class StreamPanel2 extends AbstractForm {

	/** Status panel height */
	static private final int HEIGHT_STATUS_PNL = 20;

	/** Control panel height */
	static private final int HEIGHT_CONTROL_PNL = 40;

	/** Milliseconds between updates to the status */
	static private final int STATUS_DELAY = 1000;

	/** Camera streamer thread */
	private Scheduler STREAMER;

	/** Video request */
	private final VideoRequest video_req;

	/** Auto-play mode */
	private final boolean autoplay;

	/** JPanel which holds the component used to render the video stream */
	private final JPanel screen_pnl;

	/** JPanel which holds the status widgets */
	private final JPanel status_pnl;

	/** Stream controls panel and its components */
	private final JPanel control_pnl;

	/** Stop button */
	private JButton stop_button;

	/** Play button */
	private JButton play_button;

	/** Play external button */
	private JButton playext_button;

	/** JLabel for displaying the stream details (codec, size, framerate) */
	private final JLabel status_lbl = new JLabel();

	/** Stream control commands */
	static private enum StreamCommand {
		STOP,
		PLAY,
		PLAY_EXTERNAL;
	}

	/** Stream progress timer */
	private Timer timer;

	/** Mouse PTZ control */
	private final MousePTZ mouse_ptz;

	/** Current Camera */
	private Camera camera = null;

	/** Current video stream */
	private VideoStream stream = null;

	/** Stream status listener */
	private final StreamStatusListener ss_listener;
	
	/** External viewer from user/client properties.  Null means none. */
	private final String external_viewer;

	/** Most recent streaming state.  State variable for event FSM. */
	private boolean stream_state = false;

	/** Create a mouse PTZ */
	static private MousePTZ createMousePTZ(CameraPTZ cam_ptz, Dimension sz,
		JPanel screen_pnl)
	{
		return (cam_ptz != null)
		      ? new MousePTZ(cam_ptz, sz, screen_pnl)
		      : null;
	}

	/** Stream status listeners to notify on stream status change events */
	private final Set<StreamStatusListener> ssl_set =
		new HashSet<StreamStatusListener>();
	
    private Pipeline pipe;

	/**
	 * Create a new stream panel.
	 * @param req The VideoRequest object to use.
	 * @param cam_ptz An optional (null for none) CameraPTZ PTZ manager.
	 *                Mouse PTZ control is disabled if null.
	 * @param s A reference to the current Session, or null if external
	 *          viewer support not desired.
	 * @param ctrl Enable streaming control buttons?  If false, you
	 *             probably want autoplay to be true.
	 * @param auto Automatically play upon setCamera()?
	 */
	public StreamPanel2(VideoRequest req, CameraPTZ cam_ptz, Session s,
		boolean ctrl, boolean auto)
	{
		//super(new GridBagLayout());
		super("Stream Panel " + cam_ptz.getCamera().getName());
		

		/** Timer listener for updating video status */
		class StatusUpdater implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				updateStatus();
			}
		};
		
		timer = new Timer(STATUS_DELAY, new StatusUpdater());
		
		STREAMER = new Scheduler("streamer");
		
		video_req = req;
		external_viewer = (s == null) ? null
			: UserProperty.getExternalVideoViewer(s.getProperties());
		autoplay = auto;
		VideoRequest.Size vsz = req.getSize();
		Dimension sz = UI.dimension(vsz.width, vsz.height);
		screen_pnl = createScreenPanel(sz);
		mouse_ptz = createMousePTZ(cam_ptz, sz, screen_pnl);
		status_pnl = createStatusPanel(vsz);
		control_pnl = createControlPanel(vsz);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		add(screen_pnl, c);
		add(status_pnl, c);
		if (ctrl)
			add(control_pnl, c);
		int pnlHeight = vsz.height + HEIGHT_STATUS_PNL
			+ (ctrl ? HEIGHT_CONTROL_PNL : 0);
		
		int w = (int)(vsz.width*1.1);
		int h = (int)(pnlHeight*1.55);

		setPreferredSize(UI.dimension(w, h));
		setMinimumSize(UI.dimension(w, h));
		setMaximumSize(UI.dimension(w, h));
		updateButtonState();
		
		add(new CamControlPanel(cam_ptz), BorderLayout.SOUTH);
		
		ss_listener = createStreamStatusListener();
		
		setCamera(cam_ptz.getCamera());
		

	}

	/**
	 * Create a new stream panel with autoplay, no stream controls, and
	 * no mouse PTZ.
	 */
	public StreamPanel2(VideoRequest req) {
		this(req, null, null, false, true);
	}

	/** Create the screen panel */
	private JPanel createScreenPanel(Dimension sz) {
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createBevelBorder(
			BevelBorder.LOWERED));
		p.setPreferredSize(sz);
		p.setMinimumSize(sz);
		return p;
	}

	/** Create the status panel */
	private JPanel createStatusPanel(VideoRequest.Size vsz) {
		JPanel p = new JPanel(new BorderLayout());
		p.add(status_lbl, BorderLayout.WEST);
		p.setPreferredSize(UI.dimension(vsz.width, HEIGHT_STATUS_PNL));
		p.setMinimumSize(UI.dimension(vsz.width, HEIGHT_STATUS_PNL));
		return p;
	}

	/** Create the control panel */
	private JPanel createControlPanel(VideoRequest.Size vsz) {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER,
			UI.hgap, UI.vgap));
		stop_button = createControlBtn("camera.stream.stop",
			StreamCommand.STOP);
		play_button = createControlBtn("camera.stream.play",
			StreamCommand.PLAY);
		playext_button = createControlBtn("camera.stream.playext",
			StreamCommand.PLAY_EXTERNAL);
		p.add(stop_button);
		p.add(play_button);
		p.add(playext_button);
		p.setPreferredSize(UI.dimension(vsz.width,
			HEIGHT_CONTROL_PNL));
		p.setMinimumSize(UI.dimension(vsz.width, HEIGHT_CONTROL_PNL));
		return p;
	}

	/**
	* Create a stream-control button.
	* @param text_id Text ID
	* @param sc The StreamCommand to associate with the button
	* @return The requested JButton.
	*/
	private JButton createControlBtn(String text_id,
		final StreamCommand sc)
	{
		final JButton btn;
		IAction ia = null;
		ia = new IAction(text_id) {
			@Override
			protected void doActionPerformed(ActionEvent
				ev)
			{
				handleControlBtn(sc);
			}
		};
		btn = new JButton(ia);
		btn.setPreferredSize(UI.dimension(40, 28));
		btn.setMinimumSize(UI.dimension(28, 28));
		btn.setMargin(new Insets(0, 0, 0, 0));
		ImageIcon icon = Icons.getIconByPropName(text_id);
		if (icon != null) {
			btn.setIcon(icon);
			btn.setHideActionText(true);
		}
		btn.setFocusPainted(false);
		return btn;
	}

	/** Handle control button press */
	private void handleControlBtn(StreamCommand sc) {
		if (sc == StreamCommand.STOP) {
			STREAMER.addJob(new Job() {
				public void perform() {
					stopStream();
				}
			});
		}
		else if (sc == StreamCommand.PLAY) {
			STREAMER.addJob(new Job() {
				public void perform() {
					playStream();
				}
			});
		}
		else if (sc == StreamCommand.PLAY_EXTERNAL)
			launchExternalViewer(camera);
	}

	/**
	 * Start streaming from the current camera, unless null.
	 * This is normally called from the streamer thread.
	 */
	private void playStream() {
	//	stopStream();
		if (camera == null) {
			setStatusText(null);
			return;
		}
		setStatusText(I18N.get("camera.stream.opening"));
		requestStream(camera);
		bindStreamStatusListener(ss_listener);
	}

	/**
	 * Stop streaming, if a stream is currently active.
	 * This is normally called from the streamer thread.
	 */
	public void stopStream() {
//		if (stream != null)
//	    if (pipe != null)
		clearStream();
	}

	/** Update stream status */
	private void updateStatus() {
		STREAMER.addJob(new Job() {
			public void perform() {
//				System.out.println(String.format("Updating status for stream %s", camera.getName()));
//				VideoStream vs = stream;
//				if (vs != null && vs.isPlaying())
					//setStatusText(vs.getStatus());
				if (pipe != null && pipe.isPlaying())
					setStatusText(Encoding.fromOrdinal(camera.getEncoderType().getEncoding()).toString());
				else
					clearStream();
			}
		});
	}

	@Override
	protected void close(SmartDesktop desktop) {
		// stop the stream before closing the window
		stopStream();
		super.close(desktop);
	}
	
	/**
	 * Set the Camera to use for streaming.  If a current stream exists,
	 * it is stopped.  If autoplay is enabled and Camera c can be
	 * streamed, it will be.
	 *
	 * @param c The camera to stream, or null to merely clear the current
	 *          stream.
	 */
	public void setCamera(final Camera c) {
		STREAMER.addJob(new Job() {
			public void perform() {
			//	stopStream();
				camera = c;
				updateButtonState();
				setStatusText(null);
				boolean mjpeg = video_req.hasMJPEG(c);
				if (autoplay)
					playStream();
			}
		});
	}

	/** Request a new video stream */
	private void requestStream(Camera c) {
//		try {
//			stream = createStream(c);
			if (! Gst.isInitialized()) {
				Gst.init("CameraTest");
			}
	        
//            SimpleVideoComponent vc = new SimpleVideoComponent();
//            Bin bin = Gst.parseBinFromDescription(
//                    "autovideosrc ! videoconvert",
//                    true);
//            Bin bin = Gst.parseBinFromDescription(
//                    "souphttpsrc location=http://10.1.4.183/axis-cgi/mjpg/video.cgi ! jpegdec ! videoconvert",
//                    true);
//            Bin bin = Gst.parseBinFromDescription(
//                    "rtspsrc location=rtsp://10.1.4.183/axis-media/media.amp ! rtph264depay ! avdec_h264",
//                    true);
            
//            System.out.println(System.getenv().get("GST_DEBUG_DUMP_DOT_DIR"));            
//            bin.debugToDotFile(Bin.DebugGraphDetails.SHOW_ALL, "bingraph");
            
//            pipe = new Pipeline();
//            pipe.addMany(bin, vc.getElement());
//            Pipeline.linkMany(bin, vc.getElement());
//            pipe.debugToDotFile(Bin.DebugGraphDetails.SHOW_ALL, "mjpeg_pipe");
////            vc.setPreferredSize(new Dimension(640, 480));           
//            for ( Element e : bin.getElements()) {
//            	System.out.println(e.toString());
//            }
	        

//			String pipeLaunch = "rtspsrc location=" + video_req.getUri(c) + " protocols=tcp timeout=10000000 ! rtph264depay ! avdec_h264 ! videoconvert ! appsink name=appsink";
			String pipeLaunch = "uridecodebin uri=" + video_req.getUri(c) + " ! videoconvert ! appsink name=appsink";
//			String pipeLaunch = "uridecodebin uri=" + video_req.getUri(c) + " ! videoconvert ! fakesink";
//			System.out.println(pipeLaunch);
	        pipe = (Pipeline)Gst.parseLaunch(pipeLaunch);
//	        pipe.debugToDotFile(Bin.DebugGraphDetails.SHOW_ALL, "pipe_dbg2");
	        
//	        pipe = (Pipeline)Gst.parseLaunch("rtspsrc location=rtsp://10.1.4.183/axis-media/media.amp ! rtph264depay ! avdec_h264 ! videoconvert ! appsink name=appsink");
	        SimpleVideoComponent vc = new SimpleVideoComponent((AppSink) pipe.getElementByName("appsink"));
           
            pipe.play();

//          pipe.debugToDotFile(Bin.DebugGraphDetails.SHOW_ALL, "mjpeg_pipe_play");
			JComponent screen = vc;
//			JComponent screen = stream.getComponent();
			screen.setPreferredSize(screen_pnl.getPreferredSize());
			screen_pnl.add(screen);
			timer.start();
			handleStateChange();
//		}
//		catch (IOException e) {
//			setStatusText(e.getMessage());
//		}
	}

//	/** Create a new video stream */
//	private VideoStream createStream(Camera c) throws IOException {
//		if (video_req.hasMJPEG(c))
//			return new MJPEGStream(STREAMER, video_req, c);
//		else
//			throw new IOException("Unable to stream");
//	}

	/** Create the StreamStatusListener */
	private StreamStatusListener createStreamStatusListener() {
		StreamStatusListener ssl = new StreamStatusListener() {
			@Override
			public void onStreamStarted() {
				updateCamControls();
			}
			@Override
			public void onStreamFinished() {
				// dispose of stream thingys
				clearStream();
				updateCamControls();
			}
		};
		return ssl;
	}
	
	private void updateCamControls() {
		// TODO needed? reimplement from CameraDispatcher? move from there to here and StreamPanel?
	}
	
	/** Clear the video stream */
	private void clearStream() {
//		if (camera != null)
//			System.out.println(String.format("Cleaning up after stream %s in StreamPanel2 ...", camera.getName()));
		timer.stop();
		if (pipe != null) {
			pipe.stop();
			// try getting elements and disposing of them, see what happens...
			List<Element> pipeElements = pipe.getElementsRecursive();
			for (Element e : pipeElements) {
				e.setState(null);
				e.dispose();
			}
			pipe = null;
		}
		screen_pnl.removeAll();
		screen_pnl.repaint();
//		VideoStream vs = stream;
//		if (vs != null) {
//			vs.dispose();
//			stream = null;
//		}
		setStatusText(null);
		handleStateChange();
	}

	/** Dispose of the stream panel */
	public final void dispose() {
		clearStream();
		if (mouse_ptz != null)
			mouse_ptz.dispose();
		super.dispose();
	}

	/** Set the status label. */
	private void setStatusText(String s) {
		status_lbl.setText(s);
	}

	/** Are we currently streaming? */
	public boolean isStreaming() {
		return pipe != null;//stream != null;
	}

	/**
	 * Handle a possible streaming state change.  If necessary, update
	 * stream_state, streaming control button status, and notify
	 * StreamStatusListeners, ensuring against superfluous duplicate
	 * events.
	 */
	private void handleStateChange() {
		boolean streaming = isStreaming();
		if (streaming == stream_state)
			return;
		stream_state = streaming;
		updateButtonState();
//		System.out.println(ssl_set.size());
		for (StreamStatusListener ssl : ssl_set) {
			if (stream_state)
				ssl.onStreamStarted();
			else
				ssl.onStreamFinished();
		}
	}

	/** Update the button state */
	private void updateButtonState() {
		if (camera == null) {
			stop_button.setEnabled(false);
			play_button.setEnabled(false);
			playext_button.setEnabled(false);
			return;
		}
		boolean streaming = isStreaming();
		boolean mjpeg = video_req.hasMJPEG(camera);
		stop_button.setEnabled(streaming);
		play_button.setEnabled(!streaming);
		playext_button.setEnabled(true);
	}

	/**
	 * Bind a StreamStatusListener to this StreamPanel.
	 */
	public void bindStreamStatusListener(StreamStatusListener ssl) {
		if (ssl != null)
			ssl_set.add(ssl);
	}

	/**
	 * Unbind a StreamStatusListener from this StreamPanel.
	 */
	public void unbindStreamStatusListener(StreamStatusListener ssl) {
		if (ssl != null)
			ssl_set.remove(ssl);
	}

	/** Launch the external viewer for a Camera. */
	private void launchExternalViewer(Camera c) {
		if (c == null)
			return;
		if (external_viewer == null) {
			// FIXME: i18n
			setStatusText("Error: no external viewer defined.");
			return;
		}
		String uri = video_req.getUri(c).toString();
		if (uri.length() == 0) {
			// FIXME: i18n
			setStatusText("Error: cannot determine URL.");
			return;
		}
		String[] fields = external_viewer.split(",");
		List<String> cmd =
			new ArrayList<String>(fields.length + 1);
		for (String f : fields)
			cmd.add(f);
		cmd.add(uri);
		OSUtils.spawnProcess(cmd);
		// FIXME: i18n
		setStatusText("External viewer launched.");
		return;
	}
}
