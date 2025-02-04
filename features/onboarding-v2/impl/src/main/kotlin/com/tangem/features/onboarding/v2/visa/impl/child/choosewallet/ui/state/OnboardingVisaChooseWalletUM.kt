package com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.ui.state

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class OnboardingVisaChooseWalletUM(
    val options: ImmutableList<SelectableChainRowUM> = persistentListOf(),
    val selectedOption: SelectableChainRowUM? = null,
    val onOptionSelected: (SelectableChainRowUM) -> Unit = {},
    val onContinueClick: () -> Unit = {},
)