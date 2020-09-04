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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Iterator;

import org.json.JSONObject;
import org.postgis.MultiPolygon;
import us.mn.state.dot.sched.Job;
import us.mn.state.dot.sonar.SonarException;
import us.mn.state.dot.tms.CapCertaintyEnum;
import us.mn.state.dot.tms.CapResponseType;
import us.mn.state.dot.tms.CapResponseTypeEnum;
import us.mn.state.dot.tms.CapResponseTypeHelper;
import us.mn.state.dot.tms.CapSeverityEnum;
import us.mn.state.dot.tms.CapUrgency;
import us.mn.state.dot.tms.CapUrgencyEnum;
import us.mn.state.dot.tms.CapUrgencyHelper;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.DmsMsgPriority;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.GeoLocHelper;
import us.mn.state.dot.tms.IpawsAlert;
import us.mn.state.dot.tms.IpawsAlertConfig;
import us.mn.state.dot.tms.IpawsAlertConfigHelper;
import us.mn.state.dot.tms.IpawsAlertDeployer;
import us.mn.state.dot.tms.IpawsAlertDeployerHelper;
import us.mn.state.dot.tms.IpawsAlertHelper;
import us.mn.state.dot.tms.PushNotificationHelper;
import us.mn.state.dot.tms.QuickMessage;
import us.mn.state.dot.tms.QuickMessageHelper;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.utils.I18N;
import us.mn.state.dot.tms.utils.MultiBuilder;
import us.mn.state.dot.tms.utils.MultiString;

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
		// go through all alerts
		Iterator<IpawsAlertImpl> it = IpawsAlertImpl.iterator();
		while (it.hasNext()) {
			IpawsAlertImpl ia = it.next();
			
			if (ia.getPurgeable() != null)
				continue;
			
			System.out.println("Processing IPAWS alert: " + ia.getName());
			
			String area = ia.getArea();
			System.out.println(area);
			
			// normalize the geometry and get a geographic object
			MultiPolygon mp = getGeogPoly(area);
			System.out.println(mp.toString());
			
			// set the MultiPolygon on the alert object
			ia.doSetGeoPoly(mp);
			
			// generate a GeoLoc for the alert (the alert area's centroid)
			GeoLoc gl = getGeoLoc(ia);
			ia.doSetGeoLoc(gl);
			
			// find DMS in the polygon and generate an alert deployer object
			// this will complete all processing of this alert for this cycle
			checkAlert(ia);
		}
	}
	
	/** Generate a MULTI message from an alert and alert config. */
	private String generateMulti(IpawsAlertImpl ia, IpawsAlertConfig iac,
			Date alertStart, Date alertEnd) {
		// get the message template from the alert config
		String qmn = iac.getQuickMessage();
		System.out.println("Looking up quick message: " + qmn);
		QuickMessage qm = QuickMessageHelper.lookup(qmn);
		String qmMulti = qm != null ? qm.getMulti() : "";
		System.out.println("Got message template: " + qmMulti);
		
		// use a MultiBuilder to process cap action tags
		MultiBuilder builder = new MultiBuilder() {
			@Override
			public void addCapTime(String f_txt, String a_txt, String p_txt) {
				// check the alert times against the current time to know
				// which text and time fields to use
				Date now = new Date();
				String tmplt;
				Date dt;
				if (now.before(alertStart)) {
					// alert hasn't started yet
					tmplt = f_txt;
					dt = alertStart;
				} else if (now.before(alertEnd)) {
					// alert is currently active
					tmplt = a_txt;
					dt = alertEnd;
				} else {
					// alert has expired
					tmplt = p_txt;
					dt = alertEnd;
				}
				
				// format any time strings in the text and add to the msg
				String s = IpawsAlertDeployerHelper.replaceTimeFmt(tmplt,
						dt.toInstant().atZone(ZoneId.systemDefault())
						.toLocalDateTime());
				addSpan(s);
			}
			
			@Override
			public void addCapResponse(String[] rtypes) {
				// make a HashSet of the allowed response types
				HashSet<String> rtSet = new HashSet<String>(
						Arrays.asList(rtypes));
				
				// check the response types in the alert to see if we should
				// substitute anything, taking the highest priority one
				CapResponseTypeEnum maxRT = CapResponseTypeEnum.NONE;
				CapResponseType rtSub = null;
				for (String rt: ia.getResponseTypes()) {
					if (rtSet.contains(rt)) {
						// make sure we have a matching substitution value too
						CapResponseType crt = CapResponseTypeHelper.lookupFor(
								ia.getEvent(), rt);
						
						if (crt != null) {
							CapResponseTypeEnum crte =
									CapResponseTypeEnum.fromValue(rt);
							if (crte.ordinal() > maxRT.ordinal()) {
								maxRT = crte;
								rtSub = crt;
							}
						}
					}
				}
				
				// if we had a match add the MULTI, otherwise leave it blank
				addSpan(rtSub != null ? rtSub.getMulti() : "");
			}
			
			@Override
			public void addCapUrgency(String[] uvals) {
				// make a HashSet of the allowed urgency values
				HashSet<String> urgSet = new HashSet<String>(
						Arrays.asList(uvals));
				
				// check the urgency value in the alert to see if we should
				// substitute anything
				String urg = ia.getUrgency();
				String multi = "";
				if (urgSet.contains(urg)) {
					CapUrgency subst = CapUrgencyHelper.lookupFor(
							ia.getEvent(), urg);
					if (subst != null)
						multi = subst.getMulti();
				}
				addSpan(multi);
			}
		};
		
		// process the QuickMessage with the MultiBuilder
		new MultiString(qmMulti).parse(builder);
		MultiString ms = builder.toMultiString();
		System.out.println("MULTI: " + ms.toString());
		
		// return the MULTI if it's valid and not blank
		if (ms.isValid() && !ms.isBlank()) {
			return ms.toString();
		}
		
		// return null if we couldn't generate a valid message (nothing else
		// will happen)
		return null;
	}
	
	/** Allowed DMS Message Priority values */
	private final static DmsMsgPriority[] ALLOWED_PRIORITIES = {
			DmsMsgPriority.PSA,
			DmsMsgPriority.ALERT,
			DmsMsgPriority.AWS,
			DmsMsgPriority.AWS_HIGH
	};
	
	/** Calculate the message priority for an alert given the urgency,
	 *  severity, and certainty values and weights stored as system attributes.
	 */
	private DmsMsgPriority calculateMsgPriority(IpawsAlertImpl ia) {
		// get the weights
		float wu = SystemAttrEnum.IPAWS_PRIORITY_WEIGHT_URGENCY.getFloat();
		float ws = SystemAttrEnum.IPAWS_PRIORITY_WEIGHT_SEVERITY.getFloat();
		float wc = SystemAttrEnum.IPAWS_PRIORITY_WEIGHT_CERTAINTY.getFloat();
		
		// get the urgency, severity, and certainty values
		CapUrgencyEnum u = CapUrgencyEnum.fromValue(ia.getUrgency());
		CapSeverityEnum s = CapSeverityEnum.fromValue(ia.getSeverity());
		CapCertaintyEnum c = CapCertaintyEnum.fromValue(ia.getCertainty());
		
		// convert those values to decimals
		float uf = (float) u.ordinal() / (float) CapUrgencyEnum.nValues();
		float sf = (float) s.ordinal() / (float) CapSeverityEnum.nValues();
		float cf = (float) c.ordinal() / (float) CapCertaintyEnum.nValues();
		
		// calculate a priority "score" (higher = more important)
		float score = wu * uf + ws * sf + wc * cf;
		
		System.out.println("Priority score: " + wu + " * " + uf + " + " + ws
				+ " * " + sf + " + " + wc + " * " + cf + " = " + score);
		
		// convert the score to an index and return one of the allowed values
		int i = (int) Math.round(score * ALLOWED_PRIORITIES.length);
		if (i >= ALLOWED_PRIORITIES.length)
			i = ALLOWED_PRIORITIES.length - 1;
		else if (i < 0)
			i = 0;
		return ALLOWED_PRIORITIES[i];
	}
	
	/** Check the IpawsAlert provided for relevance to this system and (if
	 *  relevant) process it for posting. Relevance is determined based on
	 *  whether there is one or more existing IpawsAlertConfig objects that
	 *  match the event in the alert and whether the alert area(s) encompass
	 *  any DMS known to the system. 
	 * 	
	 *  DMS selection uses PostGIS to handle the geospatial operations. This
	 *  method must be called after getGeoPoly() is used to create a polygon
	 *  object from the alert's area field, and after that polygon is written
	 *  to the database with the alert's doSetGeoPoly() method.
	 *  
	 *  If at least one sign is selected, an IpawsAlertDeployer object is
	 *  created to notify clients for approval (TODO auto mode??).
	 *  
	 *  If no signs are found, no deployer object is created and the IpawsAlert
	 *  object is marked purgeable.
	 *  
	 *  One deployer object is created for each matching IpawsAlertConfig,
	 *  allowing different messages to be posted to different sign types.
	 */
	private void checkAlert(IpawsAlertImpl ia) throws TMSException {
		// get alert configs for this event type
		String event = ia.getEvent();
		Iterator<IpawsAlertConfig> it = IpawsAlertConfigHelper.iterator();
		
		// collect alert deployers that have been created so we can notify
		// clients about them
		ArrayList<IpawsAlertDeployerImpl> iadList =
				new ArrayList<IpawsAlertDeployerImpl>();
		
		while (it.hasNext()) {
			IpawsAlertConfig iac = it.next();
			if (event.equals(iac.getEvent())) {
				// query the list of DMS that falls within the MultiPolygon
				// for this alert - use array_agg to get one array instead of
				// multiple rows do this once for each sign group
				IpawsAlertImpl.store.query(
				"SELECT array_agg(d.name) FROM iris." + DMS.SONAR_TYPE + " d" +
				" JOIN iris." + GeoLoc.SONAR_TYPE + " g ON d.geo_loc=g.name" +
				" WHERE ST_Covers((SELECT geo_poly FROM " + ia.getTable() +
				" WHERE name='" + ia.getName() + "'), ST_Point(g.lon," + 
				" g.lat)::geography) AND d.name IN (SELECT dms FROM" + 
				" iris.dms_sign_group WHERE sign_group='" +
				iac.getSignGroup() + "');",
				new ResultFactory() {
					@Override
					public void create(ResultSet row) throws Exception {
						try {
							// make sure we got some DMS
							String[] dms = (String[]) row.getArray(1)
									.getArray();
							
							if (dms.length > 0) {
								// if we did, finish processing the alert
								IpawsAlertDeployerImpl iad = 
										createAlertDeployer(ia, dms, iac);
								iadList.add(iad);
							} else
								// if we didn't, mark the alert as purgeable
								ia.doSetPurgeable(true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		}
		
		// note that the alert has been processed - if no deployers were
		// created, the alert can be purged
		ia.doSetPurgeable(iadList.isEmpty());
		
		// TODO create a notifier object to tell the client about the new/
		// updated alert
		// TODO need to create that whole system
	}
	
	/** Create an alert deployer for this alert. Called after querying PostGIS
	 *  for relevant DMS (only if some were found). Handles generating MULTI
	 *  and other object creating housekeeping.
	 */
	private IpawsAlertDeployerImpl createAlertDeployer(IpawsAlertImpl ia,
			String[] dms, IpawsAlertConfig iac)
			throws SonarException, TMSException {
		// try to look up the most recent deployer object for this alert
		IpawsAlertDeployerImpl iad =
				IpawsAlertDeployerImpl.lookupFromAlert(ia.getName());

		// get alert start/end times
		Date aStart = IpawsAlertHelper.getAlertStart(ia);
		Date aEnd = ia.getExpirationDate();
		
		// generate/update MULTI
		String autoMulti = generateMulti(ia, iac, aStart, aEnd);
		
		// if we got a message, calculate priority
		int priority = calculateMsgPriority(ia).ordinal();
		
		// check if any attributes have changed from this last deployer (if we
		// got one)
		if (iad == null || !iad.autoValsEqual(aStart, aEnd, dms, autoMulti)) {
			// if they have, or we didn't get one, make a new one
			// generate a new name and construct the object
			String name = IpawsAlertDeployerImpl.createUniqueName();
			iad = new IpawsAlertDeployerImpl(name, ia.getName(),
					aStart, aEnd, iac.getSignGroup(), dms,
					iac.getQuickMessage(), autoMulti, priority);
			
			// notify so clients receive the new object
			iad.notifyCreate();
			
			// process the alert for deployment
			deployAlert(iad, ia);
		}
		
		// return the deployer that was created for collecting and handling
		// the user notification
		return iad;
	}
	
	/** Process the alert for deploying, checking the mode (auto or manual)
	 *  first. In manual mode, this sends a push notification to clients to
	 *  request a user to review and approve the alert. In auto mode, this
	 *  either deploys the alert then sends a notification indicating the
	 *  new alert, or (if a non-zero timeout is configured) sends a
	 *  notification then waits until the timeout has elapsed and (if a user
	 *  hasn't deployed it manually) deploys the alert.
	 */
	private void deployAlert(IpawsAlertDeployerImpl iad, IpawsAlertImpl ia)
					throws TMSException, SonarException {
		// check the deploy mode and timeouts in system attributes
		boolean autoMode = SystemAttrEnum.IPAWS_DEPLOY_AUTO_MODE.getBoolean();
		int timeout = SystemAttrEnum.IPAWS_DEPLOY_AUTO_TIMEOUT_SECS.getInt();
		
		// generate and send the notification - the static method handles
		// the content of the notification based on the mode and timeout
		PushNotificationImpl pn = getNotification(ia.getEvent(),
				autoMode, timeout, iad.getName());
		
		// process the alert deployment/notification updates
		if (autoMode) {
			// auto mode - check timeout first
			if (timeout > 0) {
				// non-zero timeout - wait for timeout to pass. NOTE that this
				// is already happening in it's own thread, so we will just
				// wait here
				for (int i = 0; i < timeout; i++) {
					// wait a second
					try { Thread.sleep(1000); }
					catch (InterruptedException e) { /* Ignore */ }
					
					// update the notification with a new message (so the user
					// knows how much time they have before the alert deploys)
					pn.setDescriptionNotify(getTimeoutString(
							ia.getEvent(), timeout - i));
				}
				
				// after timeout passes, check if the alert has been deployed
				if (!iad.getDeployed())
					// if it hasn't, deploy it (if it has, we're done!)
					deployAlertDMS(iad);
			} else
				// no timeout - just deploy it
				deployAlertDMS(iad);
		} else {
			// manual mode - send notification to clients - fingers crossed
			// someone sees it!!
			// TODO - the client is going to need to trigger this when the
			// user clicks the "Deploy" button
		}
	}
	
	/** Deploy the alert to DMS. This is what actually sends MULTI to signs. */
	private void deployAlertDMS(IpawsAlertDeployerImpl iad) {
		// TODO
	}
	
	/** Create a notification for an alert deployer given the event type,
	 *  deployment mode (auto/manual), timeout, and the name of the deployer
	 *  object.
	 */
	private static PushNotificationImpl getNotification(String event,
			boolean autoMode, int timeout, String dName)
				throws TMSException, SonarException {
		// substitute the event type into the notification title
		String title;
		try {
			title = String.format(I18N.get("ipaws.notification.title"), event);
		} catch (IllegalFormatException e) {
			title = String.format("New %s Alert", event);
		}
		// same with the description
		String description;
		if (autoMode) {
			if (timeout == 0) {
				try {
					description = String.format(I18N.get(
						"ipaws.notification.description.auto.no_timeout"),
						event);
				} catch (IllegalFormatException e) {
					description = String.format("New %s alert received " +
						"from IPAWS. This alert has been automatically " +
						"deployed.", event);
				}
			} else
				description = getTimeoutString(event, timeout);
		} else {
			try {
				description = String.format(I18N.get(
					"ipaws.notification.description.manual"), event);
			} catch (IllegalFormatException e) {
				description = String.format("New %s alert received from " + 
					"IPAWS. Please review it for deployment.", event);
			}
		}
		// create the notification object with the values we got
		PushNotificationImpl pn = new PushNotificationImpl(
				IpawsAlertDeployer.SONAR_TYPE, dName, title, description);
		
		// notify clients of the creation so they receive it, then return
		pn.notifyCreate();
		return pn;
	}
	
	/** Get a string containing the amount of time until the timeout expires
	 *  (for auto deployment mode) given an event type and the time in seconds.
	 */
	private static String getTimeoutString(String event, int secs) {
		// use the time value to create a duration string
		String dur = Duration.ofSeconds(secs).toString();
		
		// add the string 
		String dFormat = I18N.get(
				"ipaws.notification.description.auto.timeout");
		try {
			return String.format(dFormat, event, dur);
		} catch (IllegalFormatException e) {
			return String.format("New %s alert received from IPAWS. This " +
				"alert will be automatically deployed in %s if no action " +
				"is taken.", event, dur);
		}
	}
	
	/** Use the area section of an IPAWS alert to creating a PostGIS
	 *  MultiPolygon geography object. If a polygon section is found, it is
	 *  used to create a MultiPolygon object (one for each polygon). If there is
	 *  no polygon, the other location information is used to look up one or
	 *  more polygons (TODO).
	 */
	private MultiPolygon getGeogPoly(String area) {
		// get a JSON object from the area string (which is in JSON syntax)
		JSONObject jo = new JSONObject(area);
		
		// get the "polygon" section
		// **** TODO this is where we would normalize geometry ****
		String ps = null;
		if (jo.has("polygon"))
			ps = jo.getString("polygon");
		
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
	
	/** Generate a GeoLoc for the alert as the alert area's centroid. */
	private GeoLocImpl getGeoLoc(IpawsAlertImpl ia) throws TMSException {
		// generate a GeoLoc for the alert (the alert's centroid)
		IpawsAlertImpl.store.query(
		"SELECT ST_AsText(ST_Centroid(geo_poly)) FROM event." +
		IpawsAlert.SONAR_TYPE + " WHERE name='" + ia.getName() + "';",
		new ResultFactory() {
			@Override
			public void create(ResultSet row) throws Exception {
				// strip out the lat/long from the POINT(lon lat) string
				String pStr = row.getString(1);
				String llStr = pStr.replace("POINT(", "").replace(")", "");
				String[] ll = llStr.split(" ");
				if (ll.length == 2) {
					Double lon = Double.valueOf(ll[0]);
					Double lat = Double.valueOf(ll[1]);
					
					// construct or update a GeoLoc that will be associated
					// with the deployer object
					GeoLocImpl gl = GeoLocImpl.lookupGeoLoc(ia.getName());
					if (gl == null) {
						gl = new GeoLocImpl(ia.getName(),
								IpawsAlert.SONAR_TYPE, lat, lon);
						gl.notifyCreate();
					} else {
						gl.setLatNotify(lat);
						gl.setLonNotify(lon);
					}
					
				}
			}
		});
		
		// lookup and return the GeoLoc for this alert (which should hopefully
		// exist now)
		return GeoLocImpl.lookupGeoLoc(ia.getName());
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















