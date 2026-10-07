/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2026  SRF Consulting Group
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
package us.mn.state.dot.tms.server.event;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import us.mn.state.dot.tms.EventType;

/**
 * This is a class for logging NDDOT VSL events to a database.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslEvent extends BaseEvent {

	/** VSL sign name */
	private final String vsl_sign;

	/** Speed (mph), or null */
	private final Integer speed;

	/** User who made a decision, or null */
	private final String user_id;

	/** Create a new VSL event */
	public VslEvent(EventType et, String vs, Integer s, String u) {
		super(et);
		vsl_sign = vs;
		speed = s;
		user_id = u;
	}

	/** Get the event config name */
	@Override
	protected String eventConfigName() {
		return "vsl_event";
	}

	/** Get the database table name */
	@Override
	public String getTable() {
		return "event.vsl_event";
	}

	/** Get a mapping of the columns */
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("event_date", new Timestamp(event_date.getTime()));
		map.put("event_desc", event_type.id);
		map.put("vsl_sign", vsl_sign);
		map.put("speed", speed);
		map.put("user_id", user_id);
		return map;
	}
}
