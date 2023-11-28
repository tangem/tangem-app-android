package com.tangem.feature.swap

import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Approver
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.request.PairsRequestBody
import com.tangem.datasource.api.express.models.response.SwapPair
import com.tangem.datasource.api.express.models.response.SwapPairsWithProviders
import com.tangem.datasource.api.oneinch.OneInchApi
import com.tangem.datasource.api.oneinch.OneInchApiFactory
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.config.ConfigManager
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.converters.*
import com.tangem.feature.swap.domain.SwapRepository
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.createFromAmountWithOffset
import com.tangem.feature.swap.domain.models.data.AggregatedSwapDataModel
import com.tangem.feature.swap.domain.models.domain.*
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
    private val coroutineDispatcher: CoroutineDispatcherProvider,
    private val configManager: ConfigManager,
    private val walletManagersFacade: WalletManagersFacade,
    private val walletsStateHolder: WalletsStateHolder,
    private val errorsDataConverter: ErrorsDataConverter,
) : SwapRepository {

    private val tokensConverter = TokensConverter()
    private val expressDataConverter = ExpressDataConverter()
    private val leastTokenInfoConverter = LeastTokenInfoConverter()
    private val swapPairInfoConverter = SwapPairInfoConverter()
    private val cryptoCurrencyFactory = CryptoCurrencyFactory()

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

            val allPairs = pairs.await() + reversedPairs.await()

            val providers = tangemExpressApi.getProviders().getOrThrow()

            return@withContext swapPairInfoConverter.convert(
                SwapPairsWithProviders(
                    swapPair = allPairs,
                    providers = providers,
                ),
            )
        }
    }

    private suspend fun getPairsInternal(
        from: List<NetworkLeastTokenInfo>,
        to: List<NetworkLeastTokenInfo>,
    ): List<SwapPair> {
        return tangemExpressApi.getPairs(
            PairsRequestBody(
                from = from,
                to = to,
            ),
        ).getOrThrow()
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
        fromContractAddress: String,
        fromNetwork: String,
        toContractAddress: String,
        toNetwork: String,
        fromAmount: String,
        fromDecimals: Int,
        providerId: String,
        rateType: RateType,
    ): AggregatedSwapDataModel<QuoteModel> {
        return withContext(coroutineDispatcher.io) {
            try {
                val response = tangemExpressApi.getExchangeQuote(
                    fromContractAddress = fromContractAddress,
                    fromNetwork = fromNetwork,
                    toContractAddress = toContractAddress,
                    toNetwork = toNetwork,
                    fromAmount = fromAmount,
                    fromDecimals = fromDecimals,
                    providerId = providerId,
                    rateType = rateType.name.lowercase(),
                ).getOrThrow()
                AggregatedSwapDataModel(
                    dataModel = QuoteModel(
                        toTokenAmount = createFromAmountWithOffset(response.toAmount, response.toDecimals),
                        allowanceContract = response.allowanceContract,
                    ),
                )
            } catch (ex: Exception) {
                AggregatedSwapDataModel(null, getDataError(ex))
            }
        }
    }

    override suspend fun addressForTrust(networkId: String): String {
        return withContext(coroutineDispatcher.io) {
            getOneInchApi(networkId).approveSpender().address
        }
    }

    override suspend fun getExchangeData(
        fromContractAddress: String,
        fromNetwork: String,
        toContractAddress: String,
        toNetwork: String,
        fromAmount: String,
        fromDecimals: Int,
        providerId: String,
        rateType: RateType,
        toAddress: String,
    ): AggregatedSwapDataModel<SwapDataModel> {
        return withContext(coroutineDispatcher.io) {
            try {
                val response = tangemExpressApi.getExchangeData(
                    fromContractAddress = fromContractAddress,
                    fromNetwork = fromNetwork,
                    toContractAddress = toContractAddress,
                    toNetwork = toNetwork,
                    fromAmount = fromAmount,
                    fromDecimals = fromDecimals,
                    providerId = providerId,
                    rateType = rateType.name.lowercase(),
                    toAddress = toAddress,
                ).getOrThrow()
                AggregatedSwapDataModel(
                    dataModel = expressDataConverter.convert(response),
                )
            } catch (ex: Exception) {
                AggregatedSwapDataModel(null, getDataError(ex))
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
        spenderAddress: String,
    ): BigDecimal {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = derivationPath,
        )

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
        spenderAddress: String,
    ): String {
        val blockchain =
            requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = derivationPath,
        )

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

    override fun getNativeTokenForNetwork(networkId: String): CryptoCurrency {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }

        return requireNotNull(
            cryptoCurrencyFactory.createCoin(
                blockchain = blockchain,
                extraDerivationPath = null,
                derivationStyleProvider = requireNotNull(
                    walletsStateHolder.userWalletsListManager
                        ?.selectedUserWalletSync
                        ?.scanResponse
                        ?.derivationStyleProvider,
                ),
            ),
        )
    }

    private fun getDataError(ex: Exception) : DataError {
        return if (ex is ApiResponseError.HttpException) {
            errorsDataConverter.convert(ex.errorBody ?: "")
        } else {
            DataError.UnknownError()
        }
    }

    companion object {
// [REDACTED_TODO_COMMENT]
        private const val OPTIMISM_ID = "optimistic-ethereum"
        private const val ARBITRUM_ID = "arbitrum-one"
        private const val ETHEREUM_ID = "ethereum"
    }
}
