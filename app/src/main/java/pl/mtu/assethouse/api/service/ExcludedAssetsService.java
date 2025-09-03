package pl.mtu.assethouse.api.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import pl.mtu.assethouse.api.ApiClient;
import pl.mtu.assethouse.models.ExcludedAssets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ExcludedAssetsService {

    private final ApiClient apiClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    @Getter
    private static List<String> excludedAssetIdCache = null;

    public ExcludedAssetsService(Context context) {
        this.apiClient = new ApiClient(context);
    }

    public interface FetchDataCallback {
        void onSuccess(List<ExcludedAssets> assets);
        void onError(String message);
    }

    public void fetchAllExcludedAssets(FetchDataCallback callback) {
        executorService.execute(() -> {
            try {
                String jsonResponse = apiClient.get("/api/excludedAssets/allExcludedAssets", Collections.emptyMap());

                JSONObject rootObject = new JSONObject(jsonResponse);
                JSONArray assetsArray = rootObject.getJSONArray("excludedAssets");

                List<ExcludedAssets> resultList = new ArrayList<>();
                for (int i = 0; i < assetsArray.length(); i++) {
                    JSONObject assetObject = assetsArray.getJSONObject(i);
                    String assetId = assetObject.getString("assetId");
                    String description = assetObject.getString("description");
                    resultList.add(new ExcludedAssets(assetId, description));
                }

                excludedAssetIdCache = resultList.stream()
                        .map(ExcludedAssets::getAssetId)
                        .collect(Collectors.toList());
                Log.d("ExcludedAssetsService", "Cache updated with " + excludedAssetIdCache.size() + " items.");

                mainThreadHandler.post(() -> callback.onSuccess(resultList));

            } catch (IOException | JSONException e) {
                mainThreadHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}