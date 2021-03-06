package io.sphere.sdk.products.commands.updateactions;

import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.search.SearchKeywords;

/**
 * Sets the search keywords for a product.
 *
 * {@doc.gen intro}
 *
 * {@include.example io.sphere.sdk.products.commands.ProductUpdateCommandIntegrationTest#setSearchKeywords()}
 */
public final class SetSearchKeywords extends UpdateActionImpl<Product> {
    private final SearchKeywords searchKeywords;

    private SetSearchKeywords(final SearchKeywords searchKeywords) {
        super("setSearchKeywords");
        this.searchKeywords = searchKeywords;
    }

    public SearchKeywords getSearchKeywords() {
        return searchKeywords;
    }

    public static SetSearchKeywords of(final SearchKeywords searchKeywords) {
        return new SetSearchKeywords(searchKeywords);
    }
}
