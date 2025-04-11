package rfid.assethouse.api.service;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import rfid.assethouse.api.ApiClient;
import rfid.assethouse.models.Area;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AreaService {
    private final ApiClient apiClient;

    public AreaService(Context context) {
        this.apiClient = new ApiClient(context);
    }

    public Pair<List<Area>, Integer> getAreas(String location, int page, int size) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("location", location);
        params.put("page", String.valueOf(page));
        params.put("size", String.valueOf(size));
        params.put("sort", "location");

        String response = apiClient.get("/api/dashboard/listLocationsWithAssets", params);
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray content = jsonResponse.getJSONArray("content");

        List<Area> areas = new ArrayList<>();
        for (int i = 0; i < content.length(); i++) {
            JSONObject areaJson = content.getJSONObject(i);
            areas.add(new Area(areaJson));
        }

        JSONObject pagination = jsonResponse.getJSONObject("pagination");
        int totalPages = pagination.getInt("totalPages");

        return new Pair<>(areas, totalPages);
    }
}