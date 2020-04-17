/**
 * 
 */
package us.mn.state.dot.tms.server;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import us.mn.state.dot.tms.CameraTemplate;
import us.mn.state.dot.tms.TMSException;

/**
 * @author John L. Stanley - SRF Consulting
 *
 */
public class CameraTemplateImpl extends BaseObjectImpl implements CameraTemplate {

	/** Load all the camera templates */
	static protected void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, CameraTemplateImpl.class);
		store.query("SELECT name, label, notes "
				+ "FROM iris." + SONAR_TYPE + ";", new ResultFactory()
		{
			public void create(ResultSet row) throws Exception {
				namespace.addObject(new CameraTemplateImpl(
					row.getString(1),	// name
					row.getString(2),   //label
					row.getString(3)	// notes
				));
			}
		});
	}

	/** Get a mapping of the columns */
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("label", label);
		map.put("notes", notes);
		return map;
	}

	/** Get the database table name */
	@Override
	public String getTable() {
		return "iris." + SONAR_TYPE;
	}

	/** Get the SONAR type name */
	@Override
	public String getTypeName() {
		return SONAR_TYPE;
	}

	/** Create a CameraTemplate */
	public CameraTemplateImpl(String n) {
		super(n);
	}

	/** Create a camera template */
	public CameraTemplateImpl(String n, String no, String l) {
		super(n);
		label = l;
		notes = no;
	}
	
	/** Template label */
	private String label;

	/** Get the template label */
	@Override
	public String getLabel() {
		return notes;
	}

	/** Set the template label */
	@Override
	public void setLabel(String label) {
		this.label = label;
	}
	

	/** Template notes */
	private String notes;

	/** Get the template notes */
	@Override
	public String getNotes() {
		return notes;
	}

	/** Set the template notes */
	@Override
	public void setNotes(String notes) {
		this.notes = notes;
	}
	
//	/** Autostart boolean */
//	private Boolean autostart;
//
//	/** Get the autostart boolean */
//	@Override
//	public Boolean getAutoStart() {
//		return autostart;
//	}
//
//	/** Set the autostart boolean */
//	@Override
//	public void setAutoStart(Boolean autostart) {
//		this.autostart = autostart;
//	}
//	
//	/** Failover boolean */
//	private Boolean failover;
//
//	/** Get the failover boolean */
//	@Override
//	public Boolean getFailover() {
//		return failover;
//	}
//
//	/** Set the failover boolean */
//	@Override
//	public void setFailover(Boolean failover) {
//		this.failover = failover;
//	}
//	
//	/** Connection failure time in seconds */
//	private Integer connect_fail_sec;
//
//	/** Get teh connection failture time in seconds */
//	@Override
//	public Integer getConnectFailSec() {
//		return connect_fail_sec;
//	}
//
//	/** Set the connection failure time in seconds */
//	@Override
//	public void setConnectFailSec(Integer sec) {
//		this.connect_fail_sec = sec;
//	}
//	
//	/** Lost timeout in seconds */
//	private Integer lost_timeout_sec;
//
//	/** Get the lost timeout in seconds */
//	@Override
//	public Integer getLostTimeoutSec() {
//		return lost_timeout_sec;
//	}
//
//	/** Set the lost timeout in seconds */
//	@Override
//	public void setLostTimeoutSec(Integer sec) {
//		this.lost_timeout_sec = sec;
//	}
//	
//	/** Auto reconnect boolean */
//	private Boolean auto_reconnect;
//
//	/** Get the auto reconnect boolean */
//	@Override
//	public Boolean getAutoReconnect() {
//		return auto_reconnect;
//	}
//
//	/** Set the auto reconnect bolean */
//	@Override
//	public void setAutoReconnect(Boolean autoReconnect) {
//		this.auto_reconnect = autoReconnect;
//	}
//
//	/** Reconnect timeout in seconds */
//	private Integer reconnect_timeout_sec;
//
//	/** Get the reconnect timeout in seconds */
//	@Override
//	public Integer getReconnectTimeoutSec() {
//		return reconnect_timeout_sec;
//	}
//
//	/** Set the reconnect timeout in seconds */
//	@Override
//	public void setReconnectTimeoutSec(Integer sec) {
//		this.reconnect_timeout_sec = sec;
//	}
}
