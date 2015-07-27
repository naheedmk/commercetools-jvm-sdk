package io.sphere.sdk.messages;

import com.fasterxml.jackson.core.type.TypeReference;
import io.sphere.sdk.messages.queries.MessageQueryModel;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;

import java.util.function.Supplier;

public class MessageDerivatHint<T> {
    private final TypeReference<PagedQueryResult<T>> queryResultTypeReference;
    private final TypeReference<T> elementTypeReference;
    private final Supplier<QueryPredicate<Message>> predicateSupplier;

    private MessageDerivatHint(final TypeReference<PagedQueryResult<T>> resultTypeReference,
                               final TypeReference<T> elementTypeReference,
                               final Supplier<QueryPredicate<Message>> predicateSupplier) {
        this.queryResultTypeReference = resultTypeReference;
        this.predicateSupplier = predicateSupplier;
        this.elementTypeReference = elementTypeReference;
    }

    public TypeReference<PagedQueryResult<T>> queryResultTypeReference() {
        return queryResultTypeReference;
    }

    public TypeReference<T> elementTypeReference() {
        return elementTypeReference;
    }

    public QueryPredicate<Message> predicate() {
        return predicateSupplier.get();
    }

    public static <T> MessageDerivatHint<T> ofSingleMessageType(final String type, final TypeReference<PagedQueryResult<T>> queryResultTypeReference, final TypeReference<T> elementTypeReference) {
        return new MessageDerivatHint<>(queryResultTypeReference, elementTypeReference, () -> MessageQueryModel.of().type().is(type));
    }

    public static <T> MessageDerivatHint<T> ofResourceType(final String resourceId, final TypeReference<PagedQueryResult<T>> queryResultTypeReference, final TypeReference<T> elementTypeReference) {
        return new MessageDerivatHint<>(queryResultTypeReference, elementTypeReference, () -> MessageQueryModel.of().resource().typeId().is(resourceId));
    }
}