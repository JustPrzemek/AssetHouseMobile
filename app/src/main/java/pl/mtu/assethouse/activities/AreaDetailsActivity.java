package pl.mtu.assethouse.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zebra.rfid.api3.TagData;

import org.json.JSONObject;

import pl.mtu.assethouse.adapters.AssetsAdapter;
import pl.mtu.assethouse.R;
import pl.mtu.assethouse.adapters.InventoryDataAdapter;
import pl.mtu.assethouse.api.ApiClient;
import pl.mtu.assethouse.api.service.AssetService;
import pl.mtu.assethouse.api.service.InventoryService;
import pl.mtu.assethouse.models.Asset;
import pl.mtu.assethouse.models.AssetInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AreaDetailsActivity extends AppCompatActivity implements RFIDHandler.ResponseHandlerInterface {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AssetsAdapter adapter;
    private Button saveButton;
    private RFIDHandler rfidHandler;
    private TextView toggleText, assetIdSortButton, locationNameText, statusTextViewRFID;
    private InventoryDataAdapter inventoryDataAdapter;
    private AssetService assetService;
    private List<Asset> assetsList = new ArrayList<>();
    private List<Asset> scannedAssetsList = new ArrayList<>();
    private int newAssetsCount = 0;
    private int missingToOkCount = 0;
    private Toast statusToast;
    private String currentSortParameter = "inventoryStatus";
    private final Executor runOnUiThreadExecutor = Executors.newSingleThreadExecutor(r -> {
        Handler handler = new Handler(Looper.getMainLooper());
        return new Thread(() -> {
            Looper.prepare();
            handler.post(r);
            Looper.loop();
        });
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_details);

        assetService = new AssetService(this);
        rfidHandler = new RFIDHandler();
        rfidHandler.onCreate(this);
        inventoryDataAdapter = new InventoryDataAdapter(this);

        initializeViews();
        setupRecyclerView();

        String location = getIntent().getStringExtra("AREA_NAME");
        if (location == null) {
            Toast.makeText(this, "No location provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        locationNameText.setText(location);
        checkInventoryAndFetchAssets(location);
    }

    private void initializeViews() {
        locationNameText = findViewById(R.id.locationName);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        statusTextViewRFID = findViewById(R.id.textViewStatusrfid);
        saveButton = findViewById(R.id.saveButton);
        toggleText = findViewById(R.id.toggleText);
        assetIdSortButton = findViewById(R.id.assetIdSortButton);


        saveButton.setOnClickListener(v -> saveAssets());
        toggleText.setOnClickListener(v -> toggleViewType());
        assetIdSortButton.setOnClickListener(v -> assetIdSort());
    }

    private void checkInventoryAndFetchAssets(String location) {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                Map<String, String> params = new HashMap<>();
                String response = new ApiClient(this).get("/api/mobile/inventoryNumber", params);
                JSONObject jsonResponse = new JSONObject(response);
                int serverInventoryId = jsonResponse.getInt("inventoryId");

                int localInventoryId = new InventoryService(this).getInventoryId();

                if (serverInventoryId != localInventoryId) {
                    runOnUiThread(this::restartApplication);
                    return;
                }

                runOnUiThread(() -> fetchAssets(location));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error checking inventory", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }
    private void restartApplication() {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        Runtime.getRuntime().exit(0);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssetsAdapter(assetsList, scannedAssetsList);
        recyclerView.setAdapter(adapter);
    }

    private void toggleViewType() {
        boolean showPlacement = toggleText.getText().equals("Description");
        toggleText.setText(showPlacement ? "Placement" : "Description");
        adapter.setShowPlacementView(showPlacement);
    }

    private void assetIdSort() {
        if (currentSortParameter.equals("inventoryStatus")) {
            currentSortParameter = "assetId";
        } else {
            currentSortParameter = "inventoryStatus";
        }
        fetchAssets(locationNameText.getText().toString());
    }

    private void fetchAssets(String location) {
        new FetchAssetsTask().execute(location);
    }

    // RFID Handling methods
    @Override
    public void handleTagdata(TagData[] tagData) {
        processTagData(tagData);
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    private void processTagData(TagData[] tagData) {
        Set<String> scannedTagIds = Arrays.stream(tagData)
                .map(TagData::getTagID)
                .filter(tagId -> !tagId.isEmpty())
                .map(this::cleanTagId)
                .filter(Objects::nonNull)
                .filter(tag -> !"82442-0".equals(tag))
                .collect(Collectors.toSet());

        Set<String> existingAssetIds = assetsList.stream()
                .map(Asset::getAssetId)
                .map(this::cleanTagId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        inventoryDataAdapter = new InventoryDataAdapter(this);
        List<AssetInfo> allAssetsInPrefs = inventoryDataAdapter.getAllAssets();
        Set<String> prefsAssetIds = allAssetsInPrefs.stream()
                .map(AssetInfo::getAssetId)
                .collect(Collectors.toSet());

        updateAssetStatuses(scannedTagIds, existingAssetIds);
        addNewAssets(scannedTagIds, existingAssetIds, prefsAssetIds);

        runOnUiThread(() -> adapter.updateAssets(assetsList, scannedAssetsList));
    }

    private String cleanTagId(String tagId) {
        if (tagId == null || tagId.trim().isEmpty()) {
            return null;
        }

        String cleaned = tagId.trim().replaceAll("\\s", "");

        if ("82442-0".equals(cleaned)) {
            return null;
        }

        cleaned = cleaned.split("[^a-zA-Z0-9-]")[0].trim();

        if (cleaned.contains("-")) {
            if (!cleaned.matches(".*-\\d+.*")) {
                return null;
            }
            return cleaned.replaceAll("^(\\d+-\\d+).*", "$1");
        } else {
            String digitsOnly = cleaned.replaceAll("[^0-9]", "");
            return digitsOnly.length() >= 4 ? digitsOnly : null;
        }
    }

    private void updateAssetStatuses(Set<String> scannedTagIds, Set<String> existingAssetIds) {
        for (Asset asset : assetsList) {
            String cleanAssetId = cleanTagId(asset.getAssetId());
            if (cleanAssetId == null) continue;

            String currentStatus = asset.getStatus();
            if (scannedTagIds.contains(cleanAssetId)) {
                if ("MISSING".equals(currentStatus)) {
                    asset.setStatus("OK");
                    missingToOkCount++;
                } else if ("UNSCANNED".equals(currentStatus)) {
                    asset.setStatus("OK");
                    missingToOkCount++;
                }
            }

        }
    }

    private void addNewAssets(Set<String> scannedTagIds, Set<String> existingAssetIds, Set<String> prefsAssetIds) {
        for (String scannedTag : scannedTagIds) {
            if (scannedTag != null && !existingAssetIds.contains(scannedTag)) {
                if (prefsAssetIds.contains(scannedTag)) {
                    boolean alreadyScanned = scannedAssetsList.stream()
                            .anyMatch(a -> scannedTag.equals(cleanTagId(a.getAssetId())));

                    if (!alreadyScanned) {
                        AssetInfo assetInfo = inventoryDataAdapter.getAssetById(scannedTag);

                        Asset newAsset = new Asset();
                        newAsset.setAssetId(scannedTag);
                        newAsset.setDescription(assetInfo != null ? assetInfo.getDescription() : "Newly Scanned Asset");
                        newAsset.setStatus("NEW");
                        newAsset.setExpectedLocation(assetInfo != null ? assetInfo.getLocationName() : "No location");
                        newAsset.setSystemName(assetInfo != null ? assetInfo.getSystemName() : "No system");
                        scannedAssetsList.add(newAsset);
                        newAssetsCount++;
                    }
                }
            }
        }
    }

    @Override
    public void handleTriggerPress(boolean pressed) {
        if (pressed) {
            resetCounters();
            rfidHandler.performInventory();
        } else {
            rfidHandler.stopInventory();
            showScanResults();
        }
    }

    private void resetCounters() {
        newAssetsCount = 0;
        missingToOkCount = 0;
    }

    private void showScanResults() {
        String message = "Found assets:" + "\nNew: " + newAssetsCount + "\nOK: " + missingToOkCount;
        runOnUiThread(() -> {
            if (statusToast != null) statusToast.cancel();
            statusToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
            statusToast.show();
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        String result = rfidHandler.onResume();
        Log.d("RFID", result);
    }

    @Override
    protected void onPause() {
        super.onPause();
        rfidHandler.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rfidHandler.onDestroy();
    }

    @Override
    public void barcodeData(String val) {

    }

    @Override
    public void sendToast(String val) {
        runOnUiThread(() -> {
            if (statusToast != null) {
                statusToast.cancel();
            }
            statusToast = Toast.makeText(AreaDetailsActivity.this, val, Toast.LENGTH_SHORT);
            statusToast.show();
        });
    }

    //END OF RFID HANDLING

    private class FetchAssetsTask extends AsyncTask<String, Void, List<Asset>> {
        @Override
        protected List<Asset> doInBackground(String... params) {
            try {
                List<Asset> assets = assetService.getAssetsInLocation(params[0], currentSortParameter);

                Set<String> scannedAssetIds = scannedAssetsList.stream()
                        .map(Asset::getAssetId)
                        .collect(Collectors.toSet());

                for (Asset asset : assets) {
                    if (scannedAssetIds.contains(asset.getAssetId())) {
                        asset.setStatus("OK");
                    }
                }

                return assets;
            } catch (Exception e) {
                Log.e("FetchAssets", "Error fetching assets", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Asset> assets) {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (assets == null || assets.isEmpty()) {
                Toast.makeText(AreaDetailsActivity.this, "No assets found", Toast.LENGTH_SHORT).show();
                return;
            }

            assetsList.clear();
            assetsList.addAll(assets);
            adapter.updateAssets(assetsList, scannedAssetsList);
        }
    }

    private void saveAssets() {
        missingToOkCount = (int) assetsList.stream()
                .filter(a -> "MISSING".equals(a.getStatus()))
                .count();

        newAssetsCount = scannedAssetsList.size();

        new SaveAssetsTask().execute();
    }

    private class SaveAssetsTask extends AsyncTask<Void, Void, Boolean> {

        private String errorMessage = "Unknown error";
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                assetService.updateAssets(
                        locationNameText.getText().toString(),
                        assetsList,
                        scannedAssetsList
                );
                return true;
            } catch (Exception e) {
                String backendResponse = e.getMessage();
                if (backendResponse != null) {
                    try {
                        if (backendResponse.contains("{")) {
                            backendResponse = backendResponse.substring(backendResponse.indexOf("{"));
                        }
                        JSONObject errorJson = new JSONObject(backendResponse);
                        errorMessage = errorJson.optString("message", "Unknown server error") + "\nPlease refresh the location view";
                    } catch (Exception parseException) {
                        errorMessage = backendResponse + "\nPlease refresh the location view";
                    }
                }
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(AreaDetailsActivity.this, "Data saved successfully!", Toast.LENGTH_SHORT).show();
                scannedAssetsList.clear();
                newAssetsCount = 0;
                missingToOkCount = 0;
                fetchAssets(locationNameText.getText().toString());
            } else {
                Toast.makeText(AreaDetailsActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}