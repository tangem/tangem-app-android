package com.tangem.features.send.v2.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableDialogComponent
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

interface NetworkSelectionComponent : ComposableDialogComponent {

    data class Params(
        val address: String,
        val amount: BigDecimal?,
        val memo: String?,
        val walletGroups: List<WalletGroup>,
        val onTokenSelected: (UserWalletId, CryptoCurrency) -> Unit,
        val onDismiss: () -> Unit,
    ) {
        data class WalletGroup(
            val userWalletId: UserWalletId,
            val walletName: String,
            val accounts: List<AccountGroup>,
        )

        data class AccountGroup(
            val accountId: AccountId,
            val accountName: AccountName,
            val currencies: List<CryptoCurrency>,
            val hiddenTokensCount: Int = 0,
        )
    }

    interface Factory : ComponentFactory<Params, NetworkSelectionComponent>
}