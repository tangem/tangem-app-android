package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.isShown
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TokensListItemUM2

/** The collectibles account as one [TangemAccountRow] of the wallet account list */
internal class WalletCollectiblesAccountItemConverter(
    private val appCurrency: AppCurrency,
    private val onAccountClick: (UserWalletId) -> Unit,
) {

    fun convert(value: AccountStatus.Collectibles): TokensListItemUM2.Collectibles? {
        val status = value.value
        if (!status.isShown) return null

        val id = value.accountId.value
        val title = resourceReference(R.string.collectibles_main_title)
        val onClick = { onAccountClick(value.accountId.userWalletId) }
        val fiatBalance = status.fiatBalance

        val state = when {
            status is CollectiblesAccountStatusValue.Loading -> TangemAccountRow.State.Shimmer(id = id)
            status is CollectiblesAccountStatusValue.MissedDerivation -> TangemAccountRow.State.NotActive(
                id = id,
                title = title,
                description = resourceReference(R.string.collectibles_account_reconnect_subtitle),
            )
            fiatBalance == null -> TangemAccountRow.State.BalanceError(id = id, title = title, onClick = onClick)
            else -> TangemAccountRow.State.Content(
                id = id,
                title = title,
                balance = stringReference(
                    fiatBalance.format {
                        fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                    },
                ),
                isBalanceFlickering = status.source == StatusSource.CACHE,
                onClick = onClick,
            )
        }

        return TokensListItemUM2.Collectibles(
            state = state,
            avatar = TangemAvatar.UiState.Account(kind = TangemAvatar.AccountKind.Gacha),
        )
    }
}