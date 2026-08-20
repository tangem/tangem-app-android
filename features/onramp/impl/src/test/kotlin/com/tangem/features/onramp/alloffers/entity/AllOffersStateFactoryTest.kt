package com.tangem.features.onramp.alloffers.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.onramp.model.OnrampAmount
import com.tangem.domain.onramp.model.OnrampOffer
import com.tangem.domain.onramp.model.OnrampOfferAdvantages
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampPaymentMethodGroup
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.PaymentMethodStatus
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.main.entity.OnrampOfferAdvantagesUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.Provider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AllOffersStateFactoryTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val allOffersIntents: AllOffersIntents = mockk(relaxUnitFun = true)

    private val factory = AllOffersStateFactory(
        analyticsEventHandler = analyticsEventHandler,
        currentStateProvider = Provider { AllOffersStateUM.Loading },
        allOffersIntents = allOffersIntents,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler, allOffersIntents)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN method groups WHEN getLoadedPaymentsState THEN restricted notification matches`(
        model: RestrictedModel,
    ) {
        // Act
        val state = factory.getLoadedPaymentsState(methodGroups = model.methodGroups, currencyCode = CURRENCY_CODE)

        // Assert
        val content = state as AllOffersStateUM.Content
        assertThat(content.restrictedNotification).isEqualTo(model.expectedNotification)
    }

    private fun provideTestModels() = listOf(
        RestrictedModel(
            name = "no restricted quotes - no notification",
            methodGroups = listOf(
                createGroup(createOffer(isRestricted = false), createOffer(isRestricted = false)),
            ),
            expectedNotification = null,
        ),
        RestrictedModel(
            name = "one restricted quote in any group - notification shown",
            methodGroups = listOf(
                createGroup(createOffer(isRestricted = false)),
                createGroup(createOffer(isRestricted = true), createOffer(isRestricted = false)),
            ),
            expectedNotification = OnrampRegionRestrictionNotification,
        ),
        RestrictedModel(
            name = "all quotes restricted - notification shown",
            methodGroups = listOf(createGroup(createOffer(isRestricted = true))),
            expectedNotification = OnrampRegionRestrictionNotification,
        ),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MapOffersToUM {

        @Test
        fun `GIVEN restricted offer WHEN getLoadedPaymentsState THEN row is restricted and buy click is inert`() {
            // Arrange — the domain advantage must not survive; only Restricted disables the Buy button
            val groups = listOf(createGroup(createOffer(isRestricted = true, advantages = OnrampOfferAdvantages.BestRate)))

            // Act
            val offer = offersOf(groups).single()

            // Assert
            assertThat(offer.advantages).isEqualTo(OnrampOfferAdvantagesUM.Restricted)
            assertThat(offer.diff).isNull()
            offer.onBuyClicked()
            verify(exactly = 0) { allOffersIntents.onBuyClick(any(), any()) }
        }

        @Test
        fun `GIVEN purchasable offer WHEN getLoadedPaymentsState THEN advantage is kept and buy click works`() {
            // Arrange
            val groups = listOf(createGroup(createOffer(isRestricted = false, advantages = OnrampOfferAdvantages.BestRate)))

            // Act
            val offer = offersOf(groups).single()

            // Assert
            assertThat(offer.advantages).isEqualTo(OnrampOfferAdvantagesUM.BestRate)
            offer.onBuyClicked()
            verify(exactly = 1) { allOffersIntents.onBuyClick(any(), any()) }
        }

        @Test
        fun `GIVEN restricted offer WHEN getLoadedPaymentsState THEN it still shows its real rate`() {
            // Arrange — the reason restriction is a flag on a successful quote rather than an error
            val restricted = offersOf(listOf(createGroup(createOffer(isRestricted = true)))).single()

            // Act
            val purchasable = offersOf(listOf(createGroup(createOffer(isRestricted = false)))).single()

            // Assert
            assertThat(restricted.rate).isEqualTo(purchasable.rate)
            assertThat(restricted.rate).isNotEmpty()
        }

        @Test
        fun `GIVEN amount error offer WHEN getLoadedPaymentsState THEN row is unavailable`() {
            // Arrange
            val groups = listOf(createGroup(createAmountErrorOffer()))

            // Act
            val offer = offersOf(groups).single()

            // Assert
            assertThat(offer.advantages).isEqualTo(OnrampOfferAdvantagesUM.Unavailable.MinAmount)
        }

        @Test
        fun `GIVEN generic error offer WHEN getLoadedPaymentsState THEN row is dropped`() {
            // Arrange
            val groups = listOf(
                createGroup(
                    OnrampOffer(quote = mockk<OnrampQuote.Error>(), rateDif = null),
                    createOffer(isRestricted = true),
                ),
            )

            // Act
            val offers = offersOf(groups)

            // Assert
            assertThat(offers.map { it.advantages }).containsExactly(OnrampOfferAdvantagesUM.Restricted)
        }

        private fun offersOf(groups: List<OnrampPaymentMethodGroup>) =
            (factory.getLoadedPaymentsState(methodGroups = groups, currencyCode = CURRENCY_CODE)
                as AllOffersStateUM.Content)
                .methods
                .single()
                .offers
    }

    private fun createAmountErrorOffer(): OnrampOffer {
        val quote = mockk<OnrampQuote.AmountError> {
            every { error } returns OnrampError.AmountError.TooSmallError(requiredAmount = BigDecimal.TEN)
            every { provider } returns mockk(relaxed = true) {
                every { id } returns "provider-id"
                every { info.name } returns "Provider"
            }
            every { paymentMethod } returns METHOD
            // formatRequiredAmount reads the fiat side to render "from <amount>"
            every { fromAmount } returns AMOUNT
        }
        return OnrampOffer(quote = quote, rateDif = null)
    }

    private fun createGroup(vararg offers: OnrampOffer): OnrampPaymentMethodGroup {
        return OnrampPaymentMethodGroup(
            paymentMethod = METHOD,
            offers = offers.toList(),
            bestRateOffer = null,
            providerCount = offers.size,
            isBestPaymentMethod = false,
            methodStatus = PaymentMethodStatus.Available,
        )
    }

    private fun createOffer(
        isRestricted: Boolean,
        advantages: OnrampOfferAdvantages = OnrampOfferAdvantages.Default,
    ): OnrampOffer {
        return OnrampOffer(
            quote = createQuote(isRestricted),
            rateDif = null,
            advantages = advantages,
        )
    }

    private fun createQuote(isRestricted: Boolean): OnrampQuote.Data {
        return mockk<OnrampQuote.Data> {
            every { this@mockk.isRestricted } returns isRestricted
            every { provider } returns mockk(relaxed = true) {
                every { id } returns "provider-id"
                every { info.name } returns "Provider"
            }
            every { paymentMethod } returns METHOD
            every { toAmount } returns AMOUNT
            every { fromAmount } returns AMOUNT
        }
    }

    internal data class RestrictedModel(
        val name: String,
        val methodGroups: List<OnrampPaymentMethodGroup>,
        val expectedNotification: NotificationUM?,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val CURRENCY_CODE = "USD"

        val METHOD = OnrampPaymentMethod(
            id = "card",
            name = "Card",
            imageUrl = "",
            type = PaymentMethodType.CARD,
        )

        val AMOUNT = OnrampAmount(value = BigDecimal.ONE, decimals = 8, symbol = "BTC")
    }
}