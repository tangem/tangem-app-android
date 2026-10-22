package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.data.common.quote.QuotesFetcher
import com.tangem.data.pay.util.TangemPayWithdrawDataVerifier
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.request.WithdrawDataRequest
import com.tangem.spend.datasource.pay.models.request.WithdrawRequest
import com.tangem.spend.datasource.pay.models.response.WithdrawDataResponse
import com.tangem.spend.datasource.pay.models.response.WithdrawResponse
import com.tangem.data.pay.store.TangemPayStorage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.*
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.extensions.addHexPrefix
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val TAG = "TangemPaySwapRepository"
private val POLLING_WINDOW = 5.minutes
private val FAST_POLLING_PHASE = 30.seconds
private val FAST_POLLING_INTERVAL = 3.seconds
private val SLOW_POLLING_INTERVAL = 5.seconds

private data class PollingKey(val userWalletId: String, val orderId: String)

@Suppress("LongParameterList")
internal class DefaultTangemPayWithdrawRepository(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val authDataSource: TangemPayAuthDataSource,
    private val quotesFetcher: QuotesFetcher,
    private val tangemPayStorage: TangemPayStorage,
    private val swapRepository: SwapRepository,
    private val orderRepository: CustomerOrderRepository,
    private val withdrawSourceResolver: WithdrawSourceResolver,
    private val withdrawPollingScope: AppCoroutineScope,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : TangemPayWithdrawRepository {

    private val pollingJobs = mutableMapOf<PollingKey, Job>()
    private val pollingMutex = Mutex()

    override suspend fun withdrawWithSwap(
        userWallet: UserWallet,
        receiverAddress: String,
        cryptoAmount: BigDecimal,
        sourceCurrency: CryptoCurrency,
        exchangeData: TangemPayWithdrawExchangeState,
    ): Either<UniversalError, WithdrawalResult> {
        val amountInCents = getAmountInCents(cryptoAmount, sourceCurrency)
        if (amountInCents.isNullOrEmpty()) return Either.Left(VisaApiError.WithdrawalDataError)
        val source = withdrawSourceResolver.resolve(userWallet.walletId, sourceCurrency)
            ?: return Either.Left(VisaApiError.WithdrawalDataError)
        val dataRequest = WithdrawDataRequest(
            amountInCents = amountInCents,
            recipientAddress = receiverAddress,
            chainId = source.chainId,
            tokenContractAddress = source.tokenContractAddress,
        )
        return requestHelper.performRequest(userWallet.walletId) { authHeader ->
            tangemPayApi.getWithdrawData(authHeader = authHeader, body = dataRequest)
        }.map { data ->
            val result = data.result ?: return VisaApiError.WithdrawalDataError.left()
            result.verifyMatches(request = dataRequest, tokenDecimals = sourceCurrency.decimals)
                .onLeft { return it.left() }
            val signatureResult = authDataSource.getWithdrawalSignature(
                userWallet = userWallet,
                hash = result.hash,
            ).getOrNull()

            return when (signatureResult) {
                is WithdrawalSignatureResult.Cancelled -> {
                    Either.Right(WithdrawalResult.Cancelled)
                }
                is WithdrawalSignatureResult.Success -> {
                    requestHelper.performRequest(userWallet.walletId) { authHeader ->
                        val request = WithdrawRequest(
                            amountInCents = amountInCents,
                            recipientAddress = receiverAddress,
                            adminSalt = result.salt,
                            senderAddress = result.senderAddress,
                            adminSignature = signatureResult.signature.addHexPrefix(),
                            chainId = source.chainId,
                            tokenContractAddress = source.tokenContractAddress,
                        )
                        tangemPayApi.withdraw(authHeader = authHeader, body = request)
                    }
                        .mapLeft { return Either.Left(VisaApiError.WithdrawError) }
                        .map { response ->
                            processWithdrawResult(response, userWallet, exchangeData)
                            WithdrawalResult.Success
                        }
                }
                null -> Either.Left(VisaApiError.SignWithdrawError)
            }
        }
    }

    private suspend fun processWithdrawResult(
        response: WithdrawResponse,
        userWallet: UserWallet,
        exchangeData: TangemPayWithdrawExchangeState,
    ) {
        val orderId = response.result?.orderId
        if (orderId != null) {
            tangemPayStorage.storeActiveWithdrawOrderId(userWalletId = userWallet.walletId, orderId = orderId)
            val order = orderRepository.getOrderData(userWalletId = userWallet.walletId, orderId = orderId).getOrNull()
            val withdrawTxHash = order?.withdrawTxHash
            if (order != null && !withdrawTxHash.isNullOrEmpty()) {
                finalizeWithdraw(
                    userWallet = userWallet,
                    txHash = withdrawTxHash,
                    exchangeData = exchangeData,
                    orderId = orderId,
                )
            } else {
                val createdAtMillis = currentTimeMillis()
                val storeData = TangemPayWithdrawState(
                    orderId = orderId,
                    exchangeData = exchangeData,
                    createdAtMillis = createdAtMillis,
                )
                tangemPayStorage.storeWithdrawOrder(userWalletId = userWallet.walletId, data = storeData)
                startPollingForOrder(
                    userWallet = userWallet,
                    orderId = orderId,
                    exchangeData = exchangeData,
                    createdAtMillis = createdAtMillis,
                )
            }
        }
    }

    private fun startPollingForOrder(
        userWallet: UserWallet,
        orderId: String,
        exchangeData: TangemPayWithdrawExchangeState,
        createdAtMillis: Long,
    ) {
        val key = PollingKey(userWallet.walletId.stringValue, orderId)
        val deadlineMillis = createdAtMillis + POLLING_WINDOW.inWholeMilliseconds

        withdrawPollingScope.launch {
            pollingMutex.withLock {
                if (pollingJobs.containsKey(key)) return@withLock

                pollingJobs[key] = withdrawPollingScope.launch {
                    try {
                        var lastOrderResult: Either<VisaApiError, OrderData>
                        do {
                            delay(getPollingDelay(createdAtMillis, deadlineMillis, currentTimeMillis()))
                            lastOrderResult = orderRepository.getOrderData(
                                userWalletId = userWallet.walletId,
                                orderId = orderId,
                            )
                            val txHash = lastOrderResult.getOrNull()?.withdrawTxHash
                            if (!txHash.isNullOrEmpty()) {
                                finalizeWithdraw(
                                    userWallet = userWallet,
                                    txHash = txHash,
                                    exchangeData = exchangeData,
                                    orderId = orderId,
                                )
                                pollingMutex.withLock { pollingJobs.remove(key) }
                                return@launch
                            }
                        } while (currentTimeMillis() < deadlineMillis)

                        TangemLogger.withTag(TAG).e(
                            "Polling stopped: no tx hash within $POLLING_WINDOW, " +
                                "last order request: ${lastOrderResult.map(OrderData::status)}",
                        )
                        tangemPayStorage.deleteWithdrawOrder(userWalletId = userWallet.walletId, orderId = orderId)
                        pollingMutex.withLock { pollingJobs.remove(key) }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        TangemLogger.withTag(TAG).e("Polling failed, the order is kept for the next attempt", exception)
                        pollingMutex.withLock { pollingJobs.remove(key) }
                    }
                }
            }
        }
    }

    private fun stopPolling(userWalletId: String, orderId: String) {
        withdrawPollingScope.launch {
            pollingMutex.withLock {
                val key = PollingKey(userWalletId, orderId)
                pollingJobs[key]?.cancel()
                pollingJobs.remove(key)
            }
        }
    }

    private suspend fun finalizeWithdraw(
        userWallet: UserWallet,
        orderId: String,
        txHash: String,
        exchangeData: TangemPayWithdrawExchangeState,
    ): Either<ExpressDataError, Unit> {
        return swapRepository.exchangeSent(
            userWallet = userWallet,
            txId = exchangeData.txId,
            fromNetwork = exchangeData.fromNetwork,
            fromAddress = exchangeData.fromAddress,
            payInAddress = exchangeData.payInAddress,
            txHash = txHash,
            payInExtraId = exchangeData.payInExtraId,
        ).also {
            tangemPayStorage.deleteWithdrawOrder(userWalletId = userWallet.walletId, orderId = orderId)
            stopPolling(userWalletId = userWallet.walletId.stringValue, orderId = orderId)
        }
    }

    override suspend fun withdraw(
        userWallet: UserWallet,
        receiverAddress: String,
        cryptoAmount: BigDecimal,
        sourceCurrency: CryptoCurrency,
    ): Either<UniversalError, WithdrawalResult> {
        val amountInCents = getAmountInCents(cryptoAmount, sourceCurrency)
        if (amountInCents.isNullOrEmpty()) return VisaApiError.WithdrawalDataError.left()
        val source = withdrawSourceResolver.resolve(userWallet.walletId, sourceCurrency)
            ?: return VisaApiError.WithdrawalDataError.left()

        val dataRequest = WithdrawDataRequest(
            amountInCents = amountInCents,
            recipientAddress = receiverAddress,
            chainId = source.chainId,
            tokenContractAddress = source.tokenContractAddress,
        )
        return requestHelper.performRequest(userWallet.walletId) { authHeader ->
            tangemPayApi.getWithdrawData(authHeader = authHeader, body = dataRequest)
        }.map { data ->
            val result = data.result ?: return VisaApiError.WithdrawalDataError.left()
            result.verifyMatches(request = dataRequest, tokenDecimals = sourceCurrency.decimals)
                .onLeft { return it.left() }
            val signatureResult = authDataSource.getWithdrawalSignature(
                userWallet = userWallet,
                hash = result.hash,
            ).getOrNull()
            return when (signatureResult) {
                is WithdrawalSignatureResult.Cancelled -> WithdrawalResult.Cancelled.right()
                is WithdrawalSignatureResult.Success -> requestHelper.performRequest(
                    userWalletId = userWallet.walletId,
                ) { authHeader ->
                    val request = WithdrawRequest(
                        amountInCents = amountInCents,
                        recipientAddress = receiverAddress,
                        adminSalt = result.salt,
                        senderAddress = result.senderAddress,
                        adminSignature = signatureResult.signature.addHexPrefix(),
                        chainId = source.chainId,
                        tokenContractAddress = source.tokenContractAddress,
                    )
                    tangemPayApi.withdraw(authHeader = authHeader, body = request)
                }.fold(
                    ifLeft = { VisaApiError.WithdrawError.left() },
                    ifRight = { WithdrawalResult.Success.right() },
                )
                null -> VisaApiError.SignWithdrawError.left()
            }
        }
    }

    override suspend fun hasWithdrawOrder(userWalletId: UserWalletId): Boolean {
        val orderId = tangemPayStorage.getActiveWithdrawOrderId(userWalletId)
        if (orderId.isNullOrEmpty()) return false
        val orderData = orderRepository.getOrderData(userWalletId = userWalletId, orderId = orderId).getOrNull()
        val isActive = orderData?.status == OrderStatus.NEW || orderData?.status == OrderStatus.PROCESSING
        if (!isActive) {
            tangemPayStorage.deleteActiveWithdrawOrder(userWalletId = userWalletId)
        }
        return isActive
    }

    override suspend fun pollWithdrawOrdersIfNeeds(userWallet: UserWallet) {
        tangemPayStorage.getWithdrawOrders(userWalletId = userWallet.walletId)?.forEach { state ->
            try {
                pollWithdrawOrderIfNeeds(userWallet = userWallet, data = state)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                TangemLogger.withTag(TAG).e("Error", exception)
            }
        }
    }

    private suspend fun pollWithdrawOrderIfNeeds(userWallet: UserWallet, data: TangemPayWithdrawState) {
        val exchangeData = data.exchangeData ?: return
        val storedHash = data.txHash
        if (!storedHash.isNullOrEmpty()) {
            finalizeWithdraw(
                userWallet = userWallet,
                txHash = storedHash,
                exchangeData = exchangeData,
                orderId = data.orderId,
            )
        } else {
            startPollingForOrder(
                userWallet = userWallet,
                orderId = data.orderId,
                exchangeData = exchangeData,
                createdAtMillis = data.createdAtMillis,
            )
        }
    }

    private suspend fun getAmountInCents(cryptoAmount: BigDecimal, sourceCurrency: CryptoCurrency): String? {
        val cryptoCurrencyId = sourceCurrency.id.rawCurrencyId ?: return null
        val fiatRate = getFiatRate(cryptoCurrencyId) ?: return null
        val amountInDollars = cryptoAmount.multiply(fiatRate)
        val defaultFractionDigits = Currency.getInstance(Locale.US).defaultFractionDigits
        return amountInDollars
            .setScale(defaultFractionDigits, RoundingMode.HALF_UP)
            .movePointRight(defaultFractionDigits)
            .longValueExact()
            .toString()
    }

    private suspend fun getFiatRate(cryptoCurrencyId: CryptoCurrency.RawID): BigDecimal? {
        val quotes = quotesFetcher.fetch(
            fiatCurrencyId = Currency.getInstance(Locale.US).currencyCode,
            currencyId = cryptoCurrencyId.value,
            field = QuotesFetcher.Field.PRICE,
        ).getOrNull()

        return quotes?.quotes[cryptoCurrencyId.value]?.price
    }
}

private fun getPollingDelay(createdAtMillis: Long, deadlineMillis: Long, nowMillis: Long): Duration {
    val isFastPhase = nowMillis - createdAtMillis < FAST_POLLING_PHASE.inWholeMilliseconds
    val interval = if (isFastPhase) FAST_POLLING_INTERVAL else SLOW_POLLING_INTERVAL
    return interval.coerceAtMost((deadlineMillis - nowMillis).coerceAtLeast(0L).milliseconds)
}

private fun WithdrawDataResponse.Result.verifyMatches(
    request: WithdrawDataRequest,
    tokenDecimals: Int,
): Either<VisaApiError, Unit> = TangemPayWithdrawDataVerifier.verify(
    hash = hash,
    structuredData = structuredData,
    expectation = TangemPayWithdrawDataVerifier.Expectation(
        amountInCents = request.amountInCents.toBigDecimal(),
        tokenDecimals = tokenDecimals,
        recipientAddress = request.recipientAddress,
        chainId = request.chainId,
        tokenContractAddress = request.tokenContractAddress,
    ),
).mapLeft { mismatch ->
    TangemLogger.withTag(TAG).e("Withdraw data does not match the requested withdrawal: $mismatch")
    VisaApiError.WithdrawalDataError
}