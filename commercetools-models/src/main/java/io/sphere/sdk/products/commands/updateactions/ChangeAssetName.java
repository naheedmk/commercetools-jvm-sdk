package io.sphere.sdk.products.commands.updateactions;

import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;

import javax.annotation.Nullable;

/**
 * Changes the name of an asset.
 *
 * {@doc.gen intro}
 *
 * <p>By variant ID (every variant has a variantId):</p>
 * {@include.example io.sphere.sdk.products.commands.ProductUpdateCommandIntegrationTest#changeAssetNameByVariantId()}
 *
 * <p>By SKU (attention, SKU is optional field in a variant):</p>
 * {@include.example io.sphere.sdk.products.commands.ProductUpdateCommandIntegrationTest#changeAssetNameBySku()}
 */
public final class ChangeAssetName extends UpdateActionImpl<Product> {
    @Nullable
    private final Integer variantId;
    @Nullable
    private final String sku;
    private final String assetId;
    private final LocalizedString name;

    private ChangeAssetName(final String assetId, @Nullable final Integer variantId, @Nullable final String sku, final LocalizedString name) {
        super("changeAssetName");
        this.assetId = assetId;
        this.variantId = variantId;
        this.sku = sku;
        this.name = name;
    }

    public String getAssetId() {
        return assetId;
    }

    @Nullable
    public Integer getVariantId() {
        return variantId;
    }

    @Nullable
    public String getSku() {
        return sku;
    }

    public LocalizedString getName() {
        return name;
    }

    public static ChangeAssetName ofVariantId(final Integer variantId, final String assetId, final LocalizedString name) {
        return new ChangeAssetName(assetId, variantId, null, name);
    }

    public static ChangeAssetName ofSku(final String sku, final String assetId, final LocalizedString name) {
        return new ChangeAssetName(assetId, null, sku, name);
    }
}
