package no.fint.provider.profilbilde.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.event.model.Status;
import no.fint.event.model.health.Health;
import no.fint.event.model.health.HealthStatus;
import no.fint.model.profilbilde.ProfilbildeActions;
import no.fint.model.resource.FintLinks;
import no.fint.provider.adapter.event.EventResponseService;
import no.fint.provider.adapter.event.EventStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.EnumMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The EventHandlerService receives the <code>event</code> from SSE endpoint (provider) in the {@link #handleEvent(Event)} method.
 */
@Slf4j
@Service
public class EventHandlerService {

    @Autowired
    private EventResponseService eventResponseService;

    @Autowired
    private EventStatusService eventStatusService;

    @Autowired
    private Collection<Handler> handlers;

    private EnumMap<ProfilbildeActions, Handler> actionsHandlerMap;

    /**
     * <p>
     * HandleEvent is responsible of handling the <code>event</code>. This is what should be done:
     * </p>
     * <ol>
     * <li>Verify that the adapter can handle the <code>event</code>. This is done in the {@link EventStatusService#verifyEvent(Event)} method</li>
     * <li>Call the code to handle the action</li>
     * <li>Posting back the handled <code>event</code>. This done in the {@link EventResponseService#postResponse(Event)} method</li>
     * </ol>
     * <p>
     * This is where you implement your code for handling the <code>event</code>. It is typically done by making a onEvent method:
     * </p>
     * <pre>
     *     {@code
     *     images void onGetAllDogs(Event<FintResource> dogAllEvent) {
     *
     *         // Call a service to get all dogs from the application and add the result to the event data
     *         // dogAllEvent.addData(dogResource);
     *
     *     }
     *     }
     * </pre>
     *
     * @param event The <code>event</code> received from the provider
     */
    public void handleEvent(Event event) {
        if (event.isHealthCheck()) {
            postHealthCheckResponse(event);
        } else {
            if (event != null && eventStatusService.verifyEvent(event).getStatus() == Status.ADAPTER_ACCEPTED) {
                Event<FintLinks> responseEvent = new Event<>(event);
                try {
                    ProfilbildeActions action = ProfilbildeActions.valueOf(event.getAction());

                    actionsHandlerMap.getOrDefault(action, e -> {
                        log.warn("No handler found for {}", action);
                        e.setStatus(Status.ADAPTER_REJECTED);
                        e.setResponseStatus(ResponseStatus.REJECTED);
                        e.setMessage("Unsupported action");
                        e.setStatusCode("UNSUPPORTED_ACTION");
                    }).accept(responseEvent);
                } catch (Exception e) {
                    log.info("Unable to handle event {}", event, e);
                    responseEvent.setResponseStatus(ResponseStatus.ERROR);
                    responseEvent.setStatusCode("EXCEPTION");
                    try (StringWriter s = new StringWriter(); PrintWriter w = new PrintWriter(s)) {
                        e.printStackTrace(w);
                        responseEvent.setMessage(s.toString());
                    } catch (IOException e1) {
                        log.error("Error", e1);
                    }
                }
                eventResponseService.postResponse(responseEvent);
            }
        }
    }

    /**
     * Checks if the application is healthy and updates the event object.
     *
     * @param event The event object
     */
    public void postHealthCheckResponse(Event event) {
        Event<Health> healthCheckEvent = new Event<>(event);
        healthCheckEvent.setStatus(Status.TEMP_UPSTREAM_QUEUE);

        if (healthCheck()) {
            healthCheckEvent.addData(new Health("adapter", HealthStatus.APPLICATION_HEALTHY));
        } else {
            healthCheckEvent.addData(new Health("adapter", HealthStatus.APPLICATION_UNHEALTHY));
            healthCheckEvent.setMessage("The adapter is unable to communicate with the application.");
        }

        eventResponseService.postResponse(healthCheckEvent);
    }

    /**
     * This is where we implement the health check code
     *
     * @return {@code true} if health is ok, else {@code false}
     */
    private boolean healthCheck() {
        /*
         * Check application connectivity etc.
         */
        return ThreadLocalRandom.current().nextBoolean();
    }

    /**
     * Data used in examples
     */
    @PostConstruct
    void init() {
        actionsHandlerMap = new EnumMap<ProfilbildeActions, Handler>(ProfilbildeActions.class);
        handlers.forEach(h -> h.actions().forEach(a -> actionsHandlerMap.put(a, h)));
        log.info("Registered {} handlers.", actionsHandlerMap.size());
    }
}
