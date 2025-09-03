package pl.mtu.assethouse.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import pl.mtu.assethouse.R;
import pl.mtu.assethouse.adapters.InventoryDataAdapter;
import pl.mtu.assethouse.api.service.InventoryService;
import pl.mtu.assethouse.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private InventoryService inventoryService;
    private InventoryDataAdapter inventoryDataAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        inventoryService = new InventoryService(this);
        inventoryDataAdapter = new InventoryDataAdapter(this);

        if (inventoryService.hasInventoryData()) {
            int inventoryId = inventoryService.getInventoryId();

            TextView inventoryIdText = findViewById(R.id.inventoryIdText);
            if (inventoryIdText != null) {
                inventoryIdText.setText("Inventory ID: " + inventoryId);
            }

            TextView assetCountText = findViewById(R.id.assetCountText);

            try {
                JSONObject inventoryData = inventoryService.getInventoryData();
                if (inventoryData != null && assetCountText != null) {
                    int assetCount = inventoryData.getJSONObject("assets").length();
                    assetCountText.setText("Number of assets: " + assetCount);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error displaying data", e);
            }
        } else {
            Toast.makeText(this, "No data in inventory", Toast.LENGTH_SHORT).show();
        }

        binding.btnTest.setOnClickListener(v ->
                startActivity(new Intent(this, TestActivity.class)));

        binding.btnAreas.setOnClickListener(v ->
                startActivity(new Intent(this, AreasActivity.class)));

        binding.btnExcludedAssets.setOnClickListener(v ->
                startActivity(new Intent(this, ExcludedAssetsActivity.class)));

        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }
}