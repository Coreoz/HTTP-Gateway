package com.coreoz.http.mock;

import com.coreoz.http.exception.HttpGatewayValidationException;
import org.assertj.core.api.Assertions;

public class ConfigExceptionValidation {
    public static void validateConfigException(Runnable testProcess, String partOfExceptionMessage) {
        try {
            testProcess.run();
            Assertions.fail("HttpGatewayException has not been raised");
        } catch (HttpGatewayValidationException exception) {
            Assertions.assertThat(exception).hasMessageContaining(partOfExceptionMessage);
        }
    }
}
