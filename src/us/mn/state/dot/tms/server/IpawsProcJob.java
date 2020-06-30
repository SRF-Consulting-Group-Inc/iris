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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONObject;
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Polygon;

import us.mn.state.dot.sched.Job;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.IpawsAlert;
import us.mn.state.dot.tms.IpawsAlertHelper;
import us.mn.state.dot.tms.TMSException;

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

	/** TODO temporary maps for looking up bits of MULTI for messages.
	 *  We'll change these to SONAR objects with database tables after the
	 *  demo.
	 */
	/** For looking up events */
	private HashMap<String, String> eventLookup =
			new HashMap<String, String>();
	
	/** For looking up response types */
	private HashMap<String, String> respTypeLookup =
			new HashMap<String, String>();

	/** For looking up urgency */
	private HashMap<String, String> urgencyLookup =
			new HashMap<String, String>();
		
	/** For looking up text to prepend to alert time (depends on if alert is
	 *  in the future, current, or past)
	 */
	private static final String PREPEND_FUTURE = "STARTING ";
	private static final String PREPEND_CURRENT = "UNTIL ";
	private static final String ALERT_PAST = "ALL CLEAR";
	private static final String AM = " AM";
	private static final String PM = " PM";
	
	/** For looking up the QuickMessage we should use to assemble the entire
	 *  message
	 */
	private HashMap<String, String> quickMessageLookup =
			new HashMap<String, String>();
	
	/** Create a new job to process IPAWS alerts in the database. */
	public IpawsProcJob() {
		super(Calendar.MINUTE, 1, Calendar.SECOND, OFFSET_SECS);
		
		// TODO Temporary - add some entries to our lookup "tables"
		eventLookup.put("Severe Thunderstorm Warning",
				"THUDERSTORM[nl]WARNING");
		eventLookup.put("Tornado Warning","TORNADO WARNING");
		eventLookup.put("Blizzard Warning","BLIZZARD[nl]WARNING");
		eventLookup.put("Winter Storm Warning","WINTER STORM[nl]WARNING");
		
		// TODO we probably want to add an event field for each response type
		// and urgency for additional filtering (and a default when event not
		// found)
		respTypeLookup.put("Shelter", "SHELTER");
		respTypeLookup.put("Prepare", "TRAVEL NOT[nl]ADVISED");
		
		urgencyLookup.put("Immediate", "NOW");
		urgencyLookup.put("Expected", "SOON");
		
		quickMessageLookup.put("Severe Thunderstorm Warning",
				"[capevent][nl][captime][np][capresponse][nl][capurgency]");
		quickMessageLookup.put("Blizzard Warning",
				"[capevent][nl][captime][np][capresponse][nl][capurgency]");
		quickMessageLookup.put("Winter Storm Warning",
				"[capevent][nl][captime][np][capresponse][nl][capurgency]");
		quickMessageLookup.put("Tornado Warning",
				"[capevent][nl][captime][nl][capresponse] [capurgency]");
	}
	
	/** Process IPAWS alerts in the database. */
	@Override
	public void perform() throws Exception {
		// go through all alerts
		Iterator<IpawsAlertImpl> it = IpawsAlertImpl.iterator();
		while (it.hasNext()) {
			// TODO add top-level filtering (iris.ipaws_events or similar)
			
			IpawsAlertImpl ia = it.next();
			
			// TODO add check to avoid re-processing every time (?)
			
			System.out.println("Processing IPAWS alert: " + ia.getName());
			
			String area = ia.getArea();
			System.out.println(area);
			
			// normalize the geometry and get a geographic object
			MultiPolygon mp = getGeogPoly(area);
			System.out.println(mp.toString());
			
			// set the MultiPolygon on the alert object
			ia.doSetGeoPoly(mp);
			
			// find DMS in the polygon and generate an alert notifier object
			selectDms(ia);
			
			// check if the alert was marked purgeable - if it was, move on
			if (!ia.getPurgeable()) {
				// if it wasn't, generate MULTI for the message
				// get the alert notifier object to store it first
				IpawsAlertNotifierImpl ian =
						IpawsAlertNotifierImpl.lookupFromAlert(ia.getName());
				String alertMulti = generateMulti(ia);
				System.out.println("Got MULTI: " + alertMulti);
				ian.setMultiNotify(alertMulti);
			}
		}
	}
	
	/** Generate a MULTI message from an alert. */
	private String generateMulti(IpawsAlertImpl ia) {
		// TODO use lookup tables like IncDescriptor/IncLocator/etc. instead
		// of these hard-coded maps
		
		// We will also have this group signs based on their config (we can
		// maybe/probably use stuff from MultiConfig after merging that) and
		// use that to look up QuickMessages by event type and config (we
		// would need to adjust IpawsAlertNotifier for this too).
		
		// lookup a "QuickMessage" for the event type
		String event = ia.getEvent();
		if (quickMessageLookup.containsKey(event)) {
			String qmMulti = quickMessageLookup.get(event);
			
			// get the fields that we will substitute in
			String evMulti = eventLookup.containsKey(event)
					? eventLookup.get(event) : null;
			
			// TODO need to sort response types based on their severity (e.g.
			// Shelter and Evacuate at the top, etc. (for now we'll just take
			// the first/only one)
			String rt = ia.getResponseTypes().size() > 0
					? ia.getResponseTypes().get(0) : null;
			String rtMulti = respTypeLookup.containsKey(rt)
					? respTypeLookup.get(rt) : null;
			
			String urg = ia.getUrgency();
			String urgMulti = urgencyLookup.containsKey(urg)
					? urgencyLookup.get(urg) : null;
			
			// if we got fields for everything, build the message
			if (evMulti != null && rtMulti != null && urgMulti != null) {
				// get the time field
				String timeMulti = getTimeMulti(ia);
				if (timeMulti == ALERT_PAST) {
					// if the alert was in the past, clear the response type
					// and urgency fields
					rtMulti = "";
					urgMulti = "";
					System.out.println("Alert event: " + event +
							" all clear");
					String m = qmMulti.replace("[capevent]", evMulti)
								  .replace("[captime]", timeMulti)
								  .replace("[capresponse]", rtMulti)
								  .replace("[capurgency]", urgMulti);
					if (m.endsWith("[nl]"))
						m = m.substring(0, m.length()-4);
					if (m.endsWith("[np]"))
						m = m.substring(0, m.length()-4);
					return m;
				}
				
				// replace the fields in the QuickMessage and return
				System.out.println("Alert event: " + event + " response type: "
						+ rt + " urgency: " + urg);
				return qmMulti.replace("[capevent]", evMulti)
							  .replace("[captime]", timeMulti)
							  .replace("[capresponse]", rtMulti)
							  .replace("[capurgency]", urgMulti);
			}
		}
		
		// return null if we couldn't generate a message (nothing else will
		// happen)
		return null;
	}
	
	/** Generate the time section of an alert MULTI string. This will include
	 *  a text string based on whether the alert is in the future, is current,
	 *  or in the past, prepended to the starting hour of the alert (floored,
	 *  if the alert starts after the top of the hour, and ceiling-rounded if
	 *  the alert ends after the top of the hour), or with no other text if
	 *  if the alert was in the past.
	 */
	private String getTimeMulti(IpawsAlertImpl ia) {
		// get the time fields of the alert
		// try the onset time first, then effective, and finally sent (which
		// is required)
		Date alertStart = ia.getOnsetDate();
		if (alertStart == null)
			alertStart = ia.getEffectiveDate();
		if (alertStart == null)
			alertStart = ia.getSentDate();
		
		// IPAWS requires alert expiration field
		Date alertEnd = ia.getExpirationDate();
		
		// check the time of the alert relative to now
		Date now = new Date();
		
		// NOTE since time is measured to the millisecond, it is basically
		// 100% likely that now will be strictly after alertStart
		if (now.before(alertStart)) {
			// before alert start - alert is in future
			// get hour field of alert start
			Calendar c = Calendar.getInstance();
			c.setTime(alertStart);
			int hour = c.get(Calendar.HOUR);
			int iAMPM = c.get(Calendar.AM_PM);
			String ampm = iAMPM == Calendar.AM ? AM : PM;
			return PREPEND_FUTURE + String.valueOf(hour) + ampm;
		} else if (now.after(alertStart) && now.before(alertEnd)) {
			// after alert start and before end - alert is current
			// get hour field of alert end
			Calendar c = Calendar.getInstance();
			c.setTime(alertEnd);
			int hour = c.get(Calendar.HOUR);
			int min = c.get(Calendar.MINUTE);
			
			// round up to the next hour if the minute is over 0
			if (min > 0)
				hour += 1;
			
			int iAMPM = c.get(Calendar.AM_PM);
			String ampm = iAMPM == Calendar.AM ? AM : PM;
			return PREPEND_CURRENT + String.valueOf(hour) + ampm;
		} else if (now.after(alertEnd)) {
			// after alert end - alert is in past
			// just return the "ALL CLEAR" message
			return ALERT_PAST;
		}
		// we should never get here, but just in case
		return null;
	}
	
	/** Select the set of DMS to be used for the given IPAWS alert. Uses
	 *  PostGIS to handle the geospatial operations. Must be called after 
	 *  getGeoPoly() is used to create a polygon object from the alert's area
	 *  field, and after that polygon is written to the database with the
	 *  alert's doSetGeoPoly() method. 
	 *  
	 *  If at least one sign is selected, an IpawsAlertNotifier object is
	 *  created to notify clients for approval (TODO auto mode??).
	 *  
	 *  If no signs are found, no notifier object is created and the IpawsAlert
	 *  object is marked purgeable.
	 */
	private void selectDms(IpawsAlertImpl ia) throws TMSException {
		// query the list of DMS that falls within the MultiPolygon for this
		// alert - use array_agg to get one array instead of multiple rows
		IpawsAlertImpl.store.query(
				"SELECT array_agg(d.name) FROM iris." + DMS.SONAR_TYPE + " d" +
				" JOIN iris." + GeoLoc.SONAR_TYPE + " g ON d.geo_loc=g.name" +
				" WHERE ST_Covers((SELECT geo_poly FROM " + ia.getTable() +
				" WHERE name='" + ia.getName() + "')," +
				" ST_Point(g.lon, g.lat)::geography);",
				new ResultFactory() {
			@Override
			public void create(ResultSet row) throws Exception {
				try {
					// make sure we got some DMS
					String[] dms = (String[])row.getArray(1).getArray();
					System.out.println("Got DMS: " + Arrays.toString(dms));
					
					if (dms.length > 0) {
						// if we did, try to look up an alert
						IpawsAlertNotifierImpl ian =
						  IpawsAlertNotifierImpl.lookupFromAlert(ia.getName());
						
						if (ian == null) {
							// if we didn't find one, generate a new name for
							// the alert notifier and construct a new object
							String name = IpawsAlertNotifierImpl
									.getUniqueName();
							ian = new IpawsAlertNotifierImpl(
									name, ia.getName()); //, dms);
							ian.notifyCreate();
						}
						ian.setDmsNotify(dms);
					} else
						// if we didn't, mark the alert as purgeable
						ia.doSetPurgeable(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
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
		if (sb.substring(sb.length()-2).equals(", "))
			sb.setLength(sb.length()-2);
		
		// TODO change this when fixing for multiple polygons
		sb.append(")))");
		
		return sb.toString();
	}
	
}















