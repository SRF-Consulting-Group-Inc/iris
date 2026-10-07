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

import junit.framework.TestCase;
import us.mn.state.dot.tms.VslApprovalMode;
import static us.mn.state.dot.tms.VslApprovalMode.AUTOMATED;
import static us.mn.state.dot.tms.VslApprovalMode.MANUAL;
import static us.mn.state.dot.tms.VslApprovalMode.SEMI_AUTOMATED;
import us.mn.state.dot.tms.VslState;
import us.mn.state.dot.tms.server.VslApproval.Action;

/**
 * VSL approval rule tests.  With no database, the system attributes have
 * their default values: minimum change 5, increase limit 10, decrease
 * limit 15, both thresholds 15, increase rate 2 minutes, decrease rate 1
 * minute, quiet period 10 minutes and review timeout 2 minutes.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslApprovalTest extends TestCase {

	/** Current time for the tests */
	static private final long NOW = 1_800_000_000_000L;

	/** One minute (ms) */
	static private final long MIN = 60 * 1000;

	/** Evaluate with no waiting periods */
	static private VslApproval eval(int rec, Integer posted,
		VslApprovalMode mode)
	{
		return VslApproval.evaluate(rec, posted, null, null, mode, NOW);
	}

	/** Check a decision */
	private void check(Action a, int s, VslState st, VslApproval ap) {
		assertEquals(a, ap.action);
		assertEquals(s, ap.speed);
		assertEquals(st, ap.state);
	}

	public VslApprovalTest(String name) {
		super(name);
	}

	public void testNothingPosted() {
		check(Action.POST, 45, VslState.NONE, eval(45, null,
			AUTOMATED));
		check(Action.REQUEST, 45, VslState.PENDING, eval(45, null,
			MANUAL));
	}

	public void testMinimumChange() {
		check(Action.NONE, 57, VslState.NONE, eval(57, 55, AUTOMATED));
		check(Action.NONE, 51, VslState.NONE, eval(51, 55, AUTOMATED));
		check(Action.POST, 60, VslState.NONE, eval(60, 55, AUTOMATED));
	}

	public void testApprovalModes() {
		check(Action.POST, 45, VslState.NONE, eval(45, 55, AUTOMATED));
		check(Action.REQUEST, 45, VslState.PENDING, eval(45, 55,
			MANUAL));
		check(Action.REQUEST, 45, VslState.PENDING, eval(45, 55,
			SEMI_AUTOMATED));
	}

	public void testIncreaseLimit() {
		// 40 to 52 is 12: under the threshold, so limited to 50
		check(Action.POST, 50, VslState.NONE, eval(52, 40, AUTOMATED));
	}

	public void testThresholdNotLimited() {
		// 65 to 40 is 25: a threshold change goes all the way
		VslApproval ap = eval(40, 65, AUTOMATED);
		check(Action.POST, 40, VslState.NONE, ap);
		assertTrue(ap.urgent);
		assertFalse(eval(45, 55, AUTOMATED).urgent);
	}

	public void testQuietPeriod() {
		VslApproval ap = VslApproval.evaluate(45, 55, null,
			NOW + 5 * MIN, AUTOMATED, NOW);
		check(Action.HOLD, 45, VslState.REJECTED, ap);
		ap = VslApproval.evaluate(45, 55, null, NOW - MIN, AUTOMATED,
			NOW);
		check(Action.POST, 45, VslState.NONE, ap);
	}

	public void testThresholdCancelsQuietPeriod() {
		VslApproval ap = VslApproval.evaluate(40, 55, null,
			NOW + 5 * MIN, AUTOMATED, NOW);
		check(Action.POST, 40, VslState.NONE, ap);
		assertTrue(ap.urgent);
	}

	public void testRateLimit() {
		// Decrease rate is 1 minute
		VslApproval ap = VslApproval.evaluate(45, 55, NOW - 30000L,
			null, AUTOMATED, NOW);
		check(Action.HOLD, 45, VslState.HELD, ap);
		ap = VslApproval.evaluate(45, 55, NOW - MIN, null, AUTOMATED,
			NOW);
		check(Action.POST, 45, VslState.NONE, ap);
	}

	public void testRateByDirection() {
		// 90 seconds: past the decrease rate, not the increase rate
		long pt = NOW - 90000L;
		check(Action.POST, 45, VslState.NONE, VslApproval.evaluate(45,
			55, pt, null, AUTOMATED, NOW));
		check(Action.HOLD, 65, VslState.HELD, VslApproval.evaluate(65,
			55, pt, null, AUTOMATED, NOW));
	}

	public void testThresholdCancelsRateLimit() {
		VslApproval ap = VslApproval.evaluate(70, 55, NOW - 30000L,
			null, AUTOMATED, NOW);
		check(Action.POST, 70, VslState.NONE, ap);
	}

	public void testReviewTimeout() {
		assertFalse(VslApproval.isReviewTimedOut(null, NOW));
		assertFalse(VslApproval.isReviewTimedOut(NOW - MIN, NOW));
		assertTrue(VslApproval.isReviewTimedOut(NOW - 2 * MIN, NOW));
	}

	public void testQuietExpires() {
		assertEquals(NOW + 10 * MIN, VslApproval.quietExpires(NOW));
	}
}
