package com.tangem.features.account.fetcher

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioFetcher.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class DefaultPortfolioFetcher @AssistedInject constructor(
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getWallets: GetWalletsUseCase,
    dispatchers: CoroutineDispatcherProvider,
    @Assisted mode: Mode,
    @Assisted private val scope: CoroutineScope,
) : PortfolioFetcher {

    private val _mode = MutableStateFlow(mode)
    private val _data = MutableSharedFlow<Data>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val data: Flow<Data>
        get() = _data.distinctUntilChanged()
    override val mode: StateFlow<Mode>
        get() = _mode

    init {
        _mode
            // reset cache if mode(StateFlow) changed
            .onEach { _data.resetReplayCache() }
            .flatMapLatest(::combineUseCases)
            .flowOn(dispatchers.default)
            .onEach { _data.emit(it) }
            .launchIn(scope)
    }

    override fun updateMode(mode: Mode) {
        _mode.value = mode
    }

    private fun combineUseCases(mode: Mode) = combine(
        flow = getWallets()
            .map { it.filterWallets(mode) }
            .distinctUntilChanged()
            .flatMapLatest { wallets -> balancesForWallets(wallets) },
        flow2 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
        flow3 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
    ) { balances, appCurrency, isBalanceHiding ->
        Data(
            appCurrency = appCurrency,
            isBalanceHidden = isBalanceHiding,
            balances = balances,
        )
    }

    private fun List<UserWallet>.filterWallets(mode: Mode): List<UserWallet> = this.filter { wallet ->
        when (mode) {
            is Mode.All -> if (mode.onlyMultiCurrency) wallet.isMultiCurrency else true
            is Mode.Wallet -> wallet.walletId == mode.walletId
        }
    }

    private fun balancesForWallets(wallets: List<UserWallet>): Flow<Map<UserWalletId, PortfolioBalance>> {
        val balanceFlows = wallets.map { walletAccountsBalancesFlow(it) }
        return combine(balanceFlows) { pairs -> pairs.toMap() }
    }

    private fun walletAccountsBalancesFlow(wallet: UserWallet): Flow<Pair<UserWalletId, PortfolioBalance>> =
        accountStatusListFlow(wallet).map { wallet.walletId to PortfolioBalance(wallet, it) }

    private fun accountStatusListFlow(wallet: UserWallet): Flow<AccountStatusList> =
        singleAccountStatusListSupplier(SingleAccountStatusListProducer.Params(wallet.walletId))

    @AssistedFactory
    interface Factory : PortfolioFetcher.Factory {
        override fun create(mode: Mode, scope: CoroutineScope): DefaultPortfolioFetcher
    }
}