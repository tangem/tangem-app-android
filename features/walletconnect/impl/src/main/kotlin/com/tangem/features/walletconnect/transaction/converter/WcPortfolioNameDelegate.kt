package com.tangem.features.walletconnect.transaction.converter

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.walletconnect.model.WcSession
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

internal class WcPortfolioNameDelegate @AssistedInject constructor(
    isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    @Assisted private val scope: CoroutineScope,
) {
    val isAccountMode = isAccountsModeEnabledUseCase.invoke()
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = false)

    fun createAccountTitleUM(value: WcSession): AccountTitleUM? {
        val account = when (val account = value.account) {
            is Account.CryptoPortfolio -> account
            is Account.Payment -> TODO("[REDACTED_JIRA]")
            null -> null
        }

        return if (account != null && isAccountMode.value) {
            AccountTitleUM.Account(
                prefixText = TextReference.EMPTY,
                name = account.accountName.toUM().value,
                icon = account.icon.toUM(),
            )
        } else {
            AccountTitleUM.Text(stringReference(value.wallet.name))
                .takeIf { value.showWalletInfo }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(scope: CoroutineScope): WcPortfolioNameDelegate
    }
}