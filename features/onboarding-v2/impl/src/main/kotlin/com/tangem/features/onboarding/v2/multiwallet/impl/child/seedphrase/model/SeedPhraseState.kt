package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model

import com.tangem.crypto.bip39.Mnemonic

data class SeedPhraseState(
    val generatedWords12: Mnemonic? = null,
    val generatedWords24: Mnemonic? = null,
    val words24Option: Boolean = false,
    val readyToImport: Boolean = false,
)