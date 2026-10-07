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

/**
 * VSL speed equations, from the NDDOT RWIS VSL Speed Logic Diagram
 * (version 6, December 5, 2022).
 *
 * Traffic speed from detectors combines differently with each equation:
 * the visibility equations use it as a ceiling, while the weather
 * equations take a 70/30 weighted average with it.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public enum VslEquation {

	/** Clear conditions: the sign's maximum speed */
	EQ0(TrafficMode.NONE),

	/** Low visibility on dry pavement */
	EQ1(TrafficMode.CAP),

	/** Low visibility on wet pavement */
	EQ2(TrafficMode.CAP),

	/** Low visibility on slick pavement */
	EQ3(TrafficMode.CAP),

	/** Surface temperature and wind */
	EQ4(TrafficMode.BLEND),

	/** Surface temperature, wind and humidity */
	EQ5(TrafficMode.BLEND),

	/** Surface temperature, wind and humidity, weighted to humidity */
	EQ6(TrafficMode.BLEND);

	/** How traffic speed combines with an equation */
	public enum TrafficMode {
		/** Traffic speed is not used */
		NONE,

		/** Traffic speed is a ceiling */
		CAP,

		/** 70/30 weighted average with traffic speed */
		BLEND
	}

	/** Surface temperature term (mph) */
	static public double mphSurfTemp(double st_f) {
		return 40.5 * Math.exp(0.01 * st_f);
	}

	/** Average wind speed term (mph) */
	static public double mphWind(double aws_mph) {
		return -0.031 * Math.pow(aws_mph, 1.63) + 75;
	}

	/** Relative humidity term (mph) */
	static public double mphHumidity(double rh_pct) {
		return -0.0009 * Math.pow(rh_pct, 3)
		     + 0.199 * Math.pow(rh_pct, 2)
		     - 15.152 * rh_pct
		     + 457;
	}

	/** Visibility term of equations 1 to 3 (mph) */
	static private double mphVisibility(double vis_ft, double k,
		double d)
	{
		return (-3.67 + Math.sqrt(13.47 + k * vis_ft)) / d + 5;
	}

	/** How this equation uses traffic speed */
	public final TrafficMode traffic_mode;

	/** Create a VSL equation */
	private VslEquation(TrafficMode tm) {
		traffic_mode = tm;
	}

	/** Calculate the equation, without traffic speed.
	 * @param r Weather sensor reading.
	 * @param max_speed Maximum speed (mph), used by EQ0.
	 * @return Speed (mph). */
	public double calculate(VslReading r, int max_speed) {
		switch (this) {
		case EQ1:
			return mphVisibility(r.visibility_ft, 0.12, 0.06);
		case EQ2:
			return mphVisibility(r.visibility_ft, 0.2, 0.1);
		case EQ3:
			return mphVisibility(r.visibility_ft, 0.48, 0.24);
		case EQ4:
			return 0.5 * mphSurfTemp(r.surf_temp_f)
			     + 0.3 * mphWind(r.wind_speed_mph)
			     + 15;
		case EQ5:
			return 0.35 * mphSurfTemp(r.surf_temp_f)
			     + 0.35 * mphWind(r.wind_speed_mph)
			     + 0.3 * mphHumidity(r.humidity_pct);
		case EQ6:
			return 0.35 * mphSurfTemp(r.surf_temp_f)
			     + 0.15 * mphWind(r.wind_speed_mph)
			     + 0.5 * mphHumidity(r.humidity_pct);
		default:
			return max_speed;
		}
	}

	/** Combine an equation result with traffic speed.
	 * @param s Equation result (mph).
	 * @param traffic Traffic speed (mph), or null if not available.
	 * @return Speed (mph). */
	public double applyTraffic(double s, Double traffic) {
		if (traffic == null)
			return s;
		switch (traffic_mode) {
		case CAP:
			return Math.min(s, traffic);
		case BLEND:
			return (70 * traffic + 30 * s) / 100;
		default:
			return s;
		}
	}
}
