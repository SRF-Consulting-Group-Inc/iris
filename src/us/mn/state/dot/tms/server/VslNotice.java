/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2026  SRF Consulting Group
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A VSL notification, for the VSL feeds (VSL-NTF-02 to 07).
 *
 * TODO: the wording and the information shared on public feeds are to be
 *       decided with NDDOT during testing.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslNotice {

	/** Notification type */
	static public enum Type {

		/** Operator approval requested (VSL-NTF-03) */
		PENDING("VSL approval requested", true),

		/** Operator override (VSL-NTF-04) */
		OVERRIDE("VSL override", false),

		/** Override cleared (VSL-NTF-05) */
		OVERRIDE_CLEARED("VSL override cleared", false),

		/** Posted speed changed (VSL-NTF-06) */
		POSTED("VSL speed changed", false),

		/** Sensor or communication fault (VSL-NTF-07) */
		FAULT("VSL fault", true),

		/** Fault resolved (VSL-NTF-07) */
		FAULT_CLEARED("VSL fault resolved", true);

		/** Create a notification type */
		private Type(String d, boolean c) {
			description = d;
			condition = c;
		}

		/** Short description of the type (VSL-NTF-02) */
		public final String description;

		/** Flag set for a condition, which is not repeated within the
		 * repeat interval (VSL-NTF-08).  Overrides and posted speeds
		 * are changes, so they are always sent.
		 *
		 * TODO: confirm with NDDOT which types are conditions. */
		public final boolean condition;
	}

	/** Format a time, like "October 12, 2026, 2:35 p.m." */
	static public String formatTime(long t) {
		String tm = new SimpleDateFormat("MMMM d, yyyy, h:mm a")
			.format(new Date(t));
		return tm.replace(":00 ", " ")
		         .replace("AM", "a.m.")
		         .replace("PM", "p.m.");
	}

	/** Format a speed, like "55 mph" */
	static public String formatSpeed(Integer s) {
		return (s != null) ? s + " mph" : "none";
	}

	/** Notification type */
	public final Type type;

	/** Time the notification was generated */
	public final long time;

	/** Name of the sign */
	public final String sign;

	/** Location of the sign */
	public final String location;

	/** Condition, for finding repeat notifications */
	public final String key;

	/** Lines of detail, like "Posted speed: 55 mph" */
	private final ArrayList<String> lines = new ArrayList<String>();

	/** Create a new notification.
	 * @param t Notification type.
	 * @param tm Time generated.
	 * @param s Sign name.
	 * @param loc Sign location.
	 * @param k Condition, for finding repeat notifications. */
	public VslNotice(Type t, long tm, String s, String loc, String k) {
		type = t;
		time = tm;
		sign = s;
		location = loc;
		key = t + " " + k;
	}

	/** Add a line of detail */
	public VslNotice add(String label, Object value) {
		lines.add(label + ": " + value);
		return this;
	}

	/** Add road surface information (VSL-NTF-03).
	 * @param ws Name of the weather sensor.
	 * @param r Reading of the weather sensor, or null. */
	public VslNotice addReading(String ws, VslReading r) {
		if (r != null) {
			add("Weather sensor", ws);
			add("Road surface", r.surface.toString().toLowerCase()
				.replace('_', ' '));
			add("Surface temperature", Math.round(r.surf_temp_f) +
				" F");
			add("Visibility", Math.round(r.visibility_ft) + " ft");
			add("Wind speed", Math.round(r.wind_speed_mph) +
				" mph");
		}
		return this;
	}

	/** Get the title */
	public String getTitle() {
		return type.description + ": " + sign;
	}

	/** Get all lines, starting with the time, sign and location
	 * (VSL-NTF-02) */
	public List<String> getLines() {
		ArrayList<String> all = new ArrayList<String>();
		all.add("Time: " + formatTime(time));
		all.add("Sign: " + sign);
		all.add("Location: " + location);
		all.addAll(lines);
		return all;
	}

	/** Check if this repeats a condition sent earlier (VSL-NTF-08).
	 * @param prev Time of the earlier notification, or null.
	 * @param repeat_ms Repeat interval (ms).
	 * @return true if the notification should not be sent. */
	public boolean isRepeat(Long prev, long repeat_ms) {
		return type.condition && prev != null &&
		       time < prev + repeat_ms;
	}

	/** Get the notification as a string, for the VSL log */
	@Override
	public String toString() {
		return getTitle() + " (" + String.join("; ", getLines()) + ")";
	}
}
