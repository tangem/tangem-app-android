package com.tangem.common.ui.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.StringsSigns
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

internal class SwapRateFormatterTest {

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

    @Test
    fun `GIVEN coin to stable swap WHEN formatRate THEN base is coin`() {
        val eth = coin(symbol = "ETH", decimals = 18)
        val usdt = stable(symbol = "USDT", decimals = 6)

        val result = SwapRateFormatter.formatRate(
            from = eth,
            to = usdt,
            fromAmount = BigDecimal.ONE,
            toAmount = BigDecimal("3000"),
        )

        result.assertOrder(base = "ETH", quote = "USDT")
        assertThat(result).contains("3,000")
    }

    @Test
    fun `GIVEN stable to coin swap WHEN formatRate THEN base is coin`() {
        val usdt = stable(symbol = "USDT", decimals = 6)
        val eth = coin(symbol = "ETH", decimals = 18)

        val result = SwapRateFormatter.formatRate(
            from = usdt,
            to = eth,
            fromAmount = BigDecimal("3000"),
            toAmount = BigDecimal.ONE,
        )

        result.assertOrder(base = "ETH", quote = "USDT")
        assertThat(result).contains("3,000")
    }

    @Test
    fun `GIVEN btc to other coin swap WHEN formatRate THEN base is other coin`() {
        val btc = coin(symbol = "BTC", decimals = 8)
        val sol = coin(symbol = "SOL", decimals = 8)

        val result = SwapRateFormatter.formatRate(
            from = btc,
            to = sol,
            fromAmount = BigDecimal.ONE,
            toAmount = BigDecimal("20"),
        )

        result.assertOrder(base = "SOL", quote = "BTC")
        assertThat(result).contains("0.05")
    }

    @Test
    fun `GIVEN btc and eth swap WHEN formatRate THEN base is eth`() {
        val btc = coin(symbol = "BTC", decimals = 8)
        val eth = coin(symbol = "ETH", decimals = 18)

        val result = SwapRateFormatter.formatRate(
            from = btc,
            to = eth,
            fromAmount = BigDecimal.ONE,
            toAmount = BigDecimal("18"),
        )

        result.assertOrder(base = "ETH", quote = "BTC")
        assertThat(result).contains("0.05555")
    }

    @Test
    fun `GIVEN two stables swap WHEN formatRate THEN base is higher ranked`() {
        val usdc = stable(symbol = "USDC", decimals = 6)
        val dai = stable(symbol = "DAI", decimals = 18)

        val result = SwapRateFormatter.formatRate(
            from = dai,
            to = usdc,
            fromAmount = BigDecimal.ONE,
            toAmount = BigDecimal("0.999"),
        )

        result.assertOrder(base = "USDC", quote = "DAI")
        assertThat(result).contains("1.001")
    }

    @Test
    fun `GIVEN two non-major coins swap WHEN formatRate THEN base is to currency`() {
        val sol = coin(symbol = "SOL", decimals = 8)
        val trx = coin(symbol = "TRX", decimals = 6)

        val result = SwapRateFormatter.formatRate(
            from = sol,
            to = trx,
            fromAmount = BigDecimal.ONE,
            toAmount = BigDecimal("100"),
        )

        result.assertOrder(base = "TRX", quote = "SOL")
        assertThat(result).contains("0.01")
    }

    @Test
    fun `GIVEN zero from amount WHEN formatRate THEN rate is zero`() {
        val eth = coin(symbol = "ETH", decimals = 18)
        val usdt = stable(symbol = "USDT", decimals = 6)

        val result = SwapRateFormatter.formatRate(
            from = eth,
            to = usdt,
            fromAmount = BigDecimal.ZERO,
            toAmount = BigDecimal.ZERO,
        )

        result.assertOrder(base = "ETH", quote = "USDT")
        assertThat(result).contains("0.00")
    }

    private fun String.assertOrder(base: String, quote: String) {
        val baseIndex = indexOf(base)
        val quoteIndex = lastIndexOf(quote)
        assertThat(baseIndex).isAtLeast(0)
        assertThat(quoteIndex).isGreaterThan(baseIndex)
        assertThat(this).contains(StringsSigns.APPROXIMATE)
        val approximateIndex = indexOf(StringsSigns.APPROXIMATE)
        assertThat(approximateIndex).isGreaterThan(baseIndex)
        assertThat(quoteIndex).isGreaterThan(approximateIndex)
    }

    private fun coin(symbol: String, decimals: Int): CryptoCurrency = mockk<CryptoCurrency.Coin> {
        every { this@mockk.symbol } returns symbol
        every { this@mockk.decimals } returns decimals
    }

    private fun stable(symbol: String, decimals: Int): CryptoCurrency = mockk<CryptoCurrency.Token> {
        every { this@mockk.symbol } returns symbol
        every { this@mockk.decimals } returns decimals
    }
}