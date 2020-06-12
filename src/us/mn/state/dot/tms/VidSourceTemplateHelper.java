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

import java.util.Iterator;

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
	
	/** Lookup the StreamTemplate with the specified label. */
	static public VidSourceTemplate lookupLabel(String label) {
		Iterator<VidSourceTemplate> it = iterator();
		while (it.hasNext()) {
			VidSourceTemplate vst = it.next();
			if (vst.getLabel() != null && vst.getLabel().equals(label))
				return vst;
		}
		return null;
	}
	
	/** Return the first available name for a VidSourceTemplate object. Note
	 *  that we won't allow more than 10,000,000,000,000 objects due to the
	 *  database name length limit (though this is just an artificial limit).
	 *  
	 *  The name will be "VID_SRC_#".
	 */
	static public String getFirstAvailableName() {
		for (int i = 0; i <= 9999999999999L; ++i) {
			// generate a name with this number and try to lookup an object
			String n = "VID_SRC_" + String.valueOf(i);
			VidSourceTemplate vst = lookup(n);
			if (vst == null)
				return n;
		}
		return null;
	}
	
	/** Get a StreamTemplate iterator */
	static public Iterator<VidSourceTemplate> iterator() {
		return new IteratorWrapper<VidSourceTemplate>(namespace.iterator(
			VidSourceTemplate.SONAR_TYPE));
	}
}
