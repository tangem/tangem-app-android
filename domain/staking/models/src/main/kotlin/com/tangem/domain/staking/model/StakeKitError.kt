package com.tangem.domain.staking.model

sealed class StakeKitError {

    data class MinimumAmountNotReachedError(val amount: String) : StakeKitError()

    data class MissingArgumentsError(val arguments: String) : StakeKitError()

    data class YieldUnderMaintenanceError(val yieldId: String) : StakeKitError()

    data object InsufficientFundsError : StakeKitError()

    data object StakedPositionNotFoundError : StakeKitError()

    data object InvalidAmountSubmittedError : StakeKitError()

    data object BalanceUnavailableError : StakeKitError()

    data object GasPriceUnavailableError : StakeKitError()

    data object NotImplementedError : StakeKitError()

    data object TokenNotFoundError : StakeKitError()

    data object BroadcastTransactionError : StakeKitError()

    data object MissingGasPriceStrategyError : StakeKitError()

    data object SubstrateMalformedTransactionHashError : StakeKitError()

    data object TronMaximumAmountOfValidatorsExceededError : StakeKitError()

    data object SubstratePoolNotFoundError : StakeKitError()

    data object SubstrateBondedAmountTooLowError : StakeKitError()

    data object TronMissingResourceTypeArgumentError : StakeKitError()

    data object AaveV3PoolFrozenError : StakeKitError()

    data object AaveV3TokenPairNotFoundError : StakeKitError()

    data object YearnVaultAtMaxCapacityError : StakeKitError()

    data object StETHNoWithdrawalRequestsFoundError : StakeKitError()

    data object MorphoLendingPoolPausedError : StakeKitError()

    data object NonceUnavailableError : StakeKitError()

    data object CosmosAcccountNotFoundError : StakeKitError()

    data object AvalancheMissingAdditionalAddressesArgumentError : StakeKitError()

    data object AvalancheValidatorInfoNotFoundError : StakeKitError()

    data object SolanaTransactionSignatureVerificationFailureError : StakeKitError()

    data object SolanaUnableTocreateStakeAccountError : StakeKitError()

    data object SolanaStakeAmountTooLowError : StakeKitError()

    data object SolanaUnstakeAmountTooLowError : StakeKitError()

    data object SolanaStakeAccountsNotFoundError : StakeKitError()

    data object SolanaEligibleStakeAccountsNotFoundError : StakeKitError()

    data object TezosNoBalanceDelegatedError : StakeKitError()

    data object TezosMissingPubkeyArgumentError : StakeKitError()

    data object TezosEstimateRevealGasLimitError : StakeKitError()

    data object TezosBalanceAlreadyDelegatedError : StakeKitError()

    data object BinanceAccountNotFoundError : StakeKitError()

    data object BinanceMissingAccountNumberOrSequenceError : StakeKitError()

    data object GRTStakingDisabledError : StakeKitError()

    data object GRTStakingDisabledLedgerLiveError : StakeKitError()

    data object UnknownError : StakeKitError()
}
