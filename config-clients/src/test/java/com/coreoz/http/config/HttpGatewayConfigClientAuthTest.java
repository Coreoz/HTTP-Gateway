package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayClientApiKeyAuthenticator;
import com.coreoz.http.access.control.auth.HttpGatewayClientAuthenticator;
import com.coreoz.http.access.control.auth.HttpGatewayClientBasicAuthenticator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class HttpGatewayConfigClientAuthTest {
    @Test
    public void verify_that_all_authentication_methods_are_currently_recognized_and_that_corresponding_authenticators_are_created() {
        Config config = ConfigFactory.load("application.conf");
        List<HttpGatewayClientAuthenticator> authenticators = HttpGatewayConfigClientAuth.readAuthenticators(config.getConfigList("clients"), HttpGatewayConfigClientAuth.supportedAuthConfigs());

        Assertions.assertThat(authenticators)
            .hasAtLeastOneElementOfType(HttpGatewayClientApiKeyAuthenticator.class)
            .hasAtLeastOneElementOfType(HttpGatewayClientBasicAuthenticator.class);
    }
}
