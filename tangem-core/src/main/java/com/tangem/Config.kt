package com.tangem

import com.tangem.commands.personalization.entities.Issuer
import com.tangem.common.KeyPair

class Config(
        val linkedTerminal: Boolean = true,
        val issuerPublicKey: ByteArray? = null,
        val manufacturerKeyPair: KeyPair? = null,
        val acquirerKeyPair: KeyPair? = null,
        val issuer: Issuer? = null
)