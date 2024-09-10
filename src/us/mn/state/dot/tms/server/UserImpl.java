/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2006-2024  Minnesota Department of Transportation
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

import java.security.spec.InvalidKeySpecException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import us.mn.state.dot.sonar.SonarObject;
import us.mn.state.dot.sonar.server.ServerNamespace;
import us.mn.state.dot.tms.ChangeVetoException;
import us.mn.state.dot.tms.Role;
import us.mn.state.dot.tms.TMSException;
import us.mn.state.dot.tms.User;
import static us.mn.state.dot.tms.utils.SString.countLetters;
import static us.mn.state.dot.tms.utils.SString.countUnique;
import static us.mn.state.dot.tms.utils.SString.isDisplayable;
import static us.mn.state.dot.tms.utils.SString.longestCommonSubstring;

/**
 * A user account which can access IRIS.
 *
 * @author Douglas lau
 */
public class UserImpl implements User, Storable {

	/** Get required number of unique characters for a password length */
	static private int uniqueRequirement(int plen) {
		return (plen < 24) ? (plen / 2) : 12;
	}

	/** SQL connection to database */
	static private SQLConnection store;

	/** Lookup all the users */
	static public void lookup(SQLConnection c, final ServerNamespace ns)
		throws TMSException
	{
		store = c;
		store.query("SELECT name, full_name, password, dn, role, " +
			"enabled FROM iris.user_id;", new ResultFactory()
		{
			public void create(ResultSet row) throws Exception {
				ns.addObject(new UserImpl(ns, row));
			}
		});
	}

	/** Lookup a role */
	static private RoleImpl lookupRole(ServerNamespace ns, String name){
		SonarObject so = ns.lookupObject(RoleImpl.SONAR_TYPE, name);
		return (so instanceof RoleImpl) ? (RoleImpl) so : null;
	}

	/** Get a mapping of the columns */
	@Override
	public Map<String, Object> getColumns() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("full_name", fullName);
		map.put("password", password);
		map.put("dn", dn);
		map.put("role", role);
		map.put("enabled", enabled);
		return map;
	}

	/** Store an object */
	public void doStore() throws TMSException {
		store.create(this);
	}

	/** Get the SONAR type name */
	@Override
	public String getTypeName() {
		return SONAR_TYPE;
	}

	/** Get the database table name */
	@Override
	public String getTable() {
		return "iris.user_id";
	}

	/** Create a new user */
	public UserImpl(String n) throws TMSException {
		if (!n.equals(n.toLowerCase())) {
			throw new ChangeVetoException(
				"Must not contain upper-case characters");
		}
		name = n;
		fullName = "";
		password = "";
		dn = "cn=" + name;
		role = null;
		enabled = false;
	}

	/** Create an user from a database row */
	private UserImpl(ServerNamespace ns, ResultSet row)
		throws SQLException, TMSException
	{
		this(ns, row.getString(1),  // name
		         row.getString(2),  // full_name
		         row.getString(3),  // password
		         row.getString(4),  // dn
		         row.getString(5),  // role
		         row.getBoolean(6)  // enabled
		);
	}

	/** Create an user from database lookup */
	private UserImpl(ServerNamespace ns, String n, String fn,
		String pwd, String d, String r, boolean e) throws TMSException
	{
		this(n, fn, pwd, d, lookupRole(ns, r), e);
	}

	/** Create an user from database lookup */
	private UserImpl(String n, String fn, String pwd, String d,
		RoleImpl r, boolean e)
	{
		name = n;
		fullName = fn;
		password = pwd;
		dn = d;
		role = r;
		enabled = e;
	}

	/** Get the primary key name */
	@Override
	public String getPKeyName() {
		return "name";
	}

	/** Get the primary key */
	@Override
	public String getPKey() {
		return name;
	}

	/** Get a string representation of the object */
	@Override
	public String toString() {
		return name;
	}

	/** Destroy a user */
	@Override
	public void destroy() {
		// Subclasses must remove user from backing store
	}

	/** Destroy a user */
	public void doDestroy() throws TMSException {
		store.destroy(this);
	}

	/** User name */
	private final String name;

	/** Get the SONAR object name */
	@Override
	public String getName() {
		return name;
	}

	/** Full (display) name */
	private String fullName;

	/** Set the user's full name */
	@Override
	public void setFullName(String n) {
		fullName = n;
	}

	/** Set the full (display) name */
	public void doSetFullName(String n) throws TMSException {
		if (!n.equals(fullName)) {
			store.update(this, "full_name", n);
			setFullName(n);
		}
	}

	/** Get the user's full name */
	@Override
	public String getFullName() {
		return fullName;
	}

	/** Password hash */
	private String password;

	/** Set the password */
	@Override
	public void setPassword(String pwd) {
		password = pwd;
	}

	/** Set the password */
	public void doSetPassword(String pwd) throws TMSException,
		InvalidKeySpecException
	{
		checkPassword(pwd);
		String ph = MainServer.hash_provider.createHash(
			pwd.toCharArray());
		store.update(this, "password", ph);
		setPassword(ph);
	}

	/** Check a password */
	private void checkPassword(String pwd) throws ChangeVetoException {
		final int plen = pwd.length();
		if (plen < 8) {
			throw new ChangeVetoException(
				"Must be at least 8 characters");
		}
		if (!isDisplayable(pwd)) {
			throw new ChangeVetoException(
				"All characters must be displayable");
		}
		String lpwd = pwd.toLowerCase();
		String c = longestCommonSubstring(name.toLowerCase(), lpwd);
		if (c.length() > 4)
			throw new ChangeVetoException("Based on user name");
		if (longestCommonSubstring("password", lpwd).length() > 4)
			throw new ChangeVetoException("Invalid password");
		int n_let = countLetters(pwd);
		if (0 == n_let)
			throw new ChangeVetoException("Must contain letters");
		if (plen < 20 && plen == n_let) {
			throw new ChangeVetoException(
				"Must contain non-letters");
		}
		if (countUnique(pwd) < uniqueRequirement(plen)) {
			throw new ChangeVetoException(
				"Must contain more unique characters");
		}
	}

	/** Get the password */
	public String getPassword() {
		return password;
	}

	/** LDAP Distinguished Name */
	private String dn;

	/** Set the LDAP Distinguished Name */
	@Override
	public void setDn(String d) {
		dn = d;
	}

	/** Set the LDAP distinguished name */
	public void doSetDn(String d) throws TMSException {
		if (!d.equals(dn)) {
			store.update(this, "dn", d);
			setDn(d);
		}
	}

	/** Get the LDAP Distinguished Name */
	@Override
	public String getDn() {
		return dn;
	}

	/** Role of the user */
	private RoleImpl role;

	/** Set the role */
	@Override
	public void setRole(Role r) {
		if (r instanceof RoleImpl)
			role = (RoleImpl) r;
	}

	/** Set the role assigned to the user */
	public void doSetRole(Role r) throws TMSException {
		if (r != role) {
			store.update(this, "role", r);
			setRole(r);
		}
	}

	/** Get the role */
	@Override
	public Role getRole() {
		return role;
	}

	/** Enabled flag */
	private boolean enabled;

	/** Set the enabled flag */
	@Override
	public void setEnabled(boolean e) {
		enabled = e;
	}

	/** Set the enabled flag */
	public void doSetEnabled(boolean e) throws TMSException {
		if (e != enabled) {
			store.update(this, "enabled", e);
			setEnabled(e);
		}
	}

	/** Get the enabled flag */
	@Override
	public boolean getEnabled() {
		return enabled;
	}
}
