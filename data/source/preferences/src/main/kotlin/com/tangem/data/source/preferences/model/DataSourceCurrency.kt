package com.tangem.data.source.preferences.model

data class DataSourceCurrency(
    val id: String,
    val code: String,
    val name: String,
    val rateBTC: String,
    val unit: String,
    val type: String,
)