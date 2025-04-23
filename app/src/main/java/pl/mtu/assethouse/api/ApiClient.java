package pl.mtu.assethouse.api;

import android.content.Context;
import android.net.Uri;

import pl.mtu.assethouse.utils.SSLHelper;
import pl.mtu.assethouse.utils.SharedPrefsManager;

import java.io.BufferedReader;
import java.io.IOException;
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

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                return "";
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseCode < 400 ? connection.getInputStream() : connection.getErrorStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String handleResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();

        if (responseCode >= 200 && responseCode < 300) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } else {
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream()))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                throw new IOException("HTTP error code: " + responseCode +
                        ", Response: " + errorResponse.toString());
            }
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
