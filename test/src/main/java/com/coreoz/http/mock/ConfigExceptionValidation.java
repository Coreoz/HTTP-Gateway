package com.coreoz.http.mock;

import com.coreoz.http.validation.HttpGatewayConfigException;
import org.assertj.core.api.Assertions;

public class ConfigExceptionValidation {
    public static void validateConfigException(Runnable testProcess, String partOfExceptionMessage) {
        try {
            testProcess.run();
            Assertions.fail("HttpGatewayConfigException has not been raised");
        } catch (HttpGatewayConfigException exception) {
            Assertions.assertThat(exception).hasMessageContaining(partOfExceptionMessage);
        }
    }
}
