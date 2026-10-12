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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import us.mn.state.dot.tms.ChangeVetoException;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.Hashtags;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.VslFeed;
import us.mn.state.dot.tms.VslFeedHelper;
import us.mn.state.dot.tms.VslSignHelper;

/**
 * An RSS feed of NDDOT VSL notifications (VSL-NTF-01).  Nothing is loaded
 * unless VSL is enabled when the server starts.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslFeedImpl extends BaseObjectImpl implements VslFeed {

	/** Maximum number of notifications kept by a feed.
	 *
	 * TODO: confirm how many items GovDelivery needs in a feed. */
	static private final int MAX_NOTICES = 20;

	/** Add a notification to the feeds covering a sign.
	 * @param dms Sign of the notification.
	 * @param n Notification to add.
	 * @return Names of the feeds which want the notification. */
	static public ArrayList<String> addNotice(DMS dms, VslNotice n) {
		ArrayList<String> names = new ArrayList<String>();
		if (dms == null)
			return names;
		for (VslFeed f: VslFeedHelper.find(dms)) {
			if (f instanceof VslFeedImpl &&
			    ((VslFeedImpl) f).add(n))
				names.add(f.getName());
		}
		return names;
	}

	/** Load all the VSL feeds */
	static protected void loadAll() throws TMSException {
		if (!VslSignHelper.isEnabled())
			return;
		store.query("SELECT name, title, hashtag, notify_pending, " +
			"notify_override, notify_posted, notify_fault " +
			"FROM iris." + SONAR_TYPE + ";", new ResultFactory()
		{
			public void create(ResultSet row) throws Exception {
				namespace.addObject(new VslFeedImpl(row));
			}
		});
	}

	/** Get a mapping of the columns */
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("title", title);
		map.put("hashtag", hashtag);
		map.put("notify_pending", notify_pending);
		map.put("notify_override", notify_override);
		map.put("notify_posted", notify_posted);
		map.put("notify_fault", notify_fault);
		return map;
	}

	/** Create a new VSL feed.  It notifies of everything until it is
	 * configured otherwise. */
	public VslFeedImpl(String n) {
		super(n);
		title = n;
		notify_pending = true;
		notify_override = true;
		notify_posted = true;
		notify_fault = true;
	}

	/** Create a VSL feed */
	private VslFeedImpl(ResultSet row) throws SQLException {
		this(row.getString(1),   // name
		     row.getString(2),   // title
		     row.getString(3),   // hashtag
		     row.getBoolean(4),  // notify_pending
		     row.getBoolean(5),  // notify_override
		     row.getBoolean(6),  // notify_posted
		     row.getBoolean(7)   // notify_fault
		);
	}

	/** Create a VSL feed */
	private VslFeedImpl(String n, String t, String ht, boolean np,
		boolean no, boolean ns, boolean nf)
	{
		super(n);
		title = t;
		hashtag = ht;
		notify_pending = np;
		notify_override = no;
		notify_posted = ns;
		notify_fault = nf;
	}

	/** Store a new VSL feed */
	@Override
	public void doStore() throws TMSException {
		if (!VslSignHelper.isEnabled())
			throw new ChangeVetoException("VSL is not enabled");
		// The name is part of the feed's file name
		if (!name.matches("[A-Za-z0-9_]+"))
			throw new ChangeVetoException("Bad name: " + name);
		super.doStore();
	}

	/** Feed title */
	private String title;

	/** Set the feed title */
	@Override
	public void setTitle(String t) {
		title = t;
	}

	/** Set the feed title */
	public void doSetTitle(String t) throws TMSException {
		if (t == null || t.trim().isEmpty())
			throw new ChangeVetoException("Title required");
		if (!t.equals(title)) {
			store.update(this, "title", t);
			setTitle(t);
		}
	}

	/** Get the feed title */
	@Override
	public String getTitle() {
		return title;
	}

	/** Hashtag of the feed's signs */
	private String hashtag;

	/** Set the hashtag of the feed's signs */
	@Override
	public void setHashtag(String ht) {
		hashtag = ht;
	}

	/** Set the hashtag of the feed's signs */
	public void doSetHashtag(String ht) throws TMSException {
		String t = Hashtags.normalize(ht);
		if (!objectEquals(t, ht))
			throw new ChangeVetoException("Bad hashtag");
		if (!objectEquals(ht, hashtag)) {
			store.update(this, "hashtag", ht);
			setHashtag(ht);
		}
	}

	/** Get the hashtag of the feed's signs */
	@Override
	public String getHashtag() {
		return hashtag;
	}

	/** Flag to notify of approval requests */
	private boolean notify_pending;

	/** Set flag to notify of approval requests */
	@Override
	public void setNotifyPending(boolean n) {
		notify_pending = n;
	}

	/** Set flag to notify of approval requests */
	public void doSetNotifyPending(boolean n) throws TMSException {
		if (n != notify_pending) {
			store.update(this, "notify_pending", n);
			setNotifyPending(n);
		}
	}

	/** Get flag to notify of approval requests */
	@Override
	public boolean getNotifyPending() {
		return notify_pending;
	}

	/** Flag to notify of overrides */
	private boolean notify_override;

	/** Set flag to notify of overrides */
	@Override
	public void setNotifyOverride(boolean n) {
		notify_override = n;
	}

	/** Set flag to notify of overrides */
	public void doSetNotifyOverride(boolean n) throws TMSException {
		if (n != notify_override) {
			store.update(this, "notify_override", n);
			setNotifyOverride(n);
		}
	}

	/** Get flag to notify of overrides */
	@Override
	public boolean getNotifyOverride() {
		return notify_override;
	}

	/** Flag to notify of posted speeds */
	private boolean notify_posted;

	/** Set flag to notify of posted speeds */
	@Override
	public void setNotifyPosted(boolean n) {
		notify_posted = n;
	}

	/** Set flag to notify of posted speeds */
	public void doSetNotifyPosted(boolean n) throws TMSException {
		if (n != notify_posted) {
			store.update(this, "notify_posted", n);
			setNotifyPosted(n);
		}
	}

	/** Get flag to notify of posted speeds */
	@Override
	public boolean getNotifyPosted() {
		return notify_posted;
	}

	/** Flag to notify of faults */
	private boolean notify_fault;

	/** Set flag to notify of faults */
	@Override
	public void setNotifyFault(boolean n) {
		notify_fault = n;
	}

	/** Set flag to notify of faults */
	public void doSetNotifyFault(boolean n) throws TMSException {
		if (n != notify_fault) {
			store.update(this, "notify_fault", n);
			setNotifyFault(n);
		}
	}

	/** Get flag to notify of faults */
	@Override
	public boolean getNotifyFault() {
		return notify_fault;
	}

	/** Recent notifications, newest first.  These are kept in memory
	 * only.
	 *
	 * TODO: decide whether notifications should be kept across server
	 *       restarts. */
	private transient final LinkedList<VslNotice> notices =
		new LinkedList<VslNotice>();

	/** Check if the feed wants a type of notification */
	private boolean isWanted(VslNotice.Type t) {
		switch (t) {
		case PENDING:
			return notify_pending;
		case OVERRIDE:
		case OVERRIDE_CLEARED:
			return notify_override;
		case POSTED:
			return notify_posted;
		case FAULT:
		case FAULT_CLEARED:
			return notify_fault;
		default:
			return false;
		}
	}

	/** Add a notification, if the feed wants it.
	 * @return true if the notification was added. */
	private synchronized boolean add(VslNotice n) {
		if (!isWanted(n.type))
			return false;
		notices.addFirst(n);
		while (notices.size() > MAX_NOTICES)
			notices.removeLast();
		return true;
	}

	/** Get the recent notifications, newest first */
	public synchronized List<VslNotice> getNotices() {
		return new ArrayList<VslNotice>(notices);
	}
}
