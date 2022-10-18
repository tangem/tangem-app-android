package com.tangem.tap.features.onboarding.products.wallet.saltPay

import com.tangem.common.Filter

/**
[REDACTED_AUTHOR]
 */
abstract class PinFilter : Filter<String>

class AllSymbolsTheSameFilter : PinFilter() {
    override fun filter(value: String): Boolean {
        if (value.isEmpty()) return false

        val array = value.toCharArray()
        val firstSymbol = array[0]
        return array.all { it == firstSymbol }
    }
}