package com.tangem.commands.personalization.entities

import com.tangem.common.KeyPair

data class Issuer(
        val name: String,
        val id: String,
        val dataKeyPair: KeyPair,
        val transactionKeyPair: KeyPair
)