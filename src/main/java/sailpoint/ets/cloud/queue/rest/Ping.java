/**
 * Copyright (C) 2022-2023 SailPoint Technologies
 */
package sailpoint.ets.cloud.queue.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * 
 * @author menno.pieters
 *
 * Implement a basic endpoint to test connectivity.
 */
@Path("ping")
public class Ping {

	@GET
	public String getPing() {
		return "Pong";
	}
}
