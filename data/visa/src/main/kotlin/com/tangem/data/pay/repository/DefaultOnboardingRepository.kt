package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.DeeplinkValidityRequest
import com.tangem.datasource.api.pay.models.request.OrderRequest
import com.tangem.datasource.api.pay.models.request.SetTangemPayEnabledRequest
import com.tangem.datasource.api.pay.models.response.CustomerMeResponse
import com.tangem.datasource.api.pay.models.response.OrderResponse
import com.tangem.datasource.local.visa.TangemPayCardFrozenStateStore
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.TangemPayEligibilityType
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

private const val VALID_STATUS = "valid"

@Suppress("LongParameterList")
internal class DefaultOnboardingRepository @Inject constructor(
    private val analytics: AnalyticsEventHandler,
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val tangemPayStorage: TangemPayStorage,
    private val authDataSource: TangemPayAuthDataSource,
    private val cardFrozenStateStore: TangemPayCardFrozenStateStore,
    private val userWalletsListRepository: UserWalletsListRepository,
) : OnboardingRepository {

    // Save data for a session
    private val lastFetchedCustomerInfoMap = ConcurrentHashMap<UserWalletId, CustomerInfo>()

    override suspend fun validateDeeplink(link: String): Either<VisaApiError, Boolean> {
        return requestHelper.performWithStaticToken {
            tangemPayApi.validateDeeplink(body = DeeplinkValidityRequest(link = link))
        }.map { response ->
            response.result?.status.equals(VALID_STATUS, ignoreCase = true)
        }
    }

    override suspend fun isTangemPayInitialDataProduced(userWalletId: UserWalletId): Boolean {
        return withContext(dispatcherProvider.io) {
            val customerWalletAddress =
                tangemPayStorage.getCustomerWalletAddress(userWalletId) ?: return@withContext false
            tangemPayStorage.getAuthTokens(customerWalletAddress) ?: return@withContext false

            return@withContext true
        }
    }

    override suspend fun produceInitialData(userWalletId: UserWalletId) {
        withContext(dispatcherProvider.io) {
            val userWallet = getUserWallet(userWalletId)
            val initialCredentials = authDataSource.produceInitialCredentials(userWallet)
                .fold(
                    ifLeft = { error -> error("Can not produce initial data: ${error.message}") },
                    ifRight = { it },
                )
            // should storeCheckCustomerWalletResult because we already know this
            tangemPayStorage.storeCheckCustomerWalletResult(userWallet.walletId, true)
            tangemPayStorage.storeCustomerWalletAddress(
                userWalletId = userWallet.walletId,
                customerWalletAddress = initialCredentials.customerWalletAddress,
            )
            tangemPayStorage.storeAuthTokens(
                customerWalletAddress = initialCredentials.customerWalletAddress,
                tokens = initialCredentials.authTokens,
            )
        }
    }

    override suspend fun getCustomerInfo(userWalletId: UserWalletId): Either<VisaApiError, CustomerInfo> {
        return requestHelper.performRequest(userWalletId) { authHeader -> tangemPayApi.getCustomerMe(authHeader) }
            .map { response -> getCustomerInfo(userWalletId = userWalletId, response = response.result) }
    }

    override suspend fun clearOrderId(userWalletId: UserWalletId) {
        withContext(dispatcherProvider.io) {
            val customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
            tangemPayStorage.clearOrderId(customerWalletAddress = customerWalletAddress)
        }
    }

    override suspend fun getOrderId(userWalletId: UserWalletId): String? {
        return withContext(dispatcherProvider.io) {
            val customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
            tangemPayStorage.getOrderId(customerWalletAddress)
        }
    }

    override fun getSavedCustomerInfo(userWalletId: UserWalletId): CustomerInfo? {
        return lastFetchedCustomerInfoMap[userWalletId]
    }

    override suspend fun createOrder(userWalletId: UserWalletId): Either<VisaApiError, String> =
        withContext(dispatcherProvider.io) {
            val existingOrderId = getOrderId(userWalletId)
            if (existingOrderId != null) {
                val orderStatus = requestHelper.performRequest(userWalletId) { authHeader ->
                    tangemPayApi.getOrder(authHeader, existingOrderId)
                }.map { it.result?.status }.getOrNull()

                if (orderStatus == OrderResponse.Result.Status.NEW ||
                    orderStatus == OrderResponse.Result.Status.PROCESSING
                ) {
                    return@withContext existingOrderId.right()
                }
            }

            val walletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
            requestHelper.performRequest(userWalletId) { authHeader ->
                val data = OrderRequest.Data(customerWalletAddress = walletAddress)
                tangemPayApi.createOrder(authHeader, body = OrderRequest(data = data))
            }.map { response ->
                val result = requireNotNull(response.result)
                tangemPayStorage.storeOrderId(walletAddress, result.id)
                result.id
            }
        }

    private fun getUserWallet(userWalletId: UserWalletId): UserWallet {
        return userWalletsListRepository.userWallets.value?.firstOrNull { it.walletId == userWalletId }
            ?: error("no userWallet found")
    }

    private suspend fun getCustomerInfo(
        userWalletId: UserWalletId,
        response: CustomerMeResponse.Result?,
    ): CustomerInfo {
        val kycStatus = KycStatus.fromString(status = response?.kyc?.status)
        sendKycAnalytics(kycStatus)

        val card = response?.card
        val fiatBalance = response?.balance?.fiat
        val paymentAccount = response?.paymentAccount
        val cardInfo = if (paymentAccount != null && card != null && fiatBalance != null) {
            CardInfo(
                lastFourDigits = card.cardNumberEnd,
                balance = fiatBalance.availableBalance,
                currencyCode = fiatBalance.currency,
                depositAddress = response.depositAddress,
                isPinSet = response.card?.isPinSet == true,
            )
        } else {
            null
        }
        val productInstance = response?.productInstance?.let { instance ->
            val cardFrozenState = when (instance.status) {
                CustomerMeResponse.ProductInstance.Status.ACTIVE -> TangemPayCardFrozenState.Unfrozen
                else -> TangemPayCardFrozenState.Frozen
            }
            cardFrozenStateStore.store(key = instance.cardId, value = cardFrozenState)

            ProductInstance(id = instance.id, cardId = instance.cardId)
        }
        return CustomerInfo(
            customerId = response?.id,
            productInstance = productInstance,
            kycStatus = kycStatus,
            cardInfo = cardInfo,
        ).also {
            lastFetchedCustomerInfoMap[userWalletId] = it
        }
    }

    private fun sendKycAnalytics(kycStatus: KycStatus) {
        val event = when (kycStatus) {
            KycStatus.APPROVED -> TangemPayAnalyticsEvents.KycPassedAndOrderCreated()
            KycStatus.REJECTED -> TangemPayAnalyticsEvents.KycRejected()
            KycStatus.INIT,
            KycStatus.PENDING,
            -> return
        }
        analytics.send(event)
    }

    override suspend fun hasTangemPayInWallet(userWalletId: UserWalletId): Either<VisaApiError, Boolean> {
        val hasTangemPay = tangemPayStorage.checkCustomerWalletResult(userWalletId)
        if (hasTangemPay != null) {
            return Either.Right(hasTangemPay)
        }

        return requestHelper.performWithStaticToken {
            tangemPayApi.checkCustomerWalletId(customerWalletId = userWalletId.stringValue)
        }.map { response ->
            val id = response.result?.id
            val isTangemPayEnabled = response.result?.isTangemPayEnabled == true
            val shouldShowTangemPayBlock = !id.isNullOrEmpty() && isTangemPayEnabled
            tangemPayStorage.storeCheckCustomerWalletResult(userWalletId = userWalletId, shouldShowTangemPayBlock)
            shouldShowTangemPayBlock
        }.mapLeft { error ->
            if (error is VisaApiError.NotPaeraCustomer) {
                tangemPayStorage.storeCheckCustomerWalletResult(userWalletId = userWalletId, false)
            }
            error
        }
    }

    override suspend fun checkCustomerEligibility(): List<TangemPayEligibilityType> {
        val response = requestHelper.performWithStaticToken {
            tangemPayApi.getEligibilityChannels()
        }.getOrNull()

        val channels = response?.result?.channels
        val eligibility = channels.orEmpty().toSet()
        tangemPayStorage.storeTangemPayEligibility(eligibility = eligibility)

        return eligibility.map(TangemPayEligibilityType::fromString)
    }

    override suspend fun getCustomerEligibility(): List<TangemPayEligibilityType> {
        return tangemPayStorage.getTangemPayEligibility().map(TangemPayEligibilityType::fromString)
    }

    override suspend fun getHideMainOnboardingBanner(userWalletId: UserWalletId): Boolean {
        return tangemPayStorage.getHideMainOnboardingBanner(userWalletId)
    }

    override suspend fun setHideMainOnboardingBanner(userWalletId: UserWalletId) {
        tangemPayStorage.storeHideOnboardingBanner(userWalletId, hide = true)
    }

    override suspend fun disableTangemPay(userWalletId: UserWalletId): Either<VisaApiError, Unit> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.setTangemPayEnabledStatus(
                authHeader = authHeader,
                body = SetTangemPayEnabledRequest(isTangemPayEnabled = false),
            )
        }.map {
            val address = requestHelper.getCustomerWalletAddress(userWalletId)
            tangemPayStorage.clearAll(userWalletId = userWalletId, customerWalletAddress = address)
            setHideMainOnboardingBanner(userWalletId)
        }
    }
}