package sailpoint.ets.cloud.queue.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("ping")
public class Ping {

	@GET
	public String getPing() {
		return "Pong";
	}
}
