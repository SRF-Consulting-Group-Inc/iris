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
 * @author John L. Stanley - SRF Consulting
 *
 */
public class VidSourceTemplateHelper extends BaseHelper {

	/** Don't allow instances to be created */
	private VidSourceTemplateHelper() {
		assert false;
	}

	/** Lookup the StreamTemplate with the specified name */
	static public VidSourceTemplate lookup(String name) {
		return (VidSourceTemplate) namespace.lookupObject(VidSourceTemplate.SONAR_TYPE,
			name);
	}

	/** Get a StreamTemplate iterator */
	static public Iterator<VidSourceTemplate> iterator() {
		return new IteratorWrapper<VidSourceTemplate>(namespace.iterator(
			VidSourceTemplate.SONAR_TYPE));
	}
}
