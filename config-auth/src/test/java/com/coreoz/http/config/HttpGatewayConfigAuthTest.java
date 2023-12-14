package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthApiKey;
import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.coreoz.http.access.control.auth.HttpGatewayAuthObject;
import com.coreoz.http.exception.HttpGatewayValidationException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class HttpGatewayConfigAuthTest {
    private static final Config config = ConfigFactory.load("test.conf");

    @Test
    public void readAuth__verify_auth_object_correctly_read() {
        Map<String, List<? extends HttpGatewayAuthObject>> objectsAuth = HttpGatewayConfigAuth.readAuth(
            "id",
            config.getConfigList("testok"),
            List.of(HttpGatewayConfigAuth.KEY_AUTH, HttpGatewayConfigAuth.BASIC_AUTH)
        );
        Assertions.assertThat(objectsAuth)
            .isNotNull()
            .isNotEmpty()
            .hasSize(2);
        HttpGatewayAuthObject basicAuth = objectsAuth.get(HttpGatewayConfigAuth.BASIC_AUTH.getAuthType()).get(0);
        Assertions.assertThat(basicAuth)
            .isInstanceOf(HttpGatewayAuthBasic.class)
            .isEqualTo(new HttpGatewayAuthBasic("1", "abcd", "azerty"));
        HttpGatewayAuthObject keyAuth = objectsAuth.get(HttpGatewayConfigAuth.KEY_AUTH.getAuthType()).get(0);
        Assertions.assertThat(keyAuth)
            .isInstanceOf(HttpGatewayAuthApiKey.class)
            .isEqualTo(new HttpGatewayAuthApiKey("2", "apikey"));
    }

    @Test(expected = HttpGatewayValidationException.class)
    public void readAuth__wrong_auth_key_must_raise_a_gateway_config_exception() {
        HttpGatewayConfigAuth.readAuth(
            "id",
            config.getConfigList("test-wrong-auth-type"),
            List.of()
        );
    }

    @Test(expected = ConfigException.class)
    public void readAuth__missing_auth_value_must_raise_a_config_exception() {
        HttpGatewayConfigAuth.readAuth(
            "id",
            config.getConfigList("test-missing-auth-value"),
            List.of(HttpGatewayConfigAuth.KEY_AUTH)
        );
    }
}
