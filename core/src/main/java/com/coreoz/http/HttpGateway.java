package com.coreoz.http;

import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRoutingDsl;
import com.coreoz.http.play.StreamingBodyParser;
import play.Mode;
import play.routing.RoutingDsl;
import play.server.Server;

public class HttpGateway {

    private final Server server;

    public HttpGateway(Server server) {
        this.server = server;
    }

    public static HttpGateway start(HttpGatewayConfiguration configuration) {
        return new HttpGateway(Server
            .forRouter(
                Mode.PROD,
                configuration.getHttpPort(),
                builtInComponents -> configuration.getRoutingDsl().configure(new HttpGatewayRoutingDsl(new RoutingDsl(
                    new StreamingBodyParser(builtInComponents.scalaBodyParsers().materializer())
                ))).build()
            ));
    }

    public void stop() {
        server.stop();
    }
}
