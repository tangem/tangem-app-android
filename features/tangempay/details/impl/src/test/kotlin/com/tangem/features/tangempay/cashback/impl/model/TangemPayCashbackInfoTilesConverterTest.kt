package com.tangem.features.tangempay.cashback.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCashbackInfoTilesConverterTest {

    private val converter = TangemPayCashbackInfoTilesConverter(
        onRateClick = {},
        onAccrualsClick = {},
    )

    @ParameterizedTest
    @MethodSource("titleModels")
    fun `GIVEN cards WHEN convert THEN rate title reflects the backend rates`(model: TitleModel) {
        // Act
        val result = converter.convert(model.cards)

        // Assert
        assertThat(result.rate.title).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("subtitleModels")
    fun `GIVEN cards WHEN convert THEN rate subtitle names the top rate card`(model: SubtitleModel) {
        // Act
        val result = converter.convert(model.cards)

        // Assert
        assertThat(result.rate.subtitle).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN any cards WHEN convert THEN accruals tile is static`() {
        // Act
        val result = converter.convert(twoCards())

        // Assert
        assertThat(result.accruals.title).isEqualTo(resourceReference(R.string.tangempay_cashback_accruals_title))
        assertThat(result.accruals.subtitle)
            .isEqualTo(resourceReference(R.string.tangempay_cashback_accruals_subtitle))
    }

    private fun titleModels() = listOf(
        TitleModel(
            cards = twoCards(),
            expected = resourceReference(R.string.tangempay_cashback_rate_title_up_to, wrappedList("2")),
        ),
        TitleModel(
            cards = listOf(card(cardType = "prestige", title = "Prestige Card", rate = "1.0")),
            expected = resourceReference(R.string.tangempay_cashback_rate_title, wrappedList("1")),
        ),
        TitleModel(
            cards = listOf(card(cardType = "prestige", title = "Prestige Card", rate = "1.50")),
            expected = resourceReference(R.string.tangempay_cashback_rate_title, wrappedList("1.5")),
        ),
        TitleModel(
            cards = emptyList(),
            expected = resourceReference(R.string.tangempay_cashback_title),
        ),
    )

    private fun subtitleModels() = listOf(
        SubtitleModel(
            cards = listOf(card(cardType = "plus", title = "Plus Card", rate = "2.0")),
            expected = resourceReference(R.string.tangempay_cashback_rate_subtitle, wrappedList("Plus Card")),
        ),
        SubtitleModel(
            cards = twoCards(),
            expected = resourceReference(R.string.tangempay_cashback_rate_subtitle, wrappedList("Plus Card")),
        ),
        SubtitleModel(
            cards = listOf(card(cardType = "plus", title = "", rate = "2.0")),
            expected = TextReference.EMPTY,
        ),
        SubtitleModel(cards = emptyList(), expected = TextReference.EMPTY),
    )

    private fun twoCards() = listOf(
        card(cardType = "basic", title = "Basic Card", rate = "1.0"),
        card(cardType = "plus", title = "Plus Card", rate = "2.0"),
    )

    private fun card(cardType: String, title: String, rate: String) = CashbackCard(
        cardType = cardType,
        title = title,
        rate = BigDecimal(rate),
        minPurchase = null,
    )

    data class TitleModel(val cards: List<CashbackCard>, val expected: TextReference)

    data class SubtitleModel(val cards: List<CashbackCard>, val expected: TextReference)
}