package pl.mtu.assethouse.adapters;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.mtu.assethouse.api.service.InventoryService;
import pl.mtu.assethouse.models.AssetInfo;

public class InventoryDataAdapter {
    private static final String TAG = "InventoryDataHelper";

    private final InventoryService inventoryService;

    // Parsed once. Cannot go stale: an inventory change restarts the process.
    private Map<String, AssetInfo> assetsById;

    public InventoryDataAdapter(Context context) {
        this.inventoryService = new InventoryService(context);
    }

    private synchronized Map<String, AssetInfo> assets() {
        if (assetsById != null) {
            return assetsById;
        }

        Map<String, AssetInfo> parsed = new LinkedHashMap<>();
        try {
            JSONObject inventoryData = inventoryService.getInventoryData();
            if (inventoryData == null) {
                Log.e(TAG, "No Inventory");
                return assetsById = parsed;
            }

            JSONObject assets = inventoryData.getJSONObject("assets");
            Iterator<String> keys = assets.keys();
            while (keys.hasNext()) {
                String assetId = keys.next();
                JSONObject assetJson = assets.getJSONObject(assetId);
                String description = assetJson.isNull("description") ? "" : assetJson.getString("description");
                String locationName = assetJson.getString("locationName");
                String systemName = assetJson.getString("systemName");
                parsed.put(assetId, new AssetInfo(assetId, description, locationName, systemName));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error while retrieving resource data", e);
        }

        return assetsById = parsed;
    }

    public Set<String> getAssetIds() {
        return Collections.unmodifiableSet(assets().keySet());
    }

    public int getInventoryId() {
        return inventoryService.getInventoryId();
    }

    public List<AssetInfo> getAllAssets() {
        return new ArrayList<>(assets().values());
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
        return assets().get(assetId);
    }
}