/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2019  SRF Consulting Group
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
package us.mn.state.dot.tms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class for CameraStreamLink objects
 * 
 * @author John L. Stanley - SRF Consulting
 *
 */
public class CameraVidSourceOrderHelper extends BaseHelper {

	/** Don't allow instances to be created */
	private CameraVidSourceOrderHelper() {
		assert false;
	}

	/** Lookup the camera template with the specified name */
	static public CameraVidSourceOrder lookup(String name) {
		return (CameraVidSourceOrder)namespace.lookupObject(CameraVidSourceOrder.SONAR_TYPE,
			name);
	}

	/** Get a camera template iterator */
	static public Iterator<CameraVidSourceOrder> iterator() {
		return new IteratorWrapper<CameraVidSourceOrder>(namespace.iterator(
				CameraVidSourceOrder.SONAR_TYPE));
	}
}
