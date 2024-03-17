package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthApiKey;
import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.coreoz.http.exception.HttpGatewayValidationException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Map;

public class HttpGatewayConfigAuthTest {
    private static final Config config = ConfigFactory.load("test.conf");
    private static final Map<String, HttpGatewayConfigAuth.HttpGatewayAuthReader<?>> authIndex = Map.of(
        HttpGatewayConfigAuth.KEY_AUTH.getAuthType(), HttpGatewayConfigAuth.KEY_AUTH.getAuthReader(),
        HttpGatewayConfigAuth.BASIC_AUTH.getAuthType(), HttpGatewayConfigAuth.BASIC_AUTH.getAuthReader()
    );

    @Test
    public void readAuthentication__verify_auth_object_correctly_read() {
        HttpGatewayConfigAuth.HttpGatewayAuth<?> objectAuthBasic = HttpGatewayConfigAuth.readAuthentication(
            config.getConfigList("testok").get(0),
            authIndex::get
        );
        HttpGatewayConfigAuth.HttpGatewayAuth<?> objectAuthKey = HttpGatewayConfigAuth.readAuthentication(
            config.getConfigList("testok").get(1),
            authIndex::get
        );

        Assertions.assertThat(objectAuthBasic).isNotNull();
        Assertions.assertThat(objectAuthBasic.getAuthType()).isEqualTo(HttpGatewayConfigAuth.BASIC_AUTH.getAuthType());
        Assertions.assertThat(objectAuthBasic.getAuthObject()).isEqualTo(new HttpGatewayAuthBasic("abcd", "azerty"));

        Assertions.assertThat(objectAuthKey).isNotNull();
        Assertions.assertThat(objectAuthKey.getAuthType()).isEqualTo(HttpGatewayConfigAuth.KEY_AUTH.getAuthType());
        Assertions.assertThat(objectAuthKey.getAuthObject()).isEqualTo(new HttpGatewayAuthApiKey("apikey"));
    }

    @Test(expected = HttpGatewayValidationException.class)
    public void readAuthentication__wrong_auth_key_must_raise_a_gateway_config_exception() {
        HttpGatewayConfigAuth.readAuthentication(
            config.getConfigList("test-wrong-auth-type").get(0),
            authIndex::get
        );
    }

    @Test(expected = ConfigException.class)
    public void readAuthentication__missing_auth_value_must_raise_a_config_exception() {
        HttpGatewayConfigAuth.readAuthentication(
            config.getConfigList("test-missing-auth-value").get(0),
            authIndex::get
        );
    }
}
