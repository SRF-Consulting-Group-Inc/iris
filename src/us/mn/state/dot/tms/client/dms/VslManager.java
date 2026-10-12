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

import javax.swing.JPopupMenu;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.ItemStyle;
import us.mn.state.dot.tms.VslSign;
import us.mn.state.dot.tms.VslSignHelper;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.GeoLocManager;
import us.mn.state.dot.tms.client.proxy.ProxyDescriptor;
import us.mn.state.dot.tms.client.proxy.ProxyManager;
import us.mn.state.dot.tms.client.proxy.ProxyTheme;

/**
 * A VSL manager is a container for NDDOT VSL sign objects.  It has no map
 * layer; it only provides the VSL tab.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslManager extends ProxyManager<VslSign> {

	/** Create a descriptor for VSL signs */
	static private ProxyDescriptor<VslSign> descriptor(Session s) {
		return new ProxyDescriptor<VslSign>(
			s.getSonarState().getVslSigns(), false
		);
	}

	/** Create a new VSL manager */
	public VslManager(Session s, GeoLocManager lm) {
		super(s, lm, descriptor(s), 0, ItemStyle.ALL);
	}

	/** Check if user can read VSL signs.  Nothing is shown unless VSL is
	 * enabled. */
	@Override
	public boolean canRead() {
		return VslSignHelper.isEnabled() && super.canRead();
	}

	/** Create a VSL map tab */
	@Override
	public VslTab createTab() {
		return new VslTab(session, this);
	}

	/** Create a theme for VSL signs */
	@Override
	protected ProxyTheme<VslSign> createTheme() {
		ProxyTheme<VslSign> theme = new ProxyTheme<VslSign>(this,
			new DmsMarker());
		theme.addStyle(ItemStyle.ALL);
		return theme;
	}

	/** Find the map geo location for a proxy */
	@Override
	protected GeoLoc getGeoLoc(VslSign proxy) {
		return null;
	}

	/** Create a popup menu for a single selection */
	@Override
	protected JPopupMenu createPopupSingle(VslSign proxy) {
		return null;
	}
}
