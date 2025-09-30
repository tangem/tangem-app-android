package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.DeeplinkValidityRequest
import com.tangem.datasource.api.pay.models.request.OrderRequest
import com.tangem.datasource.api.pay.models.response.CustomerMeResponse
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.repository.OnboardingRepository
import javax.inject.Inject

private const val VALID_STATUS = "valid"
private const val TAG = "TangemPay: OnboardingRepository"

internal class DefaultOnboardingRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val tangemPayStorage: TangemPayStorage,
) : OnboardingRepository {

    override suspend fun validateDeeplink(link: String): Either<UniversalError, Boolean> {
        return requestHelper.runWithErrorLogs(TAG) {
            val result = requestHelper.request {
                tangemPayApi.validateDeeplink(DeeplinkValidityRequest(link))
            }.result
            result?.status == VALID_STATUS
        }
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
            val result = requestHelper.requestWithPersistedToken { authHeader ->
                tangemPayApi.getCustomerMe(authHeader)
            }.result

            val orderStatus = getOrderStatus().getOrNull() ?: error("Order status is null")

            MainScreenCustomerInfo(info = getCustomerInfo(result), orderStatus = orderStatus)
        }
    }

    override suspend fun createOrder(): Either<UniversalError, Unit> {
        return requestHelper.runWithErrorLogs(TAG) {
            val walletAddress = requestHelper.getCustomerWalletAddress()
            val result = requestHelper.requestWithPersistedToken { authHeader ->
                tangemPayApi.createOrder(authHeader, body = OrderRequest(walletAddress))
            }.result ?: error("Create order result is null")

            tangemPayStorage.storeOrderId(result.data.customerWalletAddress, result.id)
        }
    }

    private fun getCustomerInfo(response: CustomerMeResponse.Result?): CustomerInfo {
        val card = response?.card
        val balance = response?.balance
        val productInstance = response?.productInstance
        val cardInfo = if (productInstance != null && card != null && balance != null) {
            CardInfo(
                lastFourDigits = card.cardNumberEnd,
                balance = balance.availableBalance,
                currencyCode = balance.currency,
                customerWalletAddress = productInstance.cardWalletAddress,

            )
        } else {
            null
        }
        return CustomerInfo(
            productInstance = response?.productInstance?.let { ProductInstance(id = it.id, status = it.status) },
            kycStatus = response?.kyc?.status,
            cardInfo = cardInfo,
        )
    }

    private suspend fun getOrderStatus(): Either<UniversalError, OrderStatus> {
        return requestHelper.runWithErrorLogs(TAG) {
            val walletAddress = requestHelper.getCustomerWalletAddress()
            val orderId: String = tangemPayStorage.getOrderId(walletAddress)
                ?: return@runWithErrorLogs OrderStatus.NOT_ISSUED

            val result = requestHelper.request { authHeader ->
                tangemPayApi.getOrder(authHeader, orderId)
            }.result ?: error("Order result is null")

            when (result.status) {
                OrderStatus.NEW.apiName -> OrderStatus.NEW
                OrderStatus.PROCESSING.apiName -> OrderStatus.PROCESSING
                OrderStatus.COMPLETED.apiName -> OrderStatus.COMPLETED
                else -> OrderStatus.CANCELED
            }
        }
    }
}