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

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import us.mn.state.dot.tms.Font;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.DmsColor;
import us.mn.state.dot.tms.DmsSignGroup;
import us.mn.state.dot.tms.DmsSignGroupHelper;
import us.mn.state.dot.tms.QuickMessage;
import us.mn.state.dot.tms.SignGroup;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.dms.SignPixelPanel;
import us.mn.state.dot.tms.client.proxy.ProxyListModel;
import us.mn.state.dot.tms.client.widget.SmartDesktop;
import us.mn.state.dot.tms.utils.wysiwyg.WFontCache;
import us.mn.state.dot.tms.utils.wysiwyg.WGraphicCache;
import us.mn.state.dot.tms.utils.wysiwyg.WMessage;
import us.mn.state.dot.tms.utils.wysiwyg.WPage;
import us.mn.state.dot.tms.utils.wysiwyg.WRaster;
import us.mn.state.dot.tms.utils.wysiwyg.WState;
import us.mn.state.dot.tms.utils.wysiwyg.WToken;
import us.mn.state.dot.tms.utils.wysiwyg.WTokenList;
import us.mn.state.dot.tms.utils.I18N;
import us.mn.state.dot.tms.utils.MultiConfig;
import us.mn.state.dot.tms.utils.MultiString;

/**
 * WYSIWYG DMS Message Editor Controller for handling exchanges between the
 * editor GUI form and the renderer.
 *
 * @author Gordon Parikh - SRF Consulting
 */
@SuppressWarnings("serial")

public class WController {
	/** Editing modes */
	private final static String MODE_TEXT = "text";
	private final static String MODE_GRAPHIC = "graphic";
	private final static String MODE_COLORRECT = "color_rectangle";
	private final static String MODE_TEXTRECT = "text_rectangle";
	private final static String MODE_MULTITAG = "multi_tag";
	private String editingMode = MODE_TEXT;
	
	/** Client Session and SmartDesktop */
	private Session session;
	private SmartDesktop desktop;
	
	/** Keep a handle to the editor form and sign pixel panel (TODO may 
	 * change) for any updates we need to make from here */
	private WMsgEditorForm editor;
	private WImagePanel signPanel;
	
	/** Cursor that will change depending on mode, etc. */
	// TODO should we make these final or have an initCursors method???
	private final Cursor textCursor = new Cursor(Cursor.TEXT_CURSOR);
	private final Cursor graphicCursor = new Cursor(Cursor.HAND_CURSOR);
	private final Cursor colorRectCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
	private final Cursor textRectCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
	private final Cursor multiTagCursor = new Cursor(Cursor.DEFAULT_CURSOR);
	private final Cursor moveCursor = new Cursor(Cursor.MOVE_CURSOR);
	private Cursor cursor = textCursor;
	
	/** Sign/Group and Message being edited */
	private DMS sign;
	private SignGroup sg;
	private QuickMessage qm;
	private MultiString multiString;
	private String multiStringText = "";
	
	/** MultiConfig for config-related stuff  */
	private MultiConfig multiConfig;
	
	/** Token lists for helping with cursor placement and stuff */
	private WTokenList tokensBefore = new WTokenList();
	private WTokenList tokensSelected = new WTokenList();
	private WTokenList tokensAfter = new WTokenList();
	
	/** Font and graphics caches */
	private WFontCache fontCache = new WFontCache();
	private WGraphicCache graphicCache = new WGraphicCache();
	
	/** WMessage for working with rendered message */
	private WMessage wmsg;
	
	/** Current Font
	 *  TODO need some model for this, I don't think it can just be one */
	private Font currentFont;
	
	/** Current Colors */
	private DmsColor fgColor;
	private DmsColor bgColor;
	
	/** Page list */
	private WPageList pageList;
	
	/** DMS List (for sign groups) */
	private Map<String,DMS> dmsList;
	private JComboBox<String> dms_list;
	String[] dmsNames;
	
	/** Currently selected page (defaults to first available) */
	private int selectedPageIndx = 0;
	private WPage selectedPage;
	
	public WController() {
		// empty controller - everything will be set later as it is available
	}	
	
	public WController(WMsgEditorForm e) {
		init(e);
	}	
	
	public WController(WMsgEditorForm e, DMS d) {
		init(e);
		setSign(d);
	}
	
	public WController(WMsgEditorForm e, SignGroup g) {
		init(e);
		setSignGroup(g);
	}
	
	public WController(WMsgEditorForm e, QuickMessage q, DMS d) {
		init(e);
		setSign(d);
		setQuickMessage(q);
	}
	
	public WController(WMsgEditorForm e, QuickMessage q, SignGroup g) {
		init(e);
		setSignGroup(g);
		setQuickMessage(q);
	}
	
	/** Perform some initialization on the controller. Sets the editor form
	 *  handle, sets up the mouse cursor, etc.
	 */
	public void init(WMsgEditorForm e) {
		editor = e;
		session = editor.getSession();
		desktop = session.getDesktop();
		
		// initialize the cursor, starting in text mode
		cursor = new Cursor(Cursor.TEXT_CURSOR);
	}
	
	/** Set the sign being used */
	public void setSign(DMS d) {
		sign = d;
		
		// generate the MultiConfig for the sign
		if (sign != null) {
			try {
				setMultiConfig(MultiConfig.from(sign));
			} catch (TMSException e1) {
				// TODO what to do??
			}
		} else {
			multiConfig = null;
		}
		update();
	}
	
	/** Set the sign group being used */
	public void setSignGroup(SignGroup g) {
		sg = g;
		
		if (sg != null) {
			// generate the MultiConfig for the sign group
			setMultiConfig(MultiConfig.from(sg));
			
			// generate the list of signs in the group
			makeSignListForGroup(true);
		} else {
			multiConfig = null;
			sign = null;
		}
		update();
	}
	
	/** Set the quick message being edited */
	public void setQuickMessage(QuickMessage q) {
		qm = q;
		
		// get the MULTI string text from the quick message
		if (qm != null)
			multiStringText = qm.getMulti();
		else
			multiStringText = "";
		update();
	}
	
	/** Use the AffineTransform object from the editor's SignPixelPanel to
	 *  calculate the coordinates of the click on the sign itself, rather than
	 *  the JPanel in which it resides.
	 */
//	private Point2D transformSignCoordinates(int x, int y) {
//		// update the editor to make sure everything is in place
//		update();
//		
//		// get the AffineTransform object from the pixel panel
//		if (editor != null) {
//			
//			AffineTransform t = pixel_pnl.getTransform();
//			
//			// calculate the adjusted coordinates of the click
//			if (t != null) {
//				int tx = (int) t.getTranslateX();
//				int ty = (int) t.getTranslateY();
//				return new Point2D.Double(x-tx, y-ty);
//			}
//		}
//		return null;
//	}
	
	/** Find the closest token on the page given a set of click coordinates. */
	public void findClosestToken(int x, int y) {
		// get the sign coordinates for working with the message
		WRaster wr = selectedPage.getRaster();
		int sx = wr.cvtWysiwygToSignX(x);
		int sy = wr.cvtWysiwygToSignY(y);
		
		// find the closest token on this page
		WToken tok = null;
		Iterator<WToken> it = selectedPage.tokens();
		while (it.hasNext()) {
			WToken t = it.next();
			if (t.isInside(sx, sy)) {
				tok = t;
				break;
			}
		}
		
		// TODO test code
		// back-convert to click coordinates
		int[] cx = wr.cvtSignToWysiwygX(sx);
		int cx0 = -1, cx1 = -1;
		if (cx.length == 2) {
			cx0 = cx[0];
			cx1 = cx[1];
		} else if (cx.length == 1) {
			cx0 = cx[0];
		}
		int[] cy = wr.cvtSignToWysiwygY(sy);
		int cy0 = -1, cy1 = -1;
		if (cy.length == 2) {
			cy0 = cy[0];
			cy1 = cy[1];
		} else if (cy.length == 1) {
			cy0 = cy[0];
		}
		
		String tokStr = "NOT FOUND";
		if (tok != null) {
			tokStr = tok.toString();
		
			System.out.println(String.format(
					"Click at (%d, %d) => sign coords (%d, %d) => (%d->%d, %d->%d)",
					x, y, sx, sy, cx0, cx1, cy0, cy1));
			System.out.println(String.format(
					"Token '%s' found at coords (%d, %d) w/h (%d, %d)",
					tokStr, tok.getCoordX(), tok.getCoordY(), tok.getCoordW(), tok.getCoordH()));
			System.out.println(String.format(
					"              Param coords (%d, %d) w/h (%d, %d)", 
					tok.getParamX(), tok.getParamY(), tok.getParamW(), tok.getParamH()));
			
			// move the caret
			moveCaret(tok);
		} else {
			System.out.println(String.format(
					"Click at (%d, %d) => sign coords (%d, %d) => (%d->%d, %d->%d)",
					x, y, sx, sy, cx0, cx1, cy0, cy1));
		}
	}
	
	/** Move the caret to the spot just before the specified token. Note that
	 *  this doesn't select the token. */
	public void moveCaret(WToken tok) {
		// get a list of tokens on this page and find this token in the list
		WTokenList pgTokens = selectedPage.getTokenList();
		int tIndx = pgTokens.indexOf(tok);
		if (tIndx != -1) {
			// slice the list at the token
			tokensBefore = pgTokens.slice(0, tIndx);
			tokensAfter = pgTokens.slice(tIndx, pgTokens.size());
			
			// reset the selection
			tokensSelected.clear();
			
			// set the new caret location
			signPanel.setCaretLocation(tok);
		}
	}
	
	/** Handle a click on the main editor panel */
	public void handleClick(MouseEvent e) {
		// calculate the adjusted coordinates of the click
//		Point2D pSign = transformSignCoordinates(e.getX(), e.getY());
		
		// just print for now
//		if (pSign != null) {
		int b = e.getButton();
		int x = e.getX();
		int y = e.getY();
		
		findClosestToken(x, y);
//		}
	}
	
	/** Handle a mouse move event on the main editor panel */
	public void handleMouseMove(MouseEvent e) {
		// calculate the adjusted coordinates of the mouse pointer
//		Point2D pSign = transformSignCoordinates(e.getX(), e.getY());
		
		// just print for now
//		if (pSign != null) {
		int x = e.getX();
		int y = e.getY();
		
		// TODO test code for cursor changing
//		if (y >= 100 && y <= 160) {
//			cursor = moveCursor;
//		} else {
//			setCursorFromMode();
//		}
		update();
			
			// TODO hook this into token finding and mouse cursor changing
			
//		System.out.println(String.format(
//				"Mouse moved to (%d, %d) ...", x, y));
//		}
	}
	
	/** Handle a mouse drag event on the main editor panel */
	public void handleMouseDrag(MouseEvent e) {
		// calculate the adjusted coordinates of the mouse pointer
//		Point2D pSign = transformSignCoordinates(e.getX(), e.getY());
		
		// figure out what button was pressed when dragging
		String b = "";
		if (SwingUtilities.isLeftMouseButton(e))
			b = "left";
		else if (SwingUtilities.isRightMouseButton(e))
			b = "right";
		else if (SwingUtilities.isMiddleMouseButton(e))
			b = "middle";
		
		// just print for now
//		if (pSign != null) {
		int x = e.getX();
		int y = e.getY();
//		System.out.println(String.format(
//				"Mouse dragged with %s button to (%d, %d) ...", b, x, y));
//		}
	}
	
	/** Return a WPageList for displaying a list of pages from the message */
	public WPageList getPageList(boolean enableSelection) {
		// TODO/NOTE should we split these up?
		if (!enableSelection) {
			// return a new non-selectable list
			return new WPageList(enableSelection);
		} else {
			// create and save our page list with a selection handler
			class PageSelectionHandler implements ListSelectionListener {
				public void valueChanged(ListSelectionEvent e) {
					if (!e.getValueIsAdjusting()) {
						ListSelectionModel lsm = (ListSelectionModel) e.getSource();
						int indx = lsm.getMinSelectionIndex();
						
						if (indx != -1) {
							selectedPageIndx = indx;
							updateSelectedPage();
						}
					}
				}
			}
			pageList = new WPageList(new PageSelectionHandler());
			return pageList;
		}
	}
	
	/** Get any message text in the controller */
	public String getMultiText() {
		return multiStringText;
	}
	
	/** Edit the MULTI string text directly and update the GUI */
	public void editMulti(String multiText) {
		multiStringText = multiText;
		update();
	}
	
	/** Render the message using the current MULTI String and MultiConfig */
	private void renderMsg() {
		// update the WMessage object and re-render if we have a MultiConfig
		wmsg = new WMessage(multiStringText);
//		System.out.println(multiStringText);
		if (multiConfig != null)
			wmsg.renderMsg(multiConfig);
	}
	
	/** Update everything that needs updating */
	public void update() {
		renderMsg();
		updatePageListModel();
		updateCursor();
		
		// TODO add more stuff here eventually
	}
	
	/** Update the WPageList given the current MULTI string and MultiConfig. */
	private void updatePageListModel() {
		// update the page list and the selected page
		if (pageList != null) {
			pageList.updatePageList(multiStringText, multiConfig);
			updateSelectedPage();
		}
	}
	
	/** Update the selected page to use one in the current page_list_model. */
	private void updateSelectedPage() {
		// make sure the selected page still exists
		if (selectedPageIndx >= pageList.getNumPages()) 
			selectedPageIndx = pageList.getNumPages()-1;
		
		// rerender the messsage and get the page from our wmsg
		renderMsg();
		
		if (wmsg.isValid())
			setSelectedPage(wmsg.getPage(selectedPageIndx+1));
		
		if (editor != null) {
			editor.setPageNumberLabel(getPageNumberLabel(selectedPageIndx));
			editor.updateWysiwygPanel();
		}
	}

	/** Set the currently selected page */
	public void setSelectedPage(WPage pg) {
		selectedPage = pg;
	}
	
	public WPage getSelectedPage() {
		return selectedPage;
	}
	
	/** Get the label indicating the page number */
	public static String getPageNumberLabel(int pn) {
		try {
			return String.format(I18N.get("wysiwyg.editor.page_number"),
					pn+1);
		} catch (IllegalFormatException e) {
			return "Page" + pn+1;
		}
	}
	
	/** Update the cursor that is active over the sign pixel panel */
	private void updateCursor() {
		if (signPanel != null) {
			signPanel.setCursor(cursor);
		}
	}
	
	/** Set the cursor type based on the editing mode */
	private void setCursorFromMode() {
		if (editingMode == MODE_TEXT)
			cursor = textCursor;
		else if (editingMode == MODE_GRAPHIC)
			cursor = graphicCursor;
		else if (editingMode == MODE_COLORRECT)
			cursor = colorRectCursor;
		else if (editingMode == MODE_TEXTRECT)
			cursor = textRectCursor;
		else if (editingMode == MODE_MULTITAG)
			cursor = multiTagCursor;
	}
	
	/** Get a JComboBox containing a list of sign names (only applies when
	 * editing a group).
	 * TODO same note as with JList above - should maybe abstract this more
	 * but whatever... 
	 */
	public JComboBox<String> getSignGroupComboBox() {
		if (sg != null) {
			// generate a list of signs in the sign group
			makeSignListForGroup(true);
			
			// define a selection handler for the combo box
			class SignSelectionListener implements ActionListener {
				@SuppressWarnings("unchecked")
				public void actionPerformed(ActionEvent e) {
					JComboBox<String> cb = (JComboBox<String>) e.getSource();
					String dmsName = (String) cb.getSelectedItem();
					// TODO - do we need to catch any exceptions here?
					sign = dmsList.get(dmsName);
					
					// TODO a bunch more needs to happen here in addition to this
					// (need to remake page list and stuff)
					updatePageListModel();
					if (editor != null) {
						editor.updateWysiwygPanel();
					}
				}
			}
			
			// setup and return the combo box
			dms_list = new JComboBox<String>(dmsNames);
			dms_list.addActionListener(new SignSelectionListener());
		}
		return dms_list;
	}
	
	/** Get a list of signs in the group. If setSign is true, the controller's
	 *  "sign" attribute be set to the first sign in the group.
	 */
	public void makeSignListForGroup(boolean setSign) {
		// get the list of signs in the sign group
		// look through the DmsSignGroups to find all signs with this group
		dmsList = new HashMap<String,DMS>();
		Iterator<DmsSignGroup> dsgit = DmsSignGroupHelper.iterator();
		while (dsgit.hasNext()) {
			DmsSignGroup dsg = dsgit.next();
			if (dsg.getSignGroup() == sg) {
				DMS dms = dsg.getDms();
				dmsList.put(dms.getName(), dms);
			}
		}
		
		// get the list of sign names and sort them alphabetically
		dmsNames = Arrays.stream(dmsList.keySet().toArray()).
				toArray(String[]::new);
		Arrays.sort(dmsNames);
		
		// set the current sign to the first one in the group if desired
		if (setSign && dmsNames.length > 0)
			sign = dmsList.get(dmsNames[0]);
	}
	
	public void activateTextMode() {
		// put the cursor in text mode then update everything
		editingMode = MODE_TEXT;
		setCursorFromMode();
		update();
	}
	
	public void activateGraphicMode() {
		// put the cursor in ?? hand ?? mode then update everything
		editingMode = MODE_GRAPHIC;
		setCursorFromMode();
		update();
	}
	
	public void activateTextRectangleMode() {
		// put the cursor in crosshair mode then update everything
		editingMode = MODE_TEXTRECT;
		setCursorFromMode();
		update();
	}
	
	public void activateColorRectangleMode() {
		// put the cursor in crosshair mode then update everything
		editingMode = MODE_COLORRECT;
		setCursorFromMode();
		update();
	}
		
	public void activateMultiTagMode() {
		// put the cursor in ?? default ?? mode then update everything
		editingMode = MODE_MULTITAG;
		setCursorFromMode();
		update();
	}
	
	public Cursor getCursor() {
		return cursor;
	}
	
	public MultiConfig getMultiConfig() {
		return multiConfig;
	}
	
	public ProxyListModel<Font> getFontModel() {
		if (session != null) {
			return session.getSonarState().getDmsCache().getFontModel();
		} return null;
	}
	
	public void setCurrentFont(Font f) {
		currentFont = f;
	}
	
	public Font getCurrentFont() {
		return currentFont;
	}
	
	public void setSignPanel(WImagePanel sp) {
		signPanel = sp;
	}
	
	private void setMultiConfig(MultiConfig mc) {
		multiConfig = mc;
		setFontFromConfig();
		setColorsFromConfig();
	}
	
	private void setFontFromConfig() {
		if (multiConfig != null) {
			currentFont = multiConfig.getDefaultFont();
		}
	}
	
	private void setColorsFromConfig() {
		if (multiConfig != null) {
			fgColor = multiConfig.getDefaultFG();
			bgColor = multiConfig.getDefaultBG();
		}
	}
	
	public void setForegroundColor(DmsColor c) {
		fgColor = c;
	}
		
	public void setBackgroundColor(DmsColor c) {
		bgColor = c;
	}
	
	public DmsColor getForegroundColor() {
		return fgColor;
	}
		
	public DmsColor getBackgroundColor() {
		return bgColor;
	}
	
	public Session getSession() {
		return session;
	}
		
	public SmartDesktop getDesktop() {
		return desktop;
	}
	
	/* Get the current DMS object */
	public DMS getSign() {
		return sign;
	}
	
}