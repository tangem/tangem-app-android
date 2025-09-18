package com.tangem.feature.tokendetails.presentation.tokendetails.state

sealed class TokenDetailsYieldSupplyState {

    data object Empty : TokenDetailsYieldSupplyState()

    data class Active(val yieldInfoClick: (() -> Unit)) : TokenDetailsYieldSupplyState()
}