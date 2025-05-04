package pl.mtu.assethouse.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pl.mtu.assethouse.api.service.InventoryService;
import pl.mtu.assethouse.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private InventoryService inventoryService;
    private Handler handler;

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
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.loadingText.setText("Connecting to network...");

        inventoryService.loadInventoryData()
                .thenAccept(success -> {
                    handler.post(() -> {
                        if (success) {
                            binding.loadingText.setText("Data loaded successfully");

                            handler.postDelayed(this::startMainActivity, 1000);
                        } else {
                            binding.loadingText.setText("Connection error");
                            binding.retryButton.setVisibility(View.VISIBLE);
                            binding.retryButton.setOnClickListener(v -> {
                                binding.retryButton.setVisibility(View.GONE);
                                loadData();
                            });

                            Toast.makeText(this,
                                    "Failed to download data", Toast.LENGTH_LONG).show();
                        }
                    });
                });
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
