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
package us.mn.state.dot.tms;

import junit.framework.TestCase;

/**
 * VSL sign helper tests.
 *
 * @author John L. Stanley - SRF Consulting
 */
public class VslSignHelperTest extends TestCase {

	/** Cyan, 0, 192, 224 */
	static private final Integer CYAN = 0x00C0E0;

	public VslSignHelperTest(String name) {
		super(name);
	}

	/** Check that a color string is invalid */
	private void assertInvalid(String c) {
		assertNull(c, VslSignHelper.parseColor(c));
	}

	public void testRgb() {
		assertEquals(CYAN, VslSignHelper.parseColor("0,192,224"));
		assertEquals(CYAN, VslSignHelper.parseColor("(0,192,224)"));
		assertEquals(CYAN, VslSignHelper.parseColor(
			" ( 0 , 192 , 224 ) "));
		assertEquals(Integer.valueOf(0x000000),
			VslSignHelper.parseColor("0,0,0"));
		assertEquals(Integer.valueOf(0xFFFFFF),
			VslSignHelper.parseColor("255,255,255"));
	}

	public void testHex() {
		assertEquals(CYAN, VslSignHelper.parseColor("#00C0E0"));
		assertEquals(CYAN, VslSignHelper.parseColor("#00c0e0"));
		assertEquals(CYAN, VslSignHelper.parseColor(" #00C0E0 "));
	}

	public void testBlank() {
		assertInvalid(null);
		assertInvalid("");
		assertInvalid("   ");
	}

	public void testInvalidRgb() {
		assertInvalid("0,192");
		assertInvalid("0,192,224,5");
		assertInvalid("256,0,0");
		assertInvalid("-1,0,0");
		assertInvalid("a,b,c");
		assertInvalid("0,,224");
		assertInvalid("(0,192,224");
		assertInvalid("0,192,224)");
	}

	public void testInvalidHex() {
		assertInvalid("#0CE");
		assertInvalid("#00C0E");
		assertInvalid("#00C0E00");
		assertInvalid("#GG0000");
		assertInvalid("00C0E0");
	}
}
