/*
 * IRIS -- Intelligent Roadway Information System
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
package us.mn.state.dot.tms.server.comm.ipaws;

// TODO temp for testing
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;

import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.comm.ControllerProperty;
// TODO implement (then import) CAP message and alert bucket
import us.mn.state.dot.tms.server.FeedBucket;
import us.mn.state.dot.tms.server.FeedMsg;

/**
 * Container for IPAWS alert property.
 *
 * @author Gordon Parikh
 * @author Douglas Lau
 */
public class IpawsProperty extends ControllerProperty {
	
	/** Feed name */
	private final String alertFeed;

	/** Create a new IPAWS property */
	public IpawsProperty(String afd) {
		alertFeed = afd;
	}

	/** Decode a QUERY response */
	@Override
	public void decodeQuery(ControllerImpl c, InputStream is)
		throws IOException
	{
		// TODO read and parse alerts and store in bucket
		
		// TODO temp/test code to write output to file
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
		LocalDateTime now = LocalDateTime.now();
		String fname = String.format("/var/log/iris/ipaws_test_%s.xml", dtf.format(now));
		File xmlFile = new File(fname);
		java.nio.file.Files.copy(is, xmlFile.toPath(),
				StandardCopyOption.REPLACE_EXISTING);
	}

	/** Get a string representation of the property */
	@Override
	public String toString() {
		return "alertFeed " + alertFeed;
	}
}
