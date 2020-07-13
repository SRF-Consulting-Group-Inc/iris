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
package us.mn.state.dot.tms;

import java.util.Date;
import java.util.List;

import us.mn.state.dot.sonar.SonarObject;

/**
 * An IpawsAlertDeployer is an object for notifying clients about new or
 * updated alerts from the Integrated Public Alert and Warning System (IPAWS).
 * 
 * These objects contain a reference to the alert (via the alert_id), the list
 * of DMS that were selected based on the alert area, the MULTI of the message
 * that was generated, and the name of the user that approved the alert (which
 * is null before approval or AUTO when approval mode is disabled).
 *
 * @author Gordon Parikh
 */
public interface IpawsAlertDeployer extends SonarObject {

	/** SONAR type name */
	String SONAR_TYPE = "ipaws_alert_deployer";
	
	/** Set the generation time of this deployer object */
	void setGenTime(Date gt);
	
	/** Get the generation time of this deployer object */
	Date getGenTime();
	
	/** Set the approval time of this deployer object */
	void setApprovedTime(Date at);
	
	/** Get the approval time of this deployer object */
	Date getApprovedTime();
	
	/** Set the Alert ID. */
	void setAlertId(String aid);

	/** Get the Alert ID. */
	String getAlertId();

	/** Set the alert start time */
	void setAlertStart(Date t);
	
	/** Get the alert start time */
	Date getAlertStart();
	
	/** Set the alert end time */
	void setAlertEnd(Date t);
	
	/** Get the alert end time */
	Date getAlertEnd();
	
	/** Set the sign group used for this deployment */
	void setSignGroup(String sg);
	
	/** Get the sign group used for this deployment */
	String getSignGroup();
	
	/** Set the quick message (template) used for this deployment */
	void setQuickMessage(String qm);
	
	/** Get the quick message (template) used for this deployment */
	String getQuickMessage();
	
	/** Set the list of DMS (represented as a string array) automatically 
	 *  selected for deploying alert messages.
	 */
	void setAutoDms(String[] dms);
	
	/** Get the list of DMS (represented as a string array) automatically
	 *  selected for deploying alert messages. 
	 */
	String[] getAutoDms();
	
	/** Set the list of DMS suggested automatically by the system as optional 
	 *  DMS that users may want to include for the deployment.
	 */
	void setOptionalDms(String[] dms);
	
	/** Get the list of DMS suggested automatically by the system as optional 
	 *  DMS that users may want to include for the deployment. 
	 */
	String[] getOptionalDms();
	
	/** Set the list of DMS actually used to deploy the message. */
	void setDeployedDms(String[] dms);
	
	/** Get the list of DMS actually used to deploy the message. */
	String[] getDeployedDms();
	
	/** Set area threshold used for including DMS outside the alert area.
	 *  TODO this may become editable per-alert. */
	void setAreaThreshold(Double t);
	
	/** Get area threshold used for including DMS outside the alert area.
	 *  TODO this may become editable per-alert. */
	Double getAreaThreshold();
	
	/** Set the MULTI generated automatically by the system for deploying to
	 *  DMS.
	 */
	void setAutoMulti(String m);
	
	/** Get the MULTI generated automatically by the system for deploying to
	 *  DMS.
	 */
	String getAutoMulti();
	
	/** Set the MULTI actually deployed to DMS. */
	void setDeployedMulti(String m);
	
	/** Get the MULTI actually deployed to DMS. */
	String getDeployedMulti();
	
	/** Set the approving user. */
	void setApprovedBy(String u);
	
	/** Get the approving user. */
	String getApprovedBy();
	
	/** Set the deployed state of this alert (whether it was ever deployed). */
	void setDeployed(Boolean d);
	
	/** Get the deployed state of this alert (whether it was ever deployed). */
	Boolean getDeployed();

	/** Get the state of this alert (whether it is currently deployed or not).*/
	void setActive(Boolean a);
	
	/** Get the state of this alert (whether it is currently deployed or not).*/
	Boolean getActive();
	
	// TODO not sure about this one...
	
	/** Get the alert deployer that this replaces (if any). Note that updates
	 *  to alerts trigger creation of a new deployer (not an update).
	 */
	void setReplaces(String r);
	
	/** Set the alert deployer that this replaces (if any). */
	String getReplaces();
}
