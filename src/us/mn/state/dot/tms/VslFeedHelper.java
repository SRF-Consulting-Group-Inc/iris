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
package us.mn.state.dot.tms;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Helper class for VSL feeds.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslFeedHelper extends BaseHelper {

	/** Don't allow instances to be created */
	private VslFeedHelper() {
		assert false;
	}

	/** Lookup the VSL feed with the specified name */
	static public VslFeed lookup(String name) {
		return (VslFeed) namespace.lookupObject(VslFeed.SONAR_TYPE,
			name);
	}

	/** Get a VSL feed iterator */
	static public Iterator<VslFeed> iterator() {
		return new IteratorWrapper<VslFeed>(namespace.iterator(
			VslFeed.SONAR_TYPE));
	}

	/** Find the feeds covering a sign */
	static public ArrayList<VslFeed> find(DMS dms) {
		// TODO a feed with no hashtag covers no signs; ask NDDOT
		//      whether it should cover all VSL signs instead
		ArrayList<VslFeed> feeds = new ArrayList<VslFeed>();
		Hashtags tags = new Hashtags(dms.getNotes());
		Iterator<VslFeed> it = iterator();
		while (it.hasNext()) {
			VslFeed f = it.next();
			if (tags.contains(f.getHashtag()))
				feeds.add(f);
		}
		return feeds;
	}
}
