package com.zebra.rfid.demo.sdksample;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zebra.rfid.api3.TagData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AreaDetailsActivity extends AppCompatActivity implements RFIDHandler.ResponseHandlerInterface {

    private TextView locationNameText;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AssetsAdapter adapter;
    private Button sendButton;
    private RFIDHandler rfidHandler;
    private Set<String> scannedTags = new HashSet<>();
    private List<JSONObject> assetsList = new ArrayList<>();
    private static final String BASE_URL = "http://192.168.88.55:8080/rfidentity";
    TextView statusTextViewRFID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_details);

        locationNameText = findViewById(R.id.locationName);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        sendButton = findViewById(R.id.sendButton);

        statusTextViewRFID = findViewById(R.id.textViewStatusrfid);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssetsAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("AREA_NAME")) {
            String location = intent.getStringExtra("AREA_NAME");
            locationNameText.setText(location);
            new FetchAssetsTask().execute(BASE_URL, location);
        } else {
            Toast.makeText(this, "No location provided", Toast.LENGTH_SHORT).show();
            finish();
        }

        rfidHandler = new RFIDHandler();
        rfidHandler.onCreate(this);

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
    public void handleTagdata(TagData[] tagData) {
        for (TagData tag : tagData) {
            scannedTags.add(tag.getTagID());
        }
        runOnUiThread(this::compareScannedWithAssets);
    }

    @Override
    public void handleTriggerPress(boolean pressed) {
        if (pressed) {
            scannedTags.clear();
            rfidHandler.performInventory();
        } else {
            rfidHandler.stopInventory();
        }
    }

    @Override
    public void barcodeData(String val) {

    }

    @Override
    public void sendToast(String val) {
        runOnUiThread(() -> Toast.makeText(AreaDetailsActivity.this, val, Toast.LENGTH_SHORT).show());
    }

    //END OF RFID HANDLING

    private class FetchAssetsTask extends AsyncTask<String, Void, List<JSONObject>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        @Override
        protected List<JSONObject> doInBackground(String... params) {
            List<JSONObject> assets = new ArrayList<>();
            try {
                String urlString = params[0] + "/api/locations/insideLocation?location=" + params[1] + "&page=0&size=1000&sort=inventoryStatus";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray content = jsonResponse.getJSONArray("content");

                    for (int i = 0; i < content.length(); i++) {
                        assets.add(content.getJSONObject(i));
                    }
                }
            } catch (Exception e) {
                Log.e("FetchAssets", "Error fetching assets", e);
            }
            return assets;
        }

        @Override
        protected void onPostExecute(List<JSONObject> assets) {
            super.onPostExecute(assets);
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (assets.isEmpty()) {
                Toast.makeText(AreaDetailsActivity.this, "No assets found", Toast.LENGTH_SHORT).show();
            } else {
                adapter.updateAssets(assets);
            }
        }
    }

    private void compareScannedWithAssets() {
        Set<String> assetIdsFromDB = new HashSet<>();
        List<JSONObject> updatedAssets = new ArrayList<>();

        for (JSONObject asset : assetsList) {
            String assetId = asset.optString("assetId", "");
            assetIdsFromDB.add(assetId);

            if (scannedTags.contains(assetId)) {
                try {
                    asset.put("status", "SCANNED");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    asset.put("status", "NOT_SCANNED");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            updatedAssets.add(asset);
        }

        for (String scannedTag : scannedTags) {
            if (!assetIdsFromDB.contains(scannedTag)) {
                JSONObject newAsset = new JSONObject();
                try {
                    newAsset.put("assetId", scannedTag);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                try {
                    newAsset.put("description", "Newly Scanned Asset");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                try {
                    newAsset.put("status", "NEW_SCANNED");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                updatedAssets.add(newAsset);
            }
        }

        adapter.updateAssets(updatedAssets);
    }
}
