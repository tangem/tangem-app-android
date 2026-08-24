package com.tangem.features.feed.earn.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.extensions.getActiveIconRes
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnType
import com.tangem.features.feed.earn.createEarnCurrency
import com.tangem.features.feed.earn.createEarnToken
import com.tangem.features.feed.earn.createEarnTokenWithCurrency
import com.tangem.features.feed.earn.impl.R
import org.junit.jupiter.api.Test

internal class EarnTokenWithCurrencyToMostlyUsedUMConverterTest {

    private var clicked: EarnTokenWithCurrency? = null

    private val converter = EarnTokenWithCurrencyToMostlyUsedUMConverter(onItemClick = { clicked = it })

    private val token = createEarnTokenWithCurrency(
        earnToken = createEarnToken(tokenName = "Ethereum", tokenSymbol = "ETH", type = EarnType.STAKING),
        cryptoCurrency = createEarnCurrency(
            currencyId = "coin-ethereum",
            iconUrl = "https://icons.tangem.com/eth.png",
        ),
    )

    @Test
    fun `GIVEN a token WHEN converted THEN the card id pairs the currency with the earn type`() {
        // Act
        val actual = converter.convert(token)

        // Assert — the same id scheme as the Best opportunities row, so a token can appear in both lists
        assertThat(actual.id).isEqualTo("coin-ethereum_STAKING")
    }

    @Test
    fun `GIVEN a token WHEN converted THEN the name symbol and type come from the earn token`() {
        // Act
        val actual = converter.convert(token)

        // Assert
        assertThat(actual.tokenName).isEqualTo(TextReference.Str("Ethereum"))
        assertThat(actual.symbol).isEqualTo(TextReference.Str("ETH"))
        assertThat(actual.earnType).isEqualTo(TextReference.Res(R.string.common_staking))
    }

    @Test
    fun `GIVEN a coin WHEN converted THEN the icon falls back to its network's icon`() {
        // Act
        val actual = converter.convert(token)

        // Assert
        assertThat(actual.currencyIconState).isEqualTo(
            CurrencyIconState.CoinIcon(
                url = "https://icons.tangem.com/eth.png",
                fallbackResId = getActiveIconRes(Blockchain.Ethereum),
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
        )
    }

    @Test
    fun `GIVEN a card WHEN it is clicked THEN the token behind it is reported`() {
        // Act
        converter.convert(token).onItemClick()

        // Assert
        assertThat(clicked).isEqualTo(token)
    }
}