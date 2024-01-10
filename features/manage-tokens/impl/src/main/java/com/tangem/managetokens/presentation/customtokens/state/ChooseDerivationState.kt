package com.tangem.managetokens.presentation.customtokens.state

import kotlinx.collections.immutable.ImmutableList

internal data class ChooseDerivationState(
    val derivations: ImmutableList<Derivation>,
    val selectedDerivation: Derivation?,
    val enterCustomDerivationState: EnterCustomDerivationState?,
    val onChooseDerivationClick: () -> Unit,
    val onCloseChoosingDerivationClick: () -> Unit,
    val onEnterCustomDerivation: () -> Unit,
    val show: Boolean = false,
)