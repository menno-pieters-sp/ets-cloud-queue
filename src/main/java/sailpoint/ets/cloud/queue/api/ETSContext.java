/**
 * Copyright (C) 2022-2023 SailPoint Technologies
 */
package sailpoint.ets.cloud.queue.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import sailpoint.ets.cloud.queue.tools.Util;

/**
 * 
 * @author menno.pieters
 *
 * A central helper class for the ETS queue application.
 * 
 */
public class ETSContext {

	private HttpServletRequest servletRequest = null;

	private static final Logger log = LogManager.getLogger(ETSContext.class);
	private static BasicDataSource dataSource = null;
	private static Connection connection = null;
	private static Properties properties = null;
	private int maxEntryAge = 3600;
	
	public final static String PROPERTY_TOKEN_SALT = "tokenSalt";
	
	private ETSContext() {
		throw new WebServiceException("HttpServletRequest required");
	}

	private ETSContext(HttpServletRequest servletRequest) throws FileNotFoundException, IOException, SQLException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Constructor: ETSContext(%s)", servletRequest));
		}
		this.servletRequest = servletRequest;
		init();
	}

	/**
	 * Open a datasource to the database if needed and return the database datasource.
	 * 
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private BasicDataSource getDataSource() throws FileNotFoundException, IOException {
		if (log.isDebugEnabled()) {
			log.debug("Enter: getDataSource()");
		}
		if (dataSource == null || dataSource.isClosed()) {
			Properties properties = getProperties();
			dataSource = new BasicDataSource();
			dataSource.setUrl(properties.getProperty("db.pool.url"));
			dataSource.setUsername(properties.getProperty("db.pool.user"));
			dataSource.setPassword(properties.getProperty("db.pool.password"));
			dataSource.setDriverClassName(properties.getProperty("db.pool.driver"));
			dataSource.setMinIdle(5);
			dataSource.setMaxIdle(10);
			dataSource.setMaxOpenPreparedStatements(100);
		}
		return dataSource;
	}

	/**
	 * Create a database connection if needed. Return the connection.
	 * 
	 * @return
	 * @throws FileNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	private Connection getConnection() throws FileNotFoundException, SQLException, IOException {
		if (log.isDebugEnabled()) {
			log.debug("Enter: getConnection()");
		}
		if (connection == null || connection.isClosed()) {
			connection = getDataSource().getConnection();
		}
		return connection;
	}

	/**
	 * Open the configuration file and return the contents.
	 * 
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private Properties getProperties() throws FileNotFoundException, IOException {
		if (log.isDebugEnabled()) {
			log.debug("Enter: getProperties()");
		}
		if (properties == null) {
			properties = new Properties();
			String basePath = servletRequest.getServletContext().getRealPath("/");
			properties.load(new FileInputStream(new File(basePath + "WEB-INF/classes/queue.properties")));
		}
		return properties;
	}

	/**
	 * Initialize the class. Get the configuration and open the database connection.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	private void init() throws FileNotFoundException, IOException, SQLException {
		if (log.isDebugEnabled()) {
			log.debug("Enter: init()");
			log.debug(new File("").getAbsolutePath());
		}
		getProperties();
		getDataSource();
		this.maxEntryAge = Util.otoi(properties.getProperty("db.queue.maxage", "3600"));
	}

	/**
	 * Get a fresh instance of this context.
	 * 
	 * @param servletRequest
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	public static ETSContext getContext(HttpServletRequest servletRequest) throws FileNotFoundException, IOException, SQLException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: getCurrentContext(%s)", servletRequest));
		}
		return new ETSContext(servletRequest);
	}
	
	/**
	 * Reload the configuration.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void reload() throws FileNotFoundException, IOException, SQLException {
		if (connection != null && !connection.isClosed()) {
			connection.close();
		}
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
		}
		properties = null;
		init();
	}

	/**
	 * Clean the queue. Any entries older than the maximum age (in seconds) specfied in the configuration file will be removed.
	 * 
	 * @param queue	The id of the queue to clean.
	 */
	public void cleanQueue(String queue) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: cleanQueue(%s)", queue));
		}
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, -1 * maxEntryAge);
		java.util.Date date = cal.getTime();
		if (Util.isNotNullOrEmpty(queue)) {
			String query = "DELETE FROM ets_queue_entry WHERE queue_id = ? AND created < ?";
			PreparedStatement statement = null;
			try {
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, queue);
				Timestamp t = new Timestamp(date.getTime());
				statement.setTimestamp(2, t);
				statement.execute();
			} catch (SQLException | IOException e) {
				throw new WebServiceException(e);
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		}
	}

	/**
	 * Write a new entry to the specified queue.
	 * 
	 * @param queue	The id of the queue to use.
	 * @param data	The data to be stored.
	 */
	public void writeDataToQueue(String queue, String data) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: writeDataToQueue(%s, %s)", queue, "********"));
		}
		if (Util.isNotNullOrEmpty(queue) && Util.isNotNullOrEmpty(data)) {
			String query = "INSERT INTO ets_queue_entry (queue_id, data) VALUES (?, ?)";
			PreparedStatement statement = null;
			try {
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, queue);
				statement.setString(2, data);
				statement.execute();
				// cleanQueue(queue);
			} catch (SQLException | IOException e) {
				throw new WebServiceException(e);
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		}
	}

	/**
	 * Remove data from the queue.
	 * 
	 * @param queue	The id of the queue.
	 * @param id	The id of the entry to be removed.	 * 
	 */
	public void removeQueueData(String queue, String id) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: removeQueueData(%s, %s)", queue, id));
		}
		if (Util.isNotNullOrEmpty(queue) && Util.isNotNullOrEmpty(id)) {
			String query = "DELETE FROM ets_queue_entry WHERE queue_id = ? AND id = ?";
			PreparedStatement statement = null;
			try {
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, queue);
				statement.setString(2, id);
				statement.execute();
			} catch (SQLException | IOException e) {
				throw new WebServiceException(e);
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		}
	}

	/**
	 * Check whether any entries exist in the specified queue.
	 * 
	 * @param queue	The queue to check.
	 * @return
	 */
	public boolean queueHasMore(String queue) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: queueHasMore(%s)", queue));
		}
		if (Util.isNotNullOrEmpty(queue)) {
			String query = "SELECT COUNT(*) FROM ets_queue_entry WHERE queue_id = ? ORDER BY created";
			PreparedStatement statement = null;
			try {
				// cleanQueue(queue);
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, queue);
				statement.setMaxRows(1);
				ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					int c = rs.getInt(1);
					return (c > 0);
				}
			} catch (SQLException | IOException e) {
				throw new WebServiceException(e);
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		}
		return false;
	}

	/**
	 * Get an entry from the specified queue and remove if remove is set to true.
	 * 
	 * @param queue	The id of the queue.
	 * @param remove	Remove entry if true.
	 * @return
	 */
	public String pollQueueData(String queue, boolean remove) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: pollQueueData(%s)", queue));
		}
		if (Util.isNotNullOrEmpty(queue)) {
			String query = "SELECT id,data FROM ets_queue_entry WHERE queue_id = ? ORDER BY created";
			PreparedStatement statement = null;
			try {
				// cleanQueue(queue);
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, queue);
				statement.setMaxRows(1);
				ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					String id = rs.getString("id");
					String data = rs.getString("data");
					if (remove) {
						removeQueueData(queue, id);
					}
					return data;
				}
			} catch (SQLException | IOException e) {
				throw new WebServiceException(e);
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		}
		return null;
	}
	
	/**
	 * Hash a string using a random salt.
	 * 
	 * @param token
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private String hashToken(final String token) throws FileNotFoundException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: hashToken(%s)", "**********"));
		}
		Properties properties = getProperties();
		String salt = properties.getProperty(PROPERTY_TOKEN_SALT);
		String newToken = token;
		if (Util.isNotNullOrEmpty(salt)) {
			newToken = AuthorizationHelper.ssha256(salt, token);
		} else {
			log.error(String.format("Property %s not configured", PROPERTY_TOKEN_SALT));
		}
		return newToken;
	}

	/**
	 * Check whether a user, to whom the provided token belongs, is authorized to write to the specified queue.
	 * 
	 * @param token	Plain text token.
	 * @param queue	Id of the queue.
	 * @throws AuthorizationException
	 */
	public void authorizeWrite(String token, String queue) throws AuthorizationException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: authorizeWrite(********, %s)", queue));
		}
		if (Util.isNotNullOrEmpty(token) && Util.isNotNullOrEmpty(queue)) {
			String query = "SELECT COUNT(*) FROM ets_queue_access xs, ets_user_token t, ets_user u WHERE u.id = xs.user_id AND u.active = 1 AND xs.user_id = t.user_id AND xs.queue_id = ? AND t.token = ? AND xs.write = 1 AND (t.expiration IS NULL OR t.expiration > ?);";
			PreparedStatement statement = null;
			try {
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, queue);
				statement.setString(2, hashToken(token));
				Timestamp t = new Timestamp(new java.util.Date().getTime());
				statement.setTimestamp(3, t);
				ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					int c = rs.getInt(1);
					if (c > 0) {
						// Success!
						return;
					}
				}
				throw new AuthorizationException("Access Denied");
			} catch (SQLException | IOException e) {
				throw new AuthorizationException(e);
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		} else {
			throw new AuthorizationException("Invalid Credentials");
		}
	}

	/**
	 * Check whether a user, to whom the provided token belongs, is authorized to read from the specified queue.
	 * 
	 * @param token	Plain text token.
	 * @param queue	Id of the queue.
	 * @throws AuthorizationException
	 */
	public void authorizeRead(String token, String queue) throws AuthorizationException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: authorizeRead(********, %s)", queue));
		}
		if (Util.isNotNullOrEmpty(token) && Util.isNotNullOrEmpty(queue)) {
			String query = "SELECT COUNT(*) FROM ets_queue_access xs, ets_user_token t, ets_user u WHERE u.id = xs.user_id AND u.active = 1 AND  xs.user_id = t.user_id AND xs.queue_id = ? AND t.token = ? AND xs.read = 1 AND (t.expiration IS NULL OR t.expiration > ?);";
			PreparedStatement statement = null;
			try {
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, queue);
				statement.setString(2, hashToken(token));
				Timestamp t = new Timestamp(new java.util.Date().getTime());
				statement.setTimestamp(3, t);
				ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					int c = rs.getInt(1);
					if (c > 0) {
						// Success!
						return;
					}
				}
				throw new AuthorizationException("Access Denied");
			} catch (SQLException | IOException e) {
				throw new AuthorizationException(e);
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		} else {
			log.error("Invalid credentials");
			throw new AuthorizationException("Invalid Credentials");
		}
	}

	/**
	 * Check the provided admin username and password.
	 * 
	 * @param username
	 * @param password
	 * @throws AuthorizationException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void authorizeAdmin(String username, String password) throws AuthorizationException, FileNotFoundException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: authorizeAdmin(%s, %s)", username, "********"));
		}
		if (Util.isNullOrEmpty(username) || Util.isNullOrEmpty(password)) {
			throw new AuthorizationException("Invalid Credentials");
		}
		Properties properties = getProperties();
		String adminUser = properties.getProperty("admin.user");
		if (!username.equals(adminUser)) {
			throw new AuthorizationException("Invalid Credentials");
		}
		String adminPass = properties.getProperty("admin.pass");
		if (AuthorizationHelper.validatePassword(adminPass, password)) {
			return;
		}
		throw new AuthorizationException("Invalid Credentials");
	}

	/**
	 * Check the provided admin username and password.
	 * 
	 * @param credentials
	 * @throws AuthorizationException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void authorizeAdmin(Map<String, String> credentials) throws AuthorizationException, FileNotFoundException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: authorizeAdmin(%s)", (credentials==null)?"null":"Map(********)"));
		}
		if (credentials == null || credentials.isEmpty()) {
			throw new AuthorizationException("Invalid Credentials");
		}
		authorizeAdmin(credentials.get("username"), credentials.get("password"));
	}

	/**
	 * List all available queues.
	 * 
	 * @return
	 */
	public List<Map<String, String>> getQueues() {
		if (log.isDebugEnabled()) {
			log.debug("Enter: getQueues()");
		}
		/* TODO: paging? */
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		String query = "SELECT * FROM ets_queue";
		PreparedStatement statement = null;
		try {
			Connection connection = getConnection();
			statement = connection.prepareStatement(query);
			statement.setMaxRows(1000); // TODO: add paging
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				ResultSetMetaData rsmd = rs.getMetaData();
				int n = rsmd.getColumnCount();
				if (n > 0) {
					Map<String, String> row = new HashMap<String, String>();
					for (int c = 1; c <= n; c++) {
						String name = rsmd.getColumnName(c);
						String value = rs.getString(c);
						row.put(name, value);
					}
					result.add(row);
				}
			}
		} catch (SQLException | IOException e) {
			throw new AuthorizationException(e);
		} finally {
			try {
				if (statement != null && !statement.isClosed()) {
					statement.close();
				}
			} catch (SQLException e) {
				log.error(e);
				// Silently ignore
			}
		}
		return result;
	}

	/**
	 * Create a new queue.
	 * 
	 * @param description	Description of the new queue.
	 * @return
	 * @throws SQLException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Map<String, String> createQueue(String description) throws SQLException, FileNotFoundException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: createQueue(%s)", description));
		}
		Map<String, String> result = new HashMap<String, String>();
		String query = "INSERT INTO ets_queue (id, description) VALUES (?, ?)";
		PreparedStatement statement = null;
		try {
			String uuid = Util.uuid();
			Connection connection = getConnection();
			statement = connection.prepareStatement(query);
			statement.setString(1, uuid);
			statement.setString(2, description);
			statement.execute();
			result.put("id", uuid);
			result.put("status", "success");
		} finally {
			try {
				if (statement != null && !statement.isClosed()) {
					statement.close();
				}
			} catch (SQLException e) {
				log.error(e);
				// Silently ignore
			}
		}
		return result;
	}

	/**
	 * Remove the specified queue.
	 * 
	 * @param id
	 * @throws SQLException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void deleteQueue(String id) throws SQLException, FileNotFoundException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: deleteQueue(%s)", id));
		}
		if (Util.isNotNullOrEmpty(id)) {
			String query = "DELETE FROM ets_queue WHERE id = ?";
			PreparedStatement statement = null;
			try {
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, id);
				statement.execute();
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		}
	}

	/**
	 * Get the queues and authorizations to queues for the specified user.
	 * 
	 * @param user_id
	 * @return
	 * @throws FileNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public List<Map<String, Object>> getUserQueueAccess(String user_id) throws FileNotFoundException, SQLException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: getUserQueueAccess(%s)", user_id));
		}
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		if (Util.isNotNullOrEmpty(user_id)) {
			String query = "SELECT q.id, q.description, a.read, a.write FROM ets_queue q, ets_queue_access a WHERE a.user_id = ? AND q.id = a.queue_id";
			PreparedStatement statement = null;
			try {
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, user_id);
				statement.setMaxRows(1000); // TODO: add paging
				ResultSet rs = statement.executeQuery();
				while (rs.next()) {
					String id = rs.getString("id");
					String description = rs.getString("description");
					boolean read = rs.getBoolean("read");
					boolean write = rs.getBoolean("write");
					Map<String, Object> row = new HashMap<String, Object>();
					row.put("id", id);
					row.put("description", description);
					row.put("read", read);
					row.put("write", write);
					result.add(row);
				}
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		}
		return result;
	}

	/**
	 * Get an overview of tokens generated for the user. This does not include the actual token, but just the id and description.
	 * 
	 * @param user_id
	 * @return
	 * @throws FileNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public List<Map<String, Object>> getUserTokens(String user_id) throws FileNotFoundException, SQLException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: getUserTokens(%s)", user_id));
		}
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		if (Util.isNotNullOrEmpty(user_id)) {
			String query = "SELECT id, description, expiration FROM ets_user_token WHERE user_id = ?";
			PreparedStatement statement = null;
			try {
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, user_id);
				statement.setMaxRows(1000); // TODO: add paging
				ResultSet rs = statement.executeQuery();
				while (rs.next()) {
					String id = rs.getString("id");
					String description = rs.getString("description");
					Date expiration = rs.getDate("expiration");
					Map<String, Object> row = new HashMap<String, Object>();
					row.put("id", id);
					row.put("description", description);
					row.put("expiration", expiration);
					result.add(row);
				}
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		}
		return result;
	}

	/**
	 * List all users.
	 * 
	 * @return
	 */
	public List<Map<String, Object>> getUsers() {
		if (log.isDebugEnabled()) {
			log.debug("Enter: getUsers()");
		}
		/**
		 * TODO: paging?
		 */
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		String query = "SELECT id, name, display_name, active FROM ets_user";
		PreparedStatement statement = null;
		try {
			Connection connection = getConnection();
			statement = connection.prepareStatement(query);
			statement.setMaxRows(1000); // TODO: add paging
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				Map<String, Object> row = new HashMap<String, Object>();
				String id = rs.getString("id");
				String name = rs.getString("name");
				String displayName = rs.getString("display_name");
				boolean active = rs.getBoolean("active");
				row.put("id", id);
				row.put("name", name);
				row.put("displayName", displayName);
				row.put("active", active);
				result.add(row);
				List<Map<String, Object>> tokens = getUserTokens(id);
				if (tokens != null && !tokens.isEmpty()) {
					row.put("tokens", tokens);
				}
				List<Map<String, Object>> queues = getUserQueueAccess(id);
				if (queues != null && !queues.isEmpty()) {
					row.put("queues", queues);
				}
			}
		} catch (SQLException | IOException e) {
			throw new AuthorizationException(e);
		} finally {
			try {
				if (statement != null && !statement.isClosed()) {
					statement.close();
				}
			} catch (SQLException e) {
				log.error(e);
				// Silently ignore
			}
		}
		return result;
	}

	/**
	 * Create a new token for a user, optionally with an expiration date.
	 * 
	 * @param user_id
	 * @param description
	 * @param expiration
	 * @return
	 * @throws SQLException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Map<String, String> createUserToken(String user_id, String description, java.util.Date expiration) throws SQLException, FileNotFoundException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: createUserToken(%s, %s, %b)", user_id, description, expiration));
		}
		Map<String, String> result = new HashMap<String, String>();
		String query = "INSERT INTO ets_user_token (id, token, user_id, description, expiration) VALUES (?, ?, ?, ?, ?)";
		PreparedStatement statement = null;
		try {
			String uuid = Util.uuid();
			String token = Util.generateToken(64);
			Connection connection = getConnection();
			statement = connection.prepareStatement(query);
			statement.setString(1, uuid);
			statement.setString(2, hashToken(token));
			statement.setString(3, user_id);
			statement.setString(4, description);
			Timestamp t = (expiration==null?null:new Timestamp(expiration.getTime()));
			statement.setTimestamp(5, t);
			statement.execute();
			result.put("id", uuid);
			result.put("token", token);
			result.put("status", "success");
		} finally {
			try {
				if (statement != null && !statement.isClosed()) {
					statement.close();
				}
			} catch (SQLException e) {
				log.error(e);
				// Silently ignore
			}
		}
		return result;
	}

	/**
	 * Delete a user token.
	 * 
	 * @param id
	 * @throws SQLException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void deleteUserToken(String id) throws SQLException, FileNotFoundException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: deleteUserToken(%s)", id));
		}
		if (Util.isNotNullOrEmpty(id)) {
			String query = "DELETE FROM ets_user_token WHERE id = ?";
			PreparedStatement statement = null;
			try {
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, id);
				statement.execute();
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		}
	}

	/**
	 * Create a new user.
	 * 
	 * @param name
	 * @param displayName
	 * @param active
	 * @return
	 * @throws SQLException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Map<String, String> createUser(String name, String displayName, boolean active) throws SQLException, FileNotFoundException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: createUser(%s, %s, %b)", name, displayName, active));
		}
		Map<String, String> result = new HashMap<String, String>();
		String query = "INSERT INTO ets_user (id, name, display_name, active) VALUES (?, ?, ?, ?)";
		PreparedStatement statement = null;
		try {
			String uuid = Util.uuid();
			Connection connection = getConnection();
			statement = connection.prepareStatement(query);
			statement.setString(1, uuid);
			statement.setString(2, name);
			statement.setString(3, displayName);
			statement.setBoolean(4, active);
			statement.execute();
			result.put("id", uuid);
			result.put("status", "success");
		} finally {
			try {
				if (statement != null && !statement.isClosed()) {
					statement.close();
				}
			} catch (SQLException e) {
				log.error(e);
				// Silently ignore
			}
		}
		return result;
	}

	/**
	 * Delete a user.
	 * 
	 * @param id
	 * @throws SQLException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void deleteUser(String id) throws SQLException, FileNotFoundException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: deleteUser(%s)", id));
		}
		if (Util.isNotNullOrEmpty(id)) {
			String query = "DELETE FROM ets_user WHERE id = ?";
			PreparedStatement statement = null;
			try {
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, id);
				statement.execute();
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		}
	}

	/**
	 * Set authorizations for a user on a queue.
	 * 
	 * @param userId
	 * @param queueId
	 * @param read
	 * @param write
	 * @throws FileNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public void setUserAuthorization(String userId, String queueId, boolean read, boolean write) throws FileNotFoundException, SQLException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: setUserAuthorization(%s, %s, %b, %b)", userId, queueId, read, write));
		}
		if (Util.isNotNullOrEmpty(userId) && Util.isNotNullOrEmpty(queueId)) {
			String query = "INSERT INTO ets_queue_access (`queue_id`, `user_id`, `read`, `write`) VALUES (?, ?, ?, ?)";
			PreparedStatement statement = null;
			try {
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, queueId);
				statement.setString(2, userId);
				statement.setBoolean(3, read);
				statement.setBoolean(4, write);
				statement.execute();
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		}
	}

	/**
	 * Remove all authorizations for a user on the specified queue.
	 * 
	 * @param userId
	 * @param queueId
	 * @throws FileNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public void unsetUserAuthorization(String userId, String queueId) throws FileNotFoundException, SQLException, IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: unsetUserAuthorization(%s, %s)", userId, queueId));
		}
		if (Util.isNotNullOrEmpty(userId) && Util.isNotNullOrEmpty(queueId)) {
			String query = "DELETE FROM ets_queue_access WHERE queue_id = ? AND user_id = ?";
			PreparedStatement statement = null;
			try {
				Connection connection = getConnection();
				statement = connection.prepareStatement(query);
				statement.setString(1, queueId);
				statement.setString(2, userId);
				statement.execute();
			} finally {
				try {
					if (statement != null && !statement.isClosed()) {
						statement.close();
					}
				} catch (SQLException e) {
					log.error(e);
					// Silently ignore
				}
			}
		}
	}
	
	/**
	 * Update any plain text tokens to a hashed format.
	 * 
	 * @return
	 * @throws FileNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public int hashTokens() throws FileNotFoundException, SQLException, IOException {
		if (log.isDebugEnabled()) {
			log.debug("Enter: hashTokens()");
		}
		Map<String,String> tokenMap = new HashMap<String,String>();
		int updated = 0;
		String query = "SELECT id, token FROM ets_user_token WHERE NOT(token like '{SSHA256}%')";
		PreparedStatement statement = null;
		ResultSet rs = null;
		try {
			Connection connection = getConnection();
			statement = connection.prepareStatement(query);
			rs = statement.executeQuery();
			while (rs.next()) {
				String id = rs.getString("id");
				String token = rs.getString("token");
				if (log.isTraceEnabled()) {
					log.trace(String.format("hashTokens: Token id: %s", id));
				}
				tokenMap.put(id, token);
			}
			rs.close();
			statement.close();
			if (!tokenMap.isEmpty()) {
				for (String id: tokenMap.keySet()) {
					String token = tokenMap.get(id);
					token = hashToken(token);
					if (token == null || token.equals(tokenMap.get(id))) {
						log.error("No hash update, aborting");
						return -1;
					}
					query = "UPDATE ets_user_token SET token = ? WHERE id = ?";
					statement = connection.prepareStatement(query);
					statement.setString(1, token);
					statement.setString(2, id);
					statement.execute();
					statement.close();
					updated++;
				}
			}			
		} finally {
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
				}
				if (statement != null && !statement.isClosed()) {
					statement.close();
				}
			} catch (SQLException e) {
				log.error(e);
				// Silently ignore
			}
		}
		log.trace(String.format("hashTokens: Rehashed %d tokens", updated));
		return updated;
	}

	/**
	 * To be executed if the class is removed from memory.
	 */
	protected void finalize() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("Enter: finalize()");
		}
		if (connection != null && !connection.isClosed()) {
			connection.close();
		}
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
		}
	}
}
