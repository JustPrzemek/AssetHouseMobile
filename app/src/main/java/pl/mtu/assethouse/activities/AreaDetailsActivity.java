package pl.mtu.assethouse.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.Collections;
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
    private Button switchModeButton;
    private boolean isSortedByAssetIdAsc = true;

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

        switchModeButton = findViewById(R.id.switchModeButton);
        switchModeButton.setOnClickListener(v -> switchScanMode());
        rfidHandler.setScanMode(RFIDHandler.ScanMode.RFID);
    }

    private void switchScanMode() {
        if (rfidHandler.getCurrentMode() == RFIDHandler.ScanMode.RFID) {
            rfidHandler.setScanMode(RFIDHandler.ScanMode.BARCODE);
            switchModeButton.setText(R.string.switch_to_rfid);
            Toast.makeText(this, R.string.barcode_mode, Toast.LENGTH_SHORT).show();
        } else {
            rfidHandler.setScanMode(RFIDHandler.ScanMode.RFID);
            switchModeButton.setText(R.string.switch_to_barcode);
            Toast.makeText(this, R.string.rfid_mode, Toast.LENGTH_SHORT).show();
        }
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
        List<Asset> combinedList = new ArrayList<>();
        combinedList.addAll(assetsList);
        combinedList.addAll(scannedAssetsList);
        adapter = new AssetsAdapter(combinedList, assetService);
        recyclerView.setAdapter(adapter);
    }

    private void toggleViewType() {
        boolean showPlacement = toggleText.getText().equals("Description");
        toggleText.setText(showPlacement ? "Placement" : "Description");
        adapter.setShowPlacementView(showPlacement);
    }

    private void assetIdSort() {
        List<Asset> combinedList = new ArrayList<>();
        combinedList.addAll(assetsList);
        combinedList.addAll(scannedAssetsList);

        if (isSortedByAssetIdAsc) {
            combinedList.sort((a1, a2) -> a1.getAssetId().compareToIgnoreCase(a2.getAssetId()));
        } else {
            combinedList.sort((a1, a2) -> a2.getAssetId().compareToIgnoreCase(a1.getAssetId()));
        }

        isSortedByAssetIdAsc = !isSortedByAssetIdAsc;

        adapter.updateAssets(combinedList);
        recyclerView.scrollToPosition(0);
    }

    private void fetchAssets(String location) {
        new FetchAssetsTask().execute(location);
    }

    private void updateAdapterWithSortedList() {

        List<Asset> combinedList = new ArrayList<>();
        combinedList.addAll(assetsList);
        combinedList.addAll(scannedAssetsList);

        Collections.sort(combinedList, (a1, a2) -> {
            return Long.compare(a2.getLastScannedTimestamp(), a1.getLastScannedTimestamp());
        });

        runOnUiThread(() -> {
            adapter.updateAssets(combinedList);
            recyclerView.scrollToPosition(0);
        });
    }
    // RFID Handling methods
    @Override
    public void handleTagdata(TagData[] tagData) {
        processTagData(tagData);
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

        updateAdapterWithSortedList();
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

            if (scannedTagIds.contains(cleanAssetId)) {
                String currentStatus = asset.getStatus();
                if ("MISSING".equals(currentStatus) || "UNSCANNED".equals(currentStatus)) {
                    asset.setStatus("OK");
                    missingToOkCount++;
                    asset.setLastScannedTimestamp(System.currentTimeMillis());
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
                        newAsset.setLastScannedTimestamp(System.currentTimeMillis());
                        scannedAssetsList.add(newAsset);
                        newAssetsCount++;
                    }
                }
            }
        }
    }

    @Override
    public void handleTriggerPress(boolean pressed) {
        if (rfidHandler.getCurrentMode() == RFIDHandler.ScanMode.RFID) {
            if (pressed) {
                resetCounters();
                rfidHandler.performInventory();
            } else {
                rfidHandler.stopInventory();
                showScanResults();
            }
        }
    }

    private void processBarcodeData(String barcode) {
        String cleanedBarcode = cleanTagId(barcode);
        if (cleanedBarcode == null) return;

        boolean found = false;
        for (Asset asset : assetsList) {
            if (cleanedBarcode.equals(cleanTagId(asset.getAssetId()))) {
                if ("MISSING".equals(asset.getStatus()) || "UNSCANNED".equals(asset.getStatus())) {
                    asset.setStatus("OK");
                    Toast.makeText(this, "Updated: " + asset.getAssetId(), Toast.LENGTH_SHORT).show();
                }
                asset.setLastScannedTimestamp(System.currentTimeMillis());
                found = true;
                break;
            }
        }

        if (!found) {
            boolean alreadyScanned = scannedAssetsList.stream()
                    .anyMatch(a -> cleanedBarcode.equals(cleanTagId(a.getAssetId())));

            if (!alreadyScanned) {
                AssetInfo assetInfo = inventoryDataAdapter.getAssetById(cleanedBarcode);
                if (assetInfo != null) {
                    Asset newAsset = new Asset();
                    newAsset.setAssetId(cleanedBarcode);
                    newAsset.setDescription(assetInfo.getDescription());
                    newAsset.setStatus("NEW");
                    newAsset.setExpectedLocation(assetInfo.getLocationName());
                    newAsset.setSystemName(assetInfo.getSystemName());
                    newAsset.setLastScannedTimestamp(System.currentTimeMillis());
                    scannedAssetsList.add(newAsset);
                    Toast.makeText(this, "Added new: " + newAsset.getAssetId(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Unknown barcode: " + cleanedBarcode, Toast.LENGTH_SHORT).show();
                }
            }
        }
        updateAdapterWithSortedList();
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

    private final BroadcastReceiver barcodeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(getString(R.string.activity_intent_filter_action))) {
                String scannedData = intent.getStringExtra(getResources().getString(R.string.datawedge_intent_key_data));
                if (scannedData != null) {
                    processBarcodeData(scannedData);
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        String result = rfidHandler.onResume();
        Log.d("RFID", result);

        IntentFilter filter = new IntentFilter(getString(R.string.activity_intent_filter_action));
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(barcodeReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        rfidHandler.onPause();
        unregisterReceiver(barcodeReceiver);
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

            if (assets == null) {
                Toast.makeText(AreaDetailsActivity.this, "No assets found or error fetching", Toast.LENGTH_SHORT).show();
                assetsList.clear();
            } else {
                assetsList.clear();
                assetsList.addAll(assets);
            }
            updateAdapterWithSortedList();
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
                for(Asset asset : assetsList) {
                    asset.setLastScannedTimestamp(0);
                }
                fetchAssets(locationNameText.getText().toString());
            } else {
                Toast.makeText(AreaDetailsActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}