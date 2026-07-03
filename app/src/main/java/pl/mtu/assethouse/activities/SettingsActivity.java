package pl.mtu.assethouse.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import pl.mtu.assethouse.R;
import pl.mtu.assethouse.utils.SharedPrefsManager;

public class SettingsActivity extends AppCompatActivity {
    private EditText editBaseUrl;
    private RadioGroup radioGroupEnv;
    private RadioButton radioQa, radioProd, radioCustom;
    private SharedPrefsManager prefsManager;
    private int currentSelectedId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefsManager = new SharedPrefsManager(this);
        editBaseUrl = findViewById(R.id.editBaseUrl);
        radioGroupEnv = findViewById(R.id.radioGroupEnv);
        radioQa = findViewById(R.id.radioQa);
        radioProd = findViewById(R.id.radioProd);
        radioCustom = findViewById(R.id.radioCustom);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnRestoreDefault = findViewById(R.id.btnRestoreDefault);

        String currentUrl = prefsManager.getBaseUrl();
        editBaseUrl.setText(currentUrl);

        if (SharedPrefsManager.QA_URL.equals(currentUrl)) {
            radioQa.setChecked(true);
            editBaseUrl.setEnabled(false);
            currentSelectedId = R.id.radioQa;
        } else if (SharedPrefsManager.PROD_URL.equals(currentUrl)) {
            radioProd.setChecked(true);
            editBaseUrl.setEnabled(false);
            currentSelectedId = R.id.radioProd;
        } else {
            radioCustom.setChecked(true);
            editBaseUrl.setEnabled(true);
            currentSelectedId = R.id.radioCustom;
        }

        radioGroupEnv.setOnCheckedChangeListener(envChangeListener);

        btnSave.setOnClickListener(v -> saveSettings());
        btnRestoreDefault.setOnClickListener(v -> restoreDefaults());
    }

    private final RadioGroup.OnCheckedChangeListener envChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId == R.id.radioQa) {
                editBaseUrl.setText(SharedPrefsManager.QA_URL);
                editBaseUrl.setEnabled(false);
                currentSelectedId = R.id.radioQa;
            } else if (checkedId == R.id.radioProd) {
                editBaseUrl.setText(SharedPrefsManager.PROD_URL);
                editBaseUrl.setEnabled(false);
                currentSelectedId = R.id.radioProd;
            } else if (checkedId == R.id.radioCustom) {
                if (currentSelectedId == R.id.radioCustom) {
                    return;
                }
                showCustomConfirmationDialog();
            }
        }
    };

    private void showCustomConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Are you sure you want to change the API address?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    editBaseUrl.setEnabled(true);
                    currentSelectedId = R.id.radioCustom;
                    editBaseUrl.requestFocus();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    radioGroupEnv.setOnCheckedChangeListener(null);
                    radioGroupEnv.check(currentSelectedId);
                    radioGroupEnv.setOnCheckedChangeListener(envChangeListener);
                })
                .setCancelable(false)
                .show();
    }

    private void saveSettings() {
        String newUrl = editBaseUrl.getText().toString().trim();
        if (!newUrl.isEmpty()) {
            prefsManager.setBaseUrl(newUrl);
            Toast.makeText(this, "API URL Updated!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreDefaults() {
        prefsManager.resetBaseUrl();
        radioGroupEnv.setOnCheckedChangeListener(null);
        radioGroupEnv.check(R.id.radioProd);
        radioGroupEnv.setOnCheckedChangeListener(envChangeListener);
        currentSelectedId = R.id.radioProd;
        editBaseUrl.setText(SharedPrefsManager.PROD_URL);
        editBaseUrl.setEnabled(false);
        Toast.makeText(this, "Default URL Restored!", Toast.LENGTH_SHORT).show();
    }
}
