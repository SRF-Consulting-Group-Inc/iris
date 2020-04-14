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

package us.mn.state.dot.tms.utils.wysiwyg;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;

import us.mn.state.dot.tms.client.wysiwyg.editor.WController;
import us.mn.state.dot.tms.utils.wysiwyg.token.Wt_Rectangle;

/**
 * Parent class for GUI operations with text and color rectangles in the
 * WYSIWYG DMS Message Editor. Not to be confused with the text/color
 * rectangle MULTI tag token parent class Wt_Rectangle.
 * 
 * Note that most calculations in this class are done in WYSIWYG/image space.
 *
 * @author Gordon Parikh - SRF Consulting
 */

abstract public class WgRectangle {
	
	/** The token that represents this rectangle in the MULTI string */
	protected Wt_Rectangle rt;

	/** WRaster object used to convert between coordinate spaces */
	private WRaster wr;
	
	/** Threshold used to make things a bit easier to select */
	private int t;
	
	/** Rectangles (WYSIWYG/image space) for determining relative cursor
	 *  location
	 */
	
	/** Inside or on border of this rectangle within some threshold */
	protected Rectangle near;
	
	/** Inside and not on border of this rectangle within some threshold */
	protected Rectangle inside;
	
	/** Handles for user control of resizing (WYSIWYG/image space) */
	protected HashMap<String, Rectangle> resizeHandles;
	
	/** Constants for referring to handles on rectangle */
	public final static String N = "N";
	public final static String S = "S";
	public final static String E = "E";
	public final static String W = "W";
	public final static String NE = "NE";
	public final static String NW = "NW";
	public final static String SE = "SE";
	public final static String SW = "SW";
	
	public WgRectangle(Wt_Rectangle rTok) {
		rt = rTok;
	}
	
	/** Return the MULTI tag token that defines this rectangle. */
	public Wt_Rectangle getRectToken() {
		return rt;
	}
	
	/** Return the last token associated with this rectangle (by default just
	 *  the rectangle token itself).
	 */
	public WToken getLastToken() {
		return rt;
	}
	
	/** Initialize geometry objects for use by the WRectangle. */
	public void initGeom(WRaster r, int threshold) {
		// set the WRaster and threshold
		wr = r;
		t = threshold;
		
		// create the rectangles for determining relative cursor placement
		if (rt != null) {
			// first get the rectangle coordinates in image space
			int x = wr.cvtSignToWysiwygX(rt.getParamX(), true, true);
			int rx = wr.cvtSignToWysiwygX(
					rt.getParamX() + rt.getParamW() - 1, false, true);
			int y = wr.cvtSignToWysiwygY(rt.getParamY(), true, true);
			int by = wr.cvtSignToWysiwygY(
					rt.getParamY() + rt.getParamH() - 1, false, true);
			int w = rx - x;
			int h = by - y;
			int t2 = 2*t;
			
			// now make the "near" rectangle
			near = new Rectangle(x-t, y-t, w+t2, h+t2);
			
			// the "inside" rectangle
			inside = new Rectangle(x+t, y+t, w-t2, h-t2);
			
			// and the handles for resizing
			int mx = wr.cvtSignToWysiwygX(rt.getCentroidX(), true, false);
			int my = wr.cvtSignToWysiwygY(rt.getCentroidY(), true, false);
			
			// create one handle for each midpoint/corner (N,S,E,W,NE,NW,SE,SW)
			resizeHandles = new HashMap<String, Rectangle>();
			resizeHandles.put(N, new Rectangle(mx-t,y-t,t2,t2));
			resizeHandles.put(S, new Rectangle(mx-t,by-t,t2,t2));
			resizeHandles.put(E, new Rectangle(rx-t,my-t,t2,t2));
			resizeHandles.put(W, new Rectangle(x-t,my-t,t2,t2));
			resizeHandles.put(NE, new Rectangle(rx-t,y-t,t2,t2));
			resizeHandles.put(NW, new Rectangle(x-t,y-t,t2,t2));
			resizeHandles.put(SE, new Rectangle(rx-t,by-t,t2,t2));
			resizeHandles.put(SW, new Rectangle(x-t,by-t,t2,t2));
		}
	}
	
	/** Return whether geometry objects have been initialized. */
	public boolean geomInitialized() {
		return wr != null && near != null
				&& inside != null && resizeHandles != null;
	}
	
	/** Return whether or not the point p is near (i.e. within the threshold)
	 *  this rectangle.
	 */
	public boolean isNear(WPoint p)
			throws NullPointerException {
		if (rt != null && near == null)
			throw new NullPointerException("Geometry not initialized");
		return near.contains(p.getWysiwygPoint());
	}
	
	/** Return whether or not the point p is on the border (within the
	 *  threshold) of this rectangle.
	 */
	public boolean isOnBorder(WPoint p)
			throws NullPointerException {
		if (rt != null && (near == null || inside == null))
			throw new NullPointerException("Geometry not initialized");
		Point wp = p.getWysiwygPoint();
		return near.contains(wp) && !inside.contains(wp);
	}
	
	/** Return the resize handles. makeResizeHandles must be called first,
	 *  otherwise null will be returned.
	 */
	public HashMap<String, Rectangle> getResizeHandles()
			throws NullPointerException {
		if (rt != null && resizeHandles == null)
			throw new NullPointerException("Geometry not initialized");
		return resizeHandles;
	}

	public String toString() {
		String ts = "null";
		if (rt != null)
			ts = rt.toString();
		return String.format("<%s: %s>", this.getClass().getName(), ts);
	}
	
	/** Move the rectangle by the given offsets. Note that this doesn't allow
	 *  moving past the sign border.
	 */
	public void move(int offsetX, int offsetY) {
		if (rt != null) {
			int ox = checkOffsetX(offsetX);
			int oy = checkOffsetY(offsetY);
			rt.moveTok(ox, oy);
			rt.updateString();
		}
	}
	
	/** Resize the rectangle given the direction (N/S/E/W/NE/NW/SE/SW) and
	 *  offsets provided.
	 */
	public void resize(String dir, int offsetX, int offsetY) {
		if (rt != null) {
			// first get the original dimensions of the rectangle
			int x = rt.getParamX();
			int y = rt.getParamY();
			int w = rt.getParamW();
			int h = rt.getParamH();
			
			// now use the direction to determine which values to adjust
			if (dir.startsWith(N)) {
				// for any north operations, increase y and decrease h
				y += offsetY;
				h -= offsetY;
			} else if (dir.startsWith(S))
				// otherwise for south operations just adjust h
				h += offsetY;
			
			if (dir.endsWith(W)) {
				// for any west operations, increase x and decrease w
				x += offsetX;
				w -= offsetX;
			} else if (dir.endsWith(E))
				// otherwise for east operations just adjust w
				w += offsetX;
			
			// now set the parameters and update the string
			rt.setParameters(x, y, w, h);
			rt.updateString();
		}
	}
	
	/** Check that the X offset value will not move the rectangle beyond the
	 *  sign border. If it won't, offsetX is returned unchanged, otherwise the
	 *  value will be reduced until it will fit on the sign (down to 0).
	 */
	public int checkOffsetX(int offsetX)
			throws NullPointerException {
		if (wr == null)
			throw new NullPointerException("Geometry not initialized");
		if (rt != null) {
			// first check under, then check over
			int x = rt.getParamX();
			int rx = rt.getRightEdge();
			if (x + offsetX < 1)
				return 1 - x;
			else if (rx + offsetX > wr.getWidth())
				return Math.max(rx - wr.getWidth(), 0);
			else
				return offsetX;
		}
		return 0;
	}

	/** Check that the Y offset value will not move the rectangle beyond the
	 *  sign border. If it won't, offsetY is returned unchanged, otherwise the
	 *  value will be reduced until it will fit on the sign (down to 0).
	 */
	public Integer checkOffsetY(int offsetY)
			throws NullPointerException {
		if (wr == null)
			throw new NullPointerException("Geometry not initialized");
		if (rt != null) {
			// first check under, then check over
			int y = rt.getParamY();
			int ry = rt.getBottomEdge();
			if (y + offsetY < 1)
				return 1 - y;
			else if (ry + offsetY > wr.getHeight())
				return Math.max(ry - wr.getHeight(), 0);
			else
				return offsetY;
		}
		return 0;
	}
}


















