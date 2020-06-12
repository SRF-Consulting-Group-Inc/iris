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
 * Helper class for IPAWS Alert Notifiers. Used on the client and server.
 *
 * @author Gordon Parikh
 */
public class IpawsAlertNotifierHelper extends BaseHelper {
	
	/** Don't instantiate */
	private IpawsAlertNotifierHelper() {
		assert false;
	}
	
	/** Lookup the alert notifier with the specified name */
	static public IpawsAlertNotifier lookup(String name) {
		return (IpawsAlertNotifier) namespace.lookupObject(
				IpawsAlertNotifier.SONAR_TYPE, name);
	}

	/** Lookup an alert notifier object for the specified IpawsAlert name. */
	static public IpawsAlertNotifier lookupAlertNotifierName(String alertId) {
		Iterator<IpawsAlertNotifier> it = iterator();
		while (it.hasNext()) {
			IpawsAlertNotifier ian = it.next();
			if (ian.getAlertId().equals(alertId))
				return ian;
		}
		return null;
	}
	
	/** Get an IpawsAlertNotifier object iterator */
	static public Iterator<IpawsAlertNotifier> iterator() {
		return new IteratorWrapper<IpawsAlertNotifier>(namespace.iterator(
				IpawsAlertNotifier.SONAR_TYPE));
	}
}
