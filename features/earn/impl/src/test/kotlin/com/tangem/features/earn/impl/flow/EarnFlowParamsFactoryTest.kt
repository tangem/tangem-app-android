package com.tangem.features.earn.impl.flow

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.EarnEntryComponent
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.api.EarnEntryStart
import com.tangem.features.earn.flow.api.EarnFlowComponent
import com.tangem.features.earn.flow.api.EarnFlowProduct
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.createFakeEarnProduct
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.product.api.navigation.EarnFlowRoute
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnLegal
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.Withdraw
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class EarnFlowParamsFactoryTest {

    private val lending = createFakeEarnProduct(FakeEarnProductPreset.Lending)
    private val staking = createFakeEarnProduct(FakeEarnProductPreset.Staking)
    private val registry = EarnProductsRegistry(products = setOf(lending, staking), rateSources = emptySet())
    private val factory = EarnFlowParamsFactory(registry)

    private val entry = EarnEntryComponent.Params(
        userWalletId = UserWalletId("0123456789abcdef"),
        cryptoCurrency = MockCryptoCurrencyFactory().ethereum,
        source = EarnEntrySource.Markets,
        start = EarnEntryStart.Picker(preselected = null),
    )

    private val callback: EarnFlowComponent.ModelCallback = mockk()

    @Test
    fun `GIVEN route of a known product WHEN create THEN params carry the entry, the product and the route action`() {
        // Arrange
        val route = EarnFlowRoute(productId = lending.id, action = Withdraw())

        // Act
        val actual = factory.create(route = route, entry = entry, callback = callback)

        // Assert
        val expected = EarnFlowComponent.Params(
            product = EarnProduct.Params(userWalletId = entry.userWalletId, currency = entry.cryptoCurrency),
            flowProduct = EarnFlowProduct(
                productId = lending.id,
                title = lending.title,
                provider = lending.provider,
                analytics = lending.analytics,
                howItWorks = lending.howItWorks,
            ),
            action = Withdraw(),
            source = EarnEntrySource.Markets,
            callback = callback,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN product without how it works and legal links WHEN create THEN both are absent`() {
        // Arrange
        val route = EarnFlowRoute(productId = staking.id, action = Deposit())

        // Act
        val actual = factory.create(route = route, entry = entry, callback = callback)

        // Assert
        assertThat(actual?.flowProduct?.howItWorks).isNull()
        assertThat(actual?.flowProduct?.legal).isNull()
    }

    @Test
    fun `GIVEN product with legal links WHEN create THEN the flow gets them`() {
        // Arrange
        val withLegal = object : EarnProduct by lending {
            override val legal = EarnLegal(termsOfUseUrl = "https://terms", privacyPolicyUrl = "https://privacy")
        }
        val factory = EarnFlowParamsFactory(
            EarnProductsRegistry(products = setOf(withLegal, staking), rateSources = emptySet()),
        )
        val route = EarnFlowRoute(productId = lending.id, action = Deposit())

        // Act
        val actual = factory.create(route = route, entry = entry, callback = callback)

        // Assert
        assertThat(actual?.flowProduct?.legal)
            .isEqualTo(EarnLegal(termsOfUseUrl = "https://terms", privacyPolicyUrl = "https://privacy"))
    }

    @Test
    fun `GIVEN product with how it works WHEN create THEN the flow gets it`() {
        // Arrange
        val howItWorks = EarnHowItWorks(title = stringReference("How it works"), entries = emptyList())
        val withHowItWorks = object : EarnProduct by lending {
            override val howItWorks = howItWorks
        }
        val factory = EarnFlowParamsFactory(
            EarnProductsRegistry(products = setOf(withHowItWorks, staking), rateSources = emptySet()),
        )
        val route = EarnFlowRoute(productId = lending.id, action = Deposit())

        // Act
        val actual = factory.create(route = route, entry = entry, callback = callback)

        // Assert
        assertThat(actual?.flowProduct?.howItWorks).isEqualTo(howItWorks)
    }

    @Test
    fun `GIVEN route of an unknown product WHEN create THEN null is returned`() {
        // Arrange
        val route = EarnFlowRoute(productId = EarnProductId("unknown"), action = Deposit())

        // Act
        val actual = factory.create(route = route, entry = entry, callback = callback)

        // Assert
        assertThat(actual).isNull()
    }
}