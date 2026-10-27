package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount

interface TangemPayWithdrawHubComponent : ComposableContentComponent {

    data class Params(val sourceAccount: BankWithdrawSourceAccount)

    interface Factory : ComponentFactory<Params, TangemPayWithdrawHubComponent>
}