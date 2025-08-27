package pl.mtu.assethouse.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcludedAssets {

    private String assetId;
    private String description;
}
