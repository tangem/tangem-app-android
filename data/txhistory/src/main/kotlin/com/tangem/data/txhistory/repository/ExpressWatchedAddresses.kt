package com.tangem.data.txhistory.repository

import com.tangem.data.txhistory.repository.factory.matches
import com.tangem.data.txhistory.repository.factory.toAssetId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.YieldModuleAddressProvider
import com.tangem.store.datasource.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Every address a currency's express history is watched under: all of the wallet's address types plus the dynamic
 * (UTXO) addresses already used. A payout may land on a non-default type — a swap paid out to the wallet's own legacy
 * P2PKH BTC address is still its incoming leg — so the default address alone is not enough. A swap out of Yield Mode

 */
internal class ExpressWatchedAddresses @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
    private val yieldModuleAddressProvider: YieldModuleAddressProvider,
) {

    suspend fun getWalletAddresses(userWalletId: UserWalletId, currency: CryptoCurrency): List<String> {
        val network = currency.network
        val walletAddresses = walletManagersFacade.getAddresses(userWalletId, network).map { it.value }
        val dynamicAddresses = walletManagersFacade.usedDynamicAddresses(userWalletId, network).orEmpty()
        return (walletAddresses + dynamicAddresses).distinct()
    }

    /**
     * [walletAddresses] plus the yield module, resolved only when they leave open whether [exchange] was paid from
     * [currency]: the module may take an RPC call, and a deal read by id should not wait on it otherwise.
     */
    suspend fun withYieldModuleIfUnsettled(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        walletAddresses: List<String>,
        exchange: ExpressExchangeEntity?,
    ): List<String> {
        val isUnsettled = exchange != null &&
            currency.matches(exchange.from.toAssetId()) &&
            walletAddresses.none { it.equals(exchange.fromAddress, ignoreCase = true) }
        if (!isUnsettled) return walletAddresses
        val yieldModuleAddress = getYieldModuleAddress(userWalletId, currency) ?: return walletAddresses
        return (walletAddresses + yieldModuleAddress).distinct()
    }

    /**
     * The watched addresses for a list: the yield module may take an RPC call to resolve, so the wallet's own
     * addresses are emitted first and the module joins them once known.
     */
    fun observe(userWalletId: UserWalletId, currency: CryptoCurrency): Flow<List<String>> = flow {
        val walletAddresses = getWalletAddresses(userWalletId, currency)
        emit(walletAddresses)
        val yieldModuleAddress = getYieldModuleAddress(userWalletId, currency) ?: return@flow
        emit((walletAddresses + yieldModuleAddress).distinct())
    }

    private suspend fun getYieldModuleAddress(userWalletId: UserWalletId, currency: CryptoCurrency): String? {
        if (currency !is CryptoCurrency.Token) return null
        return runSuspendCatching { yieldModuleAddressProvider.getOrFetch(userWalletId, currency.network) }
            .onFailure { TangemLogger.e("Yield module address is unavailable for the express history", it) }
            .getOrNull()
    }
}