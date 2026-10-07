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
import static us.mn.state.dot.tms.server.VslSurface.DRY;
import static us.mn.state.dot.tms.server.VslSurface.SLICK;
import static us.mn.state.dot.tms.server.VslSurface.WET;

/**
 * VSL calculator tests.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslCalculatorTest extends TestCase {

	/** Allowed difference (mph) */
	static private final double DELTA = 0.01;

	/** Calculate a candidate speed with a 75 mph sign maximum */
	static private double candidate(VslSurface s, int st, int vis,
		int aws, int rh, Double traffic)
	{
		return VslCalculator.candidateSpeed(new VslReading(s, st, vis,
			aws, rh), 75, traffic);
	}

	public VslCalculatorTest(String name) {
		super(name);
	}

	public void testClearIsMaximum() {
		assertEquals(75.0, candidate(DRY, 50, 2000, 10, 40, null),
			DELTA);
		// Traffic speed is not used in clear conditions
		assertEquals(75.0, candidate(DRY, 50, 2000, 10, 40, 30.0),
			DELTA);
	}

	public void testTrafficCap() {
		// EQ1 at 300 feet is about 61 mph
		double s = candidate(DRY, 50, 300, 10, 40, null);
		assertEquals(61.06, s, DELTA);
		assertEquals(40.0, candidate(DRY, 50, 300, 10, 40, 40.0),
			DELTA);
	}

	public void testSlickMaximum() {
		// EQ4 blends up past 65 with fast traffic, but slick
		// pavement keeps it at 65
		double s = candidate(SLICK, 20, 1000, 0, 40, 80.0);
		assertEquals(65.0, s, DELTA);
	}

	public void testSignMaximum() {
		double s = VslCalculator.candidateSpeed(new VslReading(WET,
			50, 2000, 10, 40), 55, null);
		assertEquals(55.0, s, DELTA);
	}

	public void testRoundDown() {
		assertEquals(45, VslCalculator.recommendedSpeed(49.9, 25, 75,
			5));
		assertEquals(50, VslCalculator.recommendedSpeed(50.0, 25, 75,
			5));
		assertEquals(47, VslCalculator.recommendedSpeed(47.9, 25, 75,
			1));
	}

	public void testKeepWithinSpeeds() {
		assertEquals(50, VslCalculator.recommendedSpeed(47.9, 50, 75,
			5));
		assertEquals(65, VslCalculator.recommendedSpeed(80.0, 25, 65,
			5));
	}

	public void testBadRounding() {
		assertEquals(47, VslCalculator.recommendedSpeed(47.9, 25, 75,
			0));
	}
}
