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

import java.util.Iterator;

import us.mn.state.dot.tms.utils.UniqueNameCreator;

/**
 * Helper class for CAP response type substitution values. Used on the client
 * and server.
 *
 * @author Gordon Parikh
 */
public class CapResponseTypeHelper extends BaseHelper {

	/** Don't instantiate */
	private CapResponseTypeHelper() {
		assert false;
	}
	
	/** Lookup the response type substitution value with the specified name */
	static public CapResponseType lookup(String name) {
		return (CapResponseType) namespace.lookupObject(
				CapResponseType.SONAR_TYPE, name);
	}
	
	/** Get an CapResponseType object iterator */
	static public Iterator<CapResponseType> iterator() {
		return new IteratorWrapper<CapResponseType>(namespace.iterator(
				CapResponseType.SONAR_TYPE));
	}

	/** Name creator */
	static UniqueNameCreator UNC;
	static {
		UNC = new UniqueNameCreator("cap_resp_type_%d", (n)->lookup(n));
		UNC.setMaxLength(24);
	}

	/** Create a unique CapResponseType record name */
	static public String createUniqueName() {
		return UNC.createUniqueName();
	}
}
