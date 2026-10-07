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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.DMSHelper;
import us.mn.state.dot.tms.GeoLocHelper;
import us.mn.state.dot.tms.VslSign;
import us.mn.state.dot.tms.VslState;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyColumn;
import us.mn.state.dot.tms.client.proxy.ProxyDescriptor;
import us.mn.state.dot.tms.client.proxy.ProxyTableModel;

/**
 * Table model for VSL recommendations waiting for an operator, with the
 * lowest recommended speed first (VSL-APV-03).
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslApprovalModel extends ProxyTableModel<VslSign> {

	/** Create a proxy descriptor.  The create/delete slot of the table
	 * panel holds the approve and reject buttons. */
	static private ProxyDescriptor<VslSign> descriptor(Session s) {
		return new ProxyDescriptor<VslSign>(
			s.getSonarState().getVslSigns(),
			false,	/* has_properties */
			true,	/* has_create_delete */
			false	/* has_name */
		);
	}

	/** Get the recommended speed of a sign for sorting */
	static private int recSpeed(VslSign vs) {
		Integer s = vs.getRecSpeed();
		return (s != null) ? s : Integer.MAX_VALUE;
	}

	/** Get the location of a sign */
	static private String getLocation(VslSign vs) {
		DMS dms = DMSHelper.lookup(vs.getName());
		return (dms != null)
		      ? GeoLocHelper.getLocation(dms.getGeoLoc())
		      : "";
	}

	/** Time formatter */
	private final SimpleDateFormat time_format =
		new SimpleDateFormat("HH:mm");

	/** Format a time stamp */
	private String formatTime(Long ts) {
		return (ts != null) ? time_format.format(new Date(ts)) : "";
	}

	/** Create the columns in the model */
	@Override
	protected ArrayList<ProxyColumn<VslSign>> createColumns() {
		ArrayList<ProxyColumn<VslSign>> cols =
			new ArrayList<ProxyColumn<VslSign>>(5);
		cols.add(new ProxyColumn<VslSign>("device.name", 120) {
			public Object getValueAt(VslSign vs) {
				return vs.getName();
			}
		});
		cols.add(new ProxyColumn<VslSign>("location", 260) {
			public Object getValueAt(VslSign vs) {
				return getLocation(vs);
			}
		});
		cols.add(new ProxyColumn<VslSign>("dms.vsl.posted", 80,
			Integer.class)
		{
			public Object getValueAt(VslSign vs) {
				return vs.getPostedSpeed();
			}
		});
		cols.add(new ProxyColumn<VslSign>("dms.vsl.recommended", 100,
			Integer.class)
		{
			public Object getValueAt(VslSign vs) {
				return vs.getRecSpeed();
			}
		});
		cols.add(new ProxyColumn<VslSign>("dms.vsl.requested", 80) {
			public Object getValueAt(VslSign vs) {
				return formatTime(vs.getRecTime());
			}
		});
		return cols;
	}

	/** Create a new VSL approval table model */
	public VslApprovalModel(Session s) {
		super(s, descriptor(s), 12);
	}

	/** Check if a sign is waiting for an operator */
	@Override
	protected boolean check(VslSign vs) {
		return VslState.fromOrdinal(vs.getRecState()) ==
		       VslState.PENDING;
	}

	/** Get a comparator for the lowest recommended speed first */
	@Override
	protected Comparator<VslSign> comparator() {
		return new Comparator<VslSign>() {
			public int compare(VslSign a, VslSign b) {
				int c = Integer.compare(recSpeed(a),
					recSpeed(b));
				return (c != 0)
				      ? c
				      : a.getName().compareTo(b.getName());
			}
		};
	}

	/** Signs can't be added from this table */
	@Override
	public boolean canAdd() {
		return false;
	}

	/** Signs can't be removed from this table */
	@Override
	public boolean canRemove(VslSign vs) {
		return false;
	}
}
