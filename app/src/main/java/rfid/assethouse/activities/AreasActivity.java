package rfid.assethouse.activities;
import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
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

import java.util.ArrayList;
import java.util.List;
import android.content.Intent;

import rfid.assethouse.adapters.AreasAdapter;
import rfid.assethouse.R;
import rfid.assethouse.api.service.AreaService;
import rfid.assethouse.models.Area;

public class AreasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AreasAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout buttonLayout;
    private Button btnPrevious, btnNext, searchButton, resetButton;
    private EditText searchInput;
    private int currentPage = 0;
    private int totalPages = 1;
    private String currentSearchQuery = "";
    private ViewGroup rootView;
    private boolean isKeyboardVisible = false;
    private AreaService areaService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_areas);

        areaService = new AreaService(this);
        initializeViews();
        setupRecyclerView();
        setupListeners();
        fetchAreas();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        buttonLayout = findViewById(R.id.buttonLayout);
        btnPrevious = findViewById(R.id.prevButton);
        btnNext = findViewById(R.id.nextButton);
        searchInput = findViewById(R.id.searchInput);
        searchButton = findViewById(R.id.searchButton);
        resetButton = findViewById(R.id.resetButton);
        rootView = findViewById(android.R.id.content);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AreasAdapter(new ArrayList<>(), areaName -> {
            Intent intent = new Intent(AreasActivity.this, AreaDetailsActivity.class);
            intent.putExtra("AREA_NAME", areaName);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        searchButton.setOnClickListener(v -> handleSearch());
        resetButton.setOnClickListener(v -> handleReset());
        btnNext.setOnClickListener(v -> handleNextPage());
        btnPrevious.setOnClickListener(v -> handlePreviousPage());

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


    private void handleSearch() {
        hideKeyboard();
        currentSearchQuery = searchInput.getText().toString();
        currentPage = 0;
        fetchAreas();
    }

    private void handleReset() {
        hideKeyboard();
        currentSearchQuery = "";
        currentPage = 0;
        searchInput.setText("");
        fetchAreas();
    }

    private void handleNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            fetchAreas();
            recyclerView.scrollToPosition(0);
        }
    }

    private void handlePreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            fetchAreas();
            recyclerView.scrollToPosition(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAreas();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
        searchInput.clearFocus();
    }

    private void fetchAreas() {
        new FetchAreasTask().execute();
    }

    private class FetchAreasTask extends AsyncTask<Void, Void, Pair<List<Area>, Integer>> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            buttonLayout.setVisibility(View.GONE);
        }

        @Override
        protected Pair<List<Area>, Integer> doInBackground(Void... voids) {
            try {
                return areaService.getAreas(currentSearchQuery, currentPage, 10);
            } catch (Exception e) {
                Log.e("AreasFetch", "Error fetching areas", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Pair<List<Area>, Integer> result) {
            progressBar.setVisibility(View.GONE);
            buttonLayout.setVisibility(View.VISIBLE);

            if (result == null) {
                Toast.makeText(AreasActivity.this, "Error fetching areas", Toast.LENGTH_SHORT).show();                return;
            }

            totalPages = result.second;

            adapter.updateAreas(result.first);

            if (result.first.isEmpty()) {
                Toast.makeText(AreasActivity.this, "No areas found", Toast.LENGTH_SHORT).show();
            }

            btnPrevious.setEnabled(currentPage > 0);
            btnNext.setEnabled(currentPage < totalPages - 1);

        }
    }
}