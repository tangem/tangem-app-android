package com.tangem.data.walletconnect.utils

import com.reown.walletkit.client.Wallet
import com.tangem.data.common.currency.isCustomCoin
import com.tangem.data.walletconnect.model.CAIP10
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletmanager.WalletManagersFacade
import javax.inject.Inject

internal class WcNetworksConverter @Inject constructor(
    private val namespaceConverters: Set<WcNamespaceConverter>,
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    fun createNetwork(chainId: String, wallet: UserWallet): Network? {
        return namespaceConverters
            .firstNotNullOfOrNull { it.toNetwork(chainId, wallet) }
    }

    suspend fun findWalletNetworkForRequest(
        request: WcSdkSessionRequest,
        session: WcSession,
        requestAddress: String,
    ): Network? {
        val wallet = session.wallet
        val allCoinNetwork = filterWalletNetworkForRequest(request.chainId.orEmpty(), session.wallet)

        val requestNetwork = allCoinNetwork.find { network ->
            val address = walletManagersFacade.getDefaultAddress(wallet.walletId, network)
            requestAddress.lowercase() == address?.lowercase()
        }
        return requestNetwork
    }

    /**
     * return network with not custom derivationPath or first custom or any
     */
    suspend fun mainOrAnyWalletNetworkForRequest(rawChainId: String, wallet: UserWallet): Network? {
        val networks = filterWalletNetworkForRequest(rawChainId, wallet)
        return networks.firstOrNull { !isCustomCoin(it) } ?: networks.firstOrNull()
    }

    /**
     * return all exist derivation networks
     */
    suspend fun filterWalletNetworkForRequest(rawChainId: String, wallet: UserWallet): List<Network> {
        val walletNetworks = getWalletNetworks(wallet.walletId)

        val blockchain = namespaceConverters
            .firstNotNullOfOrNull { it.toBlockchain(rawChainId) } ?: return listOf()

        val allCoinNetwork = walletNetworks.filter { it.rawId == blockchain.id }
        return allCoinNetwork
    }

    suspend fun findWalletNetworks(wallet: UserWallet, sdkSession: Wallet.Model.Session): Set<Network> {
        val walletNetworks = getWalletNetworks(wallet.walletId)
        val existNetworks = sdkSession.namespaces.values
            .map { it.accounts }.flatten().toSet()
            .mapNotNull { CAIP10.fromRaw(it) }
            .mapNotNullTo(mutableSetOf()) { caip10 ->
                val blockchain = namespaceConverters
                    .firstNotNullOfOrNull { it.toBlockchain(caip10.chainId) }
                    ?: return@mapNotNullTo null
                walletNetworks
                    // find all derivation
                    .filter { it.rawId == blockchain.id }
                    // find equal address
                    .firstOrNull {
                        val walletAddress = walletManagersFacade.getDefaultAddress(wallet.walletId, it)
                        walletAddress?.lowercase() == caip10.accountAddress.lowercase()
                    }
            }

        return existNetworks
    }

    suspend fun convertNetworksForApprove(sessionForApprove: WcSessionApprove): List<Network> {
        val walletNetworks = getWalletNetworks(sessionForApprove.wallet.walletId)
        return sessionForApprove.network
            .map { network -> walletNetworks.filter { walletNetwork -> walletNetwork.rawId == network.rawId } }
            .flatten()
    }

    private suspend fun getWalletNetworks(userWalletId: UserWalletId): List<Network> {
        return if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
            multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId = userWalletId),
            ).orEmpty()
        } else {
            currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId)
        }.filterIsInstance<CryptoCurrency.Coin>().map(CryptoCurrency.Coin::network)
    }
}