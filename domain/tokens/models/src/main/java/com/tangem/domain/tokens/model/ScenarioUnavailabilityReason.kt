package com.tangem.domain.tokens.model

sealed class ScenarioUnavailabilityReason {
    data object None : ScenarioUnavailabilityReason()

    // staking-specific
    data class StakingUnavailable(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    // send&sell-specific
    data class PendingTransaction(
        val withdrawalScenario: WithdrawalScenario,
        val networkName: String,
    ) : ScenarioUnavailabilityReason()
    data class EmptyBalance(val withdrawalScenario: WithdrawalScenario) : ScenarioUnavailabilityReason()

    // buy-specific
    data class BuyUnavailable(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    // swap-specific
    data class NotExchangeable(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    // sell-specific
    data class NotSupportedBySellService(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    data object Unreachable : ScenarioUnavailabilityReason()

    data object UnassociatedAsset : ScenarioUnavailabilityReason()

    enum class WithdrawalScenario {
        SELL, SEND // TODO staking create&process STAKING
    }
}