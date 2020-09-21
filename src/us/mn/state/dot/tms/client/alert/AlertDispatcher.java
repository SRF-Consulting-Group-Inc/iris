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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;

import org.json.JSONObject;

import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.IpawsAlert;
import us.mn.state.dot.tms.IpawsAlertDeployer;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxySelectionListener;
import us.mn.state.dot.tms.client.proxy.ProxySelectionModel;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.ILabel;
import us.mn.state.dot.tms.client.widget.IPanel;
import us.mn.state.dot.tms.client.widget.IPanel.Stretch;
import us.mn.state.dot.tms.utils.I18N;

/**
 * An alert dispatcher is a GUI panel for dispatching and reviewing automated
 * alerts for deployment on DMS.
 * 
 * NOTE this would need changing to let the alert tab handle other types of
 * alerts (we would need a new Alert parent SONAR object - not sure what that
 * would look like yet). 
 *
 * @author Gordon Parikh
 */
@SuppressWarnings("serial")
public class AlertDispatcher extends IPanel {
	
	/** Client session */
	private final Session session;
	
	/** Alert manager */
	private final AlertManager manager;

	/** Alert selection model */
	private final ProxySelectionModel<IpawsAlertDeployer> alertSelMdl;
	
	/** Alert selection listener */
	private final ProxySelectionListener alertSelLstnr =
			new ProxySelectionListener() {
		public void selectionChanged() {
			selectAlert();
		}
	};
	
	/** Currently selected alert (deployer) */
	private IpawsAlertDeployer selectedAlertDepl;
	
	/** Currently selected alert */
	private IpawsAlert selectedAlert;
	
	/** Cache of IPAWS alerts */
	private final TypeCache<IpawsAlert> alertCache;
	
	/** Labels */
	/** Name label */
	private final JLabel idLbl = createValueLabel();
	
	/** Event label */
	private final JLabel eventLbl = createValueLabel();
	
	/** Urgency label */
	private final JLabel urgencyLbl = createValueLabel();
	
	/** Severity label */
	private final JLabel serverityLbl = createValueLabel();
	
	/** Certainty label */
	private final JLabel certaintyLbl = createValueLabel();
	
	/** Status label */
	private final JLabel statusLbl = createValueLabel();
	
	/** Onset label */
	private final JLabel onsetLbl = createValueLabel();
	
	/** Expires label */
	private final JLabel expiresLbl = createValueLabel();
	
	/** Treat the area description label label a little differently */
	private final ILabel areaDescKeyLbl;
	
	/** Area description label */
	private final JLabel areaDescLbl = createValueLabel();
	
	/** Available width for area description label */
	private Integer areaDescLblWidth;
	
	/** Edit deployment button */
	private JButton editBtn;
	
	/** Cancel deployment button */
	private JButton cancelBtn;
	
	/** Alert DMS dispatcher for deploying/reviewing DMS used for this alert*/
	private final AlertDmsDispatcher dmsDispatcher;
	
	/** Create a new alert dispatcher. */
	public AlertDispatcher(Session s, AlertManager m) {
		super();
		session = s;
		manager = m;
		alertSelMdl = manager.getSelectionModel();
		alertSelMdl.setAllowMultiple(false);
		alertCache = session.getSonarState().getIpawsAlertCache();
		alertSelMdl.addProxySelectionListener(alertSelLstnr);
		areaDescKeyLbl = new ILabel("alert.area_desc");
		editBtn = new JButton(editDeployment);
		
		// make both buttons the same size (it looks nicer...)
		cancelBtn = new JButton(cancelDeployment);
		editBtn.setPreferredSize(cancelBtn.getPreferredSize());
		
		dmsDispatcher = new AlertDmsDispatcher(session, manager);
	}

	/** Initialize the widgets on the panel */
	@Override
	public void initialize() {
		super.initialize();
		setTitle(I18N.get("alert.selected"));
		add(editBtn, Stretch.TALL);
		add("alert.id");
		add(idLbl, Stretch.LAST);
		add(cancelBtn, Stretch.TALL);
		add("alert.event");
		add(eventLbl, Stretch.LAST);
		add("alert.urgency");
		add(urgencyLbl, Stretch.LAST);
		add("alert.severity");
		add(serverityLbl, Stretch.LAST);
		add("alert.certainty");
		add(certaintyLbl, Stretch.LAST);
		add("alert.status");
		add(statusLbl, Stretch.LAST);
		add("alert.onset");
		add(onsetLbl, Stretch.LAST);
		add("alert.expires");
		add(expiresLbl, Stretch.LAST);
		add(areaDescKeyLbl, Stretch.NONE);
		add(areaDescLbl, Stretch.LAST);
		
		dmsDispatcher.initialize();
		add(dmsDispatcher, Stretch.DOUBLE);
		
		alertSelMdl.addProxySelectionListener(alertSelLstnr);
	}
	
	/** Action to edit a current deployment (opens the deploy dialog) */
	private IAction editDeployment = new IAction("alert.deploy.edit") {
		@Override
		protected void doActionPerformed(ActionEvent ev) throws Exception {
			System.out.println("Editing...");
			// TODO
		}
		
	};
	
	/** Action to cancel a current deployment (removes alert messages from
	 *  signs).
	 */
	private IAction cancelDeployment = new IAction("alert.deploy.cancel") {
		@Override
		protected void doActionPerformed(ActionEvent ev) throws Exception {
			System.out.println("Canceling...");
			// TODO
		}
		
	};
	
	/** Set the selected alert in the list (for calling from other code). */
	public void selectAlert(IpawsAlertDeployer iad) {
		alertSelMdl.setSelected(iad);
	}
	
	/** Update the display to reflect the alert selected. */
	private void selectAlert() {
		Set<IpawsAlertDeployer> sel = alertSelMdl.getSelected();
		if (sel.size() == 0)
			clearSelectedAlert();
		else {
			// we should only have one alert (multiple selection is disabled)
			for (IpawsAlertDeployer iad: sel) {
				setSelectedAlert(iad);
				break;
			}
		}
	}
	
	/** Set the selected alert. */
	private void setSelectedAlert(IpawsAlertDeployer iad) {
		// set the selected alert and deployer
		selectedAlertDepl = iad;
		selectedAlert = alertCache.lookupObject(
				selectedAlertDepl.getAlertId());
		
		// fill out the value labels
		idLbl.setText(selectedAlert.getIdentifier());
		eventLbl.setText(selectedAlert.getEvent());
		urgencyLbl.setText(selectedAlert.getUrgency());
		serverityLbl.setText(selectedAlert.getSeverity());
		certaintyLbl.setText(selectedAlert.getCertainty());
		
		// TODO need to keep track of deployment status
		statusLbl.setText("Deployed");
		
		onsetLbl.setText(selectedAlertDepl.getAlertStart().toString());
		expiresLbl.setText(selectedAlertDepl.getAlertEnd().toString());
		
		// TODO put this in the helper class or something (we do something
		// similar in IpawsProcJob.getGeogPoly)
		// get a JSON object from the area string (which is in JSON syntax)
		String area = selectedAlert.getArea();
		JSONObject jo = new JSONObject(area);
		String areaDesc = jo.has("areaDesc") ? jo.getString("areaDesc") : "";
		
		// make the area description wrap (since it might be long)
		areaDescLbl.setText(areaDesc);
		if (areaDescLblWidth == null) {
			areaDescLblWidth = getPreferredSize().width
					- areaDescKeyLbl.getPreferredSize().width - 50;
		}
		areaDescLbl.setText("<html>" + areaDesc +"</html>");
		Dimension d = areaDescLbl.getPreferredSize();
		if (d.width > areaDescLblWidth) {
			double lines = Math.ceil((double) d.width / areaDescLblWidth);
			d.width = areaDescLblWidth;
			d.height *= lines;
		}
		areaDescLbl.setPreferredSize(d);
		
		// set the alert in the DMS dispatcher so sign list updates
		dmsDispatcher.setSelectedAlert(selectedAlertDepl, selectedAlert);
	}
	
	/** Clear the selected alert. */
	private void clearSelectedAlert() {
		// null the selected alert and deployer
		selectedAlertDepl = null;
		selectedAlert = null;
		
		// clear the value labels
		idLbl.setText("");
		eventLbl.setText("");
		urgencyLbl.setText("");
		serverityLbl.setText("");
		certaintyLbl.setText("");
		statusLbl.setText("");
		onsetLbl.setText("");
		expiresLbl.setText("");
		areaDescLbl.setText("");
	}
}
