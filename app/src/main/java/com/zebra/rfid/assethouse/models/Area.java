package com.zebra.rfid.assethouse.models;

import org.json.JSONObject;

public class Area {
    private String location;
    private String scannedDate;
    private int count;
    private boolean scanned;

    public Area(JSONObject json) {
        this.location = json.optString("location", "Unknown Location");
        this.scannedDate = json.optString("scannedDate", "");
        this.count = json.optInt("count", 0);

        this.scanned = !scannedDate.isEmpty() && !scannedDate.equalsIgnoreCase("null");
    }

    public String getLocation() { return location; }
    public String getScannedDate() { return scannedDate; }
    public int getCount() { return count; }

    public boolean isScanned() {
        return !scannedDate.isEmpty() && !scannedDate.equalsIgnoreCase("null");
    }
}