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
package us.mn.state.dot.tms.server;

import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import us.mn.state.dot.sched.Job;
import us.mn.state.dot.tms.VslFeed;
import us.mn.state.dot.tms.VslFeedHelper;
import us.mn.state.dot.tms.VslSignHelper;

/**
 * Job to write the VSL feed files.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslFeedXmlJob extends Job {

	/** Seconds to offset each run from the start of the minute.  This is
	 * after the VSL job (at 35 seconds), so new notifications are
	 * written right away. */
	static private final int OFFSET_SECS = 40;

	/** Create a new VSL feed job */
	public VslFeedXmlJob() {
		super(Calendar.MINUTE, 1, Calendar.SECOND, OFFSET_SECS);
	}

	/** Perform the VSL feed job */
	@Override
	public void perform() throws IOException {
		if (!VslSignHelper.isEnabled())
			return;
		Iterator<VslFeed> it = VslFeedHelper.iterator();
		while (it.hasNext()) {
			VslFeed f = it.next();
			if (f instanceof VslFeedImpl) {
				VslFeedImpl fi = (VslFeedImpl) f;
				new VslFeedXmlWriter(fi, fi.getNotices())
					.write();
			}
		}
	}
}
