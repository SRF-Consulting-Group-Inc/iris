/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2014  Minnesota Department of Transportation
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
import java.util.HashMap;
import java.util.Map;
import us.mn.state.dot.tms.CameraVidSourceOrder;
import us.mn.state.dot.tms.TMSException;

/**
 * CameraVidSourceOrderImpl provides a BaseObjectImpl
 * for the CameraVidSourceOrder interface.
 *
 * @author John L. Stanley - SRF Consulting
 */
public class CameraVidSourceOrderImpl extends BaseObjectImpl implements CameraVidSourceOrder {

	/** Load all the camera presets */
	static protected void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, CameraVidSourceOrderImpl.class);
		store.query("SELECT name, camera_template, src_order, " +
			"src_template FROM iris." + SONAR_TYPE + ";", new ResultFactory()
		{
			public void create(ResultSet row) throws Exception {
				namespace.addObject(new CameraVidSourceOrderImpl(
					row.getString(1),	// name
					row.getString(2),	// camera_template
					row.getInt(3),		// source order
					row.getString(4)	// source_template
				));
			}
		});
	}

	/** Get a mapping of the columns */
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("camera_template", camera_template);
		map.put("src_order", source_order);
		map.put("src_template", source_template);
		return map;
	}

	/** Get the database table name */
	public String getTable() {
		return "iris." + SONAR_TYPE;
	}

	/** Get the SONAR type name */
	public String getTypeName() {
		return SONAR_TYPE;
	}

	/** Create a new camera preset with a string name */
	public CameraVidSourceOrderImpl(String n) throws TMSException {
		super(n);
	}

	/** Create a camera preset */
	protected CameraVidSourceOrderImpl(String n, String ct, int so, String st) {
		super(n);
		camera_template = ct;
		source_order = so;
		source_template = st;
	}

	/** CCTV Camera */
	private String camera_template;

	/** Set the camera template name */
	@Override
	public void setCameraTemplate(String cameraTemplate) {
		this.camera_template = cameraTemplate;
	}

	/** Get the camera template name */
	@Override
	public String getCameraTemplate() {
		return camera_template;
	}
	
	/** Set the template camera_template */
	public void doSetCameraTemplate(String camera_template) throws TMSException {
		if (camera_template != this.camera_template) {
			store.update(this, "camera_template", camera_template);
			setCameraTemplate(camera_template);
		}
	}

	/** Source Order */
	private int source_order;

	/** Set the source order number */
	@Override
	public void setSourceOrder(int so) {
		this.source_order = so;
	}

	/** Get the source order number */
	@Override
	public int getSourceOrder() {
		return source_order;
	}
	
	/** Set the template source_order */
	public void doSetSourceOrder(int source_order) throws TMSException {
		if (source_order != this.source_order) {
			store.update(this, "source_order", source_order);
			setSourceOrder(source_order);
		}
	}

	/** name of stream template */
	private String source_template;

	/** Set the source template name */
	@Override
	public void setVidSourceTemplate(String st) {
		source_template = st;
	}

	/** Get the source template name */
	@Override
	public String getVidSourceTemplate() {
		return source_template;
	}
	
	/** Set the template source_template */
	public void doSetVidSourceTemplate(String source_template) throws TMSException {
		if (source_template != this.source_template) {
			store.update(this, "source_template", source_template);
			setVidSourceTemplate(source_template);
		}
	}
}
