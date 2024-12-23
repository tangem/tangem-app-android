package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import arrow.core.getOrElse
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.settings.ShouldShowMarketsTooltipUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.TokensAction
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.wallet.presentation.wallet.domain.OnrampStatusFactory
import com.tangem.feature.wallet.presentation.wallet.domain.unwrap
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.ActionsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.CloseBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.OpenBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.MultiWalletCurrencyActionsConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal interface WalletContentClickIntents {

    fun onBackClick()

    fun onDetailsClick()

    fun onManageTokensClick()

    fun onOrganizeTokensClick()

    fun onOnrampSuccessClick(externalTxId: String)

    fun onDismissMarketsOnboarding()

    fun onTokenItemClick(currencyStatus: CryptoCurrencyStatus)

    fun onTokenItemLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onTransactionClick(txHash: String)

    fun onDissmissBottomSheet()

    fun onGoToProviderClick(externalTxId: String)

    fun onExpressTransactionClick(txId: String)
}

@Suppress("LongParameterList")
@ViewModelScoped
internal class WalletContentClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val currencyActionsClickIntents: WalletCurrencyActionsClickIntentsImplementor,
    private val walletWarningsClickIntents: WalletWarningsClickIntentsImplementor,
    private val onrampStatusFactory: OnrampStatusFactory,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val shouldShowMarketsTooltipUseCase: ShouldShowMarketsTooltipUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val reduxStateHolder: ReduxStateHolder,
) : BaseWalletClickIntents(), WalletContentClickIntents {

    override fun onBackClick() = router.popBackStack()

    override fun onDetailsClick() {
        viewModelScope.launch(dispatchers.main) {
            val userWalletId = stateHolder.getSelectedWalletId()
            val userWallet = getUserWalletUseCase(userWalletId).getOrElse {
                Timber.e(
                    """
                        Unable to get user wallet
                        |- ID: $userWalletId
                        |- Exception: $it
                    """.trimIndent(),
                )

                return@launch
            }

            if (userWallet.isLocked) {
                stateHolder.showBottomSheet(
                    WalletBottomSheetConfig.UnlockWallets(
                        onUnlockClick = walletWarningsClickIntents::onUnlockWalletClick,
                        onScanClick = walletWarningsClickIntents::onScanToUnlockWalletClick,
                    ),
                )
            } else {
                router.openDetailsScreen(stateHolder.getSelectedWalletId())
            }
        }
    }

    override fun onManageTokensClick() {
        reduxStateHolder.dispatch(action = TokensAction.SetArgs.ManageAccess)
        router.openManageTokensScreen(userWalletId = stateHolder.getSelectedWalletId())
    }

    override fun onOrganizeTokensClick() {
        router.openOrganizeTokensScreen(userWalletId = stateHolder.getSelectedWalletId())
    }

    override fun onOnrampSuccessClick(externalTxId: String) {
        router.openOnrampSuccessScreen(externalTxId = externalTxId)
    }

    override fun onDismissMarketsOnboarding() {
        stateHolder.update { it.copy(showMarketsOnboarding = false) }
        viewModelScope.launch {
            shouldShowMarketsTooltipUseCase(isShown = true)
        }
    }

    override fun onTokenItemClick(currencyStatus: CryptoCurrencyStatus) {
        router.openTokenDetails(stateHolder.getSelectedWalletId(), currencyStatus)
    }

    override fun onTokenItemLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        viewModelScope.launch(dispatchers.main) {
            val userWalletId = stateHolder.getSelectedWalletId()
            val userWallet = getUserWalletUseCase(userWalletId).getOrElse {
                Timber.e(
                    """
                        Unable to get user wallet
                        |- ID: $userWalletId
                        |- Exception: $it
                    """.trimIndent(),
                )

                return@launch
            }

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
                    clickIntents = currencyActionsClickIntents,
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

            getExplorerTransactionUrlUseCase(txHash = txHash, networkId = currency.network.id)
                .fold(
                    ifLeft = { Timber.e(it.toString()) },
                    ifRight = { router.openUrl(url = it) },
                )
        }
    }

    override fun onGoToProviderClick(externalTxUrl: String) {
        router.openUrl(externalTxUrl)
    }

    override fun onDissmissBottomSheet() {
        val userWalletId = stateHolder.getSelectedWalletId()
        if (stateHolder.getSelectedWallet().bottomSheetConfig?.content is ExpressStatusBottomSheetConfig) {
            viewModelScope.launch(dispatchers.main) {
                onrampStatusFactory.removeTransactionOnBottomSheetClosed()
            }
        }
        stateHolder.update(CloseBottomSheetTransformer(userWalletId))
    }

    override fun onExpressTransactionClick(txId: String) {
        viewModelScope.launch {
            val userWalletId = stateHolder.getSelectedWalletId()
            val singleWalletState = stateHolder.getSelectedWallet() as? WalletState.SingleCurrency.Content
                ?: return@launch

            val expressTransaction = singleWalletState.expressTxsToDisplay.firstOrNull { it.info.txId == txId }
                ?: return@launch

            stateHolder.update(
                OpenBottomSheetTransformer(
                    userWalletId = userWalletId,
                    content = ExpressStatusBottomSheetConfig(
                        value = expressTransaction,
                    ),
                    onDismissBottomSheet = ::onDissmissBottomSheet,
                ),
            )
        }
    }
}