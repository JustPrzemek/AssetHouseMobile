package com.zebra.rfid.demo.sdksample;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AreaDetailsActivity extends AppCompatActivity {

    private TextView locationNameText;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AssetsAdapter adapter;

    private static final String BASE_URL = "http://192.168.88.18:8080/rfidentity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_details);

        locationNameText = findViewById(R.id.locationName);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

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
    }

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
}
