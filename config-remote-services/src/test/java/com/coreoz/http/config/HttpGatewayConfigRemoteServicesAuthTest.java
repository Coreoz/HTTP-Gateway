package com.coreoz.http.config;


import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceAuth;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceBasicAuthenticator;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class HttpGatewayConfigRemoteServicesAuthTest {
    @Test
    public void createServiceAuthentications__check_that_authentication_is_correctly_created() {
        List<HttpGatewayRemoteServiceAuth> serviceAuthentications = HttpGatewayConfigRemoteServicesAuth.createServiceAuthentications(
            List.of(HttpGatewayConfigRemoteServicesAuth.BASIC_AUTH),
            Map.of(
                HttpGatewayConfigRemoteServicesAuth.BASIC_AUTH.getAuthConfig().getAuthType(),
                List.of(new HttpGatewayAuthBasic("service-id-test", "user-test", "user-password"))
            )
        );

        Assertions.assertThat(serviceAuthentications).hasSize(1);
        Assertions.assertThat(serviceAuthentications.get(0).getServiceId()).isEqualTo("service-id-test");
        Assertions.assertThat(serviceAuthentications.get(0).getAuthenticator()).isInstanceOf(HttpGatewayRemoteServiceBasicAuthenticator.class);
        Assertions.assertThat(((HttpGatewayRemoteServiceBasicAuthenticator) serviceAuthentications.get(0).getAuthenticator()).getAuthorizationBasic()).isEqualTo("Basic dXNlci10ZXN0OnVzZXItcGFzc3dvcmQ=");
    }
}
