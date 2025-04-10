package com.zebra.rfid.assethouse.models;

import org.json.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
    private String assetId;
    private String description;
    private String status;
    private String expectedLocation;
    private String systemName;

    public Asset(JSONObject json) {
        this.assetId = json.optString("assetId", "");
        this.description = json.optString("description", "No Description");

        String inventoryStatus = json.optString("inventoryStatus", "UNSCANNED").toUpperCase();
        switch(inventoryStatus) {
            case "OK":
            case "SCANNED":
                this.status = "OK";
                break;
            case "MISSING":
            case "NOT_FOUND":
                this.status = "MISSING";
                break;
            case "NEW":
            case "NEW_ASSET":
                this.status = "NEW";
                break;
            default:
                this.status = inventoryStatus;
        }

        this.expectedLocation = json.optString("expectedLocation", "No location");
        this.systemName = json.optString("systemName", "No system");
    }

    // Getters
    public String getAssetId() { return assetId; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getExpectedLocation() { return expectedLocation; }
    public String getSystemName() { return systemName; }

    public boolean isNew() { return "NEW".equals(status); }
    public boolean isMissing() { return "MISSING".equals(status); }
    public boolean isOk() { return "OK".equals(status); }
    public boolean isUnscanned() { return "UNSCANNED".equals(status); }
}