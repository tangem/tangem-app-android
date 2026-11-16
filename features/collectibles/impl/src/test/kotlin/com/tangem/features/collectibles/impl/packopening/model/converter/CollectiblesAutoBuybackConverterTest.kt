package com.tangem.features.collectibles.impl.packopening.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.features.collectibles.impl.packopening.CollectiblesConfirmOrderBottomSheetConfig
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesAutoBuybackConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a payout and a price WHEN convert THEN the payout is signed and the price has no trailing zeros`(
        model: ConvertModel,
    ) {
        // Act
        val actual = CollectiblesAutoBuybackConverter.convert(model.input)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ConvertModel(
        val input: CollectiblesAutoBuybackConverter.Input,
        val expected: CollectiblesConfirmOrderBottomSheetConfig.AutoBuyback,
    )

    private fun provideTestModels() = listOf(
        ConvertModel(
            input = CollectiblesAutoBuybackConverter.Input(amount = BigDecimal("42.5"), price = BigDecimal("51")),
            expected = CollectiblesConfirmOrderBottomSheetConfig.AutoBuyback(
                amount = "+" + usdc("42.5"),
                price = usdc("51", minDecimals = 0),
            ),
        ),
        ConvertModel(
            input = CollectiblesAutoBuybackConverter.Input(amount = BigDecimal("35.6"), price = BigDecimal("50.5")),
            expected = CollectiblesConfirmOrderBottomSheetConfig.AutoBuyback(
                amount = "+" + usdc("35.6"),
                price = usdc("50.5", minDecimals = 0),
            ),
        ),
    )

    private fun usdc(amount: String, minDecimals: Int? = null) = BigDecimal(amount).format {
        val usdc = crypto(symbol = "USDC", decimals = 6)
        if (minDecimals == null) usdc else usdc.anyDecimals(minDecimals = minDecimals)
    }
}