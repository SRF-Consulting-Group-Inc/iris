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

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import us.mn.state.dot.tms.VslFeed;
import us.mn.state.dot.tms.VslSign;
import us.mn.state.dot.tms.client.MapTab;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyTablePanel;
import us.mn.state.dot.tms.utils.I18N;

/**
 * Tab for NDDOT VSL approvals and feeds.  The same tables are also in the
 * Message Signs menu.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslTab extends MapTab<VslSign> {

	/** Create a titled panel holding a table panel */
	static private JPanel createTitled(String t, ProxyTablePanel<?> p) {
		JPanel pnl = new JPanel(new BorderLayout());
		pnl.setBorder(BorderFactory.createTitledBorder(I18N.get(t)));
		pnl.add(p, BorderLayout.CENTER);
		return pnl;
	}

	/** VSL approvals panel */
	private final VslApprovalPanel approvals;

	/** VSL feeds panel, or null if not permitted */
	private final ProxyTablePanel<VslFeed> feeds;

	/** Create a new VSL tab */
	public VslTab(Session session, VslManager m) {
		super(m);
		approvals = new VslApprovalPanel(session);
		feeds = VslFeedForm.isPermitted(session)
		      ? new ProxyTablePanel<VslFeed>(new VslFeedModel(session))
		      : null;
		add(createTitled("dms.vsl.approvals", approvals),
			BorderLayout.NORTH);
		if (feeds != null) {
			add(createTitled("dms.vsl.feeds", feeds),
				BorderLayout.CENTER);
		}
	}

	/** Get tab number for ordering */
	@Override
	public int getTabNum() {
		return 15;
	}

	/** Initialize the VSL tab */
	@Override
	public void initialize() {
		approvals.initialize();
		if (feeds != null)
			feeds.initialize();
	}

	/** Dispose of the VSL tab */
	@Override
	public void dispose() {
		super.dispose();
		approvals.dispose();
		if (feeds != null)
			feeds.dispose();
	}
}
