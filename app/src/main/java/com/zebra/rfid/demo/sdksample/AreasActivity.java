package com.zebra.rfid.demo.sdksample;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import android.content.Intent;

import javax.net.ssl.HttpsURLConnection;

public class AreasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AreasAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout buttonLayout;
    private Button btnPrevious, btnNext, searchButton, resetButton;
    private EditText searchInput;
    private String BASE_URL;
    private int currentPage = 0;
    private int totalPages = 1;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences preferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        BASE_URL = preferences.getString("BASE_URL", "http://192.168.88.18:8080/rfidentity");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_areas);

        // Trust all certificates
        SSLHelper.trustAllCertificates();

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        buttonLayout = findViewById(R.id.buttonLayout);
        btnPrevious = findViewById(R.id.prevButton);
        btnNext = findViewById(R.id.nextButton);
        searchInput = findViewById(R.id.searchInput);
        searchButton = findViewById(R.id.searchButton);
        resetButton = findViewById(R.id.resetButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AreasAdapter(new ArrayList<>(), areaName -> {
            Intent intent = new Intent(AreasActivity.this, AreaDetailsActivity.class);
            intent.putExtra("AREA_NAME", areaName);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        fetchAreas();

        searchButton.setOnClickListener(v -> {
            currentSearchQuery = searchInput.getText().toString();
            currentPage = 0;
            new FetchAreasTask().execute(BASE_URL, String.valueOf(currentPage), currentSearchQuery);
        });
        resetButton.setOnClickListener(v -> {
            currentSearchQuery = "";
            currentPage = 0;
            new FetchAreasTask().execute(BASE_URL, String.valueOf(currentPage), currentSearchQuery);
        });
        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                fetchAreas();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                fetchAreas();
            }
        });

    }

    private void fetchAreas() {
        new FetchAreasTask().execute(BASE_URL, String.valueOf(currentPage), currentSearchQuery);
    }

    private class FetchAreasTask extends AsyncTask<String, Void, List<JSONObject>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            buttonLayout.setVisibility(View.GONE);
        }

        @Override
        protected List<JSONObject> doInBackground(String... params) {
            List<JSONObject> areas = new ArrayList<>();
            try {
                String urlString = params[0] + "/api/dashboard/listLocationsWithAssets?location=" + params[2] + "&page=" + params[1] + "&size=10&sort=location";
                URL url = new URL(urlString);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
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
                        areas.add(locations.getJSONObject(i));
                    }

                    JSONObject pagination = jsonResponse.getJSONObject("pagination");
                    totalPages = pagination.getInt("totalPages");

                }
            } catch (Exception e) {
                Log.e("AreasFetch", "Error fetching areas", e);
            }
            return areas;
        }

        @Override
        protected void onPostExecute(List<JSONObject> areas) {
            super.onPostExecute(areas);
            progressBar.setVisibility(View.GONE);
            buttonLayout.setVisibility(View.VISIBLE);

            if (areas.isEmpty()) {
                Toast.makeText(AreasActivity.this, "No areas found", Toast.LENGTH_SHORT).show();
            } else {
                adapter.updateAreas(areas);
            }

            btnPrevious.setEnabled(currentPage > 0);
            btnNext.setEnabled(currentPage < totalPages - 1);
        }
    }
}
