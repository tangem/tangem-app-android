package com.tangem.data.staking.converters.error

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.api.stakekit.models.response.model.error.AccessDeniedErrorTypeDTO
import com.tangem.datasource.api.stakekit.models.response.model.error.StakeKitErrorMessageDTO
import com.tangem.datasource.api.stakekit.models.response.model.error.StakeKitErrorResponse
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.utils.converter.Converter

internal class StakeKitErrorConverter(
    private val jsonAdapter: JsonAdapter<StakeKitErrorResponse>,
) : Converter<String, StakingError> {

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun convert(value: String): StakingError {
        return try {
            val stakeKitErrorResponse = jsonAdapter.fromJson(value) ?: return StakingError.UnknownError

            if (stakeKitErrorResponse.type == AccessDeniedErrorTypeDTO.GEO_LOCATION) {
                StakingError.UnavailableDueToGeolocationError(
                    tags = stakeKitErrorResponse.tags ?: emptyList(),
                )
            }

            when (stakeKitErrorResponse.message) {
                StakeKitErrorMessageDTO.MINIMUM_AMOUNT_NOT_REACHED -> StakingError.MinimumAmountNotReachedError(
                    amount = stakeKitErrorResponse.details?.amount ?: "",
                )
                StakeKitErrorMessageDTO.MISSING_ARGUMENTS_ERROR -> StakingError.MissingArgumentsError(
                    arguments = stakeKitErrorResponse.details?.arguments ?: "",
                )
                StakeKitErrorMessageDTO.YIELD_UNDER_MAINTENANCE_ERROR -> StakingError.YieldUnderMaintenanceError(
                    yieldId = stakeKitErrorResponse.details?.yieldId ?: "",
                )
                StakeKitErrorMessageDTO.INSUFFICIENT_FUNDS_ERROR ->
                    StakingError.InsufficientFundsError
                StakeKitErrorMessageDTO.STAKED_POSITION_NOT_FOUND_ERROR ->
                    StakingError.StakedPositionNotFoundError
                StakeKitErrorMessageDTO.INVALID_AMOUNT_SUBMITTED_ERROR ->
                    StakingError.InvalidAmountSubmittedError
                StakeKitErrorMessageDTO.BALANCE_UNAVAILABLE_ERROR ->
                    StakingError.BalanceUnavailableError
                StakeKitErrorMessageDTO.GAS_PRICE_UNAVAILABLE_ERROR ->
                    StakingError.GasPriceUnavailableError
                StakeKitErrorMessageDTO.NOT_IMPLEMENTED_ERROR ->
                    StakingError.NotImplementedError
                StakeKitErrorMessageDTO.TOKEN_NOT_FOUND_ERROR ->
                    StakingError.TokenNotFoundError
                StakeKitErrorMessageDTO.BROADCAST_TRANSACTION_ERROR ->
                    StakingError.BroadcastTransactionError
                StakeKitErrorMessageDTO.MISSING_GAS_PRICE_STRATEGY_ERROR ->
                    StakingError.MissingGasPriceStrategyError
                StakeKitErrorMessageDTO.SUBSTRATE_MALFORMED_TRANSACTION_HASH_ERROR ->
                    StakingError.SubstrateMalformedTransactionHashError
                StakeKitErrorMessageDTO.TRON_MAXIMUM_AMOUNT_OF_VALIDATORS_EXCEEDED_ERROR ->
                    StakingError.TronMaximumAmountOfValidatorsExceededError
                StakeKitErrorMessageDTO.SUBSTRATE_POOL_NOT_FOUND_ERROR ->
                    StakingError.SubstratePoolNotFoundError
                StakeKitErrorMessageDTO.SUBSTRATE_BONDED_AMOUNT_TOO_LOW_ERROR ->
                    StakingError.SubstrateBondedAmountTooLowError
                StakeKitErrorMessageDTO.TRON_MISSING_RESOURCE_TYPE_ARGUMENT_ERROR ->
                    StakingError.TronMissingResourceTypeArgumentError
                StakeKitErrorMessageDTO.AAVE_V3_POOL_FROZEN_ERROR ->
                    StakingError.AaveV3PoolFrozenError
                StakeKitErrorMessageDTO.AAVE_V3_TOKEN_PAIR_NOT_FOUND_ERROR ->
                    StakingError.AaveV3TokenPairNotFoundError
                StakeKitErrorMessageDTO.YEARN_VAULT_AT_MAX_CAPACITY_ERROR ->
                    StakingError.YearnVaultAtMaxCapacityError
                StakeKitErrorMessageDTO.STETH_NO_WITHDRAWAL_REQUESTS_FOUND_ERROR ->
                    StakingError.StETHNoWithdrawalRequestsFoundError
                StakeKitErrorMessageDTO.MORPHO_LENDING_POOL_PAUSED_ERROR ->
                    StakingError.MorphoLendingPoolPausedError
                StakeKitErrorMessageDTO.NONCE_UNAVAILABLE_ERROR ->
                    StakingError.NonceUnavailableError
                StakeKitErrorMessageDTO.COSMOS_ACCOUNT_NOT_FOUND_ERROR ->
                    StakingError.CosmosAcccountNotFoundError
                StakeKitErrorMessageDTO.AVALANCHE_MISSING_ADDITIONAL_ADDRESSES_ARGUMENT_ERROR ->
                    StakingError.AvalancheMissingAdditionalAddressesArgumentError
                StakeKitErrorMessageDTO.AVALANCHE_VALIDATOR_INFO_NOT_FOUND_ERROR ->
                    StakingError.AvalancheValidatorInfoNotFoundError
                StakeKitErrorMessageDTO.SOLANA_TRANSACTION_SIGNATURE_VERIFICATION_FAILURE_ERROR ->
                    StakingError.SolanaTransactionSignatureVerificationFailureError
                StakeKitErrorMessageDTO.SOLANA_UNABLE_TO_CREATE_STAKE_ACCOUNT_ERROR ->
                    StakingError.SolanaUnableTocreateStakeAccountError
                StakeKitErrorMessageDTO.SOLANA_STAKE_AMOUNT_TOO_LOW_ERROR ->
                    StakingError.SolanaStakeAmountTooLowError
                StakeKitErrorMessageDTO.SOLANA_UNSTAKE_AMOUNT_TOO_LOW_ERROR ->
                    StakingError.SolanaUnstakeAmountTooLowError
                StakeKitErrorMessageDTO.SOLANA_STAKE_ACCOUNTS_NOT_FOUND_ERROR ->
                    StakingError.SolanaStakeAccountsNotFoundError
                StakeKitErrorMessageDTO.SOLANA_ELIGIBLE_STAKE_ACCOUNTS_NOT_FOUND_ERROR ->
                    StakingError.SolanaEligibleStakeAccountsNotFoundError
                StakeKitErrorMessageDTO.TEZOS_NO_BALANCE_DELEGATED_ERROR ->
                    StakingError.TezosNoBalanceDelegatedError
                StakeKitErrorMessageDTO.TEZOS_MISSING_PUBKEY_ARGUMENT_ERROR ->
                    StakingError.TezosMissingPubkeyArgumentError
                StakeKitErrorMessageDTO.TEZOS_ESTIMATE_REVEAL_GAS_LIMIT_ERROR ->
                    StakingError.TezosEstimateRevealGasLimitError
                StakeKitErrorMessageDTO.TEZOS_BALANCE_ALREADY_DELEGATED_ERROR ->
                    StakingError.TezosBalanceAlreadyDelegatedError
                StakeKitErrorMessageDTO.BINANCE_ACCOUNT_NOT_FOUND_ERROR ->
                    StakingError.BinanceAccountNotFoundError
                StakeKitErrorMessageDTO.BINANCE_MISSING_ACCOUNT_NUMBER_OR_SEQUENCE_ERROR ->
                    StakingError.BinanceMissingAccountNumberOrSequenceError
                StakeKitErrorMessageDTO.GRT_STAKING_DISABLED_ERROR ->
                    StakingError.GRTStakingDisabledError
                StakeKitErrorMessageDTO.GRT_STAKING_DISABLED_LEDGER_LIVE_ERROR ->
                    StakingError.GRTStakingDisabledLedgerLiveError
                else -> StakingError.UnknownError
            }
        } catch (e: Exception) {
            StakingError.UnknownError
        }
    }
}