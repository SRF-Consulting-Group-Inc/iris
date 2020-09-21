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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

import org.postgis.MultiPolygon;
import org.postgis.Point;
import org.postgis.Polygon;

import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.DMSHelper;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.IpawsAlert;
import us.mn.state.dot.tms.IpawsAlertDeployer;
import us.mn.state.dot.tms.IpawsAlertDeployerHelper;
import us.mn.state.dot.tms.IpawsAlertHelper;
import us.mn.state.dot.tms.ItemStyle;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.dms.DmsMarker;
import us.mn.state.dot.tms.client.dms.DmsTheme;
import us.mn.state.dot.tms.client.incident.IncidentMarker;
import us.mn.state.dot.tms.client.map.MapObject;
import us.mn.state.dot.tms.client.map.Style;
import us.mn.state.dot.tms.client.map.Symbol;
import us.mn.state.dot.tms.client.map.VectorSymbol;
import us.mn.state.dot.tms.client.proxy.GeoLocManager;
import us.mn.state.dot.tms.client.proxy.MapGeoLoc;
import us.mn.state.dot.tms.client.proxy.ProxyManager;
import us.mn.state.dot.tms.client.proxy.ProxyTheme;
import us.mn.state.dot.tms.geo.Position;
import us.mn.state.dot.tms.geo.SphericalMercatorPosition;

/**
 * Theme for alert objects on the map.
 *
 * @author Gordon Parikh
 */
public class AlertTheme extends ProxyTheme<IpawsAlertDeployer> {
	public AlertTheme(ProxyManager<IpawsAlertDeployer> m) {
		// TODO for now we're stealing the incident marker
		super(m, new IncidentMarker());
		addStyle(ItemStyle.PENDING, ProxyTheme.COLOR_AVAILABLE);
		addStyle(ItemStyle.ACTIVE, ProxyTheme.COLOR_DEPLOYED);
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
	
	/** Solid stroke line */
	static private final BasicStroke LINE_SOLID = new BasicStroke(8,
		BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	
	/** Active alert area outline color */
	static private final Color ACTIVE_ALERT_OUTLINE = Color.RED;
	
	/** Active alert area fill color */
	static private final Color ACTIVE_ALERT_FILL = new Color(255,0,0,40);
	
	/** Past alert area outline color */
	static private final Color PAST_ALERT_OUTLINE = Color.DARK_GRAY;
	
	/** Past alert area fill color */
	static private final Color PAST_ALERT_FILL = new Color(105,105,105,40);
	
	/** Draw the alert on the map. The GeoLoc is the centroid of the alert and
	 *  is not drawn (really just kind of a hack to get this method to fire).
	 *  The alert area is drawn, along with DMS selected for the alert.
	 */
	@Override
	public void drawSelected(Graphics2D g, MapObject mo) {
		if (mo instanceof MapGeoLoc) {
			// save the current tranform before drawing anything
			AffineTransform t = g.getTransform();
			
			// get the GeoLoc from the object and use it to lookup the alert
			MapGeoLoc mgl = (MapGeoLoc) mo;
			GeoLoc gl = mgl.getGeoLoc();
			IpawsAlert ia = IpawsAlertHelper.lookup(gl.getName());
			
			// get the MultiPolygon from the alert and use it to create a set
			// of Shape objects
			// TODO need to make sure the PostGIS JAR makes it to the client
			// (and actually works) in WebStart
			MultiPolygon mp = ia.getGeoPoly();
			ArrayList<Shape> shapes = getShapes(mp);
			
			// get the alert deployer and check the style - past alerts will
			// be a different color
			IpawsAlertDeployer iad = IpawsAlertDeployerHelper.
					lookupDeployerFromAlert(ia.getIdentifier());
			Color outline = null;
			Color fill = null;
			if (manager.checkStyle(ItemStyle.ACTIVE, iad)) {
				// active alerts are red
				outline = ACTIVE_ALERT_OUTLINE;
				fill = ACTIVE_ALERT_FILL;
			} else if (manager.checkStyle(ItemStyle.PAST, iad)) {
				// past alerts are gray
				outline = PAST_ALERT_OUTLINE;
				fill = PAST_ALERT_FILL;
			}
			
			// draw the polygons on the graphics context
			g.setStroke(LINE_SOLID);
			for (Shape shp: shapes) {
				// draw the outline as a solid color
				g.setColor(outline);
				g.draw(shp);
				
				// fill with semi-transparent color
				g.setColor(fill);
				g.fill(shp);
			}
			
			// draw symbols for DMS selected for inclusion in the alert
			Session session = Session.getCurrent();
			DmsTheme dthm = (DmsTheme) session.getDMSManager().getTheme();
			GeoLocManager glMan = session.getGeoLocManager();
			Style deployedStyle = dthm.getStyle(ItemStyle.DEPLOYED);
			VectorSymbol dsym = (VectorSymbol) dthm.getSymbol();
			
			// loop over the DMS in the deployer
			// TODO using auto_dms for now - change to deployed (may need to
			// add more drawing code for auto/suggested in deployer dialog,
			// perhaps making auto_dms "AVAILABLE", optional_dms "ALL", and
			// deployed_dms "DEPLOYED", or something like that)
			// TODO account for selection in AlertDmsDispatcher
			System.out.println("Going to draw " + iad.getAutoDms().length + " DMS");
			for (String dmsName: iad.getAutoDms()) {
				DMS dms = DMSHelper.lookup(dmsName);
				if (dms == null || dms.getGeoLoc() == null)
					continue;
				
				// get a map object for this DMS and draw
				MapGeoLoc dmgl = glMan.findMapGeoLoc(dms.getGeoLoc());
				
				// TODO this is where we would change styles
//				System.out.println("Drawing DMS " + dmsName + " with " +
//						dmgl.getTransform());
				dsym.draw(g, dmgl, deployedStyle);
				g.setTransform(t); // reset transform
			}
		}
	}
	
	
	
	/** Build awt.Shape objects from a MultiPolygon, returning a list of Shape
	 *  objects with each representing a polygon.
	 */
	private static ArrayList<Shape> getShapes(MultiPolygon mp) {
		ArrayList<Shape> paths = new ArrayList<Shape>();
		
		// iterate over the polygons and points
		for (Polygon poly: mp.getPolygons()) {
			// draw a path of each polygon
			GeneralPath path = new GeneralPath();
			Point p = poly.getFirstPoint();
			if (p != null) {
				SphericalMercatorPosition smp = getSphereMercatorPos(p);
				path.moveTo(smp.getX(), smp.getY());
			}
			for (int i = 1; i < poly.numPoints(); ++i) {
				p = poly.getPoint(i);
				SphericalMercatorPosition smp = getSphereMercatorPos(p);
				path.lineTo(smp.getX(), smp.getY());
			}
			path.closePath();	// should already be closed, but just in case
			paths.add(path);
		}
		return paths;
	}
	
	/** Convert a PostGIS Point to a SphericalMercatorPosition object. */
	private static SphericalMercatorPosition getSphereMercatorPos(Point p) {
		// construct a lat/lon Position object first, then convert
		// that to a SphericalMercatorPosition
		Position pos = new Position(p.y, p.x);
		return SphericalMercatorPosition.convert(pos);
	}
}
