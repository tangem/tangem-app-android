package com.tangem.domain.visa.model

import org.joda.time.DateTime
import java.math.BigDecimal
import java.util.Currency

sealed class TangemPayTxHistoryItem {
    abstract val id: String
    abstract val date: DateTime
    abstract val amount: BigDecimal
    abstract val currency: Currency

    data class Spend(
        override val id: String,
        override val date: DateTime,
        override val amount: BigDecimal,
        override val currency: Currency,
        val enrichedMerchantName: String?,
        val merchantName: String,
        val enrichedMerchantCategory: String?,
        val merchantCategory: String,
        val status: Status,
        val enrichedMerchantIconUrl: String?,
    ) : TangemPayTxHistoryItem()

    data class Payment(
        override val id: String,
        override val date: DateTime,
        override val amount: BigDecimal,
        override val currency: Currency,
    ) : TangemPayTxHistoryItem()

    data class Fee(
        override val id: String,
        override val date: DateTime,
        override val amount: BigDecimal,
        override val currency: Currency,
    ) : TangemPayTxHistoryItem()

    enum class Status {
        PENDING,
        RESERVED,
        COMPLETED,
        DECLINED,
        UNKNOWN,
    }
}