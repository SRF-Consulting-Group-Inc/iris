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
 * VSL surface category tests.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslSurfaceTest extends TestCase {

	public VslSurfaceTest(String name) {
		super(name);
	}

	public void testDry() {
		assertEquals(VslSurface.DRY, VslSurface.fromStatus("dry"));
	}

	public void testWet() {
		assertEquals(VslSurface.WET, VslSurface.fromStatus(
			"traceMoisture"));
		assertEquals(VslSurface.WET, VslSurface.fromStatus("wet"));
		assertEquals(VslSurface.WET, VslSurface.fromStatus(
			"chemicallyWet"));
		assertEquals(VslSurface.WET, VslSurface.fromStatus("dew"));
		assertEquals(VslSurface.WET, VslSurface.fromStatus(
			"absorption"));
		assertEquals(VslSurface.WET, VslSurface.fromStatus(
			"absorptionAtDewpoint"));
	}

	public void testSlickInSpots() {
		assertEquals(VslSurface.SLICK_IN_SPOTS, VslSurface.fromStatus(
			"iceWatch"));
		assertEquals(VslSurface.SLICK_IN_SPOTS, VslSurface.fromStatus(
			"snowWatch"));
		assertEquals(VslSurface.SLICK_IN_SPOTS, VslSurface.fromStatus(
			"frost"));
	}

	public void testSlick() {
		assertEquals(VslSurface.SLICK, VslSurface.fromStatus(
			"iceWarning"));
		assertEquals(VslSurface.SLICK, VslSurface.fromStatus(
			"snowWarning"));
	}

	public void testNotUsable() {
		assertNull(VslSurface.fromStatus(null));
		assertNull(VslSurface.fromStatus("undefined"));
		assertNull(VslSurface.fromStatus("other"));
		assertNull(VslSurface.fromStatus("error"));
		assertNull(VslSurface.fromStatus("Dry"));
	}
}
