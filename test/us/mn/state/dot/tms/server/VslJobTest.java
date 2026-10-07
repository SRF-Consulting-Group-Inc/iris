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

/**
 * VSL job tests.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslJobTest extends TestCase {

	public VslJobTest(String name) {
		super(name);
	}

	public void testEveryMinute() {
		for (int min = 0; min < 1440; min++)
			assertTrue(VslJob.isCycleDue(min, 1));
	}

	public void testEveryFiveMinutes() {
		assertTrue(VslJob.isCycleDue(0, 5));
		assertFalse(VslJob.isCycleDue(7, 5));
		assertTrue(VslJob.isCycleDue(10, 5));
		assertTrue(VslJob.isCycleDue(1435, 5));
	}

	public void testBadRate() {
		assertTrue(VslJob.isCycleDue(7, 0));
	}
}
