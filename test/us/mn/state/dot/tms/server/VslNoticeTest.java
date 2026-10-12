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
import java.util.List;
import junit.framework.TestCase;
import us.mn.state.dot.tms.server.VslNotice.Type;

/**
 * VSL notification tests.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslNoticeTest extends TestCase {

	/** One minute (ms) */
	static private final long MIN = 60 * 1000;

	/** Get a time on October 12, 2026 */
	static private long time(int hour, int minute) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2026, Calendar.OCTOBER, 12, hour, minute);
		return cal.getTimeInMillis();
	}

	/** Create a notification at 2:35 p.m. */
	static private VslNotice notice(Type t, String k) {
		return new VslNotice(t, time(14, 35), "V1", "I-94 @ Exit 1", k);
	}

	public VslNoticeTest(String name) {
		super(name);
	}

	public void testFormatTime() {
		assertEquals("October 12, 2026, 2:35 p.m.",
			VslNotice.formatTime(time(14, 35)));
		assertEquals("October 12, 2026, 2 p.m.",
			VslNotice.formatTime(time(14, 0)));
		assertEquals("October 12, 2026, 9:05 a.m.",
			VslNotice.formatTime(time(9, 5)));
		assertEquals("October 12, 2026, 10 a.m.",
			VslNotice.formatTime(time(10, 0)));
	}

	public void testFormatSpeed() {
		assertEquals("55 mph", VslNotice.formatSpeed(55));
		assertEquals("none", VslNotice.formatSpeed(null));
	}

	public void testLines() {
		VslNotice n = notice(Type.PENDING, "55 65")
			.add("Posted speed", "65 mph");
		assertEquals("VSL approval requested: V1", n.getTitle());
		List<String> lines = n.getLines();
		assertEquals(4, lines.size());
		assertEquals("Time: October 12, 2026, 2:35 p.m.", lines.get(0));
		assertEquals("Sign: V1", lines.get(1));
		assertEquals("Location: I-94 @ Exit 1", lines.get(2));
		assertEquals("Posted speed: 65 mph", lines.get(3));
	}

	public void testReading() {
		VslReading r = new VslReading(VslSurface.SLICK_IN_SPOTS, 28.4,
			1500.2, 20.6, 80);
		List<String> lines = notice(Type.PENDING, "")
			.addReading("RWIS1", r).getLines();
		assertEquals(8, lines.size());
		assertEquals("Weather sensor: RWIS1", lines.get(3));
		assertEquals("Road surface: slick in spots", lines.get(4));
		assertEquals("Surface temperature: 28 F", lines.get(5));
		assertEquals("Visibility: 1500 ft", lines.get(6));
		assertEquals("Wind speed: 21 mph", lines.get(7));
		assertEquals(3, notice(Type.PENDING, "")
			.addReading("RWIS1", null).getLines().size());
	}

	public void testKey() {
		assertEquals(notice(Type.FAULT, "a").key,
			notice(Type.FAULT, "a").key);
		assertFalse(notice(Type.FAULT, "a").key.equals(
			notice(Type.FAULT, "b").key));
		assertFalse(notice(Type.FAULT, "a").key.equals(
			notice(Type.FAULT_CLEARED, "a").key));
	}

	public void testRepeat() {
		VslNotice n = notice(Type.FAULT, "a");
		long repeat = 30 * MIN;
		assertFalse(n.isRepeat(null, repeat));
		assertTrue(n.isRepeat(n.time - 10 * MIN, repeat));
		assertTrue(n.isRepeat(n.time - 29 * MIN, repeat));
		assertFalse(n.isRepeat(n.time - 30 * MIN, repeat));
		assertFalse(n.isRepeat(n.time - 10 * MIN, 0));
		assertTrue(notice(Type.PENDING, "a")
			.isRepeat(n.time - MIN, repeat));
		assertTrue(notice(Type.FAULT_CLEARED, "a")
			.isRepeat(n.time - MIN, repeat));
	}

	public void testChangesAlwaysSent() {
		long repeat = 30 * MIN;
		VslNotice n = notice(Type.POSTED, "65 55");
		assertFalse(n.isRepeat(n.time - MIN, repeat));
		n = notice(Type.OVERRIDE, "jdoe 45");
		assertFalse(n.isRepeat(n.time - MIN, repeat));
		n = notice(Type.OVERRIDE_CLEARED, "");
		assertFalse(n.isRepeat(n.time - MIN, repeat));
	}
}
