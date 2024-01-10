package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.TokensAction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.presentation.wallet.analytics.PortfolioEvent
import com.tangem.feature.wallet.presentation.wallet.domain.unwrap
import com.tangem.feature.wallet.presentation.wallet.state.ActionsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter.MultiWalletCurrencyActionsConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface WalletContentClickIntents {

    fun onBackClick()

    fun onDetailsClick()

    fun onManageTokensClick()

    fun onOrganizeTokensClick()

    fun onTokenItemClick(currency: CryptoCurrency)

    fun onTokenItemLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onTransactionClick(txHash: String)
}

@Suppress("LongParameterList")
@ViewModelScoped
internal class WalletContentClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val currencyActionsClickIntentsImplementor: WalletCurrencyActionsClickIntentsImplementor,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
    private val reduxStateHolder: ReduxStateHolder,
) : BaseWalletClickIntents(), WalletContentClickIntents {

    override fun onBackClick() = router.popBackStack()

    override fun onDetailsClick() = router.openDetailsScreen()

    override fun onManageTokensClick() {
        analyticsEventHandler.send(PortfolioEvent.ButtonManageTokens)
        reduxStateHolder.dispatch(action = TokensAction.SetArgs.ManageAccess)
        router.openManageTokensScreen()
    }

    override fun onOrganizeTokensClick() {
        analyticsEventHandler.send(PortfolioEvent.OrganizeTokens)
        router.openOrganizeTokensScreen(userWalletId = stateHolder.getSelectedWalletId())
    }

    override fun onTokenItemClick(currency: CryptoCurrency) {
        analyticsEventHandler.send(PortfolioEvent.TokenTapped)
        router.openTokenDetails(stateHolder.getSelectedWalletId(), currency)
    }

    override fun onTokenItemLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        viewModelScope.launch(dispatchers.main) {
            getCryptoCurrencyActionsUseCase(userWallet = userWallet, cryptoCurrencyStatus = cryptoCurrencyStatus)
                .take(count = 1)
                .collectLatest {
                    showActionsBottomSheet(it, userWallet)
                }
        }
    }

    private fun showActionsBottomSheet(tokenActionsState: TokenActionsState, userWallet: UserWallet) {
        stateHolder.showBottomSheet(
            ActionsBottomSheetConfig(
                actions = MultiWalletCurrencyActionsConverter(
                    userWallet = userWallet,
                    clickIntents = currencyActionsClickIntentsImplementor,
                ).convert(tokenActionsState),
            ),
            userWallet.walletId,
        )
    }

    override fun onTransactionClick(txHash: String) {
        viewModelScope.launch(dispatchers.main) {
            val currency = getPrimaryCurrencyStatusUpdatesUseCase.unwrap(
                userWalletId = stateHolder.getSelectedWalletId(),
            )
                ?.currency
                ?: return@launch

            router.openUrl(
                url = getExplorerTransactionUrlUseCase(txHash = txHash, networkId = currency.network.id),
            )
        }
    }
}