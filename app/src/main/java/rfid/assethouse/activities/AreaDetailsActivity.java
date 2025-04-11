package rfid.assethouse.activities;

import android.os.AsyncTask;
import android.os.Bundle;
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
import rfid.assethouse.adapters.AssetsAdapter;
import rfid.assethouse.R;
import rfid.assethouse.api.service.AssetService;
import rfid.assethouse.models.Asset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import java.util.stream.Collectors;

public class AreaDetailsActivity extends AppCompatActivity implements RFIDHandler.ResponseHandlerInterface {

    private TextView locationNameText;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AssetsAdapter adapter;
    private Button saveButton;
    private RFIDHandler rfidHandler;
    private TextView statusTextViewRFID;
    private TextView toggleText;

    private AssetService assetService;
    private List<Asset> assetsList = new ArrayList<>();
    private List<Asset> scannedAssetsList = new ArrayList<>();
    private int newAssetsCount = 0;
    private int missingToOkCount = 0;
    private Toast statusToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_details);

        assetService = new AssetService(this);
        rfidHandler = new RFIDHandler();
        rfidHandler.onCreate(this);

        initializeViews();
        setupRecyclerView();

        String location = getIntent().getStringExtra("AREA_NAME");
        if (location == null) {
            Toast.makeText(this, "No location provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        locationNameText.setText(location);
        fetchAssets(location);
    }

    private void initializeViews() {
        locationNameText = findViewById(R.id.locationName);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        statusTextViewRFID = findViewById(R.id.textViewStatusrfid);
        saveButton = findViewById(R.id.saveButton);
        toggleText = findViewById(R.id.toggleText);

        saveButton.setOnClickListener(v -> saveAssets());
        toggleText.setOnClickListener(v -> toggleViewType());
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
                .collect(Collectors.toSet());

        Set<String> existingAssetIds = assetsList.stream()
                .map(Asset::getAssetId)
                .collect(Collectors.toSet());

        updateAssetStatuses(scannedTagIds, existingAssetIds);
        addNewAssets(scannedTagIds, existingAssetIds);

        runOnUiThread(() -> adapter.updateAssets(assetsList, scannedAssetsList));
    }

    private void updateAssetStatuses(Set<String> scannedTagIds, Set<String> existingAssetIds) {
        for (Asset asset : assetsList) {
            String currentStatus = asset.getStatus();

            if (scannedTagIds.contains(asset.getAssetId())) {
                if ("MISSING".equals(currentStatus) || "UNSCANNED".equals(currentStatus)) {
                    asset.setStatus("OK");
                    if ("MISSING".equals(currentStatus)) missingToOkCount++;
                }
            } else if ("UNSCANNED".equals(currentStatus)) {
                asset.setStatus("MISSING");
            }
        }
    }

    private void addNewAssets(Set<String> scannedTagIds, Set<String> existingAssetIds) {
        for (String scannedTag : scannedTagIds) {
            if (!existingAssetIds.contains(scannedTag)) {
                boolean alreadyScanned = scannedAssetsList.stream()
                        .anyMatch(a -> a.getAssetId().equals(scannedTag));

                if (!alreadyScanned) {
                    Asset newAsset = new Asset();
                    newAsset.setAssetId(scannedTag);
                    newAsset.setDescription("Newly Scanned Asset");
                    newAsset.setStatus("NEW");
                    scannedAssetsList.add(newAsset);
                    newAssetsCount++;
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
        scannedAssetsList.clear();
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
    //RFID HANDLING HERE
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
                List<Asset> assets = assetService.getAssetsInLocation(params[0]);

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
        new SaveAssetsTask().execute();
    }

    private class SaveAssetsTask extends AsyncTask<Void, Void, Boolean> {
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
                Log.e("SaveAssets", "Error saving assets", e);
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
                Toast.makeText(AreaDetailsActivity.this, "Error saving data", Toast.LENGTH_SHORT).show();
            }
        }
    }
}