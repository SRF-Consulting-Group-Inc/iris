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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JOptionPane;

import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.IpawsAlertNotifier;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.incident.IncidentMarker;
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
	
	/** Proxy listener for SONAR updates. TODO this will change a lot. */
	private final SwingProxyAdapter<IpawsAlertNotifier> listener =
			new SwingProxyAdapter<IpawsAlertNotifier>(true) {
		
		private boolean enumComplete = false;
		
		// we use these for state for capturing attribute changes
		private StringBuilder sb;
		private boolean newAlert = false;
		private boolean haveDms;
		private boolean haveMulti;
		
		@Override
		protected void enumerationCompleteSwing(
				Collection<IpawsAlertNotifier> ians) {
			enumComplete = true;
		}
		
		String[] options = new String[] {"Deploy", "Cancel"};
		
		@Override
		protected void proxyAddedSwing(IpawsAlertNotifier ian) {
			if (enumComplete && ian != null) {
				newAlert = true;
				sb = new StringBuilder();
				sb.append("<html><body><p style='width: 400px;'>");
				sb.append("New alert: " + ian.getAlertId());
				boolean content = false;
				if (ian.getDms() != null && ian.getDms().length > 0
						&& ian.getDms()[0] != null) {
					sb.append("<br>    ");
					sb.append("Selected DMS: " + Arrays.toString(ian.getDms()));
					haveDms = true;
				}
				if (ian.getMulti() != null) {
					sb.append("<br>    ");
					sb.append("MULTI: " + ian.getMulti());
					haveMulti = true;
				}
				if (haveDms && haveMulti) {
					sb.append("</p></body></html>");
					String msg = sb.toString();
					JOptionPane.showOptionDialog(session.getDesktop(), 
			        		msg, "New Alert", JOptionPane.DEFAULT_OPTION,
			        		JOptionPane.PLAIN_MESSAGE, null,
			        		options, options[0]);
				}
			}
		}
		
		@Override
		protected void proxyChangedSwing(IpawsAlertNotifier ian, String attr) {
			if (ian != null) {
				if (sb == null) {
					sb = new StringBuilder();
					sb.append("<html><body><p style='width: 400px;'>");
					sb.append("Update to alert: " + ian.getAlertId());
					newAlert = false;
				}
				
				sb.append("<br>    ");
				if (attr.equals("dms") && ian.getDms() != null
						&& ian.getDms().length > 0 && ian.getDms()[0] != null) {
					System.out.println(ian.getDms());
					sb.append("Selected DMS: "
							+ Arrays.toString(ian.getDms()));
					haveDms = true;
				} else if (attr.equals("multi") && ian.getMulti() != null) {
					sb.append("MULTI: " + ian.getMulti());
					haveMulti = true;
				}
				
				if (!newAlert || (haveDms && haveMulti)) {
					newAlert = false;
					haveDms = false;
					haveMulti = false;
					String msg = sb.toString();
					sb = null;
					String t = newAlert ? "New Alert" : "Updated Alert";
					JOptionPane.showOptionDialog(session.getDesktop(), 
			        		msg, t, JOptionPane.DEFAULT_OPTION,
			        		JOptionPane.PLAIN_MESSAGE, null,
			        		options, options[0]);
				}
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
		// TODO for now we're stealing the incident marker
		return new ProxyTheme<IpawsAlertNotifier>(this, new IncidentMarker());
	}

	@Override
	protected GeoLoc getGeoLoc(IpawsAlertNotifier proxy) {
		// TODO need to figure out how to handle this, since IPAWS alerts are
		// areas and not points...
		return null;
	}
	
}