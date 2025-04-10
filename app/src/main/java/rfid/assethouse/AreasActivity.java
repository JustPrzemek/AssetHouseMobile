package rfid.assethouse;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
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

import java.net.URLEncoder;
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
    private ViewGroup rootView;
    private boolean isKeyboardVisible = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences preferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        BASE_URL = preferences.getString("BASE_URL", "https://rze-assethouse-t:8443/rfidentity");

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
        rootView = findViewById(android.R.id.content);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AreasAdapter(new ArrayList<>(), areaName -> {
            Intent intent = new Intent(AreasActivity.this, AreaDetailsActivity.class);
            intent.putExtra("AREA_NAME", areaName);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        fetchAreas();

        searchButton.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
            }

            currentSearchQuery = searchInput.getText().toString();
            searchInput.clearFocus();
            currentPage = 0;
            new FetchAreasTask().execute(BASE_URL, String.valueOf(currentPage), currentSearchQuery);
        });
        resetButton.setOnClickListener(v -> {
            currentSearchQuery = "";
            currentPage = 0;
            searchInput.setText("");
            new FetchAreasTask().execute(BASE_URL, String.valueOf(currentPage), currentSearchQuery);

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
            }
            searchInput.clearFocus();
        });
        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                fetchAreas();
                recyclerView.scrollToPosition(0);
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                fetchAreas();
                recyclerView.scrollToPosition(0);
            }
        });

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                boolean isKeyboardNowVisible = keypadHeight > screenHeight * 0.15;

                if (isKeyboardNowVisible != isKeyboardVisible) {
                    isKeyboardVisible = isKeyboardNowVisible;
                    if (isKeyboardVisible) {
                        buttonLayout.setVisibility(View.GONE);
                    } else {
                        if (progressBar.getVisibility() != View.VISIBLE) {
                            buttonLayout.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });

        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                buttonLayout.setVisibility(View.GONE);
            } else if (progressBar.getVisibility() != View.VISIBLE) {
                buttonLayout.setVisibility(View.VISIBLE);
            }
        });

    }

    private void fetchAreas() {
        new FetchAreasTask().execute(BASE_URL, String.valueOf(currentPage), currentSearchQuery);
    }
    @Override
    protected void onResume() {
        super.onResume();
        fetchAreas();
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
                String encodedLocation = URLEncoder.encode(params[2], "UTF-8");
                String urlString = params[0] + "/api/dashboard/listLocationsWithAssets?location=" + encodedLocation + "&page=" + params[1] + "&size=10&sort=location";
                URL url = new URL(urlString);

                HttpURLConnection connection = null;
                if (params[0].contains("https")) {
                    connection = (HttpsURLConnection) url.openConnection();
                } else if (params[0].contains("http")) {
                    connection = (HttpURLConnection) url.openConnection();
                }

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
                adapter.updateAreas(areas);
                Toast.makeText(AreasActivity.this, "No areas found", Toast.LENGTH_SHORT).show();
            } else {
                adapter.updateAreas(areas);
            }

            btnPrevious.setEnabled(currentPage > 0);
            btnNext.setEnabled(currentPage < totalPages - 1);
        }
    }
}
