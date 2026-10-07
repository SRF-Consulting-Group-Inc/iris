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

import us.mn.state.dot.tms.VslSign;
import us.mn.state.dot.tms.VslSignHelper;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyTableForm;
import us.mn.state.dot.tms.utils.I18N;

/**
 * A form for VSL recommendations waiting for an operator.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslApprovalForm extends ProxyTableForm<VslSign> {

	/** Check if the user is permitted to use the form */
	static public boolean isPermitted(Session s) {
		return VslSignHelper.isEnabled() &&
		       s.canRead(VslSign.SONAR_TYPE);
	}

	/** Create a new VSL approval form */
	public VslApprovalForm(Session s) {
		super(I18N.get("dms.vsl.approvals"), new VslApprovalPanel(s));
	}
}
