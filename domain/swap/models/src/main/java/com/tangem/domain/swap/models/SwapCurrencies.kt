package com.tangem.domain.swap.models

import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrencyStatus

/**
 * Model of currencies available to swap
 *
 * @param fromGroup group of currencies available swap from
 * @param toGroup group of currencies available swap to
 */
data class SwapCurrencies(
    val fromGroup: SwapCurrenciesGroup,
    val toGroup: SwapCurrenciesGroup,
) {
    companion object {
        val EMPTY = SwapCurrencies(
            fromGroup = SwapCurrenciesGroup(emptyList(), emptyList(), false),
            toGroup = SwapCurrenciesGroup(emptyList(), emptyList(), false),
        )
    }
}

/**
 * Return swap group depending on [swapDirection]
 */
fun SwapCurrencies.getGroupWithDirection(swapDirection: SwapDirection): SwapCurrenciesGroup {
    return when (swapDirection) {
        SwapDirection.Reverse -> toGroup
        SwapDirection.Direct -> fromGroup
    }
}

/**
 * Swap group model
 *
 * @param available list of available currencies to swap
 * @param available list of unavailable currencies to swap
 * @param isAfterSearch flag indicates whether user searched token
 */
data class SwapCurrenciesGroup(
    val available: List<SwapCryptoCurrency>,
    val unavailable: List<SwapCryptoCurrency>,
    val isAfterSearch: Boolean,
)

/**
 * Crypto currency with providers to swap
 */
data class SwapCryptoCurrency(
    val currencyStatus: CryptoCurrencyStatus,
    val providers: List<ExpressProvider>,
)

/**
 * Get initial rate type based on available providers
 */
fun List<ExpressProvider>.getInitialRateType(): ExpressRateType {
    val availableRateTypes = this.flatMap { it.rateTypes }.toSet()
    return if (availableRateTypes.contains(ExpressRateType.Fixed)) {
        ExpressRateType.Fixed
    } else {
        ExpressRateType.Float
    }
}