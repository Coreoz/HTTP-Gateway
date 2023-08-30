package com.coreoz.http.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class HttpGatewayConfigLoader {
    private final Config rootConfig;

    public HttpGatewayConfigLoader(Config rootConfig) {
        this.rootConfig = rootConfig;
    }

    public HttpGatewayConfigLoader() {
        this(ConfigFactory.load());
    }

    public Config getRootConfig() {
        return rootConfig;
    }

    public Config getHttpGatewayConfig() {
        return rootConfig.getConfig("http-gateway");
    }
}
