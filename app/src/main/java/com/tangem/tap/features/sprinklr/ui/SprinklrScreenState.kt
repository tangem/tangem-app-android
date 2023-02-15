package com.tangem.tap.features.sprinklr.ui

internal data class SprinklrScreenState(
    val initialUrl: String = "",
    val onNavigateBack: () -> Unit = {},
)
