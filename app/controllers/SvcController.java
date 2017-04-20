package controllers;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomUtils;

import com.fasterxml.jackson.databind.JsonNode;

import brave.Span.Kind;
import brave.Tracer;
import brave.propagation.TraceContext;
import play.Logger;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.With;
import zipkin.Endpoint;
import zipkin.Span;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.okhttp3.OkHttpSender;


@With(SvcAction.class)
public class SvcController extends Controller {
	private HttpExecutionContext ec;

	@Inject
	WSClient ws;

	private OkHttpSender sender;

	private AsyncReporter<Span> reporter;

	private Tracer tracer;

	@Inject
	public SvcController(HttpExecutionContext ec) {
		this.ec = ec;
		sender = OkHttpSender.create("http://127.0.0.1:9411/api/v1/spans");
		reporter = AsyncReporter.builder(sender).build();
		tracer = Tracer.newBuilder().localServiceName("zipkin-play").reporter(reporter).build();
	}

	String getSvcName() {
		String path = ctx().request().path();
		return path.substring(1);
	}
	
	Optional<String> getNextSvc() {
		String []list = {"svc1", "svc2", "svc3", "svc4"};
		Optional<String> result = Optional.empty();
		
		int rand = RandomUtils.nextInt(0, list.length + 1);
		
		if (rand < list.length) {
			result = Optional.of(list[rand]);
		}
		
		return result;
	}

	WSRequest injectB3(WSRequest req, TraceContext ctx) {
		return req.setHeader("X─B3─TraceId", String.valueOf(ctx.traceId()))
				.setHeader("X─B3─ParentSpanId", String.valueOf(ctx.parentId()))
				.setHeader("X─B3─SpanId", String.valueOf(ctx.spanId()));
	}
	
	brave.Span extractB3(Request req) {
		String traceIdString = req.getHeader("X─B3─TraceId");
		long traceId = -1;
		try {
			traceId = Long.parseLong(traceIdString);
		} catch (Exception e) {
		}

		String parentSpanIdString = req.getHeader("X─B3─ParentSpanId");
		long parentSpanId = -1;
		try {
			parentSpanId = Long.parseLong(parentSpanIdString);
		} catch (Exception e) {
		}

		String spanIdString = req.getHeader("X─B3─SpanId");
		long spanId = -1;
		try {
			spanId = Long.parseLong(spanIdString);
		} catch (Exception e) {
		}

		brave.Span span = null;

		if (traceId == -1) {
			// new trace
			span = tracer.newTrace().name(getSvcName()).kind(Kind.SERVER);
		} else {
			// existing trace
			span = tracer
					.joinSpan(TraceContext.newBuilder().traceId(traceId).spanId(spanId).parentId(parentSpanId).build());
		}

		return span;
	}

	public CompletionStage<Result> index() {
		String svcName = getSvcName();
		final Logger.ALogger log = play.Logger.of("application.Controller-" + svcName);
		log.info("Path: {}", ctx().request().path());

		CompletionStage<Result> stage = CompletableFuture.supplyAsync(() -> {
			log.info("[{}] Received request", svcName);
			
			brave.Span mySpan = extractB3(ctx().request());
			mySpan.start();

			Optional<String> nextSvcName = getNextSvc();

			log.info("Next service to be: {}", nextSvcName);
			
			if (nextSvcName.isPresent()) {
				String nextServiceName = nextSvcName.get();
				log.info("Next service: {}", nextSvcName);
				
				brave.Span newSpan = tracer.newChild(mySpan.context()).name(nextServiceName).kind(Kind.CLIENT);
				newSpan.remoteEndpoint(Endpoint.builder().serviceName(nextServiceName).build());
				newSpan.start();
				
				String url = "http://localhost:9000/" + nextServiceName;
				WSRequest req = ws.url(url);
				
				injectB3(req, newSpan.context());
				
				log.info("Calling new service: {}", nextServiceName);
				CompletionStage<WSResponse> responsePromise = req.get();
				responsePromise.whenComplete((r, t) -> {
					log.info("Service [{}] -> Service [{}], response: {}", svcName, nextServiceName, r.getBody());
					newSpan.finish();
				});
			}
			
			log.info("[{}] do some real work", svcName);
			Reply reply = doWork(svcName);
			JsonNode json = Json.toJson(reply);

			log.info("[{}] Respond request", svcName);
			mySpan.finish();
			return ok(Json.stringify(json));
		}, ec.current()).exceptionally(throwable -> {
			return badRequest("");
		});

		return stage;
	}

	private Reply doWork(String svcName) {
		long nap = RandomUtils.nextLong(10, 3000);
		try {
			Thread.sleep(nap);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		return new Reply(svcName, nap);		
	}

}

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