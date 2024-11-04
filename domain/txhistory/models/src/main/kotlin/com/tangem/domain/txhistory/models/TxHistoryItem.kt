package com.tangem.domain.txhistory.models

import java.math.BigDecimal

data class TxHistoryItem(
    val txHash: String,
    val timestampInMillis: Long,
    val isOutgoing: Boolean,
    val destinationType: DestinationType,
    val sourceType: SourceType,
    val interactionAddressType: InteractionAddressType?,
    val status: TransactionStatus,
    val type: TransactionType,
    val amount: BigDecimal,
) {

    sealed class DestinationType {
        data class Single(val addressType: AddressType) : DestinationType()
        data class Multiple(val addressTypes: List<AddressType>) : DestinationType()
    }

    sealed class SourceType {

        data class Single(val address: String) : SourceType()
        data class Multiple(val addresses: List<String>) : SourceType()
    }

    sealed class AddressType {
        abstract val address: String

        data class User(override val address: String) : AddressType()
        data class Contract(override val address: String) : AddressType()
        data class Validator(override val address: String) : AddressType()
    }

    sealed interface TransactionType {
        data object Transfer : TransactionType
        data object Approve : TransactionType
        data object Swap : TransactionType
        data object UnknownOperation : TransactionType
        data class Operation(val name: String) : TransactionType

        sealed interface StakingTransactionType : TransactionType {
            data class Vote(val validatorAddress: String) : StakingTransactionType
            data object ClaimRewards : StakingTransactionType
            data object Stake : StakingTransactionType
            data object Unstake : StakingTransactionType
            data object Withdraw : StakingTransactionType
            data object Restake : StakingTransactionType
        }
    }

    sealed class TransactionStatus {
        data object Failed : TransactionStatus()
        data object Unconfirmed : TransactionStatus()
        data object Confirmed : TransactionStatus()
    }

    sealed class InteractionAddressType {
        data class Validator(val address: String) : InteractionAddressType()
        data class User(val address: String) : InteractionAddressType()
        data class Contract(val address: String) : InteractionAddressType()
        data class Multiple(val addresses: List<String>) : InteractionAddressType()
    }
}