package com.tangem.features.account

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.account.Account

interface AccountDetailsComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Params, AccountDetailsComponent>

    data class Params(val account: Account)
}