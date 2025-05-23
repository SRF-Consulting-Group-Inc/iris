/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2012-2025  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.dinrelay;

import java.util.Iterator;
import us.mn.state.dot.tms.Lcs;
import us.mn.state.dot.tms.LcsHelper;
import us.mn.state.dot.tms.LcsIndication;
import us.mn.state.dot.tms.LcsState;
import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.LcsImpl;
import us.mn.state.dot.tms.server.comm.CommMessage;
import us.mn.state.dot.tms.server.comm.DevicePoller;
import us.mn.state.dot.tms.server.comm.PriorityLevel;

/**
 * Operation to query the indications of a Lane Control Signal array.
 *
 * @author Douglas Lau
 */
public class OpQueryLCSIndications extends OpLCS {

	/** LCS array indications */
	private final int[] indications;

	/** Flag to indicate all helper ops succeeded */
	private boolean op_success = true;

	/** Count of remaining operations */
	private int n_ops = 0;

	/** Create a new operation to query the LCS */
	public OpQueryLCSIndications(LcsImpl l) {
		super(PriorityLevel.POLL_HIGH, l);
		indications = LcsHelper.makeIndications(lcs,
			LcsIndication.DARK);
	}

	/** Create the second phase of the operation */
	@Override
	protected Phase<DinRelayProperty> phaseTwo() {
		return new CreateOutletQueries();
	}

	/** Phase to create operations to query outlet status */
	private class CreateOutletQueries extends Phase<DinRelayProperty> {

		/** Create the outlet query operations */
		protected Phase<DinRelayProperty> poll(
			CommMessage<DinRelayProperty> mess)
		{
			Iterator<ControllerImpl> it = controllers.iterator();
			synchronized (this) {
				while (it.hasNext())
					createOutletOp(it.next());
			}
			return null;
		}
	}

	/** Create one outlet query operation */
	private void createOutletOp(final ControllerImpl c) {
		DevicePoller dp = c.getPoller();
		if (dp instanceof DinRelayPoller) {
			DinRelayPoller drp = (DinRelayPoller) dp;
			drp.queryOutlets(c, new OutletProperty(
				new OutletProperty.OutletCallback()
			{
				public void updateOutlets(boolean[] outlets) {
					opUpdateOutlets(c, outlets);
				}
				public void complete(boolean success) {
					opComplete(success);
				}
			}));
			n_ops++;
		} else
			op_success = false;
	}

	/** Update the indications from one controller.
	 * @param c Controller being updated.
	 * @param outlets Current outlet state for controller. */
	private void opUpdateOutlets(ControllerImpl c, boolean[] outlets) {
		for (LcsState ls: LcsHelper.lookupStates(lcs)) {
			if (ls.getController() == c)
				updateIndication(ls, outlets);
		}
	}

	/** Update one indication value (if set).
	 * @param ls LCS state pin association.
	 * @param outlets Array of outlet states, indexed by pin */
	private void updateIndication(LcsState ls, boolean[] outlets) {
		int p = ls.getPin() - 1;
		if (p >= 0 && p < outlets.length && outlets[p]) {
			int ln = ls.getLane() - 1;
			// We must check bounds here in case the LcsState
			// was added after the "indications" array was created
			if (ln >= 0 && ln < indications.length)
				indications[ln] = ls.getIndication();
		}
	}

	/** Cleanup the operation */
	@Override
	public void cleanup() {
		testComplete();
	}

	/** Test if sub-operations are complete, and cleanup if necessary */
	private synchronized void testComplete() {
		if (n_ops <= 0) {
			if (!op_success)
				clearIndications();
			lcs.setIndicationsNotify(indications);
			super.cleanup();
		}
	}

	/** Cleanup one sub-operation */
	private synchronized void opComplete(boolean success) {
		if (!success)
			op_success = false;
		n_ops--;
		testComplete();
	}

	/** Clear all indications */
	private void clearIndications() {
		for (int ln = 0; ln < indications.length; ln++)
			indications[ln] = LcsIndication.UNKNOWN.ordinal();
	}
}
