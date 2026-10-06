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
