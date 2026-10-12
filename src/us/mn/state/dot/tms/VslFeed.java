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

import us.mn.state.dot.sonar.SonarObject;

/**
 * An RSS feed of NDDOT VSL notifications, read by GovDelivery
 * (VSL-NTF-01).  A feed covers the VSL signs whose notes contain its
 * hashtag.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public interface VslFeed extends SonarObject {

	/** SONAR type name */
	String SONAR_TYPE = "vsl_feed";

	/** Get the SONAR type name */
	@Override
	default String getTypeName() {
		return SONAR_TYPE;
	}

	/** SONAR base type name */
	String SONAR_BASE = DMS.SONAR_TYPE;

	/** Set the feed title */
	void setTitle(String t);

	/** Get the feed title */
	String getTitle();

	/** Set the hashtag of the feed's signs */
	void setHashtag(String ht);

	/** Get the hashtag of the feed's signs */
	String getHashtag();

	/** Set flag to notify of approval requests (VSL-NTF-03) */
	void setNotifyPending(boolean n);

	/** Get flag to notify of approval requests (VSL-NTF-03) */
	boolean getNotifyPending();

	/** Set flag to notify of overrides (VSL-NTF-04, VSL-NTF-05) */
	void setNotifyOverride(boolean n);

	/** Get flag to notify of overrides (VSL-NTF-04, VSL-NTF-05) */
	boolean getNotifyOverride();

	/** Set flag to notify of posted speeds (VSL-NTF-06) */
	void setNotifyPosted(boolean n);

	/** Get flag to notify of posted speeds (VSL-NTF-06) */
	boolean getNotifyPosted();

	/** Set flag to notify of faults (VSL-NTF-07) */
	void setNotifyFault(boolean n);

	/** Get flag to notify of faults (VSL-NTF-07) */
	boolean getNotifyFault();
}
