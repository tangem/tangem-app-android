package com.tangem.data.managetokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.managetokens.utils.TokenAddressesConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.card.common.extensions.canHandleBlockchain
import com.tangem.domain.card.common.extensions.hotWalletExcludedBlockchains
import com.tangem.domain.card.common.extensions.supportedBlockchains
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultCustomTokensRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val excludedBlockchains: ExcludedBlockchains,
    private val dispatchers: CoroutineDispatcherProvider,
    private val networkFactory: NetworkFactory,
) : CustomTokensRepository {

    private val excludedBlockchainsForCustom = setOf(
        Blockchain.Quai,
        Blockchain.QuaiTestnet,
    )

    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)
    private val tokenAddressConverter = TokenAddressesConverter()

    override suspend fun validateContractAddress(contractAddress: String, networkId: Network.ID): Boolean =
        withContext(dispatchers.io) {
            when (val blockchain = networkId.toBlockchain()) {
                Blockchain.Unknown,
                Blockchain.Binance,
                Blockchain.BinanceTestnet,
                Blockchain.Kaspa,
                Blockchain.TerraV1,
                Blockchain.TerraV2,
                -> true
                Blockchain.Cardano,
                Blockchain.Sui,
                Blockchain.Stellar,
                Blockchain.XRP,
                -> blockchain.validateContractAddress(contractAddress)
                else -> blockchain.validateAddress(contractAddress)
            }
        }

    override suspend fun findToken(
        userWalletId: UserWalletId,
        contractAddress: String,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Token? = withContext(dispatchers.io) {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)

        val network = requireNotNull(
            networkFactory.create(
                networkId = networkId,
                derivationPath = derivationPath,
                userWallet = userWallet,
            ),
        ) {
            "Network [$networkId] not found while finding token"
        }
        val tokenAddress = tokenAddressConverter.convertTokenAddress(
            networkId,
            contractAddress,
            symbol = null,
        )

        val supportedTokenNetworkIds = userWallet
            .supportedBlockchains(excludedBlockchains = excludedBlockchains)
            .filter(Blockchain::canHandleTokens)
            .map(Blockchain::toNetworkId)

        val response = tangemTechApi.getCoins(
            contractAddress = contractAddress,
            networkIds = network.backendId,
            active = true,
        ).getOrThrow()

        response.coins.firstNotNullOfOrNull { coin ->
            val coinNetwork = coin.networks.firstOrNull { network ->
                (network.contractAddress != null || network.decimalCount != null) &&
                    network.contractAddress.equals(tokenAddress, ignoreCase = true) &&
                    network.networkId in supportedTokenNetworkIds
            }

            if (coinNetwork != null) {
                cryptoCurrencyFactory.createToken(
                    network = network,
                    rawId = CryptoCurrency.RawID(coin.id),
                    name = coin.name,
                    symbol = coin.symbol,
                    decimals = requireNotNull(coinNetwork.decimalCount).toInt(),
                    contractAddress = tokenAddress,
                )
            } else {
                null
            }
        }
    }

    override suspend fun createCoin(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Coin {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        val network = requireNotNull(
            networkFactory.create(
                networkId = networkId,
                derivationPath = derivationPath,
                userWallet = userWallet,
            ),
        ) {
            "Network [$networkId] not found while creating coin"
        }

        return cryptoCurrencyFactory.createCoin(network)
    }

    override suspend fun createToken(
        managedCryptoCurrency: ManagedCryptoCurrency.Token,
        sourceNetwork: ManagedCryptoCurrency.SourceNetwork.Default,
        rawId: CryptoCurrency.RawID?,
    ): CryptoCurrency.Token {
        return cryptoCurrencyFactory.createToken(
            network = sourceNetwork.network,
            rawId = rawId,
            name = managedCryptoCurrency.name,
            symbol = managedCryptoCurrency.symbol,
            decimals = sourceNetwork.decimals,
            contractAddress = sourceNetwork.contractAddress,
        )
    }

    override suspend fun createCustomToken(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        formValues: AddCustomTokenForm.Validated.All,
    ): CryptoCurrency.Token {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        val network = requireNotNull(
            networkFactory.create(
                networkId = networkId,
                derivationPath = derivationPath,
                userWallet = userWallet,
            ),
        ) {
            "Network [$networkId] not found while creating custom token"
        }
        val tokenAddress = tokenAddressConverter.convertTokenAddress(
            networkId,
            formValues.contractAddress,
            formValues.symbol,
        )

        return cryptoCurrencyFactory.createToken(
            network = network,
            rawId = null,
            name = formValues.name,
            symbol = formValues.symbol,
            decimals = formValues.decimals,
            contractAddress = tokenAddress,
        )
    }

    override suspend fun convertToCryptoCurrency(
        userWalletId: UserWalletId,
        currency: ManagedCryptoCurrency.Custom,
    ): CryptoCurrency {
        return when (currency) {
            is ManagedCryptoCurrency.Custom.Coin -> createCoin(
                userWalletId = userWalletId,
                networkId = currency.network.id,
                derivationPath = currency.network.derivationPath,
            )
            is ManagedCryptoCurrency.Custom.Token -> cryptoCurrencyFactory.createToken(
                network = currency.network,
                rawId = currency.currencyId.rawCurrencyId,
                name = currency.name,
                symbol = currency.symbol,
                decimals = currency.decimals,
                contractAddress = currency.contractAddress,
            )
        }
    }

    override suspend fun getSupportedNetworks(userWalletId: UserWalletId): List<Network> = withContext(dispatchers.io) {
        when (val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)) {
            is UserWallet.Hot -> {
                Blockchain.entries.mapNotNull { blockchain ->
                    // TODO: refactor [REDACTED_JIRA]
                    val isExcluded = blockchain in excludedBlockchains || blockchain in hotWalletExcludedBlockchains
                    if (blockchain.isTestnet() || isExcluded) return@mapNotNull null

                    networkFactory.create(
                        blockchain = blockchain,
                        extraDerivationPath = null,
                        userWallet = userWallet,
                    )
                }
            }
            is UserWallet.Cold -> {
                val scanResponse = userWallet.scanResponse

                Blockchain.entries
                    .filter { it !in excludedBlockchainsForCustom }
                    .mapNotNull { blockchain ->
                        val canHandleBlockchain = scanResponse.card.canHandleBlockchain(
                            blockchain,
                            scanResponse.cardTypesResolver,
                            excludedBlockchains,
                        )

                        if (canHandleBlockchain) {
                            networkFactory.create(
                                blockchain = blockchain,
                                extraDerivationPath = null,
                                userWallet = userWallet,
                            )
                        } else {
                            null
                        }
                    }
            }
        }
    }

    override fun createDerivationPath(rawPath: String): Network.DerivationPath {
        val sdkPath = DerivationPath(rawPath)

        return Network.DerivationPath.Custom(
            value = sdkPath.rawPath,
        )
    }
}