package com.tangem.store.preferences.model

data class CurrencyDM(
    val id: String,
    val code: String,
    val name: String,
    val rateBTC: String,
    val unit: String,
    val type: String,
)
