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
public class IpawsAlertDeployerHelper extends BaseHelper {
	
	/** Don't instantiate */
	private IpawsAlertDeployerHelper() {
		assert false;
	}
	
	/** Lookup the alert deployer with the specified name */
	static public IpawsAlertDeployer lookup(String name) {
		return (IpawsAlertDeployer) namespace.lookupObject(
				IpawsAlertDeployer.SONAR_TYPE, name);
	}

	/** Lookup an alert deployer object for the specified IpawsAlert name. */
	static public IpawsAlertDeployer lookupAlertDeployerName(String alertId) {
		Iterator<IpawsAlertDeployer> it = iterator();
		while (it.hasNext()) {
			IpawsAlertDeployer ian = it.next();
			if (ian.getAlertId().equals(alertId))
				return ian;
		}
		return null;
	}
	
	/** Get an IpawsAlertDeployer object iterator */
	static public Iterator<IpawsAlertDeployer> iterator() {
		return new IteratorWrapper<IpawsAlertDeployer>(namespace.iterator(
				IpawsAlertDeployer.SONAR_TYPE));
	}
}
