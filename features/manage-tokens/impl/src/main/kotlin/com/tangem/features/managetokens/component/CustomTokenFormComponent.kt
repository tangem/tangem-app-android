package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormValues
import com.tangem.features.managetokens.entity.customtoken.SelectedDerivationPath
import com.tangem.features.managetokens.entity.customtoken.SelectedNetwork

internal interface CustomTokenFormComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val network: SelectedNetwork,
        val derivationPath: SelectedDerivationPath?,
        val formValues: CustomTokenFormValues,
        val source: ManageTokensSource,
        val onSelectNetworkClick: (CustomTokenFormValues) -> Unit,
        val onSelectDerivationPathClick: (CustomTokenFormValues) -> Unit,
        val onCurrencyAdded: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, CustomTokenFormComponent>
}