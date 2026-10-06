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
 * VSL approval mode enumeration.  The ordinal values correspond to the
 * records in the iris.vsl_approval_mode look-up table.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public enum VslApprovalMode {

	/** An operator approves every speed change */
	MANUAL("Manual"),

	/** Speed changes post unless an operator rejects them in time */
	SEMI_AUTOMATED("Semi-Automated"),

	/** Speed changes post without operator approval */
	AUTOMATED("Automated");

	/** Create a new approval mode */
	private VslApprovalMode(String d) {
		description = d;
	}

	/** Description of the approval mode */
	public final String description;

	/** Get the string representation */
	@Override
	public String toString() {
		return description;
	}

	/** Get an approval mode from an ordinal value */
	static public VslApprovalMode fromOrdinal(int o) {
		if (o >= 0 && o < values().length)
			return values()[o];
		else
			return MANUAL;
	}
}
