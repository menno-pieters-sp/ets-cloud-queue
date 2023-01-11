package sailpoint.ets.cloud.queue.rest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;

import sailpoint.ets.cloud.queue.api.AuthorizationException;
import sailpoint.ets.cloud.queue.api.AuthorizationHelper;
import sailpoint.ets.cloud.queue.api.ETSContext;
import sailpoint.ets.cloud.queue.tools.Util;

@Path("admin")
public class Admin {

	public static final Logger log = LogManager.getLogger(Admin.class);

	public Admin() {
		super();
		if (log.isDebugEnabled()) {
			log.debug("Constructor: Admin()");
		}
	}
	
	private void authenticate(ETSContext context, String authHeader) throws AuthorizationException, FileNotFoundException, IOException {
		Map<String, String> credentials = AuthorizationHelper.getBasicCredentials(authHeader);
		if (credentials == null) {
			throw new AuthorizationException("Basic Authentication Required");
		}
		context.authorizeAdmin(credentials);
	}
	
	@GET
	@Path("test")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> test(@Context HttpServletRequest servletRequest, @HeaderParam("Authorization") String authHeader) {
		ETSContext eTSContext;
		Map<String,Object> result = new HashMap<String,Object>();
		try {
			eTSContext = ETSContext.getCurrentContext(servletRequest);
			authenticate(eTSContext, authHeader);
			result.put("status", "success");
		} catch (AuthorizationException | IOException | SQLException e) {
			result.put("status", "error");
			result.put("error", e.getMessage());
		}
		return result;
	}

	@GET
	@Path("uuid")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> uuid(@Context HttpServletRequest servletRequest, @HeaderParam("Authorization") String authHeader) {
		ETSContext eTSContext;
		Map<String,Object> result = new HashMap<String,Object>();
		try {
			eTSContext = ETSContext.getCurrentContext(servletRequest);
			authenticate(eTSContext, authHeader);
			result.put("status", "success");
			result.put("uuid", Util.uuid());
		} catch (AuthorizationException | IOException | SQLException e) {
			result.put("status", "error");
			result.put("error", e.getMessage());
		}
		return result;
	}
	
	@GET
	@Path("queue/list")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> queueList(@Context HttpServletRequest servletRequest, @HeaderParam("Authorization") String authHeader) {
		ETSContext eTSContext;
		Map<String,Object> result = new HashMap<String,Object>();
		try {
			eTSContext = ETSContext.getCurrentContext(servletRequest);
			authenticate(eTSContext, authHeader);
			result.put("queues", eTSContext.getQueues());
			result.put("status", "success");
		} catch (AuthorizationException | IOException | SQLException e) {
			result.put("status", "error");
			result.put("error", e.getMessage());
		}
		return result;
	}
	
	@POST
	@Path("queue/create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> queueCreate(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @HeaderParam("Authorization") String authHeader, Map<String, Object> data)
			throws FileNotFoundException, IOException, SQLException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: queueCreate(%s, %s, %s, %s)", servletRequest, servletResponse, authHeader, data));
		}
		ETSContext eTSContext = ETSContext.getCurrentContext(servletRequest);
		authenticate(eTSContext, authHeader);
		String description = Util.otos(data.get("description"));
		Map<String, String> result = new HashMap<String, String>();
		try {
			result = eTSContext.createQueue(description);
		} catch (IOException | SQLException e) {
			servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			result.put("status", "error");
			result.put("error", e.getMessage());
		}
		return result;
	}
	
	@DELETE
	@Path("queue/delete/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> queueDelete(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @HeaderParam("Authorization") String authHeader, @PathParam("id") String id)
			throws FileNotFoundException, IOException, SQLException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: queueDelete(%s, %s, %s, %s)", servletRequest, servletResponse, authHeader, id));
		}
		ETSContext eTSContext = ETSContext.getCurrentContext(servletRequest);
		authenticate(eTSContext, authHeader);
		Map<String, String> result = new HashMap<String, String>();
		try {
			eTSContext.deleteQueue(id);
			result.put("status", "success");
		} catch (IOException | SQLException e) {
			servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			result.put("status", "error");
			result.put("error", e.getMessage());
		}
		return result;
	}

	@GET
	@Path("user/list")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> userList(@Context HttpServletRequest servletRequest, @HeaderParam("Authorization") String authHeader) {
		ETSContext eTSContext;
		Map<String,Object> result = new HashMap<String,Object>();
		try {
			eTSContext = ETSContext.getCurrentContext(servletRequest);
			authenticate(eTSContext, authHeader);
			result.put("users", eTSContext.getUsers());
			result.put("status", "success");
		} catch (AuthorizationException | IOException | SQLException e) {
			result.put("status", "error");
			result.put("error", e.getMessage());
		}
		return result;
	}
	
	@POST
	@Path("user/create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> userCreate(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @HeaderParam("Authorization") String authHeader, Map<String, Object> data)
			throws FileNotFoundException, IOException, SQLException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: queueCreate(%s, %s, %s, %s)", servletRequest, servletResponse, authHeader, data));
		}
		ETSContext eTSContext = ETSContext.getCurrentContext(servletRequest);
		authenticate(eTSContext, authHeader);
		String name = Util.otos(data.get("name"));
		String displayName = Util.otos(data.get("displayName"));
		boolean active = Util.otob(data.get("active"), true);
		Map<String, String> result = new HashMap<String, String>();
		try {
			result = eTSContext.createUser(name, displayName, active);
		} catch (IOException | SQLException e) {
			servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			result.put("status", "error");
			result.put("error", e.getMessage());
		}
		return result;
	}
	
	@POST
	@Path("user/authorization")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> userAuthorization(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @HeaderParam("Authorization") String authHeader, Map<String, Object> data)
			throws FileNotFoundException, IOException, SQLException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: queueCreate(%s, %s, %s, %s)", servletRequest, servletResponse, authHeader, data));
		}
		ETSContext eTSContext = ETSContext.getCurrentContext(servletRequest);
		authenticate(eTSContext, authHeader);
		String userId = Util.otos(data.get("userId"));
		String queueId = Util.otos(data.get("queueId"));
		boolean remove = Util.otob(data.get("remove"), false);
		boolean read = Util.otob(data.get("read"), false);
		boolean write = Util.otob(data.get("write"), false);
		Map<String, String> result = new HashMap<String, String>();
		try {
			if (remove) {
				eTSContext.unsetUserAuthorization(userId, queueId);
			} else {
				eTSContext.setUserAuthorization(userId, queueId, read, write);				
			}
			result.put("status", "success");
		} catch (IOException | SQLException e) {
			servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			result.put("status", "error");
			result.put("error", e.getMessage());
		}
		return result;
	}
	
	@DELETE
	@Path("user/delete/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> userDelete(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @HeaderParam("Authorization") String authHeader, @PathParam("id") String id)
			throws FileNotFoundException, IOException, SQLException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: userDelete(%s, %s, %s, %s)", servletRequest, servletResponse, authHeader, id));
		}
		ETSContext eTSContext = ETSContext.getCurrentContext(servletRequest);
		authenticate(eTSContext, authHeader);
		Map<String, String> result = new HashMap<String, String>();
		try {
			eTSContext.deleteUser(id);
			result.put("status", "success");
		} catch (IOException | SQLException e) {
			servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			result.put("status", "error");
			result.put("error", e.getMessage());
		}
		return result;
	}
	
	@POST
	@Path("token/create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> tokenCreate(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @HeaderParam("Authorization") String authHeader, Map<String, Object> data)
			throws FileNotFoundException, IOException, SQLException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: queueCreate(%s, %s, %s, %s)", servletRequest, servletResponse, authHeader, data));
		}
		ETSContext eTSContext = ETSContext.getCurrentContext(servletRequest);
		authenticate(eTSContext, authHeader);
		String userId = Util.otos(data.get("userId"));
		String description = Util.otos(data.get("description"));
		Map<String, String> result = new HashMap<String, String>();
		try {
			Object expirationObject = data.get("expiration");
			Date expiration = Util.objectToDate(expirationObject);
			result = eTSContext.createUserToken(userId, description, expiration);
		} catch (IOException | ParseException | SQLException e) {
			servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			result.put("status", "error");
			result.put("error", e.getMessage());
		}
		return result;
	}
	
	@DELETE
	@Path("token/delete/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> tokenDelete(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @HeaderParam("Authorization") String authHeader, @PathParam("id") String id)
			throws FileNotFoundException, IOException, SQLException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: tokenDelete(%s, %s, %s, %s)", servletRequest, servletResponse, authHeader, id));
		}
		ETSContext eTSContext = ETSContext.getCurrentContext(servletRequest);
		authenticate(eTSContext, authHeader);
		Map<String, String> result = new HashMap<String, String>();
		try {
			eTSContext.deleteUserToken(id);
			result.put("status", "success");
		} catch (IOException | SQLException e) {
			servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			result.put("status", "error");
			result.put("error", e.getMessage());
		}
		return result;
	}
	
	@GET
	@Path("hash/{password}")
	@Produces(MediaType.APPLICATION_JSON)
	public String hashPassword(@PathParam("password") String password) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: hashPassword(%s)", password));
		}
		String salt = Util.generateSalt();
		String hash = AuthorizationHelper.ssha256(salt, password);
		return hash;
	}
}
