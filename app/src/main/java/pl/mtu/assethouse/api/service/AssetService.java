package pl.mtu.assethouse.api.service;

import android.content.Context;

import lombok.Getter;
import pl.mtu.assethouse.api.ApiClient;
import pl.mtu.assethouse.models.Asset;
import pl.mtu.assethouse.utils.DeviceIdProvider;
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
    private static final String INSIDE_LOCATION_ENDPOINT = "/api/mobile/insideLocation";

    // The backend caps page size at 1000, and every request records a scanner-entry event.
    private static final int REQUESTED_PAGE_SIZE = 1000;

    private final ApiClient apiClient;
    private final SharedPrefsManager prefsManager;
    private final Context context;
    @Getter
    private int currentInventoryId = -1;
    private List<String> predefinedComments = new ArrayList<>();


    public AssetService(Context context) {
        this.apiClient = new ApiClient(context);
        this.prefsManager = new SharedPrefsManager(context);
        this.context = context.getApplicationContext();
    }

    public List<Asset> getAssetsInLocation(String location, String sort) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("location", location);
        params.put("page", "0");
        params.put("size", String.valueOf(REQUESTED_PAGE_SIZE));
        params.put("sort", sort);
        params.put("deviceId", DeviceIdProvider.get(context));

        JSONObject jsonResponse = new JSONObject(apiClient.get(INSIDE_LOCATION_ENDPOINT, params));
        this.currentInventoryId = jsonResponse.getInt("inventoryId");

        List<Asset> assets = parseAssets(jsonResponse);
        int totalElements = jsonResponse.getJSONObject("pagination").getInt("totalElements");

        // Room larger than the server cap: pull the rest, or assets go missing from the stocktake.
        for (int page = 1; assets.size() < totalElements; page++) {
            params.put("page", String.valueOf(page));
            List<Asset> nextPage = parseAssets(new JSONObject(apiClient.get(INSIDE_LOCATION_ENDPOINT, params)));
            if (nextPage.isEmpty()) {
                break;
            }
            assets.addAll(nextPage);
        }

        return assets;
    }

    private List<Asset> parseAssets(JSONObject jsonResponse) throws JSONException {
        JSONArray content = jsonResponse.getJSONArray("content");
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < content.length(); i++) {
            assets.add(new Asset(content.getJSONObject(i)));
        }
        return assets;
    }

    public boolean updateAssets(String location, List<Asset> assets, List<Asset> scannedAssets) throws Exception {
        Set<String> scannedAssetIds = scannedAssets.stream()
                .map(Asset::getAssetId)
                .collect(Collectors.toSet());

        List<String> excluded = ExcludedAssetsService.getExcludedAssetIdCache();

        for (Asset asset : assets) {
            if (excluded != null && excluded.contains(asset.getAssetId())) {
                asset.setStatus("OK");
            } else if ("UNSCANNED".equals(asset.getStatus()) && !scannedAssetIds.contains(asset.getAssetId())) {
                asset.setStatus("MISSING");
            }
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("location", location);
        requestBody.put("user", "MOBILE");
        requestBody.put("inventoryId", currentInventoryId);
        requestBody.put("deviceId", DeviceIdProvider.get(context));

        JSONArray assetsArray = new JSONArray();
        addAssetsToArray(assetsArray, assets);
        addScannedAssetsToArray(assetsArray, scannedAssets, assets);

        requestBody.put("assets", assetsArray);

        String response = apiClient.post("/api/mobile/updateOutcome", requestBody.toString());
        if (response == null || response.isEmpty()) {
            return true;
        }

        JSONObject responseJson = new JSONObject(response);
        if (responseJson.has("statusCode") && responseJson.getInt("statusCode") >= 400) {
            return false;
        }

        return true;
    }

    private void addAssetsToArray(JSONArray assetsArray, List<Asset> assets) throws JSONException {
        for (Asset asset : assets) {
            JSONObject assetObj = new JSONObject();
            assetObj.put("assetId", asset.getAssetId());
            assetObj.put("status", asset.getStatus());
            assetObj.put("comment", asset.getComment() != null ? asset.getComment() : "");
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
                assetObj.put("comment", scannedAsset.getComment() != null ? scannedAsset.getComment() : "");
                assetsArray.put(assetObj);
            }
        }
    }

    public List<String> getPredefinedComments() throws Exception {
        if (predefinedComments.isEmpty()) {
            String response = apiClient.getPredefinedComments();
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray commentsArray = jsonResponse.getJSONArray("comments");

            predefinedComments.clear();
            for (int i = 0; i < commentsArray.length(); i++) {
                JSONObject commentObj = commentsArray.getJSONObject(i);
                predefinedComments.add(commentObj.getString("comment"));
            }
        }
        return new ArrayList<>(predefinedComments);
    }
}