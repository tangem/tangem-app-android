package com.tangem.tap.features.onboarding.products.wallet.saltPay

import com.tangem.common.Filter

/**
 * Created by Anton Zhilenkov on 08.10.2022.
 */
interface PinFilter : Filter<String>

class AllSymbolsTheSameFilter : PinFilter {
    override fun filter(value: String): Boolean {
        if (value.isEmpty()) return false

        val array = value.toCharArray()
        val firstSymbol = array[0]
        return array.all { it == firstSymbol }
    }
}
