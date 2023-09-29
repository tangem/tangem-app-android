package com.tangem.feature.wallet.presentation.organizetokens.utils.converter

internal class TokenItemHiddenStateConverter {

    fun updateHiddenState(wasBalanceHidden: Boolean, isBalanceHidden: Boolean): Boolean {
        return when {
            !wasBalanceHidden && isBalanceHidden -> true
            wasBalanceHidden && !isBalanceHidden -> false
            else -> wasBalanceHidden
        }
    }
}