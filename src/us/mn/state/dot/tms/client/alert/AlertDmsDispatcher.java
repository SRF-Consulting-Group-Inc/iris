/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2020  SRF Consulting Group, Inc.
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
package us.mn.state.dot.tms.client.alert;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.GeoLocHelper;
import us.mn.state.dot.tms.IpawsAlert;
import us.mn.state.dot.tms.IpawsAlertDeployer;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.dms.DmsImagePanel;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.IPanel;
import us.mn.state.dot.tms.client.widget.IWorker;
import us.mn.state.dot.tms.utils.I18N;
import us.mn.state.dot.tms.utils.MultiConfig;

/**
 * An alert DMS dispatcher is a GUI panel for dispatching and reviewing
 * DMS involved in an automated alert deployment.
 * 
 * @author Gordon Parikh
 */
@SuppressWarnings("serial")
public class AlertDmsDispatcher extends IPanel {
	/** Client session */
	private final Session session;
	
	/** Alert manager */
	private final AlertManager manager;

	/** Currently selected alert (deployer) */
	private IpawsAlertDeployer selectedAlertDepl;
	
	/** Currently selected alert */
	private IpawsAlert selectedAlert;
	
	/** Cache of DMS */
	private final TypeCache<DMS> dmsCache;
	
	/** DMS list model */
	private final DefaultListModel<DMS> dmsListModel;
	
	/** List of DMS included in this alert */
	private final JList<DMS> dmsList;
	
	/** Tab pane containing DMS renderings */
	private JTabbedPane dmsPane;
	
	/** Sign Image panel width */
	private static final int DMS_PNL_W = 200;
	
	/** Sign Image panel height */
	private static final int DMS_PNL_H = 80;
	
	/** Image panel for current message on sign */
	private DmsImagePanel currentMsgPnl;
	
	/** Image panel for alert message */
	private DmsImagePanel alertMsgPnl;
	
	/** Button to open DMS message stack */
	private JButton signMsgButton;
	
	/** Currently selected DMS */
	private DMS selectedDms;
	
	/** Renderer for DMS list */
	private class DmsListRenderer extends JLabel
				implements ListCellRenderer<DMS> {
		public DmsListRenderer() {
			setOpaque(true);
		}
		
		@Override
		public Component getListCellRendererComponent(
				JList<? extends DMS> list, DMS dms, int index,
				boolean isSelected, boolean cellHasFocus) {
			// return a JLabel that contains the DMS name and location
			setText(dms.getName() + " - " +
						GeoLocHelper.getLocation(dms.getGeoLoc()));
			if (isSelected)
				setBackground(list.getSelectionBackground());
			else
				setBackground(list.getBackground());
			return this;
		}
	}
	
	/** Selection listener for handling DMS selection. */
	private class DmsSelectionListener implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
	        if (!e.getValueIsAdjusting()) {
	        	ListSelectionModel lsm = (ListSelectionModel) e.getSource();
	        	int indx = lsm.getMinSelectionIndex();
	        	if (indx != -1) {
	        		DMS sel = dmsListModel.get(indx);
	        		System.out.println("Selected DMS: " + sel.getName());
	        		setSelectedDms(sel);
	        	}
	        }
		}
	}
	
	/** Comparator for sorting list of DMS by name (case insensitive). */
	private class DmsComparator implements Comparator<DMS> {
		@Override
		public int compare(DMS dms0, DMS dms1) {
			return String.CASE_INSENSITIVE_ORDER.compare(
					dms0.getName(), dms1.getName());
		}
	}
	
	/** Action to open DMS message stack */
	private IAction signMsgAction = new IAction("alert.dms.messages") {
		@Override
		protected void doActionPerformed(ActionEvent ev) throws Exception {
			System.out.println("Opening DMS message stack...");
			// TODO
		}
	};
	
	/** Create a new alert DMS dispatcher */
	public AlertDmsDispatcher(Session s, AlertManager m) {
		session = s;
		manager = m;
		
		// setup DMS list
		dmsCache = session.getSonarState().getDmsCache().getDMSs();
		dmsListModel = new DefaultListModel<DMS>();
		dmsList = new JList<DMS>(dmsListModel);
		dmsList.setCellRenderer(new DmsListRenderer());
		dmsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dmsList.getSelectionModel().addListSelectionListener(
				new DmsSelectionListener());
		
		// setup image panels for rendering DMS
		dmsPane = new JTabbedPane();
		currentMsgPnl = new DmsImagePanel(DMS_PNL_W, DMS_PNL_H, true);
		alertMsgPnl = new DmsImagePanel(DMS_PNL_W, DMS_PNL_H, true);
		
		// TODO these should be centered
		dmsPane.add(I18N.get("alert.dms.current_msg"), currentMsgPnl);
		dmsPane.add(I18N.get("alert.dms.alert_msg"), alertMsgPnl);
		
		// button for opening msg stack (TODO - need that)
		signMsgButton = new JButton(signMsgAction);
	}
	
	/** Initialize the widgets on the panel */
	@Override
	public void initialize() {
		super.initialize();
		setTitle(I18N.get("alert.dms"));
		add(new JScrollPane(dmsList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), Stretch.DOUBLE);
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBorder(BorderFactory.createTitledBorder(
				I18N.get("alert.dms.selected")));
		p.add(dmsPane);
		p.add(Box.createVerticalStrut(10));
		p.add(signMsgButton);
		setRowCol(row+1, 0);
		add(p, Stretch.DOUBLE);
	}
	
	/** Set the selected alert, updating the list of DMS. */
	public void setSelectedAlert(IpawsAlertDeployer iad, IpawsAlert ia) {
		selectedAlertDepl = iad;
		selectedAlert = ia;
		
		// get the DMS list from the alert - do it in a worker thread since
		// it might take a second
		IWorker<ArrayList<DMS>> dmsWorker = new IWorker<ArrayList<DMS>>() {
			@Override
			protected ArrayList<DMS> doInBackground() throws Exception {
				// TODO this should change to getDeployedDms (once we fill
				// that)
				String[] dmsNames = selectedAlertDepl.getAutoDms();
				
				// get the DMS objects from the list of names
				ArrayList<DMS> dmsList = new ArrayList<DMS>();
				for (String n: dmsNames) {
					DMS d = dmsCache.lookupObject(n);
					if (d != null)
						dmsList.add(d);
				}
				// sort DMS by name
				dmsList.sort(new DmsComparator());
				return dmsList;
			}
			
			@Override
			public void done() {
				ArrayList<DMS> dmsList = getResult();
				if (dmsList != null)
					updateListModel(dmsList);
			}
		};
		dmsWorker.execute();
	}
	
	/** Clear the selected alert. */
	public void clearSelectedAlert() {
		selectedAlertDepl = null;
		selectedAlert = null;
		dmsListModel.clear();
	}
	
	/** Update the DMS list model with the list of DMS provided. */
	private void updateListModel(ArrayList<DMS> dmsList) {
		dmsListModel.clear();
		for (DMS d: dmsList)
			dmsListModel.addElement(d);
	}
	
	/** Set the selected DMS */
	private void setSelectedDms(DMS dms) {
		selectedDms = dms;
		
		// TODO this isn't updating right away
		if (selectedDms != null) {
			// set the image on the DMS panels
			try {
				// first set the MultiConfig for this sign
				MultiConfig mc = MultiConfig.from(selectedDms);
				currentMsgPnl.setMultiConfig(mc);
				alertMsgPnl.setMultiConfig(mc);
				
				// then set the MULTI of the message
				// TODO for now we're using the same for both current and alert
				String multi = selectedAlertDepl.getAutoMulti();
				currentMsgPnl.setMulti(multi);
				alertMsgPnl.setMulti(multi);
			} catch (TMSException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else {
			// if deselected, clear
			currentMsgPnl.clearMultiConfig();
			currentMsgPnl.clearMulti();
			alertMsgPnl.clearMultiConfig();
			alertMsgPnl.clearMulti();
		}
	}
}















