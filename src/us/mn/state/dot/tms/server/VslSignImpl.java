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
package us.mn.state.dot.tms.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import us.mn.state.dot.sched.DebugLog;
import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.ChangeVetoException;
import us.mn.state.dot.tms.DMSHelper;
import us.mn.state.dot.tms.DmsLock;
import us.mn.state.dot.tms.EventType;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.GeoLocHelper;
import us.mn.state.dot.tms.Hashtags;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.VslApprovalMode;
import us.mn.state.dot.tms.VslSign;
import us.mn.state.dot.tms.VslSignHelper;
import us.mn.state.dot.tms.VslState;
import us.mn.state.dot.tms.server.event.VslEvent;
import us.mn.state.dot.tms.utils.MultiString;

/**
 * NDDOT variable speed limit (VSL) sign.  Operators configure the hashtag,
 * speed range and approval mode; the server writes the recommendation and
 * posted speed.  Nothing is loaded unless VSL is enabled when the server
 * starts.
 *
 * @author Coby Christiansen - SRF Consulting
 * @author John L. Stanley - SRF Consulting
 */
public class VslSignImpl extends BaseObjectImpl implements VslSign {

	/** VSL debug log */
	static private final DebugLog VSL_LOG = new DebugLog("vsl");

	/** Load all the VSL signs */
	static protected void loadAll() throws TMSException {
		if (!VslSignHelper.isEnabled())
			return;
		store.query("SELECT name, hashtag, min_speed, max_speed, " +
			"approval_mode, rec_speed, rec_state, rec_time, " +
			"posted_speed, posted_time, quiet_expires FROM iris." +
			SONAR_TYPE + ";", new ResultFactory()
		{
			public void create(ResultSet row) throws Exception {
				VslSignImpl vs = new VslSignImpl(row);
				namespace.addObject(vs);
				vs.setSignVsl(true);
			}
		});
	}

	/** Get a mapping of the columns */
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("hashtag", hashtag);
		map.put("min_speed", min_speed);
		map.put("max_speed", max_speed);
		map.put("approval_mode", approval_mode);
		map.put("rec_speed", rec_speed);
		map.put("rec_state", rec_state);
		map.put("rec_time", asTimestamp(rec_time));
		map.put("posted_speed", posted_speed);
		map.put("posted_time", asTimestamp(posted_time));
		map.put("quiet_expires", asTimestamp(quiet_expires));
		return map;
	}

	/** Create a new VSL sign */
	public VslSignImpl(String n) {
		super(n);
		min_speed = SystemAttrEnum.SPEED_LIMIT_MIN_MPH.getInt();
		max_speed = SystemAttrEnum.SPEED_LIMIT_MAX_MPH.getInt();
		approval_mode = VslApprovalMode.MANUAL.ordinal();
		rec_state = VslState.NONE.ordinal();
	}

	/** Create a VSL sign */
	private VslSignImpl(ResultSet row) throws SQLException {
		this(row.getString(1),             // name
		     row.getString(2),             // hashtag
		     row.getInt(3),                // min_speed
		     row.getInt(4),                // max_speed
		     row.getInt(5),                // approval_mode
		     (Integer) row.getObject(6),   // rec_speed
		     row.getInt(7),                // rec_state
		     row.getTimestamp(8),          // rec_time
		     (Integer) row.getObject(9),   // posted_speed
		     row.getTimestamp(10),         // posted_time
		     row.getTimestamp(11)          // quiet_expires
		);
	}

	/** Create a VSL sign */
	private VslSignImpl(String n, String ht, int mn, int mx, int am,
		Integer rs, int st, Date rt, Integer ps, Date pt, Date qe)
	{
		super(n);
		hashtag = ht;
		min_speed = mn;
		max_speed = mx;
		approval_mode = am;
		rec_speed = rs;
		rec_state = st;
		rec_time = stampMillis(rt);
		posted_speed = ps;
		posted_time = stampMillis(pt);
		quiet_expires = stampMillis(qe);
	}

	/** Store a new VSL sign */
	@Override
	public void doStore() throws TMSException {
		if (!VslSignHelper.isEnabled())
			throw new ChangeVetoException("VSL is not enabled");
		if (lookupDMS(name) == null)
			throw new ChangeVetoException("No DMS named " + name);
		super.doStore();
		setSignVsl(true);
	}

	/** Destroy a VSL sign */
	@Override
	public void doDestroy() throws TMSException {
		super.doDestroy();
		setSignVsl(false);
	}

	/** Mark the sign as a VSL sign (or not), and update its styles */
	private void setSignVsl(boolean v) {
		DMSImpl dms = lookupDMS(name);
		if (dms != null) {
			dms.setVslSign(v);
			dms.updateStyles();
		}
	}

	/** Get notes (including hashtags).  These are the sign's notes, so
	 * hashtag permissions on the sign also apply to its VSL settings. */
	@Override
	public String getNotes() {
		DMSImpl dms = lookupDMS(name);
		return (dms != null) ? dms.getNotes() : null;
	}

	/** Hashtag of the sign's weather sensors and detectors */
	private String hashtag;

	/** Set the hashtag of the sign's weather sensors and detectors */
	@Override
	public void setHashtag(String ht) {
		hashtag = ht;
	}

	/** Set the hashtag of the sign's weather sensors and detectors */
	public void doSetHashtag(String ht) throws TMSException {
		String t = Hashtags.normalize(ht);
		if (!objectEquals(t, ht))
			throw new ChangeVetoException("Bad hashtag");
		if (!objectEquals(ht, hashtag)) {
			store.update(this, "hashtag", ht);
			setHashtag(ht);
		}
	}

	/** Get the hashtag of the sign's weather sensors and detectors */
	@Override
	public String getHashtag() {
		return hashtag;
	}

	/** Minimum speed (mph) */
	private int min_speed;

	/** Set the minimum speed (mph) */
	@Override
	public void setMinSpeed(int s) {
		min_speed = s;
	}

	/** Set the minimum speed (mph) */
	public void doSetMinSpeed(int s) throws TMSException {
		if (s == min_speed)
			return;
		if (s < 1) {
			throw new ChangeVetoException("Minimum speed (" + s +
				") must be positive");
		} else if (s > max_speed) {
			throw new ChangeVetoException("Minimum speed (" + s +
				") is greater than maximum");
		}
		store.update(this, "min_speed", s);
		setMinSpeed(s);
	}

	/** Get the minimum speed (mph) */
	@Override
	public int getMinSpeed() {
		return min_speed;
	}

	/** Maximum speed (mph) */
	private int max_speed;

	/** Set the maximum speed (mph) */
	@Override
	public void setMaxSpeed(int s) {
		max_speed = s;
	}

	/** Set the maximum speed (mph) */
	public void doSetMaxSpeed(int s) throws TMSException {
		if (s == max_speed)
			return;
		if (s < min_speed) {
			throw new ChangeVetoException("Maximum speed (" + s +
				") is less than minimum");
		} else if (s > SystemAttrEnum.SPEED_LIMIT_MAX_MPH.getInt()) {
			throw new ChangeVetoException("Maximum speed (" + s +
				") is greater than speed limit maximum");
		}
		store.update(this, "max_speed", s);
		setMaxSpeed(s);
	}

	/** Get the maximum speed (mph) */
	@Override
	public int getMaxSpeed() {
		return max_speed;
	}

	/** Approval mode (ordinal of VslApprovalMode) */
	private int approval_mode;

	/** Set the approval mode (ordinal of VslApprovalMode) */
	@Override
	public void setApprovalMode(int m) {
		approval_mode = m;
	}

	/** Set the approval mode (ordinal of VslApprovalMode) */
	public void doSetApprovalMode(int m) throws TMSException {
		int am = VslApprovalMode.fromOrdinal(m).ordinal();
		if (am == approval_mode)
			return;
		store.update(this, "approval_mode", am);
		setApprovalMode(am);
	}

	/** Get the approval mode (ordinal of VslApprovalMode) */
	@Override
	public int getApprovalMode() {
		return approval_mode;
	}

	/** Log a VSL message */
	private void logMsg(String msg) {
		VSL_LOG.log(getName() + ": " + msg);
	}

	/** Flag set after the first calculation */
	private transient boolean calculated;

	/** Calculate the recommended speed, then apply approval rules */
	public synchronized void calculate() {
		DMSImpl dms = lookupDMS(name);
		VslCalculator calc = new VslCalculator(hashtag, min_speed,
			max_speed);
		Integer s = calc.calculate();
		if (VSL_LOG.isOpen()) {
			for (String d: calc.detail)
				logMsg(d);
		}
		setAvailableNotify(calc.fault);
		if (checkOverride(dms)) {
			// Calculate, but don't approve (VSL-VOV-03)
			logMsg("overridden");
			setRecommendationNotify(s, VslState.NONE);
		} else if (s == null) {
			// Hold the posted speed (VSL-FLT-02)
			setRecommendationNotify(null, VslState.NONE);
		} else
			approve(s, calc);
		checkComm(dms);
		calculated = true;
	}

	/** Operator message on the sign, or null if none */
	private transient String override;

	/** User of the operator message on the sign */
	private transient String override_user;

	/** Check for an operator message on the sign (VSL-VOV-01).  A new or
	 * cleared message is notified (VSL-NTF-04, VSL-NTF-05), except on
	 * the first calculation after the server starts.
	 * @return true if the sign is overridden. */
	private boolean checkOverride(DMSImpl dms) {
		DmsLock lk = new DmsLock((dms != null) ? dms.getLock() : null);
		String ms = lk.optMulti();
		if (ms != null && new MultiString(ms).isBlank())
			ms = null;
		String user = (ms != null) ? lk.getUser() : null;
		if (calculated && !objectEquals(ms, override)) {
			if (ms != null)
				sendNotice(overrideNotice(ms, user));
			else
				sendNotice(overrideClearedNotice());
		}
		override = ms;
		override_user = user;
		return ms != null;
	}

	/** Create an override notification (VSL-NTF-04) */
	private VslNotice overrideNotice(String ms, String user) {
		String t = new MultiString(ms).asText();
		return createNotice(VslNotice.Type.OVERRIDE, user + " " + t)
			.add("Operator", user)
			.add("Override speed", t.matches("\\d+")
				? t + " mph"
				: t);
	}

	/** Create an override cleared notification (VSL-NTF-05).
	 *
	 * TODO: the operator who blanked the sign is not known here, so the
	 *       operator who set the override is given.  Naming the operator
	 *       who blanked it would need a hook in DMSImpl.doSetLock. */
	private VslNotice overrideClearedNotice() {
		return createNotice(VslNotice.Type.OVERRIDE_CLEARED, "")
			.add("Override by", override_user)
			.add("Posted speed", VslNotice.formatSpeed(
				posted_speed))
			.add("Status", "calculated speeds resume");
	}

	/** Apply the approval rules to a recommended speed */
	private void approve(int s, VslCalculator calc) {
		long now = TimeSteward.currentTimeMillis();
		VslApprovalMode mode = VslApprovalMode.fromOrdinal(
			approval_mode);
		if (isPending() && mode == VslApprovalMode.SEMI_AUTOMATED &&
		    VslApproval.isReviewTimedOut(rec_time, now))
		{
			logMsg("review timeout");
			postSpeed(rec_speed, null);
			return;
		}
		VslApproval ap = VslApproval.evaluate(s, posted_speed,
			posted_time, quiet_expires, mode, now);
		logMsg(ap.toString());
		if (ap.urgent)
			setQuietExpiresNotify(null);
		switch (ap.action) {
		case POST:
			postSpeed(ap.speed, null);
			break;
		case REQUEST:
			requestApproval(ap.speed, calc);
			break;
		default:
			setRecommendationNotify(ap.speed, ap.state);
			break;
		}
	}

	/** Check if a recommendation is waiting for an operator */
	private boolean isPending() {
		return rec_speed != null &&
		       VslState.fromOrdinal(rec_state) == VslState.PENDING;
	}

	/** Request operator approval of a speed (VSL-APV-01) */
	private void requestApproval(int s, VslCalculator calc) {
		// A request for the same speed keeps its review time
		if (isPending() && rec_speed == s)
			return;
		setRecommendationNotify(s, VslState.PENDING);
		logEvent(new VslEvent(EventType.VSL_PENDING, name, s, null));
		sendNotice(pendingNotice(s, calc));
	}

	/** Create an approval request notification (VSL-NTF-03) */
	private VslNotice pendingNotice(int s, VslCalculator calc) {
		VslNotice n = createNotice(VslNotice.Type.PENDING,
			s + " " + posted_speed)
			.add("Posted speed", VslNotice.formatSpeed(
				posted_speed))
			.add("Recommended speed", VslNotice.formatSpeed(s))
			.addReading(calc.sensor, calc.reading);
		if (VslApprovalMode.fromOrdinal(approval_mode) ==
		    VslApprovalMode.SEMI_AUTOMATED && rec_time != null)
		{
			n.add("Deploys automatically at", VslNotice.formatTime(
				VslApproval.reviewExpires(rec_time)));
		}
		return n;
	}

	/** Set an operator decision (handled by doSetDecision) */
	@Override
	public void setDecision(int st) {
		// Handled by doSetDecision
	}

	/** Set an operator decision on the pending recommendation.  An
	 * approval posts the speed at once (VSL-APV-04); a rejection keeps
	 * the posted speed and starts a quiet period (VSL-APV-05).  Both
	 * are logged with the operator's name (VSL-APV-10).
	 * @param st Ordinal of VslState.APPROVED or VslState.REJECTED. */
	public synchronized void doSetDecision(int st) throws TMSException {
		if (!isPending())
			throw new ChangeVetoException("Nothing pending");
		String user = getProcUser();
		int s = rec_speed;
		switch (VslState.fromOrdinal(st)) {
		case APPROVED:
			logEvent(new VslEvent(EventType.VSL_APPROVED, name, s,
				user));
			postSpeed(s, user);
			break;
		case REJECTED:
			logEvent(new VslEvent(EventType.VSL_REJECTED, name, s,
				user));
			setQuietExpiresNotify(VslApproval.quietExpires(
				TimeSteward.currentTimeMillis()));
			setRecommendationNotify(s, VslState.REJECTED);
			break;
		default:
			throw new ChangeVetoException("Invalid decision");
		}
	}

	/** Post a speed to the sign.
	 * @param s Speed (mph).
	 * @param user User who approved it, or null if automatic. */
	private void postSpeed(int s, String user) {
		Integer prev = posted_speed;
		Long t = TimeSteward.currentTimeMillis();
		try {
			store.update(this, "posted_speed", s);
			store.update(this, "posted_time", asTimestamp(t));
		}
		catch (TMSException e) {
			logMsg("posted_speed: " + e.getMessage());
		}
		posted_speed = s;
		posted_time = t;
		notifyAttribute("postedSpeed");
		notifyAttribute("postedTime");
		setRecommendationNotify(s, VslState.NONE);
		logEvent(new VslEvent(EventType.VSL_POSTED, name, s, user));
		sendNotice(createNotice(VslNotice.Type.POSTED, prev + " " + s)
			.add("Previous speed", VslNotice.formatSpeed(prev))
			.add("Posted speed", VslNotice.formatSpeed(s))
			.add("Approval", (user != null)
				? "approved by " + user
				: "automatic"));
	}

	/** Flag set when a weather sensor can be used (null until the first
	 * calculation) */
	private transient Boolean available;

	/** Reason no weather sensor can be used, or null */
	private transient String sensor_fault;

	/** Set flag when a weather sensor can be used.  A change is logged
	 * as an event (VSL-FLT-03, VSL-FLT-04) and notified (VSL-NTF-07).
	 * @param fault Reason no speed was calculated, or null. */
	private void setAvailableNotify(String fault) {
		boolean a = (fault == null);
		if (!objectEquals(a, available)) {
			if (!a) {
				logEvent(new VslEvent(EventType.VSL_UNAVAILABLE,
					name, null, null));
				sendNotice(faultNotice(SENSOR_ERROR, fault,
					posted_speed, getTargetSpeed()));
			} else if (available != null) {
				logEvent(new VslEvent(EventType.VSL_AVAILABLE,
					name, null, null));
				sendNotice(resolvedNotice(SENSOR_ERROR,
					sensor_fault));
			}
			available = a;
			sensor_fault = fault;
			notifyAttribute("available");
		}
	}

	/** Get the speed being deployed when a fault happens: a recommended
	 * speed different from the posted speed, or null.
	 *
	 * TODO: confirm with NDDOT what "the speed the system was trying to
	 *       deploy" means for a sensor fault (VSL-NTF-07). */
	private Integer getTargetSpeed() {
		return (rec_speed != null && !rec_speed.equals(posted_speed))
		      ? rec_speed
		      : null;
	}

	/** Label of sensor faults */
	static private final String SENSOR_ERROR = "Sensor error";

	/** Label of communication faults */
	static private final String COMM_ERROR = "Communication error";

	/** Communication problem with the sign, or null */
	private transient String comm_fault;

	/** Posted speed when the last communication fault was notified */
	private transient Integer comm_fault_speed;

	/** Posted speed while the sign was last communicating */
	private transient Integer deployed_speed;

	/** Check communication with the sign (VSL-NTF-07).  A new posted
	 * speed during a fault is notified again, since it can't be
	 * deployed. */
	private void checkComm(DMSImpl dms) {
		String f = getCommFault(dms);
		if (f != null) {
			if (!f.equals(comm_fault) ||
			    !objectEquals(posted_speed, comm_fault_speed))
			{
				sendNotice(faultNotice(COMM_ERROR, f,
					deployed_speed, posted_speed));
				comm_fault_speed = posted_speed;
			}
		} else {
			if (comm_fault != null) {
				sendNotice(resolvedNotice(COMM_ERROR,
					comm_fault));
			}
			deployed_speed = posted_speed;
		}
		comm_fault = f;
	}

	/** Get a communication problem with a sign, or null.
	 *
	 * TODO: confirm with NDDOT whether an inactive sign (such as one with
	 *       its comm link disabled) should be a fault. */
	static private String getCommFault(DMSImpl dms) {
		if (dms == null)
			return "no sign";
		else if (!DMSHelper.isActive(dms))
			return "sign not active";
		else if (DMSHelper.isOffline(dms))
			return "sign offline";
		else
			return null;
	}

	/** Create a fault notification (VSL-NTF-07).
	 * @param label Fault label.
	 * @param fault Fault description.
	 * @param last Last deployed speed, or null if unknown.
	 * @param target Speed being deployed, or null if none. */
	private VslNotice faultNotice(String label, String fault,
		Integer last, Integer target)
	{
		return createNotice(VslNotice.Type.FAULT, label + " " + target)
			.add(label, fault)
			.add("Last deployed speed", (last != null)
				? VslNotice.formatSpeed(last)
				: "unknown")
			.add("Speed being deployed", VslNotice.formatSpeed(
				target));
	}

	/** Create a fault resolved notification (VSL-NTF-07) */
	private VslNotice resolvedNotice(String label, String fault) {
		return createNotice(VslNotice.Type.FAULT_CLEARED, label)
			.add(label + " resolved", fault)
			.add("Posted speed", VslNotice.formatSpeed(
				posted_speed));
	}

	/** Create a notification for the sign (VSL-NTF-02) */
	private VslNotice createNotice(VslNotice.Type t, String k) {
		DMSImpl dms = lookupDMS(name);
		GeoLoc gl = (dms != null) ? dms.getGeoLoc() : null;
		String loc = (gl != null) ? GeoLocHelper.getLocation(gl) : "";
		return new VslNotice(t, TimeSteward.currentTimeMillis(), name,
			loc, k);
	}

	/** Times of recent notifications, by condition */
	private transient final HashMap<String, Long> notified =
		new HashMap<String, Long>();

	/** Send a notification to the sign's feeds, unless it repeats a
	 * recent condition (VSL-NTF-08) */
	private void sendNotice(VslNotice n) {
		long repeat_ms = SystemAttrEnum.NDDOT_VSL_NOTIFY_REPEAT
			.getInt() * 60L * 1000L;
		Iterator<Long> it = notified.values().iterator();
		while (it.hasNext()) {
			if (it.next() + repeat_ms <= n.time)
				it.remove();
		}
		if (n.isRepeat(notified.get(n.key), repeat_ms)) {
			logMsg("repeat notice: " + n.getTitle());
			return;
		}
		notified.put(n.key, n.time);
		ArrayList<String> feeds = VslFeedImpl.addNotice(lookupDMS(name),
			n);
		logMsg("notice: " + n + " feeds: " + feeds);
	}

	/** Get flag set when a weather sensor can be used, or null before
	 * the first calculation */
	@Override
	public Boolean getAvailable() {
		return available;
	}

	/** Recommended speed (mph) */
	private Integer rec_speed;

	/** Set the recommendation.  The recommendation time changes along
	 * with the speed or state, so a new request gets a new review time.
	 * @param s Recommended speed (mph), or null for none.
	 * @param st Recommendation state. */
	private void setRecommendationNotify(Integer s, VslState st) {
		int ord = st.ordinal();
		if (!objectEquals(s, rec_speed) || ord != rec_state) {
			Long t = TimeSteward.currentTimeMillis();
			try {
				store.update(this, "rec_speed", s);
				store.update(this, "rec_state", ord);
				store.update(this, "rec_time", asTimestamp(t));
			}
			catch (TMSException e) {
				logMsg("recommendation: " + e.getMessage());
			}
			rec_speed = s;
			rec_state = ord;
			rec_time = t;
			notifyAttribute("recSpeed");
			notifyAttribute("recState");
			notifyAttribute("recTime");
		}
	}

	/** Get the recommended speed (mph), or null if none */
	@Override
	public Integer getRecSpeed() {
		return rec_speed;
	}

	/** Recommendation state (ordinal of VslState) */
	private int rec_state;

	/** Get the recommendation state (ordinal of VslState) */
	@Override
	public int getRecState() {
		return rec_state;
	}

	/** Recommendation time (milliseconds since epoch) */
	private Long rec_time;

	/** Get the recommendation time, or null if none */
	@Override
	public Long getRecTime() {
		return rec_time;
	}

	/** Posted speed (mph) */
	private Integer posted_speed;

	/** Get the posted speed (mph), or null if none */
	@Override
	public Integer getPostedSpeed() {
		return posted_speed;
	}

	/** Posted time (milliseconds since epoch) */
	private Long posted_time;

	/** Get the posted time, or null if none */
	@Override
	public Long getPostedTime() {
		return posted_time;
	}

	/** Quiet period expiration time (milliseconds since epoch) */
	private Long quiet_expires;

	/** Set the quiet period expiration time, or null for none */
	private void setQuietExpiresNotify(Long t) {
		if (!objectEquals(t, quiet_expires)) {
			try {
				store.update(this, "quiet_expires",
					asTimestamp(t));
			}
			catch (TMSException e) {
				logMsg("quiet_expires: " + e.getMessage());
			}
			quiet_expires = t;
			notifyAttribute("quietExpires");
		}
	}

	/** Get the quiet period expiration time, or null if none */
	@Override
	public Long getQuietExpires() {
		return quiet_expires;
	}
}
