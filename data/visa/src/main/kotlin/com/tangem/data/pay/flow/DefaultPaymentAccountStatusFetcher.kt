package com.tangem.data.pay.flow

import arrow.core.Either
import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayEntryPoint
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

private const val TAG = "PaymentAccountStatusFetcher"

@Suppress("LongParameterList")
internal class DefaultPaymentAccountStatusFetcher @Inject constructor(
    private val paymentAccountStatusesStore: PaymentAccountStatusesStore,
    private val onboardingRepository: OnboardingRepository,
    private val customerOrderRepository: CustomerOrderRepository,
    private val deviceSecurity: DeviceSecurityInfoProvider,
    private val dispatchers: CoroutineDispatcherProvider,
    private val eligibilityManager: TangemPayEligibilityManager,
) : PaymentAccountStatusFetcher {

    private val logger = TangemLogger.withTag(TAG)

    override suspend fun invoke(params: PaymentAccountStatusFetcher.Params): Either<Throwable, Unit> =
        Either.catchOn(dispatchers.default) {
            val account = Account.Payment(userWalletId = params.userWalletId)
            logger.i("fetch: ${params.userWalletId.stringValue}")

            if (deviceSecurity.isSecurityExposed()) {
                logger.i("fetch security info: rooted: ${deviceSecurity.isRooted}")
                logger.i("fetch security info: xposed: ${deviceSecurity.isXposed}")
                logger.i("fetch security info: bootloader unlocked: ${deviceSecurity.isBootloaderUnlocked}")

                return@catchOn paymentAccountStatusesStore.store(
                    userWalletId = params.userWalletId,
                    status = AccountStatus.Payment(
                        account = account,
                        value = PaymentAccountStatusValue.Error.ExposedDevice,
                    ),
                )
            }

            val status = onboardingRepository.hasTangemPayInWallet(userWalletId = params.userWalletId)
                .fold(
                    ifLeft = { error ->
                        logger.e("Failed check wallet ${params.userWalletId}: ${error.javaClass.simpleName}")
                        when (error) {
                            is VisaApiError.NotPaeraCustomer -> constructNotCreatedOrEmptyStatus(params.userWalletId)
                            else -> PaymentAccountStatusValue.Error.Unavailable
                        }
                    },
                    ifRight = { hasTangemPay ->
                        proceedHasTangemPayResult(account = account, hasTangemPay = hasTangemPay)
                    },
                )
            logger.i("invoke status ${params.userWalletId}: $status")
            paymentAccountStatusesStore.store(
                userWalletId = params.userWalletId,
                status = AccountStatus.Payment(account = account, value = status),
            )
        }.onLeft {
            paymentAccountStatusesStore.updateStatusSource(
                userWalletId = params.userWalletId,
                source = StatusSource.ONLY_CACHE,
            )
        }

    private suspend fun proceedHasTangemPayResult(
        account: Account.Payment,
        hasTangemPay: Boolean,
    ): PaymentAccountStatusValue {
        logger.i("proceedHasTangemPayResult for ${account.userWalletId} hasTangemPay: $hasTangemPay")
        return if (hasTangemPay) {
            fetchTangemPayAccountStatus(account)
        } else {
            constructNotCreatedOrEmptyStatus(account.userWalletId)
        }
    }

    private suspend fun fetchTangemPayAccountStatus(account: Account.Payment): PaymentAccountStatusValue {
        val prevResult = paymentAccountStatusesStore.getSyncOrNull(account.userWalletId)
        if (prevResult == null || prevResult.value is PaymentAccountStatusValue.Error.Unavailable) {
            paymentAccountStatusesStore.store(
                userWalletId = account.userWalletId,
                status = AccountStatus.Payment(account = account, value = PaymentAccountStatusValue.Loading),
            )
        }

        return proceedWithOrderId(account = account)
    }

    private suspend fun proceedWithOrderId(account: Account.Payment): PaymentAccountStatusValue {
        return if (!onboardingRepository.isTangemPayInitialDataProduced(account.userWalletId)) {
            PaymentAccountStatusValue.Error.NotSynced
        } else {
            val orderId = onboardingRepository.getOrderId(account.userWalletId)
            if (orderId != null) {
                proceedWithOrderId(account = account, orderId = orderId)
            } else {
                proceedWithoutOrder(account = account)
            }
        }
    }

    private suspend fun proceedWithoutOrder(account: Account.Payment): PaymentAccountStatusValue {
        return onboardingRepository.getCustomerInfo(account.userWalletId).fold(
            ifLeft = { error ->
                logger.e("proceedWithoutOrder ${account.userWalletId} error: $error")
                error.mapToPaymentAccountStatus(account.userWalletId)
            },
            ifRight = { customerInfo ->
                logger.i("proceedWithoutOrder data customerInfo ${account.userWalletId}")
                val status = customerInfo.mapToPaymentAccountStatus()
                if (status is PaymentAccountStatusValue.IssuingCard && customerInfo.kycStatus == KycStatus.APPROVED) {
                    // If order id wasn't saved -> start order creation and get customer info
                    onboardingRepository.createOrder(account.userWalletId)
                        .onLeft { TangemLogger.withTag(TAG).e("createOrder failed: $it") }
                }
                status
            },
        )
    }

    private suspend fun proceedWithOrderId(account: Account.Payment, orderId: String): PaymentAccountStatusValue {
        // Step 1: Check KYC status first
        val customerInfo = onboardingRepository.getCustomerInfo(account.userWalletId).fold(
            ifLeft = { error ->
                logger.e("proceedWithOrderId KYC check ${account.userWalletId} error: $error")
                return error.mapToPaymentAccountStatus(account.userWalletId)
            },
            ifRight = { it },
        )

        logger.i("proceedWithOrderId ${account.userWalletId} kycStatus: ${customerInfo.kycStatus}")

        when (customerInfo.kycStatus) {
            KycStatus.PENDING,
            KycStatus.INIT,
            KycStatus.REJECTED,
            -> return customerInfo.mapToPaymentAccountStatus()
            KycStatus.APPROVED -> Unit // proceed to order check
        }

        // Step 2: Check order status
        return customerOrderRepository.getOrderData(userWalletId = account.userWalletId, orderId = orderId).fold(
            ifLeft = { error ->
                logger.e("proceedWithOrderId ${account.userWalletId} orderId: $orderId error: $error")
                error.mapToPaymentAccountStatus(account.userWalletId)
            },
            ifRight = { orderData ->
                logger.i("proceedWithOrderId ${account.userWalletId}: $orderId status: ${orderData.status}")
                when (orderData.status) {
                    OrderStatus.CANCELED -> handleCanceledOrder(account, orderData)
                    OrderStatus.COMPLETED -> handleCompletedOrder(account)
                    OrderStatus.UNKNOWN -> PaymentAccountStatusValue.Error.Unavailable
                    OrderStatus.NEW,
                    OrderStatus.PROCESSING,
                    -> {
                        paymentAccountStatusesStore.store(
                            userWalletId = account.userWalletId,
                            status = AccountStatus.Payment(
                                account = account,
                                value = PaymentAccountStatusValue.IssuingCard(source = StatusSource.ACTUAL),
                            ),
                        )
                        // Start polling for terminal state
                        pollOrderStatus(account = account, orderId = orderId)
                    }
                }
            },
        )
    }

    private suspend fun pollOrderStatus(account: Account.Payment, orderId: String): PaymentAccountStatusValue {
        while (currentCoroutineContext().isActive) {
            delay(1.minutes)

            val result = customerOrderRepository.getOrderData(
                userWalletId = account.userWalletId,
                orderId = orderId,
            )

            result.fold(
                ifLeft = { error ->
                    logger.e("pollOrderStatus ${account.userWalletId} orderId: $orderId error: $error")
                    // Continue polling on transient errors
                },
                ifRight = { orderData ->
                    logger.i("pollOrderStatus ${account.userWalletId}: $orderId status: ${orderData.status}")
                    when (orderData.status) {
                        OrderStatus.CANCELED -> return handleCanceledOrder(account, orderData)
                        OrderStatus.COMPLETED -> return handleCompletedOrder(account)
                        OrderStatus.NEW,
                        OrderStatus.PROCESSING,
                        OrderStatus.UNKNOWN,
                        -> Unit // Continue polling
                    }
                },
            )
        }

        return PaymentAccountStatusValue.IssuingCard(source = StatusSource.ACTUAL)
    }

    private suspend fun handleCanceledOrder(
        account: Account.Payment,
        orderData: OrderData,
    ): PaymentAccountStatusValue {
        onboardingRepository.clearOrderId(account.userWalletId)
        return PaymentAccountStatusValue.Error.CardIssueFailed(orderData.customerId)
    }

    private suspend fun handleCompletedOrder(account: Account.Payment): PaymentAccountStatusValue {
        onboardingRepository.clearOrderId(account.userWalletId)
        return onboardingRepository.getCustomerInfo(userWalletId = account.userWalletId)
            .fold(
                ifLeft = { it.mapToPaymentAccountStatus(account.userWalletId) },
                ifRight = { customerInfo -> customerInfo.mapToPaymentAccountStatus() },
            )
    }

    private fun CustomerInfo.mapToPaymentAccountStatus(): PaymentAccountStatusValue {
        val cardInfo = this.cardInfo
        val productInstance = this.productInstance

        val isDeactivated = productInstance?.status == CustomerInfo.ProductInstance.Status.DEACTIVATED
        val isFormer = state == CustomerInfo.State.FORMER
        val fiatBalance = fiatBalance

        return when {
            kycStatus != KycStatus.APPROVED && !customerId.isNullOrEmpty() -> {
                PaymentAccountStatusValue.UnderReview(
                    source = StatusSource.ACTUAL,
                    kycStatus = kycStatus,
                    customerId = requireNotNull(customerId) { "CustomerId must not be null" },
                )
            }
            fiatBalance != null && (isDeactivated || isFormer) -> {
                PaymentAccountStatusValue.Deactivated(
                    source = StatusSource.ACTUAL,
                    fiatBalance = fiatBalance,
                )
            }
            cardInfo != null && productInstance != null && !customerId.isNullOrEmpty() -> convertToContentState(
                productInstance = productInstance,
                cardInfo = cardInfo,
                customerId = requireNotNull(customerId) { "CustomerId must not be null" },
            )
            else -> PaymentAccountStatusValue.IssuingCard(source = StatusSource.ACTUAL)
        }
    }

    private fun convertToContentState(
        productInstance: CustomerInfo.ProductInstance,
        cardInfo: CustomerInfo.CardInfo,
        customerId: String,
    ): PaymentAccountStatusValue {
        return when (productInstance.frozenState) {
            TangemPayCardFrozenState.Frozen -> PaymentAccountStatusValue.Locked(
                source = StatusSource.ACTUAL,
                customerId = customerId,
                cardId = productInstance.cardId,
                lastFourDigits = cardInfo.lastFourDigits,
                currencyCode = cardInfo.currencyCode,
                depositAddress = cardInfo.depositAddress,
                isPinSet = cardInfo.isPinSet,
                fiatBalance = cardInfo.fiatBalance,
                cryptoBalance = cardInfo.cryptoBalance,
            )
            else -> PaymentAccountStatusValue.Loaded(
                source = StatusSource.ACTUAL,
                customerId = customerId,
                cardId = productInstance.cardId,
                lastFourDigits = cardInfo.lastFourDigits,
                currencyCode = cardInfo.currencyCode,
                depositAddress = cardInfo.depositAddress,
                isPinSet = cardInfo.isPinSet,
                fiatBalance = cardInfo.fiatBalance,
                cryptoBalance = cardInfo.cryptoBalance,
            )
        }
    }

    private suspend fun VisaApiError.mapToPaymentAccountStatus(userWalletId: UserWalletId): PaymentAccountStatusValue {
        return when (this) {
            is VisaApiError.RefreshTokenExpired -> PaymentAccountStatusValue.Error.NotSynced
            is VisaApiError.NotPaeraCustomer -> constructNotCreatedOrEmptyStatus(userWalletId)
            else -> PaymentAccountStatusValue.Error.Unavailable
        }
    }

    private suspend fun constructNotCreatedOrEmptyStatus(userWalletId: UserWalletId): PaymentAccountStatusValue {
        val entryPoint = TangemPayEntryPoint.BANNER
        val shouldShowBanner = !eligibilityManager.isPaeraCustomerForAnyWallet(entryPoint) &&
            eligibilityManager.getEligibleWallets(shouldExcludePaeraCustomers = false, entryPoint = entryPoint)
                .any { it.walletId == userWalletId } &&
            !onboardingRepository.getHideMainOnboardingBanner(userWalletId)

        return if (shouldShowBanner) PaymentAccountStatusValue.NotCreated else PaymentAccountStatusValue.Empty
    }
}