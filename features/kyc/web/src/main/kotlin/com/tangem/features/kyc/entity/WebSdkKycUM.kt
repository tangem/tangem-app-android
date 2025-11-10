package com.tangem.features.kyc.entity

internal data class WebSdkKycUM(
    val accessToken: String?,
    val url: String,
    val onBackClick: () -> Unit,
    val onLoadingFinished: () -> Unit,
    val isLoading: Boolean,
)