/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.reactor.util;

import static io.netty.handler.codec.http.HttpStatusClass.CLIENT_ERROR;
import static io.netty.handler.codec.http.HttpStatusClass.SERVER_ERROR;

import java.util.Map;
import java.util.function.Function;

import org.cloudfoundry.UnknownCloudFoundryException;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v3.ClientV3Exception;
import org.cloudfoundry.client.v3.Errors;
import org.cloudfoundry.reactor.HttpClientResponseWithBody;
import org.cloudfoundry.uaa.UaaException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.codec.http.HttpStatusClass;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientResponse;

public final class ErrorPayloadMappers {

    public static ErrorPayloadMapper fallback() {
        return inbound -> inbound.flatMap(responseWithBody -> {
            HttpClientResponse response = responseWithBody.getResponse();
            if (isError(response)) {
                return responseWithBody.getBody()
                    .aggregate()
                    .asString()
                    .flatMap(payload -> Mono.error(new UnknownCloudFoundryException(response.status()
                        .code(),
                        payload)));
            }
            return Mono.just(responseWithBody);
        });
    }

    @SuppressWarnings("unchecked")
    public static ErrorPayloadMapper clientV2(ObjectMapper objectMapper) {
        return inbound -> inbound.flatMap(mapToError((statusCode, payload) -> {
            Map<String, Object> map = objectMapper.readValue(payload, Map.class);
            Integer code = (Integer) map.get("code");
            String description = (String) map.get("description");
            String errorCode = (String) map.get("error_code");

            return new ClientV2Exception(statusCode, code, description, errorCode);
        }));
    }

    public static ErrorPayloadMapper clientV3(ObjectMapper objectMapper) {
        return inbound -> inbound.flatMap(mapToError((statusCode, payload) -> {
            Errors errors = objectMapper.readValue(payload, Errors.class);
            return new ClientV3Exception(statusCode, errors.getErrors());
        }));
    }

    @SuppressWarnings("unchecked")
    public static ErrorPayloadMapper uaa(ObjectMapper objectMapper) {
        return inbound -> inbound.flatMap(mapToError((statusCode, payload) -> {
            Map<String, Object> map = objectMapper.readValue(payload, Map.class);
            String error = (String) map.get("error");
            String errorDescription = (String) map.get("error_description");

            return new UaaException(statusCode, error, errorDescription);
        }));
    }

    private static boolean isError(HttpClientResponse response) {
        HttpStatusClass statusClass = response.status()
            .codeClass();
        return statusClass == CLIENT_ERROR || statusClass == SERVER_ERROR;
    }

    private static Function<HttpClientResponseWithBody, Mono<HttpClientResponseWithBody>>
    mapToError(ExceptionGenerator exceptionGenerator) {
        return response -> {
            if (!isError(response.getResponse())) {
                return Mono.just(response);
            }

            return response.getBody()
                .aggregate()
                .asString()
                .switchIfEmpty(Mono.error(new UnknownCloudFoundryException(response.getResponse()
                    .status()
                    .code())))
                .flatMap(payload -> {
                    try {
                        return Mono.error(exceptionGenerator.apply(response.getResponse()
                                .status()
                                .code(),
                            payload));
                    } catch (Exception e) {
                        return Mono.error(new UnknownCloudFoundryException(response.getResponse()
                            .status()
                            .code(),
                            payload));
                    }
                });
        };
    }

    @FunctionalInterface
    private interface ExceptionGenerator {

        RuntimeException apply(Integer statusCode, String payload) throws Exception;

    }

}
