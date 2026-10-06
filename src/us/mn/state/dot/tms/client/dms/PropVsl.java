/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2026  SRF Consulting Group
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

import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Iterator;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import us.mn.state.dot.sonar.client.ProxyListener;
import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.DeviceAction;
import us.mn.state.dot.tms.DeviceActionHelper;
import us.mn.state.dot.tms.Hashtags;
import us.mn.state.dot.tms.MsgPattern;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.VslApprovalMode;
import us.mn.state.dot.tms.VslSign;
import us.mn.state.dot.tms.VslSignHelper;
import us.mn.state.dot.tms.VslState;
import us.mn.state.dot.tms.WeatherSensor;
import us.mn.state.dot.tms.WeatherSensorHelper;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyView;
import us.mn.state.dot.tms.client.proxy.ProxyWatcher;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.IPanel;
import static us.mn.state.dot.tms.client.widget.SwingRunner.runSwing;
import us.mn.state.dot.tms.utils.I18N;
import us.mn.state.dot.tms.utils.MultiAdapter;
import us.mn.state.dot.tms.utils.MultiString;

/**
 * PropVsl is a GUI panel for displaying and editing NDDOT variable speed
 * limit (VSL) settings on a DMS properties form.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class PropVsl extends IPanel {

	/** Hashtag of signs which display VSL speeds */
	static private final String VSL_TAG = "#VSL";

	/** Check if a MULTI string contains a [vsa] action tag */
	static private boolean hasVsaTag(String ms) {
		final boolean[] vsa = new boolean[1];
		new MultiString(ms).parse(new MultiAdapter() {
			@Override
			public void addSpeedAdvisory() {
				vsa[0] = true;
			}
		});
		return vsa[0];
	}

	/** Format a speed for a label */
	static private String formatSpeed(Integer s) {
		return (s != null) ? s + " mph" : "";
	}

	/** Get the highest speed allowed for a VSL sign */
	static private int getSpeedLimitMax() {
		return SystemAttrEnum.SPEED_LIMIT_MAX_MPH.getInt();
	}

	/** Action to create the VSL sign */
	private final IAction create_act = new IAction("dms.vsl.create") {
		protected void doActionPerformed(ActionEvent e) {
			cache.createObject(dms.getName());
		}
	};

	/** Action to delete the VSL sign */
	private final IAction delete_act = new IAction("dms.vsl.delete") {
		protected void doActionPerformed(ActionEvent e) {
			VslSign vs = watcher.getProxy();
			if (vs != null)
				vs.destroy();
		}
	};

	/** Hashtag text field */
	private final JTextField hashtag_txt = new JTextField(16);

	/** Minimum speed model */
	private final SpinnerNumberModel min_mdl =
		new SpinnerNumberModel(45, 1, 100, 5);

	/** Minimum speed spinner */
	private final JSpinner min_spn = new JSpinner(min_mdl);

	/** Maximum speed model */
	private final SpinnerNumberModel max_mdl =
		new SpinnerNumberModel(75, 1, 100, 5);

	/** Maximum speed spinner */
	private final JSpinner max_spn = new JSpinner(max_mdl);

	/** Flag to ignore spinner changes made by this panel */
	private boolean adjusting;

	/** Approval mode action */
	private final IAction approval_act = new IAction(
		"dms.vsl.approval.mode")
	{
		protected void doActionPerformed(ActionEvent e) {
			VslSign vs = watcher.getProxy();
			int m = approval_cbx.getSelectedIndex();
			if (vs != null && m >= 0)
				vs.setApprovalMode(m);
		}
		@Override
		protected void doUpdateSelected() {
			VslSign vs = watcher.getProxy();
			if (vs != null) {
				approval_cbx.setSelectedIndex(
					vs.getApprovalMode());
			}
		}
	};

	/** Approval mode combo box */
	private final JComboBox<VslApprovalMode> approval_cbx =
		new JComboBox<VslApprovalMode>(VslApprovalMode.values());

	/** Weather sensors label */
	private final JLabel ess_lbl = new JLabel();

	/** Recommended speed label */
	private final JLabel rec_lbl = new JLabel();

	/** Posted speed label */
	private final JLabel posted_lbl = new JLabel();

	/** Status label */
	private final JLabel status_lbl = new JLabel();

	/** User session */
	private final Session session;

	/** Cache of VSL signs */
	private final TypeCache<VslSign> cache;

	/** DMS to display */
	private final DMS dms;

	/** Proxy watcher */
	private final ProxyWatcher<VslSign> watcher;

	/** Proxy view */
	private final ProxyView<VslSign> view = new ProxyView<VslSign>() {
		public void enumerationComplete() { }
		public void update(VslSign vs, String a) {
			updateVsl(vs, a);
		}
		public void clear() {
			clearVsl();
		}
	};

	/** Listener to watch a VSL sign once it is created.  The cache also
	 * calls proxyAdded when a sign is enumerated again, so only a
	 * different sign replaces the watched proxy. */
	private final ProxyListener<VslSign> listener =
		new ProxyListener<VslSign>()
	{
		public void proxyAdded(final VslSign vs) {
			if (vs.getName().equals(dms.getName())) {
				runSwing(new Runnable() {
					public void run() {
						if (watcher.getProxy() != vs)
							watcher.setProxy(vs);
					}
				});
			}
		}
		public void enumerationComplete() { }
		public void proxyRemoved(VslSign vs) { }
		public void proxyChanged(VslSign vs, String a) { }
	};

	/** Create a new DMS properties VSL panel */
	public PropVsl(Session s, DMS sign) {
		session = s;
		dms = sign;
		cache = s.getSonarState().getVslSigns();
		watcher = new ProxyWatcher<VslSign>(cache, view, false);
	}

	/** Initialize the widgets on the form */
	@Override
	public void initialize() {
		super.initialize();
		approval_cbx.setAction(approval_act);
		add("dms.vsl.sign");
		add(new JButton(create_act));
		add(new JButton(delete_act), Stretch.LAST);
		add("dms.vsl.hashtag");
		add(hashtag_txt, Stretch.LAST);
		add("dms.vsl.min.speed");
		add(min_spn, Stretch.LAST);
		add("dms.vsl.max.speed");
		add(max_spn, Stretch.LAST);
		add("dms.vsl.approval.mode");
		add(approval_cbx, Stretch.LAST);
		add("dms.vsl.weather.sensors");
		add(ess_lbl, Stretch.LAST);
		add("dms.vsl.recommended");
		add(rec_lbl, Stretch.LAST);
		add("dms.vsl.posted");
		add(posted_lbl, Stretch.LAST);
		add(status_lbl, Stretch.LEFT);
		createJobs();
		clearVsl();
		watcher.initialize();
		watcher.setProxy(VslSignHelper.lookup(dms.getName()));
		cache.addProxyListener(listener);
	}

	/** Create the widget jobs */
	private void createJobs() {
		hashtag_txt.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updateHashtag();
			}
		});
		min_spn.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				VslSign vs = watcher.getProxy();
				int s = min_mdl.getNumber().intValue();
				if (vs != null && !adjusting &&
				    s != vs.getMinSpeed())
					updateMinSpeed(vs, s);
			}
		});
		max_spn.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				VslSign vs = watcher.getProxy();
				int s = max_mdl.getNumber().intValue();
				if (vs != null && !adjusting &&
				    s != vs.getMaxSpeed())
					updateMaxSpeed(vs, s);
			}
		});
	}

	/** Update the minimum speed of a VSL sign */
	private void updateMinSpeed(VslSign vs, int s) {
		vs.setMinSpeed(s);
		// The server refuses a minimum above the maximum (with a
		// message), so show the speeds which are still in effect
		if (s > vs.getMaxSpeed())
			showSpeedsLater(vs);
	}

	/** Update the maximum speed of a VSL sign */
	private void updateMaxSpeed(VslSign vs, int s) {
		vs.setMaxSpeed(s);
		// The server refuses a maximum below the minimum or above
		// the speed limit maximum (with a message), so show the speeds
		// which are still in effect
		if (s < vs.getMinSpeed() || s > getSpeedLimitMax())
			showSpeedsLater(vs);
	}

	/** Show the speeds of a VSL sign, once the spinner event is done */
	private void showSpeedsLater(final VslSign vs) {
		runSwing(new Runnable() {
			public void run() {
				setSpeeds(vs.getMinSpeed(), vs.getMaxSpeed());
			}
		});
	}

	/** Set the speed spinners, without sending them to the server */
	private void setSpeeds(int mn, int mx) {
		adjusting = true;
		min_mdl.setValue(mn);
		max_mdl.setValue(mx);
		adjusting = false;
	}

	/** Update the hashtag from the text field */
	private void updateHashtag() {
		VslSign vs = watcher.getProxy();
		if (vs != null) {
			String ht = hashtag_txt.getText().trim();
			vs.setHashtag((ht.length() > 0) ? ht : null);
			// The server refuses a bad hashtag, so show the
			// hashtag which is still in effect
			if (ht.length() > 0 && Hashtags.normalize(ht) == null)
				showHashtag(vs);
		}
	}

	/** Show the hashtag of a VSL sign */
	private void showHashtag(VslSign vs) {
		String ht = vs.getHashtag();
		hashtag_txt.setText((ht != null) ? ht : "");
	}

	/** Dispose of the panel */
	@Override
	public void dispose() {
		cache.removeProxyListener(listener);
		watcher.dispose();
		super.dispose();
	}

	/** Update the edit mode */
	public void updateEditMode() {
		VslSign vs = watcher.getProxy();
		create_act.setEnabled(vs == null && session.canWrite(
			VslSign.SONAR_TYPE, dms.getName()));
		delete_act.setEnabled(session.canWrite(vs));
		hashtag_txt.setEnabled(canWrite(vs, "hashtag"));
		min_spn.setEnabled(canWrite(vs, "minSpeed"));
		max_spn.setEnabled(canWrite(vs, "maxSpeed"));
		approval_act.setEnabled(canWrite(vs, "approvalMode"));
	}

	/** Check if the user can write a VSL sign attribute */
	private boolean canWrite(VslSign vs, String aname) {
		return (vs != null) && session.canWrite(vs, aname);
	}

	/** Update one attribute on the form tab */
	public void updateAttribute(String a) {
		if (a == null || a.equals("notes"))
			updateStatus();
	}

	/** Update one attribute of the VSL sign */
	private void updateVsl(VslSign vs, String a) {
		if (a == null)
			updateEditMode();
		if (a == null || a.equals("hashtag"))
			showHashtag(vs);
		if (a == null || a.equals("minSpeed") || a.equals("maxSpeed"))
			setSpeeds(vs.getMinSpeed(), vs.getMaxSpeed());
		if (a == null || a.equals("approvalMode"))
			approval_act.updateSelected();
		if (a == null || a.equals("recSpeed") ||
		    a.equals("recState"))
		{
			VslState st = VslState.fromOrdinal(vs.getRecState());
			Integer rs = vs.getRecSpeed();
			rec_lbl.setText((rs != null)
				? formatSpeed(rs) + " (" + st + ")"
				: "");
		}
		if (a == null || a.equals("postedSpeed"))
			posted_lbl.setText(formatSpeed(vs.getPostedSpeed()));
		updateStatus();
	}

	/** Clear the VSL sign widgets */
	private void clearVsl() {
		hashtag_txt.setText("");
		setSpeeds(SystemAttrEnum.SPEED_LIMIT_MIN_MPH.getInt(),
			getSpeedLimitMax());
		approval_cbx.setSelectedIndex(-1);
		rec_lbl.setText("");
		posted_lbl.setText("");
		updateEditMode();
		updateStatus();
	}

	/** Update the weather sensors and status labels */
	private void updateStatus() {
		VslSign vs = watcher.getProxy();
		String ht = (vs != null) ? vs.getHashtag() : null;
		String ess = (ht != null) ? findWeatherSensors(ht) : "";
		ess_lbl.setText(ess);
		status_lbl.setText(formatStatus(vs, ht, ess));
	}

	/** Get the names of weather sensors with a hashtag */
	private String findWeatherSensors(String ht) {
		StringBuilder sb = new StringBuilder();
		Iterator<WeatherSensor> it = WeatherSensorHelper.iterator();
		while (it.hasNext()) {
			WeatherSensor ws = it.next();
			if (new Hashtags(ws.getNotes()).contains(ht)) {
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(ws.getName());
			}
		}
		return sb.toString();
	}

	/** Format the status of the VSL sign as HTML */
	private String formatStatus(VslSign vs, String ht, String ess) {
		if (vs == null)
			return I18N.get("dms.vsl.none");
		StringBuilder sb = new StringBuilder("<html>");
		Hashtags tags = new Hashtags(dms.getNotes());
		if (!tags.contains(VSL_TAG))
			appendWarning(sb, I18N.get("dms.vsl.warn.no.vsl.tag"));
		if (ht == null)
			appendWarning(sb, I18N.get("dms.vsl.warn.no.hashtag"));
		else if (ess.isEmpty())
			appendWarning(sb, I18N.get("dms.vsl.warn.no.sensors"));
		String plan = findVsaPlan(tags);
		if (plan != null) {
			appendWarning(sb, I18N.get("dms.vsl.warn.vsa") + " " +
				plan);
		}
		return sb.toString();
	}

	/** Append a warning to an HTML status */
	private void appendWarning(StringBuilder sb, String w) {
		sb.append("<font color=red>");
		sb.append(w);
		sb.append("</font><br>");
	}

	/** Find an action plan which puts a [vsa] tag on the sign */
	private String findVsaPlan(Hashtags tags) {
		for (DeviceAction da: DeviceActionHelper.find(tags)) {
			MsgPattern pat = da.getMsgPattern();
			if (pat != null && hasVsaTag(pat.getMulti()))
				return da.getActionPlan().getName();
		}
		return null;
	}
}
