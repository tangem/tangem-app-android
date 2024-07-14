package com.tangem.data.staking.converters.error

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.api.stakekit.models.response.model.error.StakeKitErrorMessageDTO
import com.tangem.datasource.api.stakekit.models.response.model.error.StakeKitErrorResponse
import com.tangem.domain.staking.model.StakeKitError
import com.tangem.utils.converter.Converter

internal class StakeKitErrorConverter(
    private val jsonAdapter: JsonAdapter<StakeKitErrorResponse>,
) : Converter<String, StakeKitError> {

    @Suppress("CyclomaticComplexMethod")
    override fun convert(value: String): StakeKitError {
        return try {
            val stakeKitErrorResponse = jsonAdapter.fromJson(value) ?: return StakeKitError.UnknownError

            when (stakeKitErrorResponse.message) {
                StakeKitErrorMessageDTO.MINIMUM_AMOUNT_NOT_REACHED -> StakeKitError.MinimumAmountNotReachedError(
                    amount = stakeKitErrorResponse.details?.amount ?: ""
                )
                StakeKitErrorMessageDTO.MISSING_ARGUMENTS_ERROR -> StakeKitError.MissingArgumentsError(
                    arguments = stakeKitErrorResponse.details?.arguments ?: ""
                )
                StakeKitErrorMessageDTO.YIELD_UNDER_MAINTENANCE_ERROR -> StakeKitError.YieldUnderMaintenanceError(
                    yieldId = stakeKitErrorResponse.details?.yieldId ?: ""
                )
                StakeKitErrorMessageDTO.INSUFFICIENT_FUNDS_ERROR -> StakeKitError.InsufficientFundsError
                StakeKitErrorMessageDTO.STAKED_POSITION_NOT_FOUND_ERROR -> StakeKitError.StakedPositionNotFoundError
                StakeKitErrorMessageDTO.INVALID_AMOUNT_SUBMITTED_ERROR -> StakeKitError.InvalidAmountSubmittedError
                StakeKitErrorMessageDTO.BALANCE_UNAVAILABLE_ERROR -> StakeKitError.BalanceUnavailableError
                StakeKitErrorMessageDTO.GAS_PRICE_UNAVAILABLE_ERROR -> StakeKitError.GasPriceUnavailableError
                StakeKitErrorMessageDTO.NOT_IMPLEMENTED_ERROR -> StakeKitError.NotImplementedError
                StakeKitErrorMessageDTO.TOKEN_NOT_FOUND_ERROR -> StakeKitError.TokenNotFoundError
                StakeKitErrorMessageDTO.BROADCAST_TRANSACTION_ERROR -> StakeKitError.BroadcastTransactionError
                StakeKitErrorMessageDTO.MISSING_GAS_PRICE_STRATEGY_ERROR -> StakeKitError.MissingGasPriceStrategyError
                StakeKitErrorMessageDTO.SUBSTRATE_MALFORMED_TRANSACTION_HASH_ERROR -> StakeKitError.SubstrateMalformedTransactionHashError
                StakeKitErrorMessageDTO.TRON_MAXIMUM_AMOUNT_OF_VALIDATORS_EXCEEDED_ERROR -> StakeKitError.TronMaximumAmountOfValidatorsExceededError
                StakeKitErrorMessageDTO.SUBSTRATE_POOL_NOT_FOUND_ERROR -> StakeKitError.SubstratePoolNotFoundError
                StakeKitErrorMessageDTO.SUBSTRATE_BONDED_AMOUNT_TOO_LOW_ERROR -> StakeKitError.SubstrateBondedAmountTooLowError
                StakeKitErrorMessageDTO.TRON_MISSING_RESOURCE_TYPE_ARGUMENT_ERROR -> StakeKitError.TronMissingResourceTypeArgumentError
                StakeKitErrorMessageDTO.AAVE_V3_POOL_FROZEN_ERROR -> StakeKitError.AaveV3PoolFrozenError
                StakeKitErrorMessageDTO.AAVE_V3_TOKEN_PAIR_NOT_FOUND_ERROR -> StakeKitError.AaveV3TokenPairNotFoundError
                StakeKitErrorMessageDTO.YEARN_VAULT_AT_MAX_CAPACITY_ERROR -> StakeKitError.YearnVaultAtMaxCapacityError
                StakeKitErrorMessageDTO.STETH_NO_WITHDRAWAL_REQUESTS_FOUND_ERROR -> StakeKitError.StETHNoWithdrawalRequestsFoundError
                StakeKitErrorMessageDTO.MORPHO_LENDING_POOL_PAUSED_ERROR -> StakeKitError.MorphoLendingPoolPausedError
                StakeKitErrorMessageDTO.NONCE_UNAVAILABLE_ERROR -> StakeKitError.NonceUnavailableError
                StakeKitErrorMessageDTO.COSMOS_ACCOUNT_NOT_FOUND_ERROR -> StakeKitError.CosmosAcccountNotFoundError
                StakeKitErrorMessageDTO.AVALANCHE_MISSING_ADDITIONAL_ADDRESSES_ARGUMENT_ERROR -> StakeKitError.AvalancheMissingAdditionalAddressesArgumentError
                StakeKitErrorMessageDTO.AVALANCHE_VALIDATOR_INFO_NOT_FOUND_ERROR -> StakeKitError.AvalancheValidatorInfoNotFoundError
                StakeKitErrorMessageDTO.SOLANA_TRANSACTION_SIGNATURE_VERIFICATION_FAILURE_ERROR -> StakeKitError.SolanaTransactionSignatureVerificationFailureError
                StakeKitErrorMessageDTO.SOLANA_UNABLE_TO_CREATE_STAKE_ACCOUNT_ERROR -> StakeKitError.SolanaUnableTocreateStakeAccountError
                StakeKitErrorMessageDTO.SOLANA_STAKE_AMOUNT_TOO_LOW_ERROR -> StakeKitError.SolanaStakeAmountTooLowError
                StakeKitErrorMessageDTO.SOLANA_UNSTAKE_AMOUNT_TOO_LOW_ERROR -> StakeKitError.SolanaUnstakeAmountTooLowError
                StakeKitErrorMessageDTO.SOLANA_STAKE_ACCOUNTS_NOT_FOUND_ERROR -> StakeKitError.SolanaStakeAccountsNotFoundError
                StakeKitErrorMessageDTO.SOLANA_ELIGIBLE_STAKE_ACCOUNTS_NOT_FOUND_ERROR -> StakeKitError.SolanaEligibleStakeAccountsNotFoundError
                StakeKitErrorMessageDTO.TEZOS_NO_BALANCE_DELEGATED_ERROR -> StakeKitError.TezosNoBalanceDelegatedError
                StakeKitErrorMessageDTO.TEZOS_MISSING_PUBKEY_ARGUMENT_ERROR -> StakeKitError.TezosMissingPubkeyArgumentError
                StakeKitErrorMessageDTO.TEZOS_ESTIMATE_REVEAL_GAS_LIMIT_ERROR -> StakeKitError.TezosEstimateRevealGasLimitError
                StakeKitErrorMessageDTO.TEZOS_BALANCE_ALREADY_DELEGATED_ERROR -> StakeKitError.TezosBalanceAlreadyDelegatedError
                StakeKitErrorMessageDTO.BINANCE_ACCOUNT_NOT_FOUND_ERROR -> StakeKitError.BinanceAccountNotFoundError
                StakeKitErrorMessageDTO.BINANCE_MISSING_ACCOUNT_NUMBER_OR_SEQUENCE_ERROR -> StakeKitError.BinanceMissingAccountNumberOrSequenceError
                StakeKitErrorMessageDTO.GRT_STAKING_DISABLED_ERROR -> StakeKitError.GRTStakingDisabledError
                StakeKitErrorMessageDTO.GRT_STAKING_DISABLED_LEDGER_LIVE_ERROR -> StakeKitError.GRTStakingDisabledLedgerLiveError
                else -> StakeKitError.UnknownError
            }
        } catch (e: Exception) {
            StakeKitError.UnknownError
        }
    }
}
