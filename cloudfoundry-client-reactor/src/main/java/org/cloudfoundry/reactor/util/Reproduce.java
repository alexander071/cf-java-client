package org.cloudfoundry.reactor.util;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.reactivestreams.Publisher;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.AsciiString;
import reactor.core.publisher.Flux;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;

public class Reproduce {

    private static final AsciiString CRLF = new AsciiString("\r\n");
    private static final AsciiString DOUBLE_DASH = new AsciiString("--");

    public static void main(String[] args) throws IOException {
        String response = HttpClient.create()
                                    .wiretap(true)
                                    .post()
                                    .uri("https://postman-echo.com/post")
                                    .send(Reproduce::sendGeneratedContent)
                                    .responseContent()
                                    .aggregate()
                                    .asString()
                                    .block();
        System.out.println(response);
    }

    private static Publisher<Void> sendGeneratedContent(HttpClientRequest request, NettyOutbound outbound) {
        try {
            AsciiString boundary = new AsciiString("xkVpCHkqWbNBJT6_bW8venxnA6mBYmAz");
            AsciiString delimiter = getDelimiter(boundary);
            AsciiString closeDelimiter = getCloseDelimiter(boundary);

            request.header(CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);

            for (Path part : generateParts()) {
                outbound = sendPart(outbound, part, delimiter);
            }
            return outbound.send(Flux.just(Unpooled.wrappedBuffer(closeDelimiter.toByteArray())));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static AsciiString getDelimiter(AsciiString boundary) {
        return CRLF.concat(DOUBLE_DASH)
                   .concat(boundary);
    }

    private static AsciiString getCloseDelimiter(AsciiString boundary) {
        return CRLF.concat(DOUBLE_DASH)
                   .concat(boundary)
                   .concat(DOUBLE_DASH);
    }

    private static List<Path> generateParts() throws IOException {
        return Arrays.asList(generatePart("bar"), generatePart("foo"));
    }

    private static Path generatePart(String content) throws IOException {
        Path part = Files.createTempFile(null, null);
        System.out.println(part);
        try (BufferedWriter writer = Files.newBufferedWriter(part)) {
            writer.write(content);
        }
        return part;
    }

    private static NettyOutbound sendPart(NettyOutbound outbound, Path part, AsciiString delimiter) {
        return outbound.send(Flux.just(Unpooled.wrappedBuffer(delimiter.toByteArray()), Unpooled.wrappedBuffer(CRLF.toByteArray())))
                       .sendFile(part);
    }

}
