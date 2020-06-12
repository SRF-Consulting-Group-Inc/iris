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

import us.mn.state.dot.sonar.SonarObject;
import us.mn.state.dot.tms.IpawsAlertNotifier;
import us.mn.state.dot.tms.IpawsAlertNotifierHelper;
import us.mn.state.dot.tms.TMSException;;

/**
 * Integrated Public Alert and Warning System (IPAWS) Alert notifier object
 * server-side implementation.
 *
 * @author Gordon Parikh
 */
public class IpawsAlertNotifierImpl extends BaseObjectImpl
	implements IpawsAlertNotifier {
	
	/** Database table name */
	static private final String TABLE = "event.ipaws_alert_notifier";
	
	/** Name prefix */
	static private final String NAME_PREFIX = "ipaws_notifier_";
	
	/** Maximum number of records in the table (for generating unique names */
	static private final int MAX_RECORDS = (int) Math.pow(10, 9);
	
	/** Get the first available unique name for a new alert notifier. */
	static public String getUniqueName() {
		for (int i = 0; i <= MAX_RECORDS; ++i) {
			String n = NAME_PREFIX + String.valueOf(i);
			if (lookupIpawsAlertNotifier(n) == null)
				return n;
		}
		return null;
	}
	
	/** Lookup an IpawsAlertNotifierImpl given and IpawsAlert identifier/name. */
	static public IpawsAlertNotifierImpl
				lookupFromAlert(String name) {
		// ask the helper to find the IpawsAlertNotifier object for this alert
		IpawsAlertNotifier ian =
				IpawsAlertNotifierHelper.lookupAlertNotifierName(name);
		if (ian != null)
			// lookup the IpawsAlertNotifierImpl object using the name
			return lookupIpawsAlertNotifier(ian.getName());
		return null;
	}
	
	/** Load all the IPAWS alert notifiers */
	static public void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, IpawsAlertNotifierImpl.class);
		store.query("SELECT name, alert_id, dms, multi, approved_by " + 
			"FROM event." + SONAR_TYPE + ";", new ResultFactory()
		{
			@Override
			public void create(ResultSet row) throws Exception {
				try {
					namespace.addObject(new IpawsAlertNotifierImpl(row));
				} catch (Exception e) {
					System.out.println(row.getString(1));
				}
			}
		});
	}
	
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("alert_id", alert_id);
		map.put("dms", dms);
		map.put("multi", multi);
		map.put("approved_by", approved_by);
		return map;
	}

	/** Get the database table name */
	@Override
	public String getTable() {
		return "event." + SONAR_TYPE;
	}
	
	/** Get the SONAR type name */
	@Override
	public String getTypeName() {
		return SONAR_TYPE;
	}
	
	private IpawsAlertNotifierImpl(ResultSet row) throws SQLException {
		this(row.getString(1),						// name
			 row.getString(2),						// alert identifier
			 (String[])row.getArray(3).getArray(),	// DMS list
			 row.getString(4),						// MULTI
			 row.getString(5)						// approving user
		 );
	}
	
	public IpawsAlertNotifierImpl(String n)  {
		super(n);
	}
	
	public IpawsAlertNotifierImpl(String n, String aid, String[] dms) {
		super(n);
		alert_id = aid;
		this.dms = dms;
	}
	
	public IpawsAlertNotifierImpl(String n, String aid, String[] dms,
			String m, String u) {
		super(n);
		alert_id = aid;
		this.dms = dms;
		multi = m;
		approved_by = u;
	}
	
	/** Identifier of the alert triggering this notification. */
	private String alert_id;
	
	/** Set the Alert ID. */
	@Override
	public void setAlertId(String aid) {
		alert_id = aid;
	}
	
	/** Set the Alert ID. */
	public void doSetAlertId(String aid) throws TMSException {
		if (aid != alert_id) {
			store.update(this, "alert_id", aid);
			setAlertId(aid);
		}
	}
	
	/** Get the Alert ID. */
	@Override
	public String getAlertId() {
		return alert_id;
	}
	
	/** List of DMS on which the message for this alert should be posted. */
	private String[] dms;
	
	/** Set the list of DMS (represented as a string array) to be used for
	 *  deploying alert messages.
	 */
	@Override
	public void setDms(String[] dms) {
		this.dms = dms;
	}
	
	/** Set the list of DMS (represented as a string array) to be used for
	 *  deploying alert messages.
	 */
	public void doSetDms(String[] dms) throws TMSException {
		if (dms != this.dms) {
			store.update(this, "alert_id", dms);
			setDms(dms);
		}
	}
	
	/** Get the list of DMS (represented as a string array) to be used for
	 *  deploying alert messages.
	 */
	@Override
	public String[] getDms() {
		return dms;
	}
	
	/** MULTI of the message to deploy to DMS. */
	private String multi;
	
	/** Set the MULTI to be deployed to DMS. */
	@Override
	public void setMulti(String m) {
		multi = m;
	}
	
	/** Set the MULTI to be deployed to DMS. */
	public void doSetMulti(String m) throws TMSException {
		if (m != multi) {
			store.update(this, "multi", m);
			setMulti(m);
		}
	}
	
	/** Get the MULTI to be deployed to DMS. */
	@Override
	public String getMulti() {
		return multi;
	}
	
	/** User that approved the alert message posting (may be null or "AUTO"). */
	private String approved_by;

	/** Set the approving user. */
	@Override
	public void setApprovedBy(String u) {
		approved_by = u;
	}
	
	/** Set the approving user. */
	public void doSetApprovedBy(String u) throws TMSException {
		if (u != approved_by) {
			store.update(this, "approved_by", u);
			setApprovedBy(u);
		}
	}
	
	/** Get the approving user. */
	@Override
	public String getApprovedBy() {
		return approved_by;
	}
	
}