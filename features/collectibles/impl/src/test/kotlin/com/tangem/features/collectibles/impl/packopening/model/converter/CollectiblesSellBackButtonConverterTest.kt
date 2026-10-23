package com.tangem.features.collectibles.impl.packopening.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.collectibles.models.CollectibleBuybackOffer
import com.tangem.features.collectibles.impl.packopening.model.BuybackState
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.SellBackButtonUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesSellBackButtonConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a buyback state WHEN convert THEN the button reads it`(model: ButtonModel) {
        // Act
        val actual = CollectiblesSellBackButtonConverter.convert(model.state)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ButtonModel(val state: BuybackState, val expected: SellBackButtonUM)

    private fun provideTestModels() = listOf(
        ButtonModel(state = BuybackState.Unavailable, expected = UNAVAILABLE),
        ButtonModel(state = BuybackState.Available(OFFER), expected = AVAILABLE),
        ButtonModel(state = BuybackState.InProgress(OFFER), expected = AVAILABLE.copy(isLoading = true)),
    )

    private companion object {

        val OFFER = CollectibleBuybackOffer(amount = BigDecimal("42.50"), expiresAt = null)

        val AVAILABLE = SellBackButtonUM(
            textRef = resourceReference(R.string.collectibles_open_pack_sell_back_button, wrappedList("$42.50")),
            isEnabled = true,
            isLoading = false,
        )
        val UNAVAILABLE = SellBackButtonUM(
            textRef = resourceReference(R.string.collectibles_open_pack_buyback_unavailable),
            isEnabled = false,
            isLoading = false,
        )
    }
}