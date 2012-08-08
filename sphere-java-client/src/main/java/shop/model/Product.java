package de.commercetools.sphere.client.shop.model;

import static de.commercetools.sphere.client.util.Ext.*;

import java.util.ArrayList;
import java.util.List;

/**
 *  Product in the product catalog.
 *
 *  The Product itself is a {@link Variant}. Products that only exist in one variant
 *  are represented by a single Product instance where {@link #getVariants()} is empty.
 *  */
public class Product extends Variant {
    private String id;
    private String version;
    private String name;
    private String productType; // TODO Reference<ProductType>
    private String description;
    private String vendor;      // TODO Reference<Vendor>
    private String slug;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private int quantityAtHand;
    private List<String> categories = new ArrayList<String>();  // TODO Reference<Category>
    private List<Variant> variants = new ArrayList<Variant>();

    // for JSON deserializer
    private Product() { }

    /** Returns the variant with given SKU, or null if such variant does not exist. */
    public Variant getVariantBySKU(String sku) {
        for (Variant v: variants) {
            if (v.getSKU().equals(sku)) return v;
        }
        return null;
    }

    /** Id of this product. */
    public String getID() {
        return id;
    }
    /** Version of this product that increases when the product is changed. */
    public String getVersion() {
        return version;
    }
    /** Name of this product. */
    public String getName() {
        return name;
    }
    /** Type of this product. */
    public String getProductType() {
        return productType;
    }
    /** Description of this product. */
    public String getDescription() {
        return description;
    }
    /** Vendor of this product.  */
    public String getVendor() {
        return vendor;
    }
    /** URL friendly name of this product. */
    public String getSlug() {
        return slug;
    }
    /** HTML title for product page. */
    public String getMetaTitle() {
        return metaTitle;
    }
    /** HTML meta description for product page. */
    public String getMetaDescription() {
        return metaDescription;
    }
    /** HTML meta keywords for product page. */
    public String getMetaKeywords() {
        return metaKeywords;
    }
    /** Current available stock quantity for this product. */
    public int getQuantityAtHand() {
        return quantityAtHand;
    }
    /** Categories this product is assigned to. */
    public List<String> getCategories() {
        return categories;
    }
    /** Variants of this product. */
    public List<Variant> getVariants() {
        return variants;
    }
}
