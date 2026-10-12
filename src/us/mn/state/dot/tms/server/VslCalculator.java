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

import java.util.ArrayList;
import java.util.Iterator;
import us.mn.state.dot.tms.Detector;
import us.mn.state.dot.tms.DetectorHelper;
import us.mn.state.dot.tms.Hashtags;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.WeatherSensor;
import us.mn.state.dot.tms.WeatherSensorHelper;

/**
 * Calculates the recommended speed of a VSL sign (VSL-CAL-01 to 03).  The
 * sign's weather sensors and detectors are the ones whose notes contain
 * its hashtag.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslCalculator {

	/** Calculate the candidate speed of one weather sensor reading.
	 * @param r Weather sensor reading.
	 * @param max_speed Sign's maximum speed (mph).
	 * @param traffic Traffic speed (mph), or null if not available.
	 * @return Candidate speed (mph). */
	static public double candidateSpeed(VslReading r, int max_speed,
		Double traffic)
	{
		VslDecisionTree.Choice c = VslDecisionTree.choose(r);
		int mx = (c.max_speed != null)
		       ? Math.min(max_speed, c.max_speed)
		       : max_speed;
		double s = c.equation.calculate(r, mx);
		return Math.min(c.equation.applyTraffic(s, traffic), mx);
	}

	/** Get the recommended speed from the lowest candidate speed.  It
	 * is rounded down to a multiple of the rounding increment
	 * (VSL-CAL-02), then kept within the sign's speeds (VSL-CAL-03).
	 * @param lowest Lowest candidate speed (mph).
	 * @param min_speed Sign's minimum speed (mph).
	 * @param max_speed Sign's maximum speed (mph).
	 * @param rounding Rounding increment (mph).
	 * @return Recommended speed (mph). */
	static public int recommendedSpeed(double lowest, int min_speed,
		int max_speed, int rounding)
	{
		int inc = Math.max(rounding, 1);
		int s = (int) Math.floor(lowest / inc) * inc;
		return Math.max(min_speed, Math.min(s, max_speed));
	}

	/** Hashtag of the sign's weather sensors and detectors */
	private final String hashtag;

	/** Sign's minimum speed (mph) */
	private final int min_speed;

	/** Sign's maximum speed (mph) */
	private final int max_speed;

	/** Lines describing the calculation, for the VSL log */
	public final ArrayList<String> detail = new ArrayList<String>();

	/** Reason no speed was calculated, or null (VSL-NTF-07) */
	public String fault;

	/** Name of the weather sensor with the lowest candidate speed */
	public String sensor;

	/** Reading of the weather sensor with the lowest candidate speed,
	 * for road surface information (VSL-NTF-03) */
	public VslReading reading;

	/** Create a calculator for a VSL sign */
	public VslCalculator(String ht, int mn, int mx) {
		hashtag = ht;
		min_speed = mn;
		max_speed = mx;
	}

	/** Calculate the recommended speed.
	 * @return Recommended speed (mph), or null if no weather sensor can
	 *         be used (VSL-FLT-02). */
	public Integer calculate() {
		if (hashtag == null) {
			fault = "no hashtag";
			detail.add(fault);
			return null;
		}
		Double traffic = findTrafficSpeed();
		Double lowest = null;
		Iterator<WeatherSensor> it = WeatherSensorHelper.iterator();
		while (it.hasNext()) {
			WeatherSensor ws = it.next();
			if (new Hashtags(ws.getNotes()).contains(hashtag)) {
				Double s = calculateCandidate(ws, traffic);
				if (s != null && (lowest == null || s < lowest))
				{
					lowest = s;
					sensor = ws.getName();
					reading = VslReading.fromSensor(ws);
				}
			}
		}
		if (lowest == null) {
			fault = "no usable weather sensor";
			detail.add(fault);
			return null;
		}
		int rs = recommendedSpeed(lowest, min_speed, max_speed,
			SystemAttrEnum.NDDOT_VSL_SPEED_ROUNDING.getInt());
		detail.add("recommended " + rs);
		return rs;
	}

	/** Calculate the candidate speed of one weather sensor.
	 * @return Candidate speed (mph), or null if it can't be used. */
	private Double calculateCandidate(WeatherSensor ws, Double traffic) {
		String problem = VslReading.checkSensor(ws);
		if (problem != null) {
			detail.add(ws.getName() + ": not used, " + problem);
			return null;
		}
		VslReading r = VslReading.fromSensor(ws);
		double s = candidateSpeed(r, max_speed, traffic);
		detail.add(ws.getName() + ": " + r + "; " +
			VslDecisionTree.choose(r) + " = " + Math.round(s));
		return s;
	}

	/** Find the lowest measured traffic speed of the sign's detectors.
	 * Detectors which are not sampling, or which do not measure speed,
	 * are not used (VSL-FLT-05).
	 * @return Traffic speed (mph), or null if none. */
	private Double findTrafficSpeed() {
		long stamp = StationDataJob.getStamp();
		int per_ms = DetectorImpl.BIN_PERIOD_MS;
		Double lowest = null;
		Iterator<Detector> it = DetectorHelper.iterator();
		while (it.hasNext()) {
			Detector d = it.next();
			if (d instanceof DetectorImpl &&
			    new Hashtags(d.getNotes()).contains(hashtag))
			{
				DetectorImpl det = (DetectorImpl) d;
				float s = det.getSpeedRaw(stamp, per_ms);
				if (s > 0 && (lowest == null || s < lowest))
					lowest = (double) s;
			}
		}
		detail.add("traffic speed " + ((lowest != null)
			? Math.round(lowest) + " mph"
			: "none"));
		return lowest;
	}
}
