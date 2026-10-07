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
 * NDDOT variable speed limit (VSL) sign.  A VSL sign is a DMS, and has the
 * same name.  Its weather sensors and detectors are the ones whose notes
 * contain the sign's hashtag.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public interface VslSign extends SonarObject {

	/** SONAR type name */
	String SONAR_TYPE = "vsl_sign";

	/** Get the SONAR type name */
	@Override
	default String getTypeName() {
		return SONAR_TYPE;
	}

	/** SONAR base type name */
	String SONAR_BASE = DMS.SONAR_TYPE;

	/** Set the hashtag of the sign's weather sensors and detectors */
	void setHashtag(String ht);

	/** Get the hashtag of the sign's weather sensors and detectors */
	String getHashtag();

	/** Set the minimum speed (mph) */
	void setMinSpeed(int s);

	/** Get the minimum speed (mph) */
	int getMinSpeed();

	/** Set the maximum speed (mph) */
	void setMaxSpeed(int s);

	/** Get the maximum speed (mph) */
	int getMaxSpeed();

	/** Set the approval mode (ordinal of VslApprovalMode) */
	void setApprovalMode(int m);

	/** Get the approval mode (ordinal of VslApprovalMode) */
	int getApprovalMode();

	/** Get flag set when a weather sensor can be used, or null before
	 * the first calculation */
	Boolean getAvailable();

	/** Get the recommended speed (mph), or null if none */
	Integer getRecSpeed();

	/** Get the recommendation state (ordinal of VslState) */
	int getRecState();

	/** Get the recommendation time, or null if none.  This time is in
	 * milliseconds since the epoch. */
	Long getRecTime();

	/** Get the posted speed (mph), or null if none */
	Integer getPostedSpeed();

	/** Get the posted time, or null if none.  This time is in
	 * milliseconds since the epoch. */
	Long getPostedTime();

	/** Get the quiet period expiration time, or null if none.  This time
	 * is in milliseconds since the epoch. */
	Long getQuietExpires();
}
