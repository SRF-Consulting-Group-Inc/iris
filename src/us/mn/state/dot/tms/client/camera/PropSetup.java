/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2014-2017  Minnesota Department of Transportation
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

import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import us.mn.state.dot.tms.Camera;
import us.mn.state.dot.tms.EncoderType;
import us.mn.state.dot.tms.Encoding;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.widget.IAction;
import us.mn.state.dot.tms.client.widget.IComboBoxModel;
import us.mn.state.dot.tms.client.widget.IPanel;
import us.mn.state.dot.tms.client.widget.IPanel.Stretch;
import us.mn.state.dot.tms.utils.I18N;

/**
 * Camera properties setup panel.
 *
 * @author Douglas Lau
 */
public class PropSetup extends IPanel {

	/** Parse an integer */
	static private Integer parseInt(String t) {
		try {
			return Integer.parseInt(t);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	/** Camera number text */
	private final JTextField cam_num_txt = new JTextField("", 8);

	/** Encoder type combobox */
	private final JComboBox<EncoderType> enc_type_cbx =
		new JComboBox<EncoderType>();

	/** Encoder type action */
	private final IAction enc_type_act = new IAction("camera.encoder.type"){
		protected void doActionPerformed(ActionEvent e) {
		      camera.setEncoderType(getSelectedEncoderType());
		}
		@Override
		protected void doUpdateSelected() {
			enc_type_cbx.setSelectedItem(camera.getEncoderType());
		}
	};

	/** Get the selected encoder type */
	private EncoderType getSelectedEncoderType() {
		Object et = enc_type_cbx.getSelectedItem();
		return (et instanceof EncoderType) ? (EncoderType) et : null;
	}

	/** Encoder stream URI */
	private final JTextField encoder_txt = new JTextField("", 32);

	/** Encoder multicast URI */
	private final JTextField enc_mcast_txt = new JTextField("", 32);

	/** Model for encoder channel spinner */
	private final SpinnerNumberModel num_model =
		new SpinnerNumberModel(1, 0, 10, 1);
	
	/** Model for ftp refresh interval */
	private final SpinnerNumberModel ftp_ref_model =
		new SpinnerNumberModel(1, 1, 999, 1);

	/** Encoder channel spinner */
	private final JSpinner enc_chn_spn = new JSpinner(num_model);

	/** Checkbox to allow publishing camera images */
	private final JCheckBox publish_chk = new JCheckBox(new IAction(null) {
		protected void doActionPerformed(ActionEvent e) {
			camera.setPublish(publish_chk.isSelected());
		}
	});
	
	/** Username for ftp still images */
	private final JTextField ftp_username = new JTextField("anonymous", 32);
	
	/** Password for ftp still images */
	private final JPasswordField ftp_password = new JPasswordField("", 32);
	
	/** Refresh interval for ftp still images */
	private final JSpinner ref_interval = new JSpinner(ftp_ref_model);
	
	/** FTP image base directory */
	private final JTextField ftp_path = new JTextField("", 32);
	
	/** Obtain most recent FTP image */
	private final JRadioButton most_recent_img = new JRadioButton();
	
	/** Always obtain same image filename */
	private final JRadioButton same_filename = new JRadioButton();	
	
	/** FTP image filename */
	private final JTextField ftp_filename = new JTextField("", 32);

	/** User session */
	private final Session session;

	/** Camera proxy */
	private final Camera camera;

	/** Create a new camera properties setup panel */
	public PropSetup(Session s, Camera c) {
		session = s;
		camera = c;
	}

	/** Initialize the widgets on the panel */
	@Override
	public void initialize() {
		super.initialize();
		enc_type_cbx.setModel(new IComboBoxModel<EncoderType>(session
			.getSonarState().getCamCache().getEncoderTypeModel()));
		enc_type_cbx.setAction(enc_type_act);
	
		add("camera.num");
		add(cam_num_txt, Stretch.LAST);
		add("camera.encoder.type");
		add(enc_type_cbx, Stretch.LAST);
		add("camera.encoder");
		add(encoder_txt, Stretch.LAST);
		add("camera.enc_mcast");
		add(enc_mcast_txt, Stretch.LAST);
		add("camera.encoder.note", Stretch.END);
		add("camera.encoder.channel");
		add(enc_chn_spn, Stretch.LAST);
		add("camera.publish");
		add(publish_chk, Stretch.LAST);	
	
		if(camera.getEncoderType() != null && Encoding.fromOrdinal(camera.getEncoderType().getEncoding()) == Encoding.FTP){ 
			dispose();
			add("camera.num");
			add(cam_num_txt, Stretch.LAST);
			add("camera.encoder.type");
			add(enc_type_cbx, Stretch.LAST);
			add("camera.encoder");
			add(encoder_txt, Stretch.LAST);
			add("camera.publish");
			add(publish_chk, Stretch.LAST);	
			add("camera.ftp.username");
			// Necessary to make FTP panel the same size
			setBorder(new EmptyBorder(0,61,0,0)); 
			add(ftp_username, Stretch.LAST);
			add("camera.ftp.password");
			add(ftp_password, Stretch.LAST);
			add("camera.ftp.refresh");
			add(ref_interval, Stretch.LAST);
			add("camera.ftp.path");
			add(ftp_path, Stretch.LAST);
			add("camera.ftp.mostrecentimg");
			add(most_recent_img, Stretch.LAST);
			add("camera.ftp.samefilename");
			add(same_filename, Stretch.LAST);
			add("camera.ftp.ftpfilename");
			add(ftp_filename, Stretch.LAST);
			
			/** Set radio buttons */
			most_recent_img.setSelected(!camera.getSameFilename());
			same_filename.setSelected(camera.getSameFilename());
			
			/** Set tooltips */
			ftp_username.setToolTipText(I18N.get("camera.ftp.username.tooltip"));
			ftp_password.setToolTipText(I18N.get("camera.ftp.password.tooltip"));
			ref_interval.setToolTipText(I18N.get("camera.ftp.refresh.tooltip"));
			ftp_path.setToolTipText(I18N.get("camera.ftp.path.tooltip"));
			most_recent_img.setToolTipText(I18N.get("camera.ftp.mostrecentimg.tooltip")); 
			same_filename.setToolTipText(I18N.get("camera.ftp.samefilename.tooltip"));
			ftp_filename.setToolTipText(I18N.get("camera.ftp.ftpfilename.tooltip"));
		}
	
		createJobs();

	}

	/** Create jobs */
	private void createJobs() {
		cam_num_txt.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
			    Integer cn = parseInt(cam_num_txt.getText());
			    cam_num_txt.setText((cn != null)
			                        ? cn.toString()
			                        : "");
			    camera.setCamNum(cn);
			}
		});
		encoder_txt.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
			    camera.setEncoder(encoder_txt.getText());
			}
		});
		enc_mcast_txt.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
			    camera.setEncMulticast(enc_mcast_txt.getText());
			}
		});
		enc_chn_spn.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
			    Number c = (Number)enc_chn_spn.getValue();
			    camera.setEncoderChannel(c.intValue());
			}
		});
		ftp_username.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
			    camera.setFtpUsername(ftp_username.getText());
			}
		});
		ftp_password.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
			    camera.setFtpPassword(new String(ftp_password.getPassword()).trim());
			}
		});
		ref_interval.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
			    Number c = (Number)ref_interval.getValue();
			    camera.setRefInterval(c.intValue());
			}
		});
		ftp_path.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
			    camera.setFtpPath(new String(ftp_path.getText()));
			}
		});
		same_filename.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				most_recent_img.setSelected(!same_filename.isSelected());
			    camera.setSameFilename(same_filename.isSelected());
			    ftp_filename.setEnabled(same_filename.isSelected() && canWrite("ftpFilename"));
			}
		});
		most_recent_img.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				same_filename.setSelected(!most_recent_img.isSelected());
			    camera.setSameFilename(same_filename.isSelected());
			    ftp_filename.setEnabled(same_filename.isSelected() && canWrite("ftpFilename"));
			}
		});
		ftp_filename.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
			    camera.setFtpFilename(new String(ftp_filename.getText()));
			}
		});
	}

	/** Update the edit mode */
	public void updateEditMode() {
		cam_num_txt.setEnabled(canWrite("camNum"));
		enc_type_act.setEnabled(canWrite("encoderType"));
		encoder_txt.setEnabled(canWrite("encoder"));
		enc_mcast_txt.setEnabled(canWrite("encMulticast"));
		enc_chn_spn.setEnabled(canWrite("encoderChannel"));
		publish_chk.setEnabled(canWrite("publish"));
		ftp_username.setEnabled(canWrite("ftpUsername"));
		ftp_password.setEnabled(canWrite("ftpPassword"));
		ref_interval.setEnabled(canWrite("refInterval"));
		ftp_path.setEnabled(canWrite("ftpPath"));
		most_recent_img.setEnabled(canWrite("mostRecImg"));
		same_filename.setEnabled(canWrite("sameFilename"));
		ftp_filename.setEnabled(same_filename.isSelected() && canWrite("ftpFilename"));
	}

	/** Update one attribute on the form tab */
	public void updateAttribute(String a) {
		if (a == null || a.equals("camNum")) {
			Integer cn = camera.getCamNum();
			cam_num_txt.setText((cn != null) ? cn.toString() : "");
		}
		if (a == null || a.equals("encoderType")){
			if (a != null){
				dispose();
				initialize();
				repaint();
			}
			enc_type_act.updateSelected();
		}	
		if (a == null || a.equals("encoder"))
			encoder_txt.setText(camera.getEncoder());
		if (a == null || a.equals("encMulticast"))
			enc_mcast_txt.setText(camera.getEncMulticast());
		if (a == null || a.equals("encoderChannel"))
			enc_chn_spn.setValue(camera.getEncoderChannel());
		if (a == null || a.equals("publish"))
			publish_chk.setSelected(camera.getPublish());
		if (a == null || a.equals("ftpUsername"))
			ftp_username.setText(camera.getFtpUsername());
		if (a == null || a.equals("ftpPassword"))
			ftp_password.setText(camera.getFtpPassword());
		if (a == null || a.equals("refInterval"))
			ref_interval.setValue(camera.getRefInterval());
		if (a == null || a.equals("ftpPath"))
			ftp_path.setText(camera.getFtpPath());
		if (a == null || a.equals("mostRecImg"))
			most_recent_img.setSelected(!camera.getSameFilename());
		if (a == null || a.equals("sameFilename"))
			same_filename.setSelected(camera.getSameFilename());
		if (a == null || a.equals("ftpFilename"))
			ftp_filename.setText(camera.getFtpFilename());
	}

	/** Check if the user can update an attribute */
	private boolean canWrite(String aname) {
		return session.canWrite(camera, aname);
	}
}
