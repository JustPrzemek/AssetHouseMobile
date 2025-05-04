package pl.mtu.assethouse.api.service;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pl.mtu.assethouse.api.ApiClient;
import pl.mtu.assethouse.utils.SharedPrefsManager;

public class InventoryService {
    private static final String TAG = "InventoryService";

    private final ApiClient apiClient;
    private final SharedPrefsManager prefsManager;
    private final ExecutorService executorService;

    public InventoryService(Context context) {
        this.apiClient = new ApiClient(context);
        this.prefsManager = new SharedPrefsManager(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public CompletableFuture<Boolean> loadInventoryData() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        executorService.execute(() -> {
            try {
                Map<String, String> params = new HashMap<>();
                String response = apiClient.get("/api/mobile/inventoryInfo", params);

                JSONObject jsonResponse = new JSONObject(response);
                int inventoryId = jsonResponse.getInt("inventoryId");

                prefsManager.saveInventoryData(inventoryId, response);

                Log.d(TAG, "Saved inventory data to SharedPreferences, inventoryId: " + inventoryId);
                future.complete(true);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error while retrieving inventory data", e);
                future.complete(false);
            }
        });

        return future;
    }

    public JSONObject getInventoryData() {
        String inventoryData = prefsManager.getInventoryData();
        if (inventoryData == null) {
            return null;
        }

        try {
            return new JSONObject(inventoryData);
        } catch (JSONException e) {
            Log.e(TAG, "Error while retrieving inventory data", e);
            return null;
        }
    }
    public CompletableFuture<Integer> getCurrentInventoryIdFromServer() {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        executorService.execute(() -> {
            try {
                Map<String, String> params = new HashMap<>();
                String response = apiClient.get("/api/mobile/inventoryNumber", params);

                JSONObject jsonResponse = new JSONObject(response);
                int inventoryId = jsonResponse.getInt("inventoryId");
                future.complete(inventoryId);
            } catch (Exception e) {
                Log.e(TAG, "Error getting current inventory ID", e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public int getInventoryId() {
        return prefsManager.getInventoryId();
    }

    public boolean hasInventoryData() {
        return prefsManager.hasInventoryData();
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
