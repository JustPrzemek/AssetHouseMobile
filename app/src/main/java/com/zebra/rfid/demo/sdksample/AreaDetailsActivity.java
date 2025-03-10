package com.zebra.rfid.demo.sdksample;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.io.OutputStream;
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
    private Button saveButton;
    private RFIDHandler rfidHandler;
    private Set<String> scannedTags = new HashSet<>();
    private List<JSONObject> assetsList = new ArrayList<>();
    private List<JSONObject> scannedAssetsList = new ArrayList<>();
    private String BASE_URL;
    TextView statusTextViewRFID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_details);
        SharedPreferences preferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        BASE_URL = preferences.getString("BASE_URL", "http://192.168.88.18:8080/rfidentity");

        locationNameText = findViewById(R.id.locationName);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        statusTextViewRFID = findViewById(R.id.textViewStatusrfid);
        saveButton =  findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> saveAssetsToBackend());

        assetsList = new ArrayList<>();
        scannedAssetsList = new ArrayList<>();

        adapter = new AssetsAdapter(assetsList, scannedAssetsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
        try {
            Set<String> scannedTagIds = new HashSet<>();

            for (TagData tag : tagData) {
                String tagId = tag.getTagID();
                if (tagId == null || tagId.trim().isEmpty()) {
                    Log.w("RFID", " Pusty kod RFID - pomijamy.");
                    continue;
                }
                scannedTagIds.add(tagId);
            }

            Set<String> assetIdsFromDB = new HashSet<>();
            for (JSONObject asset : assetsList) {
                assetIdsFromDB.add(asset.optString("assetId", ""));
            }

            for (JSONObject asset : assetsList) {
                try {
                    String assetId = asset.optString("assetId", "");
                    String currentStatus = asset.optString("status", "UNSCANNED");

                    if (scannedTagIds.contains(assetId)) {
                        if (currentStatus.equals("MISSING") || currentStatus.equals("UNSCANNED")) {
                            asset.put("status", "OK");
                        }
                    } else {
                        if (currentStatus.equals("UNSCANNED")) {
                            asset.put("status", "MISSING");
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            for (String scannedTag : scannedTagIds) {
                if (!assetIdsFromDB.contains(scannedTag)) {
                    boolean alreadyExists = false;

                    for (JSONObject asset : scannedAssetsList) {
                        if (asset.optString("assetId").equals(scannedTag)) {
                            alreadyExists = true;
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        JSONObject newAsset = new JSONObject();
                        newAsset.put("assetId", scannedTag);
                        newAsset.put("description", "Newly Scanned Asset");
                        newAsset.put("status", "NEW");
                        scannedAssetsList.add(newAsset);
                    }
                }
            }

            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
            });

        } catch (Exception e) {
            Log.e("RFID", "Error processing RFID tags!", e);
        }
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

                    Log.d("API_RESPONSE", "backend response: " + response.toString());

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray content = jsonResponse.getJSONArray("content");

                    for (int i = 0; i < content.length(); i++) {
                        JSONObject asset = content.getJSONObject(i);
                        Log.d("ASSET_FETCH", "asset fetch: " + asset.toString());
                        assets.add(content.getJSONObject(i));
                    }
                } else {
                    Log.e("API_RESPONSE", "error fetching data, response: " + responseCode);
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
                assetsList = new ArrayList<>(assets);

                for (JSONObject asset : assetsList) {
                    try {

                        String status = asset.has("inventoryStatus") && !asset.isNull("inventoryStatus")
                                ? asset.getString("inventoryStatus").toUpperCase()
                                : "UNSCANNED";

                        asset.put("status", status);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                adapter.updateAssets(assetsList, scannedAssetsList);
            }
        }
    }
    private void saveAssetsToBackend() {
        try {

            JSONObject requestBody = new JSONObject();
            requestBody.put("location", locationNameText.getText().toString());
            requestBody.put("user", "USER");

            JSONArray assetsArray = new JSONArray();

            for (JSONObject asset : assetsList) {
                JSONObject assetObj = new JSONObject();
                assetObj.put("assetId", asset.optString("assetId", ""));
                assetObj.put("status", asset.optString("status", "UNSCANNED"));
                assetObj.put("comment", "");
                assetsArray.put(assetObj);
            }

            for (JSONObject scannedAsset : scannedAssetsList) {
                String scannedId = scannedAsset.optString("assetId", "");

                boolean alreadyExists = false;
                for (JSONObject asset : assetsList) {
                    if (asset.optString("assetId", "").equals(scannedId)) {
                        alreadyExists = true;
                        break;
                    }
                }

                if (!alreadyExists) {
                    JSONObject assetObj = new JSONObject();
                    assetObj.put("assetId", scannedId);
                    assetObj.put("status", scannedAsset.optString("status", "NEW"));
                    assetObj.put("comment", "");
                    assetsArray.put(assetObj);
                }
            }

            requestBody.put("assets", assetsArray);

            sendPostRequest(BASE_URL + "/api/mobile/updateOutcome", requestBody.toString());

        } catch (JSONException e) {
            Log.e("SAVE", "Error creating JSON!", e);
        }
    }
    private void sendPostRequest(String urlString, String jsonBody) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);

                    OutputStream os = connection.getOutputStream();
                    os.write(jsonBody.getBytes("UTF-8"));
                    os.close();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == 204) {
                        return "Data saved successfully!";
                    } else {
                        return "Error: " + responseCode;
                    }
                } catch (Exception e) {
                    return "Connection error!";
                }
            }

            @Override
            protected void onPostExecute(String result) {
                Toast.makeText(AreaDetailsActivity.this, result, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }
}