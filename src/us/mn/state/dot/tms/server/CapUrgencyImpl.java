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

import java.util.Map;

import us.mn.state.dot.tms.CapUrgency;

/**
 * Common Alerting Protocol (CAP) urgency field substitution value server-side
 * implementation. Used for IPAWS alert processing for generating messages for
 * posting to DMS.
 *
 * @author Gordon Parikh
 */
public class CapUrgencyImpl extends BaseObjectImpl implements CapUrgency {
	
	/** Database table name */
	static private final String TABLE = "iris.cap_urgency";
	
	protected CapUrgencyImpl(String n) {
		super(n);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getTypeName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getColumns() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEvent(String ev) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getEvent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUrgency(String rt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getUrgency() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMulti(String m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getMulti() {
		// TODO Auto-generated method stub
		return null;
	}
	
}