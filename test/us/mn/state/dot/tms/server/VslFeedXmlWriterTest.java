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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import us.mn.state.dot.tms.server.VslNotice.Type;

/**
 * VSL feed RSS writer tests.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslFeedXmlWriterTest extends TestCase {

	/** Time of the first notification */
	static private final long TIME = 1_800_000_000_000L;

	/** Get the text of the first child element with a tag */
	static private String text(Element e, String tag) {
		return e.getElementsByTagName(tag).item(0).getTextContent();
	}

	/** Write a feed and parse it */
	static private Document writeFeed(VslFeedImpl f,
		ArrayList<VslNotice> notices) throws Exception
	{
		StringWriter sw = new StringWriter();
		new VslFeedXmlWriter(f, notices).write(sw);
		byte[] b = sw.toString().getBytes(StandardCharsets.UTF_8);
		return DocumentBuilderFactory.newInstance()
			.newDocumentBuilder()
			.parse(new ByteArrayInputStream(b));
	}

	public VslFeedXmlWriterTest(String name) {
		super(name);
	}

	public void testFileName() {
		assertEquals("vsl_D1.xml",
			VslFeedXmlWriter.fileName(new VslFeedImpl("D1")));
	}

	public void testFormatDate() {
		assertTrue(VslFeedXmlWriter.formatDate(TIME).matches(
			"[A-Z][a-z]{2}, \\d{2} [A-Z][a-z]{2} \\d{4} " +
			"\\d{2}:\\d{2}:\\d{2} [+-]\\d{4}"));
	}

	public void testEmpty() throws Exception {
		Document doc = writeFeed(new VslFeedImpl("D1"),
			new ArrayList<VslNotice>());
		Element rss = doc.getDocumentElement();
		assertEquals("rss", rss.getTagName());
		assertEquals("2.0", rss.getAttribute("version"));
		Element ch = (Element) rss.getElementsByTagName("channel")
			.item(0);
		assertEquals("D1", text(ch, "title"));
		assertEquals(0, ch.getElementsByTagName("item").getLength());
	}

	public void testItems() throws Exception {
		VslFeedImpl f = new VslFeedImpl("D1");
		f.setTitle("District 1 <VSL> & signs");
		ArrayList<VslNotice> notices = new ArrayList<VslNotice>();
		notices.add(new VslNotice(Type.POSTED, TIME + 60000, "V1",
			"I-94 & Exit 1", "40 35")
			.add("Posted speed", "35 mph"));
		notices.add(new VslNotice(Type.FAULT, TIME, "V1",
			"I-94 & Exit 1", "Sensor error null")
			.add("Sensor error", "no usable weather sensor"));
		notices.add(new VslNotice(Type.FAULT, TIME, "V1",
			"I-94 & Exit 1", "Communication error 35")
			.add("Communication error", "sign offline"));
		Document doc = writeFeed(f, notices);
		Element ch = (Element) doc.getElementsByTagName("channel")
			.item(0);
		assertEquals("District 1 <VSL> & signs", text(ch, "title"));
		NodeList items = ch.getElementsByTagName("item");
		assertEquals(3, items.getLength());
		Element it = (Element) items.item(0);
		assertEquals("VSL speed changed: V1", text(it, "title"));
		String desc = text(it, "description");
		assertTrue(desc.startsWith("Time: "));
		assertTrue(desc.contains("<br>Sign: V1<br>"));
		assertTrue(desc.contains("Location: I-94 &amp; Exit 1<br>"));
		assertTrue(desc.endsWith("<br>Posted speed: 35 mph"));
		assertEquals(VslFeedXmlWriter.formatDate(TIME + 60000),
			text(it, "pubDate"));
		assertEquals("V1 POSTED 40 35 " + (TIME + 60000),
			text(it, "guid"));
		it = (Element) items.item(1);
		assertEquals("VSL fault: V1", text(it, "title"));
		assertEquals("V1 FAULT Sensor error null " + TIME,
			text(it, "guid"));
		it = (Element) items.item(2);
		assertEquals("V1 FAULT Communication error 35 " + TIME,
			text(it, "guid"));
	}
}
