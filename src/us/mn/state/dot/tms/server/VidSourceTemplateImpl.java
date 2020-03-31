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
package us.mn.state.dot.tms.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import us.mn.state.dot.tms.VidSourceTemplate;
import us.mn.state.dot.tms.TMSException;

/**
 * @author John L. Stanley - SRF Consulting
 *
 */
public class VidSourceTemplateImpl extends BaseObjectImpl implements VidSourceTemplate {

	/** Load all the stream templates */
	static protected void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, VidSourceTemplateImpl.class);
		store.query("SELECT name, label, config, "+
			"default_port, subnets, latency, "+
			"encoder, scheme, codec, "+
			"rez_width, rez_height, multicast FROM iris." +
			SONAR_TYPE + ";", new ResultFactory()
		{
			public void create(ResultSet row) throws Exception {
				namespace.addObject(new VidSourceTemplateImpl(row));
			}
		});
	}

	/** Get a mapping of the columns */
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("label", label);
		map.put("config", config);
		map.put("default_port", default_port);
		map.put("subnets", subnets);
		map.put("latency", latency);
		map.put("encoder", encoder);
		map.put("scheme", scheme);
		map.put("codec", codec);
		map.put("rez_width", rez_width);
		map.put("rez_height", rez_height);
		map.put("multicast", multicast);
		return map;
	}

	/** Create a gate arm array */
	private VidSourceTemplateImpl(ResultSet row) throws SQLException {
		this(row.getString(1),    // name
		     row.getString(2),    // label
		     row.getString(3),    // config
		     row.getObject(4),    // default_port
		     row.getString(5),    // subnets
		     row.getInt(6),       // latency
		     row.getString(7),    // encoder
		     row.getString(8),    // scheme
		     row.getString(9),    // codec
		     row.getInt(10),      // rez_width
		     row.getInt(11),      // rez_height
		     row.getBoolean(12)); // multicast
	}

	/** Get the database table name */
	@Override
	public String getTable() {
		return "iris." + SONAR_TYPE;
	}

	/** Get the SONAR type name */
	@Override
	public String getTypeName() {
		return SONAR_TYPE;
	}

	/** Create an xStreamTemplate */
	public VidSourceTemplateImpl(String n) {
		super(n);
		// TODO Auto-generated constructor stub
	}

	/** Create an xStreamTemplate */
	private VidSourceTemplateImpl(String sName,
			String sLabel,  String sConfig,
			Object iDefault_port, String sSubnets,
			int iLatency, String sEncoder,
			String sScheme, String sCodec,
			int iRez_width, int iRez_height,
			boolean bMulticast) {
		super(sName);
		label = sLabel;
		config = sConfig;
		default_port = (Integer)iDefault_port;
		subnets = sSubnets;
		latency = iLatency;
		encoder = sEncoder;
		scheme = sScheme;
		codec = sCodec;
		rez_width = iRez_width;
		rez_height = iRez_height;
		multicast = bMulticast;
	}

	//---------------------------------
	
	String label;   // stream-type identifier shown in video window
	String config;
		// If codec is empty:
		//   config is a backwards-compatible uri_path string
		// If codec is not empty:
		//   config is a gst-launch config string
		// substitution fields
		//	{addr} address from camera encoder field
		//	{port} port from camera encoder field
		//	{addrport} address[:port] from camera encoder field
		//	{chan} encoder_channel from camera record
		//	{name} camera.name modified to not use reserved URI chars.
		//		(For live555 video proxy.)
		//	{<other IRIS system property>}
		//      (Substitute IRIS system-property field into config string.)
		//	examples
		//      rtsp://83.244.45.234/{camName}
		//      http://{addrport}/mpeg4/media.amp
	Integer default_port;
		// If no port specified in camera record, use this value for {port} substitution
		// If no port specified here or in camera record, and {port} or {mport} is in the config string, don't use this template
	String subnets;
		// Comma separated list of subnet identifiers where stream is available.
		// If empty, the stream is available in all subnets.
	int latency;
	String encoder; // Name of manufacturer & model
	String scheme;  // (rtsp/http/udp/ftp)
	String codec;   // (MJPEG, MPEG2, MPEG4, H264, H265, JPEG, etc)
	                // If empty, codec is probably MJPEG
	int rez_width;
	int rez_height;
	boolean multicast; // (T/F)
	
	//-- maybe implement later?
//	String users; // Semicolon separated list of user-groups and users permitted to use that stream.
//	// If field is blank, stream is usable by all users.
//	// Stretch: u-g/u identifiers prefixed with '-' are not-permitted.
	
	//---------------------------------
	
	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @return the config
	 */
	public String getConfig() {
		return encoder;
	}

	/**
	 * @param config to set
	 */
	public void setConfig(String config) {
		this.config = config;
	}

	/**
	 * @return the default_port
	 */
	public Integer getDefaultPort() {
		return default_port;
	}

	/**
	 * @param default_port the default_port to set
	 */
	public void setDefaultPort(Integer default_port) {
		this.default_port = default_port;
	}

	/**
	 * @return the subnets
	 */
	public String getSubnets() {
		return subnets;
	}

	/**
	 * @param subnets the subnets to set
	 */
	public void setSubnets(String subnets) {
		this.subnets = subnets;
	}

	/**
	 * @return the latency
	 */
	public int getLatency() {
		return latency;
	}

	/**
	 * @param latency the latency to set
	 */
	public void setLatency(int latency) {
		this.latency = latency;
	}

	@Override
	public String getEncoder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEncoder(String encoder) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @return the scheme
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * @param scheme the scheme to set
	 */
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	/**
	 * @return the codec
	 */
	public String getCodec() {
		return codec;
	}

	/**
	 * @param codec the codec to set
	 */
	public void setCodec(String codec) {
		this.codec = codec;
	}

	/**
	 * @return the rezWidth
	 */
	public int getRezWidth() {
		return rez_width;
	}

	/**
	 * @param rezWidth the rezWidth to set
	 */
	public void setRezWidth(int rezWidth) {
		this.rez_width = rezWidth;
	}

	/**
	 * @return the rez_height
	 */
	public int getRezHeight() {
		return rez_height;
	}

	/**
	 * @param rez_height the rez_height to set
	 */
	public void setRezHeight(int rez_height) {
		this.rez_height = rez_height;
	}

	/**
	 * @return the multicast
	 */
	public boolean isMulticast() {
		return multicast;
	}

	/**
	 * @param multicast the multicast to set
	 */
	public void setMulticast(boolean multicast) {
		this.multicast = multicast;
	}

	@Override
	public boolean isGstStream() {
		return (config != null) && config.contains("!");
	}
}
