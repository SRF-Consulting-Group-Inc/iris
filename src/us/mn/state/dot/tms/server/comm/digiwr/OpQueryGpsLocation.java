/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2015-2017  SRF Consulting Group
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
import java.io.UnsupportedEncodingException;

import us.mn.state.dot.tms.server.GeoLocImpl;
import us.mn.state.dot.tms.server.GpsImpl;
import us.mn.state.dot.tms.server.comm.AsciiDeviceProperty;
import us.mn.state.dot.tms.server.comm.CommMessage;
import us.mn.state.dot.tms.server.comm.OpDevice;
import us.mn.state.dot.tms.server.comm.PriorityLevel;

/**
 * This operation logs into a Digi RX-series GPS modem
 * and then queries the GPS coordinates of the modem.
 *
 * @author John L. Stanley
 */
public class OpQueryGpsLocation extends OpDevice<AsciiDeviceProperty> {

	/** GPS device */
	private final GpsImpl gps;

	/** Device location */
	private final GeoLocImpl loc;

	/** GPS location property */
	private final GpsLocationProperty gps_prop;

	/** Create a new query GPS operation */
	public OpQueryGpsLocation(GpsImpl g, GeoLocImpl l) {
		super(PriorityLevel.POLL_LOW, g);
		gps = g;
		loc = l;
		gps_prop = new GpsLocationProperty();
		gps.setLatestPollNotify();
	}

	/** Telnet prompt sent from Digi WR modem */
	static final byte[] baTelnetPrompt = {
			(byte)0xff, (byte)0xf9,             // go ahead
			(byte)0xff, (byte)0xfd, (byte)0x00, // DO binary transmission mode
			(byte)0xff, (byte)0xfb, (byte)0x01, // WILL echo
			(byte)0xff, (byte)0xfc, (byte)0x22, // WON'T line-mode
			(byte)0xff, (byte)0xfd, (byte)0x03, // DO suppress go ahead
			(byte)0xff, (byte)0xfb, (byte)0x03};// WILL suppress go ahead

	/** Telnet response sent to Digi WR modem */
	static final byte[] baTelnetResponse = {
			(byte)0xff, (byte)0xfb, (byte)0x00, // WILL binary transmission mode
			(byte)0xff,	(byte)0xfe, (byte)0x01, // DON'T echo
			(byte)0xff, (byte)0xfb,	(byte)0x03, // WILL suppress go ahead
			(byte)0xff, (byte)0xfd, (byte)0x03};// DO suppress go ahead

	/** Get the username parsed from controller password as "user:pass" */
	private String getUsername() {
		String p = getController().getPassword();
		if (p != null && p.length() > 0) {
			String[] up = p.split(":", 2);
			return up[0];
		} else
			return null;
	}

	/** Get the password parsed from controller password as "user:pass" */
	private String getPassword() {
		String p = getController().getPassword();
		if (p != null && p.length() > 0) {
			String[] up = p.split(":", 2);
			return (up.length > 1) ? up[1] : null;
		} else
			return null;
	}

	/** Create the second phase of the operation */
	@Override
	protected Phase<AsciiDeviceProperty> phaseTwo() {
		return new WaitForTelnet();
	}

	/** Phase to wait for telnet prompt */
	private class WaitForTelnet extends Phase<AsciiDeviceProperty> {

		protected Phase<AsciiDeviceProperty> poll(
			CommMessage<AsciiDeviceProperty> mess) throws IOException
		{
			// Don't send anything.  Just wait for the telnet prompt.
			HandshakeProperty prop = new HandshakeProperty("", baTelnetPrompt);
			mess.add(prop);
			mess.queryProps();
			return (prop.getResponseNumber() == 1)
			      ? new Telnet2Username()
			      : null;
		}
	}

	/** Phase to send telnet response and wait for username prompt */
	private class Telnet2Username extends Phase<AsciiDeviceProperty> {

		protected Phase<AsciiDeviceProperty> poll(
			CommMessage<AsciiDeviceProperty> mess) throws IOException
		{
			HandshakeProperty prop = new HandshakeProperty(baTelnetResponse, "Username: ");
			mess.add(prop);
			mess.queryProps();
			if (prop.getResponseNumber() == 1)
				return new Username2Password();
			else {
				setErrorStatus("No telnet prompt");
				return null;
			}
		}
	}

	/** Phase to send username and wait for password prompt */
	private class Username2Password extends Phase<AsciiDeviceProperty> {

		protected Phase<AsciiDeviceProperty> poll(
			CommMessage<AsciiDeviceProperty> mess) throws IOException
		{
			String un = getUsername();
			if (un == null)
				un = "";
			HandshakeProperty prop = new HandshakeProperty(un+"\r", "Password: ");
			mess.add(prop);
			mess.queryProps();
			if (prop.getResponseNumber() == 1)
				return new Password2Command();
			else {
				setErrorStatus("Modem login failed");
				return null;
			}
		}
	}

	/** Phase to send password and wait for modem ">" command prompt
	 * (or the "Bad Username/Password" response). */
	private class Password2Command extends Phase<AsciiDeviceProperty> {

		protected Phase<AsciiDeviceProperty> poll(
			CommMessage<AsciiDeviceProperty> mess) throws IOException
		{
			String pw = getPassword();
			if (pw == null)
				pw = "";
			HandshakeProperty prop = new HandshakeProperty(pw+"\r", ">", "Bad Username/Password");
			mess.add(prop);
			mess.queryProps();
			if (prop.getResponseNumber() == 1)
				return new QueryGps();
			else {
				setErrorStatus("Modem login failed");
				return null;
			}
		}
	}

	/** Phase to send GPS query command and parse the GPS location response. */
	private class QueryGps extends Phase<AsciiDeviceProperty> {

		protected Phase<AsciiDeviceProperty> poll(
			CommMessage<AsciiDeviceProperty> mess) throws IOException
		{
			mess.add(gps_prop);
			mess.queryProps();
			return null;
		}
	}

	/** Cleanup the operation */
	@Override
	public void cleanup() {
		if (isSuccess())
			updateGpsLocation();
		super.cleanup();
	}

	/** Update the GPS location */
	private void updateGpsLocation() {
		if (gps_prop.gotValidResponse()) {
			if (gps_prop.gotGpsLock()) {
				gps.saveDeviceLocation(loc,
					gps_prop.getLat(),
					gps_prop.getLon());
			} else
				setErrorStatus("No GPS Lock");
		}
	}
}
