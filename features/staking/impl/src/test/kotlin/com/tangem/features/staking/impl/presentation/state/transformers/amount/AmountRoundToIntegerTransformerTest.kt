package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.amountData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

internal class AmountRoundToIntegerTransformerTest {

    private val tronStatus = currencyStatus(
        currency = coin(Blockchain.Tron),
        amount = BigDecimal("10"),
        fiatRate = BigDecimal("2"),
    )
    private lateinit var defaultLocale: Locale

    @BeforeEach
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN fractional amount WHEN transform THEN crypto and fiat rounded down`() {
        // Arrange
        val input = stakingUiState(
            amountState = amountData(BigDecimal("10.7"), value = "10.7", isWarning = true, cryptoDecimals = 6),
        )

        // Act
        val actual = field(AmountRoundToIntegerTransformer(tronStatus).transform(input))

        // Assert
        assertThat(actual.cryptoAmount.value).isEqualTo(BigDecimal("10"))
        assertThat(actual.fiatAmount.value).isEquivalentAccordingToCompareTo(BigDecimal("20"))
        assertThat(actual.value).isEqualTo("10")
        assertThat(actual.fiatValue).isEqualTo("20")
        assertThat(actual.isWarning).isFalse()
    }

    @Test
    fun `GIVEN empty text value WHEN transform THEN same state`() {
        // Arrange
        val input = stakingUiState(amountState = amountData(value = ""))

        // Act
        val actual = AmountRoundToIntegerTransformer(tronStatus).transform(input)

        // Assert
        assertThat(actual).isSameInstanceAs(input)
    }

    @Test
    fun `GIVEN amount state empty WHEN transform THEN same state`() {
        // Arrange
        val input = stakingUiState(amountState = AmountState.Empty)

        // Act
        val actual = AmountRoundToIntegerTransformer(tronStatus).transform(input)

        // Assert
        assertThat(actual).isSameInstanceAs(input)
    }

    @Test
    fun `GIVEN null crypto amount WHEN transform THEN values cleared`() {
        // Arrange
        val input = stakingUiState(amountState = amountData(cryptoAmount = null, value = "1"))

        // Act
        val actual = field(AmountRoundToIntegerTransformer(tronStatus).transform(input))

        // Assert
        assertThat(actual.value).isEmpty()
        assertThat(actual.fiatValue).isEmpty()
        assertThat(actual.cryptoAmount.value).isNull()
        assertThat(actual.fiatAmount.value).isNull()
    }

    // Pins current behaviour: a missing fiat rate is passed to BigDecimal.multiply and crashes.
    @Test
    fun `GIVEN no fiat rate WHEN transform THEN null pointer exception`() {
        // Arrange
        val status: CryptoCurrencyStatus = currencyStatus(coin(Blockchain.Tron), fiatRate = null)
        val input = stakingUiState(amountState = amountData(BigDecimal("10.7"), value = "10.7"))

        // Act
        val error = runCatching { AmountRoundToIntegerTransformer(status).transform(input) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(NullPointerException::class.java)
    }

    private fun field(state: StakingUiState) = (state.amountState as AmountState.Data).amountTextField
}