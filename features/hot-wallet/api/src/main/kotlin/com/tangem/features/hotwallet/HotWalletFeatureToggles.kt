package com.tangem.features.hotwallet

interface HotWalletFeatureToggles {
    val isHotWalletEnabled: Boolean
    val isWalletCreationRestrictionEnabled: Boolean
    val isHotWalletVisible: Boolean
}