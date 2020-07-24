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

import java.util.Date;
import java.util.Iterator;

/**
 * Helper class for IPAWS Alerts. Used on the client and server.
 *
 * @author Gordon Parikh
 */
public class IpawsAlertHelper extends BaseHelper {
	
	/** Don't instantiate */
	private IpawsAlertHelper() {
		assert false;
	}
	
	/** Lookup the alert with the specified name */
	static public IpawsAlert lookup(String name) {
		return (IpawsAlert) namespace.lookupObject(IpawsAlert.SONAR_TYPE, name);
	}

	/** Get an IpawsAlert object iterator */
	static public Iterator<IpawsAlert> iterator() {
		return new IteratorWrapper<IpawsAlert>(namespace.iterator(
				IpawsAlert.SONAR_TYPE));
	}
	
	/** Get the start date/time for an alert. Checks onset time first, then
	 *  effective time, and finally sent time (which is required).
	 */
	static public Date getAlertStart(IpawsAlert ia) {
		Date alertStart = null;
		if (ia != null) {
			alertStart = ia.getOnsetDate();
			if (alertStart == null)
				alertStart = ia.getEffectiveDate();
			if (alertStart == null)
				alertStart = ia.getSentDate();
		}
		return alertStart;
	}
}
