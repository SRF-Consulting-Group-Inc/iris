/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2021  Minnesota Department of Transportation
 * Copyright (C) 2020  SRF Consulting Group, Inc.
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
package us.mn.state.dot.tms.server.comm.cap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import us.mn.state.dot.sched.TimeSteward;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.utils.DevelCfg;

/**
 * Common Alerting Protocol (CAP) reader.
 *
 * Reads CAP XML documents, converts alerts to JSON and stores to the database.
 *
 * @author Douglas Lau
 * @author Michael Janson
 * @author Gordon Parikh
 */
public class CapReader {

	/** Date formatter for formatting error file names */
	static private final SimpleDateFormat DT_FMT =
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	/** Most recent successful request date
	 *  (at startup, initialize to an hour ago) */
	static private Date REQ_SUCCESS = new Date(
		TimeSteward.currentTimeMillis() - 60 * 60 * 1000);

	/** Get most recent successful request date */
	static public Date getReqDate() {
		return REQ_SUCCESS;
	}

	/** Get XML save enabled setting */
	static private boolean getXmlSaveEnabled() {
		return SystemAttrEnum.CAP_XML_SAVE_ENABLE.getBoolean();
	}

	/** Alert processor */
	static private final AlertProcessor PROCESSOR = new AlertProcessor();

	/** Input stream */
	private final InputStream input;

	/** Output stream to cache copy of XML */
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

	/** Alert handler */
	private final AlertHandler handler;

	/** Create a new CAP reader */
	public CapReader(InputStream is) {
		input = is;
		handler = new AlertHandler(PROCESSOR);
	}

	/** Parse alerts */
	public void parse() throws IOException {
		boolean xmlSuccess;
		boolean jsonSuccess;
		Date now = TimeSteward.getDateInstance();
		String iStr = inputString();
		InputSource iSrc = new InputSource(new StringReader(iStr));
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser parser = spf.newSAXParser();
			parser.parse(iSrc, handler);
			xmlSuccess = true;
//			REQ_SUCCESS = now;
		}
		catch (ParserConfigurationException | SAXException e) {
			xmlSuccess = false;
//			CapPoller.slog("parse error: " + e.getMessage());		// TODO FIXME
//			saveXmlFile();
		}
		if (!xmlSuccess) {
			JSONObject jo = new JSONObject(iStr);
			JSONArray jof = jo.getJSONArray("features");
			for (int i = 0; i<jof.length(); i++) {
				JSONObject jf = jof.getJSONObject(i).getJSONObject("properties");
				PROCESSOR.processAlert(jf);
			}
			jsonSuccess = true;
		}
	}

	/** Get input string containing the XML */
	private String inputString() throws IOException {
		// make a copy of the input stream
		byte[] buf = new byte[1024];
		int len;
		while ((len = input.read(buf)) > -1)
			baos.write(buf, 0, len);
		baos.flush();
		return baos.toString();
	}

	/** Save the XML contents to a file */
	private void saveXmlFile() throws IOException {
		if (getXmlSaveEnabled()) {
			String fn = "cap_err_" + DT_FMT.format(
				TimeSteward.getDateInstance()) + ".xml";
			String fp = String.join(File.separator,
					DevelCfg.get("log.output.dir", "/var/log/iris/"), fn);
			baos.writeTo(new FileOutputStream(fp));
		}
	}
}
