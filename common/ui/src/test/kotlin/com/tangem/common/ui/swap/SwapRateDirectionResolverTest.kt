package com.tangem.common.ui.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class SwapRateDirectionResolverTest {

    @Test
    fun `GIVEN stable usdt and stable usdc WHEN resolve THEN base is usdt`() {
        val usdt = stable(symbol = "USDT")
        val usdc = stable(symbol = "USDC")

        val result = SwapRateDirectionResolver.resolve(from = usdt, to = usdc)

        assertThat(result).isEqualTo(SwapRateDirection(base = usdt, quote = usdc))
    }

    @Test
    fun `GIVEN stable dai and stable usdt WHEN resolve THEN base is usdt`() {
        val dai = stable(symbol = "DAI")
        val usdt = stable(symbol = "USDT")

        val result = SwapRateDirectionResolver.resolve(from = dai, to = usdt)

        assertThat(result).isEqualTo(SwapRateDirection(base = usdt, quote = dai))
    }

    @Test
    fun `GIVEN stable usdd and stable usdc WHEN resolve THEN base is usdc`() {
        val usdd = stable(symbol = "USDD")
        val usdc = stable(symbol = "USDC")

        val result = SwapRateDirectionResolver.resolve(from = usdd, to = usdc)

        assertThat(result).isEqualTo(SwapRateDirection(base = usdc, quote = usdd))
    }

    @Test
    fun `GIVEN coin and stable WHEN resolve THEN base is coin`() {
        val sol = coin(symbol = "SOL")
        val usdt = stable(symbol = "USDT")

        val result = SwapRateDirectionResolver.resolve(from = sol, to = usdt)

        assertThat(result).isEqualTo(SwapRateDirection(base = sol, quote = usdt))
    }

    @Test
    fun `GIVEN stable and coin WHEN resolve THEN base is coin`() {
        val usdt = stable(symbol = "USDT")
        val sol = coin(symbol = "SOL")

        val result = SwapRateDirectionResolver.resolve(from = usdt, to = sol)

        assertThat(result).isEqualTo(SwapRateDirection(base = sol, quote = usdt))
    }

    @Test
    fun `GIVEN coin and btc WHEN resolve THEN base is coin and quote is btc`() {
        val sol = coin(symbol = "SOL")
        val btc = coin(symbol = "BTC")

        val result = SwapRateDirectionResolver.resolve(from = sol, to = btc)

        assertThat(result).isEqualTo(SwapRateDirection(base = sol, quote = btc))
    }

    @Test
    fun `GIVEN btc and coin WHEN resolve THEN base is coin and quote is btc`() {
        val btc = coin(symbol = "BTC")
        val trx = coin(symbol = "TRX")

        val result = SwapRateDirectionResolver.resolve(from = btc, to = trx)

        assertThat(result).isEqualTo(SwapRateDirection(base = trx, quote = btc))
    }

    @Test
    fun `GIVEN coin and eth WHEN resolve THEN base is coin and quote is eth`() {
        val sol = coin(symbol = "SOL")
        val eth = coin(symbol = "ETH")

        val result = SwapRateDirectionResolver.resolve(from = sol, to = eth)

        assertThat(result).isEqualTo(SwapRateDirection(base = sol, quote = eth))
    }

    @Test
    fun `GIVEN eth and coin WHEN resolve THEN base is coin and quote is eth`() {
        val eth = coin(symbol = "ETH")
        val sol = coin(symbol = "SOL")

        val result = SwapRateDirectionResolver.resolve(from = eth, to = sol)

        assertThat(result).isEqualTo(SwapRateDirection(base = sol, quote = eth))
    }

    @Test
    fun `GIVEN btc and eth WHEN resolve THEN base is eth and quote is btc`() {
        val btc = coin(symbol = "BTC")
        val eth = coin(symbol = "ETH")

        val result = SwapRateDirectionResolver.resolve(from = btc, to = eth)

        assertThat(result).isEqualTo(SwapRateDirection(base = eth, quote = btc))
    }

    @Test
    fun `GIVEN eth and btc WHEN resolve THEN base is eth and quote is btc`() {
        val eth = coin(symbol = "ETH")
        val btc = coin(symbol = "BTC")

        val result = SwapRateDirectionResolver.resolve(from = eth, to = btc)

        assertThat(result).isEqualTo(SwapRateDirection(base = eth, quote = btc))
    }

    @Test
    fun `GIVEN two non-major coins WHEN resolve THEN base is to and quote is from`() {
        val sol = coin(symbol = "SOL")
        val trx = coin(symbol = "TRX")

        val result = SwapRateDirectionResolver.resolve(from = sol, to = trx)

        assertThat(result).isEqualTo(SwapRateDirection(base = trx, quote = sol))
    }

    @Test
    fun `GIVEN token symbol matching priority list but not Token type WHEN resolve THEN treated as coin`() {
        // Edge: a CryptoCurrency.Coin whose symbol coincidentally equals a stable symbol must NOT
        // be treated as stable — the type check is type-aware now.
        val usdtLikeCoin = coin(symbol = "USDT")
        val usdt = stable(symbol = "USDT")

        val result = SwapRateDirectionResolver.resolve(from = usdtLikeCoin, to = usdt)

        // usdt is stable, usdtLikeCoin is a Coin → Coin↔Stable rule, base = coin
        assertThat(result).isEqualTo(SwapRateDirection(base = usdtLikeCoin, quote = usdt))
    }

    @Test
    fun `GIVEN non-stable token and coin WHEN resolve THEN base is to and quote is from`() {
        // Non-stable Token (e.g., LINK) is neither Stable nor Coin → falls into default branch.
        val link = token(symbol = "LINK")
        val eth = coin(symbol = "ETH")

        val result = SwapRateDirectionResolver.resolve(from = link, to = eth)

        assertThat(result).isEqualTo(SwapRateDirection(base = eth, quote = link))
    }

    @Test
    fun `GIVEN two non-stable tokens WHEN resolve THEN base is to and quote is from`() {
        val link = token(symbol = "LINK")
        val aave = token(symbol = "AAVE")

        val result = SwapRateDirectionResolver.resolve(from = link, to = aave)

        assertThat(result).isEqualTo(SwapRateDirection(base = aave, quote = link))
    }

    @Test
    fun `GIVEN lowercase usdt and lowercase usdc WHEN resolve THEN base is usdt`() {
        val usdt = stable(symbol = "usdt")
        val usdc = stable(symbol = "usdc")

        val result = SwapRateDirectionResolver.resolve(from = usdt, to = usdc)

        assertThat(result).isEqualTo(SwapRateDirection(base = usdt, quote = usdc))
    }

    @Test
    fun `GIVEN stable usdt and bridged usdc_e WHEN resolve THEN base is usdt`() {
        val usdt = stable(symbol = "USDT")
        val usdcE = stable(symbol = "USDC.E")

        val result = SwapRateDirectionResolver.resolve(from = usdt, to = usdcE)

        assertThat(result).isEqualTo(SwapRateDirection(base = usdt, quote = usdcE))
    }

    @Test
    fun `GIVEN bridged usdc_e and stable usdt WHEN resolve THEN base is usdt`() {
        val usdcE = stable(symbol = "USDC.E")
        val usdt = stable(symbol = "USDT")

        val result = SwapRateDirectionResolver.resolve(from = usdcE, to = usdt)

        assertThat(result).isEqualTo(SwapRateDirection(base = usdt, quote = usdcE))
    }

    @Test
    fun `GIVEN coin sol and bridged usdc_e WHEN resolve THEN base is coin`() {
        val sol = coin(symbol = "SOL")
        val usdcE = stable(symbol = "USDC.E")

        val result = SwapRateDirectionResolver.resolve(from = sol, to = usdcE)

        assertThat(result).isEqualTo(SwapRateDirection(base = sol, quote = usdcE))
    }

    @Test
    fun `GIVEN bridged usdc_e and coin sol WHEN resolve THEN base is coin`() {
        val usdcE = stable(symbol = "USDC.E")
        val sol = coin(symbol = "SOL")

        val result = SwapRateDirectionResolver.resolve(from = usdcE, to = sol)

        assertThat(result).isEqualTo(SwapRateDirection(base = sol, quote = usdcE))
    }

    @Test
    fun `GIVEN lowercase bridged usdt_e and stable usdc WHEN resolve THEN base is usdt`() {
        val usdtE = stable(symbol = "usdt.e")
        val usdc = stable(symbol = "USDC")

        val result = SwapRateDirectionResolver.resolve(from = usdtE, to = usdc)

        assertThat(result).isEqualTo(SwapRateDirection(base = usdtE, quote = usdc))
    }

    @Test
    fun `GIVEN bridged dai_e and bridged usdc_e WHEN resolve THEN base is usdc`() {
        val daiE = stable(symbol = "DAI.E")
        val usdcE = stable(symbol = "USDC.E")

        val result = SwapRateDirectionResolver.resolve(from = daiE, to = usdcE)

        assertThat(result).isEqualTo(SwapRateDirection(base = usdcE, quote = daiE))
    }

    private fun coin(symbol: String): CryptoCurrency = mockk<CryptoCurrency.Coin> {
        every { this@mockk.symbol } returns symbol
    }

    private fun token(symbol: String): CryptoCurrency = mockk<CryptoCurrency.Token> {
        every { this@mockk.symbol } returns symbol
    }

    private fun stable(symbol: String): CryptoCurrency = token(symbol)
}