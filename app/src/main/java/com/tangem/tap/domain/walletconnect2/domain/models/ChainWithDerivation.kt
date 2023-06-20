package com.tangem.tap.domain.walletconnect2.domain.models

data class ChainWithDerivation(
    val chain: String,
    val derivationPath: String?,
)