package com.tangem.data.managetokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.currency.getNetwork
import com.tangem.data.managetokens.utils.TokenAddressesConverter
import com.tangem.data.tokens.utils.UserTokensBackwardCompatibility
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.extensions.canHandleBlockchain
import com.tangem.domain.common.extensions.supportedBlockchains
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class DefaultCustomTokensRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val appPreferencesStore: AppPreferencesStore,
    private val walletManagersFacade: WalletManagersFacade,
    private val excludedBlockchains: ExcludedBlockchains,
    private val dispatchers: CoroutineDispatcherProvider,
) : CustomTokensRepository {

    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)
    private val userTokensResponseFactory = UserTokensResponseFactory()
    private val tokenAddressConverter = TokenAddressesConverter()
    private val userTokensBackwardCompatibility = UserTokensBackwardCompatibility()

    override suspend fun validateContractAddress(contractAddress: String, networkId: Network.ID): Boolean =
        withContext(dispatchers.io) {
            when (val blockchain = Blockchain.fromId(networkId.value)) {
                Blockchain.Unknown,
                Blockchain.Binance,
                Blockchain.BinanceTestnet,
                -> true
                Blockchain.Cardano -> blockchain.validateContractAddress(contractAddress)
                else -> blockchain.validateAddress(contractAddress)
            }
        }

    override suspend fun isCurrencyNotAdded(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        contractAddress: String?,
    ): Boolean {
        return withContext(dispatchers.io) {
            val storedCurrencies = appPreferencesStore.getObjectSyncOrNull<UserTokensResponse>(
                key = PreferencesKeys.getUserTokensKey(userWalletId.stringValue),
            )
            requireNotNull(storedCurrencies) {
                "User tokens not found for user wallet [$userWalletId] while checking if currency is not added"
            }

            storedCurrencies.tokens.none { token ->
                Blockchain.fromId(networkId.value).toNetworkId() == token.networkId &&
                    derivationPath.value == token.derivationPath &&
                    contractAddress.equals(token.contractAddress, ignoreCase = true)
            }
        }
    }

    override suspend fun findToken(
        userWalletId: UserWalletId,
        contractAddress: String,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Token? = withContext(dispatchers.io) {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "User wallet [$userWalletId] not found while finding token"
        }
        val network = requireNotNull(
            getNetwork(networkId, derivationPath, userWallet.scanResponse, excludedBlockchains),
        ) {
            "Network [$networkId] not found while finding token"
        }
        val tokenAddress = tokenAddressConverter.convertTokenAddress(
            networkId,
            contractAddress,
            symbol = null,
        )

        val supportedTokenNetworkIds = userWallet.scanResponse.card
            .supportedBlockchains(userWallet.scanResponse.cardTypesResolver, excludedBlockchains)
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
                    rawId = coin.id,
                    name = coin.name,
                    symbol = coin.symbol,
                    decimals = coinNetwork.decimalCount!!.toInt(),
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
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "User wallet [$userWalletId] not found while creating coin"
        }
        val network = requireNotNull(
            getNetwork(networkId, derivationPath, userWallet.scanResponse, excludedBlockchains),
        ) {
            "Network [$networkId] not found while creating coin"
        }

        return cryptoCurrencyFactory.createCoin(network)
    }

    override suspend fun createToken(
        managedCryptoCurrency: ManagedCryptoCurrency.Token,
        sourceNetwork: ManagedCryptoCurrency.SourceNetwork.Default,
        rawId: String?,
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
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "User wallet [$userWalletId] not found while creating custom token"
        }
        val network = requireNotNull(
            getNetwork(networkId, derivationPath, userWallet.scanResponse, excludedBlockchains),
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

    override suspend fun removeCurrency(userWalletId: UserWalletId, currency: ManagedCryptoCurrency.Custom) =
        withContext(dispatchers.io) {
            val cryptoCurrency = when (currency) {
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
            val storedCurrencies = appPreferencesStore.getObjectSyncOrNull<UserTokensResponse>(
                key = PreferencesKeys.getUserTokensKey(userWalletId.stringValue),
            )
            requireNotNull(storedCurrencies) {
                "User tokens not found for user wallet [$userWalletId] while removing currency"
            }

            val token = userTokensResponseFactory.createResponseToken(cryptoCurrency)
            storeAndPushTokens(
                userWalletId = userWalletId,
                response = storedCurrencies.copy(tokens = storedCurrencies.tokens.filterNot { it == token }),
            )
            when (cryptoCurrency) {
                is CryptoCurrency.Coin -> walletManagersFacade.remove(userWalletId, setOf(cryptoCurrency.network))
                is CryptoCurrency.Token -> walletManagersFacade.removeTokens(userWalletId, setOf(cryptoCurrency))
            }
        }

    override suspend fun getSupportedNetworks(userWalletId: UserWalletId): List<Network> = withContext(dispatchers.io) {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "User wallet [$userWalletId] not found while getting supported networks"
        }
        val scanResponse = userWallet.scanResponse

        Blockchain.entries
            .mapNotNull { blockchain ->
                val canHandleBlockchain = scanResponse.card.canHandleBlockchain(
                    blockchain,
                    scanResponse.cardTypesResolver,
                    excludedBlockchains,
                )

                if (canHandleBlockchain) {
                    getNetwork(
                        blockchain = blockchain,
                        extraDerivationPath = null,
                        scanResponse = scanResponse,
                        excludedBlockchains = excludedBlockchains,
                    )
                } else {
                    null
                }
            }
    }

    override fun createDerivationPath(rawPath: String): Network.DerivationPath {
        val sdkPath = DerivationPath(rawPath)

        return Network.DerivationPath.Custom(
            value = sdkPath.rawPath,
        )
    }

    private suspend fun storeAndPushTokens(userWalletId: UserWalletId, response: UserTokensResponse) {
        val compatibleUserTokensResponse = userTokensBackwardCompatibility.applyCompatibilityAndGetUpdated(response)
        appPreferencesStore.storeObject(
            key = PreferencesKeys.getUserTokensKey(userWalletId = userWalletId.stringValue),
            value = compatibleUserTokensResponse,
        )

        pushTokens(userWalletId, response)
    }

    private suspend fun pushTokens(userWalletId: UserWalletId, response: UserTokensResponse) {
        safeApiCall({ tangemTechApi.saveUserTokens(userWalletId.stringValue, response).bind() }) {
            Timber.e(it, "Unable to push user tokens for: ${userWalletId.stringValue}")
        }
    }
}
