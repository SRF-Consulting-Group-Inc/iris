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

import us.mn.state.dot.tms.WeatherSensor;
import us.mn.state.dot.tms.WeatherSensorHelper;
import us.mn.state.dot.tms.units.Distance;
import static us.mn.state.dot.tms.units.Distance.Units.FEET;
import static us.mn.state.dot.tms.units.Distance.Units.METERS;
import us.mn.state.dot.tms.units.Speed;
import static us.mn.state.dot.tms.units.Speed.Units.KPH;
import static us.mn.state.dot.tms.units.Speed.Units.MPH;
import us.mn.state.dot.tms.units.Temperature;
import static us.mn.state.dot.tms.units.Temperature.Units.CELSIUS;
import static us.mn.state.dot.tms.units.Temperature.Units.FAHRENHEIT;

/**
 * Weather sensor measurements for VSL, in the units of the VSL equations.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslReading {

	/** Check if a weather sensor can be used for VSL.  Any missing or
	 * failed measurement makes the whole sensor unusable (VSL-FLT-01).
	 * @param ws Weather sensor to check.
	 * @return Reason the sensor can't be used, or null if it can. */
	static public String checkSensor(WeatherSensor ws) {
		if (ws.getStamp() == null)
			return "no sample";
		if (WeatherSensorHelper.isSampleExpired(ws))
			return "sample expired";
		String err = ws.getPvmtSensorError();
		if (err != null)
			return "pavement sensor error: " + err;
		String st = ws.getPvmtSurfStatus();
		if (VslSurface.fromStatus(st) == null)
			return "surface status: " + st;
		if (ws.getSurfTemp() == null)
			return "no surface temperature";
		if (ws.getVisibility() == null)
			return "no visibility";
		if (ws.getWindSpeed() == null)
			return "no wind speed";
		if (ws.getHumidity() == null)
			return "no humidity";
		return null;
	}

	/** Create a reading from a weather sensor which can be used */
	static public VslReading fromSensor(WeatherSensor ws) {
		return new VslReading(
			VslSurface.fromStatus(ws.getPvmtSurfStatus()),
			new Temperature(ws.getSurfTemp(), CELSIUS)
				.convert(FAHRENHEIT).value,
			new Distance(ws.getVisibility(), METERS).asFloat(FEET),
			new Speed(ws.getWindSpeed(), KPH).asFloat(MPH),
			ws.getHumidity()
		);
	}

	/** Surface category */
	public final VslSurface surface;

	/** Surface temperature (degrees Fahrenheit) */
	public final double surf_temp_f;

	/** Visibility (feet) */
	public final double visibility_ft;

	/** Average wind speed (mph) */
	public final double wind_speed_mph;

	/** Relative humidity (percent) */
	public final double humidity_pct;

	/** Create a reading */
	public VslReading(VslSurface s, double st, double vis, double ws,
		double rh)
	{
		surface = s;
		surf_temp_f = st;
		visibility_ft = vis;
		wind_speed_mph = ws;
		humidity_pct = rh;
	}

	/** Get the reading as a string */
	@Override
	public String toString() {
		return surface + ", " + Math.round(surf_temp_f) + " F, " +
			Math.round(visibility_ft) + " ft, " +
			Math.round(wind_speed_mph) + " mph, " +
			Math.round(humidity_pct) + "%";
	}
}
