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

package org.cloudfoundry.reactor.doppler;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClientResponse;

final class MultipartCodec {

    private static final Pattern BOUNDARY_PATTERN = Pattern.compile("multipart/.+; boundary=(.*)");

    private static final int MAX_PAYLOAD_SIZE = 1024 * 1024;

    private MultipartCodec() {
    }

    static Flux<InputStream> decode(ByteBufFlux body) {
        return body.asInputStream()
            .skip(1);
    }

    static DelimiterBasedFrameDecoder createDecoder(HttpClientResponse response) {
        String boundary = extractMultipartBoundary(response);

        return new DelimiterBasedFrameDecoder(MAX_PAYLOAD_SIZE,
            Unpooled.copiedBuffer(String.format("--%s\r\n\r\n", boundary), Charset.defaultCharset()),
            Unpooled.copiedBuffer(String.format("\r\n--%s\r\n\r\n", boundary), Charset.defaultCharset()),
            Unpooled.copiedBuffer(String.format("\r\n--%s--", boundary), Charset.defaultCharset()),
            Unpooled.copiedBuffer(String.format("\r\n--%s--\r\n", boundary), Charset.defaultCharset()));
    }

    private static String extractMultipartBoundary(HttpClientResponse response) {
        String contentType = response.responseHeaders()
            .get(HttpHeaderNames.CONTENT_TYPE);
        Matcher matcher = BOUNDARY_PATTERN.matcher(contentType);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            throw new IllegalStateException(String.format("Content-Type %s does not contain a valid multipart boundary", contentType));
        }
    }

}
