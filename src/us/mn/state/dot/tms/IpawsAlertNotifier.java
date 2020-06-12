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

import java.util.List;

import us.mn.state.dot.sonar.SonarObject;

/**
 * An IpawsAlertNotifier is an object for notifying clients about new or
 * updated alerts from the Integrated Public Alert and Warning System (IPAWS).
 * 
 * These objects contain a reference to the alert (via the alert_id), the list
 * of DMS that were selected based on the alert area, the MULTI of the message
 * that was generated, and the name of the user that approved the alert (which
 * is null before approval or AUTO when approval mode is disabled).
 *
 * @author Gordon Parikh
 */
public interface IpawsAlertNotifier extends SonarObject {

	/** SONAR type name */
	String SONAR_TYPE = "ipaws_alert_notifier";
	
	/** Set the Alert ID. */
	void setAlertId(String aid);

	/** Get the Alert ID. */
	String getAlertId();
	
	/** Set the list of DMS (represented as a string array) to be used for
	 *  deploying alert messages.
	 */
	void setDms(String[] dms);
	
	/** Get the list of DMS (represented as a string array) to be used for
	 *  deploying alert messages.
	 */
	String[] getDms();
	
	/** Set the MULTI to be deployed to DMS. */
	void setMulti(String m);
	
	/** Get the MULTI to be deployed to DMS. */
	String getMulti();
	
	/** Set the approving user. */
	void setApprovedBy(String u);
	
	/** Get the approving user. */
	String getApprovedBy();
}
