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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;

import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.elements.AppSink;

import us.mn.state.dot.sched.Job;
import us.mn.state.dot.sched.Scheduler;
import us.mn.state.dot.tms.Camera;
import us.mn.state.dot.tms.CameraHelper;
import us.mn.state.dot.tms.Encoding;
import us.mn.state.dot.tms.client.EditModeListener;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.UserProperty;
import us.mn.state.dot.tms.client.camera.VideoRequest.Size;
import us.mn.state.dot.tms.client.camera_test.SimpleVideoComponent;
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
 * @author Michael Janson
 * @author Gordon Parikh
 */
public class StreamPanel extends JPanel {

	/** Status panel height */
	static private final int HEIGHT_STATUS_PNL = 40;

	/** Control panel height */
	static private final int HEIGHT_CONTROL_PNL = 40;

	/** Milliseconds between updates to the status */
	static private final int STATUS_DELAY = 1000;

	/** Camera streamer thread */
	static private final Scheduler STREAMER = new Scheduler("streamer");

	/** Video request */
	private final VideoRequest video_req;

	/** Auto-play mode */
	private final boolean autoplay;

	/** JPanel which holds the component used to render the video stream */
	private final VidPanel screen_pnl;

	/** Stream controls panel and its components */
	private final JPanel control_pnl;

	/** Stop button */
	private JButton stop_button;

	/** Play button */
	private JButton play_button;

	/** Play external button */
	private JButton playext_button;
	
	/** Save layout button */
	private JButton save_layout_button;
	
	/** Restore layoutl button */
	private JButton restore_layout_button;

	/** JLabel for displaying the stream details (codec, size, framerate) */
	private final JLabel status_lbl = new JLabel();

	/** Stream control commands */
	static private enum StreamCommand {
		STOP,
		PLAY,
		PLAY_EXTERNAL;
	}
	
	/** Layout control commands */
	static private enum LayoutCommand {
		SAVE,
		RESTORE;
	}
	
	/** Current Camera */
	private Camera camera = null;

	/** Current video stream */
	private VideoStream stream = null;

	/** External viewer from user/client properties.  Null means none. */
	private final String external_viewer;

	/** Most recent streaming state.  State variable for event FSM. */
	private boolean stream_state = false;

	/** Timer listener for updating video status */
	private class StatusUpdater implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			updateStatus();
		}
	};

	/** Timer task for updating video status */
	private final StatusUpdater stat_updater = new StatusUpdater();

	/** Stream progress timer */
	private final Timer timer = new Timer(STATUS_DELAY, stat_updater);
	
	/** Stream status listeners to notify on stream status change events */
	private final Set<StreamStatusListener> ssl_set =
		new HashSet<StreamStatusListener>();
	
    private CameraPTZ ptz;
    
    private Session session;
    
	/** Smart desktop */
	private SmartDesktop desktop;
	
	/** Edit mode listener */
	private final EditModeListener edit_lsnr = new EditModeListener() {
		public void editModeChanged() {
			updateEditMode();
		}
	};
    
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
	public StreamPanel(VideoRequest req, CameraPTZ cam_ptz, Session s,
		boolean ctrl, boolean auto)
	{
		super(new GridBagLayout());
		video_req = req;
		external_viewer = (s == null) ? null
			: UserProperty.getExternalVideoViewer(s.getProperties());
		autoplay = auto;
		VideoRequest.Size vsz = req.getSize();
		Dimension sz = UI.dimension(vsz.width, vsz.height);
		screen_pnl = new VidPanel(sz);
		Dimension vpsz = screen_pnl.getPreferredSize();
		control_pnl = createControlPanel(vsz);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		add(screen_pnl, c);
		if (ctrl)
			add(control_pnl, c);
		int pnlHeight = vpsz.height + (ctrl ? HEIGHT_CONTROL_PNL : 0);
		
		setPreferredSize(UI.dimension(vpsz.width, pnlHeight));
		setMinimumSize(UI.dimension(vpsz.width, pnlHeight));
		setMaximumSize(UI.dimension(vpsz.width, pnlHeight));
		updateButtonState();
		
		ptz = cam_ptz;
		session = s;
		if (session != null) {
			desktop = session.getDesktop();
			session.addEditModeListener(edit_lsnr);
		}
	}

	/**
	 * Create a new stream panel with autoplay, no stream controls, and
	 * no mouse PTZ.
	 */
	public StreamPanel(VideoRequest req) {
		this(req, null, null, false, true);
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
		
		save_layout_button = createLayoutBtn("camera.template.save.layout",
				LayoutCommand.SAVE);
		restore_layout_button = createLayoutBtn("camera.template.restore.layout",
				LayoutCommand.RESTORE);
			
		p.add(stop_button);
		p.add(play_button);
		p.add(playext_button);
		p.add(Box.createHorizontalStrut(10));

		p.add(save_layout_button);
		p.add(restore_layout_button);
		p.setPreferredSize(UI.dimension(vsz.width,
			HEIGHT_CONTROL_PNL));
		p.setMinimumSize(UI.dimension(vsz.width, HEIGHT_CONTROL_PNL));
		return p;
	}
	
	/**
	* Create a layout button.
	* @param text_id Text ID
	* @param sc The StreamCommand to associate with the button
	* @return The requested JButton.
	*/
	private JButton createLayoutBtn(String text_id,
		final LayoutCommand lc)
	{
		final JButton btn;
		IAction ia = null;
		ia = new IAction(text_id) {
			@Override
			protected void doActionPerformed(ActionEvent
				ev)
			{
				handleLayoutBtn(lc);
			}
		};
		btn = new JButton(ia);
		btn.setPreferredSize(UI.dimension(100, 28));
		btn.setMinimumSize(UI.dimension(100, 28));
		btn.setMargin(new Insets(0, 0, 0, 0));
//		btn.setText(text_id);
		btn.setFocusPainted(false);
		return btn;
	}
	
	
	/** Handle layout button press */
	private void handleLayoutBtn(LayoutCommand lc) {
		Properties p = session.getProperties();

		if (lc == LayoutCommand.SAVE) {
			UserProperty.saveStreamLayout(p);
		}
		else if (lc == LayoutCommand.RESTORE) {
			initializeCameraFrames(p);
		}
	}

	
	/** Initialize the camera frames */
	private void initializeCameraFrames(Properties p) {

	    HashMap<String, String> hmap = UserProperty.getCameraFrames(p);
	    int num_streams = 0;
	    
	    if (hmap.get(UserProperty.NUM_STREAM.name) != null)
	    	num_streams = Integer.parseInt(hmap.get(UserProperty.NUM_STREAM.name));
	
		for (int i=0; i < num_streams; i++) {
			String cam_name = hmap.get(UserProperty.STREAM_CCTV.name + "." + Integer.toString(i));
			Camera cam = CameraHelper.lookup(cam_name);
			int w = Integer.parseInt(hmap.get(UserProperty.STREAM_WIDTH.name + "." + Integer.toString(i)));
			int h = Integer.parseInt(hmap.get(UserProperty.STREAM_HEIGHT.name + "." + Integer.toString(i)));
			Dimension d = new Dimension(w, h);
			
			int x = Integer.parseInt(hmap.get(UserProperty.STREAM_X.name + "." + Integer.toString(i)));
			int y = Integer.parseInt(hmap.get(UserProperty.STREAM_Y.name + "." + Integer.toString(i)));
				
			int strm_num = Integer.parseInt(hmap.get(UserProperty.STREAM_SRC.name + "." + Integer.toString(i)));

			desktop.showExtFrame(new VidWindow(cam, true, d, strm_num), x, y);	
		}
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
		else if (sc == StreamCommand.PLAY_EXTERNAL) {
			stopStream();
			desktop.showExtFrame(new VidWindow(camera, true, Size.MEDIUM));
		}
	}

	/**
	 * Start streaming from the current camera, unless null.
	 * This is normally called from the streamer thread.
	 */
	private void playStream() {
		stopStream();
		if (camera == null) {
			setStatusText(null);
			return;
		}
		setStatusText(I18N.get("camera.stream.opening"));
		requestStream(camera);
	}

	/**
	 * Stop streaming, if a stream is currently active.
	 * This is normally called from the streamer thread.
	 */
	private void stopStream() {
	    if (screen_pnl != null)
			clearStream();
	}

	/** Update stream status */
	private void updateStatus() {
		STREAMER.addJob(new Job() {
			public void perform() {
				updateButtonState();
			}
		});
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
				stopStream();
				camera = c;
				updateButtonState();
				setStatusText(null);
				if (autoplay)
					playStream();
			}
		});
	}

	/** Request a new video stream */
	private void requestStream(Camera c) {
		screen_pnl.setCamera(c);
		handleStateChange();
		timer.start();
	}

	/** Clear the video stream */
	private void clearStream() {
		screen_pnl.releaseStream();
		screen_pnl.stopStatusMonitor();
		handleStateChange();
	}

	/** Dispose of the stream panel */
	public final void dispose() {
		clearStream();
		if (session != null)
			session.removeEditModeListener(edit_lsnr);
		save_layout_button.setEnabled(false);
	}

	/** Set the status label. */
	private void setStatusText(String s) {
		status_lbl.setText(s);
	}

	/** Are we currently streaming? */
	public boolean isStreaming() {
		return screen_pnl != null && screen_pnl.isStreaming();
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
	
	/** Update the edit mode */
	public void updateEditMode() {
		save_layout_button.setEnabled(session.canWrite("camera"));
	}
}
