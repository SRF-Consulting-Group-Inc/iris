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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

import us.mn.state.dot.tms.PushNotification;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyColumn;
import us.mn.state.dot.tms.client.proxy.ProxyDescriptor;
import us.mn.state.dot.tms.client.proxy.ProxyTableModel;

/**
 * Table model for push notifications indicating something requiring user
 * interaction. Note that none of these fields are editable.
 *
 * @author Gordon Parikh
 */
@SuppressWarnings("serial")
public class PushNotificationModel extends ProxyTableModel<PushNotification> {
	
	/** Create a proxy descriptor */
	static public ProxyDescriptor<PushNotification> descriptor(Session s) {
		return new ProxyDescriptor<PushNotification>(
				s.getSonarState().getPushNotificationCache(),
				false, false, false);
	}
	
	/** Create the columns in the model */
	@Override
	protected ArrayList<ProxyColumn<PushNotification>> createColumns() {
		ArrayList<ProxyColumn<PushNotification>> cols =
				new ArrayList<ProxyColumn<PushNotification>>();
		cols.add(new ProxyColumn<PushNotification>("notification.title", 150) {
			@Override
			public Object getValueAt(PushNotification pn) {
				return pn.getTitle();
			}
		});
		cols.add(new ProxyColumn<PushNotification>(
				"notification.description", 400) {
			@Override
			public Object getValueAt(PushNotification pn) {
				return pn.getDescription();
			}
		});
		cols.add(new ProxyColumn<PushNotification>("notification.sent", 150) {
			@Override
			public Object getValueAt(PushNotification pn) {
				// show the time since the notification was sent
				Date sentTime = pn.getSentTime();
				if (sentTime != null) {
					LocalDateTime st = sentTime.toInstant().atZone(
							ZoneId.systemDefault()).toLocalDateTime();
					Duration d = Duration.between(LocalDateTime.now(), st);
					return d.toString(); 
				}
				return "";
			}
		});
		// NOTE we don't show the object type/name (assume it is indicated
		// in the title/description if needed)
		return cols;
	}
	
	public PushNotificationModel(Session s) {
		super(s, descriptor(s), 12);
	}
}
