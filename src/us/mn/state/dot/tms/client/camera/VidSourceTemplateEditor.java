/*
 * IRIS -- Intelligent Roadway Information System
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Iterator;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import us.mn.state.dot.tms.CameraTemplate;
import us.mn.state.dot.tms.CameraTemplateHelper;
import us.mn.state.dot.tms.CameraVidSourceOrder;
import us.mn.state.dot.tms.CameraVidSourceOrderHelper;
import us.mn.state.dot.tms.VidSourceTemplate;
import us.mn.state.dot.tms.VidSourceTemplateHelper;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyTablePanel;
import us.mn.state.dot.tms.client.widget.AbstractForm;
import us.mn.state.dot.tms.client.widget.ILabel;
import us.mn.state.dot.tms.client.widget.Widgets;
import us.mn.state.dot.tms.utils.I18N;

/**
 * User interface for creating and editing video source templates
 * assigned to camera templates and their respective priority
 *
 * @author Gordon Parikh
 */
@SuppressWarnings("serial")
public class VidSourceTemplateEditor extends AbstractForm {

	/** Client Session */
	private Session session;
	
	/** Table of Video Source Templates */
	private ProxyTablePanel<VidSourceTemplate> vidSrcTemplates;
	
	/** Video Source Template Model */
	private VidSrcTemplateModel vidSrcModel;
	
	/** Currently selected video source template */
	private VidSourceTemplate selectedVidSource;
	
	/** Model for Camera Templates assigned to the selected video source */
	private final DefaultListModel<String> camTemplateModel =
		new DefaultListModel<String>();

	/** List of Camera Templates assigned to the selected video source */
	private final JList<String> camTemplateList =
			new JList<String>(camTemplateModel);
	
	/** Video Source Edit Fields Label */
	private JLabel vidSrcEditFieldLbl;
	private String vidSrcEditFieldLblPrfx;
	
	/** Video Source Edit Fields Panel */
	private JPanel vidSrcEditFieldPnl;
	
	/** Button panel */
	private JPanel buttonPnl;
	
	protected VidSourceTemplateEditor(Session s) {
		super(I18N.get("camera.video_source.template_editor"), true);
		session = s;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setPreferredSize(new Dimension(800, 500));
		vidSrcModel = new VidSrcTemplateModel(session);
		vidSrcTemplates = new ProxyTablePanel<VidSourceTemplate>(
				vidSrcModel) {
			/** Set the selected video source template when clicked on. */
			@Override
			protected void selectProxy() {
				VidSourceTemplate vst = getSelectedProxy();
				setSelectedVideoSource(vst);
			}
		};
		
		// disable selection on the list of camera templates associated with
		// the selected video source
		camTemplateList.setSelectionModel(new DisabledSelectionModel());
	}
	
	/** A class for disabling selection in a list */
	class DisabledSelectionModel extends DefaultListSelectionModel {
		@Override
		public void setSelectionInterval(int index0, int index1) {
			super.setSelectionInterval(-1, -1);
	 	}

		@Override
		public void addSelectionInterval(int index0, int index1) {
			super.setSelectionInterval(-1, -1);
		}
	}
	
	/** Check if the user is permitted to use the form. */
	static public boolean isPermitted(Session s) {
		return s.canRead(VidSourceTemplate.SONAR_TYPE);
	}
	
	/** Initialize the form */
	@Override
	protected void initialize() {
		// initialize layout
		GridBagLayout gbl = new GridBagLayout();
		JPanel gbPanel = new JPanel(gbl);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.insets = Widgets.UI.insets();
		gbc.ipadx = 10;
		gbc.ipady = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		
		/* Video Source Template Label */
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbPanel.add(new ILabel("camera.video_source.templates"), gbc);
		
		/* Camera Templates Using Source Label */
		gbc.gridx = 1;
		gbPanel.add(new ILabel("camera.video_source.camera_templates"), gbc);
		
		/* Video Source Template Table (ProxyTableForm) */
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.BOTH;
		vidSrcTemplates.initialize();
		JScrollPane vstPn = new JScrollPane(vidSrcTemplates,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		vstPn.setMinimumSize(new Dimension(600, 200));
		gbPanel.add(vstPn, gbc);
		
		/* Camera Template Table */
		gbc.gridx = 1;
		JScrollPane ctPn = new JScrollPane(camTemplateList,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		ctPn.setMinimumSize(new Dimension(150, 150));
		gbPanel.add(ctPn, gbc);
		
		/* Video Source Edit Fields Label */
		vidSrcEditFieldLblPrfx = I18N.get("camera.video_source.edit_fields");
		vidSrcEditFieldLbl = new JLabel(vidSrcEditFieldLblPrfx);
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbPanel.add(vidSrcEditFieldLbl, gbc);
		
		/* Video source Edit Fields Panel */
		vidSrcEditFieldPnl = new JPanel();
		vidSrcEditFieldPnl.setPreferredSize(new Dimension(550,150));
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridy = 3;
		gbPanel.add(vidSrcEditFieldPnl, gbc);
		
		/* Button panel */
		buttonPnl = new JPanel();
		gbc.gridy = 4;
		gbPanel.add(buttonPnl, gbc);
		
		add(gbPanel);
	}
	
	/** Dispose of the form */
	@Override
	public void dispose() {
		vidSrcTemplates.dispose();
		super.dispose();
	}
	
	/** Handle the selection of a video source template from the table */
	private void setSelectedVideoSource(VidSourceTemplate vst) {
		selectedVidSource = vst;
		
		// update various things
		if (selectedVidSource != null) {
			// update the label
			vidSrcEditFieldLbl.setText(vidSrcEditFieldLblPrfx +
					" " + selectedVidSource.getLabel());
			
			// update the list of camera templates associated with this video
			// source
			camTemplateModel.clear();
			Iterator<CameraVidSourceOrder> it =
					CameraVidSourceOrderHelper.iterator();
			while (it.hasNext()) {
				CameraVidSourceOrder cvso = it.next();
				String vstName = cvso.getVidSourceTemplate();
				if (vstName.equals(selectedVidSource.getName())) {
					CameraTemplate ct = CameraTemplateHelper.lookup(
							cvso.getCameraTemplate());
					camTemplateModel.addElement(ct.getLabel());
				}
			}
		} else {
			vidSrcEditFieldLbl.setText(vidSrcEditFieldLblPrfx);
			camTemplateModel.clear();
		}
		
		
		// TODO button state changes
	}
}



















