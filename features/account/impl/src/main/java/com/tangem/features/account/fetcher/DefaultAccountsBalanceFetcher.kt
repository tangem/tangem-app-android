package com.tangem.features.account.fetcher

import arrow.core.getOrElse
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.account.AccountsBalanceFetcher
import com.tangem.features.account.AccountsBalanceFetcher.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

internal class DefaultAccountsBalanceFetcher @AssistedInject constructor(
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getWallets: GetWalletsUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    @Assisted mode: Mode,
    @Assisted private val scope: CoroutineScope,
) : AccountsBalanceFetcher {

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
        _mode.flatMapLatest(::combineUseCases)
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
        flow2 = appCurrencyFlow(),
        flow3 = balanceHidingFlow(),
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

    private fun balancesForWallets(wallets: List<UserWallet>): Flow<Map<UserWallet, Map<Account, AccountBalance>>> =
        wallets.asFlow()
            .map { walletAccountsBalancesFlow(it) }
            .mapLatest { accountsBalances -> combine(accountsBalances) { pairs -> pairs.toMap() } }
            .flattenConcat()

    private fun walletAccountsBalancesFlow(wallet: UserWallet): Flow<Pair<UserWallet, Map<Account, AccountBalance>>> =
        walletAccounts(wallet)
            .distinctUntilChanged()
            .map { list -> list.map(::accountBalanceFlow) }
            .mapLatest { balanceFlows -> combine(balanceFlows) { balances -> balances.toMap() } }
            .flattenConcat()
            .map { accountBalance -> wallet to accountBalance }

    private fun walletAccounts(wallet: UserWallet): Flow<List<Account>> = flow {
        // todo account load accounts
        val accounts: List<Account> = Account.CryptoPortfolio
            .createMainAccount(wallet.walletId)
            .let(::listOf)
        emit(accounts)
    }

    private fun accountBalanceFlow(account: Account): Flow<Pair<Account, AccountBalance>> = flow {
        // todo account load balance
        val balance = AccountBalance(balance = Lce.Content(TotalFiatBalance.Loading))
        emit(account to balance)
    }

    private fun appCurrencyFlow(): Flow<AppCurrency> {
        return getSelectedAppCurrencyUseCase()
            .map { it.getOrElse { AppCurrency.Default } }
            .distinctUntilChanged()
    }

    private fun balanceHidingFlow(): Flow<Boolean> {
        return getBalanceHidingSettingsUseCase()
            .map { it.isBalanceHidden }
            .distinctUntilChanged()
    }

    @AssistedFactory
    interface Factory : AccountsBalanceFetcher.Factory {
        override fun create(mode: Mode, scope: CoroutineScope): DefaultAccountsBalanceFetcher
    }
}