/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2010-2024  Minnesota Department of Transportation
 * Copyright (C) 2011  AHMCT, University of California
 * Copyright (C) 2017  Iteris Inc.
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
package us.mn.state.dot.tms;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import us.mn.state.dot.sched.TimeSteward;

/**
 * Helper class for weather sensors.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public class WeatherSensorHelper extends BaseHelper {

	/** Don't allow instances to be created */
	private WeatherSensorHelper() {
		assert false;
	}

	/** Lookup the weather sensor with the specified name */
	static public WeatherSensor lookup(String name) {
		return (WeatherSensor) namespace.lookupObject(
			WeatherSensor.SONAR_TYPE, name);
	}

	/** Get a weather sensor iterator */
	static public Iterator<WeatherSensor> iterator() {
		return new IteratorWrapper<WeatherSensor>(namespace.iterator(
			WeatherSensor.SONAR_TYPE));
	}

	/** Check if the sample data has expired */
	static public boolean isSampleExpired(WeatherSensor ws) {
		if (ws != null) {
			Long st = ws.getStamp();
			if (st == null)
				return false;
			return st + getObsAgeLimitSecs() * 1000 <
				TimeSteward.currentTimeMillis();
		} else
			return false;
	}

	/** Get the sensor observation age limit (secs).
	 * @return The sensor observation age limit. Valid observations have
	 *	   an age less than or equal to this value.  Zero indicates
	 *	   observations never expire. */
	static private int getObsAgeLimitSecs() {
		return SystemAttrEnum.RWIS_OBS_AGE_LIMIT_SECS.getInt();
	}

	/** Get a valid precipitation rate, or null */
	static public Integer getPrecipRate(WeatherSensor ws) {
		return (isSampleExpired(ws))
		      ? null
		      : ws.getPrecipRate();
	}

	/** Get the intensity for the precipitation rate, per ntcip 1204 */
	static public String getPrecipRateIntensity(WeatherSensor ws) {
		Integer pr = getPrecipRate(ws);
		if (pr != null) {
			if (pr <= 0)
				return "none";
			else if (pr < 2)
				return "slight";
			else if (pr < 8)
				return "moderate";
			else
				return "heavy";
		} else
			return "";
	}

	/** Get a sorted map of values to be shown in device tool-tip */
	//FIXME: Rewrite when we get a more robust RWIS implementation 
	public Map<String, Object> getTooltipMap(WeatherSensor ws) {
		Map<String, Object> map = new TreeMap<String, Object>();
		map.put("MaxWindGustSpeed", ws.getMaxWindGustSpeed());
		map.put("Visibility", ws.getVisibility());
		map.put("SurfTemp", ws.getSurfTemp());
		map.put("PvmtFriction", ws.getPvmtFriction());
		return map;
	}
}
