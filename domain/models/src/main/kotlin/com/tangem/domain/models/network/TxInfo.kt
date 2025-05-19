package com.tangem.domain.models.network

import java.math.BigDecimal

/**
 * Represents information about a transaction. Do not use it for sending transactions.
 *
 * @property txHash                 transaction hash
 * @property timestampInMillis      transaction timestamp in milliseconds
 * @property isOutgoing             flag that determines the direction of the transaction (incoming or outgoing)
 * @property destinationType        type of destination (single or multiple)
 * @property sourceType             type of source (single or multiple)
 * @property interactionAddressType interaction address type
 * @property status                 transaction status
 * @property type                   transaction type
 * @property amount                 transaction amount
 */
data class TxInfo(
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

    /** Destination type*/
    sealed class DestinationType {

        /**
         * Single
         *
         * @property addressType address type
         */
        data class Single(val addressType: AddressType) : DestinationType()

        /**
         * Multiple
         *
         * @property addressTypes addresses types
         */
        data class Multiple(val addressTypes: List<AddressType>) : DestinationType()
    }

    /** Address type */
    sealed class AddressType {

        /** Address value */
        abstract val address: String

        data class User(override val address: String) : AddressType()
        data class Contract(override val address: String) : AddressType()
        data class Validator(override val address: String) : AddressType()
    }

    /** Source type */
    sealed class SourceType {

        /**
         * Single
         *
         * @property address address
         */
        data class Single(val address: String) : SourceType()

        /**
         * Multiple
         *
         * @property addresses addresses
         */
        data class Multiple(val addresses: List<String>) : SourceType()
    }

    /** Transaction type */
    sealed interface TransactionType {
        data object Transfer : TransactionType
        data object Approve : TransactionType
        data object Swap : TransactionType
        data object UnknownOperation : TransactionType
        data class Operation(val name: String) : TransactionType

        sealed interface Staking : TransactionType {
            data class Vote(val validatorAddress: String) : Staking
            data object ClaimRewards : Staking
            data object Stake : Staking
            data object Unstake : Staking
            data object Withdraw : Staking
            data object Restake : Staking
        }
    }

    /** Transaction status */
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