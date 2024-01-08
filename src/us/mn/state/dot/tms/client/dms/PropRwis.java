/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2023  SRF Consulting Group
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
package us.mn.state.dot.tms.client.dms;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;

import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.DMSHelper;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.GeoLocHelper;
import us.mn.state.dot.tms.ItemStyle;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.WeatherSensor;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.SonarState;
import us.mn.state.dot.tms.client.proxy.ProxyView;
import us.mn.state.dot.tms.client.proxy.ProxyWatcher;
import us.mn.state.dot.tms.client.widget.DialogHandler;
import us.mn.state.dot.tms.client.widget.IPanel;
import us.mn.state.dot.tms.units.Distance;
import us.mn.state.dot.tms.units.Distance.Units;
import us.mn.state.dot.tms.utils.I18N;

/**
 * PropRwis is a DMS properties panel for displaying
 * and editing DMS-specific RWIS configuration info
 * (mainly weather_sensor_override) on a DMS properties
 * form.
 *
 * @author John L. Stanley - SRF Consulting
 */
@SuppressWarnings("serial")
public class PropRwis extends IPanel implements ProxyView<GeoLoc> {

	/** HTML tags to control label-color mid-label */
	private final String HTML_START  = "<html>";
	private final String HTML_RED    = "<font color=red>";
	
	/** Blank label */
	private final JLabel blank_lbl = new JLabel(" ");
	
	/** RWIS enabled label */
	private final JLabel rwisSystem_lbl = new JLabel();

	/** RWIS hashtags label */
	private final JLabel hashtagsList_lbl = new JLabel();

	/** Closest WeatherSensor label */
	private final JLabel closest_lbl = new JLabel();
	
	/** Distance label */
	private final JLabel distance_lbl = new JLabel();

	/** WeatherSensorOverride text area */
	private final JTextArea weatherSensorOverride_txt = new JTextArea(3, 25);

	/** Weather Sensor status-summary */
	private final JLabel wsStatusCounts_lbl = new JLabel();

	/** Warning */
	private final JLabel warn_lbl = new JLabel(" ");

	/** User session */
	private final Session session;

	/** DMS to display */
	private final DMS dms;

	/** Proxy GeoLoc Watcher */
	private final ProxyWatcher<GeoLoc> geoLocWatcher;
	
	/** WeatherSensor status names */
	private String ACTIVE_STR;
	private String DISABLED_STR;
	private String FAILED_STR;

	private final Units units;

	private final int defaultDismissTimeout = ToolTipManager.sharedInstance().getDismissDelay();

	AbstractAction checkOverrides;
	
	/** Create a new DMS properties setup panel */
	public PropRwis(Session s, DMS sign) {
		dms = sign;
		session = s;
		SonarState state = session.getSonarState();
		TypeCache<GeoLoc> cache = state.getGeoLocs();
		geoLocWatcher = new ProxyWatcher<GeoLoc>(cache, this, false);
		boolean useSI = SystemAttrEnum.CLIENT_UNITS_SI.getBoolean();
		units = useSI ? Units.KILOMETERS : Units.MILES;
	}

	/** Validate the overrides list and update the sensor-status counts */
	private void updateOverrides() {
		String wso = weatherSensorOverride_txt.getText();
		ArrayList<WeatherSensor> wsList;
		try {
			wsList = DMSHelper.parseWeatherSensorList(wso);
		} catch (TMSException e2) {
			DialogHandler handler = new DialogHandler();
			handler.handle(e2);
			return;
		}
		if (wsList.isEmpty())
			wso = "";
		dms.setWeatherSensorOverride(wso);
		updateGui();
	}
	
	/** Initialize the widgets on the form */
	@Override
	public void initialize() {
		super.initialize();
		add("dms.rwis.system");
		add(rwisSystem_lbl, Stretch.LAST);

		add(new JLabel(" "), Stretch.CENTER);
		add("dms.rwis.hashtags");
		add(hashtagsList_lbl, Stretch.LAST);

		add(new JLabel(" "), Stretch.CENTER);
		add("dms.rwis.closest");
		add(closest_lbl, Stretch.LAST);
		add("dms.rwis.distance");
		add(distance_lbl, Stretch.LAST);

		add("dms.rwis.override");
		add(weatherSensorOverride_txt, Stretch.LAST);

		add("dms.rwis.ess.status");
		add(wsStatusCounts_lbl, Stretch.LAST);

		add(new JLabel(" "), Stretch.CENTER);
		add(blank_lbl, Stretch.CENTER);
		add(warn_lbl, Stretch.CENTER);
		
		ACTIVE_STR   = I18N.get("dms.rwis.ess.active");
		DISABLED_STR = I18N.get("dms.rwis.ess.disabled");
		FAILED_STR   = I18N.get("dms.rwis.ess.failed");

		// keep an eye on the sign's location
		geoLocWatcher.setProxy(dms.getGeoLoc());
		
		// Set action to invoke when mouse leaves the RWIS tab
		weatherSensorOverride_txt.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updateOverrides();
			}
		});
		
		// Set action to invoke when TAB key is pressed in sensor-overrides edit box
		checkOverrides = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				updateOverrides();
			}
		};
		weatherSensorOverride_txt.getInputMap().put(
				KeyStroke.getKeyStroke("TAB"), "checkOverrides");
		weatherSensorOverride_txt.getActionMap().put(
				"checkOverrides", checkOverrides);
		weatherSensorOverride_txt.setToolTipText(
				I18N.get("dms.rwis.override.tip"));

		// Set tooltip timeout to one minute for sensor-status tooltip
		wsStatusCounts_lbl.addMouseListener(new MouseAdapter() {
			  public void mouseEntered(MouseEvent me) {
			    ToolTipManager.sharedInstance().setDismissDelay(60000);
			  }
			  public void mouseExited(MouseEvent me) {
			    ToolTipManager.sharedInstance().setDismissDelay(defaultDismissTimeout);
			  }
		});
	}

	/** Set label count */
	private void setLabelValue(JLabel lbl, Object value) {
		if (value == null)
			value = "<none>";
		else if ((value instanceof Integer) && ((Integer)value != 0))
			; // OK, use value as-is
		else if ((value instanceof String) && (((String)value).length() > 0))
			; // OK, use value as-is
		else
			value = "<none>";
		lbl.setText(String.valueOf(value));
	}

	/** Update all text and labels */
	public void updateGui() {
		boolean bRwisEnabled;
		bRwisEnabled = (SystemAttrEnum.RWIS_CYCLE_SEC.getInt() > 0); 
		rwisSystem_lbl.setText(bRwisEnabled ? "ENABLED" : "disabled");

		ArrayList<String> hashtagList = getRwisHashtags();
		String extra = getHashtagsString();
		setLabelValue(hashtagsList_lbl, extra);
		boolean bNoOverrides = dms.getWeatherSensorOverride().isBlank();

		ArrayList<WeatherSensor> sensorList;
		sensorList = DMSHelper.getAssociatedWeatherSensors(dms);
		boolean bNoSensors = sensorList.isEmpty();
		int nActiveEss   = 0;
		int nDisabledEss = 0;
		int nFailedEss   = 0;
		StringBuilder sbCounts = new StringBuilder();
		if (bNoSensors) {
			sbCounts.append("<none>");
			wsStatusCounts_lbl.setToolTipText("");
		}
		else {
			// Count active, disabled, and failed sensors.
			// Also build tooltip string.
			// (A sensor can be both active and failed, so
			//  the order of the style-bit tests matters...)
			StringBuilder sbTooltip = new StringBuilder();
			sbTooltip.append(HTML_START+"<tt><u> Distance </u>  <u>   Weather Sensor   </u>  <u> Status </u>");
			String str;
			for (WeatherSensor ws : sensorList) {
				long bits = ws.getStyles(); 
				if (ItemStyle.FAILED.checkBit(bits)) {
					++nFailedEss;
					str = FAILED_STR;
				}
				else if (ItemStyle.ACTIVE.checkBit(bits)) {
					++nActiveEss;
					str = ACTIVE_STR;
				}
				else {
					++nDisabledEss;
					str = DISABLED_STR;
				}
				GeoLoc g1 = ws.getGeoLoc();
				GeoLoc g2 = dms.getGeoLoc();
				Distance d = GeoLocHelper.distanceTo(g1, g2);
				d = d.convert(units);
				str = String.format("<br>%10s  %-20s  %-8s", d.toString(), ws.getName(), str);
				sbTooltip.append(str);
			}
			str = sbTooltip.toString().replace(" ", "&nbsp;");
			wsStatusCounts_lbl.setToolTipText(str);
			sbCounts.append(" ");
			sbCounts.append(nActiveEss);
			sbCounts.append(" ");
			sbCounts.append(ACTIVE_STR);
			sbCounts.append(", ");
			sbCounts.append(nDisabledEss);
			sbCounts.append(" ");
			sbCounts.append(DISABLED_STR);
			sbCounts.append(", ");
			sbCounts.append(nFailedEss);
			sbCounts.append(" ");
			sbCounts.append(FAILED_STR);
		}
		wsStatusCounts_lbl.setText(sbCounts.toString());
		
		boolean bHashtags  = !hashtagList.isEmpty(); // found hashtags?
		Object o[] = DMSHelper.findClosestWeatherSensor(dms, true);
		WeatherSensor ws = (WeatherSensor)o[0];
		Integer       d  = (Integer)      o[1];
		if (ws != null) {
			Distance dist = Distance.create(d, Units.METERS);
			closest_lbl.setText(ws.getName());
			String dStr = HTML_START + dist.convert(units).toString() + "  ";
			if (d > SystemAttrEnum.RWIS_AUTO_MAX_M.getInt()) {
				if (bNoOverrides ^ !bHashtags)
					dStr += HTML_RED;
				dStr += "<small>&lt;Out of Range&gt;";
			}
			//I18N.get("dms.rwis.out.of.range");
			distance_lbl.setText(dStr);
		}
		else {
			closest_lbl.setText(I18N.get("dms.rwis.none.defined"));
			distance_lbl.setText("");
		}

		boolean bActiveEss = (nActiveEss != 0);        // active sensors available? (distance OR override)
		hashtagsList_lbl.setForeground(Color.black);
		warn_lbl.setForeground(Color.black);
		String status = " ";
		if (bHashtags) {
			if (bActiveEss) {
				// found hashtags and sensors
				if (bRwisEnabled)
					status = I18N.get("dms.rwis.status.enabled");
				else
					status = I18N.get("dms.rwis.status.ready");
			}
			else {
				// found hashtags but no active sensors
				if (bNoSensors)
					status = I18N.get("dms.rwis.status.no.sensors");
				else
					status = I18N.get("dms.rwis.status.no.active.sensors");
			}
		}
		else {
			if (bActiveEss) {
				// found sensors, but no hashtags
				hashtagsList_lbl.setForeground(Color.red);
				status = I18N.get("dms.rwis.status.no.hashtags");
			}
			else {
				// didn't find hashtags or sensors
				status = I18N.get("dms.rwis.status.disabled");
			}
		}
		warn_lbl.setText(status);
		repaint();
	}

	/** Update the edit mode */
	public void updateEditMode() {
		weatherSensorOverride_txt.setEnabled(canWrite("weatherSensorOverride"));
	}

	/** Update one attribute on the form tab */
	public void updateAttribute(String a) {
		if ((null != a) && a.equals("style")) {
			updateGui();
		}
		else if (null == a || a.equals("weatherSensorOverride")) {
			String eo = dms.getWeatherSensorOverride();
			if (eo == null)
				eo = "";
			weatherSensorOverride_txt.setText(eo);
			updateGui();
		}
	}

	/** Check if the user can write an attribute */
	private boolean canWrite(String aname) {
		Session session = Session.getCurrent();
		if (session == null)
			return false;
		return session.canWrite(dms, aname);
	}

	/** Called when all proxies have been enumerated (from ProxyView). */
	@Override
	public void enumerationComplete() { }

	/** Update one attribute (from ProxyView). */
	@Override
	public void update(GeoLoc l, String a) {
		if (a == null
		 || a.equals("lat")
		 || a.equals("lon"))
			updateGui();
	}

	/** Clear all attributes (from ProxyView). */
	@Override
	public void clear() {}

	/** Get array of RWIS hashtag strings 
	 * (without leading '#').
	 *  Returns empty array if no RWIS hashtags
	 *  are associated with the sign. */
	public ArrayList<String> getRwisHashtags() {
		ArrayList<String> tagList = new ArrayList<String>();
		if (dms != null)
			for (String tag: dms.getHashtags())
				if (tag.startsWith("#RWIS"))
					tagList.add(tag.substring(1));
		return tagList;
	}

	/** Get string with list of RWIS hashtags
	 * (with leading '#' prefixes).
	 *  Returns null if no RWIS hashtags
	 *  are associated with the sign. */
	public String getHashtagsString() {
		StringBuilder sb = new StringBuilder();
		if (dms != null)
			for (String tag: dms.getHashtags())
				if (tag.startsWith("#RWIS")) {
					if (sb.length() > 0)
						sb.append(", ");
					sb.append(tag.toString());
				}
		return (sb.length() == 0)
				? null
				: sb.toString();
	}
}
