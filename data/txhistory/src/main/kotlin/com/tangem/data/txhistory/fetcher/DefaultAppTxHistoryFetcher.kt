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
import com.tangem.domain.txhistory.fetcher.AppTxHistoryFetcher
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.domain.txhistory.fetcher.WalletTxHistoryFetcher
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class DefaultAppTxHistoryFetcher @Inject constructor(
    private val utils: TxHistoryFetcherUtils,
    private val expressRepository: ExpressRepository,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val selectedWalletUseCase: GetSelectedWalletUseCase,
    private val walletTxHistoryFetcherFactory: DefaultWalletTxHistoryFetcher.Factory,
) : AppTxHistoryFetcher, TxHistoryFetcherUtils by utils {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val fetchers = ConcurrentHashMap<UserWalletId, WalletTxHistoryFetcher>()

    /** Wallets whose express providers were already loaded — to load them at most once per wallet. */
    private val providersLoadedWallets = mutableSetOf<UserWalletId>()

    init {
        defaultLaunchIn(buildFlow())
    }

    override suspend fun invoke(params: TxHistoryFetchTrigger) {
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

        selectedWalletUseCase.selectedFlow()
            .filter { wallet -> wallet.isMultiCurrency }
            // todo txhistory some init trigger?
            .onEach { wallet -> loadExpressProviders(wallet) }
            .launchIn(this)

        walletsFlow
            .map { map -> map.keys }
            .distinctUntilChanged()
            // todo txhistory create for all or lazy?
            .createForNewWallets()
            .closeForRemovedWallets()
            .launchIn(this)

        receiveTrigger()
            .onEach { trigger ->
                when (trigger) {
                    is TxHistoryFetchTrigger.TokenDetailsOpen -> fetchers[trigger.walletId]?.invoke(trigger)
                    is TxHistoryFetchTrigger.TokenDetailsPTR -> fetchers[trigger.walletId]?.invoke(trigger)
                }
            }
            .collect {}
    }

    private fun ProducerScope<*>.loadExpressProviders(wallet: UserWallet) {
        // Load once per wallet: `add` returns false if this walletId was already loaded.
        if (!providersLoadedWallets.add(wallet.walletId)) return
        flow { emit(expressRepository.getProviders(userWallet = wallet, filterProviderTypes = emptyList())) }
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