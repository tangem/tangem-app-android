package com.tangem.data.txhistory.fetcher

import androidx.annotation.VisibleForTesting
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.cancelScope
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.defaultLaunchIn
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.receiveTrigger
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetPaymentAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.txhistory.fetcher.AccountTxHistoryFetcher
import com.tangem.domain.txhistory.fetcher.ExpressTxHistoryFetcher
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

internal class DefaultAccountTxHistoryFetcher @AssistedInject constructor(
    @Assisted override val accountId: AccountId,
    private val utils: TxHistoryFetcherUtils,
    private val singleAccountSupplier: SingleAccountSupplier,
    private val paymentAccountCurrency: GetPaymentAccountCryptoCurrencyStatusUseCase,
    private val expressFetcherFactory: DefaultExpressTxHistoryFetcher.Factory,
    private val walletManagersFacade: WalletManagersFacade,
) : AccountTxHistoryFetcher, TxHistoryFetcherUtils by utils {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val expressFetchers = ConcurrentHashMap<String, ExpressTxHistoryFetcher>()

    init {
        defaultLaunchIn(buildFlow())
    }

    override suspend fun invoke(params: TxHistoryFetchTrigger) {
        sendTrigger(params)
    }

    override fun close() {
        cancelScope()
        expressFetchers.forEach { (_, fetcher) -> fetcher.close() }
        expressFetchers.clear()
    }

    private fun buildFlow(): Flow<Unit> = channelFlow {
        val accountFlow = singleAccountSupplier(accountId).stateIn(this)

        when (val account = accountFlow.value) {
            is Account.CryptoPortfolio -> {
                account.getExpressKeys().createExpressFetcher()
                accountFlow
                    .filterIsInstance<Account.CryptoPortfolio>()
                    .controlFetchersForCryptoAccount()
                    .launchIn(this)
            }
            is Account.Payment -> {
                paymentAccountCurrency.invokeSync(walletId)
                    .getOrNull()
                    ?.controlFetchersForPaymentAccount()
                controlFetchersForPaymentAccount()
                    .launchIn(this)
            }
            // Virtual account tx-history isn't wired yet (separate task) — no express fetchers for now.
            is Account.Virtual -> Unit
        }

        receiveTrigger().onEach { trigger ->
            when (trigger) {
                is TxHistoryFetchTrigger.TokenDetailsOpen -> {
                    val addressKey = getAddress(trigger.walletId, trigger.currency) ?: return@onEach
                    expressFetchers[addressKey]?.invoke(trigger)
                }
                is TxHistoryFetchTrigger.TokenDetailsPTR -> {
                    val addressKey = getAddress(trigger.walletId, trigger.currency) ?: return@onEach
                    expressFetchers[addressKey]?.invoke(trigger)
                }
            }
        }.collect {}
    }

    private fun controlFetchersForPaymentAccount(): Flow<Unit> {
        return paymentAccountCurrency(walletId)
            .map { pair -> pair.controlFetchersForPaymentAccount() }
    }

    private suspend fun Pair<AccountStatus.Payment, CryptoCurrencyStatus>?.controlFetchersForPaymentAccount() {
        val (_, paymentCurrency) = this ?: return
        val paymentNetwork = paymentCurrency.currency.network
        val address = getAddress(walletId, paymentCurrency.currency)
        if (paymentNetwork.isSupportExpressTxHistory() && !address.isNullOrBlank()) {
            getOrPutExpressFetcher(address)
        } else {
            // single currency for payment account, so we can close all(one)
            expressFetchers.forEach { (_, fetcher) -> fetcher.close() }
            expressFetchers.clear()
        }
    }

    private fun Flow<Account.CryptoPortfolio>.controlFetchersForCryptoAccount(): Flow<Unit> {
        return map { account ->
            val newExpressKeys = account.getExpressKeys()
            val previousExpressKeys = expressFetchers.keys
            val removed = previousExpressKeys - newExpressKeys
            newExpressKeys.createExpressFetcher()
            removed.forEach { address -> expressFetchers.remove(address)?.close() }
        }
    }

    private fun Set<String>.createExpressFetcher() = this.forEach { address -> getOrPutExpressFetcher(address) }

    private suspend fun Account.CryptoPortfolio.getExpressKeys(): Set<String> {
        val currencies = this.cryptoCurrencies
        val onlyCoins = currencies.filterIsInstance<CryptoCurrency.Coin>()
        val networks = onlyCoins.map { coin -> coin.network }
        val newExpressKeys = networks
            .filter { net -> net.isSupportExpressTxHistory() }
            .mapNotNull { net -> getAddress(walletId, net) }
            .toSet()
        return newExpressKeys
    }

    @Suppress("FunctionOnlyReturningConstant") // todo txhistory check
    private fun Network.isSupportExpressTxHistory(): Boolean {
        return true
    }

    private suspend fun getAddress(userWalletId: UserWalletId, currencies: CryptoCurrency): String? =
        getAddress(userWalletId, currencies.network)

    private suspend fun getAddress(userWalletId: UserWalletId, network: Network): String? =
        walletManagersFacade.getDefaultAddress(userWalletId, network)

    private fun getOrPutExpressFetcher(address: String): ExpressTxHistoryFetcher {
        return expressFetchers.computeIfAbsent(address) { expressFetcherFactory.create(address, accountId) }
    }

    @AssistedFactory
    internal interface Factory {
        fun create(accountId: AccountId): DefaultAccountTxHistoryFetcher
    }
}