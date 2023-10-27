package com.tangem.feature.swap

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Approver
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.datasource.api.oneinch.OneInchApi
import com.tangem.datasource.api.oneinch.OneInchApiFactory
import com.tangem.datasource.api.oneinch.OneInchErrorsHandler
import com.tangem.datasource.api.oneinch.errors.OneIncResponseException
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.config.ConfigManager
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.converters.QuotesConverter
import com.tangem.feature.swap.converters.SwapConverter
import com.tangem.feature.swap.converters.TokensConverter
import com.tangem.feature.swap.domain.SwapRepository
import com.tangem.feature.swap.domain.models.data.AggregatedSwapDataModel
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.QuoteModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.mapErrors
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import com.tangem.blockchain.common.Token as SdkToken

internal class SwapRepositoryImpl @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val oneInchApiFactory: OneInchApiFactory,
    private val oneInchErrorsHandler: OneInchErrorsHandler,
    private val coroutineDispatcher: CoroutineDispatcherProvider,
    private val configManager: ConfigManager,
    private val walletManagersFacade: WalletManagersFacade,
) : SwapRepository {

    private val tokensConverter = TokensConverter()
    private val quotesConverter = QuotesConverter()
    private val swapConverter = SwapConverter()

    override suspend fun getRates(currencyId: String, tokenIds: List<String>): Map<String, Double> {
        // workaround cause backend do not return arbitrum and optimism rates
        val addedTokens = if (tokenIds.contains(OPTIMISM_ID) || tokenIds.contains(ARBITRUM_ID)) {
            tokenIds.toMutableList().apply {
                add(ETHEREUM_ID)
            }
        } else {
            tokenIds
        }
        return withContext(coroutineDispatcher.io) {
            val rates = tangemTechApi.getRates(currencyId.lowercase(), addedTokens.joinToString(",")).rates
            val ethRate = rates[ETHEREUM_ID]
            if (tokenIds.contains(OPTIMISM_ID) || tokenIds.contains(ARBITRUM_ID)) {
                rates.toMutableMap().apply {
                    put(OPTIMISM_ID, ethRate ?: 0.0)
                    put(ARBITRUM_ID, ethRate ?: 0.0)
                }
            } else {
                rates
            }
        }
    }

    override suspend fun getExchangeableTokens(networkId: String): List<Currency> {
        return withContext(coroutineDispatcher.io) {
            tokensConverter.convertList(
                tangemTechApi.getCoins(
                    exchangeable = true,
                    active = true,
                    networkIds = networkId,
                ).coins,
            )
        }
    }

    override suspend fun findBestQuote(
        networkId: String,
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: String,
    ): AggregatedSwapDataModel<QuoteModel> {
        return withContext(coroutineDispatcher.io) {
            try {
                val response = oneInchErrorsHandler.handleOneInchResponse(
                    getOneInchApi(networkId).quote(
                        fromTokenAddress = fromTokenAddress,
                        toTokenAddress = toTokenAddress,
                        amount = amount,
                    ),
                )
                AggregatedSwapDataModel(dataModel = quotesConverter.convert(response))
            } catch (ex: OneIncResponseException) {
                AggregatedSwapDataModel(null, mapErrors(ex.data.description))
            }
        }
    }

    override suspend fun addressForTrust(networkId: String): String {
        return withContext(coroutineDispatcher.io) {
            getOneInchApi(networkId).approveSpender().address
        }
    }

    override suspend fun prepareSwapTransaction(
        networkId: String,
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: String,
        fromWalletAddress: String,
        slippage: Int,
    ): AggregatedSwapDataModel<SwapDataModel> {
        return withContext(coroutineDispatcher.io) {
            try {
                val swapResponse = oneInchErrorsHandler.handleOneInchResponse(
                    getOneInchApi(networkId).swap(
                        fromTokenAddress = fromTokenAddress,
                        toTokenAddress = toTokenAddress,
                        amount = amount,
                        fromAddress = fromWalletAddress,
                        slippage = slippage,
                        referrerAddress = configManager.config.swapReferrerAccount?.address,
                        fee = configManager.config.swapReferrerAccount?.fee,
                    ),
                )

                AggregatedSwapDataModel(swapConverter.convert(swapResponse))
            } catch (ex: OneIncResponseException) {
                AggregatedSwapDataModel(null, mapErrors(ex.data.description))
            }
        }
    }

    override fun getTangemFee(): Double {
        return configManager.config.swapReferrerAccount?.fee?.toDoubleOrNull() ?: 0.0
    }

    override suspend fun getCryptoCurrency(
        userWallet: UserWallet,
        currency: Currency,
        network: Network,
    ): CryptoCurrency? {
        val blockchain = Blockchain.fromNetworkId(currency.networkId) ?: return null
        val cryptoCurrencyFactory = CryptoCurrencyFactory()
        return when (currency) {
            is Currency.NativeToken -> {
                cryptoCurrencyFactory.createCoin(
                    blockchain = blockchain,
                    extraDerivationPath = network.derivationPath.value,
                    derivationStyleProvider = userWallet.scanResponse.derivationStyleProvider,
                )
            }
            is Currency.NonNativeToken -> {
                val sdkToken = SdkToken(
                    name = currency.name,
                    symbol = currency.symbol,
                    contractAddress = currency.contractAddress,
                    decimals = currency.decimalCount,
                    id = currency.id,
                )
                cryptoCurrencyFactory.createToken(
                    sdkToken = sdkToken,
                    blockchain = blockchain,
                    extraDerivationPath = network.derivationPath.value,
                    derivationStyleProvider = userWallet.scanResponse.derivationStyleProvider,
                )
            }
        } as CryptoCurrency
    }

    override suspend fun getAllowance(
        userWalletId: UserWalletId,
        networkId: String,
        derivationPath: String?,
        tokenDecimalCount: Int,
        tokenAddress: String,
    ): BigDecimal {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = derivationPath,
        )
        val spenderAddress = addressForTrust(networkId)

        val result = (walletManager as? Approver)?.getAllowance(
            spenderAddress,
            Token(
                symbol = blockchain.currency,
                contractAddress = tokenAddress,
                decimals = tokenDecimalCount,
            ),
        ) ?: error("Cannot cast to Approver")

        return when (result) {
            is Result.Success -> result.data
            is Result.Failure -> error(result.error)
        }
    }

    override suspend fun getApproveData(
        userWalletId: UserWalletId,
        networkId: String,
        derivationPath: String?,
        currency: Currency,
        amount: BigDecimal?,
    ): String {
        val blockchain =
            requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = derivationPath,
        )
        val spenderAddress = addressForTrust(networkId)

        return (walletManager as? Approver)?.getApproveData(
            spenderAddress,
            amount?.let { convertToAmount(it, currency, blockchain) },
        ) ?: error("Cannot cast to Approver")
    }

    private fun convertToAmount(amount: BigDecimal, currency: Currency, blockchain: Blockchain): Amount {
        return when (currency) {
            is Currency.NativeToken -> {
                Amount(value = amount, blockchain = blockchain)
            }
            is Currency.NonNativeToken -> {
                Amount(
                    currencySymbol = currency.symbol,
                    value = amount,
                    decimals = currency.decimalCount,
                )
            }
        }
    }

    private fun getOneInchApi(networkId: String): OneInchApi {
        return oneInchApiFactory.getApi(networkId)
    }

    companion object {
        // TODO("get this ids from blockchain enum later")
        private const val OPTIMISM_ID = "optimistic-ethereum"
        private const val ARBITRUM_ID = "arbitrum-one"
        private const val ETHEREUM_ID = "ethereum"
    }
}