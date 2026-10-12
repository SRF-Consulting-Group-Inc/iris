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

import java.util.ArrayList;
import us.mn.state.dot.tms.VslFeed;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyColumn;
import us.mn.state.dot.tms.client.proxy.ProxyDescriptor;
import us.mn.state.dot.tms.client.proxy.ProxyTableModel;

/**
 * Table model for VSL feeds.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslFeedModel extends ProxyTableModel<VslFeed> {

	/** Create a proxy descriptor */
	static public ProxyDescriptor<VslFeed> descriptor(Session s) {
		return new ProxyDescriptor<VslFeed>(
			s.getSonarState().getVslFeeds(),
			false
		);
	}

	/** Create the columns in the model */
	@Override
	protected ArrayList<ProxyColumn<VslFeed>> createColumns() {
		ArrayList<ProxyColumn<VslFeed>> cols =
			new ArrayList<ProxyColumn<VslFeed>>(7);
		cols.add(new ProxyColumn<VslFeed>("device.name", 100) {
			public Object getValueAt(VslFeed f) {
				return f.getName();
			}
		});
		cols.add(new ProxyColumn<VslFeed>("dms.vsl.feed.title", 200) {
			public Object getValueAt(VslFeed f) {
				return f.getTitle();
			}
			public boolean isEditable(VslFeed f) {
				return canWrite(f);
			}
			public void setValueAt(VslFeed f, Object value) {
				f.setTitle(value.toString().trim());
			}
		});
		cols.add(new ProxyColumn<VslFeed>("dms.vsl.hashtag", 100) {
			public Object getValueAt(VslFeed f) {
				return f.getHashtag();
			}
			public boolean isEditable(VslFeed f) {
				return canWrite(f);
			}
			public void setValueAt(VslFeed f, Object value) {
				String ht = value.toString().trim();
				f.setHashtag((ht.length() > 0) ? ht : null);
			}
		});
		cols.add(new ProxyColumn<VslFeed>("dms.vsl.feed.pending", 80,
			Boolean.class)
		{
			public Object getValueAt(VslFeed f) {
				return f.getNotifyPending();
			}
			public boolean isEditable(VslFeed f) {
				return canWrite(f);
			}
			public void setValueAt(VslFeed f, Object value) {
				if (value instanceof Boolean)
					f.setNotifyPending((Boolean) value);
			}
		});
		cols.add(new ProxyColumn<VslFeed>("dms.vsl.feed.override", 80,
			Boolean.class)
		{
			public Object getValueAt(VslFeed f) {
				return f.getNotifyOverride();
			}
			public boolean isEditable(VslFeed f) {
				return canWrite(f);
			}
			public void setValueAt(VslFeed f, Object value) {
				if (value instanceof Boolean)
					f.setNotifyOverride((Boolean) value);
			}
		});
		cols.add(new ProxyColumn<VslFeed>("dms.vsl.feed.posted", 80,
			Boolean.class)
		{
			public Object getValueAt(VslFeed f) {
				return f.getNotifyPosted();
			}
			public boolean isEditable(VslFeed f) {
				return canWrite(f);
			}
			public void setValueAt(VslFeed f, Object value) {
				if (value instanceof Boolean)
					f.setNotifyPosted((Boolean) value);
			}
		});
		cols.add(new ProxyColumn<VslFeed>("dms.vsl.feed.fault", 80,
			Boolean.class)
		{
			public Object getValueAt(VslFeed f) {
				return f.getNotifyFault();
			}
			public boolean isEditable(VslFeed f) {
				return canWrite(f);
			}
			public void setValueAt(VslFeed f, Object value) {
				if (value instanceof Boolean)
					f.setNotifyFault((Boolean) value);
			}
		});
		return cols;
	}

	/** Create a new VSL feed table model */
	public VslFeedModel(Session s) {
		super(s, descriptor(s), 12);
	}
}
