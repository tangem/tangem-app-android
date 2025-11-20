package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.data.pay.util.TangemPayWalletsManager
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.DeeplinkValidityRequest
import com.tangem.datasource.api.pay.models.request.OrderRequest
import com.tangem.datasource.api.pay.models.response.CustomerMeResponse
import com.tangem.datasource.local.visa.TangemPayCardFrozenStateStore
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val VALID_STATUS = "valid"
private const val APPROVED_KYC_STATUS = "APPROVED"
private const val TAG = "TangemPay: OnboardingRepository"

@Suppress("LongParameterList")
internal class DefaultOnboardingRepository @Inject constructor(
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val tangemPayStorage: TangemPayStorage,
    private val authDataSource: TangemPayAuthDataSource,
    private val tangemPayWalletsManager: TangemPayWalletsManager,
    private val cardFrozenStateStore: TangemPayCardFrozenStateStore,
) : OnboardingRepository {

    override suspend fun validateDeeplink(link: String): Either<UniversalError, Boolean> {
        return requestHelper.runWithErrorLogs(TAG) {
            val result = tangemPayApi.validateDeeplink(DeeplinkValidityRequest(link))
                .getOrThrow()
                .result
            result?.status == VALID_STATUS
        }
    }

    override suspend fun isTangemPayInitialDataProduced(): Boolean {
        val walletId = tangemPayWalletsManager.getDefaultWalletForTangemPay().walletId
        val customerWalletAddress = tangemPayStorage.getCustomerWalletAddress(walletId) ?: return false
        tangemPayStorage.getAuthTokens(customerWalletAddress) ?: return false

        return true
    }

    override suspend fun produceInitialData() {
        val wallet = tangemPayWalletsManager.getDefaultWalletForTangemPay()
        val initialCredentials = authDataSource.produceInitialCredentials(cardId = wallet.cardId)
            .fold(
                ifLeft = { error -> error("Can not produce initial data: ${error.message}") },
                ifRight = { it },
            )
        tangemPayStorage.storeCustomerWalletAddress(
            userWalletId = wallet.walletId,
            customerWalletAddress = initialCredentials.customerWalletAddress,
        )
        tangemPayStorage.storeAuthTokens(
            customerWalletAddress = initialCredentials.customerWalletAddress,
            tokens = initialCredentials.authTokens,
        )
    }

    override suspend fun getCustomerInfo(): Either<UniversalError, CustomerInfo> {
        return requestHelper.runWithErrorLogs(TAG) {
            val result = requestHelper.request { authHeader ->
                tangemPayApi.getCustomerMe(authHeader)
            }.result
            getCustomerInfo(result)
        }
    }

    override suspend fun getMainScreenCustomerInfo(): Either<UniversalError, MainScreenCustomerInfo> {
        return requestHelper.runWithErrorLogs(TAG) {
            val customerWalletAddress = requestHelper.getCustomerWalletAddress()

            when (val orderId = tangemPayStorage.getOrderId(customerWalletAddress)) {
                // If order id wasn't saved -> get customer info
                null -> {
                    MainScreenCustomerInfo(
                        info = getCustomerInfoWithPersistedToken(),
                        orderStatus = OrderStatus.UNKNOWN,
                    )
                }
                // If order id was saved -> check its status
                else -> {
                    val orderStatus = getOrderStatus(orderId)
                    val customerInfo = when (orderStatus) {
                        // Kyc is passed and user waits for order creation -> no need to get customer info
                        OrderStatus.NEW,
                        OrderStatus.PROCESSING,
                        -> CustomerInfo(productInstance = null, isKycApproved = true, cardInfo = null)

                        // Order was created/cancelled -> clear order id and get customer info
                        OrderStatus.UNKNOWN,
                        OrderStatus.COMPLETED,
                        OrderStatus.CANCELED,
                        -> getCustomerInfoWithPersistedToken().also {
                            tangemPayStorage.clearOrderId(customerWalletAddress)
                        }
                    }
                    MainScreenCustomerInfo(info = customerInfo, orderStatus = orderStatus)
                }
            }
        }
    }

    override suspend fun createOrder(): Either<UniversalError, Unit> = withContext(dispatcherProvider.io) {
        requestHelper.runWithErrorLogs(TAG) {
            val walletAddress = requestHelper.getCustomerWalletAddress()
            val result = requestHelper.request { authHeader ->
                tangemPayApi.createOrder(authHeader, body = OrderRequest(walletAddress))
            }.result ?: error("Create order result is null")

            tangemPayStorage.storeOrderId(result.data.customerWalletAddress, result.id)
        }
    }

    private suspend fun getCustomerInfo(response: CustomerMeResponse.Result?): CustomerInfo {
        val card = response?.card
        val balance = response?.balance
        val paymentAccount = response?.paymentAccount
        val cardInfo = if (paymentAccount != null && card != null && balance != null) {
            CardInfo(
                lastFourDigits = card.cardNumberEnd,
                balance = balance.availableBalance,
                currencyCode = balance.currency,
                customerWalletAddress = paymentAccount.customerWalletAddress,
                depositAddress = response.depositAddress,
            )
        } else {
            null
        }
        val productInstance = response?.productInstance?.let { instance ->
            cardFrozenStateStore.store(
                key = instance.cardId,
                value = when (instance.status) {
                    CustomerMeResponse.ProductInstance.Status.ACTIVE -> TangemPayCardFrozenState.Unfrozen
                    else -> TangemPayCardFrozenState.Frozen
                },
            )
            ProductInstance(
                id = instance.id,
                cardId = instance.cardId,
                status = when (instance.status) {
                    CustomerMeResponse.ProductInstance.Status.ACTIVE -> ProductInstance.Status.ACTIVE
                    else -> ProductInstance.Status.INACTIVE
                },
            )
        }
        return CustomerInfo(
            productInstance = productInstance,
            isKycApproved = response?.kyc?.status == APPROVED_KYC_STATUS,
            cardInfo = cardInfo,
        )
    }

    private suspend fun getOrderStatus(orderId: String): OrderStatus {
        val result = requestHelper.request { authHeader ->
            tangemPayApi.getOrder(authHeader, orderId)
        }.result ?: error("Order result is null")

        return when (result.status) {
            OrderStatus.NEW.apiName -> OrderStatus.NEW
            OrderStatus.PROCESSING.apiName -> OrderStatus.PROCESSING
            OrderStatus.COMPLETED.apiName -> OrderStatus.COMPLETED
            else -> OrderStatus.CANCELED
        }
    }

    private suspend fun getCustomerInfoWithPersistedToken(): CustomerInfo {
        val result = requestHelper.request { authHeader ->
            tangemPayApi.getCustomerMe(authHeader)
        }.result
        return getCustomerInfo(result)
    }
}