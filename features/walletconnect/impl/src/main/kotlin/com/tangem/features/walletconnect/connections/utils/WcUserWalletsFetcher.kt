package com.tangem.features.walletconnect.connections.utils

import com.tangem.common.ui.R
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.wallet.utils.UserWalletsFetcher
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@ModelScoped
internal class WcUserWalletsFetcher(
    userWalletsFetcherFactory: UserWalletsFetcher.Factory,
    messageSender: UiMessageSender,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val onWalletSelected: (UserWalletId) -> Unit,
) {

    private val userWalletsFetcher = userWalletsFetcherFactory.create(
        messageSender = messageSender,
        onlyMultiCurrency = true,
        isAuthMode = false,
        isClickableIfLocked = false,
        onWalletClick = { onWalletSelected(it) },
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val userWallets: Flow<ImmutableList<UserWalletItemUM>> = userWalletsFetcher.userWallets
        .flatMapLatest { listOfWalletItem ->
            val flows = listOfWalletItem.map(::getTokenListFlow)
            combine(flows) { it.toList().toImmutableList() }
        }

    private fun getTokenListFlow(walletItem: UserWalletItemUM): Flow<UserWalletItemUM> {
        return singleAccountStatusListSupplier(userWalletId = UserWalletId(walletItem.id)).map { accountStatusList ->
            val information = when (accountStatusList.totalFiatBalance) {
                is TotalFiatBalance.Failed -> UserWalletItemUM.Information.Failed
                is TotalFiatBalance.Loading -> UserWalletItemUM.Information.Loading
                is TotalFiatBalance.Loaded -> tokenCountInfo(accountStatusList.flattenCurrencies().size)
            }

            walletItem.copy(information = information)
        }
    }

    private fun tokenCountInfo(count: Int): UserWalletItemUM.Information.Loaded {
        val text = TextReference.PluralRes(
            id = R.plurals.card_label_token_count,
            count = count,
            formatArgs = wrappedList(count),
        )
        return UserWalletItemUM.Information.Loaded(text)
    }
}