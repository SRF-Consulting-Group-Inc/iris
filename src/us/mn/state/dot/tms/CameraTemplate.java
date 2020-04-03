/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2019  SRF Consulting Group
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

import us.mn.state.dot.sonar.SonarObject;

/**
 * A camera-template names and groups an ordered
 * series of vid-source-templates.  This identifies
 * the various video-streams that can provide video
 * from a camera.
 * 
 * A camera is assigned a camera-template by
 * setting the camera.camera_template field to
 * the name of the camera-template object.  
 * 
 * The many-to-many links between the
 * camera-template(s) and vid-source-template(s)
 * are saved in a camera-source-order table.
 * 
 * @author John L. Stanley - SRF Consulting
 *
 */
public interface CameraTemplate extends SonarObject {

	/** SONAR type name */
	String SONAR_TYPE = "camera_template";
	
	/** Get the notes */
	public String getNotes();
	
	/** Set the notes */
	public void setNotes(String notes);

	/** Get the AutoStart parameter */
	public Boolean getAutoStart();

	/** Set the AutoStart parameter */
	public void setAutoStart(Boolean autostart);

	/** Get the Failover parameter */
	public Boolean getFailover();

	/** Set the Failover parameter */
	public void setFailover(Boolean failover);

	/** Get the ConnectFailSec parameter */
	public Integer getConnectFailSec();

	/** Set the ConnectFailSec parameter */
	public void setConnectFailSec(Integer sec);

	/** Get the LostTimeoutSec parameter */
	public Integer getLostTimeoutSec();

	/** Set the LostTimeoutSec parameter */
	public void setLostTimeoutSec(Integer sec);

	/** Get the AutoReconnect parameter */
	public Boolean getAutoReconnect();

	/** Set the AutoReconnect parameter */
	public void setAutoReconnect(Boolean autoReconnect);

	/** Get the ReconnectTimeoutSec parameter */
	public Integer getReconnectTimeoutSec();

	/** Set the ReconnectTimeoutSec parameter */
	public void setReconnectTimeoutSec(Integer sec);
}
