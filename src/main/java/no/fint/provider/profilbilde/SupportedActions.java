package no.fint.provider.profilbilde;

import no.fint.model.profilbilde.ProfilbildeActions;
import no.fint.provider.adapter.AbstractSupportedActions;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class SupportedActions extends AbstractSupportedActions {

    @PostConstruct
    public void addSupportedActions() {
        addAll(ProfilbildeActions.class);
    }

}
