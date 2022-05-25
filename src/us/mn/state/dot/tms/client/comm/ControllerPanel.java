/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2014-2020  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.client.comm;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import us.mn.state.dot.tms.CommLink;
import us.mn.state.dot.tms.Controller;
import us.mn.state.dot.tms.ControllerHelper;
import us.mn.state.dot.tms.CtrlCondition;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.proxy.ProxyDescriptor;
import us.mn.state.dot.tms.client.proxy.ProxyTablePanel;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.ILabel;
import static us.mn.state.dot.tms.client.widget.Widgets.UI;

/**
 * A panel for displaying a table of controllers.
 *
 * @author Douglas Lau
 */
public class ControllerPanel extends ProxyTablePanel<Controller> {

	/** Action to export controller properties and status information */
	private final IAction export_csv = new IAction("report.copy.csv") {
		protected void doActionPerformed(ActionEvent e) {
			// generate a filename
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
			String fn = "Controller_Info_" + dtf.format(LocalDateTime.now()) + ".csv";
			System.out.println("Exporting controller data to file '" + fn + "' ...");
			
			// open the file
			File csvFile = new File(fn);
			try {
				PrintWriter pw = new PrintWriter(csvFile);
				
				// write the header
				String chead = String.join(",", "Controller", "Comm Link", "URI",
						"Drop ID", "Location", "Notes", "Version", "Condition",
						"Fail Time", "Status", "Maint Status", "Timeout Errors",
						"Checksum Errors", "Parsing Errors", "Controller Errors",
						"Success Operations", "Failed Operations");
				pw.write(chead + "\n");
				
				// get all the controllers
				ProxyDescriptor<Controller> d = model.getDescriptor();
				Iterator<Controller> cit = d.cache.iterator();
				Controller c = null;
				String cline = null;
				while (cit.hasNext()) {
					c = cit.next();
					String ft = c.getFailTime() != null ?
							new Date(c.getFailTime()).toString() : "";
					cline = String.join(",", c.getName(),
						c.getCommLink().getName(), c.getCommLink().getUri(),
						Integer.toString(c.getDrop()),
						c.getLocation(), c.getNotes(), c.getVersion(),
						CtrlCondition.fromOrdinal(c.getCondition()).toString(), ft,
						c.getStatus(), c.getMaint(),
						Integer.toString(c.getTimeoutErr()),
						Integer.toString(c.getChecksumErr()),
						Integer.toString(c.getParsingErr()),
						Integer.toString(c.getControllerErr()),
						Integer.toString(c.getSuccessOps()),
						Integer.toString(c.getFailedOps()));
					pw.write(cline + "\n");
				}
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}
	};

	/** Button to display the proxy properties */
	private final JButton export_btn = new JButton(export_csv);

	/** Condition filter label */
	private final ILabel cond_lbl = new ILabel(
		"controller.condition.filter");

	/** Condition filter action */
	private final IAction cond_act = new IAction(
		"controller.condition")
	{
		protected void doActionPerformed(ActionEvent e) {
			Object v = cond_cbx.getSelectedItem();
			if (v instanceof CtrlCondition)
				setCondition((CtrlCondition) v);
			else
				setCondition(null);
		}
	};

	/** Condition combobox */
	private final JComboBox<CtrlCondition> cond_cbx =
		new JComboBox<CtrlCondition>(CtrlCondition.values_with_null());

	/** Comm filter label */
	private final ILabel comm_lbl = new ILabel("controller.comm.filter");

	/** Comm filter action */
	private final IAction comm_act = new IAction("controller.comm") {
		protected void doActionPerformed(ActionEvent e) {
			Object v = comm_cbx.getSelectedItem();
			if (v instanceof CommState)
				setCommState((CommState)v);
			else
				setCommState(null);
		}
	};

	/** Comm state combo box */
	private final JComboBox<CommState> comm_cbx =
		new JComboBox<CommState>(CommState.values_with_null());

	/** Create a new controller panel */
	public ControllerPanel(Session s) {
		super(new ControllerTableModel(s));
	}

	/** Initialize the panel */
	@Override
	public void initialize() {
		super.initialize();
		cond_cbx.setAction(cond_act);
		comm_cbx.setRenderer(new CommListRenderer());
		comm_cbx.setAction(comm_act);
	}

	/** Add create/delete widgets to the button panel */
	@Override
	protected void addCreateDeleteWidgets(GroupLayout.SequentialGroup hg,
		GroupLayout.ParallelGroup vg)
	{
		hg.addComponent(export_btn);
		vg.addComponent(export_btn);
		hg.addGap(UI.hgap);
		hg.addComponent(cond_lbl);
		vg.addComponent(cond_lbl);
		hg.addGap(UI.hgap);
		hg.addComponent(cond_cbx);
		vg.addComponent(cond_cbx);
		hg.addGap(UI.hgap);
		hg.addComponent(comm_lbl);
		vg.addComponent(comm_lbl);
		hg.addGap(UI.hgap);
		hg.addComponent(comm_cbx);
		vg.addComponent(comm_cbx);
		hg.addGap(UI.hgap);
		super.addCreateDeleteWidgets(hg, vg);
	}

	/** Set comm link filter */
	public void setCommLink(CommLink cl) {
		if (model instanceof ControllerTableModel) {
			ControllerTableModel mdl = (ControllerTableModel)model;
			mdl.setCommLink(cl);
			updateSortFilter();
		}
	}

	/** Set condition filter */
	private void setCondition(CtrlCondition c) {
		if (model instanceof ControllerTableModel) {
			ControllerTableModel mdl = (ControllerTableModel)model;
			mdl.setCondition(c);
			updateSortFilter();
		}
	}

	/** Set comm state filter */
	private void setCommState(CommState cs) {
		if (model instanceof ControllerTableModel) {
			ControllerTableModel mdl = (ControllerTableModel)model;
			mdl.setCommState(cs);
			updateSortFilter();
		}
	}
}
