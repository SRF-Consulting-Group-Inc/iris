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
 * 
 * Derived in part from MNDOT's IRIS code for controlling their
 * HySecurity STC gates.
 */
package us.mn.state.dot.tms.server.comm.ndorv5;

import us.mn.state.dot.tms.GateArmState;
import us.mn.state.dot.tms.server.GateArmImpl;
import us.mn.state.dot.tms.server.comm.ControllerProperty;
import us.mn.state.dot.tms.server.comm.OpDevice;
import us.mn.state.dot.tms.server.comm.PriorityLevel;

import static us.mn.state.dot.tms.server.comm.ndorv5.GateNdorV5Poller.GATENDORv5_LOG;

import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.sonar.User;

/**
 * Operation for NDOR Gate v5 device
 * Note:  Code updated in August 2016 to include
 * multi-arm gate protocol referred to as v5.
 *
 * @author:  John L. Stanley (SRF Consulting)
 */
abstract public class OpGateNdorV5<T extends ControllerProperty>
  extends OpDevice<T> {

	/** User who initiated control */
	protected User user = null;

	/** Log an error msg */
	protected void logError(String msg) {
		if (GATENDORv5_LOG.isOpen())
			GATENDORv5_LOG.log(controller.getName() + "! " + msg);
	}

	/** Gate arm device */
	protected final GateArmImpl gate_arm;

	/** Interval to update controller operation count */
	static private final long OP_COUNT_INTERVAL_MS = 30 * 1000;

	/** Status property */
	protected GateNdorV5Property prop;

	/** Time to update operation counts */
	private long op_time = TimeSteward.currentTimeMillis();

	/** Create a new NDOR Gate v5 operation */
	protected OpGateNdorV5(PriorityLevel p, GateArmImpl ga, boolean ex) {
		super(p, ga, ex);
		gate_arm = ga;
		initGateOpVars();
	}

	// String representation of the controller's gate-arm number
	// using NDOR gate-protocol v5 (with multi-gate extension)
	// (IRIS controller pin number) == (NDORv5 controller gate arm number)
	//   pin 1 --> ""
	//   pin 2-8 --> "2"-"8"
	//   all other gate numbers --> null
	protected String sGateArm;
	
	protected String sArmNumber;
	
	/** Create a new NDOR Gate v5 operation */
	protected OpGateNdorV5(PriorityLevel p, GateArmImpl ga) {
		super(p, ga);
		gate_arm = ga;
		initGateOpVars();
	}

	protected void initGateOpVars() {
		int pin = gate_arm.getPin();
		sArmNumber = ""+pin;
		if ((pin < 1) || (pin > 8)) {
			setErrorStatus("Invalid pin");
			sGateArm = null;
		} else
			sGateArm = (pin == 1) ? "" : (""+pin);
	}

	/** Update controller status */
	protected void updateStatus() {
		if ((prop.statusOfGate == StatusOfGate.OPEN_COMPLETE)
		 &&	(prop.gateArmLights == StatusOfGateArmLights.LIGHTS_ON))
			setNdorV5ArmStateNotify(GateArmState.WARN_CLOSE, user);
		else if ((gate_arm.getArmStateEnum() == GateArmState.WARN_CLOSE)
			  && (prop.statusOfGate == StatusOfGate.CLOSE_IN_PROGRESS)) {
			// Trick GUI to shift arm state from WARN_CLOSE to CLOSING
			setNdorV5ArmStateNotify(GateArmState.UNKNOWN, user);
			setNdorV5ArmStateNotify(GateArmState.CLOSING, user);
		}
		else if ((gate_arm.getArmStateEnum() == GateArmState.BEACON_ON)
			  && (prop.statusOfGate == StatusOfGate.CLOSE_IN_PROGRESS))	{
			// Trick GUI to shift arm state from BEACON_ON to CLOSING
			setNdorV5ArmStateNotify(GateArmState.UNKNOWN, user);
			setNdorV5ArmStateNotify(GateArmState.CLOSING, user);
		}
		else			
			setNdorV5ArmStateNotify(prop.getState(), user);

		if (prop.isMoving() == false) {
			setMaintStatus(prop.getMaintStatus());
			updateMaintStatus();
		}

		if (shouldUpdateOpCount()) {
			controller.completeOperation(id, isSuccess());
			op_time += OP_COUNT_INTERVAL_MS;
		}
	}

	/** Check if we should update the controller operation count */
	private boolean shouldUpdateOpCount() {
		return (TimeSteward.currentTimeMillis() >= op_time);
	}

	@Override
	public String toString() {
		// Default OpController.toString() uses the controller's
		// name.  NDOR gates can have more than one arm per
		// controller so we need this override version.
		return "("+this.getClass().getSimpleName()+", "+gate_arm.getName()+")";
	}

	//-----------------------------------------------------

	/** Set the gate arm state with a temporary
	 *  override for the NDORv5Gate BEACON_ON state.
	 * @param gas Gate arm state.
	 * @param o User who requested new state, or null. */
	public void setNdorV5ArmStateNotify(GateArmState gas, User o) {
		// Temporarily substitute BEACON_ON for OPEN
		// at start of NDOR-gate CLOSE cycle
		if (gate_arm.getBeaconOn()
		 && (gas != GateArmState.BEACON_ON)) {
			if (gas == GateArmState.OPEN)
				gas = GateArmState.BEACON_ON;
			else
				gate_arm.setBeaconOn(false);
		}

		gate_arm.setArmStateNotify(gas, o);
	}
}
