package com.tangem.features.feed.earn.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnType
import com.tangem.features.feed.earn.createEarnCurrency
import com.tangem.features.feed.earn.createEarnToken
import com.tangem.features.feed.earn.createEarnTokenCurrency
import com.tangem.features.feed.earn.createEarnTokenWithCurrency
import com.tangem.features.feed.earn.impl.R
import org.junit.jupiter.api.Test
import com.tangem.core.ui.R as CoreUiR

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
    fun `GIVEN a token WHEN converted THEN the row id is the raw token network and earn type`() {
        // Act
        val actual = converter.convert(token)

        // Assert
        assertThat(actual.id).isEqualTo("ethereum_ethereum_STAKING")
    }

    @Test
    fun `GIVEN one token on three networks WHEN converted THEN the row ids are distinct`() {
        // Arrange — the real catalogue lists usd-coin on base, ethereum and avalanche, and all three land
        // in one list, where a repeated key is a crash
        val rows = listOf("base", "ethereum", "avalanche").map { networkId ->
            createEarnTokenWithCurrency(
                earnToken = createEarnToken(tokenId = "usd-coin", networkId = networkId, type = EarnType.YIELD),
            )
        }

        // Act
        val actual = rows.map(converter::convert)

        // Assert
        assertThat(actual.map { it.id }).containsExactly(
            "usd-coin_base_YIELD",
            "usd-coin_ethereum_YIELD",
            "usd-coin_avalanche_YIELD",
        )
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
    fun `GIVEN a native coin WHEN converted THEN the icon points at the currency's image without a badge`() {
        // Act
        val actual = converter.convert(token)

        // Assert — the coin icon already is the network icon, so a badge on top of it is noise
        assertThat(actual.icon).isEqualTo(
            TangemTokenIcon.UiState.Token(
                tokenState = TangemTokenIcon.State(url = "https://icons.tangem.com/eth.png"),
            ),
        )
    }

    @Test
    fun `GIVEN a token WHEN converted THEN the icon carries the network badge`() {
        // Arrange
        val tether = createEarnTokenWithCurrency(cryptoCurrency = createEarnTokenCurrency())

        // Act
        val actual = converter.convert(tether)

        // Assert
        assertThat(actual.icon).isEqualTo(
            TangemTokenIcon.UiState.Token(
                tokenState = TangemTokenIcon.State(
                    url = "https://icons.tangem.com/usdt.png",
                    topIcon = CoreUiR.drawable.img_eth_22,
                ),
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
        assertThat(actual.id).isEqualTo("ethereum_ethereum_YIELD")
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