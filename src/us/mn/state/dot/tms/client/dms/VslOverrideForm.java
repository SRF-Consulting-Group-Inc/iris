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
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.DMSHelper;
import us.mn.state.dot.tms.DmsLock;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.VslSign;
import us.mn.state.dot.tms.VslSignHelper;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.widget.AbstractForm;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.IPanel;
import us.mn.state.dot.tms.client.widget.IPanel.Stretch;
import us.mn.state.dot.tms.utils.I18N;

/**
 * A form for overriding the speed of a VSL sign (VSL-VOV-01).  The override
 * is an operator message, which stays until an operator blanks the sign
 * (VSL-VOV-02).
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslOverrideForm extends AbstractForm {

	/** Lowest override speed (mph) */
	static private final int SPEED_MIN = 5;

	/** Get the highest override speed (mph) */
	static private int getSpeedMax() {
		return SystemAttrEnum.SPEED_LIMIT_MAX_MPH.getInt();
	}

	/** Check if the user is permitted to override a sign.  Like other
	 * operator messages, the sign must be active. */
	static public boolean isPermitted(Session s, DMS dms) {
		return VslSignHelper.isEnabled() &&
		       VslSignHelper.lookup(dms.getName()) != null &&
		       DMSHelper.isActive(dms) &&
		       s.isWritePermitted(dms, "lock");
	}

	/** Get the starting speed: the posted VSL speed, if any */
	static private int startSpeed(DMS dms) {
		VslSign vs = VslSignHelper.lookup(dms.getName());
		Integer s = (vs != null) ? vs.getPostedSpeed() : null;
		int mx = getSpeedMax();
		return (s != null) ? Math.max(SPEED_MIN, Math.min(s, mx)) : mx;
	}

	/** User session */
	private final Session session;

	/** Sign to override */
	private final DMS dms;

	/** Speed model */
	private final SpinnerNumberModel speed_mdl;

	/** Speed spinner */
	private final JSpinner speed_spn;

	/** Action to apply the override */
	private final IAction apply_act = new IAction(
		"dms.vsl.override.apply")
	{
		protected void doActionPerformed(ActionEvent e) {
			applyOverride();
			close(session.getDesktop());
		}
	};

	/** Create a new VSL override form */
	public VslOverrideForm(Session s, DMS sign) {
		super(I18N.get("dms.vsl.override") + ": " + sign.getName());
		session = s;
		dms = sign;
		speed_mdl = new SpinnerNumberModel(startSpeed(sign), SPEED_MIN,
			getSpeedMax(), 5);
		speed_spn = new JSpinner(speed_mdl);
	}

	/** Initialize the form */
	@Override
	protected void initialize() {
		super.initialize();
		IPanel p = new IPanel();
		p.add("dms.vsl.override.help", Stretch.CENTER);
		p.add("dms.vsl.override.speed");
		p.add(speed_spn, Stretch.LAST);
		p.add(new JButton(apply_act), Stretch.RIGHT);
		add(p);
	}

	/** Set an operator message with the override speed.  It has no
	 * duration, so it stays until the sign is blanked. */
	private void applyOverride() {
		DmsLock lk = new DmsLock(null);
		lk.setReason(DmsLock.REASON_SITUATION);
		lk.setMulti(speed_mdl.getNumber().toString());
		lk.setUser(session.getUser().getName());
		dms.setLock(lk.toString());
	}
}
