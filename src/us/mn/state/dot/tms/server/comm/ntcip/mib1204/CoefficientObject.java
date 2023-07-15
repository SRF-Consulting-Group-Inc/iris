/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2022  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import us.mn.state.dot.tms.server.comm.snmp.ASN1Integer;
import us.mn.state.dot.tms.utils.Json;

/**
 * Coefficient (as in friction) object in tenths of a percent.
 *
 * @author Gordon Parikh
 * @author Douglas Lau
 */
public class CoefficientObject {

	/** A value of 1001 is an error condition or missing value */
	static private final int ERROR_MISSING = 1001;

	/** Json percent key */
	private final String key;

	/** Integer MIB node */
	public final ASN1Integer node;

	/** Create a percent object */
	public CoefficientObject(String k, ASN1Integer n) {
		key = k;
		node = n;
		node.setInteger(ERROR_MISSING);
	}

	/** Get percent value.
	 * @return Percent or null if missing */
	public Integer getPercent() {
		int p = node.getInteger();
		return (p >= 0 && p < ERROR_MISSING)
		      ? Integer.valueOf(p)
		      : null;
	}

	/** Get JSON representation */
	public String toJson() {
		String s = Json.num(key, getPercent());
		return s;
	}
}
