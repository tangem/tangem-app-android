package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model

data class SeedPhraseState(
    val generatedWords: List<String>? = null,
    val words24Option: Boolean = false,
)