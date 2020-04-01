/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2020  SRF Consulting Group
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
package us.mn.state.dot.tms.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

/** SubnetChecker is a self-launching daemon thread 
 *  that identifies which subnet the Iris client is
 *  running on using a configurable series of TCP
 *  and/or ICMP pings.
 * 
 * @author John L. Stanley - SRF Consulting
 */

//#######################################################
//TODO:  MUST modify this to allow loading a list of 
// administrator-determined ping target on the client.
// The list may come from iris-client.properties, from
// the Iris system attributes table, or somewhere else.
//#######################################################

public class SubnetChecker extends Thread {

	/** A single thread for the checker */
	static SubnetChecker thread;
	
	/** When SubnetChecker is first referenced,
	 *  this creates an instance and runs it. */
	static {
		thread = new SubnetChecker();
		thread.setDaemon(true);
		thread.start();
	}

	//-------------------------------------------

	/** Maximum wait time for failed ping.
	 * Must be less than the default timeout
	 * for both a socket connection and a
	 * command-line ping command. {3 seconds}
	 */
	private final static int timeoutMillisec = 3*1000;

	/** Constant for unknown subnet name */
	public final static String UNKNOWN = "unknown";
	
	/** Current subnet */
	private static String subnetName = UNKNOWN;

	/** Flag to request a recheck */
	private static boolean updateReq = false;
	
	/** Time from end of previous update to start
	 *  of next automatic update. {1 minute} */
	private final static long autoRetryMillisec = 60*1000;

	/** Is this machine running Windows? */
    private static boolean isWindows = 
    		System.getProperty("os.name").toLowerCase().contains("win");
    
    /** Operating system dependent ping retry-count command-line-prefix */
    private static String pingRetryArg = isWindows ? "-n" : "-c";
    
    /** List of current addresses on this machine */
    private static String ipAddressList;

	//-------------------------------------------

    //TODO:  MUST CHANGE TO ALLOW LOADING SUBNET TARGET LIST FROM EXTERNAL SOURCE!!
    
	// test/sample list of ping targets and subnet names
	private static String[] targets = {
		"www.google.com=internet",    // icmp ping
		"www.google.com:80=internet", // tcp ping
	};

	//-------------------------------------------

	/** Construct/initialize the subnet checker */
	private SubnetChecker() {
		super("SubnetChecker");
		ipAddressList = getAllMyAddresses();
	}

	//-------------------------------------------

	/** Get current subnet name */
	public static String getSubnetName() {
		return subnetName;
	}

	/** Request an update */
	public static void reqUpdate() {
		updateReq = true;
	}
	
	//-------------------------------------------

	/** Main checker loop.
	 *  Exits when program ends. */
	public void run() {
		// the main checker loop
		while (true) {
			doPings();
			System.out.println("Address list: "+ipAddressList);
			System.out.println("Subnet: "+subnetName);
			updateReq = false;
			doWait();
		}
	}

	/** Wait for one of the following:
	 *		Time for an automatic update
	 * 		Workstation IP-address change
	 * 		Workstation "sleep-mode" detection
	 * 		An update request
	 */
	private void doWait() {
		long now = System.currentTimeMillis();
		long end = now + autoRetryMillisec;
		long prev = now;
		long delta;
		String tmp;
		while (true) {
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				break;
			}
			if (updateReq) {
				System.out.println("Update requested");
				break;
			}
			// time for automatic update?
			now = System.currentTimeMillis();
			if (now > end) {
				System.out.println("Update time");
				break; // yes
			}
			// snooze or time-change detected?
			delta = Math.abs(now - prev);
			if (delta > 10000) {
				System.out.println("Sleep-mode detected: "+delta);
				break;  // yes
			}
			// IP address change?
			tmp = getAllMyAddresses();
			if (!ipAddressList.equals(tmp)) {
				ipAddressList = tmp;
				System.out.println("Address change");
				break;  // yes
			}
			prev = now;
		}
	}

	/** Ping hosts and set subnetName */
	private void doPings() {
		String target, host, port, subnet;
		String [] strs;
		int len = targets.length;
		boolean gotPing;
		for (int i = 0; (i < len); ++i) {
			target = targets[i];
			strs = target.split("=");
			if (strs.length != 2)
				continue;  // bad target string
			host = strs[0].trim();
			subnet = strs[1].trim();
			strs = host.split(":");
			if (strs.length == 2) {
				// do a TCP ping
				host = strs[0].trim();
				port = strs[1].trim();
				gotPing = doTcpPing(host, Integer.parseInt(port));
			}
			else {
				// do an ICMP (standard) ping
				gotPing = doIcmpPing(host);
			}
			if (gotPing) {
				subnetName = subnet;
				return;
			}
		}
		// didn't get any responses...
		subnetName = UNKNOWN;
	}

	/** Do an ICMP ping */
	public static boolean doIcmpPing(String host) {
		// Make sure we can resolve the host name to an IP address
    	InetSocketAddress sockaddr = new InetSocketAddress(host, 0);
    	if (sockaddr.isUnresolved())
    		return false;
		// Run command line ping with a timeout of 5 seconds
	    ProcessBuilder processBuilder = new ProcessBuilder("ping", pingRetryArg, "1", "-w", "5000", host);
		try {
			Process proc = processBuilder.start();
		    return proc.waitFor(timeoutMillisec, TimeUnit.MILLISECONDS);
		} catch (IOException e) {
			return false;
		} catch (InterruptedException e) {
			return false;
		}
	}

	/** Do a TCP ping (TCP connect and instant disconnect) */
	public static boolean doTcpPing(String host, int port) {
	    try {
	    	InetSocketAddress sockaddr = new InetSocketAddress(host, port);
	    	if (sockaddr.isUnresolved())
	    		return false;
	        try (Socket soc = new Socket()) {
	            soc.connect(sockaddr, timeoutMillisec);
	        }
	        return true;
	    } catch (IOException ex) {
	        return false;
	    }
	}
	
	/** Get string containing comma separated
	 *  list of all current IP addresses for
	 *  this workstation. */
	static public String getAllMyAddresses() {
		StringBuilder sb = new StringBuilder();
		Enumeration<NetworkInterface> e;
		try {
			e = NetworkInterface.getNetworkInterfaces();
			while(e.hasMoreElements()) {
				NetworkInterface n = (NetworkInterface) e.nextElement();
				Enumeration<InetAddress> ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {
					InetAddress i = (InetAddress) ee.nextElement();
					if (sb.length() > 0)
						sb.append(',');
					sb.append(i.getHostAddress());
				}
			}
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		return sb.toString();
	}

	//-------------------------------------------
	
	/** test */
	static public void main(String[] args) {
		try {
			sleep(10*1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Exiting");
	}	
}
