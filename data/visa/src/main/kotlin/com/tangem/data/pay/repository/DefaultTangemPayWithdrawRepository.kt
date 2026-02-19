package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.data.common.quote.QuotesFetcher
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.WithdrawDataRequest
import com.tangem.datasource.api.pay.models.request.WithdrawRequest
import com.tangem.datasource.api.pay.models.response.WithdrawResponse
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.TangemPayWithdrawExchangeState
import com.tangem.domain.pay.TangemPayWithdrawState
import com.tangem.domain.pay.WithdrawalResult
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.utils.extensions.addHexPrefix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import kotlin.collections.set
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

private const val TAG = "TangemPaySwapRepository"

@Suppress("LongParameterList")
internal class DefaultTangemPayWithdrawRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val authDataSource: TangemPayAuthDataSource,
    private val quotesFetcher: QuotesFetcher,
    private val tangemPayStorage: TangemPayStorage,
    private val swapRepository: SwapRepository,
    private val orderRepository: CustomerOrderRepository,
) : TangemPayWithdrawRepository {

    private val withdrawPollingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val withdrawPollingJobs = mutableMapOf<String, Job>()
    private val withdrawPollingMutex = Mutex()

    override suspend fun withdraw(
        userWallet: UserWallet,
        receiverAddress: String,
        cryptoAmount: BigDecimal,
        cryptoCurrencyId: CryptoCurrency.RawID,
        exchangeData: TangemPayWithdrawExchangeState,
    ): Either<UniversalError, WithdrawalResult> {
        val amountInCents = getAmountInCents(cryptoAmount, cryptoCurrencyId)
        if (amountInCents.isNullOrEmpty()) return Either.Left(VisaApiError.WithdrawalDataError)
        return requestHelper.performRequest(userWallet.walletId) { authHeader ->
            val request = WithdrawDataRequest(amountInCents = amountInCents, recipientAddress = receiverAddress)
            tangemPayApi.getWithdrawData(authHeader = authHeader, body = request)
        }.map { data ->
            val result = data.result ?: return VisaApiError.WithdrawalDataError.left()
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
                        )
                        tangemPayApi.withdraw(authHeader = authHeader, body = request)
                    }
                        .mapLeft { return Either.Left(VisaApiError.WithdrawError) }
                        .map { response ->
                            processWithdrawResult(response, userWallet, exchangeData)
                            WithdrawalResult.Success
                        }
                }
                null -> return Either.Left(VisaApiError.SignWithdrawError)
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
            val orderData = orderRepository
                .getOrderData(userWalletId = userWallet.walletId, orderId = orderId).getOrNull()
            val withdrawTxHash = orderData?.withdrawTxHash
            val storeData = TangemPayWithdrawState(
                orderId = orderId,
                exchangeData = exchangeData,
            )
            if (orderData != null && !withdrawTxHash.isNullOrEmpty()) {
                finalizeWithdraw(
                    userWallet = userWallet,
                    withdrawTxHash = withdrawTxHash,
                    orderId = orderId,
                    exchangeData = exchangeData,
                    order = orderData,
                ).onLeft {
                    tangemPayStorage.storeWithdrawOrder(userWalletId = userWallet.walletId, data = storeData)
                }
            } else {
                tangemPayStorage.storeWithdrawOrder(userWalletId = userWallet.walletId, data = storeData)
            }
        }
    }

    private suspend fun finalizeWithdraw(
        userWallet: UserWallet,
        withdrawTxHash: String,
        orderId: String,
        exchangeData: TangemPayWithdrawExchangeState,
        order: OrderData,
    ): Either<ExpressDataError, Unit> {
        return swapRepository.exchangeSent(
            userWallet = userWallet,
            txId = exchangeData.txId,
            fromNetwork = exchangeData.fromNetwork,
            fromAddress = exchangeData.fromAddress,
            payInAddress = exchangeData.payInAddress,
            txHash = withdrawTxHash,
            payInExtraId = exchangeData.payInExtraId,
        )
            .onRight {
                val isActive = order.status == OrderStatus.NEW || order.status == OrderStatus.PROCESSING
                if (!isActive) {
                    tangemPayStorage.deleteWithdrawOrder(userWalletId = userWallet.walletId)
                } else {
                    tangemPayStorage.storeWithdrawOrder(
                        userWalletId = userWallet.walletId,
                        data = TangemPayWithdrawState(orderId = orderId, exchangeData = null),
                    )
                }
            }
            .onLeft { error ->
                Timber.tag(TAG).e(error.toString())
            }
    }

    override suspend fun hasWithdrawOrder(userWallet: UserWallet): Boolean {
        val orderExchangeData = tangemPayStorage.getWithdrawOrder(userWallet.walletId)
        if (orderExchangeData == null) return false

        val exchangeData = orderExchangeData.exchangeData
        val orderData = orderRepository
            .getOrderData(userWallet.walletId, orderId = orderExchangeData.orderId).getOrNull()
        val withdrawTxHash = orderData?.withdrawTxHash

        if (exchangeData != null && orderData != null && withdrawTxHash != null) {
            finalizeWithdraw(
                userWallet = userWallet,
                withdrawTxHash = withdrawTxHash,
                orderId = orderExchangeData.orderId,
                exchangeData = exchangeData,
                order = orderData,
            )
        }

        return orderData?.status == OrderStatus.NEW || orderData?.status == OrderStatus.PROCESSING
    }

    override suspend fun pollWithdrawOrderIfNeeds(userWallet: UserWallet): Either<VisaApiError, Unit> {
        val storeData = tangemPayStorage.getWithdrawOrder(userWallet.walletId) ?: return Unit.right()
        val exchangeData = storeData.exchangeData ?: return Unit.right()

        val orderId = storeData.orderId
        val order = orderRepository
            .getOrderData(userWalletId = userWallet.walletId, orderId = orderId).getOrNull()
            ?: return Unit.right()

        val txHash = order.withdrawTxHash

        if (!txHash.isNullOrEmpty()) {
            finalizeWithdraw(
                userWallet = userWallet,
                withdrawTxHash = txHash,
                orderId = storeData.orderId,
                exchangeData = exchangeData,
                order = order,
            ).onLeft {
                startWithdrawOrderPolling(
                    userWallet = userWallet,
                    orderId = orderId,
                    storeData = storeData,
                    exchangeData = exchangeData,
                )
            }
        } else {
            startWithdrawOrderPolling(
                userWallet = userWallet,
                orderId = orderId,
                storeData = storeData,
                exchangeData = exchangeData,
            )
        }

        return Unit.right()
    }

    private suspend fun startWithdrawOrderPolling(
        userWallet: UserWallet,
        orderId: String,
        storeData: TangemPayWithdrawState,
        exchangeData: TangemPayWithdrawExchangeState,
    ) {
        withdrawPollingMutex.withLock {
            if (withdrawPollingJobs.containsKey(orderId)) return

            val pollingJob = withdrawPollingScope.launch {
                try {
                    while (isActive && withdrawPollingJobs.containsKey(orderId)) {
                        delay(duration = 5.seconds)

                        val orderData = orderRepository
                            .getOrderData(userWalletId = userWallet.walletId, orderId = orderId)
                        orderData.onRight { order ->
                            if (order.status != OrderStatus.NEW && order.status != OrderStatus.PROCESSING) {
                                tangemPayStorage.deleteWithdrawOrder(userWallet.walletId)
                                withdrawPollingJobs.remove(key = orderId)
                                return@launch
                            }
                            val txHash = order.withdrawTxHash
                            if (!txHash.isNullOrEmpty()) {
                                finalizeWithdraw(
                                    userWallet = userWallet,
                                    withdrawTxHash = txHash,
                                    orderId = storeData.orderId,
                                    exchangeData = exchangeData,
                                    order = order,
                                ).onRight {
                                    withdrawPollingJobs.remove(key = orderId)
                                    return@launch
                                }
                            }
                        }.onLeft { error ->
                            Timber.tag(TAG).e("error ${error.errorCode}")
                        }
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Timber.tag(TAG).e(exception)
                    withdrawPollingMutex.withLock { withdrawPollingJobs.remove(orderId) }
                }
            }
            withdrawPollingJobs[orderId] = pollingJob
        }
    }

    private suspend fun getAmountInCents(cryptoAmount: BigDecimal, cryptoCurrencyId: CryptoCurrency.RawID): String? {
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