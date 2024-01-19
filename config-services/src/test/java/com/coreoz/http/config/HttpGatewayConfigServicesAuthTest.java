package com.coreoz.http.config;


import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.coreoz.http.services.auth.HttpGatewayRemoteServiceAuth;
import com.coreoz.http.services.auth.HttpGatewayRemoteServicesAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceBasicAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceKeyAuthenticator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class HttpGatewayConfigServicesAuthTest {
    @Test
    public void createServiceAuthentications__check_that_authentication_is_correctly_created() {
        List<HttpGatewayRemoteServiceAuth> serviceAuthentications = HttpGatewayConfigServicesAuth.createServiceAuthentications(
            List.of(HttpGatewayConfigServicesAuth.BASIC_AUTH),
            Map.of(
                HttpGatewayConfigServicesAuth.BASIC_AUTH.getAuthConfig().getAuthType(),
                List.of(new HttpGatewayAuthBasic("service-id-test", "user-test", "user-password"))
            )
        );

        Assertions.assertThat(serviceAuthentications).hasSize(1);
        Assertions.assertThat(serviceAuthentications.getFirst().getServiceId()).isEqualTo("service-id-test");
        Assertions.assertThat(serviceAuthentications.getFirst().getAuthenticator()).isInstanceOf(HttpGatewayRemoteServiceBasicAuthenticator.class);
        Assertions.assertThat(((HttpGatewayRemoteServiceBasicAuthenticator) serviceAuthentications.getFirst().getAuthenticator()).getAuthorizationBasic()).isEqualTo("Basic dXNlci10ZXN0OnVzZXItcGFzc3dvcmQ=");
    }

    @Test
    public void readConfig__verify_that_all_authentication_types_are_correctly_recognized() {
        Config config = ConfigFactory.load("test-auth.conf");
        HttpGatewayRemoteServicesAuthenticator servicesAuthenticator = HttpGatewayConfigServicesAuth.readConfig(config);

        Assertions.assertThat(servicesAuthenticator.forRoute("test1", "")).isInstanceOf(HttpGatewayRemoteServiceBasicAuthenticator.class);
        Assertions.assertThat(servicesAuthenticator.forRoute("test2", "")).isInstanceOf(HttpGatewayRemoteServiceKeyAuthenticator.class);
    }
}
