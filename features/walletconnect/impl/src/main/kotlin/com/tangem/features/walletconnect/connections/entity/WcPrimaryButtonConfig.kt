package com.tangem.features.walletconnect.connections.entity

data class WcPrimaryButtonConfig(val showProgress: Boolean, val enabled: Boolean, val onClick: () -> Unit)