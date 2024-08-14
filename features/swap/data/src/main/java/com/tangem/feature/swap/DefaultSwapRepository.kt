package com.tangem.feature.swap

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.right
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.*
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.request.ExchangeSentRequestBody
import com.tangem.datasource.api.express.models.request.PairsRequestBody
import com.tangem.datasource.api.express.models.response.ExchangeDataResponseWithTxDetails
import com.tangem.datasource.api.express.models.response.SwapPair
import com.tangem.datasource.api.express.models.response.SwapPairsWithProviders
import com.tangem.datasource.api.express.models.response.TxDetails
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.crypto.DataSignatureVerifier
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.converters.*
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.ExpressException
import com.tangem.feature.swap.domain.models.createFromAmountWithOffset
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import com.tangem.datasource.api.express.models.request.LeastTokenInfo as NetworkLeastTokenInfo

@Suppress("LongParameterList", "LargeClass")
internal class DefaultSwapRepository @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val tangemExpressApi: TangemExpressApi,
    private val coroutineDispatcher: CoroutineDispatcherProvider,
    private val walletManagersFacade: WalletManagersFacade,
    private val userWalletsListManager: UserWalletsListManager,
    private val errorsDataConverter: ErrorsDataConverter,
    private val dataSignatureVerifier: DataSignatureVerifier,
    moshi: Moshi,
) : SwapRepository {

    private val expressDataConverter = ExpressDataConverter()
    private val leastTokenInfoConverter = LeastTokenInfoConverter()
    private val swapPairInfoConverter = SwapPairInfoConverter()
    private val cryptoCurrencyFactory = CryptoCurrencyFactory()
    private val exchangeStatusConverter = ExchangeStatusConverter()
    private val txDetailsMoshiAdapter = moshi.adapter(TxDetails::class.java)

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

    override suspend fun getExchangeData(
        fromContractAddress: String,
        fromNetwork: String,
        toContractAddress: String,
        fromAddress: String,
        toNetwork: String,
        fromAmount: String,
        fromDecimals: Int,
        toDecimals: Int,
        providerId: String,
        rateType: RateType,
        toAddress: String,
        refundAddress: String?, // for cex only
        refundExtraId: String?, // for cex only
    ): Either<DataError, SwapDataModel> {
        return withContext(coroutineDispatcher.io) {
            try {
                val requestId = UUID.randomUUID().toString()
                val response = tangemExpressApi.getExchangeData(
                    fromContractAddress = fromContractAddress,
                    fromNetwork = fromNetwork,
                    toContractAddress = toContractAddress,
                    fromAddress = fromAddress,
                    toNetwork = toNetwork,
                    fromAmount = fromAmount,
                    fromDecimals = fromDecimals,
                    toDecimals = toDecimals,
                    providerId = providerId,
                    rateType = rateType.name.lowercase(),
                    toAddress = toAddress,
                    requestId = requestId,
                    refundAddress = refundAddress,
                    refundExtraId = refundExtraId,
                ).getOrThrow()
                if (dataSignatureVerifier.verifySignature(response.signature, response.txDetailsJson)) {
                    val txDetails = parseTxDetails(response.txDetailsJson)
                        ?: return@withContext DataError.UnknownError.left()
                    if (txDetails.requestId != requestId) {
                        return@withContext DataError.InvalidRequestIdError().left()
                    }
                    if (!toAddress.equals(txDetails.payoutAddress, ignoreCase = true)) {
                        return@withContext DataError.InvalidPayoutAddressError().left()
                    }
                    expressDataConverter.convert(
                        ExchangeDataResponseWithTxDetails(
                            dataResponse = response,
                            txDetails = txDetails,
                        ),
                    ).right()
                } else {
                    DataError.InvalidSignatureError().left()
                }
            } catch (ex: Exception) {
                getDataError(ex).left()
            }
        }
    }

    override suspend fun exchangeSent(
        txId: String,
        fromNetwork: String,
        fromAddress: String,
        payInAddress: String,
        txHash: String,
        payInExtraId: String?,
    ): Either<DataError, Unit> = withContext(coroutineDispatcher.io) {
        try {
            tangemExpressApi.exchangeSent(
                ExchangeSentRequestBody(
                    txId = txId,
                    fromNetwork = fromNetwork,
                    fromAddress = fromAddress,
                    payinAddress = payInAddress,
                    payinExtraId = payInExtraId,
                    txHash = txHash,
                ),
            ).getOrThrow()
            Unit.right()
        } catch (ex: Exception) {
            getDataError(ex).left()
        }
    }

    private fun parseTxDetails(txDetailsJson: String): TxDetails? {
        return try {
            txDetailsMoshiAdapter.fromJson(txDetailsJson)
        } catch (e: IOException) {
            Timber.e(e, "error parsing txDetailsJson")
            null
        }
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

        return result.fold(
            onSuccess = { it },
            onFailure = { error(it) },
        )
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
            amount?.let { convertToAmount(it, currency) },
        ) ?: error("Cannot cast to Approver")
    }

    private fun convertToAmount(amount: BigDecimal, currency: CryptoCurrency): Amount {
        return Amount(
            currencySymbol = currency.symbol,
            value = amount,
            decimals = currency.decimals,
            type = if (currency is CryptoCurrency.Token) {
                AmountType.Token(
                    Token(
                        symbol = currency.symbol,
                        contractAddress = currency.contractAddress,
                        decimals = currency.decimals,
                    ),
                )
            } else {
                AmountType.Coin
            },
        )
    }

    override fun getNativeTokenForNetwork(networkId: String): CryptoCurrency {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }

        return requireNotNull(
            cryptoCurrencyFactory.createCoin(
                blockchain = blockchain,
                extraDerivationPath = null,
                derivationStyleProvider = requireNotNull(
                    userWalletsListManager
                        .selectedUserWalletSync
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
// [REDACTED_TODO_COMMENT]
        private const val OPTIMISM_ID = "optimistic-ethereum"
        private const val ARBITRUM_ID = "arbitrum-one"
        private const val ETHEREUM_ID = "ethereum"
    }
}
