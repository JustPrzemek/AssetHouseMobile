package com.zebra.rfid.assethouse;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText editBaseUrl;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editBaseUrl = findViewById(R.id.editBaseUrl);
        btnSave = findViewById(R.id.btnSave);

        // Load the saved BASE_URL if available
        SharedPreferences preferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String savedUrl = preferences.getString("BASE_URL", "http://192.168.88.18:8080/rfidentity");
        editBaseUrl.setText(savedUrl);

        btnSave.setOnClickListener(v -> {
            String newUrl = editBaseUrl.getText().toString().trim();

            if (!newUrl.isEmpty()) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("BASE_URL", newUrl);
                editor.apply();

                Toast.makeText(SettingsActivity.this, "API URL Updated!", Toast.LENGTH_SHORT).show();
                finish(); // Close the settings activity
            } else {
                Toast.makeText(SettingsActivity.this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
            }
        });
        Button btnRestoreDefault = findViewById(R.id.btnRestoreDefault);
        btnRestoreDefault.setOnClickListener(v -> {
            String defaultUrl = "http://192.168.88.18:8080/rfidentity"; // Default URL

            // Set the default URL in EditText
            editBaseUrl.setText(defaultUrl);

            // Save the default URL in SharedPreferences
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("BASE_URL", defaultUrl);
            editor.apply();

            Toast.makeText(SettingsActivity.this, "Default URL Restored!", Toast.LENGTH_SHORT).show();
        });

    }
}
