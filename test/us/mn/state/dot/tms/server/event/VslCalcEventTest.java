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
package us.mn.state.dot.tms.server.event;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import us.mn.state.dot.tms.EventType;
import us.mn.state.dot.tms.server.Storable;

/**
 * VSL calculation event tests.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslCalcEventTest extends TestCase {

	public VslCalcEventTest(String name) {
		super(name);
	}

	public void testCalculation() {
		VslCalcEvent ev = new VslCalcEvent("V1", 45, 75, 65);
		ev.setResult(1000L, 62.5, "ESS1", 60, null);
		ev.setDecision("REQUEST", 60, "change from 65");
		assertEquals("event.vsl_calc", ev.getTable());
		Map<String, Object> c = ev.getColumns();
		assertEquals("V1", c.get("vsl_sign"));
		assertEquals(new Timestamp(ev.event_date.getTime()),
			c.get("event_date"));
		assertEquals(EventType.VSL_CALCULATION.id, c.get("event_desc"));
		assertEquals(45, c.get("min_speed"));
		assertEquals(75, c.get("max_speed"));
		assertEquals(new Timestamp(1000L), c.get("detector_time"));
		assertEquals(62.5, c.get("traffic_speed"));
		assertEquals("ESS1", c.get("weather_sensor"));
		assertEquals(60, c.get("rec_speed"));
		assertNull(c.get("fault"));
		assertEquals(65, c.get("posted_speed"));
		assertEquals("REQUEST", c.get("action"));
		assertEquals(60, c.get("action_speed"));
		assertEquals("change from 65", c.get("reason"));
	}

	public void testNoSpeed() {
		VslCalcEvent ev = new VslCalcEvent("V1", 45, 75, null);
		ev.setResult(0L, null, null, null, "no usable weather sensor");
		Map<String, Object> c = ev.getColumns();
		assertNull(c.get("detector_time"));
		assertNull(c.get("traffic_speed"));
		assertNull(c.get("weather_sensor"));
		assertNull(c.get("rec_speed"));
		assertEquals("no usable weather sensor", c.get("fault"));
		assertNull(c.get("posted_speed"));
		assertNull(c.get("action"));
		assertNull(c.get("action_speed"));
		assertNull(c.get("reason"));
		assertTrue(ev.getRows().isEmpty());
	}

	public void testRows() {
		VslCalcEvent ev = new VslCalcEvent("V1", 45, 75, 65);
		ev.addStation("ESS1", "WET", 30.2, 1200.5, 18.6, 85, "EQ2",
			65, 61.3);
		ev.addStation("ESS2", "no visibility");
		ev.addDetector("D1", 63.5f, null);
		ev.addDetector("D2", null, "not sampling");
		List<? extends Storable> rows = ev.getRows();
		assertEquals(4, rows.size());
		Timestamp t = new Timestamp(ev.event_date.getTime());
		for (Storable r: rows) {
			Map<String, Object> c = r.getColumns();
			assertEquals("V1", c.get("vsl_sign"));
			assertEquals(t, c.get("event_date"));
		}

		Map<String, Object> s1 = rows.get(0).getColumns();
		assertEquals("event.vsl_calc_station", rows.get(0).getTable());
		assertEquals("ESS1", s1.get("weather_sensor"));
		assertNull(s1.get("problem"));
		assertEquals("WET", s1.get("surface"));
		assertEquals(30.2, s1.get("surf_temp_f"));
		assertEquals(1200.5, s1.get("visibility_ft"));
		assertEquals(18.6, s1.get("wind_speed_mph"));
		assertEquals(85.0, s1.get("humidity_pct"));
		assertEquals("EQ2", s1.get("equation"));
		assertEquals(65, s1.get("equation_max"));
		assertEquals(61.3, s1.get("candidate"));

		Map<String, Object> s2 = rows.get(1).getColumns();
		assertEquals("ESS2", s2.get("weather_sensor"));
		assertEquals("no visibility", s2.get("problem"));
		assertNull(s2.get("surface"));
		assertNull(s2.get("equation"));
		assertNull(s2.get("candidate"));

		Map<String, Object> d1 = rows.get(2).getColumns();
		assertEquals("event.vsl_calc_detector", rows.get(2).getTable());
		assertEquals("D1", d1.get("detector"));
		assertEquals(63.5f, d1.get("speed"));
		assertNull(d1.get("problem"));

		Map<String, Object> d2 = rows.get(3).getColumns();
		assertEquals("D2", d2.get("detector"));
		assertNull(d2.get("speed"));
		assertEquals("not sampling", d2.get("problem"));
	}
}
