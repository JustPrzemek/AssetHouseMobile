package pl.mtu.assethouse.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import pl.mtu.assethouse.R;
import pl.mtu.assethouse.adapters.ExcludedAssetsAdapter;
import pl.mtu.assethouse.models.ExcludedAssets;
import pl.mtu.assethouse.api.service.ExcludedAssetsService;

public class ExcludedAssetsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ExcludedAssetsAdapter adapter;
    private ExcludedAssetsService excludedAssetsService;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excluded_assets);

        recyclerView = findViewById(R.id.recyclerViewExcludedAssets);
        progressBar = findViewById(R.id.progressBar);

        excludedAssetsService = new ExcludedAssetsService(this);
        adapter = new ExcludedAssetsAdapter();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchAndDisplayData();
    }

    private void fetchAndDisplayData() {
        progressBar.setVisibility(View.VISIBLE);

        excludedAssetsService.fetchAllExcludedAssets(new ExcludedAssetsService.FetchDataCallback() {
            @Override
            public void onSuccess(List<ExcludedAssets> assets) {
                progressBar.setVisibility(View.GONE);
                adapter.setData(assets);
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ExcludedAssetsActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}