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
import static us.mn.state.dot.tms.server.VslSurface.SLICK_IN_SPOTS;
import static us.mn.state.dot.tms.server.VslSurface.WET;

/**
 * VSL decision tree tests.  Each test follows one path through the
 * diagram, including the edges of each comparison.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslDecisionTreeTest extends TestCase {

	/** Choose an equation */
	static private VslDecisionTree.Choice choose(VslSurface s, int st,
		int vis, int rh)
	{
		return VslDecisionTree.choose(new VslReading(s, st, vis, 10,
			rh));
	}

	/** Check a choice */
	private void check(VslEquation eq, Integer mx,
		VslDecisionTree.Choice c)
	{
		assertEquals(eq, c.equation);
		assertEquals(mx, c.max_speed);
	}

	public VslDecisionTreeTest(String name) {
		super(name);
	}

	public void testDryClear() {
		check(VslEquation.EQ0, null, choose(DRY, 50, 346, 94));
	}

	public void testDryLowVisibility() {
		check(VslEquation.EQ1, null, choose(DRY, 50, 345, 94));
	}

	public void testDryHumidIsWet() {
		check(VslEquation.EQ0, null, choose(DRY, 50, 1000, 95));
		check(VslEquation.EQ2, null, choose(DRY, 50, 400, 95));
	}

	public void testDryHumidFreezingIsSlick() {
		check(VslEquation.EQ6, 65, choose(DRY, 30, 1000, 95));
	}

	public void testWetFreezingIsSlick() {
		check(VslEquation.EQ3, 65, choose(WET, 32, 300, 50));
		check(VslEquation.EQ4, 65, choose(WET, 32, 1000, 50));
	}

	public void testWetLowVisibility() {
		check(VslEquation.EQ2, null, choose(WET, 33, 420, 50));
	}

	public void testWetClear() {
		check(VslEquation.EQ0, null, choose(WET, 37, 421, 50));
	}

	public void testWetCold() {
		check(VslEquation.EQ4, null, choose(WET, 36, 421, 54));
		check(VslEquation.EQ5, null, choose(WET, 33, 421, 55));
		check(VslEquation.EQ5, null, choose(WET, 33, 421, 100));
	}

	public void testSlickLowVisibility() {
		check(VslEquation.EQ3, 65, choose(SLICK, 20, 520, 50));
		check(VslEquation.EQ3, 65, choose(SLICK_IN_SPOTS, 40, 0, 50));
	}

	public void testSlickHumidity() {
		check(VslEquation.EQ4, 65, choose(SLICK, 20, 521, 54));
		check(VslEquation.EQ5, 65, choose(SLICK, 20, 521, 55));
		check(VslEquation.EQ5, 65, choose(SLICK_IN_SPOTS, 20, 521, 89));
		check(VslEquation.EQ6, 65, choose(SLICK, 20, 521, 90));
	}
}
