package com.tangem.features.tangempay.hotwallet

internal data class TangemPayHotWalletOnboardingUM(
    val isLoading: Boolean,
    val onGetCardClick: () -> Unit,
    val onTermsClick: () -> Unit,
)