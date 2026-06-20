package com.gmailintelligence.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthStateServiceTest {

    @Test
    void validatesAndConsumesStateOnce() {
        OAuthStateService service = new OAuthStateService();
        MockHttpSession session = new MockHttpSession();

        String state = service.createState(session);

        assertThat(service.validateAndConsume(session, state)).isTrue();
        assertThat(service.validateAndConsume(session, state)).isFalse();
    }
}
