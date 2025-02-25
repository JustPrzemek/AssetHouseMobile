package com.zebra.rfid.demo.sdksample;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
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

public class AreasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AreasAdapter adapter;
    private ProgressBar progressBar;
    private static final String BASE_URL = "http://192.168.88.18:8080/rfidentity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_areas);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AreasAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        new FetchAreasTask().execute(BASE_URL, "0", "");
    }

    private class FetchAreasTask extends AsyncTask<String, Void, List<String>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<String> doInBackground(String... params) {
            List<String> areas = new ArrayList<>();
            try {
                String urlString = params[0] + "/api/dashboard/listLocationsWithAssets?location=" + params[2] + "&page=" + params[1] + "&size=5&sort=location";
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
                    JSONArray locations = jsonResponse.getJSONArray("content");
                    for (int i = 0; i < locations.length(); i++) {
                        JSONObject locationObj = locations.getJSONObject(i);
                        String locationName = locationObj.getString("location");
                        areas.add(locationName);
                    }
                }
            } catch (Exception e) {
                Log.e("AreasFetch", "Error fetching areas", e);
            }
            return areas;
        }

        @Override
        protected void onPostExecute(List<String> areas) {
            super.onPostExecute(areas);
            progressBar.setVisibility(View.GONE);
            if (areas.isEmpty()) {
                Toast.makeText(AreasActivity.this, "No areas found", Toast.LENGTH_SHORT).show();
            } else {
                adapter.updateAreas(areas);
            }
        }
    }
}

