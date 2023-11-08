/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2023  SRF Consulting Group
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
package us.mn.state.dot.tms.server.comm.digiwr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import us.mn.state.dot.tms.server.comm.AsciiDeviceProperty;
import us.mn.state.dot.tms.server.comm.PromptReader;
import us.mn.state.dot.tms.utils.LineReader;

/**
 * Simplified device-handshake property.
 * Related to AsciiDeviceProperty, but with a focus
 * on sending a specific command (ASCII or byte-array)
 * and expecting one (or two) specific responses.
 *
 * @author John L. Stanley
 */

public class HandshakeProperty extends AsciiDeviceProperty {

	/** Charset name for ASCII */
	static private final String ASCII = "US-ASCII";

	/** String or byte[] object to send */
	private Object sendObj;
	
	/** Response1 */
	private String sResponse1;

	/** Response2 */
	private String sResponse2 = null;

	/** Which response did we get? */
	private Integer responseNumber = null;

	/** Create a new line reader.
	 * @param is InputStream to read from. */
	protected LineReader newLineReader(InputStream is) throws IOException {
		if (sResponse2 == null)
			return new PromptReader(is, MAX_CHARS, sResponse1);
		return new PromptReader(is, MAX_CHARS, sResponse1, sResponse2);
	}

	/** Convert byte array to ASCII string. */
	static public String ba2s(byte[] ba) {
		try {
			return new String(ba, ASCII);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}

	/** Construct a property where we send a string and
	 *  wait for a specific string response. */
	public HandshakeProperty(String sendObj, String sResponse1) {
		super(sendObj);
		this.sendObj    = sendObj;
		this.sResponse1 = sResponse1;
	}

	/** Construct a property where we send a string and
	 *  wait for a specific byte array response. */
	public HandshakeProperty(String sendObj, byte[] baResponse1) {
		super(sendObj);
		this.sendObj    = sendObj;
		this.sResponse1 = ba2s(baResponse1);
	}

	/** Construct a property where we send a string and
	 *  wait for either of two string responses. */
	public HandshakeProperty(String sendObj, String sResponse1, String sResponse2) {
		super(sendObj);
		this.sendObj    = sendObj;
		this.sResponse1 = sResponse1;
		this.sResponse2 = sResponse2;
	}

	/** Construct a property where we send a byte array and
	 *  wait for a specific string response. */
	public HandshakeProperty(byte[] sendObj, String sResponse1) {
		super(ba2s(sendObj));
		this.sendObj    = sendObj;
		this.sResponse1 = sResponse1;
	}

	/** Construct a property where we send a byte array and
	 *  wait for either of two string responses. */
	public HandshakeProperty(byte[] sendObj, String sResponse1, String sResponse2) {
		super(ba2s(sendObj));
		this.sendObj    = sendObj;
		this.sResponse1 = sResponse1;
		this.sResponse2 = sResponse2;
	}

	/** Send sendObj to device. */
	@Override
	protected void sendCommand(OutputStream os)
		throws IOException
	{
		if (sendObj instanceof byte[]) {
			// if sendObj is a byte array, send unchanged
			byte[] x = (byte[])sendObj;
			os.write(x);
		}
		else if (sendObj instanceof String) {
			// if sendObj is a String, convert to ASCII and then send
			String x = (String)sendObj;
			os.write(x.getBytes(ASCII));
		}
	}

	/** Which number response did we receive?
	 * @return null means no expected response was received. */
	public Integer getResponseNumber() {
		return responseNumber;
	}

	@Override
	protected boolean parseResponse(String resp) throws IOException {
		if (resp.contains(sResponse1)) {
			responseNumber = 1;
			return true;  // got primary response, we're done
		}
		if ((sResponse2 != null) && resp.contains(sResponse2)) {
			responseNumber = 2;
			return true;  // got secondary response, we're still done
		}
		return false;  // keep looking
	}

	/** Get a string representation */
	@Override
	public String toString() {
		if (sResponse2 == null)
			return "HandshakeProperty(\""+sendObj+"\", \""+sResponse1+"\"): responseNumber = " + responseNumber;
		return "HandshakeProperty(\""+sendObj+"\", \""+sResponse1+"\", \""+sResponse2+"\"): responseNumber = "+responseNumber;
	}
}
