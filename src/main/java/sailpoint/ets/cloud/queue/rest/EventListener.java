package sailpoint.ets.cloud.queue.rest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;

import sailpoint.ets.cloud.queue.api.AuthorizationException;
import sailpoint.ets.cloud.queue.api.AuthorizationHelper;
import sailpoint.ets.cloud.queue.api.ETSContext;
import sailpoint.ets.cloud.queue.tools.Util;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("event")
public class EventListener {

	public static final Logger log = LogManager.getLogger(EventListener.class);

	public EventListener() {
		super();
		if (log.isDebugEnabled()) {
			log.debug("Constructor: EventListener()");
		}
	}

	@POST
	@Path("trigger/{queue}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> trigger(@Context HttpServletRequest servletRequest,
			@Context HttpServletResponse servletResponse, @HeaderParam("Authorization") String authHeader,
			@PathParam("queue") String queue, Map<String, Object> data)
			throws FileNotFoundException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: trigger(%s, %s, %s, %s, %s)", servletRequest, servletResponse, authHeader,
					queue, data));
		}
		Map<String, Object> result = new HashMap<String, Object>();
		try {
			String token = AuthorizationHelper.getBearerToken(authHeader);
			ETSContext eTSContext = ETSContext.getContext(servletRequest);
			eTSContext.authorizeWrite(token, queue);
			Gson gson = new Gson();
			String dataStr = gson.toJson(data);
			eTSContext.writeDataToQueue(queue, dataStr);
			result.put("status", "success");
		} catch (AuthorizationException e) {
			throw new WebApplicationException(e, Response.Status.UNAUTHORIZED);
		} catch (IOException | SQLException e1) {
			throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
		}
		return result;
	}

	private Map<String, Object> pollInternal(HttpServletRequest servletRequest, String authHeader, String queue,
			boolean remove) throws FileNotFoundException, IOException, SQLException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: pollInternal(%s, %s, %s, %b)", servletRequest, authHeader, queue, remove));
		}
		String token = AuthorizationHelper.getBearerToken(authHeader);
		ETSContext eTSContext = ETSContext.getContext(servletRequest);
		eTSContext.authorizeRead(token, queue);
		Map<String, Object> result = new HashMap<String, Object>();
		String dataStr = eTSContext.pollQueueData(queue, remove);
		if (Util.isNotNullOrEmpty(dataStr)) {
			Gson gson = new Gson();
			@SuppressWarnings("unchecked")
			Map<String, Object> jsonObject = (Map<String, Object>) gson.fromJson(dataStr, result.getClass());
			if (jsonObject != null && !jsonObject.isEmpty()) {
				result = jsonObject;
				if (remove) {
					result.put("__hasMore", eTSContext.queueHasMore(queue));
				}
			}
		/*} else {
			if (remove) {
				result.put("__hasMore", false);
			}*/
		}
		return result;
	}

	@GET
	@Path("poll/{queue}")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> poll(@Context HttpServletRequest servletRequest,
			@Context HttpServletResponse servletResponse, @HeaderParam("Authorization") String authHeader,
			@PathParam("queue") String queue) throws FileNotFoundException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: poll(%s, %s, %s, %s)", servletRequest, servletResponse, authHeader, queue));
		}
		Map<String, Object> result = null;
		try {
			result = pollInternal(servletRequest, authHeader, queue, true);
		} catch (AuthorizationException e) {
			throw new WebApplicationException(e, Response.Status.UNAUTHORIZED);
		} catch (IOException | SQLException e1) {
			throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
		}
		return result;
	}

	@GET
	@Path("peek/{queue}")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> peek(@Context HttpServletRequest servletRequest,
			@Context HttpServletResponse servletResponse, @HeaderParam("Authorization") String authHeader,
			@PathParam("queue") String queue) throws FileNotFoundException, IOException, SQLException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Enter: peek(%s, %s, %s, %s)", servletRequest, servletResponse, authHeader, queue));
		}
		Map<String, Object> result = null;
		try {
			result = pollInternal(servletRequest, authHeader, queue, false);
		} catch (AuthorizationException e) {
			throw new WebApplicationException(e, Response.Status.UNAUTHORIZED);
		} catch (IOException | SQLException e1) {
			throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
		}
		return result;
	}
}
