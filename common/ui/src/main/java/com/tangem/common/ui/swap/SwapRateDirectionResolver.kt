package com.tangem.common.ui.swap

import com.tangem.domain.models.currency.CryptoCurrency
import java.util.Locale

/**
 * Resolves which currency goes first (base) and which goes second (quote) when displaying an
 * exchange rate for a swap pair ([REDACTED_TASK_KEY]).
 *
 * Categories used by the rules:
 *  - **Stable** — a [CryptoCurrency.Token] whose symbol is in [STABLECOIN_RANKS].
 *  - **Coin**   — a [CryptoCurrency.Coin] (any native coin: BTC, ETH, SOL, TRX, ...).
 *  - Anything else (a [CryptoCurrency.Token] outside the stable list) falls into the default
 *    branch and is treated as a regular token.
 *
 * Rules:
 *  - Stable ↔ Stable: base = the one ranked higher in [STABLECOIN_RANKS].
 *  - Coin ↔ Stable / Stable ↔ Coin: base is the coin.
 *  - Coin ↔ Coin with BTC or ETH: base is the other coin, quote is BTC/ETH.
 *  - ETH ↔ BTC (both directions): base = ETH, quote = BTC.
 *  - Otherwise (regular Coin↔Coin, any pair involving a non-stable Token): base = TO, quote = FROM.
 */
internal object SwapRateDirectionResolver {

    private val STABLECOIN_RANKS: Map<String, Int> = listOf(
        "USDT", "USDC", "USDe", "DAI", "USD1", "PYUSD", "RLUSD", "USDG", "USDf", "USDD",
    ).withIndex().associate { (rank, symbol) -> symbol.uppercase(Locale.ROOT) to rank }

    private const val BTC_SYMBOL = "BTC"
    private const val ETH_SYMBOL = "ETH"

    fun resolve(from: CryptoCurrency, to: CryptoCurrency): SwapRateDirection {
        val isFromStable = from.isStable()
        val isToStable = to.isStable()

        return when {
            isFromStable && isToStable -> resolveStableToStable(from, to)
            isFromStable -> SwapRateDirection(base = to, quote = from)
            isToStable -> SwapRateDirection(base = from, quote = to)
            from.isCoin() && to.isCoin() -> resolveCoinToCoin(from, to)
            else -> SwapRateDirection(base = to, quote = from)
        }
    }

    private fun resolveStableToStable(from: CryptoCurrency, to: CryptoCurrency): SwapRateDirection {
        val fromRank = stableRank(from.symbol.uppercaseRoot())
        val toRank = stableRank(to.symbol.uppercaseRoot())
        return if (fromRank <= toRank) {
            SwapRateDirection(base = from, quote = to)
        } else {
            SwapRateDirection(base = to, quote = from)
        }
    }

    private fun resolveCoinToCoin(from: CryptoCurrency, to: CryptoCurrency): SwapRateDirection {
        val fromSymbol = from.symbol.uppercaseRoot()
        val toSymbol = to.symbol.uppercaseRoot()
        val isFromBtcOrEth = fromSymbol.isBtcOrEth()
        val isToBtcOrEth = toSymbol.isBtcOrEth()

        return when {
            isFromBtcOrEth && isToBtcOrEth -> resolveBtcEth(from, to, fromSymbol)
            isFromBtcOrEth -> SwapRateDirection(base = to, quote = from)
            isToBtcOrEth -> SwapRateDirection(base = from, quote = to)
            else -> SwapRateDirection(base = to, quote = from)
        }
    }

    private fun resolveBtcEth(from: CryptoCurrency, to: CryptoCurrency, fromSymbol: String): SwapRateDirection {
        return if (fromSymbol == ETH_SYMBOL) {
            SwapRateDirection(base = from, quote = to)
        } else {
            SwapRateDirection(base = to, quote = from)
        }
    }

    private fun stableRank(symbol: String): Int = STABLECOIN_RANKS[symbol] ?: Int.MAX_VALUE

    private fun CryptoCurrency.isStable(): Boolean {
        return this is CryptoCurrency.Token && STABLECOIN_RANKS.containsKey(symbol.uppercaseRoot())
    }

    private fun CryptoCurrency.isCoin(): Boolean = this is CryptoCurrency.Coin

    private fun String.isBtcOrEth(): Boolean = this == BTC_SYMBOL || this == ETH_SYMBOL

    private fun String.uppercaseRoot(): String = uppercase(Locale.ROOT)
}

internal data class SwapRateDirection(val base: CryptoCurrency, val quote: CryptoCurrency)