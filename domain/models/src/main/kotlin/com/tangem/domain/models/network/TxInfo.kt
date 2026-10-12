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
 * @property fee                    transaction fee
 */
@Serializable
sealed interface TxInfo {

    val txHash: String
    val timestampInMillis: Long
    val isOutgoing: Boolean
    val destinationType: DestinationType
    val sourceType: SourceType
    val interactionAddressType: InteractionAddressType?
    val status: TransactionStatus
    val type: TransactionType
    val amount: SerializedBigDecimal
    val fee: SdkAmount?

    @Serializable
    data class Single(
        override val txHash: String,
        override val timestampInMillis: Long,
        override val isOutgoing: Boolean,
        override val destinationType: DestinationType,
        override val sourceType: SourceType,
        override val interactionAddressType: InteractionAddressType?,
        override val status: TransactionStatus,
        override val type: TransactionType,
        override val amount: SerializedBigDecimal,
        override val fee: SdkAmount? = null,
    ) : TxInfo

    @Serializable
    data class MultiTransfer(
        override val amount: SerializedBigDecimal,
        override val isOutgoing: Boolean,
        override val type: TransactionType,
        override val destinationType: DestinationType,
        override val sourceType: SourceType,
        override val interactionAddressType: InteractionAddressType?,
        val transfers: List<Single>,
    ) : TxInfo {

        init {
            require(transfers.isNotEmpty()) { "A MultiTransfer must hold at least one transfer" }
        }

        private val first: Single get() = transfers.first()

        override val txHash: String get() = first.txHash
        override val timestampInMillis: Long get() = first.timestampInMillis
        override val status: TransactionStatus get() = first.status
        override val fee: SdkAmount? get() = first.fee

        val gaslessFee: Single? get() = transfers.firstOrNull { it.type == TransactionType.GaslessFee }
    }

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

        /**
         * ERC-20 `approve` — the granted allowance rather than a transfer.
         *
         * @property amount  allowance in the approved token (its symbol, decimals and contract); a `null`
         *                   [SdkAmount.value] is an unlimited approval
         * @property address spender the allowance is granted to
         */
        @Serializable
        data class Approve(
            val amount: SdkAmount,
            val address: String,
        ) : TransactionType

        @Serializable
        data object Swap : TransactionType

        @Serializable
        data object UnknownOperation : TransactionType

        /**
         * A contract call the history shows by its raw method [name].
         *
         * @property name       the contract method, unlocalized — the history renders it as it came
         * @property recognized what the call really was, when a decoded type had to give way to this plain one: a
         *                      protocol interaction viewed from a coin's own history moved no coin and only paid the
         *                      gas of a token's operation, so it is reduced to an operation that still knows what it
         *                      paid for. `null` — the usual case — when the call was never recognized. Never another
         *                      [Operation]: this carries the type that was given up, not a chain of them.
         */
        @Serializable
        data class Operation(val name: String, val recognized: TransactionType? = null) : TransactionType

        @Serializable
        data object GaslessFee : TransactionType

        /**
         * A transaction that moved no coin — the wallet only paid the network fee of a token transfer. The fee
         * itself is in [TxInfo.fee].
         *
         * @property amount  the tokens that did move; `null` for a token the wallet does not track
         * @property address recipient of the tokens
         */
        @Serializable
        data class NetworkFee(
            val amount: SdkAmount?,
            val address: String,
        ) : TransactionType

        @Serializable
        sealed interface YieldSupply : TransactionType {

            val address: String?

            @Serializable
            data class Enter(override val address: String) : YieldSupply

            @Serializable
            data class Exit(override val address: String) : YieldSupply

            @Serializable
            data object Topup : YieldSupply {
                override val address: String? = null
            }

            @Serializable
            data class Send(
                override val address: String? = null,
                val isYieldSupplyWithdraw: Boolean,
            ) : YieldSupply

            @Serializable
            data class DeployContract(override val address: String) : YieldSupply

            @Serializable
            data class ReactivateToken(override val address: String) : YieldSupply

            @Serializable
            data class InitializeToken(override val address: String) : YieldSupply
        }

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