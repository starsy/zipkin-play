package controllers;

import static org.junit.Assert.*;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.Logger;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

public class SvcControllerTest {
	private final Logger.ALogger log = play.Logger.of("application.SvcControllerTest");
	
	@Inject
	WSClient ws;

	@Before
	public void setUp() throws Exception {
		ws = WS.newClient(9000);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		String url = "http://localhost:9000/svc1";
		WSRequest req = ws.url(url);
		
	
		log.info("Calling new service: {}", url);
		CompletionStage<WSResponse> responsePromise = req.setContentType("application/json")
				.setRequestTimeout(-1).setBody("").get();
		log.info("Got the promise for new service: {}", url);
		responsePromise.thenAccept((r) -> {
			log.info("Service [{}] -> Service [{}], response: {}", "a", "b", r.getBody());
		}).thenRun(() -> {
            try {
                ws.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
		
		while(true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

}
