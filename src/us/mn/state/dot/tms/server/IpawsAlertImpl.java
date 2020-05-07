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

package us.mn.state.dot.tms.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.mn.state.dot.tms.IpawsAlert;
import us.mn.state.dot.tms.TMSException;

/**
 * Integrated Public Alert and Warning System (IPAWS) Alert object
 * server-side implementation.
 *
 * @author Michael Janson
 * @author Gordon Parikh
 */
public class IpawsAlertImpl extends BaseObjectImpl implements IpawsAlert {
	
	/** Load all the incidents */
	static public void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, IpawsAlertImpl.class);
		store.query("SELECT name, identifier, sender, sent_date, status, " +
			"message_type, scope, codes, note, alert_references, incidents, " +
			"categories, event, response_types, urgency, severity, " +
			"certainty, audience, effective_date, onset_date, " +
			"expiration_date, sender_name, headline, alert_description, " + 
			"instruction, parameters, area FROM event." +
			SONAR_TYPE + ";",new ResultFactory()
		{
			public void create(ResultSet row) throws Exception {
				namespace.addObject(new IpawsAlertImpl(row));
			}
		});
	}

	
	/** Get a mapping of the columns */
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("identifier", identifier);
		map.put("sender", sender);
		map.put("sent_date", sent_date);
		map.put("status", status);
		map.put("message_type", message_type);
		map.put("scope", scope);
		map.put("codes", codes);
		map.put("note", note);
		map.put("alert_references", alert_references);
		map.put("incidents", incidents);
		map.put("categories", categories);
		map.put("event", event);
		map.put("response_types", response_types);
		map.put("urgency", urgency);
		map.put("severity", severity);
		map.put("certainty", certainty);
		map.put("audience", audience);
		map.put("effective_date", effective_date);
		map.put("onset_date", onset_date);
		map.put("expiration_date", expiration_date);
		map.put("sender_name", sender_name);
		map.put("headline", headline);
		map.put("alert_description", alert_description);
		map.put("instruction", instruction);
		map.put("parameters", parameters);
		map.put("area", area);
		return map;
	}

	/** Get the database table name */
	@Override
	public String getTable() {
		return "event." + SONAR_TYPE;
	}

	/** Get the SONAR type name */
	@Override
	public String getTypeName() {
		return SONAR_TYPE;
	}
	
	/** Create an incident advice */
	private IpawsAlertImpl(ResultSet row) throws SQLException {
		this(row.getString(1),	// name
			row.getString(2),	// identifier
			row.getString(3),	// sender
			row.getTimestamp(4),// sent date
			row.getString(5),	// status
			row.getString(6),	// message type
			row.getString(7),	// scope
			(String[])row.getArray(8).getArray(),	// codes
			row.getString(9),	// note
			(String[])row.getArray(10).getArray(),	// alert references
			(String[])row.getArray(11).getArray(),	// incidents
			(String[])row.getArray(12).getArray(),	// categories
			row.getString(13),	// event
			(String[])row.getArray(14).getArray(),	// response types
			row.getString(15),	// urgency
			row.getString(16),	// severity
			row.getString(17),	// certainty
			row.getString(18), //audience
			row.getTimestamp(19),// effective date
			row.getTimestamp(20),// onset date
			row.getTimestamp(21),// expiration date
			row.getString(22),	// sender name
			row.getString(23),	// headline
			row.getString(24), // alert description
			row.getString(25), // instruction
			row.getString(26),	// parameters
			row.getString(27)	//area
		);
	}
	

	public IpawsAlertImpl(String n) throws TMSException {
		super(n);
	}
	
	
	public IpawsAlertImpl(String n, String i, String se, Date sd, String sta, 
			String mt, String sc, String[] cd, String nt, String[]ref,
			String[] inc, String[] ct, String ev, String[] rt, String u, 
			String sv, String cy, String au, Date efd, Date od, Date exd, 
			String sn, String hl, String ades, String in, 
			String par, String ar) 
	{
		super(n);
		identifier = i;
		sender = se;
		sent_date = sd;
		status = sta;
		message_type = mt;
		scope = sc;
		codes = Arrays.asList(cd);
		note = nt;
		alert_references = Arrays.asList(ref);
		incidents = Arrays.asList(inc);
		categories = Arrays.asList(ct);
		event = ev;
		response_types = Arrays.asList(rt);
		urgency = u;
		severity = sv;
		certainty = cy;
		audience = au;
		effective_date = efd;
		onset_date = od;
		expiration_date = exd;
		sender_name = sn;
		headline = hl;
		alert_description = ades;
		instruction = in;
		parameters = par;
		area = ar;
	
	}

	/** Identifier for the alert */
	private String identifier;

	/** Set the identifier */
	@Override
	public void setIdentifier(String i) {
		identifier = i;
	}

	/** Get the identifier */
	@Override
	public String getIdentifier() {
		return identifier;
	}

	/** Alert sender */
	private String sender;
	
	/** Set the sender */
	@Override
	public void setSender(String se) {
		sender = se;
	}

	/** Get the sender */
	@Override
	public String getSender() {
		return sender;
	}

	/** Sent date of alert */
	private Date sent_date;
	
	/** Set the sent date */
	@Override
	public void setSentDate(Date sd) {
		sent_date = sd;
	}

	/** Get the sent date */
	@Override
	public Date getSentDate() {
		return sent_date;
	}

	/** Status of alert */
	private String status;
	
	/** Set the status */
	@Override
	public void setStatus(String sta) {
		status = sta;
	}

	/** Get the status */
	@Override
	public String getStatus() {
		return status;
	}

	/** Alert message type */
	private String message_type;
	
	/** Set the message type */
	@Override
	public void setMsgType(String mt) {
		message_type = mt;
	}

	/** Get the message type */
	@Override
	public String getMsgType() {
		return message_type;
	}

	/** Alert scope */
	private String scope;
	
	/** Set the scope */
	@Override
	public void setScope(String sc) {
		scope = sc;
	}
	
	/** Get the scope */
	@Override
	public String getScope() {
		return scope;
	}

	/** Alert codes */
	private List<String> codes;
	
	/** Set the codes */
	@Override
	public void setCodes(List<String> cd) {
		codes = cd;
	}

	/** Get the codes */
	@Override
	public List<String> getCodes() {
		return codes;
	}

	/** Alert note */
	private String note;
	
	/** Set the note */
	@Override
	public void setNote(String nt) {
		note = nt;
	}

	/** Get the note */
	@Override
	public String getNote() {
		return note;
	}
	
	/** Alert references */
	private List<String> alert_references;
	
	/** Set the alert references */
	@Override
	public void setAlertReferences(List<String> ref) {
		alert_references = ref;
	}

	/** Get the alert references */
	@Override
	public List<String> getAlertReferences() {
		return alert_references;
	}
	/** Alert incidents */
	private List<String> incidents;
	
	/** Set the incidents */
	@Override
	public void setIncidents(List<String> inc) {
		incidents = inc;
	}

	/** Get the incidents */
	@Override
	public List<String> getIncidents() {
		return incidents;
	}

	/** Categories of alert */
	private List<String> categories;
	
	/** Set the categories */
	@Override
	public void setCategories(List<String> ct) {
		categories = ct;
	}

	/** Get the categories */
	@Override
	public List<String> getCategories() {
		return categories;
	}
	
	/** Alert event */
	private String event;

	/** Set the event */
	@Override
	public void setEvent(String ev) {
		event = ev;
	}

	/** Get the event */
	@Override
	public String getEvent() {
		return event;
	}
	
	/** Alert response types */
	private List<String> response_types;
	
	/** Set the response types */
	@Override
	public void setResponseTypes(List<String> rt) {
		response_types = rt;
	}

	/** Get the response type(s) */
	@Override
	public List<String> getResponseTypes() {
		return response_types;
	}
	
	/** Urgency of alert */
	private String urgency;

	/** Set the urgency */
	@Override
	public void setUrgency(String u) {
		urgency = u;
	}

	/** Get the ugency */
	@Override
	public String getUrgency() {
		return urgency;
	}
	
	/** Severity of the alert */
	private String severity;
	
	/** Set the severity */
	@Override
	public void setSeverity(String sv) {
		severity = sv;
	}

	/** Get the severity */
	@Override
	public String getSeverity() {
		return severity;
	}

	/** Certainty of the alert */
	private String certainty;
	
	/** Set the certainty */
	@Override
	public void setCertainty(String cy) {
		certainty = cy;
	}

	/** Get the certainty */
	@Override
	public String getCertainty() {
		return certainty;
	}
	
	/** Audience for the alert */
	private String audience;
	
	/** Set the audience */
	@Override
	public void setAudience(String au) {
		audience = au;
	}

	/** Get the audience */
	@Override
	public String getAudience() {
		return audience;
	}	

	/** Effective date of the alert */
	private Date effective_date;
	
	/** Set the effective date */
	@Override
	public void setEffectiveDate(Date efd) {
		effective_date = efd;
	}

	/** Get the effective date */
	@Override
	public Date getEffectiveDate() {
		return effective_date;
	}
	
	/** Onset date for alert */
	private Date onset_date;
	
	/** Set the onset date */
	@Override
	public void setOnsetDate(Date od) {
		onset_date = od;
	}

	/** Get the onset date */
	@Override
	public Date getOnsetDate() {
		return onset_date;
	}

	/** Expiration date for alert */
	private Date expiration_date;
	
	/** Set the expiration date*/
	@Override
	public void setExpirationDate(Date exd) {
		expiration_date = exd;
	}

	/** Get the expiration date */
	@Override
	public Date getExpirationDate() {
		return expiration_date;
	}
	
	/** The alert sender's name */
	private String sender_name;

	/** Set the sender's name */
	@Override
	public void setSenderName(String sn) {
		sender_name = sn;
	}

	/** Get the sender's name */
	@Override
	public String getSenderName() {
		return sender_name;
	}

	/** Headline for the alert */
	private String headline;
	
	/** Set the alert headline */
	@Override
	public void setHeadline(String hl) {
		headline = hl;
	}

	/** Get the alert headline */
	@Override
	public String getHeadline() {
		return headline;
	}
	
	/** Description of alert */
	private String alert_description;
	
	/** Set the description */
	@Override
	public void setAlertDescription(String ad) {
		alert_description = ad;
	}

	/** Get the description */
	@Override
	public String getAlertDescription() {
		return alert_description;
	}
	
	/** Alert instruction */
	private String instruction;
	
	/** Set the instruction */
	@Override
	public void setInstruction(String in) {
		instruction = in;
	}

	/** Get the description */
	@Override
	public String getInstruction() {
		return instruction;
	}
	
	/** Parameters */
	private String parameters;
	
	/** Set the parameters */
	@Override
	public void setParameters(String par) {
		parameters = par;
	}

	/** Get the parameters */
	@Override
	public String getParameters() {
		return parameters;
	}
	
	/** Area */
	private String area;
	
	/** Set the area */
	@Override
	public void setArea(String ar) {
		area = ar;
	}

	/** Get the area */
	@Override
	public String getArea() {
		return area;
	}

}
