package com.zebra.rfid.assethouse.services;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchService {

    private static final String TAG = "ApiFetcher";

    public static JSONObject areasFetch(String urlPath, int page, String search) {
        JSONObject result = null;
        try {
            String requestUrl = urlPath + "/api/dashboard/listLocationsWithAssets?location=" + search + "&page=" + page + "&size=5&sort=location";
            result = fetchData(requestUrl);
        } catch (Exception e) {
            Log.e(TAG, "areasFetch error:", e);
        }
        return result;
    }

    public static JSONObject tableFetch(String urlPath, String room) {
        JSONObject result = null;
        try {
            Log.d(TAG, "Fetching table for room: " + room);
            String requestUrl = urlPath + "/api/locations/locationsToRoomsPage?searchLocation=" + room + "&page=0&size=1";
            result = fetchData(requestUrl);
        } catch (Exception e) {
            Log.e(TAG, "tableFetch error:", e);
        }
        return result;
    }

    public static JSONObject insideLocationFetch(String urlPath, String location) {
        JSONObject result = null;
        try {
            String requestUrl = urlPath + "/api/locations/insideLocation?location=" + location + "&page=0&size=1000&sort=inventoryStatus";
            result = fetchData(requestUrl);
        } catch (Exception e) {
            Log.e(TAG, "insideLocationFetch error:", e);
        }
        return result;
    }

    private static JSONObject fetchData(String requestUrl) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        JSONObject jsonResponse = null;
        try {
            URL url = new URL(requestUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("HTTP error code: " + responseCode);
            }

            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }

            jsonResponse = new JSONObject(responseBuilder.toString());
        } catch (Exception e) {
            Log.e(TAG, "fetchData error:", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing reader", e);
            }
        }
        return jsonResponse;
    }
}
