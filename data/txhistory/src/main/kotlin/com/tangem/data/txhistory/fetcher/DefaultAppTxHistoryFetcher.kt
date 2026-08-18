package com.tangem.data.txhistory.fetcher

import androidx.annotation.VisibleForTesting
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.cancelScope
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.defaultLaunchIn
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.receiveTrigger
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.retryThreeTimes
import com.tangem.domain.express.ExpressRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.txhistory.fetcher.AppTxHistoryFetcher
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.domain.txhistory.fetcher.WalletTxHistoryFetcher
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class DefaultAppTxHistoryFetcher @Inject constructor(
    private val utils: TxHistoryFetcherUtils,
    private val expressRepository: ExpressRepository,
    private val onrampRepository: OnrampRepository,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val walletTxHistoryFetcherFactory: DefaultWalletTxHistoryFetcher.Factory,
) : AppTxHistoryFetcher, TxHistoryFetcherUtils by utils {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val fetchers = ConcurrentHashMap<UserWalletId, WalletTxHistoryFetcher>()

    init {
        defaultLaunchIn(buildFlow())
    }

    override fun invoke(params: TxHistoryFetchTrigger) {
        sendTrigger(params)
    }

    override fun close() {
        cancelScope()
        fetchers.forEach { (_, fetcher) -> fetcher.close() }
        fetchers.clear()
    }

    private fun buildFlow(): Flow<Unit> = channelFlow {
        val walletsFlow: StateFlow<Map<UserWalletId, UserWallet>> = getWalletsUseCase
            .invokeAsMap(isOnlyMultiCurrency = true, filterLocked = true)
            .stateIn(this)

        walletsFlow.value.keys.createForNewWallets()
        walletsFlow.value.values.firstOrNull()?.let { wallet ->
            loadExpressProviders(wallet)
            loadOnrampCurrencies(wallet)
        }

        walletsFlow
            .map { map -> map.keys }
            .distinctUntilChanged()
            .createForNewWallets()
            .closeForRemovedWallets()
            .launchIn(this)

        receiveTrigger()
            .onEach { trigger ->
                val walletId = when (trigger) {
                    is TxHistoryFetchTrigger.WalletSelected -> trigger.walletId
                    is TxHistoryFetchTrigger.TokenDetailsOpen -> trigger.walletId
                    is TxHistoryFetchTrigger.TokenDetailsPTR -> trigger.walletId
                }
                if (walletsFlow.value[walletId]?.isMultiCurrency == true) {
                    getOrPutFetcher(walletId).invoke(trigger)
                }
            }
            .collect {}
    }

    private fun ProducerScope<*>.loadExpressProviders(wallet: UserWallet) {
        flow { emit(expressRepository.getProviders(userWallet = wallet, filterProviderTypes = emptyList())) }
            .retryThreeTimes()
            .launchIn(this)
    }

    private fun ProducerScope<*>.loadOnrampCurrencies(wallet: UserWallet) {
        flow { emit(onrampRepository.fetchCurrencies(userWallet = wallet)) }
            .retryThreeTimes()
            .launchIn(this)
    }

    private fun Flow<Set<UserWalletId>>.createForNewWallets() = onEach { ids -> ids.createForNewWallets() }

    private fun Set<UserWalletId>.createForNewWallets() = this.forEach { walletId -> getOrPutFetcher(walletId) }

    private fun Flow<Set<UserWalletId>>.closeForRemovedWallets() = runningReduce { previousIds, newIds ->
        val removedWallets = previousIds.subtract(newIds)
        removedWallets.forEach { walletId -> fetchers.remove(walletId)?.close() }
        newIds
    }

    private fun getOrPutFetcher(id: UserWalletId): WalletTxHistoryFetcher {
        return fetchers.computeIfAbsent(id) { createFetcher(id) }
    }

    private fun createFetcher(id: UserWalletId): WalletTxHistoryFetcher {
        return walletTxHistoryFetcherFactory.create(id)
    }
}