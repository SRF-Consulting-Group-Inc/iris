/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2015-2021  SRF Consulting Group
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
 * Status for NdorGateV5 warning sign(s).
 *
 * @author John L. Stanley - SRF Consulting
 */
public enum StatusOfWarningSignLights {
	//	Gate response field b - Warning Sign Light Status (0-2)
	SIGN_OFF,    // 0 - Off
	SIGN_ON,     // 1 - On
	SIGN_ERROR;  // 2 - Error

	/** Lookup status from ordinal */
	static public StatusOfWarningSignLights fromOrdinal(int o) {
		for (StatusOfWarningSignLights cs: StatusOfWarningSignLights.values()) {
			if (cs.ordinal() == o)
				return cs;
		}
		return null;
	}

	/** Test if lights are on */
	public boolean isOn() {
		return (this == StatusOfWarningSignLights.SIGN_ON);
	}

	/** Test if lights are off */
	public boolean isOff() {
		return (this == StatusOfWarningSignLights.SIGN_OFF);
	}

	/** Test if lights are in an error state */
	public boolean isError() {
		return (this == StatusOfWarningSignLights.SIGN_ERROR);
	}
}
