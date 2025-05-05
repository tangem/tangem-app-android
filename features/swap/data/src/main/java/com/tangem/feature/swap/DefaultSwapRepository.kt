package com.tangem.feature.swap

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.right
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.*
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.CryptoCurrencyFactory
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
import com.tangem.datasource.crypto.DataSignatureVerifier
import com.tangem.datasource.exchangeservice.swap.ExpressUtils
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.converters.*
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.ExpressDataError
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
import com.tangem.datasource.api.express.models.request.LeastTokenInfo as NetworkLeastTokenInfo

@Suppress("LongParameterList", "LargeClass")
internal class DefaultSwapRepository(
    private val tangemExpressApi: TangemExpressApi,
    private val coroutineDispatcher: CoroutineDispatcherProvider,
    private val walletManagersFacade: WalletManagersFacade,
    private val userWalletsListManager: UserWalletsListManager,
    private val errorsDataConverter: ErrorsDataConverter,
    private val dataSignatureVerifier: DataSignatureVerifier,
    private val appPreferencesStore: AppPreferencesStore,
    moshi: Moshi,
    excludedBlockchains: ExcludedBlockchains,
) : SwapRepository {

    private val expressDataConverter = ExpressDataConverter()
    private val leastTokenInfoConverter = LeastTokenInfoConverter()
    private val swapPairInfoConverter = SwapPairInfoConverter()
    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)
    private val exchangeStatusConverter = ExchangeStatusConverter()
    private val txDetailsMoshiAdapter = moshi.adapter(TxDetails::class.java)

    override suspend fun getPairs(
        userWallet: UserWallet,
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
                        userWallet = userWallet,
                        from = arrayListOf(initial),
                        to = currenciesList,
                    )
                }

                val reversedPairsDeferred = async {
                    getPairsInternal(
                        userWallet = userWallet,
                        from = currenciesList,
                        to = arrayListOf(initial),
                    )
                }

                val pairs = pairsDeferred.await().getOrThrow()
                val reversedPairs = reversedPairsDeferred.await().getOrThrow()

                val allPairs = pairs + reversedPairs

                val providers = tangemExpressApi.getProviders(
                    userWalletId = userWallet.walletId.stringValue,
                    refCode = ExpressUtils.getRefCode(
                        userWallet = userWallet,
                        appPreferencesStore = appPreferencesStore,
                    ),
                ).getOrThrow()

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
        userWallet: UserWallet,
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
                        userWallet = userWallet,
                        from = arrayListOf(initial),
                        to = currenciesList,
                    )
                }

                val reversedPairsDeferred = async {
                    getPairsInternal(
                        userWallet = userWallet,
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
        userWallet: UserWallet,
        from: List<NetworkLeastTokenInfo>,
        to: List<NetworkLeastTokenInfo>,
    ): ApiResponse<List<SwapPair>> {
        return tangemExpressApi.getPairs(
            userWalletId = userWallet.walletId.stringValue,
            refCode = ExpressUtils.getRefCode(
                userWallet = userWallet,
                appPreferencesStore = appPreferencesStore,
            ),
            body = PairsRequestBody(
                from = from,
                to = to,
            ),
        )
    }

    override suspend fun getExchangeStatus(
        userWallet: UserWallet,
        txId: String,
    ): Either<UnknownError,
        ExchangeStatusModel,> {
        return withContext(coroutineDispatcher.io) {
            either {
                catch(
                    block = {
                        exchangeStatusConverter.convert(
                            tangemExpressApi
                                .getExchangeStatus(
                                    userWalletId = userWallet.walletId.stringValue,
                                    refCode = ExpressUtils.getRefCode(
                                        userWallet = userWallet,
                                        appPreferencesStore = appPreferencesStore,
                                    ),
                                    txId = txId,
                                )
                                .getOrThrow(),
                        )
                    },
                    catch = {
                        Timber.e("getExchangeStatus error: $it")
                        raise(UnknownError(it.message))
                    },
                )
            }
        }
    }

    override suspend fun findBestQuote(
        userWallet: UserWallet,
        fromContractAddress: String,
        fromNetwork: String,
        toContractAddress: String,
        toNetwork: String,
        fromAmount: String,
        fromDecimals: Int,
        toDecimals: Int,
        providerId: String,
        rateType: RateType,
    ): Either<ExpressDataError, QuoteModel> {
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
                    userWalletId = userWallet.walletId.stringValue,
                    refCode = ExpressUtils.getRefCode(
                        userWallet = userWallet,
                        appPreferencesStore = appPreferencesStore,
                    ),
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
        userWallet: UserWallet,
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
    ): Either<ExpressDataError, SwapDataModel> {
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
                    userWalletId = userWallet.walletId.stringValue,
                    refCode = ExpressUtils.getRefCode(
                        userWallet = userWallet,
                        appPreferencesStore = appPreferencesStore,
                    ),
                ).getOrThrow()
                if (dataSignatureVerifier.verifySignature(response.signature, response.txDetailsJson)) {
                    val txDetails = parseTxDetails(response.txDetailsJson)
                        ?: return@withContext ExpressDataError.UnknownError.left()
                    if (txDetails.requestId != requestId) {
                        return@withContext ExpressDataError.InvalidRequestIdError().left()
                    }
                    if (!toAddress.equals(txDetails.payoutAddress, ignoreCase = true)) {
                        return@withContext ExpressDataError.InvalidPayoutAddressError().left()
                    }
                    expressDataConverter.convert(
                        ExchangeDataResponseWithTxDetails(
                            dataResponse = response,
                            txDetails = txDetails,
                        ),
                    ).right()
                } else {
                    ExpressDataError.InvalidSignatureError().left()
                }
            } catch (ex: Exception) {
                getDataError(ex).left()
            }
        }
    }

    override suspend fun exchangeSent(
        userWallet: UserWallet,
        txId: String,
        fromNetwork: String,
        fromAddress: String,
        payInAddress: String,
        txHash: String,
        payInExtraId: String?,
    ): Either<ExpressDataError, Unit> = withContext(coroutineDispatcher.io) {
        try {
            tangemExpressApi.exchangeSent(
                userWalletId = userWallet.walletId.stringValue,
                refCode = ExpressUtils.getRefCode(
                    userWallet = userWallet,
                    appPreferencesStore = appPreferencesStore,
                ),
                body = ExchangeSentRequestBody(
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

    override fun getNativeTokenForNetwork(networkId: String): CryptoCurrency {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }

        return requireNotNull(
            cryptoCurrencyFactory.createCoin(
                blockchain = blockchain,
                extraDerivationPath = null,
                scanResponse = requireNotNull(
                    userWalletsListManager
                        .selectedUserWalletSync
                        ?.scanResponse,
                ),
            ),
        )
    }

    private fun getDataError(ex: Exception): ExpressDataError {
        return if (ex is ApiResponseError.HttpException) {
            errorsDataConverter.convert(ex.errorBody ?: "")
        } else {
            ExpressDataError.UnknownError
        }
    }
}