package no.fint.provider.adapter.sse;

import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.HeaderConstants;
import no.fint.provider.adapter.FintAdapterProps;
import no.fint.provider.profilbilde.service.EventHandlerService;
import no.fint.sse.FintSse;
import no.fint.sse.FintSseConfig;
import no.fint.sse.oauth.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Handles the client connections to the provider SSE endpoint
 */
@Slf4j
@Component
public class SseInitializer {

    @Getter
    private List<FintSse> sseClients = new ArrayList<>();

    @Autowired
    private FintAdapterProps props;

    @Autowired
    private EventHandlerService eventHandlerService;

    @Autowired(required = false)
    private TokenService tokenService;

    private final AtomicInteger count = new AtomicInteger();

    private volatile long timestamp;

    @PostConstruct
    @Synchronized
    public void init() {
        FintSseConfig config = FintSseConfig.withOrgIds(props.getOrganizations());
        Arrays.asList(props.getOrganizations()).forEach(orgId -> {
            FintSse fintSse = new FintSse(props.getSseEndpoint(), tokenService, config);
            FintEventListener fintEventListener = new FintEventListener(eventHandlerService);
            fintSse.connect(fintEventListener, Map.of(HeaderConstants.ORG_ID, orgId));
            sseClients.add(fintSse);
        });
    }

    @Scheduled(initialDelay = 20000L, fixedDelay = 5000L)
    public void checkSseConnection() {
        timestamp = System.currentTimeMillis();
        count.incrementAndGet();
        if (sseClients.isEmpty()) {
            log.warn("Reinitializing SSE connections!");
            init();
            return;
        }
        try {
            Map<String, Long> expired = sseClients
                    .stream()
                    .collect(Collectors.toMap(FintSse::getSseUrl, FintSse::getAge, Math::max))
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue() > props.getExpiration())
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            if (!expired.isEmpty()) {
                log.warn("Stale connections detected: {}", expired);
                cleanup();
                init();
            } else {
                for (FintSse sseClient : sseClients) {
                    if (!sseClient.verifyConnection()) {
                        log.info("Reconnecting SSE client {}", sseClient.getSseUrl());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error during SSE connection check!", e);
        }
    }

    @PreDestroy
    @Synchronized
    public void cleanup() {
        List<FintSse> oldClients = sseClients;
        sseClients = new ArrayList<>();
        for (FintSse sseClient : oldClients) {
            sseClient.close();
        }
    }

    public int getCount() {
        return count.get();
    }

    public String getTimestamp() {
        return String.format("%TF %<TT.%<TL", timestamp);
    }
}
