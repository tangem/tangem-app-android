package com.tangem.features.onramp.main.entity.factory

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.onramp.model.OnrampAmount
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampOffer
import com.tangem.domain.onramp.model.OnrampOfferAdvantages
import com.tangem.domain.onramp.model.OnrampOfferCategory
import com.tangem.domain.onramp.model.OnrampOffersBlock
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.features.onramp.main.entity.OnrampIntents
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.features.onramp.main.entity.OnrampOfferAdvantagesUM
import com.tangem.features.onramp.main.entity.OnrampOfferCategoryUM
import com.tangem.features.onramp.main.entity.OnrampOffersBlockUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.Provider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Covers [OnrampOffersStateFactory], the only place that turns a region-restricted domain offer into a
 * non-executable main-screen card. The domain advantage of a restricted offer is deliberately discarded
 * in favour of [OnrampOfferAdvantagesUM.Restricted], which is what disables its Buy button.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OnrampOffersStateFactoryTest {

    private lateinit var currentState: OnrampMainComponentUM

    private val onrampIntents: OnrampIntents = mockk(relaxUnitFun = true)

    private val stateFactory = OnrampStateFactory(
        currentStateProvider = Provider { currentState },
        onrampAmountButtonUMStateFactory = OnrampAmountButtonUMStateFactory(),
        cryptoCurrency = MockCryptoCurrencyFactory().ethereum,
        onrampIntents = onrampIntents,
    )

    private val factory = OnrampOffersStateFactory(
        currentStateProvider = Provider { currentState },
        onrampIntents = onrampIntents,
    )

    @BeforeEach
    fun resetState() {
        clearMocks(onrampIntents)
        currentState = buildReadyContent()
    }

    @Test
    fun `GIVEN restricted offer WHEN getOffersState THEN card is restricted and buy click is inert`() {
        // Arrange — a restricted offer that the domain still labelled GreatRate
        val blocks = listOf(
            buildBlock(
                category = OnrampOfferCategory.Recommended,
                offers = listOf(buildOffer(isRestricted = true, advantages = OnrampOfferAdvantages.GreatRate)),
            ),
        )

        // Act
        val offer = factory.getOffersState(blocks).recommendedOffers().single()

        // Assert
        assertThat(offer.advantages).isEqualTo(OnrampOfferAdvantagesUM.Restricted)
        assertThat(offer.diff).isNull()
        offer.onBuyClicked()
        verify(exactly = 0) { onrampIntents.onBuyClick(any(), any(), any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN purchasable offer WHEN getOffersState THEN advantage is mapped and buy click works`(
        model: AdvantageModel,
    ) {
        // Arrange
        val blocks = listOf(
            buildBlock(
                category = OnrampOfferCategory.Recommended,
                offers = listOf(buildOffer(isRestricted = false, advantages = model.domain)),
            ),
        )

        // Act
        val offer = factory.getOffersState(blocks).recommendedOffers().single()

        // Assert
        assertThat(offer.advantages).isEqualTo(model.expected)
        offer.onBuyClicked()
        verify(exactly = 1) { onrampIntents.onBuyClick(any(), any(), any()) }
    }

    private fun provideTestModels() = listOf(
        AdvantageModel("default", OnrampOfferAdvantages.Default, OnrampOfferAdvantagesUM.Default),
        AdvantageModel("best rate", OnrampOfferAdvantages.BestRate, OnrampOfferAdvantagesUM.BestRate),
        AdvantageModel("fastest", OnrampOfferAdvantages.Fastest, OnrampOfferAdvantagesUM.Fastest),
        AdvantageModel("great rate", OnrampOfferAdvantages.GreatRate, OnrampOfferAdvantagesUM.GreatRate),
    )

    @Test
    fun `GIVEN recent and recommended blocks WHEN getOffersState THEN categories are kept apart`() {
        // Arrange
        val blocks = listOf(
            buildBlock(OnrampOfferCategory.Recent, listOf(buildOffer(providerId = "recent"))),
            buildBlock(OnrampOfferCategory.Recommended, listOf(buildOffer(providerId = "recommended"))),
        )

        // Act
        val content = factory.getOffersState(blocks).offersContent()

        // Assert
        assertThat(content.recentOffer?.providerId).isEqualTo("recent")
        assertThat(content.recentOffer?.category).isEqualTo(OnrampOfferCategoryUM.RecentlyUsed)
        assertThat(content.recommended.map { it.providerId }).containsExactly("recommended")
    }

    @Test
    fun `GIVEN restricted offer in recent block WHEN getOffersState THEN recent card is restricted`() {
        // Arrange
        val blocks = listOf(
            buildBlock(OnrampOfferCategory.Recent, listOf(buildOffer(isRestricted = true, providerId = "recent"))),
        )

        // Act
        val content = factory.getOffersState(blocks).offersContent()

        // Assert
        assertThat(content.recentOffer?.category).isEqualTo(OnrampOfferCategoryUM.RecentlyUsed)
        assertThat(content.recentOffer?.advantages).isEqualTo(OnrampOfferAdvantagesUM.Restricted)
    }

    @Test
    fun `GIVEN only non-data quotes WHEN getOffersState THEN offers block is empty`() {
        // Arrange
        val blocks = listOf(
            buildBlock(
                category = OnrampOfferCategory.Recommended,
                offers = listOf(
                    OnrampOffer(quote = mockk<OnrampQuote.AmountError>(), rateDif = null),
                    OnrampOffer(quote = mockk<OnrampQuote.Error>(), rateDif = null),
                ),
            ),
        )

        // Act
        val result = factory.getOffersState(blocks) as OnrampMainComponentUM.Content

        // Assert
        assertThat(result.offersBlockState).isEqualTo(OnrampOffersBlockUM.Empty)
    }

    @Test
    fun `GIVEN more offers available WHEN getOffersState THEN all-offers button is shown`() {
        // Arrange
        val blocks = listOf(buildBlock(OnrampOfferCategory.Recommended, listOf(buildOffer()), hasMoreOffers = true))

        // Act
        val content = factory.getOffersState(blocks).offersContent()

        // Assert
        assertThat(content.onrampAllOffersButtonConfig).isNotNull()
    }

    @Test
    fun `GIVEN no more offers WHEN getOffersState THEN all-offers button is hidden`() {
        // Arrange
        val blocks = listOf(buildBlock(OnrampOfferCategory.Recommended, listOf(buildOffer()), hasMoreOffers = false))

        // Act
        val content = factory.getOffersState(blocks).offersContent()

        // Assert
        assertThat(content.onrampAllOffersButtonConfig).isNull()
    }

    @Test
    fun `GIVEN offers block still loading WHEN getOffersState THEN emission is ignored`() {
        // Arrange — the block is cleared elsewhere first; until then offer emissions must not land
        currentState = buildReadyContent().copy(offersBlockState = OnrampOffersBlockUM.Loading)
        val blocks = listOf(buildBlock(OnrampOfferCategory.Recommended, listOf(buildOffer())))

        // Act
        val result = factory.getOffersState(blocks)

        // Assert
        assertThat(result).isEqualTo(currentState)
    }

    private fun OnrampMainComponentUM.offersContent(): OnrampOffersBlockUM.Content =
        (this as OnrampMainComponentUM.Content).offersBlockState as OnrampOffersBlockUM.Content

    private fun OnrampMainComponentUM.recommendedOffers() = offersContent().recommended

    private fun buildReadyContent(): OnrampMainComponentUM.Content {
        currentState = stateFactory.getInitialState(currency = "BTC", onClose = {}, openSettings = {})
        return stateFactory.getReadyState(currency = USD_CURRENCY).copy(
            offersBlockState = OnrampOffersBlockUM.Empty,
        )
    }

    private fun buildBlock(
        category: OnrampOfferCategory,
        offers: List<OnrampOffer>,
        hasMoreOffers: Boolean = false,
    ) = OnrampOffersBlock(category = category, offers = offers, hasMoreOffers = hasMoreOffers)

    private fun buildOffer(
        isRestricted: Boolean = false,
        advantages: OnrampOfferAdvantages = OnrampOfferAdvantages.Default,
        providerId: String = "provider-id",
    ) = OnrampOffer(
        quote = buildQuote(isRestricted = isRestricted, providerId = providerId),
        rateDif = BigDecimal("0.1"),
        advantages = advantages,
    )

    private fun buildQuote(isRestricted: Boolean, providerId: String) = mockk<OnrampQuote.Data> {
        every { this@mockk.isRestricted } returns isRestricted
        every { provider } returns mockk(relaxed = true) {
            every { id } returns providerId
            every { info.name } returns "Provider"
        }
        every { paymentMethod } returns METHOD
        every { toAmount } returns AMOUNT
        every { fromAmount } returns AMOUNT
    }

    internal data class AdvantageModel(
        val name: String,
        val domain: OnrampOfferAdvantages,
        val expected: OnrampOfferAdvantagesUM,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        val METHOD = OnrampPaymentMethod(id = "card", name = "Card", imageUrl = "", type = PaymentMethodType.CARD)

        val AMOUNT = OnrampAmount(value = BigDecimal.ONE, decimals = 8, symbol = "BTC")

        val USD_CURRENCY = OnrampCurrency(
            name = "US Dollar",
            code = "USD",
            image = null,
            precision = 2,
            unit = "$",
        )
    }
}