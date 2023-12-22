package com.tangem.managetokens.presentation.customtokens.state

internal data class Derivation(
    val networkName: String,
    val standardType: String?,
    val path: String,
    val networkId: String?,
    val onDerivationSelected: (Derivation) -> Unit,
)