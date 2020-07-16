/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2020  SRF Consulting Group, Inc.
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
 
package us.mn.state.dot.tms.client.toolbar;

import java.awt.event.ActionEvent;
import javax.swing.JButton;

import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.widget.IAction;

/**
 * A tool panel that opens a notification manager.
 *
 * @author Gordon Parikh
 */
@SuppressWarnings("serial")
public class NotificationPanel extends ToolPanel {
	
	/** Is this panel IRIS enabled? */
	static public boolean getIEnabled() {
		return true;
	}

	/** User session */
	private final Session session;
	
	/** Button to open notification manager */
	private final JButton notificationBtn;
	
	/** Action to open notification manager */
	private final IAction openNotifMgr = new IAction("notification") {
		@Override
		protected void doActionPerformed(ActionEvent ev) throws Exception {
			// TODO for now just printing
			System.out.println("Opening notification manager...");
		}
	};
	
	public NotificationPanel(Session s) {
		session = s;
		notificationBtn = new JButton(openNotifMgr);
		add(notificationBtn);
	}

}