package com.tangem.managetokens.presentation.customtokens.state.previewdata

import com.tangem.managetokens.presentation.customtokens.state.ChooseDerivationState
import com.tangem.managetokens.presentation.customtokens.state.Derivation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal object ChooseDerivationPreviewData {
    private val derivations: ImmutableList<Derivation> = persistentListOf(
        Derivation(
            networkName = "Ethereum",
            path = "m/44’/9001’/0’/0/0",
            networkId = "ethereum",
            standardType = "ERC",
            onDerivationSelected = {},
        ),
        Derivation(
            networkName = "Polygon",
            path = "m/44’/9001’/0’/0/0",
            networkId = "polygon",
            standardType = "ERC",
            onDerivationSelected = {},
        ),
        Derivation(
            networkName = "Avalanche",
            path = "m/44’/9001’/0’/0/0",
            networkId = "avalanche",
            standardType = "ERC",
            onDerivationSelected = {},
        ),
    )

    val state = ChooseDerivationState(
        derivations = derivations,
        selectedDerivation = derivations.first(),
        enterCustomDerivationState = null,
        onEnterCustomDerivation = {},
        onCloseChoosingDerivationClick = {},
        onChooseDerivationClick = {},
        show = true,
    )
}