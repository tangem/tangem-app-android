package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeShape
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.badge.TangemBadgeType
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account.Personal.Companion.createMainAccount
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.WalletContentClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.api.overview.EarnOverviews
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

internal class WalletTokenCurrencyItemConverterTest {

    private val userWalletId = UserWalletId("00")
    private val accountId = createMainAccount(userWalletId).accountId
    private val factory = MockCryptoCurrencyFactory()
    private val coin = factory.createCoin(Blockchain.Ethereum)
    private val token = factory.createToken(
        blockchain = Blockchain.Ethereum,
        contractAddress = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
    )
    private val clickIntents: WalletContentClickIntents = mockk(relaxed = true)

    private val yieldPercent = BigDecimal("5.5")
    private val yieldMap = mapOf(token.yieldSupplyKey() to yieldPercent)
    private val promoText = resourceReference(
        R.string.yield_module_main_screen_promo_banner_message,
        wrappedList("5.50"),
    )

    private var previousLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        clearMocks(clickIntents)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(previousLocale)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Badge {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a headline WHEN convert THEN the badge has its text and is blue only when active`(
            model: BadgeColorModel,
        ) {
            // Arrange
            val headline = earnHeadline(isActive = model.isActive, badgeAccent = model.badgeAccent)
            val converter = createConverter(earnOverviews = earnOverviewsOf(coin, headline))

            // Act
            val actual = converter.convert(CryptoCurrencyStatus(coin, loaded()))

            // Assert
            val expected = listOf(
                TEST_EARN_BADGE_TEXT,
                model.expectedColor,
                TangemBadgeType.Solid,
                TangemBadgeShape.Rounded,
                TangemBadgeSize.X4,
            )
            assertThat(actual.contentTitle().badge?.fields()).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            BadgeColorModel(true, EarnBadgeAccent.Info, expectedColor = TangemBadgeColor.Blue),
            BadgeColorModel(true, EarnBadgeAccent.Success, expectedColor = TangemBadgeColor.Blue),
            BadgeColorModel(false, EarnBadgeAccent.Info, expectedColor = TangemBadgeColor.Gray),
            BadgeColorModel(false, EarnBadgeAccent.Success, expectedColor = TangemBadgeColor.Gray),
        )

        @Test
        fun `GIVEN empty overviews WHEN convert THEN there is no badge`() {
            // Act
            val actual = createConverter().convert(CryptoCurrencyStatus(coin, loaded()))

            // Assert
            assertThat(actual.contentTitle().badge).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StatusesWithoutBalance {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a headline AND a status without balance WHEN convert THEN there is no badge`(
            model: NoBalanceStatusModel,
        ) {
            // Arrange
            val converter = createConverter(earnOverviews = earnOverviewsOf(coin, earnHeadline(isActive = true)))

            // Act
            val actual = converter.convert(CryptoCurrencyStatus(coin, model.value))

            // Assert
            assertThat(actual.contentTitle().badge).isNull()
        }

        private fun provideTestModels() = listOf(
            NoBalanceStatusModel(description = "Loading", value = CryptoCurrencyStatus.Loading),
            NoBalanceStatusModel(
                description = "Unreachable",
                value = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
            ),
            NoBalanceStatusModel(
                description = "NoAmount",
                value = CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null),
            ),
            NoBalanceStatusModel(
                description = "MissedDerivation",
                value = CryptoCurrencyStatus.MissedDerivation(priceChange = null, fiatRate = null),
            ),
        )
    }

    @Nested
    inner class YieldPromo {

        @Test
        fun `GIVEN a Loaded token in the yield map WHEN convert with promo THEN the banner shows the yield rate`() {
            // Arrange
            val converter = createConverter(shouldShowPromo = true, yieldModuleApyMap = yieldMap)

            // Act
            val actual = converter.convert(CryptoCurrencyStatus(token, loaded()))

            // Assert
            val banner = actual.promoBannerUM as TangemTokenRowUM.PromoBannerUM.Content
            assertThat(banner.title).isEqualTo(promoText)
        }

        @Test
        fun `GIVEN the promo banner WHEN it is clicked THEN the yield percent of the token is passed`() {
            // Arrange
            val converter = createConverter(shouldShowPromo = true, yieldModuleApyMap = yieldMap)
            val status = CryptoCurrencyStatus(token, loaded())
            val banner = converter.convert(status).promoBannerUM as TangemTokenRowUM.PromoBannerUM.Content

            // Act
            banner.onPromoBannerClick()

            // Assert
            verify(exactly = 1) { clickIntents.onYieldPromoClicked(accountId, status, "5.5") }
        }

        @Test
        fun `GIVEN a staking headline AND a promo token in the yield map WHEN convert THEN the banner shows the yield rate`() {
            // Arrange
            val stakingHeadline = earnHeadline(isActive = true, badgeAccent = EarnBadgeAccent.Info, fraction = "0.08")
            val converter = createConverter(
                shouldShowPromo = true,
                yieldModuleApyMap = yieldMap,
                earnOverviews = earnOverviewsOf(token, stakingHeadline),
            )

            // Act
            val actual = converter.convert(CryptoCurrencyStatus(token, loaded()))

            // Assert
            val banner = actual.promoBannerUM as TangemTokenRowUM.PromoBannerUM.Content
            assertThat(banner.title).isEqualTo(promoText)
        }

        @Test
        fun `GIVEN a token missing in the yield map WHEN convert with promo THEN there is no banner`() {
            // Arrange
            val converter = createConverter(shouldShowPromo = true, yieldModuleApyMap = emptyMap())

            // Act
            val actual = converter.convert(CryptoCurrencyStatus(token, loaded()))

            // Assert
            assertThat(actual.promoBannerUM).isEqualTo(TangemTokenRowUM.PromoBannerUM.Empty)
        }

        @Test
        fun `GIVEN a token in the yield map WHEN convert without promo THEN there is no banner`() {
            // Arrange
            val converter = createConverter(shouldShowPromo = false, yieldModuleApyMap = yieldMap)

            // Act
            val actual = converter.convert(CryptoCurrencyStatus(token, loaded()))

            // Assert
            assertThat(actual.promoBannerUM).isEqualTo(TangemTokenRowUM.PromoBannerUM.Empty)
        }
    }

    private fun createConverter(
        shouldShowPromo: Boolean = false,
        yieldModuleApyMap: Map<String, BigDecimal> = emptyMap(),
        earnOverviews: EarnOverviews = EarnOverviews.Empty,
    ) = WalletTokenCurrencyItemConverter(
        appCurrency = AppCurrency.Default,
        accountId = accountId,
        shouldShowPromo = shouldShowPromo,
        yieldModuleApyMap = yieldModuleApyMap,
        clickIntents = clickIntents,
        earnOverviews = earnOverviews,
        userWalletId = userWalletId,
    )

    private fun TangemTokenRowUM.contentTitle() = titleUM as TangemTokenRowUM.TitleUM.Content

    /** `TangemBadgeUM` is not a data class, so its look is compared field by field. */
    private fun TangemBadgeUM.fields() = listOf(text, color, type, shape, size)

    private fun loaded() = CryptoCurrencyStatus.Loaded(
        amount = BigDecimal.ONE,
        fiatAmount = BigDecimal.TEN,
        fiatRate = BigDecimal.TEN,
        priceChange = BigDecimal.ZERO,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(value = "addr", type = NetworkAddress.Address.Type.Primary),
        ),
        sources = CryptoCurrencyStatus.Sources(),
    )

    internal data class NoBalanceStatusModel(val description: String, val value: CryptoCurrencyStatus.Value) {
        override fun toString(): String = description
    }

    internal data class BadgeColorModel(
        val isActive: Boolean,
        val badgeAccent: EarnBadgeAccent,
        val expectedColor: TangemBadgeColor,
    ) {
        override fun toString(): String = "active=$isActive $badgeAccent -> $expectedColor"
    }
}