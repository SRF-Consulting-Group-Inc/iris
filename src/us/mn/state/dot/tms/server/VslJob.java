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

import java.util.Calendar;
import java.util.Iterator;
import us.mn.state.dot.sched.Job;
import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.VslSign;
import us.mn.state.dot.tms.VslSignHelper;

/**
 * Job to calculate recommended speeds of NDDOT VSL signs.  It runs once a
 * minute, after the station data job, and calculates when a cycle is due
 * (VSL-SPD-01, VSL-SPD-02).
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslJob extends Job {

	/** Seconds to offset each run from the start of the minute.  This is
	 * after the station data job (at 26 seconds), so the latest traffic
	 * data is used. */
	static private final int OFFSET_SECS = 35;

	/** Check if a cycle is due.
	 * @param min Minute of the day.
	 * @param rate Cycle rate (minutes).
	 * @return true if a cycle is due. */
	static public boolean isCycleDue(int min, int rate) {
		return min % Math.max(rate, 1) == 0;
	}

	/** Create a new VSL job */
	public VslJob() {
		super(Calendar.MINUTE, 1, Calendar.SECOND, OFFSET_SECS);
	}

	/** Perform job */
	@Override
	public void perform() {
		if (VslSignHelper.isEnabled() && isCycleDue(getMinuteOfDay(),
		    SystemAttrEnum.NDDOT_VSL_CYCLE_RATE.getInt()))
			calculateAll();
	}

	/** Get the current minute of the day */
	private int getMinuteOfDay() {
		Calendar cal = TimeSteward.getCalendarInstance();
		return cal.get(Calendar.HOUR_OF_DAY) * 60 +
			cal.get(Calendar.MINUTE);
	}

	/** Calculate recommended speeds of all VSL signs */
	private void calculateAll() {
		Iterator<VslSign> it = VslSignHelper.iterator();
		while (it.hasNext()) {
			VslSign vs = it.next();
			if (vs instanceof VslSignImpl)
				((VslSignImpl) vs).calculate();
		}
	}
}
