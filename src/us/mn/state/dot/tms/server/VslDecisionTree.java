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
 * VSL decision tree, from the NDDOT RWIS VSL Speed Logic Diagram
 * (version 6, December 5, 2022).  The surface category picks the branch;
 * humidity, surface temperature and visibility pick the equation.
 *
 * Dry pavement with high humidity is treated as wet, and wet pavement at
 * or below freezing is treated as slick.  The diagram's "or unavailable"
 * paths are never taken, since a sensor missing any measurement is not
 * used at all.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslDecisionTree {

	/** Maximum speed on slick pavement (mph) */
	static public final int SLICK_MAX_SPEED = 65;

	/** Humidity at which dry pavement is treated as wet (percent) */
	static private final double DRY_HUMIDITY = 95;

	/** Visibility above which dry pavement is clear (feet) */
	static private final double DRY_VISIBILITY = 345;

	/** Surface temperature at which wet pavement is slick (F) */
	static private final double WET_FREEZING = 32;

	/** Surface temperature above which wet pavement is clear (F) */
	static private final double WET_CLEAR = 36;

	/** Visibility above which wet pavement uses weather terms (feet) */
	static private final double WET_VISIBILITY = 420;

	/** Visibility above which slick pavement uses weather terms (feet) */
	static private final double SLICK_VISIBILITY = 520;

	/** Humidity below which equation 4 is used (percent) */
	static private final double HUMIDITY_LOW = 55;

	/** Humidity below which equation 5 is used (percent) */
	static private final double HUMIDITY_HIGH = 90;

	/** Equation chosen by the tree, with any ceiling on its branch */
	static public class Choice {

		/** Equation to calculate */
		public final VslEquation equation;

		/** Maximum speed of the branch (mph), or null if none */
		public final Integer max_speed;

		/** Create a choice */
		private Choice(VslEquation eq, Integer mx) {
			equation = eq;
			max_speed = mx;
		}

		/** Get the choice as a string */
		@Override
		public String toString() {
			return (max_speed != null)
			      ? equation + " (max " + max_speed + ")"
			      : equation.toString();
		}
	}

	/** Don't allow instances to be created */
	private VslDecisionTree() {
		assert false;
	}

	/** Choose the equation for a weather sensor reading */
	static public Choice choose(VslReading r) {
		switch (r.surface) {
		case DRY:
			return chooseDry(r);
		case WET:
			return chooseWet(r);
		default:
			return chooseSlick(r);
		}
	}

	/** Choose an equation for dry pavement */
	static private Choice chooseDry(VslReading r) {
		if (r.humidity_pct >= DRY_HUMIDITY)
			return chooseWet(r);
		return (r.visibility_ft > DRY_VISIBILITY)
		      ? new Choice(VslEquation.EQ0, null)
		      : new Choice(VslEquation.EQ1, null);
	}

	/** Choose an equation for wet pavement */
	static private Choice chooseWet(VslReading r) {
		if (r.surf_temp_f <= WET_FREEZING)
			return chooseSlick(r);
		if (r.visibility_ft <= WET_VISIBILITY)
			return new Choice(VslEquation.EQ2, null);
		if (r.surf_temp_f > WET_CLEAR)
			return new Choice(VslEquation.EQ0, null);
		return (r.humidity_pct < HUMIDITY_LOW)
		      ? new Choice(VslEquation.EQ4, null)
		      : new Choice(VslEquation.EQ5, null);
	}

	/** Choose an equation for slick pavement */
	static private Choice chooseSlick(VslReading r) {
		Integer mx = SLICK_MAX_SPEED;
		if (r.visibility_ft <= SLICK_VISIBILITY)
			return new Choice(VslEquation.EQ3, mx);
		if (r.humidity_pct < HUMIDITY_LOW)
			return new Choice(VslEquation.EQ4, mx);
		return (r.humidity_pct < HUMIDITY_HIGH)
		      ? new Choice(VslEquation.EQ5, mx)
		      : new Choice(VslEquation.EQ6, mx);
	}
}
