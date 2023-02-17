package com.tangem.tap.features.sprinklr.redux

data class SprinklrState(
    val url: String = "",
    val sprinklrDomains: List<String> = emptyList(),
)
