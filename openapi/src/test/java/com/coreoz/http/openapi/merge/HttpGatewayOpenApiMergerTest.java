package com.coreoz.http.openapi.merge;

import com.coreoz.http.routes.HttpRoutes;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.junit.Assert.*;

public class HttpGatewayOpenApiMergerTest {
    @Test
    public void makeDownstreamPathUsingOperationPathParamNames() {
        String operationPath = "/pets/{petId}";
        String upstreamPath = "/pets/{id}";
        String downstreamPath = "/custom-path/other/{id}/strange-path-segment";
        Assertions.assertThat(HttpGatewayOpenApiMerger.makeDownstreamPathUsingOperationPathParamNames(
            HttpRoutes.parsePath(operationPath),
            HttpRoutes.parsePath(upstreamPath),
            downstreamPath
        )).isEqualTo("/custom-path/other/{petId}/strange-path-segment");
    }
}
