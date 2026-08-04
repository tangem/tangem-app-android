package com.tangem.data.pay.flow

import arrow.core.Either
import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.*
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.*
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance.SpecificationDataType
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayEntryPoint
import com.tangem.domain.pay.repository.*
import com.tangem.domain.pay.usecase.GetTangemPayTariffPlanStateUseCase
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.virtualaccount.VirtualAccountFeatureToggles
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
    private val virtualAccountFeatureToggles: VirtualAccountFeatureToggles,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val getTangemPayTariffPlanStateUseCase: GetTangemPayTariffPlanStateUseCase,
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
                        error.toStatusValueWhenTangemPayStatusUnknown(params.userWalletId)
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

    override suspend fun markVirtualAccountProcessing(userWalletId: UserWalletId) {
        paymentAccountStatusesStore.markVirtualAccountProcessing(userWalletId)
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
                        error = error.toErrorValue(),
                    )
                } else {
                    logger.e("proceedWithoutOrder ${account.userWalletId} error: $error")
                    error.toStatusValueWhenHasTangemPay(account.userWalletId)
                }
            },
            ifRight = { customerInfo ->
                logger.i("proceedWithoutOrder data customerInfo ${account.userWalletId}")
                resolveFinalStatus(
                    account = account,
                    customerInfo = customerInfo,
                    status = customerInfo.mapToPaymentAccountStatus(account.userWalletId),
                )
            },
        )
    }

    private suspend fun resolveFinalStatus(
        account: Account.Payment,
        customerInfo: CustomerInfo,
        status: PaymentAccountStatusValue,
    ): PaymentAccountStatusValue {
        val isIssuing = status is PaymentAccountStatusValue.IssuingCard
        val isApproved = customerInfo.kycStatus == KycStatus.APPROVED
        val userWalletId = account.userWalletId
        val tariffPlan = customerInfo.tariffPlan

        if (!tangemPayFeatureToggles.isTiersPlusPlanEnabled) {
            if (isIssuing && isApproved) {
                // If order id wasn't saved -> start order creation and get customer info
                onboardingRepository.createOrder(userWalletId)
                    .onLeft { logger.e("createOrder failed: $it") }
            }
            return status
        }

        if (!isIssuing || !isApproved) {
            return status
        }

        if (tariffPlan == null) {
            return PaymentAccountStatusValue.IssuingCard(source = StatusSource.ACTUAL)
        }

        val hasActiveIssueOrder = issueCardRepository.getIssueOrderIds(userWalletId).isNotEmpty()
        return if (hasActiveIssueOrder) {
            PaymentAccountStatusValue.Inactive(
                source = StatusSource.ACTUAL,
                tariffPlan = getTangemPayTariffPlanStateUseCase(
                    userWalletId = userWalletId,
                    tariff = tariffPlan,
                ),
                fiatBalance = PaymentAccountStatusValue.FiatBalance(
                    availableBalance = BigDecimal.ZERO,
                    currency = tariffPlan.plan.feeCurrencyOrDefault(),
                ),
            )
        } else {
            PaymentAccountStatusValue.AwaitingPlanSelection(
                source = StatusSource.ACTUAL,
                tariffPlan = tariffPlan,
            )
        }
    }

    private suspend fun proceedWithOrderId(account: Account.Payment, orderId: String): PaymentAccountStatusValue {
        // Step 1: Check KYC status first
        val customerInfo = onboardingRepository.getCustomerInfo(account.userWalletId).fold(
            ifLeft = { error ->
                logger.e("proceedWithOrderId KYC check ${account.userWalletId} error: $error")
                return error.toStatusValueWhenHasTangemPay(account.userWalletId)
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
                if (error is VisaApiError.OrderNotFound) {
                    handleOrderNotFound(account = account)
                } else {
                    error.toStatusValueWhenHasTangemPay(account.userWalletId)
                }
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
                    if (error is VisaApiError.OrderNotFound) {
                        return handleOrderNotFound(account = account)
                    }
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

    private suspend fun handleOrderNotFound(account: Account.Payment): PaymentAccountStatusValue {
        onboardingRepository.clearOrderId(account.userWalletId)
        return proceedWithoutOrder(account = account)
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
                ifLeft = { it.toStatusValueWhenHasTangemPay(account.userWalletId) },
                ifRight = { customerInfo -> customerInfo.mapToPaymentAccountStatus(account.userWalletId) },
            )
    }

    private suspend fun CustomerInfo.mapToPaymentAccountStatus(userWalletId: UserWalletId): PaymentAccountStatusValue {
        val quotesData = singleQuoteSupplier.getSyncOrNull(
            params = SingleQuoteStatusProducer.Params(rawCurrencyId = TangemPayCurrencyFactory.TOKEN_ID),
        )?.value as? QuoteStatus.Data

        val customerId = customerId
        val isDeactivated = productInstance?.status == CustomerInfo.ProductInstance.Status.DEACTIVATED
        val isFormer = state == CustomerInfo.State.FORMER
        val fiatBalance = fiatBalance
        val cryptoBalance = cryptoBalance
        val hasCardData = cards.isNotEmpty() && productInstances.isNotEmpty()
        val isTiersPlusPlanEnabled = tangemPayFeatureToggles.isTiersPlusPlanEnabled
        return when {
            customerId.isNullOrEmpty() -> PaymentAccountStatusValue.IssuingCard(
                source = StatusSource.ACTUAL,
            )
            kycStatus != KycStatus.APPROVED -> PaymentAccountStatusValue.UnderReview(
                source = StatusSource.ACTUAL,
                kycStatus = kycStatus,
                customerId = customerId,
            )
            fiatBalance != null && cryptoBalance != null && (isDeactivated || isFormer) ->
                PaymentAccountStatusValue.Deactivated(
                    source = StatusSource.ACTUAL,
                    customerId = customerId,
                    balance = PaymentAccountStatusValue.Balance(
                        fiatBalance = fiatBalance,
                        cryptoBalance = cryptoBalance,
                        availableForWithdrawal = availableForWithdrawal.orZero(),
                    ),
                    cryptoCurrency = tangemPayCurrencyFactory.create(userWalletId),
                    fiatRate = quotesData?.fiatRate,
                    error = null,
                )
            fiatBalance != null && cryptoBalance != null && (hasCardData || isTiersPlusPlanEnabled) ->
                convertToContentState(
                    userWalletId = userWalletId,
                    fiatBalance = fiatBalance,
                    cryptoBalance = cryptoBalance,
                    fiatRate = quotesData?.fiatRate,
                    customerId = customerId,
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
        val tangemPayCards = cardProductInstances.mapNotNull { productInstance ->
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
                images = cardInfo.images,
                state = getCardState(cardId, userWalletId),
            )
        }

        if (!tangemPayFeatureToggles.isTiersPlusPlanEnabled && tangemPayCards.isEmpty()) {
            return PaymentAccountStatusValue.IssuingCard(source = StatusSource.ACTUAL)
        }

        // Additional-card issuance: the backend omits the new card until it is provisioned, so surface a
        // placeholder for every locally tracked in-flight issuance order alongside the real cards.
        val issuingCards = buildIssuingCards(userWalletId)

        // Keep the card order stable across refetches: the backend orders `productInstances` by a mutable
        // field (a rename bumps `updated_at`), which would otherwise make the renamed card jump. Anchor on
        // the previously shown order and append newly seen cards at the end.
        val orderedCards = tangemPayCards.stableOrder(previousRealCardOrder(userWalletId))

        val allCards = orderedCards + issuingCards

        if (allCards.isEmpty()) {
            return PaymentAccountStatusValue.IssuingCard(source = StatusSource.ACTUAL)
        }

        val virtualAccount = resolveVirtualAccountOnramp(userWalletId)

        return PaymentAccountStatusValue.Loaded(
            source = StatusSource.ACTUAL,
            customerId = customerId,
            depositAddress = cryptoBalance.depositAddress,
            cryptoCurrency = tangemPayCurrencyFactory.create(userWalletId),
            fiatRate = fiatRate,
            cards = allCards,
            balance = PaymentAccountStatusValue.Balance(
                fiatBalance = fiatBalance,
                cryptoBalance = cryptoBalance,
                availableForWithdrawal = availableForWithdrawal.orZero(),
            ),
            error = null,
            virtualAccount = virtualAccount,
            tariffPlan = tariffPlan?.let { tariff ->
                getTangemPayTariffPlanStateUseCase(
                    userWalletId = userWalletId,
                    tariff = tariff,
                )
            },
        )
    }

    /**
     * Resolves the Virtual Account on-ramp dimension (VA MVP0, TWI-1638). Gated by the feature toggle.
     *
     * Resolution order:
     * 1. A product instance with [SpecificationDataType.ACCOUNT] exists — clears any stale persisted VA order id
     *    (idempotent) and eagerly fetches its bank credentials ([VirtualAccountOnramp.Available], or
     *    [VirtualAccountOnramp.BankCredentialsError] on failure).
     * 2. Otherwise, a VA order id is persisted locally — checks its status via `getOrderData`:
     *    NEW/PROCESSING/COMPLETED (or a transient lookup failure) surface [VirtualAccountOnramp.Processing]; CANCELED
     *    or a [VisaApiError.OrderNotFound] (the persisted id went stale) clears the persisted id and falls through
     *    to eligibility.
     * 3. Otherwise (or after a CANCELED order) — surfaces [VirtualAccountOnramp.Eligible] when the wallet has
     *    the `VISA_VIRTUAL_ACCOUNT` eligibility channel (fetched fresh via the user token), else `null`.
     */
    private suspend fun CustomerInfo.resolveVirtualAccountOnramp(userWalletId: UserWalletId): VirtualAccountOnramp? {
        if (!virtualAccountFeatureToggles.isVaMvp0Enabled) return null

        val accountInstance = productInstances.firstOrNull {
            it.specificationDataType == SpecificationDataType.ACCOUNT
        }
        if (accountInstance != null) {
            // Order provisioned into an ACCOUNT product instance — drop the in-flight order hint (idempotent).
            onboardingRepository.clearVirtualAccountOrderId(userWalletId)
            return onboardingRepository.getBankCredentials(userWalletId, accountInstance.id).fold(
                ifLeft = { error ->
                    logger.e("getBankCredentials failed for ${accountInstance.id}: $error")
                    VirtualAccountOnramp.BankCredentialsError
                },
                ifRight = { credentials ->
                    VirtualAccountOnramp.Available(
                        productInstanceId = accountInstance.id,
                        bankCredentials = credentials,
                    )
                },
            )
        }

        val vaOrderId = onboardingRepository.getVirtualAccountOrderId(userWalletId)
        if (vaOrderId != null) {
            return customerOrderRepository.getOrderData(userWalletId = userWalletId, orderId = vaOrderId).fold(
                ifLeft = { error ->
                    logger.e("getOrderData(va) failed for $vaOrderId: $error")
                    if (error is VisaApiError.OrderNotFound) {
                        onboardingRepository.clearVirtualAccountOrderId(userWalletId)
                        resolveEligibility(userWalletId)
                    } else {
                        VirtualAccountOnramp.Processing
                    }
                },
                ifRight = { orderData ->
                    when (orderData.status) {
                        OrderStatus.CANCELED -> {
                            onboardingRepository.clearVirtualAccountOrderId(userWalletId)
                            resolveEligibility(userWalletId)
                        }
                        OrderStatus.NEW,
                        OrderStatus.PROCESSING,
                        OrderStatus.COMPLETED,
                        -> VirtualAccountOnramp.Processing
                    }
                },
            )
        }

        return resolveEligibility(userWalletId)
    }

    private suspend fun resolveEligibility(userWalletId: UserWalletId): VirtualAccountOnramp? {
        return onboardingRepository.fetchCustomerEligibility(userWalletId).fold(
            ifLeft = { error ->
                logger.e("fetchCustomerEligibility failed for $userWalletId: $error")
                null
            },
            ifRight = { channels ->
                if (channels.contains(TangemPayEligibilityType.VISA_VIRTUAL_ACCOUNT)) {
                    VirtualAccountOnramp.Eligible
                } else {
                    null
                }
            },
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
                closeCardRepository.removeCloseOrderId(cardId)
                TangemPayCardState.Active
            } else {
                TangemPayCardState.Closing
            }
        } else if (reissueOrderId != null) {
            val order = cardDetailsRepository.getOrderInfo(userWalletId, reissueOrderId).getOrNull()
            if (order != null && order.orderStatus.isTerminal) {
                reissueCardRepository.removeReissueOrderId(cardId)
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
            cardDetailsRepository.getOrderInfo(userWalletId, orderId).fold(
                ifLeft = { error ->
                    if (error == VisaApiError.OrderNotFound) {
                        logger.i("buildIssuingCards $userWalletId: dropping missing order $orderId")
                        issueCardRepository.removeIssueOrderId(userWalletId, orderId)
                        null
                    } else {
                        issuingPlaceholderCard(orderId)
                    }
                },
                ifRight = { order ->
                    if (order.orderStatus.isTerminal) {
                        issueCardRepository.removeIssueOrderId(userWalletId, orderId)
                        null
                    } else {
                        issuingPlaceholderCard(orderId)
                    }
                },
            )
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
        images = emptyList(),
        state = TangemPayCardState.Issuing,
    )

    private suspend fun VisaApiError.toStatusValueWhenHasTangemPay(
        userWalletId: UserWalletId,
    ): PaymentAccountStatusValue {
        return when (this) {
            is VisaApiError.NotFound -> constructNotCreatedOrEmptyStatus(userWalletId)
            else -> toErrorValue()
        }
    }

    private suspend fun VisaApiError.toStatusValueWhenTangemPayStatusUnknown(
        userWalletId: UserWalletId,
    ): PaymentAccountStatusValue {
        return when (this) {
            is VisaApiError.NotFound -> constructNotCreatedOrEmptyStatus(userWalletId)
            else -> {
                val previousValue = paymentAccountStatusesStore.getSyncOrNull(userWalletId)?.value
                if (previousValue != null && previousValue.hasAccountData()) {
                    previousValue.copySealed(
                        source = StatusSource.ONLY_CACHE,
                        error = toErrorValue(),
                    )
                } else {
                    constructNotCreatedOrEmptyStatus(userWalletId)
                }
            }
        }
    }

    private fun VisaApiError.toErrorValue(): PaymentAccountStatusValue.Error = when (this) {
        is VisaApiError.RefreshTokenExpired -> PaymentAccountStatusValue.Error.NotSynced
        else -> PaymentAccountStatusValue.Error.Unavailable
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