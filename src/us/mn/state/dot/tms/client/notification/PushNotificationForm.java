/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2020  SRF Consulting Group
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

package us.mn.state.dot.tms.client.notification;

import javax.swing.JInternalFrame;

import us.mn.state.dot.tms.PushNotification;
import us.mn.state.dot.tms.client.ScreenPane;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.map.MapBean;
import us.mn.state.dot.tms.client.proxy.ProxyTableForm;
import us.mn.state.dot.tms.utils.I18N;

/**
 * Form for displaying push notifications indicating something requiring user
 * interaction. Note that none of these fields are editable.
 *
 * @author Gordon Parikh
 */
@SuppressWarnings("serial")
public class PushNotificationForm extends ProxyTableForm<PushNotification> {
	
	/** Check if the user is permitted to use the form */
	static public boolean isPermitted(Session s) {
		return s.canRead(PushNotification.SONAR_TYPE);
	}
	
	/** Create a new Push Notification form */
	public PushNotificationForm(Session s, MapBean map, ScreenPane p) {
		super(I18N.get("notification"), new PushNotificationProxyPanel(
				new PushNotificationModel(s), map, p));
		((PushNotificationProxyPanel) panel).setForm(this);
	}
}
