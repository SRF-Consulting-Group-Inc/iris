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

	/** Load all the camera presets */
	static protected void loadAll() throws TMSException {
		namespace.registerType(SONAR_TYPE, CameraTemplateImpl.class);
		store.query("SELECT name, label, notes " +
			"FROM iris." + SONAR_TYPE + ";", new ResultFactory()
		{
			public void create(ResultSet row) throws Exception {
				namespace.addObject(new CameraTemplateImpl(
					row.getString(1),	// name
					row.getString(2),	// label
					row.getString(2)	// notes
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
		// TODO Auto-generated constructor stub
	}

	/** Create a camera template */
	public CameraTemplateImpl(String n, String l, String no) {
		super(n);
		label = l;
		notes = no;
	}

	private String  notes;
	private String  label;
//	private Boolean autostart;
//	private Boolean failover;
//	private Integer connectFailSec;
//	private Integer lostTimeoutSec;
//	private Boolean autoReconnect;
//	private Integer reconnectTimeoutSec;
	
	@Override
	/* (non-Javadoc)
	 * @see us.mn.state.dot.tms.CameraTemplate#getLabel()
	 */
	public String getLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	/* (non-Javadoc)
	 * @see us.mn.state.dot.tms.CameraTemplate#setLabel()
	 */
	public void setLabel(String label) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see us.mn.state.dot.tms.CameraTemplate#getNotes()
	 */
	@Override
	public String getNotes() {
		return notes;
	}

	/* (non-Javadoc)
	 * @see us.mn.state.dot.tms.CameraTemplate#setNotes(java.lang.String)
	 */
	@Override
	public void setNotes(String notes) {
		this.notes = notes;
	}

//	@Override
//	public Boolean getAutoStart() {
//		return autostart;
//	}
//
//	@Override
//	public void setAutoStart(Boolean autostart) {
//		this.autostart = autostart;
//	}
//
//	@Override
//	public Boolean getFailover() {
//		return failover;
//	}
//
//	@Override
//	public void setFailover(Boolean failover) {
//		this.failover = failover;
//	}
//
//	@Override
//	public Integer getConnectFailSec() {
//		return connectFailSec;
//	}
//
//	@Override
//	public void setConnectFailSec(Integer sec) {
//		this.connectFailSec = sec;
//	}
//
//	@Override
//	public Integer getLostTimeoutSec() {
//		return lostTimeoutSec;
//	}
//
//	@Override
//	public void setLostTimeoutSec(Integer sec) {
//		this.lostTimeoutSec = sec;
//	}
//
//	@Override
//	public Boolean getAutoReconnect() {
//		return autoReconnect;
//	}
//
//	@Override
//	public void setAutoReconnect(Boolean autoReconnect) {
//		this.autoReconnect = autoReconnect;
//	}
//
//	@Override
//	public Integer getReconnectTimeoutSec() {
//		return reconnectTimeoutSec;
//	}
//
//	@Override
//	public void setReconnectTimeoutSec(Integer sec) {
//		this.reconnectTimeoutSec = sec;
//	}
}
