package rfid.assethouse.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import rfid.assethouse.R;
import rfid.assethouse.utils.SharedPrefsManager;

public class SettingsActivity extends AppCompatActivity {
    private EditText editBaseUrl;
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefsManager = new SharedPrefsManager(this);
        editBaseUrl = findViewById(R.id.editBaseUrl);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnRestoreDefault = findViewById(R.id.btnRestoreDefault);

        editBaseUrl.setText(prefsManager.getBaseUrl());

        btnSave.setOnClickListener(v -> saveSettings());
        btnRestoreDefault.setOnClickListener(v -> restoreDefaults());
    }

    private void saveSettings() {
        String newUrl = editBaseUrl.getText().toString().trim();
        if (!newUrl.isEmpty()) {
            prefsManager.setBaseUrl(newUrl);
            Toast.makeText(this, "API URL Updated!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreDefaults() {
        prefsManager.resetBaseUrl();
        editBaseUrl.setText(prefsManager.getBaseUrl());
        Toast.makeText(this, "Default URL Restored!", Toast.LENGTH_SHORT).show();
    }
}
