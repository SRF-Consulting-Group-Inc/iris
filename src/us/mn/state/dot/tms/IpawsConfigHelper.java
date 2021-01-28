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
 * Helper class for IPAWS Alert Deployers.  Used on the client and server.
 *
 * @author Gordon Parikh
 */
public class IpawsConfigHelper extends BaseHelper {

	/** Don't instantiate */
	private IpawsConfigHelper() {
		assert false;
	}

	/** Lookup the alert config with the specified name */
	static public IpawsConfig lookup(String name) {
		return (IpawsConfig) namespace.lookupObject(
				IpawsConfig.SONAR_TYPE, name);
	}

	/** Get an IpawsConfig object iterator */
	static public Iterator<IpawsConfig> iterator() {
		return new IteratorWrapper<IpawsConfig>(namespace.iterator(
				IpawsConfig.SONAR_TYPE));
	}

	/** Name creator */
	static private final UniqueNameCreator UNC = new UniqueNameCreator(
		"ipaws_cfg_%d", 24, (n)->lookup(n));

	/** Create a unique IpawsConfig record name */
	static public String createUniqueName() {
		return UNC.createUniqueName();
	}
}
