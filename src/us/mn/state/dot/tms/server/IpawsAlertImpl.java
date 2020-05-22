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
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.TMSException;

/**
 * Integrated Public Alert and Warning System (IPAWS) Alert object
 * server-side implementation.
 *
 * @author Michael Janson
 * @author Gordon Parikh
 */
public class IpawsAlertImpl extends BaseObjectImpl implements IpawsAlert {
	
	/** Database table name */
	static private final String TABLE = "event.ipaws";
	
	/** Load all the incidents */
	static public void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, IpawsAlertImpl.class);
		store.query("SELECT name, identifier, sender, sent_date, status, " +
			"message_type, scope, codes, note, alert_references, incidents, " +
			"categories, event, response_types, urgency, severity, " +
			"certainty, audience, effective_date, onset_date, " +
			"expiration_date, sender_name, headline, alert_description, " + 
			"instruction, parameters, area, purgeable FROM event." +
			SONAR_TYPE + ";",new ResultFactory()
		{
			public void create(ResultSet row) throws Exception {
				try {
					namespace.addObject(new IpawsAlertImpl(row));
				} catch(Exception e) {
					System.out.println(row.getString(1));
				}
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
		map.put("purgeable", purgeable);
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
			row.getString(27),	//area
			row.getBoolean(28) // purgeable flag
		);
	}

	/** Get IPAWS alert purge threshold (days) */
	static private int getPurgeDays() {
		return SystemAttrEnum.IPAWS_ALERT_PURGE_DAYS.getInt();
	}

	/** Purge old records that have been marked "purgeable". The age of the
	 *  records is determined based on the expiration_date field.
	 *  
	 *  TODO should we have another method that also purges old non-purgeable
	 *  records??
	 */
	static public void purgeRecords() throws TMSException {
		int age = getPurgeDays();
		System.out.println("Purging purgeable IPAWS alert records older than "
				+ age + " days...");
		if (store != null && age > 0) {
			store.update("DELETE FROM " + TABLE +
				" WHERE expiration_date < now() - '" + age +
				" days'::interval;");
		}
	}

	public IpawsAlertImpl(String n) throws TMSException {
		super(n);
	}
	
	
	public IpawsAlertImpl(String n, String i, String se, Date sd, String sta, 
			String mt, String sc, String[] cd, String nt, String[]ref,
			String[] inc, String[] ct, String ev, String[] rt, String u, 
			String sv, String cy, String au, Date efd, Date od, Date exd, 
			String sn, String hl, String ades, String in, 
			String par, String ar, boolean p) 
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
		purgeable = p;
	}

	/** Identifier for the alert */
	private String identifier;

	/** Set the identifier */
	@Override
	public void setIdentifier(String i) {
		identifier = i;
	}

	/** Set the identifier */
	public void doSetIdentifier(String i) throws TMSException {
		if (i != identifier) {
			store.update(this, "identifier", i);
			setIdentifier(i);
		}
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

	/** Set the sender */
	public void doSetSender(String se) throws TMSException {
		if (se != sender) {
			store.update(this, "sender", se);
			setSender(se);
		}
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

	/** Set the sent date */
	public void doSetSentDate(Date sd) throws TMSException {
		if (sd != sent_date) {
			store.update(this, "sent_date", sd);
			setSentDate(sd);
		}
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

	/** Set the status */
	public void doSetStatus(String sta) throws TMSException {
		if (status != sta) {
			store.update(this, "status", sta);
			setStatus(sta);
		}
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

	/** Set the message type */
	public void doSetMsgType(String mt) throws TMSException {
		if (message_type != mt) {
			store.update(this, "message_type", mt);
			setMsgType(mt);
		}
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

	/** Set the scope */
	public void doSetScope(String sc) throws TMSException {
		if (scope != sc) {
			store.update(this, "scope", sc);
			setScope(sc);
		}
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

	/** Set the codes */
	public void doSetCodes(List<String> cd) throws TMSException {
		if (codes != cd) {
			store.update(this, "codes", cd);
			setCodes(cd);
		}
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

	/** Set the note */
	public void doSetNote(String nt) throws TMSException {
		if (note != nt) {
			store.update(this, "note", nt);
			setNote(nt);
		}
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

	/** Set the alert references */
	public void doSetAlertReferences(List<String> ref) throws TMSException {
		if (alert_references != ref) {
			store.update(this, "alert_references", ref);
			setAlertReferences(ref);
		}
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

	/** Set the incidents */
	public void doSetIncidents(List<String> inc) throws TMSException {
		if (incidents != inc) {
			store.update(this, "incidents", inc);
			setIncidents(inc);
		}
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

	/** Set the categories */
	public void doSetCategories(List<String> ct) throws TMSException {
		if (categories != ct) {
			store.update(this, "categories", ct);
			setCategories(ct);
		}
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

	/** Set the event */
	public void doSetEvent(String ev) throws TMSException {
		if (event != ev) {
			store.update(this, "event", ev);
			setUrgency(ev);
		}
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

	/** Set the response types */
	public void doSetResponseTypes(List<String> rt) throws TMSException {
		if (response_types != rt) {
			store.update(this, "response_types", rt);
			setResponseTypes(rt);
		}
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

	/** Set the urgency */
	public void doSetUrgency(String u) throws TMSException {
		if (urgency != u) {
			store.update(this, "urgency", u);
			setUrgency(u);
		}
	}
	
	/** Get the urgency */
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

	/** Set the severity */
	public void doSetSeverity(String sv) throws TMSException {
		if (urgency != sv) {
			store.update(this, "severity", sv);
			setSeverity(sv);
		}
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

	/** Set the certainty */
	public void doSetCertainty(String cy) throws TMSException {
		if (certainty != cy) {
			store.update(this, "certainty", cy);
			setCertainty(cy);
		}
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

	/** Set the audience */
	public void doSetAudience(String au) throws TMSException {
		if (audience != au) {
			store.update(this, "audience", au);
			setAudience(au);
		}
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

	/** Set the effective date */
	public void doSetEffectiveDate(Date efd) throws TMSException {
		if (effective_date != efd) {
			store.update(this, "effective_date", efd);
			setEffectiveDate(efd);
		}
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

	/** Set the onset date */
	public void doSetOnsetDate(Date od) throws TMSException {
		if (od != onset_date) {
			store.update(this, "onset_date", od);
			setOnsetDate(od);
		}
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

	/** Set the expiration date */
	public void doSetExpirationDate(Date exd) throws TMSException {
		if (exd != expiration_date) {
			store.update(this, "expiration_date", exd);
			setExpirationDate(exd);
		}
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

	/** Set the sender's name */
	public void doSetSenderName(String sn) throws TMSException {
		if (sender_name != sn) {
			store.update(this, "sender_name", sn);
			setSenderName(sn);
		}
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

	/** Set the alert headline */
	public void doSetHeadline(String hl) throws TMSException {
		if (headline != hl) {
			store.update(this, "headline", hl);
			setHeadline(hl);
		}
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

	/** Set the alert description */
	public void doSetAlertDescription(String ad) throws TMSException {
		if (alert_description != ad) {
			store.update(this, "alert_description", ad);
			setAlertDescription(ad);
		}
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

	/** Set the instruction */
	public void doSetInstruction(String in) throws TMSException {
		if (instruction != in) {
			store.update(this, "instruction", in);
			setInstruction(in);
		}
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

	/** Set the parameters */
	public void doSetParameters(String par) throws TMSException {
		if (parameters != par) {
			store.update(this, "parameters", par);
			setParameters(par);
		}
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

	/** Set the area */
	public void doSetArea(String ar) throws TMSException {
		if (area != ar) {
			store.update(this, "area", ar);
			setArea(ar);
		}
	}
	
	/** Get the area */
	@Override
	public String getArea() {
		return area;
	}
	
	/** Purgeable flag. Set to true if alert is determined to be irrelevant
	 *  to this system's users.
	 */
	private boolean purgeable;
	
	/** Set if this alert is purgeable (irrelevant to us) */
	public void setPurgeable(boolean p) {
		purgeable = p;
	}

	/** Set the area */
	public void doSetPurgeable(boolean p) throws TMSException {
		if (purgeable != p) {
			store.update(this, "purgeable", p);
			setPurgeable(p);
		}
	}
	
	/** Return if this alert is purgeable (irrelevant to us) */
	public boolean getPurgeable() {
		return purgeable;
	}
}
