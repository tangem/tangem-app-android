package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.jointaccount.model.JointPendingOperation
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
import io.mockk.every
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
import kotlin.reflect.KClass

internal class WalletTokenRowConverterTest {

    private val userWalletId = UserWalletId("00")
    private val coin = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)
    private val clickIntents: WalletContentClickIntents = mockk(relaxed = true)

    private val accountId = createMainAccount(userWalletId).accountId

    private val converter = createConverter()

    private fun createConverter(
        jointPendingOperations: List<JointPendingOperation> = emptyList(),
        yieldModuleApyMap: Map<String, BigDecimal> = emptyMap(),
        earnOverviews: EarnOverviews = EarnOverviews.Empty,
    ) = WalletTokenRowConverter(
        appCurrency = AppCurrency.Default,
        accountId = accountId,
        clickIntents = clickIntents,
        yieldModuleApyMap = yieldModuleApyMap,
        earnOverviews = earnOverviews,
        userWalletId = userWalletId,
        jointPendingOperations = jointPendingOperations,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Badge {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a headline WHEN convert THEN the badge has its text and the status of activity and accent`(
            model: BadgeStatusModel,
        ) {
            // Arrange
            val headline = earnHeadline(isActive = model.isActive, badgeAccent = model.badgeAccent)
            val rowConverter = createConverter(earnOverviews = earnOverviewsOf(coin, headline))

            // Act
            val actual = rowConverter.convert(CryptoCurrencyStatus(coin, loaded()), shouldShowPromo = false)

            // Assert
            val expected = TangemTokenRow.Badge(text = TEST_EARN_BADGE_TEXT, status = model.expectedStatus)
            assertThat((actual.state as TangemTokenRow.State.Content).badge).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            BadgeStatusModel(false, EarnBadgeAccent.Info, expectedStatus = TangemBadge.Status.Neutral),
            BadgeStatusModel(false, EarnBadgeAccent.Success, expectedStatus = TangemBadge.Status.Neutral),
            BadgeStatusModel(true, EarnBadgeAccent.Info, expectedStatus = TangemBadge.Status.Info),
            BadgeStatusModel(true, EarnBadgeAccent.Success, expectedStatus = TangemBadge.Status.Success),
        )

        @Test
        fun `GIVEN overviews without the currency WHEN convert THEN there is no badge`() {
            // Arrange
            val other = MockCryptoCurrencyFactory().createCoin(Blockchain.Bitcoin)
            val rowConverter = createConverter(earnOverviews = earnOverviewsOf(other, earnHeadline(isActive = true)))

            // Act
            val actual = rowConverter.convert(CryptoCurrencyStatus(coin, loaded()), shouldShowPromo = false)

            // Assert
            assertThat((actual.state as TangemTokenRow.State.Content).badge).isNull()
        }

        @Test
        fun `GIVEN empty overviews WHEN convert THEN there is no badge`() {
            // Act
            val actual = converter.convert(CryptoCurrencyStatus(coin, loaded()), shouldShowPromo = false)

            // Assert
            assertThat((actual.state as TangemTokenRow.State.Content).badge).isNull()
        }
    }

    @Nested
    inner class YieldPromo {

        private val token = MockCryptoCurrencyFactory().createToken(
            blockchain = Blockchain.Ethereum,
            contractAddress = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
        )
        private val yieldPercent = BigDecimal("5.5")
        private val yieldMap = mapOf(token.yieldSupplyKey() to yieldPercent)
        private val promoText = resourceReference(
            R.string.yield_module_main_screen_promo_banner_message,
            wrappedList("5.50"),
        )

        private var previousLocale: Locale = Locale.getDefault()

        @BeforeEach
        fun pinLocale() {
            previousLocale = Locale.getDefault()
            Locale.setDefault(Locale.US)
            clearMocks(clickIntents)
        }

        @AfterEach
        fun restoreLocale() {
            Locale.setDefault(previousLocale)
        }

        @Test
        fun `GIVEN a Loaded token in the yield map WHEN convert with promo THEN the bubble shows the yield rate`() {
            // Arrange
            val rowConverter = createConverter(yieldModuleApyMap = yieldMap)

            // Act
            val actual = rowConverter.convert(CryptoCurrencyStatus(token, loaded()), shouldShowPromo = true)

            // Assert
            val bubble = (actual.state as TangemTokenRow.State.Content).messageBubble
            assertThat(bubble?.text).isEqualTo(promoText)
        }

        @Test
        fun `GIVEN the promo bubble WHEN it is clicked THEN the yield percent of the token is passed`() {
            // Arrange
            val rowConverter = createConverter(yieldModuleApyMap = yieldMap)
            val status = CryptoCurrencyStatus(token, loaded())
            val bubble = (rowConverter.convert(status, shouldShowPromo = true).state as TangemTokenRow.State.Content)
                .messageBubble

            // Act
            bubble?.onClick?.invoke()

            // Assert
            verify(exactly = 1) { clickIntents.onYieldPromoClicked(accountId, status, "5.5") }
        }

        @Test
        fun `GIVEN a staking headline AND a promo token in the yield map WHEN convert THEN the promo shows the yield rate`() {
            // Arrange
            val stakingHeadline = earnHeadline(isActive = true, badgeAccent = EarnBadgeAccent.Info, fraction = "0.08")
            val rowConverter = createConverter(
                yieldModuleApyMap = yieldMap,
                earnOverviews = earnOverviewsOf(token, stakingHeadline),
            )

            // Act
            val actual = rowConverter.convert(CryptoCurrencyStatus(token, loaded()), shouldShowPromo = true)

            // Assert
            val bubble = (actual.state as TangemTokenRow.State.Content).messageBubble
            assertThat(bubble?.text).isEqualTo(promoText)
        }

        @Test
        fun `GIVEN a token missing in the yield map WHEN convert with promo THEN there is no promo`() {
            // Arrange
            val rowConverter = createConverter(yieldModuleApyMap = emptyMap())

            // Act
            val actual = rowConverter.convert(CryptoCurrencyStatus(token, loaded()), shouldShowPromo = true)

            // Assert
            assertThat((actual.state as TangemTokenRow.State.Content).messageBubble).isNull()
            assertThat(actual.onPromoShown).isNull()
        }

        @Test
        fun `GIVEN a coin WHEN convert with promo THEN there is no promo`() {
            // Arrange
            val rowConverter = createConverter(yieldModuleApyMap = yieldMap)

            // Act
            val actual = rowConverter.convert(CryptoCurrencyStatus(coin, loaded()), shouldShowPromo = true)

            // Assert
            assertThat((actual.state as TangemTokenRow.State.Content).messageBubble).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StatusesWithoutBalance {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a headline AND a status without balance WHEN convert THEN the row carries no earn badge`(
            model: NoBalanceStatusModel,
        ) {
            // Arrange
            val rowConverter = createConverter(earnOverviews = earnOverviewsOf(coin, earnHeadline(isActive = true)))

            // Act
            val actual = rowConverter.convert(CryptoCurrencyStatus(coin, model.value), shouldShowPromo = false)

            // Assert
            assertThat(actual.state).isInstanceOf(model.expectedState.java)
            assertThat((actual.state as? TangemTokenRow.State.Content)?.badge).isNull()
        }

        private fun provideTestModels() = listOf(
            NoBalanceStatusModel(
                description = "Loading",
                value = CryptoCurrencyStatus.Loading,
                expectedState = TangemTokenRow.State.Shimmer::class,
            ),
            NoBalanceStatusModel(
                description = "Unreachable",
                value = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
                expectedState = TangemTokenRow.State.Unreachable::class,
            ),
            NoBalanceStatusModel(
                description = "NoAmount",
                value = CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null),
                expectedState = TangemTokenRow.State.Content::class,
            ),
            NoBalanceStatusModel(
                description = "MissedDerivation",
                value = CryptoCurrencyStatus.MissedDerivation(priceChange = null, fiatRate = null),
                expectedState = TangemTokenRow.State.NoAddress::class,
            ),
        )
    }

    internal data class NoBalanceStatusModel(
        val description: String,
        val value: CryptoCurrencyStatus.Value,
        val expectedState: KClass<out TangemTokenRow.State>,
    ) {
        override fun toString(): String = description
    }

    internal data class BadgeStatusModel(
        val isActive: Boolean,
        val badgeAccent: EarnBadgeAccent,
        val expectedStatus: TangemBadge.Status,
    ) {
        override fun toString(): String = "active=$isActive $badgeAccent -> $expectedStatus"
    }

    @Test
    fun `GIVEN loading status WHEN convert THEN long click is disabled`() {
        // Act
        val actual = converter.convert(
            CryptoCurrencyStatus(currency = coin, value = CryptoCurrencyStatus.Loading),
            shouldShowPromo = false,
        )

        // Assert
        assertThat(actual.onLongClick).isNull()
    }

    @Test
    fun `GIVEN loaded status WHEN convert THEN long click is enabled`() {
        // Act
        val actual = converter.convert(CryptoCurrencyStatus(currency = coin, value = loaded()), shouldShowPromo = false)

        // Assert
        assertThat(actual.onLongClick).isNotNull()
    }

    @Test
    fun `GIVEN no amount status with pending joint operation WHEN convert THEN pending bubble is shown`() {
        // Arrange
        val operation: JointPendingOperation = mockk(relaxed = true) {
            every { transfer.currency } returns coin
        }
        val jointConverter = createConverter(jointPendingOperations = listOf(operation))
        val status = CryptoCurrencyStatus(
            currency = coin,
            value = CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null),
        )

        // Act
        val actual = jointConverter.convert(status, shouldShowPromo = true)

        // Assert
        assertThat((actual.state as TangemTokenRow.State.Content).messageBubble).isNotNull()
        assertThat(actual.onPromoShown).isNull()
    }

    private fun loaded() = CryptoCurrencyStatus.Loaded(
        amount = BigDecimal.ONE,
        fiatAmount = BigDecimal.TEN,
        fiatRate = BigDecimal.TEN,
        priceChange = BigDecimal.ZERO,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = networkAddress,
        sources = CryptoCurrencyStatus.Sources(),
    )

    private val networkAddress = NetworkAddress.Single(
        defaultAddress = NetworkAddress.Address(value = "addr", type = NetworkAddress.Address.Type.Primary),
    )
}