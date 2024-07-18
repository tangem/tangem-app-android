package com.tangem.datasource.api.stakekit.models.response.model.error

import com.squareup.moshi.Json

class StakeKitErrorResponse(
    @Json(name = "details")
    val details: StakeKitErrorDetailsDTO? = null,
    @Json(name = "message")
    val message: StakeKitErrorMessageDTO? = null,
    @Json(name = "level")
    val level: String? = null, // unused

    // 403
    @Json(name = "type")
    val type: AccessDeniedErrorTypeDTO? = null,
    @Json(name = "code")
    val code: String? = null,
    @Json(name = "countryCode")
    val countryCode: String,
    @Json(name = "regionCode")
    val regionCode: String? = null,
    @Json(name = "tags")
    val tags: List<String>? = null,
)

data class StakeKitErrorDetailsDTO(
    @Json(name = "arguments")
    val arguments: String? = null,
    @Json(name = "amount")
    val amount: String? = null,
    @Json(name = "yieldId")
    val yieldId: String? = null,
)

enum class AccessDeniedErrorTypeDTO {
    @Json(name = "GEO_LOCATION")
    GEO_LOCATION,
}

enum class StakeKitErrorMessageDTO {
    @Json(name = "MissingArgumentsError")
    MISSING_ARGUMENTS_ERROR,

    @Json(name = "MinimumAmountNotReached")
    MINIMUM_AMOUNT_NOT_REACHED,

    @Json(name = "YieldUnderMaintenanceError")
    YIELD_UNDER_MAINTENANCE_ERROR,

    @Json(name = "InsufficientFundsError")
    INSUFFICIENT_FUNDS_ERROR,

    @Json(name = "StakedPositionNotFoundError")
    STAKED_POSITION_NOT_FOUND_ERROR,

    @Json(name = "InvalidAmountSubmittedError")
    INVALID_AMOUNT_SUBMITTED_ERROR,

    @Json(name = "BalanceUnavailableError")
    BALANCE_UNAVAILABLE_ERROR,

    @Json(name = "GasPriceUnavailableError")
    GAS_PRICE_UNAVAILABLE_ERROR,

    @Json(name = "NotImplementedError")
    NOT_IMPLEMENTED_ERROR,

    @Json(name = "TokenNotFoundError")
    TOKEN_NOT_FOUND_ERROR,

    @Json(name = "BroadcastTransactionError")
    BROADCAST_TRANSACTION_ERROR,

    @Json(name = "MissingGasPriceStrategyError")
    MISSING_GAS_PRICE_STRATEGY_ERROR,

    @Json(name = "SubstrateMalformedTransactionHashError")
    SUBSTRATE_MALFORMED_TRANSACTION_HASH_ERROR,

    @Json(name = "TronMaximumAmountOfValidatorsExceededError")
    TRON_MAXIMUM_AMOUNT_OF_VALIDATORS_EXCEEDED_ERROR,

    @Json(name = "SubstratePoolNotFoundError")
    SUBSTRATE_POOL_NOT_FOUND_ERROR,

    @Json(name = "SubstrateBondedAmountTooLowError")
    SUBSTRATE_BONDED_AMOUNT_TOO_LOW_ERROR,

    @Json(name = "TronMissingResourceTypeArgumentError")
    TRON_MISSING_RESOURCE_TYPE_ARGUMENT_ERROR,

    @Json(name = "AaveV3PoolFrozenError")
    AAVE_V3_POOL_FROZEN_ERROR,

    @Json(name = "AaveV3TokenPairNotFoundError")
    AAVE_V3_TOKEN_PAIR_NOT_FOUND_ERROR,

    @Json(name = "YearnVaultAtMaxCapacityError")
    YEARN_VAULT_AT_MAX_CAPACITY_ERROR,

    @Json(name = "StETHNoWithdrawalRequestsFoundError")
    STETH_NO_WITHDRAWAL_REQUESTS_FOUND_ERROR,

    @Json(name = "MorphoLendingPoolPausedError")
    MORPHO_LENDING_POOL_PAUSED_ERROR,

    @Json(name = "NonceUnavailableError")
    NONCE_UNAVAILABLE_ERROR,

    @Json(name = "CosmosAcccountNotFoundError")
    COSMOS_ACCOUNT_NOT_FOUND_ERROR,

    @Json(name = "AvalancheMissingAdditionalAddressesArgumentError")
    AVALANCHE_MISSING_ADDITIONAL_ADDRESSES_ARGUMENT_ERROR,

    @Json(name = "AvalancheValidatorInfoNotFoundError")
    AVALANCHE_VALIDATOR_INFO_NOT_FOUND_ERROR,

    @Json(name = "SolanaTransactionSignatureVerificationFailureError")
    SOLANA_TRANSACTION_SIGNATURE_VERIFICATION_FAILURE_ERROR,

    @Json(name = "SolanaUnableTocreateStakeAccountError")
    SOLANA_UNABLE_TO_CREATE_STAKE_ACCOUNT_ERROR,

    @Json(name = "SolanaStakeAmountTooLowError")
    SOLANA_STAKE_AMOUNT_TOO_LOW_ERROR,

    @Json(name = "SolanaUnstakeAmountTooLowError")
    SOLANA_UNSTAKE_AMOUNT_TOO_LOW_ERROR,

    @Json(name = "SolanaStakeAccountsNotFoundError")
    SOLANA_STAKE_ACCOUNTS_NOT_FOUND_ERROR,

    @Json(name = "SolanaEligibleStakeAccountsNotFoundError")
    SOLANA_ELIGIBLE_STAKE_ACCOUNTS_NOT_FOUND_ERROR,

    @Json(name = "TezosNoBalanceDelegatedError")
    TEZOS_NO_BALANCE_DELEGATED_ERROR,

    @Json(name = "TezosMissingPubkeyArgumentError")
    TEZOS_MISSING_PUBKEY_ARGUMENT_ERROR,

    @Json(name = "TezosEstimateRevealGasLimitError")
    TEZOS_ESTIMATE_REVEAL_GAS_LIMIT_ERROR,

    @Json(name = "TezosBalanceAlreadyDelegatedError")
    TEZOS_BALANCE_ALREADY_DELEGATED_ERROR,

    @Json(name = "BinanceAccountNotFoundError")
    BINANCE_ACCOUNT_NOT_FOUND_ERROR,

    @Json(name = "BinanceMissingAccountNumberOrSequenceError")
    BINANCE_MISSING_ACCOUNT_NUMBER_OR_SEQUENCE_ERROR,

    @Json(name = "GRTStakingDisabledError")
    GRT_STAKING_DISABLED_ERROR,

    @Json(name = "GRTStakingDisabledLedgerLiveError")
    GRT_STAKING_DISABLED_LEDGER_LIVE_ERROR,
}
