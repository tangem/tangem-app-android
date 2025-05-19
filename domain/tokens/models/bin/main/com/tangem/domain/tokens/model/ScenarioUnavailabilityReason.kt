package com.tangem.domain.tokens.model

sealed class ScenarioUnavailabilityReason {
    data object None : ScenarioUnavailabilityReason()

    data object UsedOutdatedData : ScenarioUnavailabilityReason()

    data object DataLoading : ScenarioUnavailabilityReason()

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
    /**
     * False for this token in asset request
     */
    data class NotExchangeable(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    data class CustomToken(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    data class TokenNoQuotes(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    data object SingleWallet : ScenarioUnavailabilityReason()

    /**
     * Express request failed
     */
    data class ExpressUnreachable(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    /**
     * Assets still loading
     */
    data class ExpressLoading(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    /**
     * No token in asset list returned from express
     */
    data class AssetNotFound(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    // sell-specific
    data class NotSupportedBySellService(val cryptoCurrencyName: String) : ScenarioUnavailabilityReason()

    data object Unreachable : ScenarioUnavailabilityReason()

    data object UnassociatedAsset : ScenarioUnavailabilityReason()

    enum class WithdrawalScenario {
        SELL, SEND // TODO staking create&process STAKING
    }
}