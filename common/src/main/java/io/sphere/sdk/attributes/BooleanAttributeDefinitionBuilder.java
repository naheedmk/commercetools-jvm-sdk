package io.sphere.sdk.attributes;

import io.sphere.sdk.models.LocalizedString;

public class BooleanAttributeDefinitionBuilder extends BaseBuilder<BooleanAttributeDefinition, BooleanAttributeDefinitionBuilder> {
    BooleanAttributeDefinitionBuilder(final String name, final LocalizedString label) {
        super(name, label);
    }

    @Override
    protected BooleanAttributeDefinitionBuilder getThis() {
        return this;
    }

    @Override
    public BooleanAttributeDefinition build() {
        return new BooleanAttributeDefinition(new BooleanType(), getName(), getLabel(), isRequired(), getAttributeConstraint(), isSearchable());
    }

    public static BooleanAttributeDefinitionBuilder of(final String name, final LocalizedString label) {
        return new BooleanAttributeDefinitionBuilder(name, label);
    }
}