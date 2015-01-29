package io.sphere.sdk.products.commands;

import io.sphere.sdk.attributes.AttributeAccess;
import io.sphere.sdk.attributes.AttributeGetterSetter;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Image;
import io.sphere.sdk.models.LocalizedEnumValue;
import io.sphere.sdk.models.LocalizedStrings;
import io.sphere.sdk.models.MetaAttributes;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.commands.updateactions.*;
import io.sphere.sdk.suppliers.TShirtProductTypeDraftSupplier;
import io.sphere.sdk.test.IntegrationTest;
import io.sphere.sdk.test.OptionalAssert;
import io.sphere.sdk.test.ReferenceAssert;
import io.sphere.sdk.utils.MoneyImpl;
import org.junit.Test;

import javax.money.MonetaryAmount;
import java.util.Random;

import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static io.sphere.sdk.models.LocalizedStrings.ofEnglishLocale;
import static io.sphere.sdk.products.ProductUpdateScope.STAGED_AND_CURRENT;
import static java.util.Locale.ENGLISH;
import static org.fest.assertions.Assertions.assertThat;
import static io.sphere.sdk.test.SphereTestUtils.*;
import static io.sphere.sdk.products.ProductFixtures.*;

public class ProductUpdateCommandTest extends IntegrationTest {
    public static final Random RANDOM = new Random();

    @Test
    public void addExternalImage() throws Exception {
        withUpdateableProduct(client(), product -> {
            assertThat(product.getMasterData().getStaged().getMasterVariant().getImages()).hasSize(0);

            final Image image = Image.ofWidthAndHeight("http://www.commercetools.com/assets/img/ct_logo_farbe.gif", 460, 102, "commercetools logo");
            final Product updatedProduct = execute(ProductUpdateCommand.of(product, AddExternalImage.of(image, MASTER_VARIANT_ID, STAGED_AND_CURRENT)));

            assertThat(updatedProduct.getMasterData().getStaged().getMasterVariant().getImages()).containsExactly(image);
            return updatedProduct;
        });
    }

    @Test
    public void addPrice() throws Exception {
        withUpdateableProduct(client(), product -> {
            final Price expectedPrice = Price.of(MoneyImpl.of(123, EUR));
            final Product updatedProduct = client()
                    .execute(ProductUpdateCommand.of(product, AddPrice.of(1, expectedPrice, STAGED_AND_CURRENT)));

            final Price actualPrice = updatedProduct.getMasterData().getStaged().getMasterVariant().getPrices().get(0);
            assertThat(actualPrice).isEqualTo(expectedPrice);
            return updatedProduct;
        });
    }

    @Test
    public void addToCategory() throws Exception {
        withProductAndCategory(client(), (final Product product, final Category category) -> {
            assertThat(product.getMasterData().getStaged().getCategories()).isEmpty();

            final Product updatedProduct = client()
                    .execute(ProductUpdateCommand.of(product, AddToCategory.of(category, STAGED_AND_CURRENT)));

            ReferenceAssert.assertThat(updatedProduct.getMasterData().getStaged().getCategories().get(0)).references(category);
        });
    }

    @Test
    public void changeName() throws Exception {
        withUpdateableProduct(client(), product -> {
            final LocalizedStrings newName = ofEnglishLocale("newName " + RANDOM.nextInt());
            final Product updatedProduct = execute(ProductUpdateCommand.of(product, ChangeName.of(newName, STAGED_AND_CURRENT)));

            assertThat(updatedProduct.getMasterData().getStaged().getName()).isEqualTo(newName);
            return updatedProduct;
        });
    }

    @Test
    public void changePrice() throws Exception {
        withUpdateablePricedProduct(client(), product -> {
            final Price newPrice = Price.of(MoneyImpl.of(234, EUR));
            assertThat(product.getMasterData().getStaged().getMasterVariant()
                    .getPrices().stream().anyMatch(p -> p.equals(newPrice))).isFalse();

            final Product updatedProduct = client()
                    .execute(ProductUpdateCommand.of(product, ChangePrice.of(MASTER_VARIANT_ID, newPrice, STAGED_AND_CURRENT)));

            final Price actualPrice = updatedProduct.getMasterData().getStaged().getMasterVariant().getPrices().get(0);
            assertThat(actualPrice).isEqualTo(newPrice);

            return updatedProduct;
        });
    }

    @Test
    public void changeSlug() throws Exception {
        withUpdateableProduct(client(), product -> {
            final LocalizedStrings newSlug = ofEnglishLocale("new-slug-" + RANDOM.nextInt());
            final Product updatedProduct = execute(ProductUpdateCommand.of(product, ChangeSlug.of(newSlug, STAGED_AND_CURRENT)));

            assertThat(updatedProduct.getMasterData().getStaged().getSlug()).isEqualTo(newSlug);
            return updatedProduct;
        });
    }

    @Test
    public void publish() throws Exception {
        withUpdateableProduct(client(), product -> {
            assertThat(product.getMasterData().isPublished()).isFalse();

            final Product publishedProduct = execute(ProductUpdateCommand.of(product, Publish.of()));
            assertThat(publishedProduct.getMasterData().isPublished()).isTrue();

            final Product unpublishedProduct = execute(ProductUpdateCommand.of(publishedProduct, Unpublish.of()));
            assertThat(unpublishedProduct.getMasterData().isPublished()).isFalse();
            return unpublishedProduct;
        });
    }

    @Test
    public void removeImage() throws Exception {
        final Image image = Image.ofWidthAndHeight("http://www.commercetools.com/assets/img/ct_logo_farbe.gif", 460, 102, "commercetools logo");
        withUpdateableProduct(client(), product -> {
            final Product productWithImage = client().execute(ProductUpdateCommand.of(product, AddExternalImage.of(image, MASTER_VARIANT_ID, STAGED_AND_CURRENT)));
            assertThat(productWithImage.getMasterData().getStaged().getMasterVariant().getImages()).containsExactly(image);

            final Product updatedProduct = execute(ProductUpdateCommand.of(productWithImage, RemoveImage.of(image, MASTER_VARIANT_ID, STAGED_AND_CURRENT)));
            assertThat(updatedProduct.getMasterData().getStaged().getMasterVariant().getImages()).hasSize(0);
            return updatedProduct;
        });
    }

    @Test
    public void removePrice() throws Exception {
        withUpdateablePricedProduct(client(), product -> {
            final Price oldPrice = product.getMasterData().getStaged().getMasterVariant().getPrices().get(0);

            final Product updatedProduct = client()
                    .execute(ProductUpdateCommand.of(product, RemovePrice.of(MASTER_VARIANT_ID, oldPrice, STAGED_AND_CURRENT)));

            assertThat(updatedProduct.getMasterData().getStaged().getMasterVariant()
                    .getPrices().stream().anyMatch(p -> p.equals(oldPrice))).isFalse();

            return updatedProduct;
        });
    }

    @Test
    public void setDescription() throws Exception {
        withUpdateableProduct(client(), product -> {
            final LocalizedStrings newDescription = ofEnglishLocale("new description " + RANDOM.nextInt());
            final ProductUpdateCommand cmd = ProductUpdateCommand.of(product, SetDescription.of(newDescription, STAGED_AND_CURRENT));
            final Product updatedProduct = execute(cmd);

            OptionalAssert.assertThat(updatedProduct.getMasterData().getStaged().getDescription()).isPresentAs(newDescription);
            return updatedProduct;
        });
    }

    @Test
    public void setMetaAttributes() throws Exception {
        withUpdateableProduct(client(), product -> {
            final MetaAttributes metaAttributes = MetaAttributes.metaAttributesOf(ENGLISH,
                    "commercetools SPHERE.IO&#8482; - Next generation eCommerce",
                    "SPHERE.IO&#8482; is the first and leading Platform-as-a-Service solution for eCommerce.",
                    "Platform-as-a-Service, e-commerce, http, api, tool");
            final Product updatedProduct = client()
                    .execute(ProductUpdateCommand.of(product, SetMetaAttributes.of(metaAttributes, STAGED_AND_CURRENT)));

            final ProductData productData = updatedProduct.getMasterData().getStaged();
            OptionalAssert.assertThat(productData.getMetaTitle()).isEqualTo(metaAttributes.getMetaTitle());
            OptionalAssert.assertThat(productData.getMetaDescription()).isEqualTo(metaAttributes.getMetaDescription());
            OptionalAssert.assertThat(productData.getMetaKeywords()).isEqualTo(metaAttributes.getMetaKeywords());
            return updatedProduct;
        });
    }

    @Test
    public void setAttribute() throws Exception {
        withUpdateableProduct(client(), product -> {
            //the setter contains the name and a JSON mapper, declare it only one time in your project per attribute
            //example for MonetaryAmount attribute
            final String moneyAttributeName = TShirtProductTypeDraftSupplier.MONEY_ATTRIBUTE_NAME;
            final AttributeGetterSetter<Product, MonetaryAmount> moneyAttribute =
                    AttributeAccess.ofMoney().ofName(moneyAttributeName);
            final MonetaryAmount newValueForMoney = EURO_10;

            //example for LocalizedEnumValue attribute
            final AttributeGetterSetter<Product, LocalizedEnumValue> colorAttribute = TShirtProductTypeDraftSupplier.Colors.ATTRIBUTE;
            final LocalizedEnumValue oldValueForColor = TShirtProductTypeDraftSupplier.Colors.GREEN;
            final LocalizedEnumValue newValueForColor = TShirtProductTypeDraftSupplier.Colors.RED;

            OptionalAssert.assertThat(product.getMasterData().getStaged().getMasterVariant().getAttribute(moneyAttribute)).isAbsent();
            OptionalAssert.assertThat(product.getMasterData().getStaged().getMasterVariant().getAttribute(colorAttribute)).isPresentAs(oldValueForColor);

            final SetAttribute moneyUpdate = SetAttribute.of(MASTER_VARIANT_ID, moneyAttribute, newValueForMoney, STAGED_AND_CURRENT);
            final SetAttribute localizedEnumUpdate = SetAttribute.of(MASTER_VARIANT_ID, colorAttribute, newValueForColor, STAGED_AND_CURRENT);

            final Product updatedProduct = client().execute(ProductUpdateCommand.of(product, asList(moneyUpdate, localizedEnumUpdate)));
            OptionalAssert.assertThat(updatedProduct.getMasterData().getStaged().getMasterVariant().getAttribute(moneyAttribute)).isPresentAs(newValueForMoney);
            OptionalAssert.assertThat(updatedProduct.getMasterData().getStaged().getMasterVariant().getAttribute(colorAttribute)).isPresentAs(newValueForColor);

            final SetAttribute unsetAction = SetAttribute.ofUnsetAttribute(MASTER_VARIANT_ID, moneyAttribute, STAGED_AND_CURRENT);
            final Product productWithoutMoney = client().execute(ProductUpdateCommand.of(updatedProduct, unsetAction));

            OptionalAssert.assertThat(productWithoutMoney.getMasterData().getStaged().getMasterVariant().getAttribute(moneyAttribute)).isAbsent();

            return productWithoutMoney;
        });
    }

    @Test
    public void setAttributeInAllVariants() throws Exception {
        withUpdateableProduct(client(), product -> {
            //the setter contains the name and a JSON mapper, declare it only one time in your project per attribute
            //example for MonetaryAmount attribute
            final String moneyAttributeName = TShirtProductTypeDraftSupplier.MONEY_ATTRIBUTE_NAME;
            final AttributeGetterSetter<Product, MonetaryAmount> moneyAttribute =
                    AttributeAccess.ofMoney().ofName(moneyAttributeName);
            final MonetaryAmount newValueForMoney = EURO_10;

            //example for LocalizedEnumValue attribute
            final AttributeGetterSetter<Product, LocalizedEnumValue> colorAttribute = TShirtProductTypeDraftSupplier.Colors.ATTRIBUTE;
            final LocalizedEnumValue oldValueForColor = TShirtProductTypeDraftSupplier.Colors.GREEN;
            final LocalizedEnumValue newValueForColor = TShirtProductTypeDraftSupplier.Colors.RED;

            OptionalAssert.assertThat(product.getMasterData().getStaged().getMasterVariant().getAttribute(moneyAttribute)).isAbsent();
            OptionalAssert.assertThat(product.getMasterData().getStaged().getMasterVariant().getAttribute(colorAttribute)).isPresentAs(oldValueForColor);

            final SetAttributeInAllVariants moneyUpdate = SetAttributeInAllVariants.of(moneyAttribute, newValueForMoney, STAGED_AND_CURRENT);
            final SetAttributeInAllVariants localizedEnumUpdate = SetAttributeInAllVariants.of(colorAttribute, newValueForColor, STAGED_AND_CURRENT);

            final Product updatedProduct = client().execute(ProductUpdateCommand.of(product, asList(moneyUpdate, localizedEnumUpdate)));
            OptionalAssert.assertThat(updatedProduct.getMasterData().getStaged().getMasterVariant().getAttribute(moneyAttribute)).isPresentAs(newValueForMoney);
            OptionalAssert.assertThat(updatedProduct.getMasterData().getStaged().getMasterVariant().getAttribute(colorAttribute)).isPresentAs(newValueForColor);

            final SetAttributeInAllVariants unsetAction = SetAttributeInAllVariants.ofUnsetAttribute(moneyAttribute, STAGED_AND_CURRENT);
            final Product productWithoutMoney = client().execute(ProductUpdateCommand.of(updatedProduct, unsetAction));

            OptionalAssert.assertThat(productWithoutMoney.getMasterData().getStaged().getMasterVariant().getAttribute(moneyAttribute)).isAbsent();

            return productWithoutMoney;
        });
    }
}