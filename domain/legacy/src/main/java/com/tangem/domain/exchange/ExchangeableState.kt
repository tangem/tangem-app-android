package com.tangem.domain.exchange

sealed interface ExchangeableState {
    data object Exchangeable : ExchangeableState
    data object NotExchangeable : ExchangeableState
    data object Loading : ExchangeableState
    data object Error : ExchangeableState
    data object AssetNotFound : ExchangeableState
}