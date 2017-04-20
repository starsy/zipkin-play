package controllers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomUtils;

import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;


class Reply {
	public long sleepTime;
	public String service;
	
	public Reply(String s, long t) {
		sleepTime = t;
		service = s;
	}

	public long getSleepTime() {
		return sleepTime;
	}

	public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}
}

@With(SvcAction.class)
public class SvcController extends Controller {
	private HttpExecutionContext ec;
	
	@Inject WSClient ws;
	
	@Inject
	public SvcController(HttpExecutionContext ec) {
		this.ec = ec;
	}
	
	String getSvcName() {
		String path = ctx().request().path();
		return path.substring(1);
	}
	
	public CompletionStage<Result> index() {
		String svcName = getSvcName();
		final Logger.ALogger log = play.Logger.of("application.Controller-" + svcName);
		
		log.info("Path: {}", ctx().request().path());
		
		return CompletableFuture.supplyAsync(() -> {
			log.info("Received request");
			long nap = RandomUtils.nextLong(10, 5000);
			try {
				Thread.sleep(nap);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			JsonNode json = Json.toJson(new Reply(svcName, nap));

			log.info("Respond request");
			return ok(Json.stringify(json));
		}, ec.current()).exceptionally(throwable -> {
			return badRequest("");
		});
	}

}