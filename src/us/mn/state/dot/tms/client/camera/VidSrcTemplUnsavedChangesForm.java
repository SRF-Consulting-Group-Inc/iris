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
import java.awt.event.ActionEvent;

import javax.swing.JButton;

import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.widget.AbstractForm;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.ILabel;
import us.mn.state.dot.tms.utils.I18N;

/**
 * Dialog for user to confirm if they want to exit the video source template
 * editor (or change/clear the selected video source) with unsaved changes.  
 * 
 * @author Gordon Parikh
 */
@SuppressWarnings("serial")
public class VidSrcTemplUnsavedChangesForm extends AbstractForm {
	
	/** Handle to the main vid source template editor form */
	private VidSourceTemplateEditor eForm;

	/** Primary message to prompt the user. */
	private ILabel msgLbl;

	/** "Go Back" button to keep changes */
	private JButton goBackBtn;
	
	/** "Discard Changes" button to discard changes and continue */
	private JButton discardChangesBtn;
	
	protected VidSrcTemplUnsavedChangesForm(VidSourceTemplateEditor f) {
		super(I18N.get(
				"camera.video_source.template.unsaved_changes_title"));
		eForm = f;
		msgLbl = new ILabel(
				"camera.video_source.template.unsaved_changes_msg");
		Dimension d = msgLbl.getPreferredSize();
		d.width = 250;
		d.height *= 2;
		msgLbl.setPreferredSize(d);
	}
	
	private IAction goBack = new IAction(
			"camera.video_source.template.unsaved_changes_goback") {
		@Override
		protected void doActionPerformed(ActionEvent ev) throws Exception {
			// just close the form
			close(Session.getCurrent().getDesktop());
		}
	};
	
	private IAction discardChanges = new IAction(
			"camera.video_source.template.unsaved_changes_discard") {
		@Override
		protected void doActionPerformed(ActionEvent ev) throws Exception {
			
			close(Session.getCurrent().getDesktop());
		}
	};
	
}