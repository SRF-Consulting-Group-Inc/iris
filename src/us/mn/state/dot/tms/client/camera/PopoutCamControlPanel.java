/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2014-2016  Minnesota Department of Transportation
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

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.MutableComboBoxModel;

import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.Camera;
import us.mn.state.dot.tms.CameraPreset;
import us.mn.state.dot.tms.CameraPresetHelper;
import us.mn.state.dot.tms.PlayList;
import us.mn.state.dot.tms.VideoMonitor;
import us.mn.state.dot.tms.VideoMonitorHelper;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyView;
import us.mn.state.dot.tms.client.proxy.ProxyWatcher;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.IComboBoxModel;

import static us.mn.state.dot.tms.client.widget.Widgets.UI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

/**
 * Camera control panel.
 *
 * @author Douglas Lau
 */
public class PopoutCamControlPanel extends JPanel {

	/** User session */
	private final Session session;

	/** Panel for zoom control */
	private final ZoomPanel zoom_pnl;

	/** Camera PTZ control */
	private final CameraPTZ camera_ptz;
	
	/** Video output selection ComboBox */
	private final JComboBox<VideoMonitor> monitor_cbx;
	
	/** Selected video monitor output */
	private VideoMonitor video_monitor;
	
	/** Video monitor watcher */
	private final ProxyWatcher<VideoMonitor> watcher;
	

	/** Video monitor view */
	private final ProxyView<VideoMonitor> vm_view =
		new ProxyView<VideoMonitor>()
	{
		public void enumerationComplete() { }
		public void update(VideoMonitor vm, String a) {
			video_monitor = vm;
		}
		public void clear() {
			video_monitor = null;
		}
	};
	
	/** Cache of video monitor objects */
	private final TypeCache<VideoMonitor> vm_cache;
	
	/** Camera preset combo box */
	private final JComboBox<CameraPreset> preset_cbx =
		new JComboBox<CameraPreset>();

	/** Camera preset combo box model */
	private final DefaultComboBoxModel<CameraPreset> preset_mdl;
	
	/** Camera preset action */
	private final IAction preset_act = new IAction("camera.preset") {
		protected void doActionPerformed(ActionEvent e) {
			CameraPreset cp = (CameraPreset) preset_cbx.getSelectedItem();
			camera_ptz.recallPreset(cp.getPresetNum());
			preset_cbx.setEditable(false);
		}
	};
	
	/** Video monitor action */
	private final IAction monitor_act = new IAction("video.monitor") {
		protected void doActionPerformed(ActionEvent e) {
		}
	};

	/** Create a new camera control panel */
	public PopoutCamControlPanel(CameraPTZ cam_ptz) {
		session = Session.getCurrent();
		camera_ptz = cam_ptz;
		zoom_pnl = new ZoomPanel(cam_ptz);
	
		preset_mdl = createPresetModel(camera_ptz);
		preset_cbx.setModel(preset_mdl);
		preset_cbx.setEditable(true);
		preset_cbx.setSelectedItem("Camera Preset");
		preset_cbx.setAction(preset_act);
		preset_cbx.setRenderer(new PresetComboRendererLong());
		
		monitor_cbx = createMonitorCombo();
		monitor_cbx.setEditable(true);
		monitor_cbx.setSelectedItem("Video Monitor");
		monitor_cbx.setRenderer(new MonComboRendererLong());
		monitor_cbx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				monitor_cbx.setEditable(false);
				monitorSelected();
			}
		});
		monitor_cbx.setAction(monitor_act);
		vm_cache = session.getSonarState().getCamCache()
                .getVideoMonitors();
		watcher = new ProxyWatcher<VideoMonitor>(vm_cache,vm_view,true);
		
		layoutPanel();


	}
	/** Create the video output selection combo box */
	private JComboBox<VideoMonitor> createMonitorCombo() {
		JComboBox<VideoMonitor> box = new JComboBox<VideoMonitor>();
		DefaultComboBoxModel<VideoMonitor> cbxm = new DefaultComboBoxModel<VideoMonitor>();
		Iterator<VideoMonitor> it = VideoMonitorHelper.iterator();
		while (it.hasNext()) {
			cbxm.addElement(it.next());
		}
		box.setModel(cbxm);
		return box;
//		FilteredMonitorModel m = FilteredMonitorModel.create(session);
//		box.setModel(new IComboBoxModel<VideoMonitor>(m));
//		if (m.getSize() > 1)
//			box.setSelectedIndex(1);
//		return box;
	}
	
	
	/** Create the camera preset model */
	private DefaultComboBoxModel<CameraPreset> createPresetModel(CameraPTZ cam_ptz) {
		Camera c = cam_ptz.getCamera();
		DefaultComboBoxModel<CameraPreset> cbxm = new DefaultComboBoxModel<CameraPreset>();
		if (c != null) {
			cam_ptz.setCamera(c);
			for (int i = 1; i <= CameraPreset.MAX_PRESET; ++i) {
				CameraPreset cp = CameraPresetHelper.lookup(c, i);
				if (cp != null)
					cbxm.addElement(cp);
			}
		}
		return cbxm;
	}


	/** Layout the panel */
	private void layoutPanel() {
		GroupLayout gl = new GroupLayout(this);
		gl.setHonorsVisibility(false);
		gl.setAutoCreateGaps(false);
		gl.setAutoCreateContainerGaps(false);
		gl.setHorizontalGroup(createHorizontalGroup(gl));
		gl.setVerticalGroup(createVerticalGroup(gl));
		setLayout(gl);
	}

	/** Create the horizontal group */
	private GroupLayout.Group createHorizontalGroup(GroupLayout gl) {
		GroupLayout.SequentialGroup hg = gl.createSequentialGroup();
		hg.addComponent(zoom_pnl);
		hg.addGap(UI.hgap);
		hg.addComponent(preset_cbx);
		hg.addGap(UI.hgap);
		hg.addComponent(monitor_cbx);
		return hg;
	}

	/** Create the vertical group */
	private GroupLayout.Group createVerticalGroup(GroupLayout gl) {
		GroupLayout.ParallelGroup vg = gl.createParallelGroup(
			GroupLayout.Alignment.CENTER);
		vg.addComponent(zoom_pnl);
		vg.addComponent(preset_cbx);
		vg.addComponent(monitor_cbx);
		return vg;
	}

	/** Dispose of the camera control panel */
	public void dispose() {
		removeAll();
	}

	/** Set enabled status */
	@Override
	public void setEnabled(boolean e) {
		super.setEnabled(e);
		zoom_pnl.setEnabled(e);
		preset_cbx.setEnabled(e);
		monitor_cbx.setEnabled(e);
	}
	
	/** Called when a video monitor is selected */
	private void monitorSelected() {
		watcher.setProxy(getSelectedOutput());
	}

	/** Get the selected video monitor from UI */
	private VideoMonitor getSelectedOutput() {
		Object o = monitor_cbx.getSelectedItem();
		return (o instanceof VideoMonitor) ? (VideoMonitor) o : null;
	}


}
