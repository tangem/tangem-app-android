package com.tangem.domain.models.network

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

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
@Serializable
data class TxInfo(
    val txHash: String,
    val timestampInMillis: Long,
    val isOutgoing: Boolean,
    val destinationType: DestinationType,
    val sourceType: SourceType,
    val interactionAddressType: InteractionAddressType?,
    val status: TransactionStatus,
    val type: TransactionType,
    val amount: SerializedBigDecimal,
) {

    /** Destination type*/
    @Serializable
    sealed class DestinationType {

        /**
         * Single
         *
         * @property addressType address type
         */
        @Serializable
        data class Single(val addressType: AddressType) : DestinationType()

        /**
         * Multiple
         *
         * @property addressTypes addresses types
         */
        @Serializable
        data class Multiple(val addressTypes: List<AddressType>) : DestinationType()
    }

    /** Address type */
    @Serializable
    sealed class AddressType {

        /** Address value */
        abstract val address: String

        @Serializable
        data class User(override val address: String) : AddressType()

        @Serializable
        data class Contract(override val address: String) : AddressType()

        @Serializable
        data class Validator(override val address: String) : AddressType()
    }

    /** Source type */
    @Serializable
    sealed class SourceType {

        /**
         * Single
         *
         * @property address address
         */
        @Serializable
        data class Single(val address: String) : SourceType()

        /**
         * Multiple
         *
         * @property addresses addresses
         */
        @Serializable
        data class Multiple(val addresses: List<String>) : SourceType()
    }

    /** Transaction type */
    @Serializable
    sealed interface TransactionType {

        @Serializable
        data object Transfer : TransactionType

        @Serializable
        data object Approve : TransactionType

        @Serializable
        data object Swap : TransactionType

        @Serializable
        data object UnknownOperation : TransactionType

        @Serializable
        data class Operation(val name: String) : TransactionType

        @Serializable
        sealed interface Staking : TransactionType {

            @Serializable
            data class Vote(val validatorAddress: String) : Staking

            @Serializable
            data object ClaimRewards : Staking

            @Serializable
            data object Stake : Staking

            @Serializable
            data object Unstake : Staking

            @Serializable
            data object Withdraw : Staking

            @Serializable
            data object Restake : Staking
        }
    }

    /** Transaction status */
    @Serializable
    sealed class TransactionStatus {

        @Serializable
        data object Failed : TransactionStatus()

        @Serializable
        data object Unconfirmed : TransactionStatus()

        @Serializable
        data object Confirmed : TransactionStatus()
    }

    @Serializable
    sealed class InteractionAddressType {

        @Serializable
        data class Validator(val address: String) : InteractionAddressType()

        @Serializable
        data class User(val address: String) : InteractionAddressType()

        @Serializable
        data class Contract(val address: String) : InteractionAddressType()

        @Serializable
        data class Multiple(val addresses: List<String>) : InteractionAddressType()
    }
}