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
package us.mn.state.dot.tms.client.alert;

import java.awt.Color;
import java.awt.Graphics2D;

import org.postgis.MultiPolygon;

import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.IpawsAlert;
import us.mn.state.dot.tms.IpawsAlertDeployer;
import us.mn.state.dot.tms.IpawsAlertHelper;
import us.mn.state.dot.tms.ItemStyle;
import us.mn.state.dot.tms.client.incident.IncidentMarker;
import us.mn.state.dot.tms.client.map.MapObject;
import us.mn.state.dot.tms.client.proxy.MapGeoLoc;
import us.mn.state.dot.tms.client.proxy.ProxyManager;
import us.mn.state.dot.tms.client.proxy.ProxyTheme;

/**
 * Theme for alert objects on the map.
 *
 * @author Gordon Parikh
 */
public class AlertTheme extends ProxyTheme<IpawsAlertDeployer> {
	public AlertTheme(ProxyManager<IpawsAlertDeployer> m) {
		// TODO for now we're stealing the incident marker
		super(m,  new IncidentMarker());
		addStyle(ItemStyle.ACTIVE, ProxyTheme.COLOR_AVAILABLE);
		addStyle(ItemStyle.PAST, ProxyTheme.COLOR_INACTIVE);
		addStyle(ItemStyle.ALL, Color.WHITE);
	}
	
	/** Draw the specified map object. Overridden to do nothing so only
	 *  selected alerts are drawn.
	 *  
	 *  TODO decide if we want to draw non-selected alerts too and distinguish
	 *  selected ones somehow...
	 */
	@Override
	public void draw(Graphics2D g, MapObject mo) { }
	
	/** Draw the alert on the map. The GeoLoc is the centroid of the alert and
	 *  is not drawn (really just kind of a hack to get this method to fire).
	 *  The alert area is drawn, along with DMS selected for the alert.
	 */
	@Override
	public void drawSelected(Graphics2D g, MapObject mo) {
		if (mo instanceof MapGeoLoc) {
			// get the GeoLoc from the object and use it to lookup the alert
			MapGeoLoc mgl = (MapGeoLoc) mo;
			GeoLoc gl = mgl.getGeoLoc();
			IpawsAlert ia = IpawsAlertHelper.lookup(gl.getName());
			
			// draw the alert area
			// TODO need to make sure the PostGIS JAR makes it to the client
			// (and actually works) in WebStart
			MultiPolygon mp = ia.getGeoPoly();
			System.out.println("Drawing polygon: " + mp.toString());
			
			// TODO draw the polygon (need to pull out coordinates and draw
			// the polygon as a path, making sure to work in the right
			// coordinate system (maybe WGS will be OK, probably need to do 
			// some transformation(s))
		}
	}
}
