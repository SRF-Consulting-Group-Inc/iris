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

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Iterator;

import org.json.JSONObject;
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Polygon;

import us.mn.state.dot.sched.Job;
import us.mn.state.dot.tms.IpawsAlert;
import us.mn.state.dot.tms.IpawsAlertHelper;

/**
 * Job to process IPAWS alerts. Alerts are written to the database by the
 * ipaws CommLink, which handles polling the IPAWS-OPEN server, parsing CAP
 * XMLs, and storing all alerts in the database.
 * 
 * This job processes these alerts, performing filtering based on the contents
 * of the alert (including field values and geographic reach). Irrelevant
 * alerts are marked for purging to be (optionally) deleted by a flush job
 * (TODO).
 * 
 * This job also standardizes geographic data from the alerts and handles DMS
 * selection, message creation, client notification, and in some modes posting
 * alert messages to DMS. 
 *
 * @author Gordon Parikh
 */
public class IpawsProcJob extends Job {

	/** Seconds to offset this job from the start of interval.
	 *  Alerts will generally be polled at the top of each minute, so we will
	 *  run this job 30 seconds after.
	 */
	static private final int OFFSET_SECS = 30;
	
	
	/** Create a new job to process IPAWS alerts in the database. */
	public IpawsProcJob() {
		super(Calendar.MINUTE, 1, Calendar.SECOND, OFFSET_SECS);
	}
	
	/** Process IPAWS alerts in the database. */
	@Override
	public void perform() throws Exception {
		System.out.println("Starting IPAWS alert processing...");
		
		Iterator<IpawsAlertImpl> it = IpawsAlertImpl.iterator();
		while (it.hasNext()) {
			IpawsAlertImpl ia = it.next();
			String area = ia.getArea();
			System.out.println(area);
			
			// normalize the geometry and get a geographic object
			MultiPolygon mp = getGeogPoly(area);
			System.out.println(mp.toString());
			
			// set the MultiPolygon on the alert object
			ia.doSetGeoPoly(mp);
			
			// find DMS in the polygon using a query like this or similar (the
			// ST_Covers is the important part)
			/* SELECT d.name
			 *   FROM iris.dms d
			 *   JOIN iris.geo_loc g
			 *     ON d.geo_loc=g.name
			 *   WHERE ST_Covers(
			 *     (SELECT geo_poly
			 *        FROM event.ipaws
			 *        WHERE name='SRF-TEST-ALERT-00001'),
			 *     ST_Point(g.lon, g.lat)::geography);
			 *  */
		}
	}
	
	/** Use the area section of an IPAWS alert to creating a PostGIS
	 *  MultiPolygon geography object. If a polygon section is found, it is
	 *  used to create a MultiPolygon object (one for each polygon). If there is
	 *  no polygon, the other location information is used to look up one or
	 *  more polygons (TODO, after demo).
	 */
	private MultiPolygon getGeogPoly(String area) {
		// get a JSON object from the area string (which is in JSON syntax)
		JSONObject jo = new JSONObject(area);
		
		// get the "polygon" section
		// **** TODO this is where we would normalize geometry ****
		String ps = null;
		if (jo.has("polygon"))
			ps = (String) jo.get("polygon");
		
		// if we didn't get a polygon, check the other fields in the area to
		// find one we can use to lookup a geographical area
		if (ps == null) {
			// TODO - set ps to something else in the end
			// OR maybe do something else like return a MultiPolygon object
			// directly
		}
		
		// reformat the string so PostGIS will accept it
		try {
			String pgps = formatMultiPolyStr(ps);
			System.out.println(pgps);
			return new MultiPolygon(pgps);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/** Reformat the text taken from the polygon section of a CAP alert's area
	 *  section to match the WKT syntax used by PostGIS.
	 *  
	 *  TODO will need to test this to make sure it works with multiple
	 *  polygons (after demo). For now we assume it only gets one polygon.
	 */
	private String formatMultiPolyStr(String capPolyStr)
			throws NoSuchFieldException {
		// the string comes in as space-delimited coordinate pairs (which
		// themselves are separated by commas) in lat, lon order,
		// e.g.: 45.0,-93.0 45.0,-93.1 ...
		// we need something that looks like this (note coordinates are in
		// lon, lat order (which is x, y)
		// MULTIPOLYGON(((-93.0 45.0, -93.1 45.0, ...), (...)))
		
		// start a StringBuilder
		StringBuilder sb = new StringBuilder();
		
		// TODO change this when fixing for multiple polygons
		sb.append("MULTIPOLYGON(((");
		
		// split the polygon string on spaces to get coordinate pairs
		String coords[] = capPolyStr.split(" ");
		for (String c: coords) {
			String clatlon[] = c.split(",");
			String lat, lon;
			if (clatlon.length == 2) {
				lat = clatlon[0];
				lon = clatlon[1];
			} else {
				throw new NoSuchFieldException(
						"Problem decoding polygon field");
			}
			// add the coordinates to the string as "lon lat"
			sb.append(lon);
			sb.append(" ");
			sb.append(lat);
			sb.append(", ");
		}
		
		// remove the trailing comma
		System.out.println(sb.substring(sb.length()-2));
		if (sb.substring(sb.length()-2).equals(", "))
			sb.setLength(sb.length()-2);
		
		// TODO change this when fixing for multiple polygons
		sb.append(")))");
		
		return sb.toString();
	}
	
}















