package com.tangem.common.ui.tokens

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import kotlin.reflect.KClass

/**
 * The earn badge in the title of a token row comes from `earnBadgeProvider`: the converter knows no earn rule, it only
 * puts the badge it is given into the statuses that carry a balance.
 */
internal class TokenItemStateConverterEarnBadgeTest {

    private val factory = MockCryptoCurrencyFactory()
    private val ethereum = factory.createCoin(Blockchain.Ethereum)
    private val yieldToken = factory.createToken(
        blockchain = Blockchain.Ethereum,
        contractAddress = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
    )

    private val badgeText = stringReference("APY 5.50%")
    private val yieldApy = BigDecimal("5.851295672613467")
    private val yieldApyMap = mapOf(yieldToken.yieldSupplyKey() to yieldApy)

    private val networkAddress = NetworkAddress.Single(
        NetworkAddress.Address("address", NetworkAddress.Address.Type.Primary),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StatusesWithBalance {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a provider badge AND a status with balance WHEN convert THEN the title carries the badge text and activity`(
            model: BadgeModel,
        ) {
            // Arrange
            val converter = converter(earnBadgeProvider = { EarnBadge(text = badgeText, isActive = model.isActive) })

            // Act
            val actual = converter.contentTitle(CryptoCurrencyStatus(currency = ethereum, value = model.value))

            // Assert
            val expected = expectedTitle(ethereum, earnApy = badgeText, isEarnApyActive = model.isActive)
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            BadgeModel(description = "Loaded, active", value = loaded(), isActive = true),
            BadgeModel(description = "Loaded, inactive", value = loaded(), isActive = false),
            BadgeModel(description = "Custom, active", value = custom(), isActive = true),
            BadgeModel(description = "NoQuote, inactive", value = noQuote(), isActive = false),
            BadgeModel(description = "NoAccount, active", value = noAccount(), isActive = true),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WithoutProvider {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN no provider AND a status with balance WHEN convert THEN the title has no badge`(
            model: BalanceStatusModel,
        ) {
            // Arrange
            val converter = converter()

            // Act
            val actual = converter.contentTitle(CryptoCurrencyStatus(currency = ethereum, value = model.value))

            // Assert
            assertThat(actual).isEqualTo(expectedTitle(ethereum))
        }

        private fun provideTestModels() = listOf(
            BalanceStatusModel(description = "Loaded", value = loaded()),
            BalanceStatusModel(description = "Custom", value = custom()),
            BalanceStatusModel(description = "NoQuote", value = noQuote()),
            BalanceStatusModel(description = "NoAccount", value = noAccount()),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StatusesWithoutBalance {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a provider badge AND a status without balance WHEN convert THEN no badge and the provider is not asked`(
            model: NonContentStatusModel,
        ) {
            // Arrange
            val provider: (CryptoCurrency.ID) -> EarnBadge? = mockk()
            every { provider(any()) } returns EarnBadge(text = badgeText, isActive = true)
            val converter = converter(earnBadgeProvider = provider)

            // Act
            val actual = converter.convert(CryptoCurrencyStatus(currency = ethereum, value = model.value))

            // Assert
            assertThat(actual).isInstanceOf(model.expectedState.java)
            assertThat(actual.titleState).isEqualTo(expectedTitle(ethereum))
            verify(exactly = 0) { provider(any()) }
        }

        private fun provideTestModels() = listOf(
            NonContentStatusModel(
                description = "Loading",
                value = CryptoCurrencyStatus.Loading,
                expectedState = TokenItemState.Loading::class,
            ),
            NonContentStatusModel(
                description = "Unreachable",
                value = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
                expectedState = TokenItemState.Unreachable::class,
            ),
            NonContentStatusModel(
                description = "NoAmount",
                value = CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null),
                expectedState = TokenItemState.Unreachable::class,
            ),
            NonContentStatusModel(
                description = "MissedDerivation",
                value = CryptoCurrencyStatus.MissedDerivation(priceChange = null, fiatRate = null),
                expectedState = TokenItemState.NoAddress::class,
            ),
        )
    }

    @Nested
    inner class ProviderArgument {

        @Test
        fun `GIVEN a currency WHEN convert THEN the provider is asked once with the currency id`() {
            // Arrange
            val asked = mutableListOf<CryptoCurrency.ID>()
            val converter = converter(
                earnBadgeProvider = { id ->
                    asked += id
                    null
                },
            )

            // Act
            converter.convert(CryptoCurrencyStatus(currency = ethereum, value = loaded()))

            // Assert
            assertThat(asked).containsExactly(ethereum.id)
        }
    }

    @Nested
    inner class YieldPromo {

        private val status = CryptoCurrencyStatus(currency = yieldToken, value = loaded())

        @Test
        fun `GIVEN the yield map AND the promo currency WHEN convert THEN the promo banner carries the yield rate`() {
            // Arrange
            val converter = promoConverter()

            // Act
            val actual = (converter.convert(status) as TokenItemState.Content).promoBannerState

            // Assert
            val expectedTitle = resourceReference(
                R.string.yield_module_main_screen_promo_banner_message,
                wrappedList(yieldApy),
            )
            assertThat(actual).isInstanceOf(TokenItemState.PromoBannerState.Content::class.java)
            assertThat((actual as TokenItemState.PromoBannerState.Content).title).isEqualTo(expectedTitle)
        }

        @Test
        fun `GIVEN the promo banner WHEN it is clicked THEN the yield percent of the token is passed`() {
            // Arrange
            val clicks = mutableListOf<Pair<CryptoCurrencyStatus, String>>()
            val converter = promoConverter(onYieldPromoClicked = { clicked, apy -> clicks += clicked to apy })
            val banner = (converter.convert(status) as TokenItemState.Content).promoBannerState
                as TokenItemState.PromoBannerState.Content

            // Act
            banner.onPromoBannerClick()

            // Assert
            assertThat(clicks).containsExactly(status to yieldApy.toString())
        }

        private fun promoConverter(onYieldPromoClicked: ((CryptoCurrencyStatus, String) -> Unit)? = null) =
            TokenItemStateConverter(
                appCurrency = AppCurrency.Default,
                yieldModuleApyMap = yieldApyMap,
                promoCryptoCurrencyStatus = status,
                onYieldPromoClicked = onYieldPromoClicked,
                iconStateProvider = { CurrencyIconState.Loading },
                subtitleStateProvider = { TokenItemState.SubtitleState.Loading },
                subtitle2StateProvider = { TokenItemState.Subtitle2State.Loading },
                fiatAmountStateProvider = { TokenItemState.FiatAmountState.Loading },
            )
    }

    private fun converter(earnBadgeProvider: (CryptoCurrency.ID) -> EarnBadge? = { null }) = TokenItemStateConverter(
        appCurrency = AppCurrency.Default,
        earnBadgeProvider = earnBadgeProvider,
        iconStateProvider = { CurrencyIconState.Loading },
        subtitleStateProvider = { TokenItemState.SubtitleState.Loading },
        subtitle2StateProvider = { TokenItemState.Subtitle2State.Loading },
        fiatAmountStateProvider = { TokenItemState.FiatAmountState.Loading },
        promoBannerProvider = { TokenItemState.PromoBannerState.Empty },
    )

    private fun TokenItemStateConverter.contentTitle(status: CryptoCurrencyStatus): TokenItemState.TitleState {
        return (convert(status) as TokenItemState.Content).titleState as TokenItemState.TitleState.Content
    }

    private fun expectedTitle(
        currency: CryptoCurrency,
        earnApy: TextReference? = null,
        isEarnApyActive: Boolean = false,
    ) = TokenItemState.TitleState.Content(
        text = stringReference(currency.name),
        earnApy = earnApy,
        isEarnApyActive = isEarnApyActive,
    )

    private fun loaded() = CryptoCurrencyStatus.Loaded(
        amount = BigDecimal.ONE,
        fiatAmount = BigDecimal.ONE,
        fiatRate = BigDecimal.ONE,
        priceChange = BigDecimal.ZERO,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = networkAddress,
        sources = CryptoCurrencyStatus.Sources(),
        contributions = emptyList(),
    )

    private fun custom() = CryptoCurrencyStatus.Custom(
        amount = BigDecimal.ONE,
        fiatAmount = BigDecimal.ONE,
        fiatRate = BigDecimal.ONE,
        priceChange = BigDecimal.ZERO,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = networkAddress,
        sources = CryptoCurrencyStatus.Sources(),
        contributions = emptyList(),
    )

    private fun noQuote() = CryptoCurrencyStatus.NoQuote(
        amount = BigDecimal.ONE,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = networkAddress,
        sources = CryptoCurrencyStatus.Sources(),
        contributions = emptyList(),
    )

    private fun noAccount() = CryptoCurrencyStatus.NoAccount(
        amountToCreateAccount = BigDecimal.ONE,
        fiatAmount = BigDecimal.ONE,
        priceChange = BigDecimal.ZERO,
        fiatRate = BigDecimal.ONE,
        networkAddress = networkAddress,
        sources = CryptoCurrencyStatus.Sources(),
    )

    internal data class BadgeModel(
        val description: String,
        val value: CryptoCurrencyStatus.Value,
        val isActive: Boolean,
    ) {
        override fun toString(): String = description
    }

    internal data class BalanceStatusModel(val description: String, val value: CryptoCurrencyStatus.Value) {
        override fun toString(): String = description
    }

    internal data class NonContentStatusModel(
        val description: String,
        val value: CryptoCurrencyStatus.Value,
        val expectedState: KClass<out TokenItemState>,
    ) {
        override fun toString(): String = description
    }
}