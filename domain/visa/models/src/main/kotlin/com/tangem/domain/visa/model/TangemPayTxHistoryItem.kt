package com.tangem.domain.visa.model

import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.serialization.SerializedCurrency
import com.tangem.domain.models.serialization.SerializedDateTime
import kotlinx.serialization.Serializable

@Serializable
sealed class TangemPayTxHistoryItem {
    abstract val id: String
    abstract val date: SerializedDateTime
    abstract val amount: SerializedBigDecimal
    abstract val currency: SerializedCurrency
    abstract val jsonRepresentation: String

    @Serializable
    data class Spend(
        override val id: String,
        override val jsonRepresentation: String,
        override val date: SerializedDateTime,
        override val amount: SerializedBigDecimal,
        override val currency: SerializedCurrency,
        val localAmount: SerializedBigDecimal?,
        val localCurrency: SerializedCurrency?,
        val enrichedMerchantName: String?,
        val merchantName: String,
        val enrichedMerchantCategory: String?,
        val merchantCategoryCode: String?,
        val merchantCategory: String?,
        val status: Status,
        val enrichedMerchantIconUrl: String?,
    ) : TangemPayTxHistoryItem()

    @Serializable
    data class Payment(
        override val id: String,
        override val jsonRepresentation: String,
        override val date: SerializedDateTime,
        override val amount: SerializedBigDecimal,
        override val currency: SerializedCurrency,
        val transactionHash: String?,
    ) : TangemPayTxHistoryItem()

    @Serializable
    data class Fee(
        override val id: String,
        override val jsonRepresentation: String,
        override val date: SerializedDateTime,
        override val amount: SerializedBigDecimal,
        override val currency: SerializedCurrency,
        val description: String?,
    ) : TangemPayTxHistoryItem()

    @Serializable
    data class Collateral(
        override val id: String,
        override val jsonRepresentation: String,
        override val date: SerializedDateTime,
        override val amount: SerializedBigDecimal,
        override val currency: SerializedCurrency,
        val transactionHash: String,
        val type: Type,
    ) : TangemPayTxHistoryItem()

    enum class Type {
        Deposit, Withdrawal
    }

    enum class Status {
        PENDING,
        RESERVED,
        COMPLETED,
        DECLINED,
        UNKNOWN,
    }
}