package pl.mtu.assethouse.adapters;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import pl.mtu.assethouse.api.service.InventoryService;
import pl.mtu.assethouse.models.AssetInfo;

public class InventoryDataAdapter {
    private static final String TAG = "InventoryDataHelper";

    private final InventoryService inventoryService;

    public InventoryDataAdapter(Context context) {
        this.inventoryService = new InventoryService(context);
    }

    public int getInventoryId() {
        return inventoryService.getInventoryId();
    }

    public List<AssetInfo> getAllAssets() {
        List<AssetInfo> assetList = new ArrayList<>();

        try {
            JSONObject inventoryData = inventoryService.getInventoryData();
            if (inventoryData == null) {
                Log.e(TAG, "No Inventory");
                return assetList;
            }

            JSONObject assets = inventoryData.getJSONObject("assets");
            Iterator<String> keys = assets.keys();

            while (keys.hasNext()) {
                String assetId = keys.next();
                JSONObject assetJson = assets.getJSONObject(assetId);

                String description = assetJson.isNull("description") ? "" : assetJson.getString("description");
                String locationName = assetJson.getString("locationName");
                String systemName = assetJson.getString("systemName");

                AssetInfo assetInfo = new AssetInfo(assetId, description, locationName, systemName);
                assetList.add(assetInfo);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error while retrieving resource data", e);
        }

        return assetList;
    }

    public List<String> getAllLocations() {
        List<String> locations = new ArrayList<>();
        List<AssetInfo> assets = getAllAssets();

        for (AssetInfo asset : assets) {
            String location = asset.getLocationName();
            if (!locations.contains(location)) {
                locations.add(location);
            }
        }

        return locations;
    }

    public List<AssetInfo> getAssetsByLocation(String location) {
        List<AssetInfo> filteredAssets = new ArrayList<>();
        List<AssetInfo> allAssets = getAllAssets();

        for (AssetInfo asset : allAssets) {
            if (asset.getLocationName().equals(location)) {
                filteredAssets.add(asset);
            }
        }

        return filteredAssets;
    }

    public AssetInfo getAssetById(String assetId) {
        try {
            JSONObject inventoryData = inventoryService.getInventoryData();
            if (inventoryData == null) {
                return null;
            }

            JSONObject assets = inventoryData.getJSONObject("assets");
            if (assets.has(assetId)) {
                JSONObject assetJson = assets.getJSONObject(assetId);

                String description = assetJson.isNull("description") ? "" : assetJson.getString("description");
                String locationName = assetJson.getString("locationName");
                String systemName = assetJson.getString("systemName");

                return new AssetInfo(assetId, description, locationName, systemName);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error while downloading resource with ID " + assetId, e);
        }

        return null;
    }
}