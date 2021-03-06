/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2016-2018  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.pelcop;

import java.io.IOException;
import java.nio.ByteBuffer;
import us.mn.state.dot.tms.Camera;
import us.mn.state.dot.tms.CameraHelper;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.VideoMonitor;
import us.mn.state.dot.tms.VideoMonitorHelper;
import us.mn.state.dot.tms.server.CameraImpl;
import us.mn.state.dot.tms.server.VideoMonitorImpl;
import us.mn.state.dot.tms.server.comm.Operation;
import us.mn.state.dot.tms.server.comm.ParsingException;

/**
 * Monitor status property.  The CM9760-KBD sends this request to the matrix
 * a few times per second.  The matrix should send a monitor status response
 * back.  If logon is required, an empty monitor status response is sent.
 *
 * @author Douglas Lau
 */
public class MonStatusProp extends PelcoPProp {

	/** Monitor status request code */
	static public final int REQ_CODE = 0xBA;

	/** Monitor status response code */
	static public final int RESP_CODE = 0xB1;

	/** Get the "blank" camera number */
	static protected int cameraNumBlank() {
		return SystemAttrEnum.CAMERA_NUM_BLANK.getInt();
	}

	/** Get camera number */
	static private int getCamNum(VideoMonitor vm) {
		if (vm != null) {
			Camera c = vm.getCamera();
			if (c != null) {
				Integer cn = c.getCamNum();
				if (cn != null)
					return cn;
			}
		}
		return cameraNumBlank();
	}

	/** Get camera sequence number */
	static private int getSequenceNum(VideoMonitorImpl vm) {
		if (vm != null) {
			Integer n = vm.getSeqNum();
			if (n != null && n > 0)
				return n;
		}
		return 0;
	}

	/** Check if a sequence is running */
	static protected boolean isSequenceRunning(VideoMonitorImpl vm) {
		return (vm != null) && vm.isSequenceRunning();
	}

	/** Find a camera by UID */
	static protected CameraImpl findCamera(int cam_num) {
		Camera c = CameraHelper.findNum(cam_num);
		return (c instanceof CameraImpl) ? (CameraImpl) c : null;
	}

	/** Flag for monitor macro running status */
	static protected final int BIT_MACRO = 0x02;

	/** Flag for monitor locked status */
	static protected final int BIT_LOCKED = 0x10;

	/** Flag for monitor online status */
	static protected final int BIT_ONLINE = 0x40;

	/** Logged in flag */
	private final boolean logged_in;

	/** Video monitor number */
	private int mon_num;

	/** Create a new monitor status property */
	public MonStatusProp(boolean l, int mn) {
		logged_in = l;
		mon_num = mn;
	}

	/** Decode a QUERY request from keyboard */
	@Override
	public void decodeQuery(Operation op, ByteBuffer rx_buf)
		throws ParsingException
	{
		int mlo = parseBCD2(rx_buf);
		if (parse8(rx_buf) != 1)
			throw new ParsingException("MON EXT");
		int mhi = parseBCD2(rx_buf);
		setMonNumber((100 * mhi) + mlo);
	}

	/** Encode a QUERY response to keyboard */
	@Override
	public void encodeQuery(Operation op, ByteBuffer tx_buf)
		throws IOException
	{
		if (hasError()) {
			encodeError(op, tx_buf);
			return;
		}
		format8(tx_buf, RESP_CODE);
		int mon = getMonNumber();
		if (logged_in && mon > 0) {
			VideoMonitorImpl vm = findVideoMonitor();
			int cam = getCamNum(vm);
			int sqn = getSequenceNum(vm);
			int chi = cam / 100;
			int clo = cam % 100;
			int mhi = mon / 100;
			int mlo = mon % 100;
			int shi = sqn / 100;
			int slo = sqn % 100;
			formatBCD2(tx_buf, mlo);
			format8(tx_buf, getModeBits(vm));
			format8(tx_buf, 0);
			formatBCD2(tx_buf, chi);
			formatBCD2(tx_buf, clo);
			formatBCD2(tx_buf, shi);
			formatBCD2(tx_buf, slo);
			format16(tx_buf, 0);
			format8(tx_buf, 0);
			formatBCD2(tx_buf, chi);
			formatBCD2(tx_buf, clo);
			format16(tx_buf, 0);
			format8(tx_buf, 0);
			formatBCD2(tx_buf, mhi);
		} else {
			format8(tx_buf, 0);
			format8(tx_buf, 0);
		}
	}

	/** Get the mode bits */
	protected int getModeBits(VideoMonitorImpl vm) {
		return isSequenceRunning(vm)
			? (BIT_ONLINE | BIT_MACRO)
			: BIT_ONLINE;
	}

	/** Get current camera number on the selected video monitor */
	protected int getCamNumber() {
		return getCamNum(findVideoMonitor());
	}

	/** Find a video monitor by number */
	protected VideoMonitorImpl findVideoMonitor() {
		VideoMonitor vm = VideoMonitorHelper.findUID(mon_num);
		return (vm instanceof VideoMonitorImpl)
		     ? (VideoMonitorImpl) vm
		     : null;
	}

	/** Set the video monitor number */
	protected void setMonNumber(int m) {
		mon_num = m;
		if ((m > 0) && findVideoMonitor() == null)
			setErrMsg(ErrorMsg.MonNotPresent);
	}

	/** Get the video monitor number */
	protected int getMonNumber() {
		return mon_num;
	}

	/** Select a new camera on a video monitor */
	protected void selectCamera(CameraImpl c, String src) {
		assert(c != null);
		stopCamControl();
		VideoMonitorImpl.setCamMirrored(getMonNumber(), c, src);
	}

	/** Stop camera control on selected camera */
	protected void stopCamControl() {
		VideoMonitorImpl vm = findVideoMonitor();
		if (vm != null) {
			Camera c = vm.getCamera();
			if (c instanceof CameraImpl)
				((CameraImpl) c).sendPTZ(0, 0, 0);
		}
	}
}
