package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model

import com.tangem.crypto.bip39.Mnemonic

internal data class SeedPhraseState(
    val generatedWords12: Mnemonic?,
    val generatedWords24: Mnemonic?,
    val generatedWordsType: GeneratedWordsType,
    val readyToImport: Boolean,
)

internal enum class GeneratedWordsType(val length: Int) {
    Words12(length = 12),
    Words24(length = 24),
}