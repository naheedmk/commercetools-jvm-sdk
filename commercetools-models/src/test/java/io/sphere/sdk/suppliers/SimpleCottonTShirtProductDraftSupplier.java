package io.sphere.sdk.suppliers;

import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.Referenceable;
import io.sphere.sdk.products.*;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.suppliers.TShirtProductTypeDraftSupplier.*;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

public class SimpleCottonTShirtProductDraftSupplier implements Supplier<ProductDraft> {
    private final Reference<ProductType> productType;
    private final String name;

    public SimpleCottonTShirtProductDraftSupplier(final Referenceable<ProductType> productType, final String name) {
        this.productType = productType.toReference();
        this.name = name;
    }

    @Override
    public ProductDraft get() {
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder.of()
                .plusAttribute(Sizes.ATTRIBUTE.draftOf(Sizes.S))
                .plusAttribute(Colors.ATTRIBUTE.draftOf(Colors.GREEN))
                .sku(UUID.randomUUID().toString())
                .build();
        final LocalizedString slug = en(name).slugifiedUnique();
        return ProductDraftBuilder.of(productType, en(name), slug, masterVariant)
                .description(en(name))
                .metaTitle(en("cotton t-shirt"))
                .metaDescription(en("cotton t-shirt description"))
                .metaKeywords(en("cotton, t-shirt, clothes"))
                .build();
    }

    private LocalizedString en(final String value) {
        return LocalizedString.of(Locale.ENGLISH, value);
    }
}
