/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2016-2023  SRF Consulting Group
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
import us.mn.state.dot.tms.utils.LineReader;

/**
 * Simple text line reader modified to (in addition to
 * parsing normal EOL (CR/LF) terminated lines) also
 * look for two specific expected responses with no EOL.
 *
 * @author John L. Stanley
 */
public class PromptReader extends LineReader {

	/** String the reader will look for at the end of a line */
	protected String response1;

	/** Alternate string the reader will look for at the end of a line */
	protected String response2 = null;

	/** Create a new line reader.
	 * @param is Input stream to read.
	 * @param max_chars Maximum number of characters on a line.
	 * @param response1 Response the reader is looking for.
	 * @throws IOException */
	public PromptReader(InputStream is, int max_chars, String response1)
		throws IOException
	{
		super(is, max_chars);
		this.response1 = response1;
	}

	/** Create a new line reader.
	 * @param is Input stream to read.
	 * @param max_chars Maximum number of characters on a line.
	 * @param response1 Response the reader is looking for.
	 * @throws IOException */
	public PromptReader(InputStream is, int max_chars, String response1, String response2)
		throws IOException
	{
		super(is, max_chars);
		this.response1 = response1;
		this.response2 = response2;
	}

	/** Read a line of text */
	@Override
	public String readLine() throws IOException {
		int eol = endOfLine();
		String str;
		while ((eol < 0) && (n_chars < buffer.length)) {
			if (n_chars > 0) {
				str = peekBuffer();
				if (str.contains(response1)) {
					n_chars = 0;
					return str;
				}
				if ((response2 != null) && str.contains(response2)) {
					n_chars = 0;
					return str;
				}
			}
			int n = reader.read(buffer, n_chars,
			                    buffer.length - n_chars);
			if (n < 0) {
				if (n_chars > 0)
					return bufferedLine(n_chars);
				else
					return null;
			} else {
				n_chars += n;
				eol = endOfLine();
			}
		}

		if (eol < 0)
			throw new IOException("PromptReader buffer full");

		return bufferedLine(eol);
	}

	/** Gets a string copy of all remaining text in buffer
	 *  but doesn't modify any offsets */
	protected String peekBuffer() {
		return new String(buffer, 0, n_chars);
	}
}
