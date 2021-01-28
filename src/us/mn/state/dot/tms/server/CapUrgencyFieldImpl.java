/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2020  SRF Consulting Group, Inc.
 * Copyright (C) 2021  Minnesota Department of Transportation
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

import us.mn.state.dot.tms.CapUrgencyField;
import us.mn.state.dot.tms.TMSException;

/**
 * Common Alerting Protocol (CAP) urgency field substitution value server-side
 * implementation.  Used for IPAWS alert processing for generating messages for
 * posting to DMS.
 *
 * @author Gordon Parikh
 * @author Douglas Lau
 */
public class CapUrgencyFieldImpl extends BaseObjectImpl
	implements CapUrgencyField
{
	/** Load all the CAP urgency substitution values */
	static public void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, CapUrgencyFieldImpl.class);
		store.query("SELECT name, event, urgency, multi FROM iris." +
			SONAR_TYPE + ";", new ResultFactory()
		{
			@Override
			public void create(ResultSet row) throws Exception {
				namespace.addObject(new CapUrgencyFieldImpl(row));
			}
		});
	}

	public CapUrgencyFieldImpl(String n) {
		super(n);
	}

	public CapUrgencyFieldImpl(String n, String ev, int u, String m) {
		super(n);
		event = ev;
		urgency = u;
		multi = m;
	}

	@Override
	public String getTypeName() {
		return SONAR_TYPE;
	}

	@Override
	public String getTable() {
		return "iris." + SONAR_TYPE;
	}

	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("event", event);
		map.put("urgency", urgency);
		map.put("multi", multi);
		return map;
	}

	private CapUrgencyFieldImpl(ResultSet row) throws SQLException {
		this(row.getString(1),  // name
		     row.getString(2),  // event
		     row.getInt(3),     // urgency
		     row.getString(4)); // MULTI
	}

	/** Applicable alert event type */
	private String event;

	/** Set the applicable alert event type */
	@Override
	public void setEvent(String ev) {
		event = ev;
	}

	/** Set the applicable alert event type */
	public void doSetEvent(String ev) throws TMSException {
		if (ev != event) {
			store.update(this, "event", ev);
			setEvent(ev);
		}
	}

	/** Get the applicable alert event type */
	@Override
	public String getEvent() {
		return event;
	}

	/** Urgency ordinal value */
	private int urgency;

	/** Set the urgency ordinal value */
	@Override
	public void setUrgency(int u) {
		urgency = u;
	}

	/** Set the urgency ordinal value */
	public void doSetUrgency(int u) throws TMSException {
		if (u != urgency) {
			store.update(this, "urgency", u);
			setUrgency(u);
		}
	}

	/** Get the urgency ordinal value */
	@Override
	public int getUrgency() {
		return urgency;
	}

	/** MULTI string that will be substituted into the message */
	private String multi;

	/** Set the MULTI string that will be substituted into the message */
	@Override
	public void setMulti(String m) {
		multi = m;
	}

	/** Set the MULTI string that will be substituted into the message */
	public void doSetMulti(String m) throws TMSException {
		if (m != multi) {
			store.update(this, "multi", m);
			setMulti(m);
		}
	}

	/** Get the MULTI string that will be substituted into the message */
	@Override
	public String getMulti() {
		return multi;
	}
}
