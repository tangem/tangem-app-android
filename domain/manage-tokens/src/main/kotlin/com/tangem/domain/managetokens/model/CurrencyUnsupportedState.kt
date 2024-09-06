package com.tangem.domain.managetokens.model

sealed class CurrencyUnsupportedState {

    abstract val networkName: String
    sealed class Token : CurrencyUnsupportedState() {
        data class NetworkTokensUnsupported(override val networkName: String) : Token()
        data class UnsupportedCurve(override val networkName: String) : Token()
    }

    data class UnsupportedNetwork(override val networkName: String) : CurrencyUnsupportedState()
}