package com.tangem.tap.features.sprinklr.ui

import com.google.accompanist.web.WebContent

internal data class SprinklrScreenState(
    val initialUrl: String = "",
    val sprinklrDomains: List<String> = emptyList(),
    val onNavigateBack: () -> Unit = {},
    val onNewUrl: WebContent.(String) -> WebContent = { this },
)
