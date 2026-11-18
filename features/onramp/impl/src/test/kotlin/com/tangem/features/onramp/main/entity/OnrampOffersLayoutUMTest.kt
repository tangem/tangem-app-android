package com.tangem.features.onramp.main.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.marketing.models.MarketingBanner.RecommendedSlot
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OnrampOffersLayoutUMTest {

    private val best = offer(providerId = "best", OnrampOfferAdvantagesUM.GreatRate, RecommendedSlot.BEST)
    private val fastest = offer(providerId = "fastest", OnrampOfferAdvantagesUM.Fastest, RecommendedSlot.FASTEST)
    private val restricted = offer(providerId = "restricted", OnrampOfferAdvantagesUM.Restricted, RecommendedSlot.BEST)
    private val single = offer(providerId = "single", OnrampOfferAdvantagesUM.Default, RecommendedSlot.BEST)
    private val unslotted = offer(providerId = "unslotted", OnrampOfferAdvantagesUM.GreatRate, slot = null)
    private val button = OnrampAllOffersButtonConfig(title = TextReference.EMPTY, onClick = {})
    private val content = OnrampOffersBlockUM.Content(
        recentOffer = null,
        recommended = persistentListOf(best, fastest),
        onrampAllOffersButtonConfig = null,
        allOffersButtonIfOfferHidden = button,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN replaced slots WHEN layoutFor THEN banners take matching positions and button follows hidden offers`(
        model: LayoutModel,
    ) {
        // Act
        val actual = model.state.layoutFor(model.slots)

        // Assert
        assertThat(actual.recommendedRows).containsExactlyElementsIn(model.expectedRows).inOrder()
        assertThat(actual.allOffersButton).isEqualTo(model.expectedButton)
    }

    @Test
    fun `GIVEN offers with more offers available WHEN no slot hidden THEN own all offers button stays`() {
        // Arrange
        val own = OnrampAllOffersButtonConfig(title = TextReference.EMPTY, onClick = {})
        val withOwnButton = content.copy(onrampAllOffersButtonConfig = own)

        // Act
        val actual = withOwnButton.layoutFor(setOf(RecommendedSlot.BEST))

        // Assert
        assertThat(actual.allOffersButton).isEqualTo(own)
    }

    @Test
    fun `GIVEN recent offer WHEN every slot replaced THEN recent offer stays`() {
        // Arrange
        val withRecent = content.copy(recentOffer = best.copy(category = OnrampOfferCategoryUM.RecentlyUsed))

        // Act
        val actual = withRecent.layoutFor(RecommendedSlot.entries.toSet())

        // Assert
        assertThat(actual.recentOffer).isEqualTo(withRecent.recentOffer)
    }

    @Test
    fun `GIVEN visible banner WHEN hidden again THEN offers and button return to original state`() {
        // Act
        val shown = content.layoutFor(setOf(RecommendedSlot.BEST))
        val restored = content.layoutFor(emptySet())

        // Assert
        assertThat(shown.allOffersButton).isEqualTo(button)
        assertThat(restored).isEqualTo(
            OnrampOffersLayoutUM(
                recentOffer = null,
                recommendedRows = persistentListOf(
                    OnrampRecommendedRowUM.Offer(best),
                    OnrampRecommendedRowUM.Offer(fastest),
                ),
                allOffersButton = null,
            ),
        )
    }

    internal data class LayoutModel(
        val state: OnrampOffersBlockUM,
        val slots: Set<RecommendedSlot>,
        val expectedRows: List<OnrampRecommendedRowUM>,
        val expectedButton: OnrampAllOffersButtonConfig?,
    )

    private fun provideTestModels(): List<LayoutModel> {
        val bestRow = OnrampRecommendedRowUM.Offer(best)
        val fastestRow = OnrampRecommendedRowUM.Offer(fastest)
        val bestBanner = OnrampRecommendedRowUM.Banner(RecommendedSlot.BEST)
        val fastestBanner = OnrampRecommendedRowUM.Banner(RecommendedSlot.FASTEST)
        return listOf(
            LayoutModel(content, emptySet(), listOf(bestRow, fastestRow), null),
            LayoutModel(content, setOf(RecommendedSlot.BEST), listOf(bestBanner, fastestRow), button),
            LayoutModel(content, setOf(RecommendedSlot.FASTEST), listOf(bestRow, fastestBanner), button),
            LayoutModel(content, RecommendedSlot.entries.toSet(), listOf(bestBanner, fastestBanner), button),
            LayoutModel(
                state = content.copy(recentOffer = best, recommended = persistentListOf(fastest)),
                slots = setOf(RecommendedSlot.BEST),
                expectedRows = listOf(bestBanner, fastestRow),
                expectedButton = null,
            ),
            LayoutModel(
                state = content.copy(recommended = persistentListOf(restricted)),
                slots = setOf(RecommendedSlot.BEST),
                expectedRows = listOf(bestBanner),
                expectedButton = button,
            ),
            LayoutModel(
                state = content.copy(recommended = persistentListOf(single)),
                slots = setOf(RecommendedSlot.BEST),
                expectedRows = listOf(bestBanner),
                expectedButton = button,
            ),
            LayoutModel(
                state = content.copy(recommended = persistentListOf(single)),
                slots = setOf(RecommendedSlot.FASTEST),
                expectedRows = listOf(OnrampRecommendedRowUM.Offer(single), fastestBanner),
                expectedButton = null,
            ),
            LayoutModel(
                state = content.copy(recommended = persistentListOf(unslotted)),
                slots = RecommendedSlot.entries.toSet(),
                expectedRows = listOf(bestBanner, fastestBanner, OnrampRecommendedRowUM.Offer(unslotted)),
                expectedButton = null,
            ),
            LayoutModel(OnrampOffersBlockUM.Empty, setOf(RecommendedSlot.BEST), emptyList(), null),
            LayoutModel(
                state = content.copy(recentOffer = best, recommended = persistentListOf()),
                slots = RecommendedSlot.entries.toSet(),
                expectedRows = emptyList(),
                expectedButton = null,
            ),
            LayoutModel(OnrampOffersBlockUM.Loading, setOf(RecommendedSlot.FASTEST), listOf(fastestBanner), null),
        )
    }

    private fun offer(providerId: String, advantages: OnrampOfferAdvantagesUM, slot: RecommendedSlot?) = OnrampOfferUM(
        category = OnrampOfferCategoryUM.Recommended,
        advantages = advantages,
        paymentMethod = OnrampPaymentMethod(id = "card", name = "Card", imageUrl = "", type = PaymentMethodType.CARD),
        providerId = providerId,
        providerName = providerId,
        rate = "1",
        diff = null,
        onBuyClicked = {},
        recommendedSlot = slot,
    )
}