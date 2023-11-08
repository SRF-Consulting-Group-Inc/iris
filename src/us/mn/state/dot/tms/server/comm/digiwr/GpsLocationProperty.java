/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2023  SRF Consulting Group
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
package us.mn.state.dot.tms.server.comm.digiwr;

import java.io.IOException;

import us.mn.state.dot.tms.server.comm.AsciiDeviceProperty;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Property to send a "at\mibs=gps.0.stats" command to a
 * Digi WR series modem and to parse the GPS coordinates
 * and GPS-lock status from the response.
 *
 * @author John L. Stanley
 */
public class GpsLocationProperty extends AsciiDeviceProperty {

	/** Parse a double from a Digi WR GPS latitude/longitude response.
	 * @param str		String being parsed
	 * @param pat		Pattern to parse
	 * @return If it finds a match, it returns a Double
	 *  containing a positive value.  If it doesn't find
	 *  a match, it returns null.
	 */
	static private Double parseDigiDbl(String str, Pattern pat) {
		try {
			Matcher m = pat.matcher(str);
			if (m.matches()) {
				String sNum = m.group(1);
				return Double.valueOf(sNum);
			}
		}
		catch (NumberFormatException | NullPointerException ex) {
			; // fall thru and return null
		}
		return null;
	}

	/** Patterns for positive and negative latitude/longitude
	 *  values in Digi WR GPS format */
	static final Pattern latPosPat = Pattern.compile("stats.latitude = ([0-9]+[\\.][0-9]+) N");
	static final Pattern latNegPat = Pattern.compile("stats.latitude = ([0-9]+[\\.][0-9]+) S");
	static final Pattern lonPosPat = Pattern.compile("stats.longitude = ([0-9]+[\\.][0-9]+) E");
	static final Pattern lonNegPat = Pattern.compile("stats.longitude = ([0-9]+[\\.][0-9]+) W");

	/** Parse a double from a Digi GPS lat/lon string.
	 * @param str		String being parsed
	 * @param posPat	Positive pattern to look for
	 * @param negPat	Negative pattern to look for
	 * @return If it finds a match, it returns a Double
	 *  containing a signed numeric value. If it doesn't
	 *  find a match, it returns null.
	 */
	static private Double parseLatLon(String str, 
			Pattern posPat, Pattern negPat) {
		Double d;
		d = parseDigiDbl(str, posPat);
		if (d != null)
			return d;
		d = parseDigiDbl(str, negPat);
		if (d != null)
			return -d;
		return null;
	}

	/** GPS lock flag */
	private Boolean gps_lock;

	/** Did we get a GPS lock (valid)? */
	public boolean gotGpsLock() {
		return (gps_lock != null) ? gps_lock : false;
	}

	/** GPS latitude */
	private Double lat;

	/** Get GPS latitude */
	public double getLat() {
		return (lat != null) ? lat : 0;
	}

	/** GPS longitude */
	private Double lon;

	/** Get GPS longitude */
	public double getLon() {
		return (lon != null) ? lon : 0;
	}

	/** Create a new GPS location property */
	public GpsLocationProperty() {
		super("at\\mibs=gps.0.stats\r");
	}

	/** Did we get a valid response? */
	@Override
	public boolean gotValidResponse() {
		return (gps_lock != null) && (lat != null) && (lon != null);
	}

	/** Parse three lines from Digi WR-series modem's many-line
	 *  response.
	 * @param resp Response string.
	 * @return Returns false until we have parsed a full response.
	 * @throws IOException if a response line is longer than max_chars.
	 */
	@Override
	protected boolean parseResponse(String resp) throws IOException {
		if (resp.startsWith("stats.integrity")) {
			gps_lock = resp.startsWith("stats.integrity = Valid");
		}
		else {
			Double d;
			d = parseLatLon(resp, latPosPat, latNegPat);
			if (d != null)
				lat = d;
			else {
				d = parseLatLon(resp, lonPosPat, lonNegPat);
				if (d != null)
					lon = d;
				else {
					// Deal with possible failure mode
					// of all properties being blank.
					if (resp.equals("OK"))
						return true;
				}
			}
		}
		return gotValidResponse();
	}

	/** Get a string representation */
	@Override
	public String toString() {
		return "lat:" + lat + " lon:" + lon + " gps_lock:" + gps_lock;
	}
}
