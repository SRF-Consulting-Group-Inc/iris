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

import us.mn.state.dot.sonar.SonarObject;
import us.mn.state.dot.tms.IpawsAlertDeployer;
import us.mn.state.dot.tms.IpawsAlertDeployerHelper;
import us.mn.state.dot.tms.TMSException;;

/**
 * Integrated Public Alert and Warning System (IPAWS) Alert deployer object
 * server-side implementation.
 *
 * @author Gordon Parikh
 */
public class IpawsAlertDeployerImpl extends BaseObjectImpl
	implements IpawsAlertDeployer {
	
	/** Database table name */
	static private final String TABLE = "event.ipaws_alert_deployer";
	
	/** Name prefix */
	static private final String NAME_PREFIX = "ipaws_deployer_";
	
	/** Maximum number of records in the table (for generating unique names */
	static private final int MAX_RECORDS = (int) Math.pow(10, 9);
	
	/** Get the first available unique name for a new alert deployer. */
	// TODO change to use UniqueNameCreator once merged with video changes
	static public String getUniqueName() {
		for (int i = 0; i <= MAX_RECORDS; ++i) {
			String n = NAME_PREFIX + String.valueOf(i);
			if (lookupIpawsAlertDeployer(n) == null)
				return n;
		}
		return null;
	}
	
	/** Lookup an IpawsAlertDeployerImpl given and IpawsAlert identifier/name. */
	static public IpawsAlertDeployerImpl
				lookupFromAlert(String name) {
		// ask the helper to find the IpawsAlertDeployer object for this alert
		IpawsAlertDeployer ian =
				IpawsAlertDeployerHelper.lookupAlertDeployerName(name);
		if (ian != null)
			// lookup the IpawsAlertDeployerImpl object using the name
			return lookupIpawsAlertDeployer(ian.getName());
		return null;
	}
	
	/** Load all the IPAWS alert deployers */
	static public void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, IpawsAlertDeployerImpl.class);
		store.query("SELECT name, gen_time, approved_time, alert_id," +
			"alert_start, alert_end, sign_group, quick_message, auto_dms," +
			"optional_dms, deployed_dms, area_threshold, auto_multi," +
			"deployed_multi, approved_by, deployed, active, replaces " +
			"FROM event." + SONAR_TYPE + ";", new ResultFactory()
		{
			@Override
			public void create(ResultSet row) throws Exception {
				try {
					namespace.addObject(new IpawsAlertDeployerImpl(row));
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
		map.put("gen_time", gen_time);
		map.put("approved_time", approved_time);
		map.put("alert_id", alert_id);
		map.put("alert_start", alert_start);
		map.put("alert_end", alert_end);
		map.put("sign_group", sign_group);
		map.put("quick_message", quick_message);
		map.put("auto_dms", auto_dms);
		map.put("optional_dms", optional_dms);
		map.put("deployed_dms", deployed_dms);
		map.put("auto_multi", auto_multi);
		map.put("deployed_multi", deployed_multi);
		map.put("approved_by", approved_by);
		map.put("deployed", deployed);
		map.put("active", active);
		map.put("replaces", replaces);
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
	
	private IpawsAlertDeployerImpl(ResultSet row) throws SQLException {
		this(row.getString(1),						// name
			row.getTimestamp(2),					// generated time
			row.getTimestamp(3),					// approval time
			row.getString(4),						// alert identifier
			row.getTimestamp(5),					// alert start
			row.getTimestamp(6),					// alert end
			row.getString(7),						// sign group
			row.getString(8),						// quick message
			(String[])row.getArray(9).getArray(),	// auto DMS list
			(String[])row.getArray(10).getArray(),	// optional DMS list
			(String[])row.getArray(11).getArray(),	// deployed DMS list
			row.getDouble(12),						// area threshold
			row.getString(13),						// auto MULTI
			row.getString(14),						// deployed MULTI
			row.getString(15),						// approving user
			row.getBoolean(16),						// deployed
			row.getBoolean(17),						// active
			row.getString(18)						// replaces
		 );
	}
	
	public IpawsAlertDeployerImpl(String n)  {
		super(n);
	}
	
	public IpawsAlertDeployerImpl(String n, String aid)  {
		super(n);
		alert_id = aid;
	}
	
	public IpawsAlertDeployerImpl(String n, String aid, String[] dms) {
		super(n);
		alert_id = aid;
		auto_dms = dms;
	}
	
	public IpawsAlertDeployerImpl(String n, String aid, String[] dms,
			String m, String u) {
		super(n);
		alert_id = aid;
		auto_dms = dms;
		auto_multi = m;
		approved_by = u;
	}
	
	public IpawsAlertDeployerImpl(String n, Date gt, Date at, String aid,
			Date as, Date ae, String sg, String qm, String[] adms,
			String[] odms, String[] ddms, Double t, String am, String dm,
			String u, Boolean d, Boolean a, String r) {
		super(n);
		gen_time = gt;
		approved_time = at;
		alert_id = aid;
		alert_start = as;
		alert_end = ae;
		sign_group = sg;
		quick_message = qm;
		auto_dms = adms;
		optional_dms = odms;
		deployed_dms = ddms;
		auto_multi = am;
		deployed_multi = dm;
		approved_by = u;
		deployed = d;
		active = a;
		replaces = r;
	}

	/** Generation time of alert deployer */
	private Date gen_time;
	
	/** Set the generation time of this deployer object */
	@Override
	public void setGenTime(Date gt) {
		gen_time = gt;
	}

	// TODO doSetGenTime()
	
	/** Get the generation time of this deployer object */
	@Override
	public Date getGenTime() {
		return gen_time;
	}
	
	/** Approved time of alert deployer */
	private Date approved_time;
	
	/** Set the approval time of this deployer object */
	@Override
	public void setApprovedTime(Date at) {
		approved_time = at;
	}
	
	// TODO doSetApprovedTime()
	
	/** Get the approval time of this deployer object */
	@Override
	public Date getApprovedTime() {
		return approved_time;
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
	
	/** Start time of alert (stored here for convenience) */
	private Date alert_start;
	
	/** Set the alert start time */
	@Override
	public void setAlertStart(Date t) {
		alert_start = t;
	}
	
	// TODO doSet...()
	
	/** Get the alert start time */
	@Override
	public Date getAlertStart() {
		return alert_start;
	}
	
	/** End time of alert (stored here for convenience) */
	private Date alert_end;
	
	/** Set the alert end time */
	@Override
	public void setAlertEnd(Date t) {
		alert_end = t;
	}

	// TODO doSet...()
	
	/** Get the alert end time */
	@Override
	public Date getAlertEnd() {
		return alert_end;
	}
	
	/** Sign group used for this deployment */
	private String sign_group;
	
	/** Set the sign group used for this deployment */
	@Override
	public void setSignGroup(String sg) {
		sign_group = sg;
	}

	// TODO doSet...()
	
	/** Get the sign group used for this deployment */
	@Override
	public String getSignGroup() {
		return sign_group;
	}

	/** Quick message used for this deployment */
	private String quick_message;
	
	/** Set the quick message (template) used for this deployment */
	@Override
	public void setQuickMessage(String qm) {
		quick_message = qm;
	}

	// TODO doSet...()
	
	/** Get the quick message (template) used for this deployment */
	@Override
	public String getQuickMessage() {
		return quick_message;
	}

	/** List of DMS automatically selected for this alert. */
	private String[] auto_dms;
	
	/** Set the list of DMS (represented as a string array) automatically 
	 *  selected for deploying alert messages.
	 */
	@Override
	public void setAutoDms(String[] dms) {
		auto_dms = dms;
	}
	
	/** Set the list of DMS (represented as a string array) automatically 
	 *  selected for deploying alert messages.
	 */
	public boolean doSetAutoDms(String[] dms) throws TMSException {
		if (!Arrays.deepEquals(dms, this.auto_dms)) {
			store.update(this, "auto_dms", Arrays.toString(dms));
			setAutoDms(dms);
			return true;
		}
		return false;
	}
	
	/** Set the list of DMS (represented as a string array) automatically 
	 *  selected for deploying alert messages, notifying clients if it has
	 *  changed.
	 */
	public void setAutoDmsNotify(String[] dms) throws TMSException {
		if (doSetAutoDms(dms))
			notifyAttribute("auto_dms");
	}
	
	/** Get the list of DMS (represented as a string array) automatically 
	 *  selected for deploying alert messages.
	 */
	@Override
	public String[] getAutoDms() {
		return auto_dms;
	}

	/** List of DMS suggested automatically as optional DMS that users may
	 *  want to include in the deployment. */
	private String[] optional_dms;
	
	/** Set the list of DMS suggested automatically by the system as optional 
	 *  DMS that users may want to include for the deployment.
	 */
	@Override
	public void setOptionalDms(String[] dms) {
		optional_dms = dms;
	}

	// TODO doSet...()
	
	/** Get the list of DMS suggested automatically by the system as optional 
	 *  DMS that users may want to include for the deployment.
	 */
	@Override
	public String[] getOptionalDms() {
		return optional_dms;
	}

	/** List of DMS actually used to deploy the message. */
	private String[] deployed_dms;
	
	/** Set the list of DMS actually used to deploy the message. */
	@Override
	public void setDeployedDms(String[] dms) {
		deployed_dms = dms;
	}

	// TODO doSet...()
	
	/** Get the list of DMS actually used to deploy the message. */
	@Override
	public String[] getDeployedDms() {
		return deployed_dms;
	}

	/** Area threshold used for including DMS outside the alert area. */
	private Double alert_threshold;
	
	@Override
	public void setAreaThreshold(Double t) {
		alert_threshold = t;
	}

	// TODO doSet...()
	
	@Override
	public Double getAreaThreshold() {
		return alert_threshold;
	}

	/** MULTI generated automatically by the system for to deploying to DMS. */
	private String auto_multi;
	
	/** Set the MULTI generated automatically by the system for deploying to
	 *  DMS.
	 */
	@Override
	public void setAutoMulti(String m) {
		auto_multi = m;
	}
	
	/** Set the MULTI generated automatically by the system for deploying to
	 *  DMS.
	 */
	public boolean doSetAutoMulti(String m) throws TMSException {
		if (m == null || !m.equals(auto_multi)) {
			store.update(this, "auto_multi", m);
			setAutoMulti(m);
			return true;
		}
		return false;
	}

	/** Set the MULTI generated automatically by the system for deploying to
	 *  DMS, notifying clients if it has changed.
	 */
	public void setAutoMultiNotify(String m) throws TMSException {
		if (doSetAutoMulti(m))
			notifyAttribute("auto_multi");
	}
	
	/** Get the MULTI generated automatically by the system for deploying to
	 *  DMS.
	 */
	@Override
	public String getAutoMulti() {
		return auto_multi;
	}

	/** MULTI actually deployed to DMS. */
	private String deployed_multi;
	
	/** Set the MULTI actually deployed to DMS. */
	@Override
	public void setDeployedMulti(String m) {
		deployed_multi = m;
	}

	// TODO doSet...()
	
	/** Get the MULTI actually deployed to DMS. */
	@Override
	public String getDeployedMulti() {
		return deployed_multi;
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

	/** Deployed state of this alert (whether it was ever deployed). */
	private Boolean deployed;
	
	@Override
	public void setDeployed(Boolean d) {
		deployed = d;
	}

	// TODO doSet...()
	
	@Override
	public Boolean getDeployed() {
		return deployed;
	}
	
	/** State of this alert (whether it is currently deployed or not). */
	private Boolean active;
	
	@Override
	public void setActive(Boolean a) {
		active = a;
	}

	// TODO doSet...()
	
	@Override
	public Boolean getActive() {
		return active;
	}
	
	/** Alert deployer that this replaces (if any). Note that updates to
	 *  alerts trigger creation of a new deployer (not an update).
	 */
	private String replaces;
	
	@Override
	public void setReplaces(String r) {
		replaces = r;
	}

	// TODO doSet...()
	
	@Override
	public String getReplaces() {
		return replaces;
	}
	
}