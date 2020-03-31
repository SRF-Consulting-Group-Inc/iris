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
 * CameraVidSourceOrder: an interface for objects
 * that bind a camera-template to a source-
 * template and provide a sort order.  The pair
 * of camera-template and order-number will be
 * unique.
 * 
 * @author John L. Stanley - SRF Consulting
 */
public interface CameraVidSourceOrder extends SonarObject, Comparable<CameraVidSourceOrder> {
	
	/** SONAR type name */
	String SONAR_TYPE = "camera_vid_source_order";
	
	public void setCameraTemplate(String cameraTemplate);
	public String getCameraTemplate();
	
	public void setOrder(int order);
	public int getOrder();
	
	public void setVidSourceTemplate(String vidSourceTemplate);
	public String getVidSourceTemplate();
	
	default int compareTo(CameraVidSourceOrder cso2) {
		return Integer.compare(getOrder(), cso2.getOrder());
	}
	

}
