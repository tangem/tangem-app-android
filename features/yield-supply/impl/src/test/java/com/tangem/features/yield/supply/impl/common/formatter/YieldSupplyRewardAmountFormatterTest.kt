package com.tangem.features.yield.supply.impl.common.formatter

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply
import com.tangem.utils.StringsSigns.NON_BREAKING_SPACE
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyRewardAmountFormatterTest {

    private var originalLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun formatAmount(model: FormatModel) {
        // Arrange
        val reward = MockYieldSupply.reward(symbol = model.symbol, tokenDecimals = model.tokenDecimals)

        // Act
        val actual = reward.formatAmount(model.amount)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class FormatModel(
        val amount: BigInteger,
        val tokenDecimals: Int,
        val expected: String,
        val symbol: String = "WETH",
    )

    private fun provideTestModels() = listOf(
        FormatModel(
            amount = BigInteger("50000123456789123456789"),
            tokenDecimals = 18,
            expected = "WETH${NON_BREAKING_SPACE}50,000.12345678",
        ),
        FormatModel(
            amount = BigInteger("1999999999000000000"),
            tokenDecimals = 18,
            expected = "WETH${NON_BREAKING_SPACE}1.99999999",
        ),
        FormatModel(
            amount = BigInteger("1000000000"),
            tokenDecimals = 18,
            expected = "<${NON_BREAKING_SPACE}WETH${NON_BREAKING_SPACE}0.00000001",
        ),
        FormatModel(
            amount = BigInteger.ZERO,
            tokenDecimals = 18,
            expected = "WETH${NON_BREAKING_SPACE}0.00",
        ),
        FormatModel(
            amount = BigInteger("1234"),
            tokenDecimals = 2,
            expected = "WETH${NON_BREAKING_SPACE}12.34",
        ),
        FormatModel(
            amount = BigInteger("7"),
            tokenDecimals = 0,
            symbol = "MON",
            expected = "MON${NON_BREAKING_SPACE}7",
        ),
    )
}