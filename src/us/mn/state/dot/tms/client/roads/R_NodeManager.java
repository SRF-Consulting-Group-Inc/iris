/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2006-2010  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.client.roads;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.DefaultListModel;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;
import us.mn.state.dot.map.StyledTheme;
import us.mn.state.dot.map.Symbol;
import us.mn.state.dot.sched.AbstractJob;
import us.mn.state.dot.sonar.Checker;
import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.CorridorBase;
import us.mn.state.dot.tms.GeoLoc;
import us.mn.state.dot.tms.GeoLocHelper;
import us.mn.state.dot.tms.R_Node;
import us.mn.state.dot.tms.R_NodeHelper;
import us.mn.state.dot.tms.R_NodeTransition;
import us.mn.state.dot.tms.R_NodeType;
import us.mn.state.dot.tms.Road;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.GeoLocManager;
import us.mn.state.dot.tms.client.proxy.MapGeoLoc;
import us.mn.state.dot.tms.client.proxy.PropertiesAction;
import us.mn.state.dot.tms.client.proxy.ProxyManager;
import us.mn.state.dot.tms.client.proxy.StyleListModel;
import us.mn.state.dot.tms.client.toast.SmartDesktop;

/**
 * R_Node manager provides proxies for roadway nodes.
 *
 * @author Douglas Lau
 */
public class R_NodeManager extends ProxyManager<R_Node> {

	/** Marker to draw roadway entrance nodes */
	static protected final Shape ENTRANCE_MARKER = new EntranceMarker();

	/** Marker to draw roadway exit nodes */
	static protected final Shape EXIT_MARKER = new ExitMarker();

	/** Marker to draw roadway station nodes */
	static protected final Shape STATION_MARKER = new StationMarker();

	/** Marker to draw roadway intersection nodes */
	static protected final Shape INTERSECTION_MARKER =
		new IntersectionMarker();

	/** Marker to draw access nodes */
	static protected final Shape ACCESS_MARKER = new AccessMarker();

	/** Offset angle for default North map markers */
	static protected final double NORTH_ANGLE = Math.PI / 2;

	/** Name of "has GPS" style */
	static public final String STYLE_GPS = "Has GPS";

	/** Name of "no GPS" style */
	static public final String STYLE_NO_GPS = "No GPS";

	/** Name of "no location" style */
	static public final String STYLE_NO_LOC = "No Location";

	/** Map to of corridor names to corridors */
	protected final Map<String, CorridorBase> corridors =
		new TreeMap<String, CorridorBase>();

	/** Map to hold exit/entrance links */
	protected final Map<String, R_Node> e_links =
		new HashMap<String, R_Node>();

	/** List model of all corridors */
	protected final DefaultListModel model = new DefaultListModel();

	/** Get the corridor list model */
	public ListModel getCorridorModel() {
		return model;
	}

	/** User session */
	protected final Session session;

	/** Currently selected corridor */
	protected String corridor = "";

	/** Select a new freeway corridor */
	public void setCorridor(String c) {
		corridor = c;
	}

	/** Create a new roadway node manager */
	public R_NodeManager(Session s, TypeCache<R_Node> c,
		GeoLocManager lm)
	{
		super(c, lm);
		session = s;
		lm.setR_NodeManager(this);
		cache.addProxyListener(this);
	}

	/** Add a new proxy to the r_node manager */
	protected void proxyAddedSlow(R_Node n) {
		super.proxyAddedSlow(n);
		addCorridor(n);
	}

	/** Called when proxy enumeration is complete */
	public void enumerationComplete() {
		// Don't hog the SONAR TaskProcessor thread
		new AbstractJob() {
			public void perform() {
				arrangeCorridors();
				superComplete();
			}
		}.addToScheduler();
	}

	/** Call the enumerationComplete method of the super class */
	protected void superComplete() {
		super.enumerationComplete();
	}

	/** Arrange the corridor mapping */
	protected void arrangeCorridors() {
		R_NodeHelper.find(new Checker<R_Node>() {
			public boolean check(R_Node r_node) {
				findDownstreamLinks(r_node);
				return false;
			}
		});
		for(CorridorBase c: corridors.values()) {
			c.arrangeNodes();
			setTangentAngles(c);
		}
	}

	/** Set the tangent angles for all the nodes in a corridor */
	protected void setTangentAngles(CorridorBase c) {
		LinkedList<MapGeoLoc> locs = new LinkedList<MapGeoLoc>();
		for(R_Node n: c.getNodes()) {
			MapGeoLoc loc = super.findGeoLoc(n);
			if(loc != null)
				locs.add(loc);
		}
		if(locs.size() > 0) {
			// The first and last locations need to be in the list
			// twice to allow tangents to be calculated.
			locs.addFirst(locs.getFirst());
			locs.addLast(locs.getLast());
		}
		MapGeoLoc loc_a = null;
		MapGeoLoc loc = null;
		for(MapGeoLoc loc_b: locs) {
			if(loc_a != null) {
				Vector2D va = Vector2D.create(
					loc_a.getGeoLoc());
				Vector2D vb = Vector2D.create(
					loc_b.getGeoLoc());
				Vector2D a = vb.subtract(va);
				double t = a.getAngle();
				if(!Double.isInfinite(t) && !Double.isNaN(t))
					loc.setTangent(t - NORTH_ANGLE);
			}
			loc_a = loc;
			loc = loc_b;
		}
	}

	/** Find downstream links (not in corridor) for the given node */
	protected void findDownstreamLinks(R_Node r_node) {
		if(R_NodeHelper.isExit(r_node))
			linkExitToEntrance(r_node);
	}

	/** Link an exit node with a corresponding entrance node */
	protected void linkExitToEntrance(final R_Node r_node) {
		final LinkedList<R_Node> nl = new LinkedList<R_Node>();
		R_NodeHelper.find(new Checker<R_Node>() {
			public boolean check(R_Node other) {
				if(R_NodeHelper.isExitLink(r_node, other))
					nl.add(other);
				return false;
			}
		});
		R_Node link = findNearest(r_node, nl);
		if(link != null) {
			e_links.put(r_node.getName(), link);
			e_links.put(link.getName(), r_node);
		}
	}

	/** Find the nearest r_node in a list */
	static protected R_Node findNearest(R_Node r_node, List<R_Node> others){
		R_Node nearest = null;
		double distance = 0;
		for(R_Node other: others) {
			double m = CorridorBase.metersTo(r_node, other);
			if(nearest == null || m < distance) {
				nearest = other;
				distance = m;
			}
		}
		return nearest;
	}

	/** Create the segment layer */
	public SegmentLayer createSegmentLayer() {
		SegmentLayer seg_layer = new SegmentLayer(this, session);
		for(CorridorBase c: corridors.values()) {
			if(c.getFreeDir() > 0)
				seg_layer.addCorridor(c);
		}
		return seg_layer;
	}

	/** Get the proxy type */
	public String getProxyType() {
		return "R_Node";
	}

	/** Get the shape to use for the given r_node */
	protected Shape getShape(R_Node n, AffineTransform at) {
		R_NodeType nt = R_NodeType.fromOrdinal(n.getNodeType());
		switch(nt) {
		case ENTRANCE:
			return ENTRANCE_MARKER;
		case EXIT:
			return EXIT_MARKER;
		case INTERSECTION:
			return INTERSECTION_MARKER;
		case ACCESS:
			return ACCESS_MARKER;
		default:
			return STATION_MARKER;
		}
	}

	/** Check the style of the specified proxy */
	public boolean checkStyle(String s, R_Node proxy) {
		if(STYLE_GPS.equals(s))
			return GeoLocHelper.hasGPS(getGeoLoc(proxy));
		else if(STYLE_NO_GPS.equals(s))
			return !GeoLocHelper.hasGPS(getGeoLoc(proxy));
		else if(STYLE_NO_LOC.equals(s))
			return GeoLocHelper.isNull(getGeoLoc(proxy));
		else if(corridors.containsKey(s)) {
			String c=GeoLocHelper.getCorridorName(getGeoLoc(proxy));
			return s.equals(c);
		} else
			return STYLE_ALL.equals(s);
	}

	/** Create a style list model for the given symbol */
	protected StyleListModel<R_Node> createStyleListModel(Symbol s) {
		// No style list models on roadway tab
		return null;
	}

	/** Create a styled theme for r_nodes */
	protected StyledTheme createTheme() {
		R_NodeMapTheme theme = new R_NodeMapTheme(this);
		theme.addStyle(STYLE_GPS, R_NodeRenderer.COLOR_GPS);
		theme.addStyle(STYLE_NO_GPS, R_NodeRenderer.COLOR_NO_GPS);
		theme.addStyle(STYLE_NO_LOC, R_NodeRenderer.COLOR_NO_LOC);
		theme.addStyle(STYLE_ALL);
		return theme;
	}

	/** Lookup the corridor for a location */
	public CorridorBase lookupCorridor(GeoLoc loc) {
		String cid = GeoLocHelper.getCorridorName(loc);
		if(cid != null)
			return corridors.get(cid);
		else
			return null;
	}

	/** Add a corridor for the specified r_node */
	protected void addCorridor(R_Node r_node) {
		GeoLoc loc = r_node.getGeoLoc();
		String cid = GeoLocHelper.getCorridorName(loc);
		if(cid != null) {
			if(!corridors.containsKey(cid))
				addCorridor(new CorridorBase(loc, false));
			CorridorBase c = corridors.get(cid);
			if(c != null)
				c.addNode(r_node);
		}
	}

	/** Add a corridor to the corridor model */
	protected void addCorridor(CorridorBase c) {
		String cid = c.getName();
		corridors.put(cid, c);
		Iterator<String> it = corridors.keySet().iterator();
		for(int i = 0; it.hasNext(); i++) {
			if(cid.equals(it.next())) {
				model.add(i, cid);
				return;
			}
		}
	}

	/** Create a set of roadway nodes for the current corridor */
	public Set<R_Node> createSet() {
		final HashSet<R_Node> nodes = new HashSet<R_Node>();
		findCorridor(new Checker<R_Node>() {
			public boolean check(R_Node n) {
				nodes.add(n);
				return false;
			}
		});
		return nodes;
	}

	/** Find all r_nodes on the specified corridor */
	public R_Node findCorridor(final Checker<R_Node> ch) {
		return cache.findObject(new Checker<R_Node>() {
			public boolean check(R_Node n) {
				if(checkCorridor(n))
					return ch.check(n);
				else
					return false;
			}
		});
	}

	/** Check the corridor of an r_node */
	public boolean checkCorridor(R_Node n) {
		String c = GeoLocHelper.getCorridorName(n.getGeoLoc());
		if(c != null)
			return c.equals(corridor);
		else
			return "".equals(corridor);
	}

	/** Show the properties form for the selected proxy */
	public void showPropertiesForm() {
		if(s_model.getSelectedCount() == 1) {
			for(R_Node n: s_model.getSelected())
				showPropertiesForm(n);
		}
	}

	/** Show the properties form for the given proxy */
	protected void showPropertiesForm(R_Node n) {
		SmartDesktop desktop = session.getDesktop();
		try {
			desktop.show(new R_NodeProperties(session, n));
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/** Create a popup menu for the selected proxy object(s) */
	protected JPopupMenu createPopup() {
		int n_selected = s_model.getSelectedCount();
		if(n_selected < 1)
			return null;
		if(n_selected == 1) {
			for(R_Node n: s_model.getSelected())
				return createSinglePopup(n);
		}
		JPopupMenu p = new JPopupMenu();
		p.add(new javax.swing.JLabel("" + n_selected + " R_Nodes"));
		return p;
	}

	/** Create a popup menu for a single selection */
	protected JPopupMenu createSinglePopup(R_Node proxy) {
		JPopupMenu p = new JPopupMenu();
		p.add(makeMenuLabel(getDescription(proxy)));
		p.addSeparator();
		p.add(new PropertiesAction<R_Node>(proxy) {
			protected void do_perform() {
				showPropertiesForm();
			}
		});
		return p;
	}

	/** Find the map geo location for a proxy */
	public MapGeoLoc findGeoLoc(R_Node proxy) {
		if("".equals(corridor) || checkCorridor(proxy))
			return super.findGeoLoc(proxy);
		else
			return null;
	}

	/** Get the GeoLoc for the specified proxy */
	protected GeoLoc getGeoLoc(R_Node proxy) {
		return proxy.getGeoLoc();
	}

	/** Create a GeoLoc snapped to nearest corridor */
	public GeoLoc createGeoLoc(int easting, int northing, boolean cd_road) {
		GeoLoc loc = null;
		double distance = Double.POSITIVE_INFINITY;
		for(CorridorBase c: corridors.values()) {
			boolean cd = c.getFreeway().getRClass() == Road.CD_ROAD;
			if((cd_road && !cd) || (cd && !cd_road))
				continue;
			ClientGeoLoc l = createGeoLoc(c, easting, northing);
			if(l != null && l.getDistance() < distance) {
				loc = l;
				distance = l.getDistance();
			}
		}
		return loc;
	}

	/** Create the nearest GeoLoc for the given corridor.
	 * @param c Corridor to search.
	 * @param east Easting of selected point.
	 * @param north Northing of selected point.
	 * @return ClientGeoLoc snapped to corridor, or null if not found. */
	protected ClientGeoLoc createGeoLoc(CorridorBase c, int east,
		int north)
	{
		R_Node n0 = null;
		R_Node n1 = null;
		R_Node n_prev = null;
		double n_meters = Double.POSITIVE_INFINITY;
		for(R_Node n: c.getNodes()) {
			if(isContinuityBreak(n)) {
				n_prev = null;
				continue;
			}
			if(n_prev != null) {
				double m = calcDistance(n_prev, n, east, north);
				if(m < n_meters) {
					n0 = n_prev;
					n1 = n;
					n_meters = m;
				}
			}
			n_prev = n;
		}
		if(n0 != null)
			return createGeoLoc(n0, n1, east, north, n_meters);
		else
			return null;
	}

	/** Check if a given node is a continuity break */
	protected boolean isContinuityBreak(R_Node n) {
		if(n.getNodeType() == R_NodeType.ACCESS.ordinal())
			return true;
		if(n.getTransition() == R_NodeTransition.COMMON.ordinal())
			return true;
		return false;
	}

	/** Calculate the distance from a point to the given line segment.
	 * @param n0 First r_node
	 * @param n1 Second (adjacent) r_node.
	 * @param e Selected Easting.
	 * @param n Selected Northing.
	 * @return Distance (meters) from segment to selected point. */
	protected double calcDistance(R_Node n0, R_Node n1, int e, int n) {
		GeoLoc l0 = n0.getGeoLoc();
		GeoLoc l1 = n1.getGeoLoc();
		if(GeoLocHelper.isNull(l0) || GeoLocHelper.isNull(l1))
			return Double.POSITIVE_INFINITY;
		int x0 = GeoLocHelper.getTrueEasting(l0);
		int y0 = GeoLocHelper.getTrueNorthing(l0);
		int x1 = GeoLocHelper.getTrueEasting(l1);
		int y1 = GeoLocHelper.getTrueNorthing(l1);
		LineSegment2D seg = new LineSegment2D(x0, y0, x1, y1);
		return seg.distanceTo(e, n);
	}

	/** Create a GeoLoc projected onto the line between two nodes.
	 * @param n0 First node.
	 * @param n1 Second (adjacent) node.
	 * @param e Selected Easting.
	 * @param n Selected Northing.
	 * @param d Distance (meters).
	 * @return ClientGeoLoc snapped to corridor, or null if not found. */
	protected ClientGeoLoc createGeoLoc(R_Node n0, R_Node n1, int e, int n,
		double dist)
	{
		GeoLoc l0 = n0.getGeoLoc();
		GeoLoc l1 = n1.getGeoLoc();
		if(GeoLocHelper.isNull(l0) || GeoLocHelper.isNull(l1))
			return null;
		int x0 = GeoLocHelper.getTrueEasting(l0);
		int y0 = GeoLocHelper.getTrueNorthing(l0);
		int x1 = GeoLocHelper.getTrueEasting(l1);
		int y1 = GeoLocHelper.getTrueNorthing(l1);
		LineSegment2D seg = new LineSegment2D(x0, y0, x1, y1);
		Vector2D pnt = seg.snap(e, n);
		return new ClientGeoLoc(l0.getFreeway(), l0.getFreeDir(),
			(int)pnt.x, (int)pnt.y, dist);
	}
}
