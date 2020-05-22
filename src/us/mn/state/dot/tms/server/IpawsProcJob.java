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

import java.util.Calendar;

import us.mn.state.dot.sched.Job;

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
	}
}
