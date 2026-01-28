package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.*
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import kotlinx.coroutines.flow.*
import timber.log.Timber

private const val TAG = "TangemPayMainScreenCustomerInfoUseCase"

class TangemPayMainScreenCustomerInfoUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val customerOrderRepository: CustomerOrderRepository,
    private val eligibilityManager: TangemPayEligibilityManager,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val deviceSecurity: DeviceSecurityInfoProvider,
) {

    val state: StateFlow<Map<UserWalletId, Either<TangemPayCustomerInfoError, MainCustomerInfoContentState>>>
        field = MutableStateFlow(value = mapOf())

    suspend fun fetch(userWalletId: UserWalletId) {
        Timber.tag(TAG).i("fetch: ${userWalletId.stringValue}")

        if (deviceSecurity.isSecurityExposed()) {
            Timber.tag(TAG).i("fetch security info: rooted: ${deviceSecurity.isRooted}")
            Timber.tag(TAG).i("fetch security info: xposed: ${deviceSecurity.isXposed}")
            Timber.tag(TAG).i("fetch security info: bootloader unlocked: ${deviceSecurity.isBootloaderUnlocked}")

            updateState(userWalletId = userWalletId, either = TangemPayCustomerInfoError.ExposedDeviceError.left())
            return // fast exit
        }

        onboardingRepository.checkCustomerWallet(userWalletId)
            .fold(
                ifLeft = { error ->
                    Timber.tag(TAG).e("Failed checkCustomerWallet for $userWalletId: ${error.javaClass.simpleName}")
                    if (error is VisaApiError.NotPaeraCustomer && tangemPayFeatureToggles.isEntryPointsEnabled) {
                        showOnboardingBannerIfEligible(userWalletId)
                    } else {
                        updateState(userWalletId, TangemPayCustomerInfoError.UnknownError.left())
                    }
                },
                ifRight = { hasTangemPay ->
                    Timber.tag(TAG).i("checkCustomerWallet for $userWalletId: $hasTangemPay")
                    if (hasTangemPay) {
                        val oldResult = state.value[userWalletId]
                        if (oldResult == null) {
                            updateState(userWalletId, MainCustomerInfoContentState.Loading.right())
                        }

                        val result = proceedWithPaeraCustomerResult(userWalletId)
                            .map(MainCustomerInfoContentState::Content)
                        updateState(userWalletId, result)
                    } else {
                        if (tangemPayFeatureToggles.isEntryPointsEnabled) {
                            // if there's no tangem pay, check eligibility and show onboarding banner
                            showOnboardingBannerIfEligible(userWalletId)
                        } else {
                            // ignore if there's no TangemPay
                            updateState(userWalletId, TangemPayCustomerInfoError.UnknownError.left())
                        }
                    }
                },
            )
    }

    private suspend fun showOnboardingBannerIfEligible(userWalletId: UserWalletId) {
        val isEligible = eligibilityManager
            .getEligibleWallets(shouldExcludePaeraCustomers = false)
            .any { it.walletId == userWalletId }
        if (isEligible) {
            if (onboardingRepository.getHideMainOnboardingBanner(userWalletId)) {
                updateState(userWalletId, TangemPayCustomerInfoError.UnknownError.left())
            } else {
                updateState(userWalletId, MainCustomerInfoContentState.OnboardingBanner.right())
            }
        } else {
            updateState(userWalletId, TangemPayCustomerInfoError.UnknownError.left())
        }
    }

    operator fun invoke(
        userWalletId: UserWalletId,
    ): Flow<Either<TangemPayCustomerInfoError, MainCustomerInfoContentState>> {
        return state.mapNotNull { map -> map[userWalletId] }
    }

    private fun updateState(
        userWalletId: UserWalletId,
        either: Either<TangemPayCustomerInfoError, MainCustomerInfoContentState>,
    ) {
        state.update { currentMap ->
            currentMap.toMutableMap().apply { this[userWalletId] = either }
        }
    }

    private suspend fun proceedWithPaeraCustomerResult(
        userWalletId: UserWalletId,
    ): Either<TangemPayCustomerInfoError, MainScreenCustomerInfo> {
        if (!onboardingRepository.isTangemPayInitialDataProduced(userWalletId)) {
            return TangemPayCustomerInfoError.RefreshNeededError.left()
        }
        val orderId = onboardingRepository.getOrderId(userWalletId)
        return if (orderId != null) {
            proceedWithOrderId(userWalletId = userWalletId, orderId = orderId)
        } else {
            proceedWithoutOrder(userWalletId = userWalletId)
        }
    }

    private suspend fun proceedWithoutOrder(
        userWalletId: UserWalletId,
    ): Either<TangemPayCustomerInfoError, MainScreenCustomerInfo> {
        return onboardingRepository.getCustomerInfo(userWalletId)
            .mapLeft { error ->
                Timber.tag(TAG).e("mapErrorForCustomer: $error")
                error.mapErrorForCustomer()
            }
            .map { customerInfo ->
                Timber.tag(TAG).i("customerInfo")
                if (customerInfo.cardInfo == null && customerInfo.kycStatus == CustomerInfo.KycStatus.APPROVED) {
                    // If order id wasn't saved -> start order creation and get customer info
                    onboardingRepository.createOrder(userWalletId)
                }
                MainScreenCustomerInfo(info = customerInfo, orderStatus = OrderStatus.UNKNOWN)
            }
    }

    private suspend fun proceedWithOrderId(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<TangemPayCustomerInfoError, MainScreenCustomerInfo> {
        return customerOrderRepository.getOrderStatus(userWalletId, orderId = orderId)
            .fold(
                ifLeft = { error ->
                    error.mapErrorForCustomer().left()
                },
                ifRight = { orderStatus ->
                    when (orderStatus) {
                        // Kyc is passed and user waits for order creation -> no need to get customer info
                        OrderStatus.NEW,
                        OrderStatus.PROCESSING,
                        -> MainScreenCustomerInfo(
                            info = CustomerInfo(
                                customerId = null,
                                productInstance = null,
                                kycStatus = CustomerInfo.KycStatus.APPROVED,
                                cardInfo = null,
                            ),
                            orderStatus = orderStatus,
                        ).right()

                        // Order was created/cancelled -> clear order id and get customer info
                        OrderStatus.COMPLETED,
                        OrderStatus.CANCELED,
                        OrderStatus.UNKNOWN,
                        -> {
                            onboardingRepository.clearOrderId(userWalletId)
                            // If order was cancelled -> start order creation
                            if (orderStatus == OrderStatus.CANCELED) onboardingRepository.createOrder(userWalletId)
                            onboardingRepository.getCustomerInfo(userWalletId = userWalletId)
                                .mapLeft { it.mapErrorForCustomer() }
                                .map { customerInfo ->
                                    MainScreenCustomerInfo(info = customerInfo, orderStatus = orderStatus)
                                }
                        }
                    }
                },
            )
    }

    private fun VisaApiError.mapErrorForCustomer(): TangemPayCustomerInfoError {
        return when (this) {
            is VisaApiError.RefreshTokenExpired -> TangemPayCustomerInfoError.RefreshNeededError
            is VisaApiError.NotPaeraCustomer -> TangemPayCustomerInfoError.UnknownError
            else -> TangemPayCustomerInfoError.UnavailableError
        }
    }
}