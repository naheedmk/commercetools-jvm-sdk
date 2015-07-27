package io.sphere.sdk.orders.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.sphere.sdk.messages.GenericMessageImpl;
import io.sphere.sdk.messages.MessageDerivatHint;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.queries.PagedQueryResult;

import java.time.ZonedDateTime;

@JsonDeserialize(as = OrderCreatedMessage.class)//important to override annotation in Message class
public class OrderCreatedMessage extends GenericMessageImpl<Order> {
    public static final String MESSAGE_TYPE = "OrderCreated";
    public static final MessageDerivatHint<OrderCreatedMessage> MESSAGE_HINT =
            MessageDerivatHint.ofSingleMessageType(MESSAGE_TYPE,
                    new TypeReference<PagedQueryResult<OrderCreatedMessage>>() {
                    },
                    new TypeReference<OrderCreatedMessage>() {
                    }
            );

    private final Order order;

    @JsonCreator
    private OrderCreatedMessage(final String id, final long version, final ZonedDateTime createdAt, final ZonedDateTime lastModifiedAt, final JsonNode resource, final long sequenceNumber, final long resourceVersion, final String type, final Order order) {
        super(id, version, createdAt, lastModifiedAt, resource, sequenceNumber, resourceVersion, type);
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}
