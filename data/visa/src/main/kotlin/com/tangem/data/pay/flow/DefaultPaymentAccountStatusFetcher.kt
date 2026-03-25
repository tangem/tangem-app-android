package com.tangem.data.pay.flow

import arrow.core.Either
import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.domain.core.utils.eitherOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.PaymentAccountStatus
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "PaymentAccountStatusFetcher"

internal class DefaultPaymentAccountStatusFetcher @Inject constructor(
    private val paymentAccountStatusesStore: PaymentAccountStatusesStore,
    private val onboardingRepository: OnboardingRepository,
    private val customerOrderRepository: CustomerOrderRepository,
    private val deviceSecurity: DeviceSecurityInfoProvider,
    private val dispatchers: CoroutineDispatcherProvider,
) : PaymentAccountStatusFetcher {

    override suspend fun invoke(params: PaymentAccountStatusFetcher.Params): Either<Throwable, Unit> =
        eitherOn(dispatchers.default) {
            Timber.tag(TAG).i("fetch: ${params.userWalletId.stringValue}")

            if (deviceSecurity.isSecurityExposed()) {
                Timber.tag(TAG).i("fetch security info: rooted: ${deviceSecurity.isRooted}")
                Timber.tag(TAG).i("fetch security info: xposed: ${deviceSecurity.isXposed}")
                Timber.tag(TAG).i("fetch security info: bootloader unlocked: ${deviceSecurity.isBootloaderUnlocked}")

                return@eitherOn paymentAccountStatusesStore.store(
                    userWalletId = params.userWalletId,
                    status = PaymentAccountStatus.Error.ExposedDevice,
                )
            }

            val status = onboardingRepository.hasTangemPayInWallet(userWalletId = params.userWalletId)
                .fold(
                    ifLeft = { error ->
                        Timber.tag(TAG).e("Failed check wallet ${params.userWalletId}: ${error.javaClass.simpleName}")
                        when (error) {
                            is VisaApiError.NotPaeraCustomer -> PaymentAccountStatus.NotCreated
                            else -> PaymentAccountStatus.Error.Unavailable(source = StatusSource.ACTUAL)
                        }
                    },
                    ifRight = { hasTangemPay ->
                        proceedHasTangemPayResult(userWalletId = params.userWalletId, hasTangemPay = hasTangemPay)
                    },
                )
            Timber.tag(TAG).i("invoke status ${params.userWalletId}: $status")
            paymentAccountStatusesStore.store(userWalletId = params.userWalletId, status = status)
        }

    private suspend fun proceedHasTangemPayResult(
        userWalletId: UserWalletId,
        hasTangemPay: Boolean,
    ): PaymentAccountStatus {
        Timber.tag(TAG).i("proceedHasTangemPayResult for $userWalletId hasTangemPay: $hasTangemPay")
        return if (hasTangemPay) {
            fetchTangemPayAccountStatus(userWalletId = userWalletId)
        } else {
            PaymentAccountStatus.NotCreated
        }
    }

    private suspend fun fetchTangemPayAccountStatus(userWalletId: UserWalletId): PaymentAccountStatus {
        val prevResult = paymentAccountStatusesStore.getSyncOrNull(userWalletId)
        if (prevResult == null || prevResult is PaymentAccountStatus.Error) {
            paymentAccountStatusesStore.store(userWalletId = userWalletId, status = PaymentAccountStatus.Loading)
        }

        return proceedWithOrderId(userWalletId = userWalletId)
    }

    private suspend fun proceedWithOrderId(userWalletId: UserWalletId): PaymentAccountStatus {
        return if (!onboardingRepository.isTangemPayInitialDataProduced(userWalletId)) {
            PaymentAccountStatus.Error.NotSynced
        } else {
            val orderId = onboardingRepository.getOrderId(userWalletId)
            if (orderId != null) {
                proceedWithOrderId(userWalletId = userWalletId, orderId = orderId)
            } else {
                proceedWithoutOrder(userWalletId = userWalletId)
            }
        }
    }

    private suspend fun proceedWithoutOrder(userWalletId: UserWalletId): PaymentAccountStatus {
        return onboardingRepository.getCustomerInfo(userWalletId).fold(
            ifLeft = { error ->
                Timber.tag(TAG).e("proceedWithoutOrder $userWalletId error: $error")
                error.mapToPaymentAccountStatus()
            },
            ifRight = { customerInfo ->
                Timber.tag(TAG).i("proceedWithoutOrder data customerInfo $userWalletId")
                val status = customerInfo.mapToPaymentAccountStatus()
                if (customerInfo.productInstance == null) {
                    onboardingRepository.createOrder(userWalletId)
                        .onLeft { Timber.tag(TAG).e("createOrder failed: $it") }
                }
                status
            },
        )
    }

    private suspend fun proceedWithOrderId(userWalletId: UserWalletId, orderId: String): PaymentAccountStatus {
        return customerOrderRepository.getOrderData(userWalletId, orderId = orderId).fold(
            ifLeft = { error ->
                Timber.tag(TAG).e("proceedWithOrderId $userWalletId orderId: $orderId error: $error")
                error.mapToPaymentAccountStatus()
            },
            ifRight = { orderData ->
                Timber.tag(TAG).i("proceedWithOrderId $userWalletId: $orderId status: ${orderData.status}")
                when (orderData.status) {
                    // Kyc is passed and user waits for order creation -> no need to get customer info
                    OrderStatus.NEW,
                    OrderStatus.PROCESSING,
                    -> PaymentAccountStatus.IssuingCard(source = StatusSource.ACTUAL)

                    OrderStatus.CANCELED -> {
                        PaymentAccountStatus.Error.CardIssueFailed
                    }
                    OrderStatus.COMPLETED -> {
                        // Order was completed -> clear order id and get customer info
                        onboardingRepository.clearOrderId(userWalletId)
                        onboardingRepository.getCustomerInfo(userWalletId = userWalletId)
                            .fold(
                                ifLeft = { it.mapToPaymentAccountStatus() },
                                ifRight = { customerInfo -> customerInfo.mapToPaymentAccountStatus() },
                            )
                    }
                    OrderStatus.UNKNOWN -> PaymentAccountStatus.Error.Unavailable(source = StatusSource.ACTUAL)
                }
            },
        )
    }

    private fun CustomerInfo.mapToPaymentAccountStatus(): PaymentAccountStatus {
        val cardInfo = this.cardInfo
        val productInstance = this.productInstance
        return if (kycStatus != KycStatus.APPROVED && !customerId.isNullOrEmpty()) {
            PaymentAccountStatus.UnderReview(source = StatusSource.ACTUAL, kycStatus = kycStatus)
        } else if (cardInfo != null && productInstance != null) {
            PaymentAccountStatus.Loaded(
                source = StatusSource.ACTUAL,
                cardId = productInstance.cardId,
                lastFourDigits = cardInfo.lastFourDigits,
                balance = cardInfo.balance,
                currencyCode = cardInfo.currencyCode,
                depositAddress = cardInfo.depositAddress,
                isPinSet = cardInfo.isPinSet,
            )
        } else {
            PaymentAccountStatus.IssuingCard(source = StatusSource.ACTUAL)
        }
    }

    private fun VisaApiError.mapToPaymentAccountStatus(): PaymentAccountStatus {
        return when (this) {
            is VisaApiError.RefreshTokenExpired -> PaymentAccountStatus.Error.NotSynced
            is VisaApiError.NotPaeraCustomer -> PaymentAccountStatus.NotCreated
            else -> PaymentAccountStatus.Error.Unavailable(source = StatusSource.ACTUAL)
        }
    }
}