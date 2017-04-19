package controllers;

import akka.actor.ActorSystem;

import net.jodah.failsafe.FailsafeException;
import play.Logger;
import play.libs.concurrent.Futures;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static play.mvc.Http.Status.GATEWAY_TIMEOUT;
import static play.mvc.Http.Status.NOT_ACCEPTABLE;
import static play.mvc.Http.Status.SERVICE_UNAVAILABLE;

public class PostAction extends play.mvc.Action.Simple {
    private final Logger.ALogger logger = play.Logger.of("application.PostAction");


    private final HttpExecutionContext ec;

    @Singleton
    @Inject
    public PostAction(HttpExecutionContext ec, ActorSystem actorSystem) {
        this.ec = ec;
   }

    public CompletionStage<Result> call(Http.Context ctx) {
        if (logger.isTraceEnabled()) {
            logger.trace("call: ctx = " + ctx);
        }
		if (ctx.request().accepts("application/json")) {
			return timeout(doCall(ctx), 1L, TimeUnit.SECONDS);
		} else {
            return CompletableFuture.completedFuture(
                    status(NOT_ACCEPTABLE, "We only accept application/json")
            );
        }
    }

    private CompletionStage<Result> doCall(Http.Context ctx) {
        return delegate.call(ctx).handleAsync((result, e) -> {
            if (e != null) {
                if (e instanceof CompletionException) {
                    Throwable completionException = e.getCause();
                    if (completionException instanceof FailsafeException) {
                        logger.error("Circuit breaker is open!", completionException);
                        return Results.status(SERVICE_UNAVAILABLE, "Service has timed out");
                    } else {
                        logger.error("Direct exception " + e.getMessage(), e);
                        return internalServerError();
                    }
                } else {
                    logger.error("Unknown exception " + e.getMessage(), e);
                    return internalServerError();
                }
            } else {
                return result;
            }
        }, ec.current());
    }

    private CompletionStage<Result> timeout(final CompletionStage<Result> stage, final long delay, final TimeUnit unit) {
        final CompletionStage<Result> timeoutFuture = Futures.timeout(delay, unit).handle((v, e) -> {
            return Results.status(GATEWAY_TIMEOUT, views.html.index.render());
        });
        return stage.applyToEither(timeoutFuture, Function.identity());
    }

}