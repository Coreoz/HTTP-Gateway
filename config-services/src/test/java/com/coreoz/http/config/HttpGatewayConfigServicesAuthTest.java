package com.coreoz.http.config;


import com.coreoz.http.services.auth.HttpGatewayRemoteServicesAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceBasicAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceKeyAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.List;

public class HttpGatewayConfigServicesAuthTest {
    @Test
    public void readRemoteServiceAuthentication__check_that_authentication_is_correctly_created() {
        @Nullable HttpGatewayUpstreamAuthenticator serviceAuthentication = HttpGatewayConfigServicesAuth.readRemoteServiceAuthentication(
            ConfigFactory.load("test-auth.conf").getConfigList("remote-services").get(0),
            HttpGatewayConfigServicesAuth.indexAuthenticationConfigs(List.of(HttpGatewayConfigServicesAuth.BASIC_AUTH))
        );

        Assertions.assertThat(serviceAuthentication).isNotNull();
        Assertions.assertThat(((HttpGatewayRemoteServiceBasicAuthenticator) serviceAuthentication).getAuthorizationBasic()).isEqualTo("Basic dGVzdC1hdXRoOmF1dGgtcGFzc3dvcmQ=");
    }

    @Test
    public void readConfig__verify_that_all_authentication_types_are_correctly_recognized() {
        Config config = ConfigFactory.load("test-auth.conf");
        HttpGatewayRemoteServicesAuthenticator servicesAuthenticator = HttpGatewayConfigServicesAuth.readConfig(config);

        Assertions.assertThat(servicesAuthenticator.forRoute("test1", "")).isInstanceOf(HttpGatewayRemoteServiceBasicAuthenticator.class);
        Assertions.assertThat(servicesAuthenticator.forRoute("test2", "")).isInstanceOf(HttpGatewayRemoteServiceKeyAuthenticator.class);
    }
}
