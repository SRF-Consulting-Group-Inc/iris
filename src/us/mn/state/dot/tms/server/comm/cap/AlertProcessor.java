/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2021  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.cap;

import org.json.JSONObject;
import us.mn.state.dot.sched.Job;
import us.mn.state.dot.sched.Scheduler;
import us.mn.state.dot.tms.server.CapAlert;

/**
 * The alert processor stores alerts in the database and processes them.
 *
 * @author Douglas Lau
 */
public class AlertProcessor {

	/** Timer thread for CAP jobs */
	static private final Scheduler SCHED = new Scheduler("cap");

	/** Process one alert */
	public void processAlert(JSONObject ja) {
		String id = null;
		if (ja.has("identifier"))
			id = ja.getString("identifier");
		else if (ja.has("id"))
			id = ja.getString("id");
		
		if (id != null) {
			CapAlert ca = new CapAlert(id, ja);
			SCHED.addJob(new Job() {
				public void perform() {
					ca.process();
				}
			});
		} else
			CapPoller.slog("identifier not found!");
	}
}
