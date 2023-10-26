package com.tangem.domain.txhistory.models

import java.math.BigDecimal

data class TxHistoryItem(
    val txHash: String,
    val timestampInMillis: Long,
    val isOutgoing: Boolean,
    val destinationType: DestinationType,
    val sourceType: SourceType,
    val interactionAddressType: InteractionAddressType,
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
    }

    sealed interface TransactionType {
        object Transfer : TransactionType
        object Approve : TransactionType
        object Swap : TransactionType
        object UnknownOperation : TransactionType
        data class Operation(val name: String) : TransactionType
    }

    sealed class TransactionStatus {
        object Failed : TransactionStatus()
        object Unconfirmed : TransactionStatus()
        object Confirmed : TransactionStatus()
    }

    sealed class InteractionAddressType {
        data class User(val address: String) : InteractionAddressType()
        data class Contract(val address: String) : InteractionAddressType()
        data class Multiple(val addresses: List<String>) : InteractionAddressType()
    }
}