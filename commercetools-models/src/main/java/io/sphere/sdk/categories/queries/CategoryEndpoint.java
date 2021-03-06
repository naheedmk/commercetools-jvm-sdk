package io.sphere.sdk.categories.queries;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.JsonEndpoint;

final class CategoryEndpoint {
    public static final JsonEndpoint<Category> ENDPOINT = JsonEndpoint.of(Category.typeReference(), "/categories");
}
