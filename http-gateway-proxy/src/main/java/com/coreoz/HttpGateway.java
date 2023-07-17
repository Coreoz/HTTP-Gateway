package com.coreoz;

import com.coreoz.conf.HttpGatewayConfiguration;
import com.coreoz.play.StreamingBodyParser;
import play.Mode;
import play.routing.RoutingDsl;
import play.server.Server;

// TODO rename everywhere upstream and downstream
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
                builtInComponents -> configuration.getRouterConfiguration().configureRoutes(new RoutingDsl(
                    new StreamingBodyParser(builtInComponents.scalaBodyParsers().materializer())
                ))
            ));
    }

    public void stop() {
        server.stop();
    }
}
