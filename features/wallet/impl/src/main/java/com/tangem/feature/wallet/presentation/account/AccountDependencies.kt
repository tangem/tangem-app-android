package com.tangem.feature.wallet.presentation.account

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import javax.inject.Inject

@ModelScoped
internal class AccountDependencies @Inject constructor(
    val accountsFeatureToggles: AccountsFeatureToggles,
    val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    val expandedAccountsHolder: ExpandedAccountsHolder,
    val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    val singleAccountListSupplier: SingleAccountListSupplier,
)