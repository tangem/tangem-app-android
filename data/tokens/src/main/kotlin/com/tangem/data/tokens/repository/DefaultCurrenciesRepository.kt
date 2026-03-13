package com.tangem.data.tokens.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.currency.getTokenId
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import com.tangem.blockchain.common.FeePaidCurrency as FeePaidSdkCurrency

internal class DefaultCurrenciesRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
    excludedBlockchains: ExcludedBlockchains,
) : CurrenciesRepository {

    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)

    override suspend fun isSendBlockedByPendingTransactions(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Boolean {
        val blockchain = cryptoCurrencyStatus.currency.network.toBlockchain()
        val isBitcoinBlockchain =
            blockchain == Blockchain.Bitcoin || blockchain == Blockchain.BitcoinTestnet
        return when {
            cryptoCurrencyStatus.currency is CryptoCurrency.Coin && isBitcoinBlockchain -> {
                val outgoingTransactions =
                    cryptoCurrencyStatus.value.pendingTransactions.filter { it.isOutgoing }
                outgoingTransactions.isNotEmpty()
            }

            blockchain.isEvm() -> false
            blockchain == Blockchain.Tron || blockchain == Blockchain.TronTestnet -> false
            else -> {
                val walletManager = walletManagersFacade.getOrCreateWalletManager(
                    userWalletId = userWalletId,
                    network = cryptoCurrencyStatus.currency.network,
                ) ?: return false

                walletManager.wallet.recentTransactions.any { it.status == TransactionStatus.Unconfirmed }
            }
        }
    }

    override suspend fun getFeePaidCurrency(userWalletId: UserWalletId, network: Network): FeePaidCurrency {
        return withContext(dispatchers.io) {
            val blockchain = network.toBlockchain()
            when (val feePaidCurrency = blockchain.feePaidCurrency()) {
                FeePaidSdkCurrency.Coin -> FeePaidCurrency.Coin
                FeePaidSdkCurrency.SameCurrency -> FeePaidCurrency.SameCurrency
                is FeePaidSdkCurrency.Token -> {
                    val balance = walletManagersFacade.tokenBalance(
                        userWalletId = userWalletId,
                        network = network,
                        name = feePaidCurrency.token.name,
                        symbol = feePaidCurrency.token.symbol,
                        contractAddress = feePaidCurrency.token.contractAddress,
                        decimals = feePaidCurrency.token.decimals,
                        id = feePaidCurrency.token.id,
                    )
                    FeePaidCurrency.Token(
                        tokenId = getTokenId(network = network, sdkToken = feePaidCurrency.token),
                        name = feePaidCurrency.token.name,
                        symbol = feePaidCurrency.token.symbol,
                        contractAddress = feePaidCurrency.token.contractAddress,
                        balance = balance,
                    )
                }

                is FeePaidSdkCurrency.FeeResource -> FeePaidCurrency.FeeResource(currency = feePaidCurrency.currency)
            }
        }
    }

    override fun createCoinCurrency(network: Network): CryptoCurrency.Coin {
        return cryptoCurrencyFactory.createCoin(network = network)
    }

    override fun createTokenCurrency(cryptoCurrency: CryptoCurrency.Token, network: Network): CryptoCurrency.Token {
        return cryptoCurrencyFactory.createToken(
            cryptoCurrency = cryptoCurrency,
            network = network,
        )
    }

    override suspend fun createTokenCurrency(
        userWalletId: UserWalletId,
        contractAddress: String,
        networkId: String,
    ): CryptoCurrency.Token {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        val token = withContext(dispatchers.io) {
            val foundToken = tangemTechApi.getCoins(
                contractAddress = contractAddress,
                networkId = networkId,
            )
                .getOrThrow()
                .coins
                .firstOrNull()
                ?: error("Token not found")
            val network = foundToken.networks.firstOrNull { it.networkId == networkId }
                ?: error("Network not found")
            CryptoCurrencyFactory.Token(
                symbol = foundToken.symbol,
                name = foundToken.name,
                contractAddress = contractAddress,
                decimals = network.decimalCount?.toInt() ?: error("Decimals not found"),
                id = foundToken.id,
            )
        }
        return cryptoCurrencyFactory.createToken(
            token = token,
            networkId = networkId,
            extraDerivationPath = null,
            userWallet = userWallet,
        ) ?: error("Unable to create token")
    }

    override fun isNetworkFeeZero(userWalletId: UserWalletId, network: Network): Boolean {
        val blockchain = Blockchain.fromNetworkId(network.backendId)
        return blockchain?.isNetworkFeeZero() == true
    }
}