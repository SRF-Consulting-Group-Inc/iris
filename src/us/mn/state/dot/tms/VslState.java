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
package us.mn.state.dot.tms;

/**
 * VSL recommendation state enumeration.  The ordinal values correspond to
 * the records in the iris.vsl_state look-up table.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public enum VslState {

	/** No recommendation is waiting */
	NONE("None"),

	/** Recommendation is waiting for an operator */
	PENDING("Pending"),

	/** Recommendation was approved by an operator */
	APPROVED("Approved"),

	/** Recommendation was rejected by an operator */
	REJECTED("Rejected"),

	/** Recommendation is held by a waiting period */
	HELD("Held");

	/** Create a new recommendation state */
	private VslState(String d) {
		description = d;
	}

	/** Description of the state */
	public final String description;

	/** Get the string representation */
	@Override
	public String toString() {
		return description;
	}

	/** Get a recommendation state from an ordinal value */
	static public VslState fromOrdinal(int o) {
		if (o >= 0 && o < values().length)
			return values()[o];
		else
			return NONE;
	}
}
