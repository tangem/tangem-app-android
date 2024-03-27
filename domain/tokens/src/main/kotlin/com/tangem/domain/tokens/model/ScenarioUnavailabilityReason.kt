package com.tangem.domain.tokens.model

sealed class ScenarioUnavailabilityReason {
    data object None : ScenarioUnavailabilityReason()

    // send-specific
    data class PendingTransaction(val cryptoCurrencySymbol: String) : ScenarioUnavailabilityReason()
    data object EmptyBalance : ScenarioUnavailabilityReason()
    data object InsufficientFundsForFee : ScenarioUnavailabilityReason() // if token

    // buy-specific
    data class BuyUnavailable(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    // swap-specific
    data class NotExchangeable(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    // sell-specific
    data class SellUnavailable(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    data object NoQuotes : ScenarioUnavailabilityReason()
}