package com.tangem.features.feed.earn.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnType
import com.tangem.features.feed.earn.createEarnCurrency
import com.tangem.features.feed.earn.createEarnToken
import com.tangem.features.feed.earn.createEarnTokenWithCurrency
import com.tangem.features.feed.earn.impl.R
import org.junit.jupiter.api.Test

/**
 * The row draws from two sources at once — the earn token and the currency behind it — so the assertions
 * pin which of the two each slot comes from.
 */
internal class EarnTokenWithCurrencyToTokenRowConverterTest {

    private var clicked: EarnTokenWithCurrency? = null

    private val converter = EarnTokenWithCurrencyToTokenRowConverter(onItemClick = { clicked = it })

    private val token = createEarnTokenWithCurrency(
        earnToken = createEarnToken(tokenName = "Ethereum", tokenSymbol = "ETH", type = EarnType.STAKING),
        cryptoCurrency = createEarnCurrency(
            currencyId = "coin-ethereum",
            iconUrl = "https://icons.tangem.com/eth.png",
        ),
        networkName = "Ethereum Network",
    )

    @Test
    fun `GIVEN a token WHEN converted THEN the row id pairs the currency with the earn type`() {
        // Act
        val actual = converter.convert(token)

        // Assert
        assertThat(actual.id).isEqualTo("coin-ethereum_STAKING")
    }

    @Test
    fun `GIVEN a token WHEN converted THEN the title is the token name and the quote is the network`() {
        // Act
        val actual = converter.convert(token)

        // Assert — the two are easy to swap, and the row reads wrong either way round
        assertThat(actual.title).isEqualTo(stringReference("Ethereum"))
        assertThat(actual.quote).isEqualTo(stringReference("Ethereum Network"))
    }

    @Test
    fun `GIVEN a token WHEN converted THEN the icon points at the currency's image`() {
        // Act
        val actual = converter.convert(token)

        // Assert
        assertThat(actual.icon).isEqualTo(
            TangemTokenIcon.UiState.Token(
                tokenState = TangemTokenIcon.State(url = "https://icons.tangem.com/eth.png"),
            ),
        )
    }

    @Test
    fun `GIVEN a staking token WHEN converted THEN the balance line names the earn type`() {
        // Act
        val actual = converter.convert(token)

        // Assert
        assertThat(actual.cryptoBalance).isEqualTo(TextReference.Res(R.string.common_staking))
    }

    @Test
    fun `GIVEN a yield token WHEN converted THEN the id and the balance line follow the type`() {
        // Arrange
        val yieldToken = createEarnTokenWithCurrency(earnToken = createEarnToken(type = EarnType.YIELD))

        // Act
        val actual = converter.convert(yieldToken)

        // Assert
        assertThat(actual.id).isEqualTo("coin-ethereum_YIELD")
        assertThat(actual.cryptoBalance).isEqualTo(TextReference.Res(R.string.common_yield_mode))
    }

    @Test
    fun `GIVEN a row WHEN it is clicked THEN the token behind it is reported`() {
        // Act
        converter.convert(token).onClick?.invoke()

        // Assert
        assertThat(clicked).isEqualTo(token)
    }
}