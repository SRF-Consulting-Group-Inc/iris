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
package us.mn.state.dot.tms.client.alert;

import java.awt.BorderLayout;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import us.mn.state.dot.tms.IpawsAlertDeployer;
import us.mn.state.dot.tms.client.MapTab;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxySelectionListener;
import us.mn.state.dot.tms.client.proxy.ProxySelectionModel;
import us.mn.state.dot.tms.client.proxy.StyleSummary;

/**
 * The AlertTab class provides the GUI for working with automated alert
 * objects, e.g. weather (and other) alerts from IPAWS.
 * 
 * NOTE this would need changing to let the alert tab handle other types of
 * alerts (we would need a new Alert parent SONAR object - not sure what that
 * would look like yet). 
 *
 * @author Gordon Parikh
 */

@SuppressWarnings("serial")
public class AlertTab extends MapTab<IpawsAlertDeployer> {
	
	/** Summary of alerts */
	private final StyleSummary<IpawsAlertDeployer> summary;
	
	/** Alert dispatcher for dispatching and reviewing alerts */
	private final AlertDispatcher dispatcher;
	
	protected AlertTab(Session session, AlertManager man) {
		super(man);
		summary = man.createStyleSummary(false, 1);
		dispatcher = new AlertDispatcher(session, man);
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.add(summary);
		p.add(dispatcher);
		add(p, BorderLayout.NORTH);
	}
	
	/** Initialize the alert tab. */
	@Override
	public void initialize() {
		summary.initialize();
		dispatcher.initialize();
	}
	
	/** Dispose of the alert tab. */
	@Override
	public void dispose() {
		super.dispose();
		summary.dispose();
	}
	
	@Override
	public String getTabId() {
		return "alert";
	}
	
}
