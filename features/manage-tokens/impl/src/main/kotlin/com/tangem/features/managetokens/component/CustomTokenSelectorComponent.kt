package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.managetokens.entity.customtoken.SelectedDerivationPath
import com.tangem.features.managetokens.entity.customtoken.SelectedNetwork

internal interface CustomTokenSelectorComponent : ComposableContentComponent {

    sealed class Params {

        data class NetworkSelector(
            val mode: AddCustomTokenMode,
            val selectedNetwork: SelectedNetwork?,
            val onNetworkSelected: (SelectedNetwork) -> Unit,
        ) : Params()

        data class DerivationPathSelector(
            val mode: AddCustomTokenMode,
            val selectedNetwork: SelectedNetwork,
            val selectedDerivationPath: SelectedDerivationPath?,
            val onDerivationPathSelected: (SelectedDerivationPath) -> Unit,
        ) : Params()
    }

    interface Factory : ComponentFactory<Params, CustomTokenSelectorComponent>
}