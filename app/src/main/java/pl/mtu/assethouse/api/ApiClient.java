package pl.mtu.assethouse.api;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import pl.mtu.assethouse.utils.SSLHelper;
import pl.mtu.assethouse.utils.SharedPrefsManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private final String baseUrl;
    private final SharedPrefsManager prefsManager;

    public ApiClient(Context context) {
        prefsManager = new SharedPrefsManager(context);
        this.baseUrl = prefsManager.getBaseUrl();
        SSLHelper.trustAllCertificates();
    }

    public String get(String endpoint, Map<String, String> params) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = buildUrl(endpoint, params);

            if (baseUrl.contains("https")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");

            return handleResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String post(String endpoint, String jsonBody) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl + endpoint);

            if (baseUrl.contains("https")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            return handleResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    private String handleResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();

        InputStream stream = (responseCode >= 200 && responseCode < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();

        if (stream == null) {
            throw new IOException("No response from server, HTTP code: " + responseCode);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            if (responseCode >= 400) {
                Log.e(TAG, "Request failed: HTTP " + responseCode + ", body: " + response);
                throw new IOException("HTTP error " + responseCode + ": " + response.toString());
            }

            return response.toString();
        }
    }


    private URL buildUrl(String endpoint, Map<String, String> params) throws MalformedURLException {
        Uri.Builder uriBuilder = Uri.parse(baseUrl + endpoint).buildUpon();
        for (Map.Entry<String, String> param : params.entrySet()) {
            uriBuilder.appendQueryParameter(param.getKey(), param.getValue());
        }
        return new URL(uriBuilder.build().toString());
    }
}
