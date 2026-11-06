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
package us.mn.state.dot.tms.server.event;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import us.mn.state.dot.tms.EventConfigHelper;
import us.mn.state.dot.tms.EventType;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.server.Storable;

/**
 * This is a class for logging NDDOT VSL calculations to a database.  Each
 * calculation has a row for each of the sign's weather sensors and
 * detectors, which are stored after the calculation row.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslCalcEvent extends BaseEvent {

	/** Base class for a row of a calculation */
	abstract private class Row implements Storable {

		/** Get the primary key name */
		@Override
		public String getPKeyName() {
			return "name";
		}

		/** Get the primary key */
		@Override
		public String getPKey() {
			return null;
		}

		/** Create a mapping of the calculation's key columns */
		protected HashMap<String, Object> keyColumns() {
			HashMap<String, Object> map =
				new HashMap<String, Object>();
			map.put("vsl_sign", vsl_sign);
			map.put("event_date",
				new Timestamp(event_date.getTime()));
			return map;
		}
	}

	/** Weather sensor row */
	private class StationRow extends Row {

		/** Weather sensor name */
		private final String weather_sensor;

		/** Reason the sensor can't be used, or null if it was used */
		private final String problem;

		/** Surface category, or null */
		private final String surface;

		/** Surface temperature (degrees Fahrenheit), or null */
		private final Double surf_temp_f;

		/** Visibility (feet), or null */
		private final Double visibility_ft;

		/** Average wind speed (mph), or null */
		private final Double wind_speed_mph;

		/** Relative humidity (percent), or null */
		private final Double humidity_pct;

		/** Equation label, or null */
		private final String equation;

		/** Maximum speed of the equation (mph), or null */
		private final Integer equation_max;

		/** Candidate speed (mph), or null */
		private final Double candidate;

		/** Create a weather sensor row */
		private StationRow(String ws, String p, String sfc, Double st,
			Double vis, Double wind, Double rh, String eq,
			Integer eqm, Double cs)
		{
			weather_sensor = ws;
			problem = p;
			surface = sfc;
			surf_temp_f = st;
			visibility_ft = vis;
			wind_speed_mph = wind;
			humidity_pct = rh;
			equation = eq;
			equation_max = eqm;
			candidate = cs;
		}

		/** Get the database table name */
		@Override
		public String getTable() {
			return "event.vsl_calc_station";
		}

		/** Get a mapping of the columns */
		@Override
		public Map<String, Object> getColumns() {
			HashMap<String, Object> map = keyColumns();
			map.put("weather_sensor", weather_sensor);
			map.put("problem", problem);
			map.put("surface", surface);
			map.put("surf_temp_f", surf_temp_f);
			map.put("visibility_ft", visibility_ft);
			map.put("wind_speed_mph", wind_speed_mph);
			map.put("humidity_pct", humidity_pct);
			map.put("equation", equation);
			map.put("equation_max", equation_max);
			map.put("candidate", candidate);
			return map;
		}
	}

	/** Detector row */
	private class DetectorRow extends Row {

		/** Detector name */
		private final String detector;

		/** Speed (mph), or null if none */
		private final Float speed;

		/** Reason there is no speed, or null */
		private final String problem;

		/** Create a detector row */
		private DetectorRow(String det, Float s, String p) {
			detector = det;
			speed = s;
			problem = p;
		}

		/** Get the database table name */
		@Override
		public String getTable() {
			return "event.vsl_calc_detector";
		}

		/** Get a mapping of the columns */
		@Override
		public Map<String, Object> getColumns() {
			HashMap<String, Object> map = keyColumns();
			map.put("detector", detector);
			map.put("speed", speed);
			map.put("problem", problem);
			return map;
		}
	}

	/** VSL sign name */
	private final String vsl_sign;

	/** Sign's minimum speed (mph) */
	private final int min_speed;

	/** Sign's maximum speed (mph) */
	private final int max_speed;

	/** Posted speed before the calculation (mph), or null */
	private final Integer posted_speed;

	/** Time stamp of the detector sample interval (ms), or 0 */
	private long det_stamp;

	/** Traffic speed (mph), or null */
	private Double traffic_speed;

	/** Weather sensor with the lowest candidate speed, or null */
	private String weather_sensor;

	/** Recommended speed (mph), or null */
	private Integer rec_speed;

	/** Reason no speed was calculated, or null */
	private String fault;

	/** Approval action, or null */
	private String action;

	/** Speed of the approval action (mph), or null */
	private Integer action_speed;

	/** Reason for the approval action, or null */
	private String reason;

	/** Weather sensor and detector rows */
	private final ArrayList<Row> rows = new ArrayList<Row>();

	/** Create a new VSL calculation event */
	public VslCalcEvent(String vs, int mn, int mx, Integer ps) {
		super(EventType.VSL_CALCULATION);
		vsl_sign = vs;
		min_speed = mn;
		max_speed = mx;
		posted_speed = ps;
	}

	/** Set the result of the calculation.
	 * @param ds Time stamp of the detector sample interval (ms), or 0.
	 * @param ts Traffic speed (mph), or null.
	 * @param ws Weather sensor with the lowest candidate speed, or null.
	 * @param rs Recommended speed (mph), or null.
	 * @param f Reason no speed was calculated, or null. */
	public void setResult(long ds, Double ts, String ws, Integer rs,
		String f)
	{
		det_stamp = ds;
		traffic_speed = ts;
		weather_sensor = ws;
		rec_speed = rs;
		fault = f;
	}

	/** Set the approval decision.
	 * @param a Approval action, or null.
	 * @param s Speed of the action (mph), or null.
	 * @param r Reason for the decision, or null. */
	public void setDecision(String a, Integer s, String r) {
		action = a;
		action_speed = s;
		reason = r;
	}

	/** Add a weather sensor which can't be used */
	public void addStation(String ws, String problem) {
		rows.add(new StationRow(ws, problem, null, null, null, null,
			null, null, null, null));
	}

	/** Add a weather sensor which was used.
	 * @param ws Weather sensor name.
	 * @param sfc Surface category.
	 * @param st Surface temperature (degrees Fahrenheit).
	 * @param vis Visibility (feet).
	 * @param wind Average wind speed (mph).
	 * @param rh Relative humidity (percent).
	 * @param eq Equation label.
	 * @param eqm Maximum speed of the equation (mph), or null.
	 * @param cs Candidate speed (mph). */
	public void addStation(String ws, String sfc, double st, double vis,
		double wind, double rh, String eq, Integer eqm, double cs)
	{
		rows.add(new StationRow(ws, null, sfc, st, vis, wind, rh, eq,
			eqm, cs));
	}

	/** Add a detector.
	 * @param det Detector name.
	 * @param s Speed (mph), or null.
	 * @param p Reason there is no speed, or null. */
	public void addDetector(String det, Float s, String p) {
		rows.add(new DetectorRow(det, s, p));
	}

	/** Get the weather sensor and detector rows */
	public List<? extends Storable> getRows() {
		return rows;
	}

	/** Get the event config name */
	@Override
	protected String eventConfigName() {
		return "vsl_calc";
	}

	/** Get the database table name */
	@Override
	public String getTable() {
		return "event.vsl_calc";
	}

	/** Get a mapping of the columns */
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("vsl_sign", vsl_sign);
		map.put("event_date", new Timestamp(event_date.getTime()));
		map.put("event_desc", event_type.id);
		map.put("min_speed", min_speed);
		map.put("max_speed", max_speed);
		map.put("detector_time", (det_stamp > 0)
			? new Timestamp(det_stamp)
			: null);
		map.put("traffic_speed", traffic_speed);
		map.put("weather_sensor", weather_sensor);
		map.put("rec_speed", rec_speed);
		map.put("fault", fault);
		map.put("posted_speed", posted_speed);
		map.put("action", action);
		map.put("action_speed", action_speed);
		map.put("reason", reason);
		return map;
	}

	/** Store the calculation, then its weather sensor and detector
	 * rows, which refer to it */
	@Override
	public void doStore() throws TMSException {
		if (EventConfigHelper.isStoreEnabled(eventConfigName())) {
			store.create(this);
			for (Row r: rows)
				store.create(r);
		}
	}
}
