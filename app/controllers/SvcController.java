package controllers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomUtils;

import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

@With(SvcAction.class)
public class SvcController extends Controller {
	private HttpExecutionContext ec;
	
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
		final Logger.ALogger log = play.Logger.of("application." +svcName + "-Controller");
		
		log.info("Path: {}", ctx().request().path());
		
		return CompletableFuture.supplyAsync(() -> {
			log.info("Received request");
			long nap = RandomUtils.nextLong(10, 5000);
			try {
				Thread.sleep(nap);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			JsonNode json = Json.newObject().set("slept", Json.newObject().numberNode(nap));

			log.info("Respond request");
			return ok(Json.stringify(json));
		}, ec.current()).exceptionally(throwable -> {
			return badRequest("");
		});
	}

}