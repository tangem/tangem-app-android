package com.tangem.data.tangempay.flow

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.data.tangempay.store.PaymentAccountStatusesStore
import com.tangem.data.tangempay.util.PlasticCardStateResolver
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.*
import com.tangem.domain.models.tangempay.*
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayCurrencyFactory
import com.tangem.domain.tangempay.TangemPayEligibilityManager
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.model.CachePolicy
import com.tangem.domain.tangempay.model.CustomerInfo
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.OrderType
import com.tangem.domain.tangempay.model.PaymentAccountActivationState
import com.tangem.domain.tangempay.model.TangemPayCustomerState
import com.tangem.domain.tangempay.model.TangemPayEntryPoint
import com.tangem.domain.tangempay.repository.*
import com.tangem.domain.tangempay.usecase.GetCustomerSummaryUseCase
import com.tangem.domain.tangempay.usecase.GetTangemPayFiatRateUseCase
import com.tangem.domain.tangempay.usecase.GetTangemPayTariffPlanStateUseCase
import com.tangem.domain.tangempay.usecase.ResolvePaymentAccountStatusFromSummaryUseCase
import com.tangem.domain.tangempay.utils.TangemPayCustomerStateConverter
import com.tangem.domain.tangempay.utils.isKycAdditionalInfoRequested
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal
import javax.inject.Inject

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
    private val getTangemPayFiatRateUseCase: GetTangemPayFiatRateUseCase,
    private val closeCardRepository: TangemPayCloseCardRepository,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val issueCardRepository: TangemPayIssueCardRepository,
    private val getTangemPayTariffPlanStateUseCase: GetTangemPayTariffPlanStateUseCase,
    private val resolvePaymentAccountStatusFromSummaryUseCase: ResolvePaymentAccountStatusFromSummaryUseCase,
    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
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
                        logger.i("proceedHasTangemPayResult for ${account.userWalletId} hasTangemPay: $hasTangemPay")
                        if (hasTangemPay) {
                            fetchTangemPayAccountStatus(account)
                        } else {
                            constructNotCreatedOrEmptyStatus(account.userWalletId)
                        }
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

    override suspend fun markVirtualAccountEligible(userWalletId: UserWalletId) {
        paymentAccountStatusesStore.markVirtualAccountEligible(userWalletId)
    }

    override suspend fun markCardActivating(userWalletId: UserWalletId, productInstanceId: String) {
        paymentAccountStatusesStore.markCardActivating(userWalletId, productInstanceId)
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

        return proceed(account = account)
    }

    private suspend fun proceed(account: Account.Payment): PaymentAccountStatusValue {
        val isInitial = onboardingRepository.isTangemPayInitialDataProduced(account.userWalletId)
        logger.i("proceed ${account.userWalletId}: isTangemPayInitialDataProduced=$isInitial")
        if (!isInitial) {
            return PaymentAccountStatusValue.Error.NotSynced
        } else {
            val userWalletId = account.userWalletId
            val summary = getCustomerSummaryUseCase.invoke(userWalletId, CachePolicy.REFRESH).fold(
                ifLeft = { error ->
                    error.toCachedStatusOrNull(userWalletId) ?: error.toStatusValueWhenHasTangemPay(userWalletId)
                },
                ifRight = { summary ->
                    summary?.let { resolvePaymentAccountStatusFromSummaryUseCase(userWalletId, it) }
                },
            )
            return summary ?: proceedWithCustomerInfo(account = account)
        }
    }

    private suspend fun proceedWithCustomerInfo(account: Account.Payment): PaymentAccountStatusValue {
        return onboardingRepository.getCustomerInfo(account.userWalletId).fold(
            ifLeft = { error ->
                logger.e("proceedWithCustomerInfo ${account.userWalletId} error: $error")
                error.toCachedStatusOrNull(account.userWalletId)
                    ?: error.toStatusValueWhenHasTangemPay(account.userWalletId)
            },
            ifRight = { customerInfo ->
                logger.i("proceedWithCustomerInfo data customerInfo ${account.userWalletId}")
                customerInfo.mapToPaymentAccountStatus(account.userWalletId)
            },
        )
    }

    private suspend fun CustomerInfo.mapToPaymentAccountStatus(userWalletId: UserWalletId): PaymentAccountStatusValue {
        val fiatRate = getTangemPayFiatRateUseCase()
        val isDeactivated = paymentAccountProductInstance?.status == CustomerInfo.ProductInstance.Status.DEACTIVATED
        val isFormer = state == CustomerInfo.State.FORMER
        val multichainNetworkStatuses = tangemPayCurrencyFactory.createNetworkStatuses(userWalletId, networks, fiatRate)
        val resolved = resolveBalance(userWalletId)
        return when {
            isDeactivated || isFormer -> {
                PaymentAccountStatusValue.Deactivated(
                    source = resolved.source,
                    customerId = customerId,
                    balance = resolved.balance,
                    cryptoCurrency = tangemPayCurrencyFactory.create(userWalletId),
                    networks = multichainNetworkStatuses,
                    fiatRate = fiatRate,
                    error = null,
                )
            }
            else -> buildLoadedState(
                userWalletId = userWalletId,
                balance = resolved.balance,
                source = resolved.source,
                fiatRate = fiatRate,
                customerId = customerId,
                networks = multichainNetworkStatuses,
                isPaymentAccountIssuing = isPaymentAccountIssuing(userWalletId),
                isKycAdditionalInfoRequired = isKycAdditionalInfoRequested,
            )
        }
    }

    private suspend fun isPaymentAccountIssuing(userWalletId: UserWalletId): Boolean {
        val summary = getCustomerSummaryUseCase(userWalletId, CachePolicy.ONLY_CACHE).getOrNull() ?: return false
        val state = TangemPayCustomerStateConverter.convert(summary) as? TangemPayCustomerState.Accounts
        return state?.paymentAccount == PaymentAccountActivationState.Issuing(
            step = PaymentAccountActivationState.Issuing.Step.ISSUING,
        )
    }

    private suspend fun CustomerInfo.resolveBalance(userWalletId: UserWalletId): ResolvedBalance {
        val fiat = fiatBalance
        if (fiat != null) {
            return ResolvedBalance(
                balance = PaymentAccountStatusValue.Balance(fiatBalance = fiat),
                source = StatusSource.ACTUAL,
            )
        }

        val cached = paymentAccountStatusesStore.getSyncOrNull(userWalletId)?.value?.balanceOrNull
        logger.i("resolveBalance $userWalletId: response without balances, cachedBalance=${cached != null}")
        return if (cached == null) {
            ResolvedBalance(balance = null, source = StatusSource.ACTUAL)
        } else {
            ResolvedBalance(balance = cached, source = StatusSource.ONLY_CACHE)
        }
    }

    private data class ResolvedBalance(
        val balance: PaymentAccountStatusValue.Balance?,
        val source: StatusSource,
    )

    private suspend fun CustomerInfo.resolveCards(userWalletId: UserWalletId): List<TangemPayCard> {
        val cardsById = cards.associateBy { it.cardId }
        val activatingProductInstanceIds = resolveActivatingProductInstanceIds(userWalletId, cardsById)
        return cardProductInstances.mapNotNull { productInstance ->
            val cardId = productInstance.cardId.ifEmpty { return@mapNotNull null }
            val cardInfo = cardsById[cardId]
            val cardState = resolveCardState(
                cardInfo = cardInfo,
                productInstance = productInstance,
                cardId = cardId,
                userWalletId = userWalletId,
                isActivating = productInstance.id in activatingProductInstanceIds,
            )
            TangemPayCard(
                id = cardId,
                productInstanceId = productInstance.id,
                cardStatus = cardInfo?.cardStatus ?: productInstance.status.toCardStatus(),
                hasPinCode = cardInfo?.isPinSet == true,
                displayName = productInstance.displayName,
                limit = TangemPayCardLimitData(
                    actualCardLimit = productInstance.actualCardLimit,
                    adminCardLimit = productInstance.adminCardLimit,
                ),
                frozenState = resolveFrozenState(productInstance = productInstance, cardId = cardId),
                lastDigits = cardInfo?.lastFourDigits.orEmpty(),
                images = cardInfo?.images ?: tariffPlan?.plan?.images.orEmpty(),
                state = cardState,
                embossName = cardInfo?.embossName,
                cardType = cardInfo?.cardType ?: TangemPayCardType.UNDEFINED,
            )
        }
    }

    private suspend fun resolveFrozenState(
        productInstance: CustomerInfo.ProductInstance,
        cardId: String,
    ): TangemPayCardFrozenState {
        if (tangemPayFeatureToggles.isOrderFrameworkEnabled) return productInstance.frozenState

        val isChanging = cardDetailsRepository.cardFrozenStateSync(cardId) == TangemPayCardFrozenState.Pending
        return if (isChanging) TangemPayCardFrozenState.Pending else productInstance.frozenState
    }

    private suspend fun CustomerInfo.buildLoadedState(
        userWalletId: UserWalletId,
        balance: PaymentAccountStatusValue.Balance?,
        source: StatusSource,
        customerId: String,
        fiatRate: BigDecimal?,
        networks: List<PaymentNetworkStatus>,
        isPaymentAccountIssuing: Boolean,
        isKycAdditionalInfoRequired: Boolean,
    ): PaymentAccountStatusValue {
        val cards = resolveCards(userWalletId)

        val issuingCards = buildIssuingCards(
            userWalletId = userWalletId,
            surfacedProductInstanceIds = cards.mapTo(mutableSetOf()) { it.productInstanceId },
        )

        val orderedCards = cards.stableOrder(previousRealCardOrder(userWalletId))

        val tariffPlanState = tariffPlan?.let { tariff ->
            getTangemPayTariffPlanStateUseCase(
                userWalletId = userWalletId,
                tariff = tariff,
            )
        }
        val channels = onboardingRepository.fetchCustomerEligibility(userWalletId).getOrElse { error ->
            logger.e("fetchCustomerEligibility failed for $userWalletId: $error")
            null
        }
        return PaymentAccountStatusValue.Loaded(
            source = source,
            customerId = customerId,
            paymentAccountAddress = paymentAccount?.address,
            cryptoCurrency = tangemPayCurrencyFactory.create(userWalletId),
            networks = networks,
            fiatRate = fiatRate,
            cards = orderedCards + issuingCards,
            balance = balance,
            error = null,
            virtualAccount = resolveVirtualAccountOnramp(userWalletId = userWalletId, channels = channels),
            isBankOfframpAvailable = channels?.contains(TangemPayEligibilityType.BANK_OFFRAMP),
            tariffPlan = tariffPlanState,
            isPaymentAccountIssuing = isPaymentAccountIssuing || tariffPlanState?.isPlanTransitioningState == true,
            isKycAdditionalInfoRequired = isKycAdditionalInfoRequired,
        )
    }

    /**
     * Resolves the Virtual Account on-ramp dimension (VA MVP0, TWI-1638).
     *
     * Resolution order:
     * 1. An activated product instance with [SpecificationDataType.ACCOUNT] exists — clears any stale persisted
     *    VA order id and failure marker and surfaces [VirtualAccountOnramp.Available] carrying only its id; bank
     *    credentials are fetched on demand by the deposit screen, not here. A still provisioning one (see
     *    [isProvisioning]) surfaces [VirtualAccountOnramp.Processing].
     * 2. Otherwise, a VA order id is persisted locally — checks its status via `getOrderData`:
     *    NEW/PROCESSING/COMPLETED (or a transient lookup failure) surface [VirtualAccountOnramp.Processing]; CANCELED
     *    clears the persisted id, persists the failure marker and falls through to eligibility; a
     *    [TangemPayApiError.OrderNotFound] (the persisted id went stale) clears the persisted id and falls through.
     * 3. Otherwise — when the wallet has the `VIRTUAL_ACCOUNT` eligibility channel (fetched fresh via the
     *    user token), surfaces [VirtualAccountOnramp.Failed] while the failure marker is persisted (until the

     *    without the channel.
     */
    private suspend fun CustomerInfo.resolveVirtualAccountOnramp(
        userWalletId: UserWalletId,
        channels: List<TangemPayEligibilityType>?,
    ): VirtualAccountOnramp? {
        val accountInstance = virtualAccountProductInstance
        if (accountInstance != null) {
            if (accountInstance.isProvisioning()) return VirtualAccountOnramp.Processing
            onboardingRepository.clearVirtualAccountOrderId(userWalletId)
            onboardingRepository.storeVirtualAccountOrderFailed(userWalletId = userWalletId, isFailed = false)
            return VirtualAccountOnramp.Available(productInstanceId = accountInstance.id)
        }

        val vaOrderId = onboardingRepository.getVirtualAccountOrderId(userWalletId)
        if (vaOrderId != null) {
            return customerOrderRepository.getOrderData(userWalletId = userWalletId, orderId = vaOrderId).fold(
                ifLeft = { error ->
                    logger.e("getOrderData(va) failed for $vaOrderId: $error")
                    if (error is TangemPayApiError.OrderNotFound) {
                        onboardingRepository.clearVirtualAccountOrderId(userWalletId)
                        channels.toVirtualAccountOnramp(isOrderFailed = false)
                    } else {
                        VirtualAccountOnramp.Processing
                    }
                },
                ifRight = { orderData ->
                    when (orderData.status) {
                        OrderStatus.CANCELED -> {
                            onboardingRepository.clearVirtualAccountOrderId(userWalletId)
                            onboardingRepository.storeVirtualAccountOrderFailed(
                                userWalletId = userWalletId,
                                isFailed = true,
                            )
                            channels.toVirtualAccountOnramp(isOrderFailed = true)
                        }
                        OrderStatus.NEW,
                        OrderStatus.PROCESSING,
                        OrderStatus.COMPLETED,
                        -> VirtualAccountOnramp.Processing
                    }
                },
            )
        }

        val isOrderFailed = onboardingRepository.hasVirtualAccountOrderFailed(userWalletId)
        return channels.toVirtualAccountOnramp(isOrderFailed = isOrderFailed)
    }

    private fun CustomerInfo.ProductInstance.isProvisioning(): Boolean = when (status) {
        CustomerInfo.ProductInstance.Status.NEW,
        CustomerInfo.ProductInstance.Status.READY_FOR_MANUFACTURING,
        CustomerInfo.ProductInstance.Status.MANUFACTURING,
        CustomerInfo.ProductInstance.Status.SENT_TO_DELIVERY,
        CustomerInfo.ProductInstance.Status.DELIVERED,
        CustomerInfo.ProductInstance.Status.ACTIVATING,
        -> true
        CustomerInfo.ProductInstance.Status.ACTIVE,
        CustomerInfo.ProductInstance.Status.BLOCKED,
        CustomerInfo.ProductInstance.Status.DEACTIVATING,
        CustomerInfo.ProductInstance.Status.DEACTIVATED,
        CustomerInfo.ProductInstance.Status.CANCELED,
        CustomerInfo.ProductInstance.Status.UNKNOWN,
        -> false
    }

    private fun List<TangemPayEligibilityType>?.toVirtualAccountOnramp(isOrderFailed: Boolean): VirtualAccountOnramp? {
        if (this?.contains(TangemPayEligibilityType.VIRTUAL_ACCOUNT) != true) return null
        return if (isOrderFailed) VirtualAccountOnramp.Failed else VirtualAccountOnramp.Eligible
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
            ?.filterNot { it.isPlaceholder || it.state == TangemPayCardState.Issuing }
            ?.map { it.id }
            .orEmpty()
    }

    private suspend fun resolveCardState(
        cardInfo: CustomerInfo.CardInfo?,
        productInstance: CustomerInfo.ProductInstance,
        cardId: String,
        userWalletId: UserWalletId,
        isActivating: Boolean,
    ): TangemPayCardState {
        val hintedState = getCardState(cardId = cardId, userWalletId = userWalletId)
        if (hintedState != TangemPayCardState.Active) return hintedState

        val isAwaitingActivation = cardInfo != null && PlasticCardStateResolver.isAwaitingActivation(
            cardInfo = cardInfo,
            productInstance = productInstance,
        )
        return when {
            isAwaitingActivation && isActivating -> TangemPayCardState.Activating
            isAwaitingActivation -> TangemPayCardState.Delivering
            else -> TangemPayCardState.Active
        }
    }

    /**
     * Product instances whose delivered physical card has an activation order still in flight. `findOrders`
     * is the source of truth: the card stays `INACTIVE` until the order completes, so without this the card
     * would fall back to the "enter the last 4 digits" state while its activation is still being processed —
     * and the in-memory poller does not survive a force close.
     *
     * The request is skipped entirely unless some card is awaiting activation, and the response is
     * re-filtered client-side, so that an order of any other type for the same product instance cannot pin
     * the card to activating if the backend ignored the type filter.
     */
    private suspend fun CustomerInfo.resolveActivatingProductInstanceIds(
        userWalletId: UserWalletId,
        cardsById: Map<String, CustomerInfo.CardInfo>,
    ): Set<String> {
        val hasCardAwaitingActivation = cardProductInstances.any { productInstance ->
            val cardInfo = cardsById[productInstance.cardId] ?: return@any false
            PlasticCardStateResolver.isAwaitingActivation(cardInfo = cardInfo, productInstance = productInstance)
        }
        if (!hasCardAwaitingActivation) return emptySet()

        return customerOrderRepository.findOrders(
            userWalletId = userWalletId,
            types = setOf(OrderType.CARD_ACTIVATION_PLASTIC_RAIN),
            statuses = OrderStatus.activeStatuses,
        ).getOrNull()
            ?.filter { it.type == OrderType.CARD_ACTIVATION_PLASTIC_RAIN && it.status.isActive }
            ?.mapNotNullTo(mutableSetOf()) { it.productInstanceId }
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
     * Builds the placeholder cards from locally tracked additional-card orders. Each order is
     * re-checked against the backend; terminal orders are dropped (and forgotten) because the real card
     * is now part of [CustomerInfo], while in-flight orders surface as a placeholder card.
     *
     * A plastic issue order surfaces as a delivering placeholder only until the backend creates its
     * product instance and the real physical card appears in [CustomerInfo] (matched via
     * [surfacedProductInstanceIds]) — the order itself stays active for longer.
     */
    private suspend fun buildIssuingCards(
        userWalletId: UserWalletId,
        surfacedProductInstanceIds: Set<String>,
    ): List<TangemPayCard> {
        val orderIds = issueCardRepository.getIssueOrderIds(userWalletId)
        return orderIds.mapNotNull { orderId ->
            cardDetailsRepository.getOrderInfo(userWalletId, orderId).fold(
                ifLeft = { error ->
                    if (error == TangemPayApiError.OrderNotFound) {
                        logger.i("buildIssuingCards $userWalletId: dropping missing order $orderId")
                        issueCardRepository.removeIssueOrderId(userWalletId, orderId)
                        null
                    } else {
                        issuingPlaceholderCard(orderId)
                    }
                },
                ifRight = { order ->
                    when {
                        order.orderStatus.isTerminal -> {
                            issueCardRepository.removeIssueOrderId(userWalletId, orderId)
                            null
                        }
                        order.orderType == OrderType.CARD_ISSUE_PLASTIC_RAIN -> {
                            val isCardSurfaced = order.productInstanceId in surfacedProductInstanceIds
                            if (isCardSurfaced) null else deliveringPlaceholderCard(orderId)
                        }
                        else -> issuingPlaceholderCard(orderId)
                    }
                },
            )
        }
    }

    /**
     * Card status derived from the owning product instance, used when `customer/me`.cards[] carries no entry
     * for it — the account is operational, only the card payload is missing.
     */
    private fun CustomerInfo.ProductInstance.Status.toCardStatus(): TangemPayCard.Status = when (this) {
        CustomerInfo.ProductInstance.Status.ACTIVE -> TangemPayCard.Status.ACTIVE
        CustomerInfo.ProductInstance.Status.BLOCKED -> TangemPayCard.Status.BLOCKED
        CustomerInfo.ProductInstance.Status.DEACTIVATED,
        CustomerInfo.ProductInstance.Status.DEACTIVATING,
        CustomerInfo.ProductInstance.Status.CANCELED,
        -> TangemPayCard.Status.CANCELED
        CustomerInfo.ProductInstance.Status.NEW,
        CustomerInfo.ProductInstance.Status.READY_FOR_MANUFACTURING,
        CustomerInfo.ProductInstance.Status.MANUFACTURING,
        CustomerInfo.ProductInstance.Status.SENT_TO_DELIVERY,
        CustomerInfo.ProductInstance.Status.DELIVERED,
        CustomerInfo.ProductInstance.Status.ACTIVATING,
        -> TangemPayCard.Status.INACTIVE
        CustomerInfo.ProductInstance.Status.UNKNOWN -> TangemPayCard.Status.UNDEFINED
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
        embossName = null,
        cardType = TangemPayCardType.VIRTUAL,
        isPlaceholder = true,
    )

    /** Placeholder card for an ordered plastic card the backend has not surfaced in `customer/me` yet. */
    private fun deliveringPlaceholderCard(orderId: String): TangemPayCard = TangemPayCard(
        id = orderId,
        productInstanceId = orderId,
        cardStatus = TangemPayCard.Status.INACTIVE,
        hasPinCode = false,
        displayName = null,
        limit = null,
        frozenState = TangemPayCardFrozenState.Unfrozen,
        lastDigits = "",
        images = emptyList(),
        state = TangemPayCardState.Delivering,
        embossName = null,
        cardType = TangemPayCardType.PHYSICAL,
        isPlaceholder = true,
    )

    private suspend fun TangemPayApiError.toStatusValueWhenHasTangemPay(
        userWalletId: UserWalletId,
    ): PaymentAccountStatusValue {
        return when (this) {
            is TangemPayApiError.NotFound -> constructNotCreatedOrEmptyStatus(userWalletId)
            else -> toErrorValue()
        }
    }

    private suspend fun TangemPayApiError.toStatusValueWhenTangemPayStatusUnknown(
        userWalletId: UserWalletId,
    ): PaymentAccountStatusValue {
        return when (this) {
            is TangemPayApiError.NotFound -> constructNotCreatedOrEmptyStatus(userWalletId)
            else -> toCachedStatusOrNull(userWalletId) ?: constructNotCreatedOrEmptyStatus(userWalletId)
        }
    }

    private suspend fun TangemPayApiError.toCachedStatusOrNull(userWalletId: UserWalletId): PaymentAccountStatusValue? {
        val cached = paymentAccountStatusesStore.getSyncOrNull(userWalletId)?.value ?: return null
        val isKeptOnFailure = when (cached) {
            is PaymentAccountStatusValue.UnderReview -> this !is TangemPayApiError.RefreshTokenExpired
            else -> cached.hasAccountData()
        }
        if (!isKeptOnFailure) return null
        return cached.copySealed(source = StatusSource.ONLY_CACHE, error = toErrorValue())
    }

    private fun TangemPayApiError.toErrorValue(): PaymentAccountStatusValue.Error = when (this) {
        is TangemPayApiError.RefreshTokenExpired -> PaymentAccountStatusValue.Error.NotSynced
        else -> PaymentAccountStatusValue.Error.Unavailable
    }

    private suspend fun constructNotCreatedOrEmptyStatus(userWalletId: UserWalletId): PaymentAccountStatusValue {
        val entryPoint = TangemPayEntryPoint.BANNER
        val isAvailable = !eligibilityManager.isPaeraCustomerForAnyWallet(entryPoint) &&
            eligibilityManager.getEligibleWallets(shouldExcludePaeraCustomers = false, entryPoint = entryPoint)
                .any { it.walletId == userWalletId }

        return if (isAvailable) PaymentAccountStatusValue.NotCreated else PaymentAccountStatusValue.Empty
    }
}