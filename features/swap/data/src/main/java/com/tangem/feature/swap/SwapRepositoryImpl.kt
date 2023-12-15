package com.tangem.feature.swap

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.right
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Approver
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.datasource.api.common.response.ApiResponse
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
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.ExpressException
import com.tangem.feature.swap.domain.models.createFromAmountWithOffset
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import com.tangem.datasource.api.express.models.request.LeastTokenInfo as NetworkLeastTokenInfo

@Suppress("LongParameterList", "LargeClass")
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
    private val exchangeStatusConverter = ExchangeStatusConverter()

    override suspend fun getPairs(
        initialCurrency: LeastTokenInfo,
        currencyList: List<CryptoCurrency>,
    ): PairsWithProviders {
        return withContext(coroutineDispatcher.io) {
            try {
                val initial = NetworkLeastTokenInfo(
                    contractAddress = initialCurrency.contractAddress,
                    network = initialCurrency.network,
                )
                val currenciesList = currencyList.map { leastTokenInfoConverter.convert(it) }

                val pairsDeferred = async {
                    getPairsInternal(
                        from = arrayListOf(initial),
                        to = currenciesList,
                    )
                }

                val reversedPairsDeferred = async {
                    getPairsInternal(
                        from = currenciesList,
                        to = arrayListOf(initial),
                    )
                }

                val pairs = pairsDeferred.await().getOrThrow()
                val reversedPairs = reversedPairsDeferred.await().getOrThrow()

                val allPairs = pairs + reversedPairs

                val providers = tangemExpressApi.getProviders().getOrThrow()

                return@withContext swapPairInfoConverter.convert(
                    SwapPairsWithProviders(
                        swapPair = allPairs,
                        providers = providers,
                    ),
                )
            } catch (exception: Exception) {
                if (exception is ApiResponseError.HttpException) {
                    throw ExpressException(errorsDataConverter.convert(exception.errorBody ?: ""))
                } else {
                    throw exception
                }
            }
        }
    }

    override suspend fun getPairsOnly(
        initialCurrency: LeastTokenInfo,
        currencyList: List<CryptoCurrency>,
    ): PairsWithProviders {
        return withContext(coroutineDispatcher.io) {
            try {
                val initial = NetworkLeastTokenInfo(
                    contractAddress = initialCurrency.contractAddress,
                    network = initialCurrency.network,
                )
                val currenciesList = currencyList.map { leastTokenInfoConverter.convert(it) }

                val pairsDeferred = async {
                    getPairsInternal(
                        from = arrayListOf(initial),
                        to = currenciesList,
                    )
                }

                val reversedPairsDeferred = async {
                    getPairsInternal(
                        from = currenciesList,
                        to = arrayListOf(initial),
                    )
                }

                val pairs = pairsDeferred.await().getOrThrow()
                val reversedPairs = reversedPairsDeferred.await().getOrThrow()

                val allPairs = pairs + reversedPairs

                return@withContext swapPairInfoConverter.convert(
                    SwapPairsWithProviders(
                        swapPair = allPairs,
                        providers = emptyList(),
                    ),
                )
            } catch (exception: Exception) {
                if (exception is ApiResponseError.HttpException) {
                    throw ExpressException(errorsDataConverter.convert(exception.errorBody ?: ""))
                } else {
                    throw exception
                }
            }
        }
    }

    private suspend fun getPairsInternal(
        from: List<NetworkLeastTokenInfo>,
        to: List<NetworkLeastTokenInfo>,
    ): ApiResponse<List<SwapPair>> {
        return tangemExpressApi.getPairs(
            PairsRequestBody(
                from = from,
                to = to,
            ),
        )
    }

    override suspend fun getExchangeStatus(txId: String): Either<UnknownError, ExchangeStatusModel> {
        return withContext(coroutineDispatcher.io) {
            either {
                catch(
                    {
                        exchangeStatusConverter.convert(
                            tangemExpressApi
                                .getExchangeStatus(txId)
                                .getOrThrow(),
                        )
                    },
                    {
                        raise(UnknownError(it.message))
                    },
                )
            }
        }
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
        toDecimals: Int,
        providerId: String,
        rateType: RateType,
    ): Either<DataError, QuoteModel> {
        return withContext(coroutineDispatcher.io) {
            try {
                val response = tangemExpressApi.getExchangeQuote(
                    fromContractAddress = fromContractAddress,
                    fromNetwork = fromNetwork,
                    toContractAddress = toContractAddress,
                    toNetwork = toNetwork,
                    fromAmount = fromAmount,
                    fromDecimals = fromDecimals,
                    toDecimals = toDecimals,
                    providerId = providerId,
                    rateType = rateType.name.lowercase(),
                ).getOrThrow()
                QuoteModel(
                    toTokenAmount = createFromAmountWithOffset(response.toAmount, response.toDecimals),
                    allowanceContract = response.allowanceContract,
                ).right()
            } catch (ex: Exception) {
                getDataError(ex).left()
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
        toDecimals: Int,
        providerId: String,
        rateType: RateType,
        toAddress: String,
    ): Either<DataError, SwapDataModel> {
        return withContext(coroutineDispatcher.io) {
            try {
                val response = tangemExpressApi.getExchangeData(
                    fromContractAddress = fromContractAddress,
                    fromNetwork = fromNetwork,
                    toContractAddress = toContractAddress,
                    toNetwork = toNetwork,
                    fromAmount = fromAmount,
                    fromDecimals = fromDecimals,
                    toDecimals = toDecimals,
                    providerId = providerId,
                    rateType = rateType.name.lowercase(),
                    toAddress = toAddress,
                ).getOrThrow()
                expressDataConverter.convert(response).right()
            } catch (ex: Exception) {
                getDataError(ex).left()
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

    private fun getDataError(ex: Exception): DataError {
        return if (ex is ApiResponseError.HttpException) {
            errorsDataConverter.convert(ex.errorBody ?: "")
        } else {
            DataError.UnknownError
        }
    }

    companion object {
        // TODO("get this ids from blockchain enum later")
        private const val OPTIMISM_ID = "optimistic-ethereum"
        private const val ARBITRUM_ID = "arbitrum-one"
        private const val ETHEREUM_ID = "ethereum"
    }
}