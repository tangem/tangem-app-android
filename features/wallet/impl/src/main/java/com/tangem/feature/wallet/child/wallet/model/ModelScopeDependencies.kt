package com.tangem.feature.wallet.child.wallet.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.feature.wallet.presentation.account.AccountsSharedFlowHolder
import javax.inject.Inject

@ModelScoped
internal class ModelScopeDependencies @Inject constructor(
    val accountsSharedFlowHolder: AccountsSharedFlowHolder,
)