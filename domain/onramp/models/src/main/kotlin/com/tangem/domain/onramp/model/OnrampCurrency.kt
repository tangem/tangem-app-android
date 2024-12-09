package com.tangem.domain.onramp.model

import kotlinx.serialization.Serializable

@Serializable
data class OnrampCurrency(
    val name: String,
    val code: String,
    val image: String,
    val precision: Int,
    val unit: String,
)