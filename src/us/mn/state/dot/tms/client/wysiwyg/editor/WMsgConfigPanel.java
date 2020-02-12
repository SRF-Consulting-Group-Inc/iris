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

package us.mn.state.dot.tms.client.wysiwyg.editor;

import static us.mn.state.dot.tms.units.Distance.Units.INCHES;
import static us.mn.state.dot.tms.units.Distance.Units.MILLIMETERS;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JLabel;

import us.mn.state.dot.tms.Font;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.widget.IPanel;
import us.mn.state.dot.tms.units.Distance;
import us.mn.state.dot.tms.utils.HexString;
import us.mn.state.dot.tms.utils.I18N;
import us.mn.state.dot.tms.utils.MultiConfig;

/**
 * WYSIWYG DMS Message Editor Config tab with information about the sign
 * configuration corresponding to the current sign.
 *
 * @author Gordon Parikh  - SRF Consulting
 */
@SuppressWarnings("serial")


public class WMsgConfigPanel extends IPanel {
	/** Controller for updating renderings */
	private WController controller;

	/** Icon size */
	static private final int ICON_SIZE = 24;

	/** Icon for colors */
	static private class ColorIcon implements Icon {
		private final Color color;
		private ColorIcon(int rgb) {
			color = new Color(rgb);
		}
		public int getIconHeight() {
			return ICON_SIZE;
		}
		public int getIconWidth() {
			return ICON_SIZE;
		}
		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(color);
			g.fillRect(x, y, ICON_SIZE, ICON_SIZE);
		}
	}

	/** Get tiny distance units to use for display */
	static private Distance.Units distUnitsTiny() {
		return SystemAttrEnum.CLIENT_UNITS_SI.getBoolean()
		     ? Distance.Units.CENTIMETERS : INCHES;
	}

	/** Unknown value string */
	static private final String UNKNOWN = "???";

	/** Format a string field */
	static private String formatString(String s) {
		if (s != null && s.length() > 0)
			return s;
		else
			return UNKNOWN;
	}

	/** Format millimeter units for display */
	static private String formatMM(int i) {
		return (i > 0) ? i + " " + I18N.get("units.mm") : UNKNOWN;
	}

	/** Format pixel units for display */
	static private String formatPixels(int i) {
		if (i > 0)
			return i + " " + I18N.get("units.pixels");
		else if (0 == i)
			return I18N.get("units.pixels.variable");
		else
			return UNKNOWN;
	}

	/** Sign face width label */
	private JLabel f_width_lbl = createValueLabel();

	/** Sign face height label */
	private JLabel f_height_lbl = createValueLabel();

	/** Horizontal border label */
	private JLabel h_border_lbl = createValueLabel();

	/** Vertical border label */
	private JLabel v_border_lbl = createValueLabel();

	/** Horizontal pitch label */
	private JLabel h_pitch_lbl = createValueLabel();

	/** Vertical pitch label */
	private JLabel v_pitch_lbl = createValueLabel();

	/** Sign width (pixels) label */
	private JLabel p_width_lbl = createValueLabel();

	/** Sign height (pixels) label */
	private JLabel p_height_lbl = createValueLabel();

	/** Character width label */
	private JLabel c_width_lbl = createValueLabel();

	/** Character height label */
	private JLabel c_height_lbl = createValueLabel();

	/** Monochrome foreground label */
	private JLabel m_foreground_lbl = createValueLabel();

	/** Monochrome background label */
	private JLabel m_background_lbl = createValueLabel();

	/** Color scheme label */
	private JLabel c_scheme_lbl = createValueLabel();
	
	/** Default Font label */
	private JLabel font_lbl = createValueLabel();

	/** Font height label */
	private JLabel font_height_lbl = IPanel.createValueLabel();

	/** User session */
	private Session session;
	
	/** MultiConfig for sign(s) being edited */
	private MultiConfig config;
	
	/** Create a new MULTI-mode panel */
	public WMsgConfigPanel(WController c) {
		controller = c;
		session = controller.getSession();
		config = controller.getMultiConfig();
		
		// initialize the panel
		initialize();
		
		
		// TODO try overriding PropConfiguration and replacing the methods with ones that use MultiConfig
	}
	
	/** Initialize the widgets on the form */
	@Override
	public void initialize() {
		super.initialize();
		add("dms.face.width");
		add(f_width_lbl, Stretch.LAST);
		add("dms.face.height");
		add(f_height_lbl, Stretch.LAST);
		add("dms.border.horiz");
		add(h_border_lbl, Stretch.LAST);
		add("dms.border.vert");
		add(v_border_lbl, Stretch.LAST);
		add("dms.pitch.horiz");
		add(h_pitch_lbl, Stretch.LAST);
		add("dms.pitch.vert");
		add(v_pitch_lbl, Stretch.LAST);
		add("dms.pixel.width");
		add(p_width_lbl, Stretch.LAST);
		add("dms.pixel.height");
		add(p_height_lbl, Stretch.LAST);
		add("dms.char.width");
		add(c_width_lbl, Stretch.LAST);
		add("dms.char.height");
		add(c_height_lbl, Stretch.LAST);
		add("dms.monochrome.foreground");
		add(m_foreground_lbl, Stretch.LAST);
		add("dms.monochrome.background");
		add(m_background_lbl, Stretch.LAST);
		add("dms.color.scheme");
		add(c_scheme_lbl, Stretch.LAST);
		add("dms.font.default");
		add(font_lbl, Stretch.LAST);
		add("dms.font.height");
		add(font_height_lbl, Stretch.LAST);
		updateAttribute(null);
	}

	/** Update one attribute on the form tab */
	public void updateAttribute(String a) {
		MultiConfig sc = config;
		f_width_lbl.setText(formatMM(sc.getFaceWidth()));
		f_height_lbl.setText(formatMM(sc.getFaceHeight()));
		h_border_lbl.setText(formatMM(sc.getBorderHoriz()));
		v_border_lbl.setText(formatMM(sc.getBorderVert()));
		h_pitch_lbl.setText(formatMM(sc.getPitchHoriz()));
		v_pitch_lbl.setText(formatMM(sc.getPitchVert()));
		p_width_lbl.setText(formatPixels(sc.getPixelWidth()));
		p_height_lbl.setText(formatPixels(sc.getPixelHeight()));
		c_width_lbl.setText(formatPixels(sc.getCharWidth()));
		c_height_lbl.setText(formatPixels(sc.getCharHeight()));
		m_foreground_lbl.setText(HexString.format(
			sc.getDefaultFG().rgb(), 6));
		m_foreground_lbl.setIcon(new ColorIcon(
			sc.getDefaultFG().rgb()));
		m_background_lbl.setText(HexString.format(
			sc.getDefaultBG().rgb(), 6));
		m_background_lbl.setIcon(new ColorIcon(
			sc.getDefaultBG().rgb()));
		c_scheme_lbl.setText(sc.getColorScheme().description);
		font_lbl.setText(sc.getDefaultFont().getName());
		font_height_lbl.setText(calculateFontHeight());
	}

	/** Calculate the height of the default font on the sign */
	private String calculateFontHeight() {
		MultiConfig sc = config;
		Font f = sc.getDefaultFont();
		if (f != null) {
			int pv = sc.getPitchVert();
			int h = f.getHeight();
			if (h > 0 && pv > 0) {
				float mm = (h - 0.5f) * pv;
				Distance fh = new Distance(mm, MILLIMETERS);
				return formatFontHeight(fh);
			}
		}
		return UNKNOWN;
	}

	/** Format the font height for display */
	private String formatFontHeight(Distance fh) {
		Distance.Formatter df = new Distance.Formatter(1);
		return df.format(fh.convert(distUnitsTiny()));
	}
	
}