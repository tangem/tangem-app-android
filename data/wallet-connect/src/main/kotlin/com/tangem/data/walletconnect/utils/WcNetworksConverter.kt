package com.tangem.data.walletconnect.utils

import com.reown.walletkit.client.Wallet
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.data.common.currency.isCustomCoin
import com.tangem.data.walletconnect.model.CAIP10
import com.tangem.domain.account.producer.SingleAccountProducer
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletmanager.WalletManagersFacade
import javax.inject.Inject

internal class WcNetworksConverter @Inject constructor(
    private val namespaceConverters: Set<WcNamespaceConverter>,
    private val walletManagersFacade: WalletManagersFacade,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val singleAccountSupplier: SingleAccountSupplier,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
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
            val address = getAddressForWC(wallet.walletId, network)
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

    suspend fun allAddressForChain(rawChainId: String, wallet: UserWallet): List<String> {
        return filterWalletNetworkForRequest(rawChainId, wallet)
            .mapNotNull { getAddressForWC(wallet.walletId, it)?.lowercase() }
    }

    suspend fun getAddressForWC(userWalletId: UserWalletId, network: Network): String? {
        return when (network.toBlockchain()) {
            Blockchain.XDC,
            Blockchain.XDCTestnet,
            -> walletManagersFacade.getAddresses(userWalletId, network)
                .find { address -> address.type == AddressType.Legacy }
                ?.value
            else -> walletManagersFacade.getDefaultAddress(userWalletId, network)
        }
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

    suspend fun findWalletNetworks(
        wallet: UserWallet,
        account: Account?,
        sdkSession: Wallet.Model.Session,
    ): Set<Network> {
        val portfolioNetworks = account?.let { getAccountNetworks(it.accountId) }
            ?: getWalletNetworks(wallet.walletId)
        val existNetworks = sdkSession.namespaces.values
            .map { it.accounts }.flatten().toSet()
            .mapNotNull { CAIP10.fromRaw(it) }
            .mapNotNullTo(mutableSetOf()) { caip10 ->
                val blockchain = namespaceConverters
                    .firstNotNullOfOrNull { it.toBlockchain(caip10.chainId) }
                    ?: return@mapNotNullTo null
                portfolioNetworks
                    // find all derivation
                    .filter { it.rawId == blockchain.id }
                    // find equal address
                    .firstOrNull { network ->
                        val walletAddress = getAddressForWC(wallet.walletId, network)
                        walletAddress?.lowercase() == caip10.accountAddress.lowercase()
                    }
            }

        return existNetworks
    }

    suspend fun getAccount(accountId: AccountId): Account? {
        return singleAccountSupplier.getSyncOrNull(SingleAccountProducer.Params(accountId))
    }

    suspend fun convertNetworksForApprove(sessionForApprove: WcSessionApprove): List<Network> {
        val portfolioNetworks = sessionForApprove.account?.let { getAccountNetworks(it.accountId) }
            ?: getWalletNetworks(sessionForApprove.wallet.walletId)
        return sessionForApprove.network
            .map { network -> portfolioNetworks.filter { walletNetwork -> walletNetwork.rawId == network.rawId } }
            .flatten()
    }

    suspend fun getWalletNetworks(userWalletId: UserWalletId): List<Network> {
        return multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
            params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId = userWalletId),
        )
            .orEmpty()
            .filterIsInstance<CryptoCurrency.Coin>().map(CryptoCurrency.Coin::network)
    }

    private suspend fun getAccountStatus(accountId: AccountId): AccountStatus.Crypto? {
        return singleAccountStatusListSupplier.getSyncOrNull(
            SingleAccountStatusListProducer.Params(accountId.userWalletId),
        )?.accountStatuses
            ?.filterIsInstance<AccountStatus.Crypto>()
            ?.find { it.account.accountId == accountId }
    }

    suspend fun getAccountNetworks(accountId: AccountId): List<Network> {
        return getAccountStatus(accountId)
            ?.flattenCurrencies()
            ?.map { it.currency }
            ?.filterIsInstance<CryptoCurrency.Coin>()
            ?.map(CryptoCurrency.Coin::network)
            ?: emptyList()
    }
}