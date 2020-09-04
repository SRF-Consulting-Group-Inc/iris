/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2020  SRF Consulting Group, Inc.
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

import us.mn.state.dot.tms.utils.UniqueNameCreator;

/**
 * Helper class for Push Notifications. Used on the client and server.
 *
 * @author Gordon Parikh
 */
public class PushNotificationHelper extends BaseHelper {

	/** Don't instantiate */
	private PushNotificationHelper() {
		assert false;
	}

	/** Name creator */
	static UniqueNameCreator UNC;
	static {
		UNC = new UniqueNameCreator("push_notif_%d", (n)->lookup(n));
		UNC.setMaxLength(30);
	}

	/** Create a unique PushNotification record name */
	static public String createUniqueName() {
		return UNC.createUniqueName();
	}

	/** Lookup the PushNotification with the specified name */
	static public PushNotification lookup(String name) {
		return (PushNotification) namespace.lookupObject(
				PushNotification.SONAR_TYPE, name);
	}
	
	/** Get an PushNotification object iterator */
	static public Iterator<PushNotification> iterator() {
		return new IteratorWrapper<PushNotification>(namespace.iterator(
				PushNotification.SONAR_TYPE));
	}
}