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
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.*

class TangemPayMainScreenCustomerInfoUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val customerOrderRepository: CustomerOrderRepository,
    private val eligibilityManager: TangemPayEligibilityManager,
    private val deviceSecurity: DeviceSecurityInfoProvider,
) {

    val state: StateFlow<Map<UserWalletId, Either<TangemPayCustomerInfoError, MainCustomerInfoContentState>>>
        field = MutableStateFlow(value = mapOf())

    private val logger = TangemLogger.withTag("TangemPayMainScreenCustomerInfoUseCase")

    suspend fun fetch(userWalletId: UserWalletId) {
        logger.i("fetch: ${userWalletId.stringValue}")

        if (onboardingRepository.isTangemPayDeactivated(userWalletId)) {
            updateState(userWalletId, MainCustomerInfoContentState.Empty.right())
            return
        }

        if (deviceSecurity.isSecurityExposed()) {
            logger.i("fetch security info: rooted: ${deviceSecurity.isRooted}")
            logger.i("fetch security info: xposed: ${deviceSecurity.isXposed}")
            logger.i("fetch security info: bootloader unlocked: ${deviceSecurity.isBootloaderUnlocked}")

            updateState(userWalletId = userWalletId, either = TangemPayCustomerInfoError.ExposedDeviceError.left())
            return // fast exit
        }

        onboardingRepository.hasTangemPayInWallet(userWalletId)
            .fold(
                ifLeft = { error ->
                    logger.e("Failed checkCustomerWallet for $userWalletId: ${error.javaClass.simpleName}")
                    if (error is VisaApiError.NotPaeraCustomer) {
                        showOnboardingBannerIfEligible(userWalletId)
                    } else {
                        updateState(userWalletId, TangemPayCustomerInfoError.UnknownError.left())
                    }
                },
                ifRight = { hasTangemPay ->
                    logger.i("checkCustomerWallet for $userWalletId: $hasTangemPay")
                    if (hasTangemPay) {
                        val oldResult = state.value[userWalletId]
                        if (oldResult == null) {
                            updateState(userWalletId, MainCustomerInfoContentState.Loading.right())
                        }

                        val result = proceedWithPaeraCustomerResult(userWalletId)
                        updateState(userWalletId, result.map(MainCustomerInfoContentState::Content))
                    } else {
                        // if there's no tangem pay, check eligibility and show onboarding banner
                        showOnboardingBannerIfEligible(userWalletId)
                    }
                },
            )
    }

    private suspend fun showOnboardingBannerIfEligible(userWalletId: UserWalletId) {
        val tangemPayEntryPoint = TangemPayEntryPoint.BANNER
        if (eligibilityManager.isPaeraCustomerForAnyWallet(tangemPayEntryPoint)) {
            updateState(userWalletId, MainCustomerInfoContentState.Empty.right())
            return
        }
        val isEligible = eligibilityManager
            .getEligibleWallets(
                shouldExcludePaeraCustomers = false,
                entryPoint = tangemPayEntryPoint,
            )
            .any { it.walletId == userWalletId }
        if (isEligible) {
            if (onboardingRepository.getHideMainOnboardingBanner(userWalletId)) {
                updateState(userWalletId, MainCustomerInfoContentState.Empty.right())
            } else {
                updateState(userWalletId, MainCustomerInfoContentState.OnboardingBanner.right())
            }
        } else {
            updateState(userWalletId, MainCustomerInfoContentState.Empty.right())
        }
    }

    operator fun invoke(
        userWalletId: UserWalletId,
    ): Flow<Either<TangemPayCustomerInfoError, MainCustomerInfoContentState>> {
        logger.i("invoke($userWalletId): flow requested")
        return state.mapNotNull { map -> map[userWalletId] }
            .onStart { logger.i("invoke($userWalletId): flow subscribed (current state size=${state.value.size})") }
            .onEach { value ->
                logger.i(
                    "invoke($userWalletId): emit ${value.fold(
                        { "Left(${it.javaClass.simpleName})" },
                        { "Right(${it.javaClass.simpleName})" },
                    )}",
                )
            }
    }

    private fun updateState(
        userWalletId: UserWalletId,
        either: Either<TangemPayCustomerInfoError, MainCustomerInfoContentState>,
    ) {
        logger.i(
            "updateState($userWalletId) -> ${either.fold(
                { "Left(${it.javaClass.simpleName})" },
                { "Right(${it.javaClass.simpleName})" },
            )}",
        )
        state.update { currentMap ->
            currentMap.toMutableMap().apply { this[userWalletId] = either }
        }
    }

    private suspend fun proceedWithPaeraCustomerResult(
        userWalletId: UserWalletId,
    ): Either<TangemPayCustomerInfoError, MainScreenCustomerInfo> {
        logger.i("proceedWithPaeraCustomerResult($userWalletId) entry")
        val isInitial = onboardingRepository.isTangemPayInitialDataProduced(userWalletId)
        logger.i("proceedWithPaeraCustomerResult($userWalletId): isTangemPayInitialDataProduced=$isInitial")
        if (!isInitial) {
            return TangemPayCustomerInfoError.RefreshNeededError.left()
        }
        val orderId = onboardingRepository.getOrderId(userWalletId)
        logger.i("proceedWithPaeraCustomerResult($userWalletId): orderId=$orderId")
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
                logger.e("mapErrorForCustomer: $error")
                error.mapErrorForCustomer()
            }
            .map { customerInfo ->
                logger.i("customerInfo")
                if (customerInfo.productInstance == null) {
                    onboardingRepository.createOrder(userWalletId)
                    MainScreenCustomerInfo(info = customerInfo, orderStatus = OrderStatus.NEW)
                } else {
                    MainScreenCustomerInfo(info = customerInfo, orderStatus = OrderStatus.COMPLETED)
                }
            }
    }

    private suspend fun proceedWithOrderId(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<TangemPayCustomerInfoError, MainScreenCustomerInfo> {
        logger.i("proceedWithOrderId($userWalletId, orderId=$orderId) entry")
        return customerOrderRepository.getOrderData(userWalletId, orderId = orderId)
            .fold(
                ifLeft = { error ->
                    logger.e("proceedWithOrderId($userWalletId): getOrderData failed: ${error.javaClass.simpleName}")
                    error.mapErrorForCustomer().left()
                },
                ifRight = { orderData ->
                    logger.i("proceedWithOrderId($userWalletId): orderData.status=${orderData.status}")
                    if (orderData.status in setOf(OrderStatus.COMPLETED, OrderStatus.UNKNOWN)) {
                        onboardingRepository.clearOrderId(userWalletId)
                    }
                    onboardingRepository.getCustomerInfo(userWalletId = userWalletId)
                        .mapLeft { error ->
                            logger.e("proceedWithOrderId($userWalletId): getCustomerInfo failed: $error")
                            error.mapErrorForCustomer()
                        }
                        .map { customerInfo ->
                            logger.i("proceedWithOrderId($userWalletId): got customerInfo")
                            MainScreenCustomerInfo(info = customerInfo, orderStatus = orderData.status)
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