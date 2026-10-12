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

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.VslFeed;

/**
 * This class writes a VSL feed to an RSS 2.0 file, for GovDelivery
 * (VSL-NTF-01).
 *
 * TODO: confirm with NDDOT / GovDelivery:
 *       - the channel link (RSS 2.0 requires one),
 *       - whether items need a link or category,
 *       - how often GovDelivery reads the feed.
 *
 * @author Coby Christiansen - SRF Consulting
 */
public class VslFeedXmlWriter extends XmlWriter {

	/** Get the file name of a VSL feed */
	static public String fileName(VslFeed f) {
		return "vsl_" + f.getName() + ".xml";
	}

	/** Format a date for RSS (RFC 822) */
	static public String formatDate(long t) {
		return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z",
			Locale.US).format(new Date(t));
	}

	/** Write an element with a text value */
	static private void writeElement(Writer w, String e, String v)
		throws IOException
	{
		w.write("<" + e + ">" + validateElementValue(v) + "</" + e +
			">\n");
	}

	/** Get the description of a notification, as HTML lines */
	static private String htmlLines(VslNotice n) {
		ArrayList<String> lines = new ArrayList<String>();
		for (String ln: n.getLines())
			lines.add(validateElementValue(ln));
		return String.join("<br>", lines);
	}

	/** VSL feed */
	private final VslFeed feed;

	/** Notifications to write, newest first */
	private final List<VslNotice> notices;

	/** Create a new VSL feed writer.
	 * @param f VSL feed.
	 * @param n Notifications, newest first. */
	public VslFeedXmlWriter(VslFeed f, List<VslNotice> n) {
		super(fileName(f), false);
		feed = f;
		notices = n;
	}

	/** Write the VSL feed */
	@Override
	protected void write(Writer w) throws IOException {
		w.write(XML_DECLARATION);
		w.write("<rss version='2.0'>\n");
		w.write("<channel>\n");
		writeElement(w, "title", feed.getTitle());
		writeElement(w, "link", "");
		writeElement(w, "description",
			"Variable speed limit notifications");
		writeElement(w, "lastBuildDate", formatDate(
			TimeSteward.currentTimeMillis()));
		for (VslNotice n: notices)
			writeItem(w, n);
		w.write("</channel>\n");
		w.write("</rss>\n");
	}

	/** Write one notification as an item.  The guid has the condition
	 * as well as the time, since a sensor fault and a communication
	 * fault can happen in the same millisecond. */
	private void writeItem(Writer w, VslNotice n) throws IOException {
		w.write("<item>\n");
		writeElement(w, "title", n.getTitle());
		writeElement(w, "description", htmlLines(n));
		writeElement(w, "pubDate", formatDate(n.time));
		w.write("<guid isPermaLink='false'>" + validateElementValue(
			n.sign + " " + n.key + " " + n.time) + "</guid>\n");
		w.write("</item>\n");
	}
}
