package io.sphere.sdk.taxcategories;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.models.Base;

import javax.annotation.Nullable;
import java.util.List;

final class ExternalTaxRateDraftImpl extends Base implements ExternalTaxRateDraft {
    private final String name;
    @Nullable
    private final Double amount;
    private final CountryCode country;
    @Nullable
    private final String state;
    @Nullable
    private final List<SubRate> subRates;

    public ExternalTaxRateDraftImpl(final Double amount, final String name, final CountryCode country, final String state, final List<SubRate> subRates) {
        this.amount = amount;
        this.name = name;
        this.country = country;
        this.state = state;
        this.subRates = subRates;
    }

    @Override
    @Nullable
    public Double getAmount() {
        return amount;
    }

    @Override
    public CountryCode getCountry() {
        return country;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @Nullable
    public String getState() {
        return state;
    }

    @Override
    @Nullable
    public List<SubRate> getSubRates() {
        return subRates;
    }
}
