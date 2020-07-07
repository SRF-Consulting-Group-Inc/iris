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
package us.mn.state.dot.tms.server.comm.gps.sierragx;

import java.io.IOException;

import us.mn.state.dot.tms.server.GpsImpl;
import us.mn.state.dot.tms.server.comm.CommMessage;
import us.mn.state.dot.tms.server.comm.gps.OpQueryGpsLocation;

/**
 * This operation logs into a Sierra Wireless GX GPS modem
 * and then queries the GPS coordinates of the modem.
 *
 * @author John L. Stanley
 */
public class OpQueryGpsLocationSierraGx
		extends OpQueryGpsLocation {

	protected final TestLoginModeProperty propTestLoginNode;
	protected final SendUsernameProperty  propSendUsername;
	protected final SendPasswordProperty  propSendPassword;
	protected final GpsSierraGxProperty   propQueryGps;

	/** GPS modem to talk to */
	protected final GpsImpl gps;
	
	/** Create a new login object */
	public OpQueryGpsLocationSierraGx(GpsImpl g, boolean force) {
		super(g, force, new GpsSierraGxProperty());
		gps = g;
		propTestLoginNode = new TestLoginModeProperty();
		propSendUsername  = new SendUsernameProperty(g.getUn());
		propSendPassword  = new SendPasswordProperty(g.getPw());
		propQueryGps      = (GpsSierraGxProperty) prop;
	}

	//----------------------------------------------

	/** Create the second phase of the operation */
	@SuppressWarnings("rawtypes")
	@Override
	protected Phase phaseTwo() {
		boolean bUseLogin = (gps.getUn() != null) || (gps.getPw() != null);
		if (bUseLogin)
			return (propTestLoginNode == null)
					? null
					: (new DoCheckNeedLogin());
		return (propQueryGps == null)
				? null
				: (new DoQueryGps());
	}

	/** Phase to check if we need to log into the modem */
	@SuppressWarnings("rawtypes")
	protected class DoCheckNeedLogin extends Phase {

		@SuppressWarnings("unchecked")
		protected Phase poll(CommMessage mess)
			throws IOException
		{
			mess.add(propTestLoginNode);
			mess.queryProps();
//			if (!isSuccess())
//				return null;  // error exit

			if (propTestLoginNode.gotLoginPrompt()) {
				return new DoSendUsername();
			}

			//FIXME: Find some way to force a disconnect from here...
			return null;
		}
	}

	/** Phase to send username */
	@SuppressWarnings("rawtypes")
	protected class DoSendUsername extends Phase {

		@SuppressWarnings("unchecked")
		protected Phase poll(CommMessage mess)
			throws IOException
		{
			mess.add(propSendUsername);
			mess.queryProps();
			if (!propSendUsername.gotValidResponse() || !isSuccess())
				return null;  // error exit

			if (propSendUsername.gotPwPrompt()) {
				return new DoSendPassword();
			}
			
			setErrorStatus("Login Failed");
			return null;
		}
	}

	/** Phase to send password */
	@SuppressWarnings("rawtypes")
	protected class DoSendPassword extends Phase {

		@SuppressWarnings("unchecked")
		protected Phase poll(CommMessage mess)
			throws IOException
		{
			mess.add(propSendPassword);
			mess.queryProps();
			if (!propSendPassword.gotValidResponse() || !isSuccess())
				return null;  // error exit

			if (propSendPassword.getLoginFinished())
				return new DoQueryGps();
			
			setErrorStatus("Login Failed");
			return null;
		}
	}

	/** Phase to query GPS location */
	@SuppressWarnings("rawtypes")
	protected class DoQueryGps extends Phase {

		@SuppressWarnings({ "unchecked", "synthetic-access" })
		protected Phase poll(CommMessage mess)
			throws IOException
		{
			mess.add(propQueryGps);
			mess.queryProps();
			if (!propQueryGps.gotValidResponse() || !isSuccess())
				return null;

			if (!propQueryGps.gotGpsLock()) {
				setErrorStatus("No GPS Lock");
				return null;
			}
			
			if (addSample(propQueryGps.getLat(), propQueryGps.getLon())) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return this;  // ask for another sample
			}

			filterSamplesAndSave();
			return null;
		}
	}
	
	void disconnect() {
		this.controller.getPoller().disconnectIfIdle();
	}
}
