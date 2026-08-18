package pl.mtu.assethouse.api;

import java.io.IOException;

import lombok.Getter;

/**
 * Thrown by {@link ApiClient} when the server answers with an HTTP error status.
 * <p>
 * Extends {@link IOException} and keeps the original message format
 * ("HTTP error &lt;code&gt;: &lt;body&gt;") so existing callers that catch IOException
 * or scrape the message keep working unchanged.
 */
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
