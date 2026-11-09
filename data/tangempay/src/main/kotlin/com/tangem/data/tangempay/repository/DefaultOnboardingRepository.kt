package com.tangem.data.tangempay.repository

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.tangempay.analytics.TangemPayUserPropertiesSender
import com.tangem.data.tangempay.store.CashbackSummaryStore
import com.tangem.data.tangempay.store.PaymentAccountStatusesStore
import com.tangem.data.tangempay.store.TangemPayCustomerInfoStore
import com.tangem.data.tangempay.util.BankCredentialsConverter
import com.tangem.data.tangempay.util.CustomerInfoConverter
import com.tangem.data.tangempay.util.AccountFeeConverter
import com.tangem.spend.datasource.tangempay.TangemPayApi
import com.tangem.spend.datasource.tangempay.models.request.DeeplinkValidityRequest
import com.tangem.spend.datasource.tangempay.models.request.OrderRequest
import com.tangem.spend.datasource.tangempay.models.request.SetTangemPayEnabledRequest
import com.tangem.spend.datasource.tangempay.models.response.CustomerMeResponse
import com.tangem.spend.datasource.tangempay.models.response.OrderResponse
import com.tangem.spend.datasource.tangempay.store.TangemPayCardFrozenStateStore
import com.tangem.data.tangempay.store.TangemPayStorage
import com.tangem.data.tangempay.util.OrderRequestConverter
import com.tangem.spend.datasource.tangempay.store.TangemPayTxHistoryItemsStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.tangempay.TangemPayEligibilityType
import com.tangem.domain.models.tangempay.TangemPayCustomerResidency
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.tangempay.id
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.datasource.TangemPayAuthDataSource
import com.tangem.domain.tangempay.model.AccountFee
import com.tangem.domain.tangempay.model.AccountFeeGroupType
import com.tangem.domain.tangempay.model.CustomerInfo
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.TangemPayInitialCredentials
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val VALID_STATUS = "valid"
private const val ALL_RAILS = "all"
private const val FIAT_BANK_CREDENTIALS_TYPE = "fiat"

@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
internal class DefaultOnboardingRepository @Inject constructor(
    private val analytics: AnalyticsEventHandler,
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val tangemPayStorage: TangemPayStorage,
    private val authDataSource: TangemPayAuthDataSource,
    private val cardFrozenStateStore: TangemPayCardFrozenStateStore,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val paymentAccountStatusStore: PaymentAccountStatusesStore,
    private val txHistoryItemsStore: TangemPayTxHistoryItemsStore,
    private val cashbackSummaryStore: CashbackSummaryStore,
    private val customerInfoStore: TangemPayCustomerInfoStore,
    private val derivationsRepository: DerivationsRepository,
    private val userPropertiesSender: TangemPayUserPropertiesSender,
) : OnboardingRepository {

    override suspend fun validateDeeplink(link: String): Either<TangemPayApiError, Boolean> {
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
            val initialData = authDataSource.produceInitialData(userWallet)
                .fold(
                    ifLeft = { error -> error("Can not produce initial data: ${error.message}") },
                    ifRight = { it },
                )
            // should storeCheckCustomerWalletResult because we already know this
            tangemPayStorage.storeCheckCustomerWalletResult(userWallet.walletId, true)
            storeInitialCredentials(
                userWalletId = userWallet.walletId,
                initialCredentials = initialData.initialCredentials,
            )
            derivationsRepository.storeDerivedKeys(
                userWalletId = userWallet.walletId,
                derivedKeys = initialData.moneyAccountDerivedKeys,
            )
        }
    }

    override suspend fun storeInitialCredentials(
        userWalletId: UserWalletId,
        initialCredentials: TangemPayInitialCredentials,
    ) {
        withContext(dispatcherProvider.io) {
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

    override suspend fun getCustomerInfo(userWalletId: UserWalletId): Either<TangemPayApiError, CustomerInfo> {
        return requestHelper.performRequest(userWalletId) { authHeader -> tangemPayApi.getCustomerMe(authHeader) }
            .flatMap { response ->
                val result = response.result ?: return@flatMap TangemPayApiError.UnknownWithoutCode.left()
                val productInstances = result.productInstances

                val isDeactivated = productInstances.isNotEmpty() &&
                    productInstances.all { it.status == CustomerMeResponse.ProductInstance.Status.DEACTIVATED }

                val isFormer = result.state.let { CustomerInfo.State.fromString(it) } == CustomerInfo.State.FORMER
                if (isDeactivated || isFormer) {
                    tangemPayStorage.storeIsTangemPayDeactivated(userWalletId)
                }
                getCustomerInfo(userWalletId = userWalletId, response = result).right()
            }
            .onRight { customerInfo -> userPropertiesSender.onCustomerInfoFetched(userWalletId, customerInfo) }
    }

    override suspend fun getBankCredentials(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<TangemPayApiError, BankCredentials> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getBankCredentials(authHeader = authHeader, productInstanceId = productInstanceId)
        }.flatMap { response ->
            val result = response.result ?: return@flatMap TangemPayApiError.UnknownWithoutCode.left()
            BankCredentialsConverter.convert(result).right()
        }
    }

    override suspend fun getAllBankCredentials(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<TangemPayApiError, List<BankCredentials>> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getAllBankCredentials(
                authHeader = authHeader,
                productInstanceId = productInstanceId,
                rails = ALL_RAILS,
            )
        }.flatMap { response ->
            val credentials = response.result.orEmpty()
                .filter { it.type.equals(FIAT_BANK_CREDENTIALS_TYPE, ignoreCase = true) }
                .filterNot { it.accountNumber.isNullOrBlank() }
                .map(BankCredentialsConverter::convert)
            if (credentials.isEmpty()) TangemPayApiError.UnknownWithoutCode.left() else credentials.right()
        }
    }

    override suspend fun getFees(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
        types: Set<AccountFeeGroupType>,
    ): Either<TangemPayApiError, List<AccountFee>> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getFees(
                authHeader = authHeader,
                accountType = product.id,
                groups = types.map(AccountFeeGroupType::toString),
            )
        }.flatMap { response ->
            val result = response.result ?: return@flatMap TangemPayApiError.UnknownWithoutCode.left()
            result.mapNotNull(AccountFeeConverter::convert).right()
        }
    }

    override suspend fun isTangemPayDeactivated(userWalletId: UserWalletId): Boolean {
        return tangemPayStorage.isTangemPayDeactivated(userWalletId)
    }

    override suspend fun storeCustomerResidency(userWalletId: UserWalletId, residency: TangemPayCustomerResidency) {
        tangemPayStorage.storeCustomerResidency(userWalletId, residency)
    }

    override suspend fun getCustomerResidency(userWalletId: UserWalletId): TangemPayCustomerResidency? {
        return tangemPayStorage.getCustomerResidency(userWalletId)
    }

    override suspend fun clearCustomerResidency(userWalletId: UserWalletId) {
        tangemPayStorage.clearCustomerResidency(userWalletId)
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
        return customerInfoStore.get().value[userWalletId]
    }

    override suspend fun createOrder(
        userWalletId: UserWalletId,
        request: CustomerOrderRequest.CardIssue,
        idempotencyKey: String,
    ): Either<TangemPayApiError, String> = withContext(dispatcherProvider.io) {
        val walletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
        val isCampaignOrder = request.deeplinkValue != null
        val reusableOrderId = findReusableOrderId(
            userWalletId = userWalletId,
            walletAddress = walletAddress,
            isCampaignOrder = isCampaignOrder,
        )
        if (reusableOrderId != null) return@withContext reusableOrderId.right()

        val body = OrderRequestConverter.convert(
            OrderRequestConverter.Input(customerWalletAddress = walletAddress, request = request),
        ).getOrElse { return@withContext it.left() }

        requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.createOrder(authHeader = authHeader, idempotencyKey = idempotencyKey, body = body)
        }.flatMap { response ->
            val result = response.result ?: return@flatMap TangemPayApiError.UnknownWithoutCode.left()
            tangemPayStorage.storeOrderId(walletAddress, result.id, isCampaignOrder)
            result.id.right()
        }
    }

    private suspend fun findReusableOrderId(
        userWalletId: UserWalletId,
        walletAddress: String,
        isCampaignOrder: Boolean,
    ): String? {
        val existingOrderId = tangemPayStorage.getOrderId(walletAddress) ?: return null
        if (isCampaignOrder && !tangemPayStorage.isCampaignOrder(walletAddress)) return null

        val orderStatus = requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getOrder(authHeader, existingOrderId)
        }.map { it.result?.status }.getOrNull()
        val isActive = orderStatus == OrderResponse.Result.Status.NEW ||
            orderStatus == OrderResponse.Result.Status.PROCESSING

        return existingOrderId.takeIf { isActive }
    }

    override suspend fun createVirtualAccountOrder(
        userWalletId: UserWalletId,
        paymentAccountAddress: String,
        idempotencyKey: String,
    ): Either<TangemPayApiError, String> = withContext(dispatcherProvider.io) {
        requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.createOrder(
                authHeader = authHeader,
                idempotencyKey = idempotencyKey,
                body = OrderRequest(data = OrderRequest.VirtualAccountIssue(depositAddress = paymentAccountAddress)),
            )
        }.flatMap { response -> response.result?.id?.right() ?: TangemPayApiError.UnknownWithoutCode.left() }
    }

    override suspend fun getVirtualAccountOrderId(userWalletId: UserWalletId): String? =
        withContext(dispatcherProvider.io) {
            val customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
            tangemPayStorage.getVirtualAccountOrderId(customerWalletAddress)
        }

    override suspend fun storeVirtualAccountOrderId(userWalletId: UserWalletId, vaOrderId: String) {
        withContext(dispatcherProvider.io) {
            val customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
            tangemPayStorage.storeVirtualAccountOrderId(customerWalletAddress, vaOrderId)
        }
    }

    override suspend fun clearVirtualAccountOrderId(userWalletId: UserWalletId) {
        withContext(dispatcherProvider.io) {
            val customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
            tangemPayStorage.clearVirtualAccountOrderId(customerWalletAddress)
        }
    }

    override suspend fun hasVirtualAccountOrderFailed(userWalletId: UserWalletId): Boolean =
        withContext(dispatcherProvider.io) {
            val customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
            tangemPayStorage.getVirtualAccountOrderFailed(customerWalletAddress)
        }

    override suspend fun storeVirtualAccountOrderFailed(userWalletId: UserWalletId, isFailed: Boolean) {
        withContext(dispatcherProvider.io) {
            val customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
            tangemPayStorage.storeVirtualAccountOrderFailed(customerWalletAddress, isFailed)
        }
    }

    private fun getUserWallet(userWalletId: UserWalletId): UserWallet {
        return userWalletsListRepository.userWallets.value?.firstOrNull { it.walletId == userWalletId }
            ?: error("no userWallet found")
    }

    @Suppress("ComplexCondition")
    private suspend fun getCustomerInfo(
        userWalletId: UserWalletId,
        response: CustomerMeResponse.Result,
    ): CustomerInfo {
        val customerInfo = CustomerInfoConverter.convert(response)
        sendKycAnalytics(customerInfo.kycStatus)

        // Keep the per-card frozen state up to date for every card.
        customerInfo.cardProductInstances.forEach { instance ->
            cardFrozenStateStore.store(key = instance.cardId, value = instance.frozenState)
        }

        return customerInfo.also { customerInfoStore.update { cache -> cache + (userWalletId to it) } }
    }

    private fun sendKycAnalytics(kycStatus: KycStatus) {
        val event = when (kycStatus) {
            KycStatus.APPROVED -> TangemPayAnalyticsEvents.KycPassedAndOrderCreated()
            KycStatus.REJECTED -> TangemPayAnalyticsEvents.KycRejected()
            KycStatus.UNKNOWN,
            KycStatus.REQUIRED,
            KycStatus.INIT,
            KycStatus.PENDING,
            -> return
        }
        analytics.send(event)
    }

    override suspend fun hasTangemPayInWallet(userWalletId: UserWalletId): Either<TangemPayApiError, Boolean> {
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
            if (error is TangemPayApiError.NotFound) {
                tangemPayStorage.storeCheckCustomerWalletResult(userWalletId = userWalletId, false)
            }
            error
        }
    }

    override suspend fun checkCustomerEligibility(): List<TangemPayEligibilityType> {
        val response = requestHelper.performWithStaticToken {
            tangemPayApi.getEligibilityChannels()
        }.getOrNull() ?: return emptyList()

        val eligibility = response.result.channels.toSet()
        tangemPayStorage.storeTangemPayEligibility(eligibility = eligibility)

        return eligibility.map(TangemPayEligibilityType::fromString)
    }

    override suspend fun getCustomerEligibility(): List<TangemPayEligibilityType>? {
        return tangemPayStorage.getTangemPayEligibility()?.map(TangemPayEligibilityType::fromString)
    }

    override suspend fun fetchCustomerEligibility(
        userWalletId: UserWalletId,
    ): Either<TangemPayApiError, List<TangemPayEligibilityType>> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getUserEligibilityChannels(authHeader)
        }.map { response ->
            response.result.channels.map(TangemPayEligibilityType::fromString)
        }
    }

    override fun isMainOnboardingBannerHidden(userWalletId: UserWalletId): Flow<Boolean> {
        return tangemPayStorage.isMainOnboardingBannerHidden(userWalletId)
    }

    override suspend fun setHideMainOnboardingBanner(userWalletId: UserWalletId) {
        tangemPayStorage.storeHideOnboardingBanner(userWalletId, hide = true)
    }

    override suspend fun disableTangemPay(userWalletId: UserWalletId): Either<TangemPayApiError, Unit> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.setTangemPayEnabledStatus(
                authHeader = authHeader,
                body = SetTangemPayEnabledRequest(isTangemPayEnabled = false),
            )
        }.map {
            val address = requestHelper.getCustomerWalletAddress(userWalletId)
            tangemPayStorage.clearAll(userWalletId = userWalletId, customerWalletAddress = address)
            txHistoryItemsStore.remove(userWalletId.stringValue)
            cashbackSummaryStore.remove(listOf(userWalletId))
            setHideMainOnboardingBanner(userWalletId)
            paymentAccountStatusStore.store(
                userWalletId = userWalletId,
                status = AccountStatus.Payment(
                    account = Account.Payment(userWalletId),
                    value = PaymentAccountStatusValue.Empty,
                ),
            )
        }
    }
}