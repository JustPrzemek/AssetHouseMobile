package pl.mtu.assethouse.api;

import java.io.IOException;

import lombok.Getter;

// Keeps the original "HTTP error <code>: <body>" message so callers that scrape it still work.
@Getter
public class ApiException extends IOException {
    private final int statusCode;
    private final String body;

    public ApiException(int statusCode, String body) {
        super("HTTP error " + statusCode + ": " + body);
        this.statusCode = statusCode;
        this.body = body;
    }
}
