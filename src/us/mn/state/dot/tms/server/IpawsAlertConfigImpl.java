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

package us.mn.state.dot.tms.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import us.mn.state.dot.tms.IpawsAlertConfig;
import us.mn.state.dot.tms.TMSException;

/**
 * IPAWS Alert Configuration object server-side implementation. Connects a
 * particular alert type ("event" field) to a number of sign group/quick
 * message pairs to control which signs are eligible for inclusion in an alert
 * and which message template to use. 
 *
 * @author Gordon Parikh
 */
public class IpawsAlertConfigImpl extends BaseObjectImpl
	implements IpawsAlertConfig {

	/** Database table name */
	static private final String TABLE = "iris.ipaws_alert_config";

	/** Lookup the IpawsAlertConfigImpl object with the given name. */
	public static IpawsAlertConfigImpl lookupConfigImpl(String name) {
		return lookupIpawsAlertConfig(name);
	}
	
	private IpawsAlertConfigImpl(ResultSet row) throws SQLException {
		this(row.getString(1),		// name
			row.getString(2),		// event
			row.getString(3),		// sign group
			row.getString(4),		// quick message
			row.getInt(5)			// after alert time
		);
	}
	
	public IpawsAlertConfigImpl(String n) {
		super(n);
	}

	public IpawsAlertConfigImpl(String n, String ev, String sg,
			String qm, int mins) {
		super(n);
		event = ev;
		sign_group = sg;
		quick_message = qm;
		after_alert_time = mins;
	}
	
	/** Get the SONAR type name */
	@Override
	public String getTypeName() {
		return SONAR_TYPE;
	}

	/** Get the database table name */
	@Override
	public String getTable() {
		return "iris." + SONAR_TYPE;
	}

	/** Load all the IPAWS alert config objects */
	static public void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, IpawsAlertConfigImpl.class);
		store.query("SELECT name, event, sign_group, quick_message, " +
				"after_alert_time FROM iris." + SONAR_TYPE + ";",
				new ResultFactory()
		{
			@Override
			public void create(ResultSet row) throws Exception {
				try {
					namespace.addObject(new IpawsAlertConfigImpl(row));
				} catch (Exception e) {
					// TODO do we need/want this??
					System.out.println(row.getString(1));
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("event", event);
		map.put("sign_group", sign_group);
		map.put("quick_message", quick_message);
		map.put("after_alert_time", after_alert_time);
		return map;
	}

	/** Alert event type */
	private String event;

	/** Set the alert event type */
	@Override
	public void setEvent(String ev) {
		event = ev;
	}

	/** Set the alert event type */
	public void doSetEvent(String ev) throws TMSException {
		if (ev != event) {
			store.update(this, "event", ev);
			setEvent(ev);
		}
	}

	/** Get the alert event type */
	@Override
	public String getEvent() {
		return event;
	}

	/** Sign group */
	private String sign_group;

	/** Set the sign group */
	@Override
	public void setSignGroup(String sg) {
		sign_group = sg;
	}

	/** Set the sign group */
	public void doSetSignGroup(String sg) throws TMSException {
		if (sg != sign_group) {
			store.update(this, "sign_group", sg);
			setSignGroup(sg);
		}
	}

	/** Get the sign group */
	@Override
	public String getSignGroup() {
		return sign_group;
	}

	/** Quick message (template) */
	private String quick_message;

	/** Set the quick message (template) */
	@Override
	public void setQuickMessage(String qm) {
		quick_message = qm;
	}

	/** Set the quick message (template) */
	public void doSetQuickMessage(String qm) throws TMSException {
		if (qm != quick_message) {
			store.update(this, "quick_message", qm);
			setQuickMessage(qm);
		}
	}

	/** Get the quick message (template) */
	@Override
	public String getQuickMessage() {
		return quick_message;
	}
	
	/** Amount of time (in minutes) to display a post-alert message after an
	 *  alert expires or an AllClear response type is sent via IPAWS. Default
	 *  is 0.
	 */
	private int after_alert_time = 0;

	/** Set amount of time (in minutes) to display a post-alert message after
	 *  an alert expires or an AllClear response type is sent via IPAWS.
	 */
	@Override
	public void setAfterAlertTime(int mins) {
		after_alert_time = mins;
	}

	/** Set amount of time (in minutes) to display a post-alert message after
	 *  an alert expires or an AllClear response type is sent via IPAWS.
	 */
	public void doSetAfterAlertTime(int mins) throws TMSException {
		if (mins != after_alert_time) {
			store.update(this, "after_alert_time", mins);
			setAfterAlertTime(mins);
		}
	}

	/** Get amount of time (in minutes) to display a post-alert message after
	 *  an alert expires or an AllClear response type is sent via IPAWS.
	 */
	@Override
	public int getAfterAlertTime() {
		return after_alert_time;
	}
	
	/** Check if the current time is past the allowed after alert time given
	 *  the time provided (which should be an alert end time).
	 */
	public boolean isPastAfterAlertTime(Date alertEnd) {
		Date now = new Date();
		if (now.after(alertEnd)) {
			long t = now.getTime() - alertEnd.getTime();
			int mins = (int) TimeUnit.MINUTES.convert(
					t, TimeUnit.MILLISECONDS);
			return mins >= after_alert_time;
		}
		return false;
	}
}
