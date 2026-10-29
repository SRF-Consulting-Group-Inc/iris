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

import java.util.Iterator;

/**
 * Helper class for VSL signs.
 *
 * @author Coby Christiansen - SRF Consulting
 * @author John L. Stanley - SRF Consulting
 */
public class VslSignHelper extends BaseHelper {

	/** Don't allow instances to be created */
	private VslSignHelper() {
		assert false;
	}

	/** Check if NDDOT VSL is enabled */
	static public boolean isEnabled() {
		return SystemAttrEnum.NDDOT_VSL_ENABLE.getBoolean();
	}

	/** Get the color of VSL signs on the DMS tab.
	 * @return RGB value (0xRRGGBB), or null to show VSL signs as
	 *         dedicated-purpose signs. */
	static public Integer getColor() {
		return parseColor(SystemAttrEnum.NDDOT_VSL_COLOR.getString());
	}

	/** Parse a color: "R,G,B", "(R,G,B)" or "#RRGGBB".
	 * @param c Color string.
	 * @return RGB value (0xRRGGBB), or null if blank or invalid. */
	static public Integer parseColor(String c) {
		String v = (c != null) ? c.trim() : "";
		if (v.matches("#[0-9A-Fa-f]{6}"))
			return Integer.parseInt(v.substring(1), 16);
		if (v.startsWith("(") && v.endsWith(")"))
			v = v.substring(1, v.length() - 1);
		String[] parts = v.split(",", -1);
		if (parts.length != 3)
			return null;
		int rgb = 0;
		for (String p: parts) {
			String n = p.trim();
			if (!n.matches("\\d{1,3}"))
				return null;
			int i = Integer.parseInt(n);
			if (i > 255)
				return null;
			rgb = (rgb << 8) | i;
		}
		return rgb;
	}

	/** Lookup the VSL sign with the specified name */
	static public VslSign lookup(String name) {
		return (VslSign) namespace.lookupObject(VslSign.SONAR_TYPE,
			name);
	}

	/** Get a VSL sign iterator */
	static public Iterator<VslSign> iterator() {
		return new IteratorWrapper<VslSign>(namespace.iterator(
			VslSign.SONAR_TYPE));
	}
}
