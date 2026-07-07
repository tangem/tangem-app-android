package com.tangem.data.pay.flow

import arrow.core.Either
import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.hasAccountData
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardLimitData
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayEntryPoint
import com.tangem.domain.pay.repository.*
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

private const val TAG = "PaymentAccountStatusFetcher"

/**
 * Reorders cards to match [previousOrder] (by [TangemPayCard.id]), appending any card absent from it at the
 * end while preserving the relative order among the new ones. Keeps the card layout stable when the backend
 * reorders `productInstances` (e.g. after a rename bumps `updated_at`). Returns the receiver unchanged when
 * [previousOrder] is empty (first load → backend order).
 */
internal fun List<TangemPayCard>.stableOrder(previousOrder: List<String>): List<TangemPayCard> {
    if (previousOrder.isEmpty()) return this
    val indexById = previousOrder.withIndex().associate { (index, id) -> id to index }
    return sortedBy { indexById[it.id] ?: Int.MAX_VALUE }
}

@Suppress("LongParameterList", "LargeClass")
internal class DefaultPaymentAccountStatusFetcher @Inject constructor(
    private val paymentAccountStatusesStore: PaymentAccountStatusesStore,
    private val onboardingRepository: OnboardingRepository,
    private val customerOrderRepository: CustomerOrderRepository,
    private val deviceSecurity: DeviceSecurityInfoProvider,
    private val dispatchers: CoroutineDispatcherProvider,
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory,
    private val eligibilityManager: TangemPayEligibilityManager,
    private val reissueCardRepository: TangemPayReissueCardRepository,
    private val singleQuoteSupplier: SingleQuoteStatusSupplier,
    private val closeCardRepository: TangemPayCloseCardRepository,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val issueCardRepository: TangemPayIssueCardRepository,
) : PaymentAccountStatusFetcher {

    private val logger = TangemLogger.withTag(TAG)

    override suspend fun invoke(params: PaymentAccountStatusFetcher.Params): Either<Throwable, Unit> =
        Either.catchOn(dispatchers.default) {
            val account = Account.Payment(userWalletId = params.userWalletId)
            logger.i("invoke() start: ${params.userWalletId.stringValue}")

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
        }.onLeft { throwable ->
            logger.e("invoke() ${params.userWalletId} threw, falling back to ONLY_CACHE", throwable)
            paymentAccountStatusesStore.updateStatusSource(
                userWalletId = params.userWalletId,
                source = StatusSource.ONLY_CACHE,
            )
        }.also { result ->
            logger.i("invoke() end ${params.userWalletId}: isRight=${result.isRight()}")
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
        logger.i(
            "fetchTangemPayAccountStatus ${account.userWalletId}: " +
                "prevResultType=${prevResult?.value?.let { it::class.simpleName } ?: "null"}",
        )
        if (prevResult == null || prevResult.value is PaymentAccountStatusValue.Error.Unavailable) {
            logger.i("fetchTangemPayAccountStatus ${account.userWalletId}: writing Loading placeholder to store")
            paymentAccountStatusesStore.store(
                userWalletId = account.userWalletId,
                status = AccountStatus.Payment(account = account, value = PaymentAccountStatusValue.Loading),
            )
        }

        return proceedWithOrderId(account = account)
    }

    private suspend fun proceedWithOrderId(account: Account.Payment): PaymentAccountStatusValue {
        val isInitial = onboardingRepository.isTangemPayInitialDataProduced(account.userWalletId)
        logger.i("proceedWithOrderId ${account.userWalletId}: isTangemPayInitialDataProduced=$isInitial")
        return if (!isInitial) {
            PaymentAccountStatusValue.Error.NotSynced
        } else {
            val orderId = onboardingRepository.getOrderId(account.userWalletId)
            logger.i("proceedWithOrderId ${account.userWalletId}: orderIdPresent=${orderId != null}")
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
                val cache = paymentAccountStatusesStore.getSyncOrNull(account.userWalletId)
                if (cache != null && cache.value.hasAccountData()) {
                    cache.value.copySealed(
                        source = StatusSource.ONLY_CACHE,
                        error = when (error) {
                            is VisaApiError.RefreshTokenExpired -> PaymentAccountStatusValue.Error.NotSynced
                            else -> PaymentAccountStatusValue.Error.Unavailable
                        },
                    )
                } else {
                    logger.e("proceedWithoutOrder ${account.userWalletId} error: $error")
                    error.mapToPaymentAccountStatus(account.userWalletId)
                }
            },
            ifRight = { customerInfo ->
                logger.i("proceedWithoutOrder data customerInfo ${account.userWalletId}")
                val status = customerInfo.mapToPaymentAccountStatus(account.userWalletId)
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
            -> return customerInfo.mapToPaymentAccountStatus(account.userWalletId)
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
                ifRight = { customerInfo -> customerInfo.mapToPaymentAccountStatus(account.userWalletId) },
            )
    }

    private suspend fun CustomerInfo.mapToPaymentAccountStatus(userWalletId: UserWalletId): PaymentAccountStatusValue {
        val quotesData = singleQuoteSupplier.getSyncOrNull(
            params = SingleQuoteStatusProducer.Params(rawCurrencyId = TangemPayCurrencyFactory.TOKEN_ID),
        )?.value as? QuoteStatus.Data

        val isDeactivated = productInstance?.status == CustomerInfo.ProductInstance.Status.DEACTIVATED
        val isFormer = state == CustomerInfo.State.FORMER
        val fiatBalance = fiatBalance
        val cryptoBalance = cryptoBalance

        return when {
            kycStatus != KycStatus.APPROVED && !customerId.isNullOrEmpty() -> {
                PaymentAccountStatusValue.UnderReview(
                    source = StatusSource.ACTUAL,
                    kycStatus = kycStatus,
                    customerId = requireNotNull(customerId) { "CustomerId must not be null" },
                )
            }
            fiatBalance != null && cryptoBalance != null && !customerId.isNullOrEmpty() &&
                (isDeactivated || isFormer) -> {
                PaymentAccountStatusValue.Deactivated(
                    source = StatusSource.ACTUAL,
                    customerId = requireNotNull(customerId) { "CustomerId must not be null" },
                    balance = PaymentAccountStatusValue.Balance(
                        fiatBalance = fiatBalance,
                        cryptoBalance = cryptoBalance,
                        availableForWithdrawal = availableForWithdrawal.orZero(),
                    ),
                    cryptoCurrency = tangemPayCurrencyFactory.create(userWalletId),
                    fiatRate = quotesData?.fiatRate,
                    error = null,
                )
            }
            cards.isNotEmpty() && productInstances.isNotEmpty() &&
                fiatBalance != null && cryptoBalance != null && !customerId.isNullOrEmpty() -> convertToContentState(
                userWalletId = userWalletId,
                fiatBalance = fiatBalance,
                cryptoBalance = cryptoBalance,
                fiatRate = quotesData?.fiatRate,
                customerId = requireNotNull(customerId) { "CustomerId must not be null" },
            )
            else -> PaymentAccountStatusValue.IssuingCard(source = StatusSource.ACTUAL)
        }
    }

    /**
     * Builds the [PaymentAccountStatusValue.Loaded] content state with the full list of cards.
     * Each card is the join of a product instance with its card info by `cardId`; balances are
     * payment-account-level (shared across cards). Falls back to [PaymentAccountStatusValue.IssuingCard]
     * when no card has both a product instance and card info yet (e.g. issuance in progress).
     */
    private suspend fun CustomerInfo.convertToContentState(
        userWalletId: UserWalletId,
        fiatBalance: PaymentAccountStatusValue.FiatBalance,
        cryptoBalance: PaymentAccountStatusValue.CryptoBalance,
        customerId: String,
        fiatRate: BigDecimal?,
    ): PaymentAccountStatusValue {
        val cardsById = cards.associateBy { it.cardId }
        val tangemPayCards = productInstances.mapNotNull { productInstance ->
            val cardInfo = cardsById[productInstance.cardId] ?: return@mapNotNull null
            val cardId = productInstance.cardId
            val cardFrozenState = cardDetailsRepository.cardFrozenStateSync(cardId)
            TangemPayCard(
                id = cardId,
                productInstanceId = productInstance.id,
                cardStatus = cardInfo.cardStatus,
                hasPinCode = cardInfo.isPinSet,
                displayName = productInstance.displayName,
                limit = TangemPayCardLimitData(
                    actualCardLimit = productInstance.actualCardLimit,
                    adminCardLimit = productInstance.adminCardLimit,
                ),
                frozenState = if (cardFrozenState == TangemPayCardFrozenState.Pending) {
                    TangemPayCardFrozenState.Pending
                } else {
                    productInstance.frozenState
                },
                lastDigits = cardInfo.lastFourDigits,
                state = getCardState(cardId, userWalletId),
            )
        }

        if (tangemPayCards.isEmpty()) return PaymentAccountStatusValue.IssuingCard(source = StatusSource.ACTUAL)

        // Additional-card issuance: the backend omits the new card until it is provisioned, so surface a
        // placeholder for every locally tracked in-flight issuance order alongside the real cards.
        val issuingCards = buildIssuingCards(userWalletId)

        // Keep the card order stable across refetches: the backend orders `productInstances` by a mutable
        // field (a rename bumps `updated_at`), which would otherwise make the renamed card jump. Anchor on
        // the previously shown order and append newly seen cards at the end.
        val orderedCards = tangemPayCards.stableOrder(previousRealCardOrder(userWalletId))

        return PaymentAccountStatusValue.Loaded(
            source = StatusSource.ACTUAL,
            customerId = customerId,
            depositAddress = cryptoBalance.depositAddress,
            cryptoCurrency = tangemPayCurrencyFactory.create(userWalletId),
            fiatRate = fiatRate,
            cards = orderedCards + issuingCards,
            balance = PaymentAccountStatusValue.Balance(
                fiatBalance = fiatBalance,
                cryptoBalance = cryptoBalance,
                availableForWithdrawal = availableForWithdrawal.orZero(),
            ),
            error = null,
        )
    }

    /**
     * Order of real (product-instance-backed) cards from the previously stored status, used as the stable
     * anchor for [stableOrder]. Issuing placeholders are excluded — they carry synthetic order ids and are
     * always appended last. Empty on the first load (no prior [PaymentAccountStatusValue.Loaded]), which makes
     * [stableOrder] fall back to the backend order.
     */
    private suspend fun previousRealCardOrder(userWalletId: UserWalletId): List<String> {
        val previousValue = paymentAccountStatusesStore.getSyncOrNull(userWalletId)?.value
        return (previousValue as? PaymentAccountStatusValue.Loaded)
            ?.cards
            ?.filterNot { it.state == TangemPayCardState.Issuing }
            ?.map { it.id }
            .orEmpty()
    }

    private suspend fun getCardState(cardId: String, userWalletId: UserWalletId): TangemPayCardState {
        val closingOrderId = closeCardRepository.getCloseOrderId(userWalletId, cardId).getOrNull()
        val reissueOrderId = reissueCardRepository.getReissueOrderId(userWalletId, cardId).getOrNull()
        return if (closingOrderId != null) {
            val order = cardDetailsRepository.getOrderInfo(userWalletId, closingOrderId).getOrNull()
            if (order != null && order.orderStatus.isTerminal) {
                closeCardRepository.setCloseOrderId(cardId, null)
                TangemPayCardState.Active
            } else {
                TangemPayCardState.Closing
            }
        } else if (reissueOrderId != null) {
            val order = cardDetailsRepository.getOrderInfo(userWalletId, reissueOrderId).getOrNull()
            if (order != null && order.orderStatus.isTerminal) {
                TangemPayCardState.Active
            } else {
                TangemPayCardState.Reissuing
            }
        } else {
            TangemPayCardState.Active
        }
    }

    /**
     * Builds the issuing placeholder cards from locally tracked additional-card orders. Each order is
     * re-checked against the backend; terminal orders are dropped (and forgotten) because the real card
     * is now part of [CustomerInfo], while in-flight orders surface as an issuing placeholder card.
     */
    private suspend fun buildIssuingCards(userWalletId: UserWalletId): List<TangemPayCard> {
        val orderIds = issueCardRepository.getIssueOrderIds(userWalletId)
        return orderIds.mapNotNull { orderId ->
            val order = cardDetailsRepository.getOrderInfo(userWalletId, orderId).getOrNull()
            if (order != null && order.orderStatus.isTerminal) {
                issueCardRepository.removeIssueOrderId(userWalletId, orderId)
                null
            } else {
                issuingPlaceholderCard(orderId)
            }
        }
    }

    /** Placeholder card for an additional card that is still being issued (no backend card yet). */
    private fun issuingPlaceholderCard(orderId: String): TangemPayCard = TangemPayCard(
        id = orderId,
        productInstanceId = orderId,
        cardStatus = TangemPayCard.Status.INACTIVE,
        hasPinCode = false,
        displayName = null,
        limit = null,
        frozenState = TangemPayCardFrozenState.Unfrozen,
        lastDigits = "",
        state = TangemPayCardState.Issuing,
    )

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