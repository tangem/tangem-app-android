package com.tangem.commands.personalization.entities

import com.tangem.common.KeyPair

data class Manufacturer(
        val keyPair: KeyPair,
        val name: String? = null
)