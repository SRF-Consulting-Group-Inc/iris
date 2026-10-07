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
import javax.swing.GroupLayout;
import javax.swing.JButton;
import us.mn.state.dot.tms.VslSign;
import us.mn.state.dot.tms.VslState;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyTablePanel;
import us.mn.state.dot.tms.client.widget.IAction;
import static us.mn.state.dot.tms.client.widget.Widgets.UI;

/**
 * A panel for approving or rejecting VSL recommendations.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslApprovalPanel extends ProxyTablePanel<VslSign> {

	/** Action to approve the selected recommendation */
	private final IAction approve_act = new IAction("dms.vsl.approve") {
		protected void doActionPerformed(ActionEvent e) {
			decide(VslState.APPROVED);
		}
	};

	/** Action to reject the selected recommendation */
	private final IAction reject_act = new IAction("dms.vsl.reject") {
		protected void doActionPerformed(ActionEvent e) {
			decide(VslState.REJECTED);
		}
	};

	/** Create a new VSL approval panel */
	public VslApprovalPanel(Session s) {
		super(new VslApprovalModel(s));
	}

	/** Add approve and reject buttons in place of create and delete */
	@Override
	protected void addCreateDeleteWidgets(GroupLayout.SequentialGroup hg,
		GroupLayout.ParallelGroup vg)
	{
		JButton approve_btn = new JButton(approve_act);
		hg.addComponent(approve_btn);
		vg.addComponent(approve_btn);
		hg.addGap(UI.hgap);
		JButton reject_btn = new JButton(reject_act);
		hg.addComponent(reject_btn);
		vg.addComponent(reject_btn);
	}

	/** Update the button panel.  Decisions are operations, so edit mode
	 * is not needed. */
	@Override
	public void updateButtonPanel() {
		VslSign vs = getSelectedProxy();
		boolean d = vs != null &&
			model.getSession().isWritePermitted(vs, "decision");
		approve_act.setEnabled(d);
		reject_act.setEnabled(d);
		super.updateButtonPanel();
	}

	/** Decide on the selected recommendation */
	private void decide(VslState st) {
		VslSign vs = getSelectedProxy();
		if (vs != null)
			vs.setDecision(st.ordinal());
	}
}
