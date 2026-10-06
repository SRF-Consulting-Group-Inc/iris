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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import us.mn.state.dot.tms.ChangeVetoException;
import us.mn.state.dot.tms.Hashtags;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.VslApprovalMode;
import us.mn.state.dot.tms.VslSign;
import us.mn.state.dot.tms.VslSignHelper;
import us.mn.state.dot.tms.VslState;

/**
 * NDDOT variable speed limit (VSL) sign.  Operators configure the hashtag,
 * speed range and approval mode; the server writes the recommendation and
 * posted speed.  Nothing is loaded unless VSL is enabled when the server
 * starts.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslSignImpl extends BaseObjectImpl implements VslSign {

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
				namespace.addObject(new VslSignImpl(row));
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

	/** Recommended speed (mph) */
	private Integer rec_speed;

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

	/** Get the quiet period expiration time, or null if none */
	@Override
	public Long getQuietExpires() {
		return quiet_expires;
	}
}
