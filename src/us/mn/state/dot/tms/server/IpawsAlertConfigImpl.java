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
import java.util.HashMap;
import java.util.Map;

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
	
	/** Name prefix */
	static private final String NAME_PREFIX = "ipaws_cfg_";

	private IpawsAlertConfigImpl(ResultSet row) throws SQLException {
		this(row.getString(1),		// name
			row.getString(2),		// event
			row.getString(3),		// sign group
			row.getString(4)		// quick message
		);
	}
	
	public IpawsAlertConfigImpl(String n) {
		super(n);
	}

	public IpawsAlertConfigImpl(String n, String ev, String sg, String qm) {
		super(n);
		event = ev;
		sign_group = sg;
		quick_message = qm;
	}
	
	/** Get the SONAR type name */
	@Override
	public String getTypeName() {
		return SONAR_TYPE;
	}

	/** Get the database table name */
	@Override
	public String getTable() {
		return "event." + SONAR_TYPE;
	}

	/** Load all the IPAWS alert config objects */
	static public void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, IpawsAlertConfigImpl.class);
		store.query("SELECT name, event, sign_group, quick_message " +
				"FROM iris." + SONAR_TYPE + ";", new ResultFactory()
		{
			@Override
			public void create(ResultSet row) throws Exception {
				try {
					namespace.addObject(new IpawsAlertConfigImpl(row));
				} catch (Exception e) {
					// TODO do we need/want this??
					System.out.println(row.getString(1));
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
		return map;
	}

	/** Alert event type */
	private String event;

	/** Set the alert event type */
	@Override
	public void setEvent(String ev) {
		event = ev;
	}

	// TODO doSet...()

	/** Get the alert event type */
	@Override
	public String getEvent() {
		// TODO Auto-generated method stub
		return null;
	}

	/** Sign group */
	private String sign_group;

	/** Set the sign group */
	@Override
	public void setSignGroup(String sg) {
		sign_group = sg;
	}

	// TODO doSet...()

	/** Get the sign group */
	@Override
	public String getSignGroup(String sg) {
		return sign_group;
	}

	/** Quick message (template) */
	private String quick_message;

	/** Set the quick message (template) */
	@Override
	public void setQuickMessage(String qm) {
		quick_message = qm;
	}

	// TODO doSet...()

	/** Get the quick message (template) */
	@Override
	public String getQuickMessage(String qm) {
		return quick_message;
	}

}