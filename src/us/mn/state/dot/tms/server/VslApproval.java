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

import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.VslApprovalMode;
import us.mn.state.dot.tms.VslState;

/**
 * VSL approval rules (VSL-SPD-03, VSL-APV-01 to 09).  Decides what to do
 * with a recommended speed, given the posted speed and waiting periods.
 *
 * The checks are made in order: minimum change, then threshold changes
 * (which cancel all waiting periods and are not limited), then the quiet
 * period after a rejection, then the rate limit, then the step limit.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslApproval {

	/** Approval action */
	static public enum Action {

		/** No change is needed */
		NONE,

		/** Change is held by a waiting period */
		HOLD,

		/** Change needs operator approval */
		REQUEST,

		/** Change is posted */
		POST
	}

	/** Convert minutes to milliseconds */
	static private long minutesMs(int m) {
		return m * 60L * 1000L;
	}

	/** Get the smallest speed change which is acted on (mph) */
	static private int getMinSpeedChange() {
		return SystemAttrEnum.NDDOT_VSL_MIN_SPEED_CHANGE.getInt();
	}

	/** Get the threshold for a change (mph) */
	static private int getThreshold(boolean up) {
		return up
		      ? SystemAttrEnum.NDDOT_VSL_INCREASE_THRESHOLD.getInt()
		      : SystemAttrEnum.NDDOT_VSL_DECREASE_THRESHOLD.getInt();
	}

	/** Get the rate limit for a change (minutes) */
	static private int getRate(boolean up) {
		return up
		      ? SystemAttrEnum.NDDOT_VSL_INCREASE_RATE.getInt()
		      : SystemAttrEnum.NDDOT_VSL_DECREASE_RATE.getInt();
	}

	/** Get the step limit for a change (mph) */
	static private int getLimit(boolean up) {
		return up
		      ? SystemAttrEnum.NDDOT_VSL_INCREASE_LIMIT.getInt()
		      : SystemAttrEnum.NDDOT_VSL_DECREASE_LIMIT.getInt();
	}

	/** Get the quiet period expiration time after a rejection */
	static public long quietExpires(long now) {
		int m = SystemAttrEnum.NDDOT_VSL_QUIET_PERIOD.getInt();
		return now + minutesMs(m);
	}

	/** Check if the semi-automated review timeout has passed.
	 * @param rec_time Time of the approval request.
	 * @param now Current time.
	 * @return true if the review timeout has passed (VSL-APV-02). */
	static public boolean isReviewTimedOut(Long rec_time, long now) {
		int m = SystemAttrEnum.NDDOT_VSL_REVIEW_TIMEOUT.getInt();
		return rec_time != null && now >= rec_time + minutesMs(m);
	}

	/** Evaluate the approval rules.
	 * @param rec Recommended speed (mph).
	 * @param posted Posted speed (mph), or null if none.
	 * @param posted_time Time of the posted speed, or null.
	 * @param quiet_expires Quiet period expiration time, or null.
	 * @param mode Approval mode.
	 * @param now Current time.
	 * @return Approval decision. */
	static public VslApproval evaluate(int rec, Integer posted,
		Long posted_time, Long quiet_expires, VslApprovalMode mode,
		long now)
	{
		if (posted == null)
			return decide(mode, rec, false, "no posted speed");
		int diff = rec - posted;
		if (Math.abs(diff) < getMinSpeedChange()) {
			return new VslApproval(Action.NONE, rec, VslState.NONE,
				false, "within minimum change of " + posted);
		}
		boolean up = diff > 0;
		if (Math.abs(diff) >= getThreshold(up))
			return decide(mode, rec, true, "threshold change");
		int limit = getLimit(up);
		int s = up
		      ? Math.min(rec, posted + limit)
		      : Math.max(rec, posted - limit);
		if (quiet_expires != null && now < quiet_expires) {
			return new VslApproval(Action.HOLD, s,
				VslState.REJECTED, false, "quiet period");
		}
		if (posted_time != null &&
		    now < posted_time + minutesMs(getRate(up)))
		{
			return new VslApproval(Action.HOLD, s, VslState.HELD,
				false, "rate limit");
		}
		return decide(mode, s, false, (s != rec)
			? "limited to " + s
			: "change from " + posted);
	}

	/** Decide whether a change needs approval (VSL-APV-01) */
	static private VslApproval decide(VslApprovalMode mode, int s,
		boolean urgent, String why)
	{
		return (mode == VslApprovalMode.AUTOMATED)
		      ? new VslApproval(Action.POST, s, VslState.NONE, urgent,
		                        why)
		      : new VslApproval(Action.REQUEST, s, VslState.PENDING,
		                        urgent, why);
	}

	/** Approval action */
	public final Action action;

	/** Speed to post, request or hold (mph) */
	public final int speed;

	/** Recommendation state */
	public final VslState state;

	/** Flag set for a threshold change, which cancels waiting periods
	 * (VSL-APV-08) */
	public final boolean urgent;

	/** Reason for the decision, for the VSL log */
	public final String reason;

	/** Create an approval decision */
	private VslApproval(Action a, int s, VslState st, boolean u,
		String r)
	{
		action = a;
		speed = s;
		state = st;
		urgent = u;
		reason = r;
	}

	/** Get the decision as a string */
	@Override
	public String toString() {
		return action + " " + speed + " (" + reason + ")";
	}
}
