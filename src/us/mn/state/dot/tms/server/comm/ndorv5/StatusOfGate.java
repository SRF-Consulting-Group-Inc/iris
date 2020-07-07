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

/**
 * Gate status for NdorGateV5 gate arms.
 * 
 * Note:  Updated in August 2016 to include
 * multi-arm gate protocol referred to as v5.
 *
 * @author John L. Stanley (SRF Consulting)
 */
public enum StatusOfGate {    // a-Gate Status  (0-9	Gate Status)
	CLOSE_COMPLETE,         //  0 - Closed (lowered)
	OPEN_COMPLETE,          //  1 - Open (raised)
	OPEN_IN_PROGRESS,       //  2 - Gate moving from Closed to Open
	CLOSE_IN_PROGRESS,      //  3 - Gate Moving from Open to Closed
	UNKNOWN_4,
	UNKNOWN_5,
	TIMEOUT_STILL_CLOSED,   //  6 - Error - Timed out and never moved from Closed
	TIMEOUT_OPENING_FAILED, //  7 - Error - Timed out moving from Closed to Open
	TIMEOUT_STILL_OPENED,   //  8 - Error - Timed out and never moved from Open
	TIMEOUT_CLOSING_FAILED, //  9 - Error - Timed out moving from Open to Closed 
	
	// Following error state is from v5 protocol extension....
	GATE_NOT_CONFIGURED;    // 10 - Error - Requested gate-arm number not configured

	/** Lookup status from ordinal */
	static public StatusOfGate fromOrdinal(int o) {
		for (StatusOfGate cs: StatusOfGate.values()) {
			if (cs.ordinal() == o)
				return cs;
		}
		return null;
	}

	/** Test if status is "reset" */
	public boolean isReset() {
		return false;  // NdorGateV5 controllers don't report RESET status.
	}

	/** Test if status is "normal" */
	public boolean isNormal() {
		switch (this) {
			case OPEN_IN_PROGRESS:
			case OPEN_COMPLETE:
			case CLOSE_IN_PROGRESS:
			case CLOSE_COMPLETE:
				return true;
			default:
				return false;
		}
	}

	/** Test if status is "fault" */
	public boolean isFault() {
		switch (this) {
			case TIMEOUT_STILL_CLOSED:
			case TIMEOUT_OPENING_FAILED:
			case TIMEOUT_STILL_OPENED:
			case TIMEOUT_CLOSING_FAILED:
			case GATE_NOT_CONFIGURED:
				return true;
			default:
				return false;
		}
	}

	/** Test if status is "opening" */
	public boolean isOpening() {
		switch (this) {
			case OPEN_IN_PROGRESS:
				return true;
			default:
				return false;
		}
	}

	/** Test if status is "closing" */
	public boolean isClosing() {
		switch (this) {
			case CLOSE_IN_PROGRESS:
				return true;
			default:
				return false;
		}
	}

	/** Test if status is "moving" */
	public boolean isMoving() {
		switch (this) {
			case OPEN_IN_PROGRESS:
			case CLOSE_IN_PROGRESS:
				return true;
			default:
				return false;
		}
	}

	/** Test if status is "open" */
	public boolean isOpen() {
		switch (this) {
		case OPEN_COMPLETE:
		case TIMEOUT_STILL_OPENED:
			return true;
		default:
			return false;
		}
	}

	/** Test if status is "closed" */
	public boolean isClosed() {
		switch (this) {
		case CLOSE_COMPLETE:
		case TIMEOUT_STILL_CLOSED:
			return true;
		default:
			return false;
		}
	}
}
