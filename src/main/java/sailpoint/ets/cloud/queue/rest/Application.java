package sailpoint.ets.cloud.queue.rest;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("rest")
public class Application extends ResourceConfig {

	public Application() {
		registerClasses(Application.class);
	}

}
