package pl.mtu.assethouse.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import pl.mtu.assethouse.api.service.InventoryService;
import pl.mtu.assethouse.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private InventoryService inventoryService;
    private Handler handler;
    private static final int SETTINGS_REQUEST_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        inventoryService = new InventoryService(this);
        handler = new Handler(Looper.getMainLooper());

        loadData();
    }

    private void loadData() {
        inventoryService = new InventoryService(this);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.loadingText.setText("Connecting to network...");
        binding.retryButton.setVisibility(View.GONE);
        binding.btnSettings.setVisibility(View.GONE);

        inventoryService.loadInventoryData()
                .thenAccept(success -> {
                    handler.post(() -> {
                        if (success) {
                            binding.loadingText.setText("Data loaded successfully");
                            handler.postDelayed(this::startMainActivity, 1000);
                        } else {
                            showRetryUI();
                        }
                    });
                });
    }

    private void showRetryUI() {
        binding.loadingText.setText("Connection error");
        binding.retryButton.setVisibility(View.VISIBLE);
        binding.btnSettings.setVisibility(View.VISIBLE);

        binding.retryButton.setOnClickListener(v -> loadData());

        binding.btnSettings.setOnClickListener(v -> {
            startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS_REQUEST_CODE);
        });

        Toast.makeText(this, "Failed to download data", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE) {
            loadData();
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        inventoryService.shutdown();
    }
}
