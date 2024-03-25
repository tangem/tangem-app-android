package com.tangem.tap.domain.walletconnect2.domain.models

data class Session(
    val topic: String,
    val accounts: List<Account>,
)