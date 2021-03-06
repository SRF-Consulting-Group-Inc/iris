/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2015-2020  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.e6;

import us.mn.state.dot.sched.DebugLog;
import us.mn.state.dot.tms.CommLink;
import us.mn.state.dot.tms.DeviceRequest;
import us.mn.state.dot.tms.server.TagReaderImpl;
import us.mn.state.dot.tms.server.comm.TagReaderPoller;
import us.mn.state.dot.tms.server.comm.ThreadedPoller;
import static us.mn.state.dot.tms.utils.URIUtil.UDP;

/**
 * A Poller to communicate with TransCore E6 tag readers.
 *
 * @author Douglas Lau
 */
public class E6Poller extends ThreadedPoller<E6Property>
	implements TagReaderPoller
{
	/** E6 debug log */
	static private final DebugLog E6_LOG = new DebugLog("e6");

	/** Create a new E6 poller */
	public E6Poller(CommLink link) {
		super(link, UDP, E6_LOG);
	}

	/** Create a comm thread */
	@Override
	public E6Thread createCommThread(String uri, int timeout, int nrd) {
		return new E6Thread(this, queue, scheme, uri, timeout, nrd);
	}

	/** Tag reader */
	private TagReaderImpl reader;

	/** Get the tag reader */
	public TagReaderImpl getReader() {
		return reader;
	}

	/** Send a device request message to the tag reader */
	@Override
	public void sendRequest(TagReaderImpl tr, DeviceRequest r) {
		// FIXME: this is hacky
		if (reader != tr)
			reader = tr;
		switch (r) {
		case QUERY_SETTINGS:
			addOp(new OpQuerySettings(tr));
			break;
		case SEND_SETTINGS:
			addOp(new OpSendSettings(tr));
			break;
		case QUERY_STATUS:
			addOp(new OpQueryStatus(tr));
			break;
		default:
			// Ignore other requests
			break;
		}
	}
}
