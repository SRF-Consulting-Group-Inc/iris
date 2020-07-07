/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2015  SRF Consulting Group
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
package us.mn.state.dot.tms.client.camera;

import java.util.Comparator;

import com.enterprisedt.net.ftp.FTPFile;

/**
 * An FTP file comparator - returning most recently updated file
 *
 * @author Michael Janson
 */
public class FTPComparator implements Comparator<FTPFile> {

	@Override
	public int compare(FTPFile f1, FTPFile f2) {
		return f1.lastModified().compareTo(f2.lastModified());
	}
}
