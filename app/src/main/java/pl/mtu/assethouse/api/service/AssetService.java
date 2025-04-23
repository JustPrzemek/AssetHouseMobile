package pl.mtu.assethouse.api.service;

import android.content.Context;

import pl.mtu.assethouse.api.ApiClient;
import pl.mtu.assethouse.models.Asset;
import pl.mtu.assethouse.utils.SharedPrefsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AssetService {
    private final ApiClient apiClient;
    private final SharedPrefsManager prefsManager;

    public AssetService(Context context) {
        this.apiClient = new ApiClient(context);
        this.prefsManager = new SharedPrefsManager(context);
    }

    public List<Asset> getAssetsInLocation(String location) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("location", location);
        params.put("page", "0");
        params.put("size", "1000");
        params.put("sort", "inventoryStatus");

        String response = apiClient.get("/api/locations/insideLocation", params);

        JSONObject jsonResponse = new JSONObject(response);
        JSONArray content = jsonResponse.getJSONArray("content");

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < content.length(); i++) {
            JSONObject assetJson = content.getJSONObject(i);
            assets.add(new Asset(assetJson));
        }
        return assets;
    }

    public boolean updateAssets(String location, List<Asset> assets, List<Asset> scannedAssets) throws Exception {
        Set<String> scannedAssetIds = scannedAssets.stream()
                .map(Asset::getAssetId)
                .collect(Collectors.toSet());

        for (Asset asset : assets) {
            if ("UNSCANNED".equals(asset.getStatus()) && !scannedAssetIds.contains(asset.getAssetId())) {
                asset.setStatus("MISSING");
            }
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("location", location);
        requestBody.put("user", "USER");

        JSONArray assetsArray = new JSONArray();
        addAssetsToArray(assetsArray, assets);
        addScannedAssetsToArray(assetsArray, scannedAssets, assets);

        requestBody.put("assets", assetsArray);

        String response = apiClient.post("/api/mobile/updateOutcome", requestBody.toString());
        return response != null || response.isEmpty();
    }

    private void addAssetsToArray(JSONArray assetsArray, List<Asset> assets) throws JSONException {
        for (Asset asset : assets) {
            JSONObject assetObj = new JSONObject();
            assetObj.put("assetId", asset.getAssetId());
            assetObj.put("status", asset.getStatus());
            assetObj.put("comment", "");
            assetsArray.put(assetObj);
        }
    }

    private void addScannedAssetsToArray(JSONArray assetsArray, List<Asset> scannedAssets, List<Asset> existingAssets) throws JSONException {
        Set<String> existingAssetIds = existingAssets.stream()
                .map(Asset::getAssetId)
                .collect(Collectors.toSet());

        for (Asset scannedAsset : scannedAssets) {
            if (!existingAssetIds.contains(scannedAsset.getAssetId())) {
                JSONObject assetObj = new JSONObject();
                assetObj.put("assetId", scannedAsset.getAssetId());
                assetObj.put("status", scannedAsset.getStatus());
                assetObj.put("comment", "");
                assetsArray.put(assetObj);
            }
        }
    }
}