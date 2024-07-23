package com.tangem.domain.staking.model.stakekit

sealed class StakingError {

    // region stakekit errors

    data class MinimumAmountNotReachedError(val amount: String) : StakingError()

    data class MissingArgumentsError(val arguments: String) : StakingError()

    data class YieldUnderMaintenanceError(val yieldId: String) : StakingError()

    data object InsufficientFundsError : StakingError()

    data object StakedPositionNotFoundError : StakingError()

    data object InvalidAmountSubmittedError : StakingError()

    data object BalanceUnavailableError : StakingError()

    data object GasPriceUnavailableError : StakingError()

    data object NotImplementedError : StakingError()

    data object TokenNotFoundError : StakingError()

    data object BroadcastTransactionError : StakingError()

    data object MissingGasPriceStrategyError : StakingError()

    data object SubstrateMalformedTransactionHashError : StakingError()

    data object TronMaximumAmountOfValidatorsExceededError : StakingError()

    data object SubstratePoolNotFoundError : StakingError()

    data object SubstrateBondedAmountTooLowError : StakingError()

    data object TronMissingResourceTypeArgumentError : StakingError()

    data object AaveV3PoolFrozenError : StakingError()

    data object AaveV3TokenPairNotFoundError : StakingError()

    data object YearnVaultAtMaxCapacityError : StakingError()

    data object StETHNoWithdrawalRequestsFoundError : StakingError()

    data object MorphoLendingPoolPausedError : StakingError()

    data object NonceUnavailableError : StakingError()

    data object CosmosAcccountNotFoundError : StakingError()

    data object AvalancheMissingAdditionalAddressesArgumentError : StakingError()

    data object AvalancheValidatorInfoNotFoundError : StakingError()

    data object SolanaTransactionSignatureVerificationFailureError : StakingError()

    data object SolanaUnableTocreateStakeAccountError : StakingError()

    data object SolanaStakeAmountTooLowError : StakingError()

    data object SolanaUnstakeAmountTooLowError : StakingError()

    data object SolanaStakeAccountsNotFoundError : StakingError()

    data object SolanaEligibleStakeAccountsNotFoundError : StakingError()

    data object TezosNoBalanceDelegatedError : StakingError()

    data object TezosMissingPubkeyArgumentError : StakingError()

    data object TezosEstimateRevealGasLimitError : StakingError()

    data object TezosBalanceAlreadyDelegatedError : StakingError()

    data object BinanceAccountNotFoundError : StakingError()

    data object BinanceMissingAccountNumberOrSequenceError : StakingError()

    data object GRTStakingDisabledError : StakingError()

    data object GRTStakingDisabledLedgerLiveError : StakingError()

    data class UnavailableDueToGeolocationError(
        val tags: List<String>,
    ) : StakingError()

    // endregion

    data class DataError(val cause: Throwable) : StakingError()

    data object UnknownError : StakingError()
}