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
 * VSL road surface category, from the NTCIP 1204 pavement surface status
 * reported by a weather sensor.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public enum VslSurface {

	/** Dry pavement */
	DRY,

	/** Wet pavement */
	WET,

	/** Slick pavement in spots */
	SLICK_IN_SPOTS,

	/** Slick pavement */
	SLICK;

	/** Get the surface category of a pavement surface status.
	 * @param st Surface status (name of an essSurfaceStatus value).
	 * @return Surface category, or null if the status is not usable. */
	static public VslSurface fromStatus(String st) {
		if (st == null)
			return null;
		switch (st) {
		case "dry":
			return DRY;
		case "traceMoisture":
		case "wet":
		case "chemicallyWet":
		case "dew":
		case "absorption":
		case "absorptionAtDewpoint":
			return WET;
		case "iceWatch":
		case "snowWatch":
		case "frost":
			return SLICK_IN_SPOTS;
		case "iceWarning":
		case "snowWarning":
			return SLICK;
		default:
			return null;
		}
	}
}
