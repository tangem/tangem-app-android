package com.tangem.domain.pay.model

/** Cashback program document, from `GET v1/customer/cashback/accruals/docs`. */
data class CashbackDocument(
    val id: String,
    val title: String,
    val url: String,
)