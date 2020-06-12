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
package us.mn.state.dot.tms.client.ipaws;

import java.util.Iterator;

import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.IpawsAlertNotifier;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.GeoLocManager;
import us.mn.state.dot.tms.client.proxy.ProxyDescriptor;
import us.mn.state.dot.tms.client.proxy.ProxyManager;
import us.mn.state.dot.tms.client.proxy.ProxyTheme;
import us.mn.state.dot.tms.client.proxy.SwingProxyAdapter;

/**
 * An IPAWS alert manager is a container for SONAR IpawsAlertNotifierObjects.
 *
 * @author Gordon Parikh
 */
public class IpawsManager extends ProxyManager<IpawsAlertNotifier> {
	
	/** IpawsAlertNotifier cache */
	private final TypeCache<IpawsAlertNotifier> cache;
	
	/** Proxy listener for SONAR updates */
	private final SwingProxyAdapter<IpawsAlertNotifier> listener =
			new SwingProxyAdapter<IpawsAlertNotifier>() {
		@Override
		protected void proxyAddedSwing(IpawsAlertNotifier ian) {
			if (ian != null) {
				System.out.println("Got new alert: " + ian.getAlertId());
				System.out.println("    Has DMS: " + ian.getDms());
				System.out.println("    Has MULTI: " + ian.getMulti());
			}
		}
		
		@Override
		protected void proxyChangedSwing(IpawsAlertNotifier ian, String attr) {
			if (ian != null) {
				System.out.println("Got update to alert: " + ian.getAlertId() +
						" attribute: " + attr);
				
				if (attr == "dms")
					System.out.println("    Has DMS: " + ian.getDms());
				else if (attr == "multi")
					System.out.println("    Has MULTI: " + ian.getMulti());
			}
		}
	};
	
	/** Create a proxy descriptor.
	 * 
	 *  TODO should we make an IpawsCache class? It will probably become
	 *  evident that we should at some point...
	 */
	static private ProxyDescriptor<IpawsAlertNotifier> descriptor(Session s) {
		return new ProxyDescriptor<IpawsAlertNotifier>(
			s.getSonarState().getIpawsCache(), false);
	}
	
	public IpawsManager(Session s, GeoLocManager lm) {
		super(s, lm, descriptor(s), 10);
		// TODO may change things about this
		
		// add the listener to the cache
		cache = s.getSonarState().getIpawsCache();
		cache.addProxyListener(listener);
		Iterator<IpawsAlertNotifier> it = cache.iterator();
		while (it.hasNext()) {
			IpawsAlertNotifier ian = it.next();
			System.out.println("Have alert: " + ian.getAlertId());
		}
	}

	@Override
	protected ProxyTheme<IpawsAlertNotifier> createTheme() {
		// TODO Auto-generated method stub
		return new ProxyTheme<IpawsAlertNotifier>(this, null);
	}

	@Override
	protected GeoLoc getGeoLoc(IpawsAlertNotifier proxy) {
		// TODO need to figure out how to handle this, since IPAWS alerts are
		// areas and not points...
		return null;
	}
	
}