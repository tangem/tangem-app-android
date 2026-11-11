package com.tangem.features.collectibles.impl.packopening.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.collectibles.models.OpenedCollectiblePack
import com.tangem.features.collectibles.impl.createOpenedPack
import com.tangem.features.collectibles.impl.packopening.CollectiblesConfirmOrderBottomSheetConfig
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesAutoBuybackConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a payout, a price and the sold card WHEN convert THEN the sheet shows them formatted`(
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
            input = input(amount = "42.5", price = "51", pack = null),
            expected = autoBuyback(amount = "42.5", price = "51", cardImageUrl = null, cardTitle = null),
        ),
        ConvertModel(
            input = input(amount = "35.6", price = "50.5", pack = null),
            expected = autoBuyback(amount = "35.6", price = "50.5", cardImageUrl = null, cardTitle = null),
        ),
        ConvertModel(
            input = input(pack = createOpenedPack()),
            expected = autoBuyback(cardImageUrl = "https://tangem.com/front.png", cardTitle = "2024 Series One #216"),
        ),
        ConvertModel(
            input = input(pack = createOpenedPack(imageFrontUrl = null, set = null)),
            expected = autoBuyback(cardImageUrl = null, cardTitle = "2024 #216"),
        ),
        ConvertModel(
            input = input(pack = createOpenedPack(year = null, set = null, serialNumber = null)),
            expected = autoBuyback(cardImageUrl = "https://tangem.com/front.png", cardTitle = null),
        ),
    )

    private fun input(amount: String = "42.5", price: String = "51", pack: OpenedCollectiblePack?) =
        CollectiblesAutoBuybackConverter.Input(amount = BigDecimal(amount), price = BigDecimal(price), pack = pack)

    private fun autoBuyback(amount: String = "42.5", price: String = "51", cardImageUrl: String?, cardTitle: String?) =
        CollectiblesConfirmOrderBottomSheetConfig.AutoBuyback(
            amount = "+" + usdc(amount),
            price = usdc(price, minDecimals = 0),
            cardImageUrl = cardImageUrl,
            cardTitle = cardTitle,
        )

    private fun usdc(amount: String, minDecimals: Int? = null) = BigDecimal(amount).format {
        val usdc = crypto(symbol = "USDC", decimals = 6, ignoreSymbolPosition = true)
        if (minDecimals == null) usdc else usdc.anyDecimals(minDecimals = minDecimals)
    }
}