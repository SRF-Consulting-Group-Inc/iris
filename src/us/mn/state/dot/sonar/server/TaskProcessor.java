/*
 * SONAR -- Simple Object Notification And Replication
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
package us.mn.state.dot.sonar.server;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import us.mn.state.dot.sched.DebugLog;
import us.mn.state.dot.sched.ExceptionHandler;
import us.mn.state.dot.sched.Work;
import us.mn.state.dot.sched.Worker;
import static us.mn.state.dot.sched.TimeSteward.currentTimeMillis;
import us.mn.state.dot.sonar.ConfigurationError;
import us.mn.state.dot.sonar.Name;
import us.mn.state.dot.sonar.Namespace;
import us.mn.state.dot.sonar.NamespaceError;
import us.mn.state.dot.sonar.Security;
import us.mn.state.dot.sonar.SonarException;
import us.mn.state.dot.sonar.SonarObject;
import us.mn.state.dot.sonar.SSLState;
import us.mn.state.dot.sonar.User;
import us.mn.state.dot.tms.server.AccessLogger;
import us.mn.state.dot.tms.server.HashProvider;

/**
 * The task processor handles all SONAR tasks.
 *
 * @author Douglas Lau
 */
public class TaskProcessor {

	/** SONAR debug log */
	static public final DebugLog DEBUG = new DebugLog("sonar");

	/** SONAR task debug log */
	static private final DebugLog DEBUG_TASK = new DebugLog("sonar_task");

	/** SONAR time debug log */
	static final DebugLog DEBUG_TIME = new DebugLog("sonar_time");

	/** Debug a task */
	static private void debugTask(String msg, ConnectionImpl c) {
		if (DEBUG_TASK.isOpen()) {
			if (c != null)
				DEBUG_TASK.log(msg + ": " + c.getName());
			else
				DEBUG_TASK.log(msg + ": no connection");
		}
	}

	/** Debug a task */
	static private void debugTask(String msg, String n) {
		if (DEBUG_TASK.isOpen())
			DEBUG_TASK.log(msg + ": " + n);
	}

	/** Minimum elapsed time to log tasks */
	static private final int MIN_ELAPSED_LOG_MS = 500;

	/** Debug task elapsed time */
	static void debugElapsed(String msg, long el) {
		if (el > MIN_ELAPSED_LOG_MS)
			DEBUG_TIME.log(msg + " ELAPSED: " + Long.toString(el));
	}

	/** Task processor work */
	static abstract private class TaskWork extends Work {
		private final String name;
		private final ConnectionImpl conn;
		private TaskWork(String n, ConnectionImpl c) {
			name = n;
			conn = c;
		}
		private TaskWork(String n) {
			this(n, null);
		}
		@Override public final void perform() throws Exception {
			final boolean op = DEBUG_TIME.isOpen();
			final long st = (op) ? currentTimeMillis() : 0;
			try {
				debugTask(name, conn);
				doPerform();
			}
			finally {
				if (op) {
					long el = currentTimeMillis() - st;
					debugElapsed(name, el);
				}
			}
		}
		abstract protected void doPerform() throws Exception;
	}

	/** SONAR namespace being served */
	private final ServerNamespace namespace;

	/** Server properties */
	private final Properties props;

	/** Access logger */
	private final AccessLogger access_logger;

	/** SSL context */
	private final SSLContext context;

	/** Task processor worker */
	private final Worker processor = new Worker("sonar_proc",
 		new ExceptionHandler()
	{
		public boolean handle(Exception e) {
			if (e instanceof CancelledKeyException)
				DEBUG.log("Key already cancelled");
			else if (e instanceof SSLException)
				DEBUG.log("SSL error " + e.getMessage());
			else {
				System.err.println("SONAR " + e.getMessage());
				e.printStackTrace();
			}
			return true;
		}
	});

	/** Authenticator for user credentials */
	private final Authenticator authenticator;

	/** Map of active client connections */
	private final Map<SelectionKey, ConnectionImpl> clients =
		new HashMap<SelectionKey, ConnectionImpl>();

	/** List of active client connections (protected by clients lock) */
	private List<ConnectionImpl> conn_list =
		new ArrayList<ConnectionImpl>();

	/** File to write session list */
	private final String session_file;

	/** User for current message processing */
	private String proc_user = null;

	/** Create a task processor */
	public TaskProcessor(ServerNamespace n, Properties p,
		AccessLogger al, HashProvider hp) throws IOException,
		ConfigurationError
	{
		namespace = n;
		props = p;
		access_logger = al;
		authenticator = new Authenticator(this, hp);
		context = Security.createContext(props);
		LDAPSocketFactory.FACTORY = context.getSocketFactory();
		String url = props.getProperty("sonar.ldap.url");
		if (url != null)
			authenticator.setLdapProvider(new LdapProvider(url));
		session_file = props.getProperty("sonar.session.file");
	}

	/** Create SSL state */
	public SSLState createSSLState(ConnectionImpl conn) throws IOException,
		SSLException
	{
		return new SSLState(conn, context, props, false);
	}

	/** Get the SONAR namespace */
	public ServerNamespace getNamespace() {
		return namespace;
	}

	/** Get user for current message processing */
	public String getProcUser() {
		return proc_user;
	}

	/** Get a list of active connections */
	private List<ConnectionImpl> getConnectionList() {
		synchronized (clients) {
			return conn_list;
		}
	}

	/** Update list of active connections */
	private void updateConnectionList() {
		conn_list = Collections.unmodifiableList(
			new ArrayList<ConnectionImpl>(clients.values()));
	}

	/** Schedule a client connection */
	public void scheduleConnect(final SelectionKey skey,
		final SocketChannel sc)
	{
		processor.addWork(new TaskWork("Connect") {
			protected void doPerform() throws Exception {
				try {
					doConnect(skey, sc);
				}
				catch (Exception e) {
					// Don't leak channels
					skey.cancel();
					sc.close();
					throw e;
				}
			}
		});
	}

	/** Create a client connection */
	private void doConnect(SelectionKey skey, SocketChannel sc)
		throws IOException, NamespaceError
	{
		ConnectionImpl con = new ConnectionImpl(this, skey, sc);
		doAddObject(con);
		access_logger.connect(con.getName());
		synchronized (clients) {
			clients.put(skey, con);
			updateConnectionList();
		}
		updateSessionList();
		// Enable OP_READ interest
		con.disableWrite();
	}

	/** Schedule a disconnect on a selection key */
	public void scheduleDisconnect(final SelectionKey skey) {
		processor.addWork(new TaskWork("Disconnect key") {
			protected void doPerform() {
				disconnect(skey);
			}
		});
	}

	/** Schedule a connection to be disconnected */
	public void scheduleDisconnect(final ConnectionImpl c,
		final String msg)
	{
		processor.addWork(new TaskWork("Disconnect", c) {
			protected void doPerform() {
				if (msg != null)
					c.disconnect(msg);
				else
					c.disconnect();
			}
		});
	}

	/** Disconnect the client associated with the selection key. */
	void disconnect(SelectionKey skey) {
		skey.cancel();
		ConnectionImpl c;
		synchronized (clients) {
			c = clients.remove(skey);
			updateConnectionList();
		}
		debugTask("Disconnecting", c);
		if (c != null) {
			access_logger.disconnect(c.getName(), c.getUserName());
			updateSessionList();
			scheduleRemoveObject(c);
		}
	}

	/** Update list of valid session IDs */
	private void updateSessionList() {
		if (session_file == null)
			return;
		try {
			FileWriter fw = new FileWriter(session_file);
			try {
				for (ConnectionImpl c: getConnectionList()) {
					fw.write(String.valueOf(
						c.getSessionId()));
					fw.append('\n');
				}
			}
			finally {
				fw.close();
			}
		}
		catch (IOException e) {
			DEBUG.log("Error writing session file: " +
				session_file + " (" + e.getMessage() + ")");
		}
	}

	/** Process messages on one connection */
	void processMessages(final ConnectionImpl c) {
		processor.addWork(new TaskWork("Processing msgs", c) {
			protected void doPerform() {
				proc_user = c.getUserName();
				c.processMessages();
				proc_user = null;
			}
		});
	}

	/** Flush outgoing data for one connection */
	void flush(final ConnectionImpl c) {
		processor.addWork(new TaskWork("Flush", c) {
			protected void doPerform() {
				c.flush();
			}
		});
	}

	/** Authenticate a user connection */
	void authenticate(ConnectionImpl c, String name, char[] password) {
		if (DEBUG.isOpen())
			DEBUG.log("authenticating " + name + " on " + c);
		UserImpl user = lookupUser(name);
		if (user != null)
			authenticator.authenticate(c, user, password);
		else
			failLogin(c, name, false);
	}

	/** Lookup a user by name. */
	private UserImpl lookupUser(String n) {
		return (UserImpl) namespace.lookupObject(User.SONAR_TYPE, n);
	}

	/** Finish a LOGIN */
	void finishLogin(final ConnectionImpl c, final UserImpl u) {
		processor.addWork(new TaskWork("Finish LOGIN", c) {
			protected void doPerform() {
				access_logger.authenticate(c.getName(),
					u.getName());
				scheduleSetAttribute(c, "user");
				c.finishLogin(u);
			}
		});
	}

	/** Fail a LOGIN */
	void failLogin(final ConnectionImpl c, final String name,
		final boolean domain)
	{
		processor.addWork(new TaskWork("Fail LOGIN", c) {
			protected void doPerform() {
				if (domain) {
					access_logger.failDomain(c.getName(),
						name);
				} else {
					access_logger.failAuthentication(
						c.getName(), name);
				}
				c.failLogin();
			}
		});
	}

	/** Change a user password */
	void changePassword(ConnectionImpl c, UserImpl u, char[] pwd_current,
		char[] pwd_new)
	{
		authenticator.changePassword(c, u, pwd_current, pwd_new);
	}

	/** Finish a PASSWORD.
	 * @param update Update cached password (true),
	 *               or change password (false). */
	void finishPassword(final ConnectionImpl c, final UserImpl u,
		char[] pwd_new, final boolean update)
	{
		// Need to copy password, since authenticator will clear it
		final String pwd = new String(pwd_new);
		processor.addWork(new TaskWork("Finish PASSWORD", c) {
			protected void doPerform() {
				try {
					u.doSetPassword(pwd);
					if (update) {
						access_logger.updatePassword(
							c.getName(),
							u.getName());
					} else {
						access_logger.changePassword(
							c.getName(),
							u.getName());
					}
				}
				catch (Exception e) {
					failPassword(c, u, e.getMessage());
				}
			}
		});
	}

	/** Fail a PASSWORD */
	void failPassword(final ConnectionImpl c, final UserImpl u,
		final String msg)
	{
		processor.addWork(new TaskWork("Fail PASSWORD", c) {
			protected void doPerform() {
				c.failPassword(msg);
				access_logger.failPassword(c.getName(),
					u.getName());
			}
		});
	}

	/** Lookup the client connection for a selection key */
	public ConnectionImpl lookupClient(SelectionKey skey) {
		synchronized (clients) {
			return clients.get(skey);
		}
	}

	/** Notify all connections watching a name of an object add. */
	private void notifyObject(SonarObject o) {
		Name name = new Name(o);
		for (ConnectionImpl c: getConnectionList())
			c.notifyObject(name, o);
	}

	/** Notify all connections watching a name of an attribute change. */
	void notifyAttribute(Name name, String[] params) {
		if (DEBUG_TASK.isOpen()) {
			debugTask("Notify attribute", name.toString() + " (" +
				processor.size() + ")");
		}
		if (namespace.isGettable(name)) {
			for (ConnectionImpl c: getConnectionList())
				c.notifyAttribute(name, params);
		}
	}

	/** Notify all connections watching a name of an object remove. */
	void notifyRemove(Name name) {
		for (ConnectionImpl c: getConnectionList())
			c.notifyRemove(name);
	}

	/** Schedule an object to be added to the server's namespace */
	public void scheduleAddObject(final SonarObject o) {
		processor.addWork(new TaskWork("Add object") {
			protected void doPerform() throws NamespaceError {
				doAddObject(o);
			}
		});
	}

	/** Perform an add object task. */
	private void doAddObject(SonarObject o) throws NamespaceError {
		debugTask("Adding object", o.getName());
		namespace.addObject(o);
		notifyObject(o);
	}

	/** Create (synchronously) an object in the server's namespace */
	public void storeObject(final SonarObject o) throws SonarException {
		// Calling waitForCompletion will hang if we're
		// running on the task processor thread.
		if (processor.isCurrentThread()) {
			doStoreObject(o);
			return;
		}
		// Array used to capture exception from processor thread
		final SonarException[] se = new SonarException[1];
		Work w = new TaskWork("Store object") {
			protected void doPerform() {
				try {
					doStoreObject(o);
				}
				catch (SonarException e) {
					se[0] = e;
				}
			}
		};
		processor.addWork(w);
		try {
			// Only wait for 30 seconds before giving up
			w.waitForCompletion(30000);
		}
		catch (TimeoutException e) {
			throw new SonarException(e);
		}
		// If an exception was captured, wrap and throw
		if (se[0] != null)
			throw new SonarException(se[0]);
	}

	/** Store an object in the server's namespace. */
	void doStoreObject(SonarObject o) throws SonarException {
		debugTask("Storing object", o.getName());
		namespace.storeObject(o);
		notifyObject(o);
	}

	/** Remove the specified object from the server's namespace */
	public void scheduleRemoveObject(final SonarObject o) {
		processor.addWork(new TaskWork("Remove object") {
			protected void doPerform() throws SonarException {
				doRemoveObject(o);
			}
		});
	}

	/** Perform a remove object task. */
	private void doRemoveObject(SonarObject o) throws SonarException {
		debugTask("Removing object", o.getName());
		notifyRemove(new Name(o));
		namespace.removeObject(o);
	}

	/** Set the specified attribute in the server's namespace */
	public void scheduleSetAttribute(SonarObject o, String a) {
		final Name name = new Name(o, a);
		processor.addWork(new TaskWork("Set attribute") {
			protected void doPerform() throws SonarException {
				doSetAttribute(name);
			}
		});
	}

	/** Perform a "set attribute" task. */
	private void doSetAttribute(Name name) throws SonarException {
		String[] v = namespace.getAttribute(name);
		notifyAttribute(name, v);
	}
}
