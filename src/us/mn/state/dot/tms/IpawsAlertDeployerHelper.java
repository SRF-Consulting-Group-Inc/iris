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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
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

	/** Lookup an alert deployer object for the specified IpawsAlert name.
	 *  Returns the most recent deployer for this alert.
	 */
	static public IpawsAlertDeployer lookupDeployerFromAlert(String alertId) {
		// get the list of deployers for this alert sorted newest to oldest
		ArrayList<IpawsAlertDeployer> deployers = getDeployerList(alertId);
		if (deployers.size() > 0)
			return deployers.get(0);
		return null;
	}
	
	/** Comparator to sort IpawsAlertDeployers by genTime. */
	static private class DeployerGenTimeComparator
				implements Comparator<IpawsAlertDeployer> {
		
		/** Multiplier to sort ascending (+1) or descending (-1) */
		private int ascMult = 1;
		
		public DeployerGenTimeComparator(boolean ascending) {
			if (!ascending)
				ascMult = -1;
		}
		
		@Override
		public int compare(IpawsAlertDeployer o1, IpawsAlertDeployer o2) {
			// get genTimes
			Date gt0 = o1.getGenTime();
			Date gt1 = o2.getGenTime();
			
			// check for nulls (just in case) - the non-null one should
			// be higher
			if (gt0 == null && gt1 != null)
				return 1;
			else if (gt0 != null && gt1 == null)
				return -1;
			else if (gt0 == null && gt1 == null)
				return 0;
			
			// no nulls - compare dates and apply multiplier
			return ascMult * gt0.compareTo(gt1);
		}
		
	}
	
	/** Get a list of all deployers associated with the alert ID provided.
	 *  Objects are sorted from newest to oldest.
	 */
	static public ArrayList<IpawsAlertDeployer>
					getDeployerList(String alertId) {
		ArrayList<IpawsAlertDeployer> deployers =
				new ArrayList<IpawsAlertDeployer>();
		
		// find all deployers associated with this alert
		Iterator<IpawsAlertDeployer> it = iterator();
		while (it.hasNext()) {
			IpawsAlertDeployer ian = it.next();
			if (ian.getAlertId().equals(alertId))
				deployers.add(ian);
		}
		
		// sort the list using a custom comparator
		deployers.sort(new DeployerGenTimeComparator(false));
		return deployers;
	}
	
	
	/** Get an IpawsAlertDeployer object iterator */
	static public Iterator<IpawsAlertDeployer> iterator() {
		return new IteratorWrapper<IpawsAlertDeployer>(namespace.iterator(
				IpawsAlertDeployer.SONAR_TYPE));
	}
}
