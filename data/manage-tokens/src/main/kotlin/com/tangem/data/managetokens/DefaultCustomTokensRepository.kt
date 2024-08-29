package com.tangem.data.managetokens

import com.tangem.blockchain.blockchains.cardano.CardanoTokenAddressConverter
import com.tangem.blockchain.blockchains.hedera.HederaTokenAddressConverter
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.currency.getBlockchain
import com.tangem.data.common.currency.getNetwork
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.extensions.supportedBlockchains
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultCustomTokensRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : CustomTokensRepository {

    private val cryptoCurrencyFactory = CryptoCurrencyFactory()

    override suspend fun validateContractAddress(contractAddress: String, networkId: Network.ID): Boolean =
        withContext(dispatchers.io) {
            when (val blockchain = Blockchain.fromId(networkId.value)) {
                Blockchain.Unknown,
                Blockchain.Binance,
                Blockchain.BinanceTestnet,
                -> true
                Blockchain.Cardano -> blockchain.validateContractAddress(contractAddress.lowercase())
                else -> blockchain.validateAddress(contractAddress.lowercase())
            }
        }

    override suspend fun isCurrencyNotAdded(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        contractAddress: String?,
    ): Boolean {
        return withContext(dispatchers.io) {
            val storedCurrencies: UserTokensResponse = appPreferencesStore.getObjectSyncOrNull(
                key = PreferencesKeys.getUserTokensKey(userWalletId.stringValue),
            ) ?: error("User tokens not found")

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
        val userWallet = userWalletsStore.getSyncOrNull(userWalletId)
            ?: error("User wallet not found")
        val network = getNetwork(networkId, derivationPath)
        val tokenAddress = convertTokenAddress(
            networkId,
            contractAddress,
            symbol = null,
        )

        val supportedTokenNetworkIds = userWallet.scanResponse.card
            .supportedBlockchains(userWallet.scanResponse.cardTypesResolver)
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
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Coin {
        val network = getNetwork(networkId, derivationPath)

        return cryptoCurrencyFactory.createCoin(network)
    }

    override suspend fun createCustomToken(
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        formValues: AddCustomTokenForm.Validated.All,
    ): CryptoCurrency.Token {
        val network = getNetwork(networkId, derivationPath)
        val tokenAddress = convertTokenAddress(
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

    private fun convertTokenAddress(networkId: Network.ID, contractAddress: String, symbol: String?): String {
        val convertedAddress = when (getBlockchain(networkId)) {
            Blockchain.Hedera,
            Blockchain.HederaTestnet,
            -> HederaTokenAddressConverter().convertToTokenId(contractAddress)
            Blockchain.Cardano -> {
                // TODO: https://tangem.atlassian.net/browse/AND-7559
                CardanoTokenAddressConverter().convertToFingerprint(contractAddress, symbol)
            }
            else -> contractAddress
        }

        return requireNotNull(convertedAddress) {
            "Token contract address is invalid"
        }
    }
}
