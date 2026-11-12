package com.tangem.domain.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.test.core.ProvideTestModels
import java.math.BigDecimal

/**
 * Pins which sends go through the Yield Module: a `TokenYieldSupply` amount makes the transaction a module call to the
 * module address, so an inactive position (or a wallet without a module) must stay a plain token transfer.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ConvertToSdkAmountTest {

    private val currency: CryptoCurrency.Token = mockk {
        every { symbol } returns "USDC"
        every { decimals } returns 6
        every { contractAddress } returns "0xUSDC"
    }
    private val sdkToken = Token(symbol = "USDC", contractAddress = "0xUSDC", decimals = 6)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the yield position of the token WHEN convertToSdkAmount THEN module send only for active yield`(
        model: AmountTypeModel,
    ) {
        // Arrange
        val status = CryptoCurrencyStatus(currency = currency, value = loaded(model.contributions))

        // Act
        val actual = BigDecimal.ONE.convertToSdkAmount(status).type

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        AmountTypeModel(
            name = "no yield contribution",
            contributions = emptyList(),
            expected = AmountType.Token(token = sdkToken),
        ),
        AmountTypeModel(
            name = "wallet without a Yield Module",
            contributions = listOf(contribution(isActive = false, isInitialized = false, isAllowedToSpend = false)),
            expected = AmountType.Token(token = sdkToken),
        ),
        AmountTypeModel(
            name = "initialized but inactive",
            contributions = listOf(contribution(isActive = false, isInitialized = true, isAllowedToSpend = true)),
            expected = AmountType.Token(token = sdkToken),
        ),
        AmountTypeModel(
            name = "active yield",
            contributions = listOf(contribution(isActive = true, isInitialized = true, isAllowedToSpend = true)),
            expected = AmountType.TokenYieldSupply(
                token = sdkToken,
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
            ),
        ),
    )

    data class AmountTypeModel(
        val name: String,
        val contributions: List<YieldSupplyContribution>,
        val expected: AmountType,
    ) {
        override fun toString(): String = name
    }

    private fun contribution(isActive: Boolean, isInitialized: Boolean, isAllowedToSpend: Boolean) =
        YieldSupplyContribution(
            status = YieldSupplyStatus(
                isActive = isActive,
                isInitialized = isInitialized,
                isAllowedToSpend = isAllowedToSpend,
                effectiveProtocolBalance = if (isActive) BigDecimal("7") else null,
            ),
            source = StatusSource.ACTUAL,
        )

    private fun loaded(contributions: List<YieldSupplyContribution>) = CryptoCurrencyStatus.Loaded(
        amount = BigDecimal.TEN,
        fiatAmount = BigDecimal.TEN,
        fiatRate = BigDecimal.ONE,
        priceChange = BigDecimal.ZERO,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = mockk(relaxed = true),
        sources = CryptoCurrencyStatus.Sources(),
        contributions = contributions,
    )
}