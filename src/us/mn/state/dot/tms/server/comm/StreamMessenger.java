/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2007-2020  Minnesota Department of Transportation
 * Copyright (C) 2020       SRF Consulting Group
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
package us.mn.state.dot.tms.server.comm;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import us.mn.state.dot.tms.server.ControllerImpl;

/**
 * A StreamMessenger is a class which can poll a field controller and get the
 * response using a TCP socket connection.
 *
 * @author Douglas Lau
 * @author John L. Stanley - SRF Consulting
 */
public class StreamMessenger extends BasicMessenger {

	/** Create a TCP stream messenger.
	 * @param u URI of remote host.
	 * @param rt Receive timeout (ms).
	 * @param ct Connect timeout (ms).
	 * @param nrd No-response disconnect (sec). */
	static protected StreamMessenger create(URI u, int rt, int ct, int nrd)
		throws MessengerException, IOException
	{
		return new StreamMessenger(createSocketAddress(u), rt, ct, nrd);
	}

	/** Address to connect */
	private final SocketAddress address;

	/** Receive timeout (ms) */
	private final int recv_timeout;

	/** Connect timeout (ms) */
	private final int conn_timeout;

	/** TCP socket */
	private final Socket socket;

	/** Input stream */
	private final InputStream input;

	/** Output stream */
	private final OutputStream output;

	/** Create a new stream messenger.
	 * NOTE: must call setConnected to switch from conn_timeout to
	 *       recv_timeout. */
	private StreamMessenger(SocketAddress a, int rt, int ct, int nrd)
		throws IOException
	{
		super(nrd);
		address = a;
		recv_timeout = rt;
		conn_timeout = ct;
		socket = new Socket();
		socket.setSoTimeout(conn_timeout);
		socket.connect(address, conn_timeout);
		input = socket.getInputStream();
		output = socket.getOutputStream();
	}

	/** Get the input stream.
	 * @param path Relative path name.
	 * @return An input stream for reading from the messenger. */
	@Override
	protected InputStream getRawInputStream(String path) {
		return input;
	}

	/** Get the output stream */
	@Override
	protected OutputStream getRawOutputStream(ControllerImpl c) {
		return output;
	}

	/** Drain any bytes from the input stream */
	@Override
	public void drain() throws IOException {
		int a = input.available();
		if (a > 0)
			input.skip(a);
	}

	/** Close the stream messenger */
	@Override
	protected void close2() throws IOException {
		socket.close();
	}

	/** Set the messenger to a connected state */
	public void setConnected() throws SocketException {
		socket.setSoTimeout(recv_timeout);
	}
}
