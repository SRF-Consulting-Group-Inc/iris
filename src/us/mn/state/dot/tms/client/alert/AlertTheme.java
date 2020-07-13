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

import java.awt.Color;

import us.mn.state.dot.tms.IpawsAlertDeployer;
import us.mn.state.dot.tms.ItemStyle;
import us.mn.state.dot.tms.client.incident.IncidentMarker;
import us.mn.state.dot.tms.client.proxy.ProxyManager;
import us.mn.state.dot.tms.client.proxy.ProxyTheme;

/**
 * Theme for alert objects on the map.
 *
 * @author Gordon Parikh
 */
public class AlertTheme extends ProxyTheme<IpawsAlertDeployer> {
	public AlertTheme(ProxyManager<IpawsAlertDeployer> m) {
		// TODO for now we're stealing the incident marker
		super(m,  new IncidentMarker());
		addStyle(ItemStyle.ACTIVE, ProxyTheme.COLOR_AVAILABLE);
		addStyle(ItemStyle.PAST, ProxyTheme.COLOR_INACTIVE);
		addStyle(ItemStyle.ALL, Color.WHITE);
	}
	
}