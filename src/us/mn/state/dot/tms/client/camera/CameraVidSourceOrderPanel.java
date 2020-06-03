/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2017-2018  Minnesota Department of Transportation
 * Copyright (C) 2020 SRF Consulting Group
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.CameraTemplate;
import us.mn.state.dot.tms.CameraVidSourceOrder;
import us.mn.state.dot.tms.CameraVidSourceOrderHelper;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.Gps;
import us.mn.state.dot.tms.VidSourceTemplate;
import us.mn.state.dot.tms.VidSourceTemplateHelper;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyView;
import us.mn.state.dot.tms.client.proxy.ProxyWatcher;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.ILabel;
import us.mn.state.dot.tms.client.widget.IListSelectionAdapter;
import us.mn.state.dot.tms.client.widget.Icons;
import us.mn.state.dot.tms.server.CameraVidSourceOrderImpl;

import static us.mn.state.dot.tms.client.widget.Widgets.UI;
import us.mn.state.dot.tms.utils.I18N;

/**
 * CameraVidSourceOrder is a UI for editing the video source templates
 * assigned to camera templates and their respective priority
 *
 * @author Douglas Lau
 * @author Michael Janson
 */
public class CameraVidSourceOrderPanel extends JPanel
		implements ProxyView<CameraVidSourceOrder>{

	/** Parse an integer */
	static private Integer parseInt(String t) {
		try {
			return Integer.parseInt(t);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	/** User Session */
	private final Session session;
	
	/** Cache of CameraVidSourceOrder objects */
	private final TypeCache<CameraVidSourceOrder> cache;

	/** Proxy watcher */
	private final ProxyWatcher<CameraVidSourceOrder> watcher;

	/** Camera template */
	private final CameraTemplate camera_template;
	
	private List<CameraVidSourceOrder> cam_vid_src;
	
	/** Camera template video source list model */
	private final DefaultListModel<VidSourceTemplate> cam_vid_src_mdl =
		new DefaultListModel<VidSourceTemplate>();

	/** Camera template video source list */
	private final JList<VidSourceTemplate> cam_vid_src_lst =
			new JList<VidSourceTemplate>(cam_vid_src_mdl);

	/** Camera template video source scroll pane */
	private final JScrollPane cam_vid_src_scrl =
			new JScrollPane(cam_vid_src_lst);
	
	/** Available video source list model */
	private final DefaultListModel<VidSourceTemplate> vid_src_mdl =
		new DefaultListModel<VidSourceTemplate>();

	/** Available video source list */
	private final JList<VidSourceTemplate> vid_src_lst =
			new JList<VidSourceTemplate>(vid_src_mdl);

	/** Available template video source scroll pane */
	private final JScrollPane vid_src_scrl = new JScrollPane(vid_src_lst);

	/** Insert video source button */
	private final JButton insert_btn = new JButton(
			new IAction("camera.template.source.add")
	{
		protected void doActionPerformed(ActionEvent e) {
			insertVideoSource();
		}
	});
	
	/** Video source info */
	private final JTextArea vid_src_info =
			new JTextArea(14, 20);
	
	/** Camera video source label */
	private final ILabel cam_vid_src_lbl =
			new ILabel("camera.template.sources");
	
	/** Available video source label */
	private final ILabel avail_vid_src_lbl =
			new ILabel("camera.template.available_sources"); 
	
	/** Video source info */
	private final ILabel vid_src_info_lbl =
			new ILabel("camera.template.source.info");

	/** Insert a video source */
	private void insertVideoSource() {
		VidSourceTemplate vst = vid_src_lst.getSelectedValue();
		if (vst != null) {
			cam_vid_src_mdl.addElement(vst);
			cam_vid_src_lst.setSelectedIndex(cam_vid_src_mdl.size() - 1);
			String src_name = vst.getName();
			int src_order = cam_vid_src_mdl.size() - 1;
			String n = camera_template + "_" + Integer.toString(src_order);
			cache.createObject(n);
			CameraVidSourceOrder cvo = cache.lookupObjectWait(n);
			cvo.setCameraTemplate(camera_template.getName());
			cvo.setVidSourceTemplate(src_name);
			cvo.setSourceOrder(src_order);
			cam_vid_src.add(cvo);
		}
	}
	
	/** Remove video source button */
	private final JButton remove_btn = new JButton(
			new IAction("camera.template.source.remove")
	{
		protected void doActionPerformed(ActionEvent e) {
			int s = cam_vid_src_lst.getSelectedIndex();
			VidSourceTemplate vst = cam_vid_src_lst.getSelectedValue();
			if (s >= 0) {
				cam_vid_src_mdl.remove(s);
				String n = camera_template + "_" + Integer.toString(s);
				CameraVidSourceOrder cvo = cache.lookupObject(n);
				cvo.destroy();
				cam_vid_src.remove(cvo);
			}
		}
	});

	/** Up button */
	private final JButton up_btn = new JButton(
			new IAction("camera.template.source.up")
	{
		protected void doActionPerformed(ActionEvent e) {
			moveVidSrcUp();
		}
	});

	/** Move selected camera up */
	private void moveVidSrcUp() {
		int s = cam_vid_src_lst.getSelectedIndex();
		if (s > 0) {
			VidSourceTemplate vst0 = cam_vid_src_mdl.get(s - 1);
			VidSourceTemplate vst1 = cam_vid_src_mdl.get(s);
			cam_vid_src_mdl.set(s - 1, vst1);
			cam_vid_src_mdl.set(s, vst0);
			cam_vid_src_lst.setSelectedIndex(s - 1);
			CameraVidSourceOrder cmvo1 = cam_vid_src.get(s-1);
			CameraVidSourceOrder cmvo2 = cam_vid_src.get(s);
			cmvo1.setSourceOrder(s);
			cmvo2.setSourceOrder(s-1);
			cam_vid_src.set(s, cmvo1);
			cam_vid_src.set(s-1, cmvo2);
			}
	}

	/** Down action */
	private final JButton down_btn = new JButton(
			new IAction("camera.template.source.down")
	{
		protected void doActionPerformed(ActionEvent e) {
			moveCameraDown();
		}
	});
	
	/** Display information for video source */
	private void displayVidSrcInfo(VidSourceTemplate vst) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("Name: ");
		sb.append(checkNull(vst.getLabel()));
		sb.append("\n");
		
		sb.append("Configuration: ");
		sb.append(checkNull(vst.getConfig()));
		sb.append("\n");

		sb.append("Codec: ");
		sb.append(checkNull(vst.getCodec()));
		sb.append("\n");
		
		sb.append("Encoder: ");
		sb.append(checkNull(vst.getEncoder()));
		sb.append("\n");
		
		sb.append("Notes: ");
		sb.append(checkNull(vst.getNotes()));
		sb.append("\n");
		
		sb.append("Scheme: ");
		sb.append(checkNull(vst.getScheme()));
		sb.append("\n");
		
		sb.append("Subnets: ");
		sb.append(checkNull(vst.getSubnets()));
		sb.append("\n");
		
		sb.append("Type Name: ");
		sb.append(checkNull(vst.getTypeName()));
		sb.append("\n");
		
		sb.append("Default Port: ");
		sb.append(checkNull(vst.getDefaultPort()));
		sb.append("\n");
		
		sb.append("Latency: ");
		sb.append(checkNull(vst.getLatency()));
		sb.append("\n");
		
		sb.append("Resolution Height: ");
		sb.append(checkNull(vst.getRezHeight()));
		sb.append("\n");
		
		sb.append("Resolution Width: ");
		sb.append(checkNull(vst.getRezWidth()));
		
		vid_src_info.setText(sb.toString());
	}
	
	/** Return blank string if field is null */
	private static String checkNull(String s) {
		return s != null ? s : "";
	}
	
	/** Return blank string if field is null */
	private static String checkNull(Integer i) {
		return i != null ? i.toString() : "";
	}

	/** Move selected camera down */
	private void moveCameraDown() {
		int s = cam_vid_src_lst.getSelectedIndex();
		if (s >= 0 && s < cam_vid_src_mdl.size() - 1) {
			VidSourceTemplate vst0 = cam_vid_src_mdl.get(s);
			VidSourceTemplate vst1 = cam_vid_src_mdl.get(s + 1);
			cam_vid_src_mdl.set(s, vst1);
			cam_vid_src_mdl.set(s + 1, vst0);
			cam_vid_src_lst.setSelectedIndex(s + 1);
			CameraVidSourceOrder cmvo1 = cam_vid_src.get(s);
			CameraVidSourceOrder cmvo2 = cam_vid_src.get(s+1);
			cmvo1.setSourceOrder(s+1);
			cmvo2.setSourceOrder(s);
			cam_vid_src.set(s+1, cmvo1);
			cam_vid_src.set(s, cmvo2);
		}
	}

	/** Create a new play list panel */
	public CameraVidSourceOrderPanel(Session s, CameraTemplate ct) {
		session = s;
		camera_template = ct;
		cache = s.getSonarState().getCamVidSrcOrder();
		watcher = new ProxyWatcher<CameraVidSourceOrder>(cache, this, false);
		cam_vid_src = VidStreamReq.getCamVidSrcOrder(camera_template);
	}

	/** Initialize the widgets */
	public void initialize() {
		setBorder(UI.border);
		cam_vid_src_lst.setVisibleRowCount(12);
		vid_src_lst.setVisibleRowCount(12);
		cam_vid_src_scrl.setPreferredSize(new Dimension(150,200));
		vid_src_scrl.setPreferredSize(new Dimension(150,200));
		vid_src_info.setEditable(false);
		vid_src_info.setBackground(UIManager.getColor("Panel.background"));
		vid_src_info.setLineWrap(true);
		ImageIcon insert_icon = Icons.getIconByPropName(
				"camera.template.source.add");
		insert_btn.setIcon(insert_icon);
		insert_btn.setHideActionText(true);
		insert_btn.setToolTipText(I18N.get("camera.template.source.add"));
		remove_btn.setText("X");
		remove_btn.setFont(
				new java.awt.Font("Arial", java.awt.Font.PLAIN, 18));
		remove_btn.setToolTipText(I18N.get("camera.template.source.remove"));
		
		ImageIcon up_icon = Icons.getIconByPropName(
				"camera.template.source.up");
		up_btn.setIcon(up_icon);
		up_btn.setHideActionText(true);
		ImageIcon down_icon = Icons.getIconByPropName(
				"camera.template.source.down");
		down_btn.setIcon(down_icon);
		down_btn.setHideActionText(true);
		
		layoutPanel();
		initializeVidSrc();
		createJobs();
		watcher.initialize();
	}

	/** Layout the panel */
	private void layoutPanel() {
		GroupLayout gl = new GroupLayout(this);
		gl.setHonorsVisibility(false);
		gl.setAutoCreateGaps(false);
		gl.setAutoCreateContainerGaps(false);
		gl.setHorizontalGroup(createHorizontalGroup(gl));
		gl.setVerticalGroup(createVerticalGroup(gl));
		gl.linkSize(insert_btn, remove_btn, up_btn, down_btn);
		setLayout(gl);
	}

	/** Create the horizontal group */
	private GroupLayout.Group createHorizontalGroup(GroupLayout gl) {
		GroupLayout.SequentialGroup hg = gl.createSequentialGroup();
		
		GroupLayout.ParallelGroup p1 = gl.createParallelGroup(
			GroupLayout.Alignment.CENTER);
		p1.addComponent(cam_vid_src_lbl);
		p1.addComponent(cam_vid_src_scrl);
		gl.linkSize(SwingConstants.HORIZONTAL, cam_vid_src_scrl);
		hg.addGroup(p1);
		
		hg.addGap(UI.hgap);

		GroupLayout.ParallelGroup p2 = gl.createParallelGroup(
				GroupLayout.Alignment.TRAILING);
		p2.addComponent(up_btn);
		p2.addComponent(insert_btn);
		p2.addComponent(remove_btn);
		p2.addComponent(down_btn);
		hg.addGroup(p2);
		
		hg.addGap(UI.hgap);

		GroupLayout.ParallelGroup p3 = gl.createParallelGroup(
				GroupLayout.Alignment.CENTER);
		p3.addComponent(avail_vid_src_lbl);
		p3.addComponent(vid_src_scrl);
		gl.linkSize(SwingConstants.HORIZONTAL, vid_src_scrl);
		hg.addGroup(p3);
		
		GroupLayout.ParallelGroup pg = gl.createParallelGroup(
				GroupLayout.Alignment.LEADING);
		pg.addComponent(vid_src_info_lbl);
		pg.addComponent(vid_src_info);
		pg.addGroup(hg);
		
		return pg;
	}

	/** Create the vertical group */
	private GroupLayout.Group createVerticalGroup(GroupLayout gl) {
		GroupLayout.SequentialGroup vg = gl.createSequentialGroup();
		
		GroupLayout.ParallelGroup p0 = gl.createParallelGroup(
				GroupLayout.Alignment.CENTER);
		
		p0.addComponent(cam_vid_src_lbl);
		p0.addComponent(avail_vid_src_lbl);
		
		GroupLayout.ParallelGroup p1 = gl.createParallelGroup(
			GroupLayout.Alignment.CENTER);
		
		GroupLayout.SequentialGroup v0 = gl.createSequentialGroup();
		v0.addComponent(up_btn);
		v0.addGap(10*UI.vgap);
		v0.addComponent(insert_btn);
		v0.addGap(UI.vgap);
		v0.addComponent(remove_btn);
		v0.addGap(10*UI.vgap);
		v0.addComponent(down_btn);
		p1.addGroup(v0);
		
		p1.addComponent(cam_vid_src_scrl);
		p1.addComponent(vid_src_scrl);
		
		GroupLayout.ParallelGroup p2 = gl.createParallelGroup(
				GroupLayout.Alignment.CENTER);
		
		p2.addComponent(vid_src_info_lbl);
		
		GroupLayout.ParallelGroup p3 = gl.createParallelGroup(
				GroupLayout.Alignment.CENTER);
		
		p3.addComponent(vid_src_info);
		
		vg.addGroup(p0);
		vg.addGroup(p1);
		vg.addGroup(p2);
		vg.addGroup(p3);
		return vg;
	}

	/** Create jobs */
	private void createJobs() {
		ListSelectionModel cvs = cam_vid_src_lst.getSelectionModel();
		cvs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cvs.addListSelectionListener(new IListSelectionAdapter() {
			@Override
			public void valueChanged() {
				if (isCamSrcSelected()) {
					vid_src_lst.clearSelection();
					displayVidSrcInfo(cam_vid_src_lst.getSelectedValue());
				}
				updateButtons();
			}
		});
		
		ListSelectionModel vs = vid_src_lst.getSelectionModel();
		vs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		vs.addListSelectionListener(new IListSelectionAdapter() {
			@Override
			public void valueChanged() {
				if (isVidSrcSelected()) {
					cam_vid_src_lst.clearSelection();
					displayVidSrcInfo(vid_src_lst.getSelectedValue());
				}
				updateButtons();
			}
		});
	}

	/** Update the edit mode */
	public void updateEditMode() {
		boolean ud = canWrite("camera_template");
		updateButtons();
	}

	/** Update buttons */
	private void updateButtons() {
		boolean ud = canWrite("camera_template");
		insert_btn.setEnabled(ud && isVidSrcSelected());
		remove_btn.setEnabled(ud && isCamSrcSelected());
		up_btn.setEnabled(ud && isCamSrcSelected()&& canMoveUp());
		down_btn.setEnabled(ud && isCamSrcSelected() && canMoveDown());
	}
	
	//Method to check is already exists

	/** Check if the user can write an attribute */
	private boolean canWrite(String attr) {
		return session.canWrite(camera_template, attr);
	}

	/** Check if a camera video source is selected */
	private boolean isCamSrcSelected() {
		return cam_vid_src_lst.getSelectedIndex() >= 0;
	}
	
	/** Check if a video source is selected */
	private boolean isVidSrcSelected() {
		return vid_src_lst.getSelectedIndex() >= 0;
	}

	/** Check if selected video source can be moved up */
	private boolean canMoveUp() {
		return cam_vid_src_lst.getSelectedIndex() > 0;
	}

	/** Check if selected video source can be moved down */
	private boolean canMoveDown() {
		int s = cam_vid_src_lst.getSelectedIndex();
		return (s >= 0 && s < cam_vid_src_mdl.size() - 1);
	}

	/** Update one attribute on the form */
	public void updateAttribute(String a) {
		if (null == a || a.equals("camera_template"))
			updateCamVidSrc();
	}

	/** Update the camera video source list */
	private void updateCamVidSrc() {
		cam_vid_src = VidStreamReq.getCamVidSrcOrder(camera_template);
		if (cam_vid_src != null) {
			for (int i = 0; i < cam_vid_src.size(); i++) {
				VidSourceTemplate vst = lookupVidSource(cam_vid_src.get(i));
				if (i < cam_vid_src_mdl.size())
					cam_vid_src_mdl.set(i, vst);
				else
					cam_vid_src_mdl.addElement(vst);
			}
			for (int i = cam_vid_src.size(); i < cam_vid_src_mdl.size(); i++)
				cam_vid_src_mdl.remove(i);
		}
	}
	
	/** Initialize video source list */
	private void initializeVidSrc() {
		Iterator<VidSourceTemplate> it = VidSourceTemplateHelper.iterator();
		while (it.hasNext())
			vid_src_mdl.addElement(it.next());
	}

	
	private static VidSourceTemplate lookupVidSource(CameraVidSourceOrder cvso) {
		return VidSourceTemplateHelper.lookup(cvso.getVidSourceTemplate());
	}

	@Override
	public void enumerationComplete() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(CameraVidSourceOrder p, String a) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}
}
