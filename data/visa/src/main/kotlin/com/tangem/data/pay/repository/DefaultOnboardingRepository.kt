package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.DeeplinkValidityRequest
import com.tangem.datasource.api.pay.models.request.OrderRequest
import com.tangem.datasource.api.pay.models.response.CustomerMeResponse
import com.tangem.datasource.local.visa.TangemPayCardFrozenStateStore
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
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
    private val cardFrozenStateStore: TangemPayCardFrozenStateStore,
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
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
            val initialCredentials = authDataSource.produceInitialCredentials(cardId = getCardId(userWalletId))
                .fold(
                    ifLeft = { error -> error("Can not produce initial data: ${error.message}") },
                    ifRight = { it },
                )
            // should storeCheckCustomerWalletResult because we already know this
            tangemPayStorage.storeCheckCustomerWalletResult(userWalletId, true)
            tangemPayStorage.storeCustomerWalletAddress(
                userWalletId = userWalletId,
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

    override suspend fun createOrder(userWalletId: UserWalletId) = withContext(dispatcherProvider.io) {
        launch {
            requestHelper.runWithErrorLogs(TAG) {
                val walletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
                val result = requestHelper.request(userWalletId) { authHeader ->
                    tangemPayApi.createOrder(authHeader, body = OrderRequest(walletAddress))
                }.result ?: error("Create order result is null")

                val customerWalletAddress = requireNotNull(result.data.customerWalletAddress)
                tangemPayStorage.storeOrderId(customerWalletAddress, result.id)
            }
        }
    }

    private fun getCardId(userWalletId: UserWalletId): String {
        val userWallet = if (hotWalletFeatureToggles.isHotWalletEnabled) {
            userWalletsListRepository.userWallets.value?.firstOrNull { it.walletId == userWalletId }
        } else {
            userWalletsListManager.userWalletsSync.firstOrNull { it.walletId == userWalletId }
        } ?: error("no userWallet found")
        return if (userWallet is UserWallet.Cold) {
            userWallet.cardId
        } else {
            TODO("[REDACTED_JIRA]")
        }
    }

    private suspend fun getCustomerInfo(
        userWalletId: UserWalletId,
        response: CustomerMeResponse.Result?,
    ): CustomerInfo {
        val card = response?.card
        val fiatBalance = response?.balance?.fiat
        val paymentAccount = response?.paymentAccount
        val cardInfo = if (paymentAccount != null && card != null && fiatBalance != null) {
            CardInfo(
                lastFourDigits = card.cardNumberEnd,
                balance = fiatBalance.availableBalance,
                currencyCode = fiatBalance.currency,
                customerWalletAddress = paymentAccount.customerWalletAddress,
                depositAddress = response.depositAddress,
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

            ProductInstance(id = instance.id, cardId = instance.cardId, cardFrozenState = cardFrozenState)
        }
        return CustomerInfo(
            productInstance = productInstance,
            isKycApproved = response?.kyc?.status == APPROVED_KYC_STATUS,
            cardInfo = cardInfo,
        ).also {
            lastFetchedCustomerInfoMap[userWalletId] = it
        }
    }

    override suspend fun checkCustomerWallet(userWalletId: UserWalletId): Either<VisaApiError, Boolean> {
        val hasTangemPay = tangemPayStorage.checkCustomerWalletResult(userWalletId)
        if (hasTangemPay != null) {
            return Either.Right(hasTangemPay)
        }

        return requestHelper.performWithStaticToken { staticToken ->
            tangemPayApi.checkCustomerWalletId(
                authHeader = staticToken,
                customerWalletId = userWalletId.stringValue,
            )
        }.map { response ->
            val id = response.result?.id
            val isPaeraCustomer = !id.isNullOrEmpty()
            tangemPayStorage.storeCheckCustomerWalletResult(userWalletId = userWalletId, isPaeraCustomer)
            isPaeraCustomer
        }.mapLeft { error ->
            if (error is VisaApiError.NotPaeraCustomer) {
                tangemPayStorage.storeCheckCustomerWalletResult(userWalletId = userWalletId, false)
            }
            error
        }
    }

    override suspend fun checkCustomerEligibility(): Boolean {
        val response = requestHelper.performWithoutToken {
            tangemPayApi.checkCustomerEligibility()
        }.getOrNull()
        return response?.result?.isTangemPayAvailable == true
    }

    override suspend fun getHideMainOnboardingBanner(userWalletId: UserWalletId): Boolean {
        return tangemPayStorage.getHideMainOnboardingBanner(userWalletId)
    }

    override suspend fun setHideMainOnboardingBanner(userWalletId: UserWalletId) {
        tangemPayStorage.storeHideOnboardingBanner(userWalletId, hide = true)
    }
}