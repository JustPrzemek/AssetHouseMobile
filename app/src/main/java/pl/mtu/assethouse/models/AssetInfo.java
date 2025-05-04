package pl.mtu.assethouse.models;

public class AssetInfo {
    private final String assetId;
    private final String description;
    private final String locationName;
    private final String systemName;

    public AssetInfo(String assetId, String description, String locationName, String systemName) {
        this.assetId = assetId;
        this.description = description;
        this.locationName = locationName;
        this.systemName = systemName;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getDescription() {
        return description;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getSystemName() {
        return systemName;
    }

    @Override
    public String toString() {
        return "AssetInfo{" +
                "assetId='" + assetId + '\'' +
                ", description='" + description + '\'' +
                ", locationName='" + locationName + '\'' +
                ", systemName='" + systemName + '\'' +
                '}';
    }
}
