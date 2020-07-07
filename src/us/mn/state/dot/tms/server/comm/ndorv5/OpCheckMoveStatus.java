/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2015-2017  SRF Consulting Group
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
package us.mn.state.dot.tms.server.comm.ndorv5;

import java.io.IOException;

import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.sonar.User;
import us.mn.state.dot.tms.EventType;
import us.mn.state.dot.tms.GateArmState;
import us.mn.state.dot.tms.server.GateArmImpl;
import us.mn.state.dot.tms.server.comm.CommMessage;
import us.mn.state.dot.tms.server.comm.FutureOp;
import us.mn.state.dot.tms.server.comm.PriorityLevel;

/**
 * Operation to see if gate-arm has finished
 * moving to the correct position.
 *
 * @author John L. Stanley (SRF Consulting)
 */
@SuppressWarnings("rawtypes")
public class OpCheckMoveStatus extends OpGateNdorV5 {

	/** Requested gate arm state */
	private final GateArmState target_state;

	/** Estimated maximum time needed to move gate arm
	 * (from sending command until motion is complete).
	 * Due to possible failure conditions, this is a
	 * bit longer than twice the normal motion time. */
	private long maxEndTime;

	/** Create a new gate-arm motion-monitor operation */
	@SuppressWarnings("unchecked")
	public OpCheckMoveStatus(GateArmImpl d, User o,
			GateArmState gas, int delaySec) {
		super(PriorityLevel.DEVICE_DATA, d, false); // priority 5, non-exclusive
		user = o;
		target_state = gas;

		if (sGateArm != null) {
			// NDORv5 "Retrieve Gate Status" command
			prop = new GateNdorV5Property("*S"+sGateArm+"#\r\n");

			long started = TimeSteward.currentTimeMillis();
			maxEndTime = started + ((delaySec + 220) * 1000L);
		}
	}

	/** Create the second phase of the operation */
	protected Phase phaseTwo() {
		return (prop == null)
		     ? null
		     : (new CheckMoveCompletion());
	}

	/** Phase to check for move completion */
	protected class CheckMoveCompletion extends Phase {

		int failCount = 0;

		/** Query the status */
		@SuppressWarnings({ "unchecked", "synthetic-access" })
		protected Phase poll(CommMessage mess)
			throws IOException
		{
			mess.add(prop);
			mess.queryProps();

			// Did we get a valid response?
			if (prop.gotValidResponse() == false) {
				if (failCount++ < 3) {
					return this; // try again
				}

				handleCommError(EventType.COMM_FAILED, "No Response");
				updateStatus();
				return null;
			}
			failCount = 0;

			// Has the arm finished moving?
			GateArmState cur_state = prop.getState();
			if (cur_state == target_state) {
				updateStatus();
				return null;  // motion complete!!
			}
			if (cur_state == GateArmState.FAULT) {
				String str = prop.getMaintStatus();
				if (str.isEmpty())
					str = ""+cur_state;
				handleCommError(EventType.GATE_ARM_FAULT, str);
				updateStatus();
				return null;
			}
			if (cur_state == GateArmState.TIMEOUT) {
				handleCommError(EventType.GATE_ARM_TIMEOUT, "Gate Timeout");
				updateStatus();
				return null;
			}

			// is the maximum end-time still in the future?
			long now = TimeSteward.currentTimeMillis();
			if ((maxEndTime - now) > 0)
				requeueOp();  // if so, then keep monitoring...

			updateStatus();
			return null;
		}
	}
	
	private void requeueOp() {
		FutureOp.queueOp(device, 10, this); // recheck in ~10 seconds
	}

	/** Cleanup the operation */
	@Override
	public void cleanup() {
		super.cleanup();
		if (!isSuccess())
			gate_arm.checkTimeout();
	}
}
