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

/**
 * Helper class for IPAWS Alert Deployers. Used on the client and server.
 *
 * @author Gordon Parikh
 */
public class IpawsAlertConfigHelper extends BaseHelper {
	
	/** Don't instantiate */
	private IpawsAlertConfigHelper() {
		assert false;
	}
	
	/** Lookup the alert deployer with the specified name */
	static public IpawsAlertConfig lookup(String name) {
		return (IpawsAlertConfig) namespace.lookupObject(
				IpawsAlertConfig.SONAR_TYPE, name);
	}

	/** Get an IpawsAlertConfig object iterator */
	static public Iterator<IpawsAlertConfig> iterator() {
		return new IteratorWrapper<IpawsAlertConfig>(namespace.iterator(
				IpawsAlertConfig.SONAR_TYPE));
	}
	
	/** Name prefix */
	static private final String NAME_PREFIX = "ipaws_cfg_";
	
	/** Maximum number of records in the table (for generating unique names */
	static private final int MAX_RECORDS = (int) Math.pow(10, 12);
	
	/** Get the first available unique name for a new alert config. */
	// TODO change to use UniqueNameCreator once merged with video changes
	static public String createUniqueName() {
		for (int i = 0; i <= MAX_RECORDS; ++i) {
			String n = NAME_PREFIX + String.valueOf(i);
			if (lookup(n) == null)
				return n;
		}
		return null;
	}
}
