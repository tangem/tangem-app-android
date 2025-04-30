package com.tangem.features.onramp.success.entity

interface OnrampSuccessClickIntents {
    fun goToProviderClick(providerLink: String)
    fun onCopyClick(copiedText: String)
}