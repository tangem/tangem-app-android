package com.tangem.features.walletconnect.connections.entity

data class WcPrimaryButtonConfig(val shouldShowProgress: Boolean, val isEnabled: Boolean, val onClick: () -> Unit)