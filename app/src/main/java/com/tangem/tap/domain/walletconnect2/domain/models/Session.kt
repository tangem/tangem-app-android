package com.tangem.tap.domain.walletconnect2.domain.models

data class Session(
    val topic: String,
    val accounts: List<Account>,
) {
    companion object {
        fun fromAccounts(accounts: List<Account>, topic: String): Session {
            return Session(
                topic = topic,
                accounts = accounts,
            )
        }
    }
}