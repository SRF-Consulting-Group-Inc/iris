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
 * VSL equation tests.  The expected values come from the expressions in
 * the NDDOT reference implementation (vslfunctions.java), copied as-is.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslEquationTest extends TestCase {

	/** Allowed difference from the reference (mph) */
	static private final double DELTA = 0.01;

	/** Reference vslequation1rwis */
	static private float ref1(int visibilityFt) {
		return (float) ((-3.67 + Math.sqrt(13.47 + 0.12 * visibilityFt))
			/ 0.06) + 5;
	}

	/** Reference vslequation2rwis */
	static private float ref2(int visibilityFt) {
		return (float) ((-3.67 + Math.sqrt(13.47 + 0.2 * visibilityFt))
			/ 0.1) + 5;
	}

	/** Reference vslequation3rwis */
	static private float ref3(int visibilityFt) {
		return (float) ((-3.67 + Math.sqrt(13.47 + 0.48 * visibilityFt))
			/ 0.24) + 5;
	}

	/** Reference vslequation4rwis */
	static private float ref4(float st, int aws) {
		return (float) (0.5 * (40.5 * Math.pow(2.718281828,
			(0.01 * st)))
			+ 0.3 * (-0.031 * Math.pow(aws, 1.63) + 75) + 15);
	}

	/** Reference vslequation5rwis */
	static private float ref5(float st, int aws, int rh) {
		return (float) (0.35 * (40.5 * Math.pow(2.718281828,
			(0.01 * st)))
			+ 0.35 * (-0.031 * Math.pow(aws, 1.63) + 75)
			+ 0.3 * ((-0.0009 * Math.pow(rh, 3)) + (0.199 *
			Math.pow(rh, 2)) - (15.152 * rh) + 457));
	}

	/** Reference vslequation6rwis */
	static private float ref6(float st, int aws, int rh) {
		return (float) (0.35 * (40.5 * Math.pow(2.718281828,
			(0.01 * st)))
			+ 0.15 * (-0.031 * Math.pow(aws, 1.63) + 75)
			+ 0.5 * ((-0.0009 * Math.pow(rh, 3)) + (0.199 *
			Math.pow(rh, 2)) - (15.152 * rh) + 457));
	}

	/** Create a reading */
	static private VslReading reading(float st, int vis, int aws,
		int rh)
	{
		return new VslReading(VslSurface.WET, st, vis, aws, rh);
	}

	public VslEquationTest(String name) {
		super(name);
	}

	public void testEq0() {
		VslReading r = reading(50, 2000, 10, 40);
		assertEquals(70.0, VslEquation.EQ0.calculate(r, 70), DELTA);
	}

	public void testVisibility() {
		for (int vis = 0; vis <= 1500; vis += 50) {
			VslReading r = reading(50, vis, 10, 40);
			assertEquals(ref1(vis), VslEquation.EQ1.calculate(r,
				75), DELTA);
			assertEquals(ref2(vis), VslEquation.EQ2.calculate(r,
				75), DELTA);
			assertEquals(ref3(vis), VslEquation.EQ3.calculate(r,
				75), DELTA);
		}
	}

	public void testWeather() {
		for (int st = -20; st <= 60; st += 10) {
			for (int aws = 0; aws <= 60; aws += 15) {
				for (int rh = 10; rh <= 100; rh += 15)
					checkWeather(st, aws, rh);
			}
		}
	}

	/** Check the weather equations against the reference */
	private void checkWeather(int st, int aws, int rh) {
		VslReading r = reading(st, 1000, aws, rh);
		assertEquals(ref4(st, aws), VslEquation.EQ4.calculate(r, 75),
			DELTA);
		assertEquals(ref5(st, aws, rh), VslEquation.EQ5.calculate(r,
			75), DELTA);
		assertEquals(ref6(st, aws, rh), VslEquation.EQ6.calculate(r,
			75), DELTA);
	}

	public void testNoTraffic() {
		for (VslEquation eq: VslEquation.values())
			assertEquals(50.0, eq.applyTraffic(50, null), DELTA);
	}

	public void testTrafficIgnored() {
		assertEquals(70.0, VslEquation.EQ0.applyTraffic(70, 30.0),
			DELTA);
	}

	public void testTrafficCap() {
		assertEquals(40.0, VslEquation.EQ1.applyTraffic(55, 40.0),
			DELTA);
		assertEquals(55.0, VslEquation.EQ2.applyTraffic(55, 60.0),
			DELTA);
		assertEquals(40.0, VslEquation.EQ3.applyTraffic(55, 40.0),
			DELTA);
	}

	public void testTrafficBlend() {
		// (70 * 40 + 30 * 60) / 100 = 46
		assertEquals(46.0, VslEquation.EQ4.applyTraffic(60, 40.0),
			DELTA);
		assertEquals(46.0, VslEquation.EQ5.applyTraffic(60, 40.0),
			DELTA);
		// A blend can raise the result: (70 * 70 + 30 * 50) / 100
		assertEquals(64.0, VslEquation.EQ6.applyTraffic(50, 70.0),
			DELTA);
	}
}
