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
package us.mn.state.dot.tms.client.alert;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import us.mn.state.dot.sonar.client.ProxyListener;
import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.IpawsAlert;
import us.mn.state.dot.tms.IpawsAlertHelper;
import us.mn.state.dot.tms.IpawsAlertDeployer;
import us.mn.state.dot.tms.ItemStyle;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.incident.IncidentMarker;
import us.mn.state.dot.tms.client.proxy.GeoLocManager;
import us.mn.state.dot.tms.client.proxy.ProxyDescriptor;
import us.mn.state.dot.tms.client.proxy.ProxyManager;
import us.mn.state.dot.tms.client.proxy.ProxyTheme;
import us.mn.state.dot.tms.client.proxy.SwingProxyAdapter;

/**
 * An (IPAWS) AlertManager is a container for SONAR IpawsAlertDeployerObjects.
 * NOTE this would need changing to let alert tab handle other types of
 * alerts.
 *
 * @author Gordon Parikh
 */
public class AlertManager extends ProxyManager<IpawsAlertDeployer> {
	
	/** IpawsAlertDeployer cache */
	private final TypeCache<IpawsAlertDeployer> adcache;

	/** IpawsAlert cache */
	private final TypeCache<IpawsAlert> acache;
	
	/** Proxy listener for SONAR updates. TODO this will change a lot. */
	private final SwingProxyAdapter<IpawsAlertDeployer> listener =
			new SwingProxyAdapter<IpawsAlertDeployer>(true) {
		
		private boolean enumComplete = false;
		
		// we use these for state for capturing attribute changes
		private StringBuilder sb;
		private boolean newAlert = false;
		private boolean haveDms;
		private boolean haveMulti;
		
		@Override
		protected void enumerationCompleteSwing(
				Collection<IpawsAlertDeployer> ians) {
			enumComplete = true;
		}
		
		String[] options = new String[] {"Deploy", "Cancel"};
		
		@Override
		protected void proxyAddedSwing(IpawsAlertDeployer ian) {
			if (enumComplete && ian != null) {
				newAlert = true;
				sb = new StringBuilder();
				sb.append("<html><body><p style='width: 400px;'>");
				sb.append("New alert: " + ian.getAlertId());
				boolean content = false;
				if (ian.getAutoDms() != null && ian.getAutoDms().length > 0
						&& ian.getAutoDms()[0] != null) {
					sb.append("<br>    ");
					sb.append("Selected DMS: " +
							Arrays.toString(ian.getAutoDms()));
					haveDms = true;
				}
				if (ian.getAutoMulti() != null) {
					sb.append("<br>    ");
					sb.append("MULTI: " + ian.getAutoMulti());
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
		protected void proxyChangedSwing(IpawsAlertDeployer ian, String attr) {
			if (ian != null) {
				if (sb == null) {
					sb = new StringBuilder();
					sb.append("<html><body><p style='width: 400px;'>");
					sb.append("Update to alert: " + ian.getAlertId());
					newAlert = false;
				}
				
				sb.append("<br>    ");
				if (attr.equals("dms") && ian.getAutoDms() != null
						&& ian.getAutoDms().length > 0 && ian.getAutoDms()[0] != null) {
					System.out.println(ian.getAutoDms());
					sb.append("Selected DMS: "
							+ Arrays.toString(ian.getAutoDms()));
					haveDms = true;
				} else if (attr.equals("multi")
						&& ian.getAutoMulti() != null) {
					sb.append("MULTI: " + ian.getAutoMulti());
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
	static private ProxyDescriptor<IpawsAlertDeployer> descriptor(Session s) {
		return new ProxyDescriptor<IpawsAlertDeployer>(
			s.getSonarState().getIpawsDeployerCache(), false);
	}
	
	public AlertManager(Session s, GeoLocManager lm) {
		super(s, lm, descriptor(s), 10);
		// TODO may change things about this
		
		// add the listener to the cache
		adcache = s.getSonarState().getIpawsDeployerCache();
		acache = s.getSonarState().getIpawsAlertCache();
//		cache.addProxyListener(listener);
//		Iterator<IpawsAlertDeployer> it = cache.iterator();
//		while (it.hasNext()) {
//			IpawsAlertDeployer ian = it.next();
//			System.out.println("Have alert: " + ian.getAlertId());
//		}
	}
	
	/** Create an alert tab */
	public AlertTab createTab() {
		AlertTab t = new AlertTab(session, this);
		Iterator<IpawsAlertDeployer> it = adcache.iterator();
		while (it.hasNext()) {
			System.out.println(it.next().getAlertId());
		}

		LinkedList<ProxyListener<IpawsAlertDeployer>> listeners =
				adcache.getProxyListeners();
		for (ProxyListener<IpawsAlertDeployer> l: listeners) {
			System.out.println(l.toString());
		}
		return t;
	}

	@Override
	protected AlertTheme createTheme() {
		return new AlertTheme(this);
	}
	
	@Override
	public boolean checkStyle(ItemStyle is, IpawsAlertDeployer proxy) {
		Integer t = checkAlertTimes(proxy);
		if (t == null)
			// problem with the dates
			return false;
		switch (is) {
		case ACTIVE:
			return t <= 0;
		case PAST:
			return t > 0;
		case ALL:
			return true;
		default:
			return false;
		}
	}
	
	/** Check when this alert will start relative to the current time. Returns
	 *  -1 if this alert has not yet started, 0 if the alert is currently
	 *  active, and 1 if the alert is in the past. If the time fields are not
	 *  filled, null is returned. 
	 */
	private Integer checkAlertTimes(IpawsAlertDeployer iad) {
		if (iad.getAlertStart() != null && iad.getAlertEnd() != null) {
			// check the time of the alert relative to now
			Date now = new Date();
			if (now.before(iad.getAlertStart()))
				return -1;
			else if (now.after(iad.getAlertStart())
					&& now.before(iad.getAlertEnd()))
				return 0;
			return 1;
		}
		// missing alert times - return null
		return null;
	}

	@Override
	protected GeoLoc getGeoLoc(IpawsAlertDeployer proxy) {
		// TODO need to figure out how to handle this, since IPAWS alerts are
		// areas and not points...
		return null;
	}
	
}