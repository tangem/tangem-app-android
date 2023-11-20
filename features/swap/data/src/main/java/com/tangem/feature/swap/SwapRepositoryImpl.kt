package com.tangem.feature.swap

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Approver
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.request.PairsRequestBody
import com.tangem.datasource.api.oneinch.OneInchApi
import com.tangem.datasource.api.oneinch.OneInchApiFactory
import com.tangem.datasource.api.oneinch.OneInchErrorsHandler
import com.tangem.datasource.api.oneinch.errors.OneIncResponseException
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.config.ConfigManager
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.converters.*
import com.tangem.feature.swap.domain.SwapRepository
import com.tangem.feature.swap.domain.models.data.AggregatedSwapDataModel
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.mapErrors
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import com.tangem.datasource.api.express.models.request.LeastTokenInfo as NetworkLeastTokenInfo

@Suppress("LongParameterList")
internal class SwapRepositoryImpl @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val tangemExpressApi: TangemExpressApi,
    private val oneInchApiFactory: OneInchApiFactory,
    private val oneInchErrorsHandler: OneInchErrorsHandler,
    private val coroutineDispatcher: CoroutineDispatcherProvider,
    private val configManager: ConfigManager,
    private val walletManagersFacade: WalletManagersFacade,
) : SwapRepository {

    private val tokensConverter = TokensConverter()
    private val quotesConverter = QuotesConverter()
    private val swapConverter = SwapConverter()
    private val leastTokenInfoConverter = LeastTokenInfoConverter()
    private val swapPairInfoConverter = SwapPairInfoConverter()
    private val rateTypeConverter = RateTypeConverter()

    override suspend fun getPairs(
        initialCurrency: LeastTokenInfo,
        currencyList: List<CryptoCurrency>,
    ): List<SwapPairLeast> {
        return withContext(coroutineDispatcher.io) {
            val initial = NetworkLeastTokenInfo(
                contractAddress = initialCurrency.contractAddress,
                network = initialCurrency.network,
            )
            val currenciesList = currencyList.map { leastTokenInfoConverter.convert(it) }

            val pairs = async {
                getPairsInternal(
                    from = arrayListOf(initial),
                    to = currenciesList,
                )
            }

            val reversedPairs = async {
                getPairsInternal(
                    from = currenciesList,
                    to = arrayListOf(initial),
                )
            }

            pairs.await() + reversedPairs.await()
        }
    }

    private suspend fun getPairsInternal(
        from: List<NetworkLeastTokenInfo>,
        to: List<NetworkLeastTokenInfo>,
    ): List<SwapPairLeast> {
        return tangemExpressApi.getPairs(
            PairsRequestBody(
                from = from,
                to = to,
            ),
        )
            .getOrThrow()
            .map { swapPairInfoConverter.convert(it) }
    }

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
        currency: CryptoCurrency,
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

    private fun convertToAmount(amount: BigDecimal, currency: CryptoCurrency, blockchain: Blockchain): Amount {
        return when (currency) {
            is CryptoCurrency.Token -> {
                Amount(value = amount, blockchain = blockchain)
            }
            is CryptoCurrency.Coin -> {
                Amount(
                    currencySymbol = currency.symbol,
                    value = amount,
                    decimals = currency.decimals,
                )
            }
        }
    }

    private fun getOneInchApi(networkId: String): OneInchApi {
        return oneInchApiFactory.getApi(networkId)
    }

    override suspend fun getExchangeQuote(
        fromContractAddress: String,
        fromNetwork: String,
        toContractAddress: String,
        toNetwork: String,
        fromAmount: BigDecimal,
        providerId: Int,
        rateType: RateType
    ): ExchangeQuote {
        val response = tangemExpressApi.getExchangeQuote(
            fromContractAddress,
            fromNetwork,
            toContractAddress,
            toNetwork,
            fromAmount,
            providerId,
            rateTypeConverter.convertBack(rateType)
        ).getOrThrow()

        return ExchangeQuote(
            toAmount = response.toAmount,
            allowanceContract = response.allowanceContract
        )
    }

    companion object {
        // TODO("get this ids from blockchain enum later")
        private const val OPTIMISM_ID = "optimistic-ethereum"
        private const val ARBITRUM_ID = "arbitrum-one"
        private const val ETHEREUM_ID = "ethereum"
    }
}