/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2020  SRF Consulting Group, Inc.
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

/**
 * Common Alerting Protocol (CAP) certainty field value enum. Used for IPAWS
 * alert processing for generating messages for posting to DMS. Values are
 * taken from the OASIS CAP Standard v1.2. Values are ordered from least
 * (Unknown/Unlikely) to most (Observed) emphatic for use in calculating
 * message priority.
 *
 * @author Gordon Parikh
 */
public enum CapCertaintyEnum {
	UNKNOWN("Unknown", "Certainty unknown"),
	UNLIKELY("Unlikely", "Not expected to occur (p ~ 0)"),
	POSSIBLE("Possible", "Possible but not likely (p <= ~50%)"),
	LIKELY("Likely", "Likely (p > ~50%)"),
	OBSERVED("Observed", "Determine to have occurred or to be ongoing");

	/** Value used in CAP messages */
	public final String value;
	
	/** Description of value from CAP standard */
	public final String description;
	
	private CapCertaintyEnum(String v, String d) {
		value = v;
		description = d;
	}
	
	/** Return the CapCertaintyEnum from the value provided. */
	static public CapCertaintyEnum fromValue(String v) {
		for (CapCertaintyEnum e: values()) {
			if (e.value.equals(v))
				return e;
		}
		return UNKNOWN;
	}
	
	/** Return an array of the string values (i.e. the ones seen in a CAP
	 *  message).
	 */
	static public String[] stringValues() {
		CapCertaintyEnum[] evals = values();
		String[] svals = new String[evals.length];
		for (int i = 0; i < evals.length; ++i)
			svals[i] = evals[i].value;
		return svals;
	}
}
