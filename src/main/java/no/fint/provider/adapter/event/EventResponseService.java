package no.fint.provider.adapter.event;

import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.event.model.HeaderConstants;
import no.fint.provider.adapter.FintAdapterProps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Handles responses back to the provider status endpoint.
 */
@Slf4j
@Service
public class EventResponseService {

    @Autowired
    private FintAdapterProps props;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Method for posting back the response to the provider.
     *
     * @param event Event to post back
     */
    public void postResponse(Event event) {
        log.debug("Response: {}", event);
        HttpHeaders headers = new HttpHeaders();
        headers.put(HeaderConstants.ORG_ID, List.of(event.getOrgId()));
        ResponseEntity<Void> response = restTemplate.exchange(props.getResponseEndpoint(), HttpMethod.POST, new HttpEntity<>(event, headers), Void.class);
        log.info("Provider POST response: {}", response.getStatusCode());
    }
}
