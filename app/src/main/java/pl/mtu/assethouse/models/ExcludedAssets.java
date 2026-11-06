package pl.mtu.assethouse.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// The backend keeps two kinds of row here: plain exclusions (deviceId null) and registered
// scanners, which are excluded so they do not scan themselves.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcludedAssets {

    private String assetId;
    private String description;
    private String deviceId;
    private String friendlyName;
    private String iconName;

    public ExcludedAssets(String assetId, String description) {
        this(assetId, description, null, null, null);
    }

    public boolean isScanner() {
        return deviceId != null && !deviceId.trim().isEmpty();
    }

    public String getDisplayName() {
        return friendlyName != null && !friendlyName.trim().isEmpty() ? friendlyName : assetId;
    }
}
