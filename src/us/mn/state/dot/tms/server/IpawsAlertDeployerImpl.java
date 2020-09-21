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
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.utils.UniqueNameCreator;;

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
	
	/** Name creator */
	static UniqueNameCreator UNC;
	static {
		UNC = new UniqueNameCreator("ipaws_deployer_%d",
				(n)->lookupIpawsAlertDeployer(n));
		UNC.setMaxLength(24);
	}

	/** Create a unique IpawsAlertDeployer record name */
	static public String createUniqueName() {
		return UNC.createUniqueName();
	}
	
	/** Lookup an IpawsAlertDeployerImpl given and IpawsAlert identifier/name. */
	static public IpawsAlertDeployerImpl lookupFromAlert(String name) {
		// ask the helper to find the IpawsAlertDeployer object for this alert
		IpawsAlertDeployer ian =
				IpawsAlertDeployerHelper.lookupDeployerFromAlert(name);
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
			"deployed_multi, msg_priority, approved_by, deployed, active," +
			"replaces FROM event." + SONAR_TYPE + ";", new ResultFactory()
		{
			@Override
			public void create(ResultSet row) throws Exception {
				try {
					namespace.addObject(new IpawsAlertDeployerImpl(row));
				} catch (Exception e) {
					System.out.println("Error adding: " + row.getString(1));
					e.printStackTrace();
				}
			}
		});
	}
	
	/** Get columns (names and values) for storing this in the database. */
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
		map.put("auto_dms", arrayToString(auto_dms));
		map.put("optional_dms", arrayToString(optional_dms));
		map.put("deployed_dms", arrayToString(deployed_dms));
		map.put("auto_multi", auto_multi);
		map.put("deployed_multi", deployed_multi);
		map.put("msg_priority", msg_priority);
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
		this(row.getString(1),			// name
			row.getTimestamp(2),		// generated time
			row.getTimestamp(3),		// approval time
			row.getString(4),			// alert identifier
			row.getTimestamp(5),		// alert start
			row.getTimestamp(6),		// alert end
			row.getString(7),			// sign group
			row.getString(8),			// quick message
			getStringArray(row, 9),		// auto DMS list
			getStringArray(row, 10),	// optional DMS list
			getStringArray(row, 11),	// deployed DMS list
			row.getDouble(12),			// area threshold
			row.getString(13),			// auto MULTI
			row.getString(14),			// deployed MULTI
			row.getString(15),			// approving user
			row.getBoolean(16),			// deployed
			row.getBoolean(17),			// active
			row.getString(18)			// replaces
		 );
	}
	
	public IpawsAlertDeployerImpl(String n, String aid)  {
		super(n);
		alert_id = aid;
		gen_time = new Date();
	}
	
	public IpawsAlertDeployerImpl(String n, String aid, String[] dms) {
		super(n);
		alert_id = aid;
		auto_dms = dms;
		gen_time = new Date();
	}
	
	public IpawsAlertDeployerImpl(String n, String aid, String[] dms,
			String m, String u) {
		super(n);
		alert_id = aid;
		auto_dms = dms;
		auto_multi = m;
		approved_by = u;
		gen_time = new Date();
	}
	
	public IpawsAlertDeployerImpl(String n, String aid,
			Date as, Date ae, String sg, String[] dms,
			String qm, String m, int mp) {
		// TODO add SignGroup/QuickMessage/etc.
		super(n);
		alert_id = aid;
		alert_start = as;
		alert_end = ae;
		sign_group = sg;
		auto_dms = dms;
		quick_message = qm;
		auto_multi = m;
		msg_priority = mp;
		gen_time = new Date();
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

	/** Check if the provided values (alert start, alert end, auto-suggested
	 *  DMS, and auto-generated MULTI) are equal to the corresponding values
	 *  contained in this alert deployer.
	 */
	public boolean autoValsEqual(Date aStart, Date aEnd,
			String[] adms, String aMulti) {
		boolean startEq = aStart == null ? alert_start == null
				: aStart.equals(alert_start);
		boolean endEq = aEnd == null ? alert_end == null
				: aEnd.equals(alert_end);
		boolean dmsEq = Arrays.equals(auto_dms, adms);
		boolean multiEq = aMulti == null ? auto_multi == null
				: aMulti.equals(auto_multi);
		return startEq && endEq && dmsEq && multiEq;
	}
	
	/** Generation time of alert deployer */
	private Date gen_time;
	
	/** Set the generation time of this deployer object */
	@Override
	public void setGenTime(Date gt) {
		gen_time = gt;
	}

	/** Set the generation time of this deployer object */
	public void doSetGenTime(Date gt) throws TMSException {
		if (gt != gen_time) {
			store.update(this, "gen_time", gt);
			setGenTime(gt);
		}
	}
	
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
	
	/** Set the approval time of this deployer object */
	public void doSetApprovedTime(Date at) throws TMSException {
		if (at != approved_time) {
			store.update(this, "approved_time", at);
			setApprovedTime(at);
		}
	}
	
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
	
	/** Set the alert start time */
	public void doSetAlertStart(Date t) throws TMSException {
		if (t != alert_start) {
			store.update(this, "alert_start", t);
			setAlertStart(t);
		}
	}
	
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

	/** Set the alert end time */
	public void doSetAlertEnd(Date t) throws TMSException {
		if (t != alert_end) {
			store.update(this, "alert_end", t);
			setAlertEnd(t);
		}
	}
	
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

	/** Set the sign group used for this deployment */
	public void doSetSignGroup(String sg) throws TMSException {
		if (sg != sign_group) {
			store.update(this, "sign_group", sg);
			setSignGroup(sg);
		}
	}
	
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

	/** Set the quick message (template) used for this deployment */
	public void doSetQuickMessage(String qm) throws TMSException {
		if (qm != quick_message) {
			store.update(this, "quick_message", qm);
			setQuickMessage(qm);
		}
	}
	
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
			store.update(this, "auto_dms", arrayToString(dms));
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
			notifyAttribute("autoDms");
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

	/** Set the list of DMS suggested automatically by the system as optional 
	 *  DMS that users may want to include for the deployment.
	 */
	public void doSetOptionalDms(String[] dms) throws TMSException {
		if (!Arrays.deepEquals(dms, this.optional_dms)) {
			store.update(this, "optional_dms", arrayToString(dms));
			setOptionalDms(dms);
		}
	}
	
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

	/** Set the list of DMS actually used to deploy the message. */
	public void doSetDeployedDms(String[] dms) throws TMSException {
		if (!Arrays.deepEquals(dms, this.deployed_dms)) {
			store.update(this, "deployed_dms", arrayToString(dms));
			setDeployedDms(dms);
		}
	}
	
	/** Get the list of DMS actually used to deploy the message. */
	@Override
	public String[] getDeployedDms() {
		return deployed_dms;
	}

	/** Area threshold used for including DMS outside the alert area. */
	private Double alert_threshold;
	
	/** Set area threshold used for including DMS outside the alert area. */
	@Override
	public void setAreaThreshold(Double t) {
		alert_threshold = t;
	}

	/** Set area threshold used for including DMS outside the alert area. */
	public void doSetAreaThreshold(Double t) throws TMSException {
		if (t != alert_threshold) {
			store.update(this, "alert_threshold", t);
			setAreaThreshold(t);
		}
	}
	
	/** Get area threshold used for including DMS outside the alert area. */
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
			notifyAttribute("autoMulti");
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

	/** Set the MULTI actually deployed to DMS. */
	public void doSetDeployedMulti(String m) throws TMSException {
		if (m != deployed_multi) {
			store.update(this, "deployed_multi", m);
			setDeployedMulti(m);
		}
	}
	
	/** Get the MULTI actually deployed to DMS. */
	@Override
	public String getDeployedMulti() {
		return deployed_multi;
	}

	/** Message priority calculated from alert fields (can be overridden by
	 *  user). 
	 */
	private int msg_priority;
	
	/** Set the message priority */
	@Override
	public void setMsgPriority(Integer p) {
		msg_priority = p;
	}
	
	/** Set the message priority  */
	public void doSetMsgPriority(Integer p) throws TMSException {
		if (p != msg_priority) {
			store.update(this, "msg_priority", p);
			setMsgPriority(p);
		}
	}
	
	/** Get the message priority */
	@Override
	public Integer getMsgPriorty() {
		return msg_priority;
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
	
	/** Set the deployed state of this alert (whether it was ever deployed). */
	@Override
	public void setDeployed(Boolean d) {
		deployed = d;
	}

	/** Set the deployed state of this alert (whether it was ever deployed). */
	public boolean doSetDeployed(Boolean d) throws TMSException {
		if (d != deployed) {
			store.update(this, "deployed", d);
			setDeployed(d);
			return true;
		}
		return false;
	}
	
	/** Set the deployed state of this alert (whether it was ever deployed),
	 *  notifying clients if it has changed.
	 */
	public void setDeployedNotify(Boolean d) throws TMSException {
		if (doSetDeployed(d))
			notifyAttribute("deployed");
	}
	
	/** Get the deployed state of this alert (whether it was ever deployed). */
	@Override
	public Boolean getDeployed() {
		return deployed;
	}
	
	/** State of this alert (whether it is currently deployed or not). */
	private boolean active;
	
	/** Set the state of this alert (whether it is currently deployed or not).*/
	@Override
	public void setActive(boolean a) {
		active = a;
	}

	/** Set the state of this alert (whether it is currently deployed or not).*/
	public void doSetActive(boolean a) throws TMSException {
		if (a != active) {
			store.update(this, "active", a);
			setActive(a);
		}
	}
	
	/** Get the state of this alert (whether it is currently deployed or not).*/
	@Override
	public boolean getActive() {
		return active;
	}
	
	/** Alert deployer that this replaces (if any). Note that updates to
	 *  alerts trigger creation of a new deployer (not an update).
	 */
	private String replaces;
	
	/** Get the alert deployer that this replaces (if any). Note that updates
	 *  to alerts trigger creation of a new deployer (not an update).
	 */
	@Override
	public void setReplaces(String r) {
		replaces = r;
	}

	/** Get the alert deployer that this replaces (if any). Note that updates
	 *  to alerts trigger creation of a new deployer (not an update).
	 */
	public void doSetReplaces(String r) throws TMSException {
		if (r != replaces) {
			store.update(this, "replaces", r);
			setReplaces(r);
		}
	}
	
	/** Set the alert deployer that this replaces (if any). */
	@Override
	public String getReplaces() {
		return replaces;
	}
	
}