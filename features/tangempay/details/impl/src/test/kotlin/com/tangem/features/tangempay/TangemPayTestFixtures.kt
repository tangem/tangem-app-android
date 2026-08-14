package com.tangem.features.tangempay

import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import org.joda.time.DateTime
import java.math.BigDecimal
import java.util.Currency

internal fun spendTransaction(
    id: String = "tx_1",
    cardName: String? = "Basic card",
    cardNumberLast4: String? = "9092",
    status: TangemPayTxHistoryItem.Status = TangemPayTxHistoryItem.Status.COMPLETED,
    cashback: TangemPayTxHistoryItem.Cashback? = null,
): TangemPayTxHistoryItem.Spend = TangemPayTxHistoryItem.Spend(
    id = id,
    jsonRepresentation = "{}",
    date = DateTime(0L),
    amount = BigDecimal.TEN,
    currency = Currency.getInstance("USD"),
    authorizedAmount = BigDecimal.TEN,
    localAmount = null,
    localCurrency = null,
    enrichedMerchantName = null,
    merchantName = "Merchant",
    enrichedMerchantCategory = null,
    merchantCategoryCode = null,
    merchantCategory = null,
    status = status,
    enrichedMerchantIconUrl = null,
    declinedReason = null,
    cardName = cardName,
    cardNumberLast4 = cardNumberLast4,
    cashback = cashback,
)

internal fun cashback(
    status: TangemPayTxHistoryItem.Cashback.Status = TangemPayTxHistoryItem.Cashback.Status.CONFIRMED,
    amount: BigDecimal? = BigDecimal("5.00"),
    currency: Currency? = Currency.getInstance("USD"),
    isCapTrimmed: Boolean = false,
    exclusionReason: TangemPayTxHistoryItem.Cashback.ExclusionReason? = null,
    promotionIds: List<String> = emptyList(),
): TangemPayTxHistoryItem.Cashback = TangemPayTxHistoryItem.Cashback(
    status = status,
    amount = amount,
    currency = currency,
    isCapTrimmed = isCapTrimmed,
    exclusionReason = exclusionReason,
    promotionIds = promotionIds,
)

internal fun paymentTransaction(id: String = "tx_payment_1"): TangemPayTxHistoryItem.Payment =
    TangemPayTxHistoryItem.Payment(
        id = id,
        jsonRepresentation = "{}",
        date = DateTime(0L),
        amount = BigDecimal.TEN,
        currency = Currency.getInstance("USD"),
        transactionHash = null,
    )

internal fun recurringFee(
    amount: BigDecimal = BigDecimal("29.99"),
    currency: String = "USD",
): TangemPayTariffPlan.Fee = TangemPayTariffPlan.Fee(
    type = TangemPayTariffPlan.Fee.Type.RECURRING,
    amount = amount,
    currency = currency,
    description = "Monthly plan fee",
    period = TangemPayTariffPlan.Fee.Period.MONTH,
)

internal fun tariffPlan(
    tierId: String = "PLUS",
    isBasicTier: Boolean = false,
    name: String = tierId,
    descriptionItems: List<TangemPayTariffPlan.DescriptionItem> = emptyList(),
    fees: List<TangemPayTariffPlan.Fee> = listOf(recurringFee()),
): TangemPayTariffPlan = TangemPayTariffPlan(
    id = "plan-${tierId.lowercase()}",
    tierId = tierId,
    isBasicTier = isBasicTier,
    name = name,
    programName = "program-$tierId",
    descriptionItems = descriptionItems,
    fees = fees,
)

internal fun customerTariffPlan(
    status: TangemPayCustomerTariffPlan.Status = TangemPayCustomerTariffPlan.Status.ACTIVE,
    plan: TangemPayTariffPlan = tariffPlan(),
    nextBillingAt: DateTime? = null,
    pendingPlan: TangemPayTariffPlan? = null,
    source: TangemPayCustomerTariffPlan.Source = TangemPayCustomerTariffPlan.Source.CUSTOMER,
): TangemPayCustomerTariffPlan = TangemPayCustomerTariffPlan(
    status = status,
    source = source,
    plan = plan,
    nextBillingAt = nextBillingAt,
    pendingPlan = pendingPlan,
    pendingTransitionAt = null,
)

internal fun awaitingDepositOrder(
    orderId: String = "order-1",
    fromPlan: TangemPayTariffPlan = tariffPlan(tierId = "BASIC", isBasicTier = true),
    toPlan: TangemPayTariffPlan = tariffPlan(tierId = "PLUS"),
): TangemPayTariffPlanState.Order = TangemPayTariffPlanState.Order(
    orderId = orderId,
    step = TangemPayTariffPlanState.OrderStep.AwaitingDeposit(fromPlan = fromPlan, toPlan = toPlan),
)

internal fun tariffPlanState(
    tariff: TangemPayCustomerTariffPlan = customerTariffPlan(),
    order: TangemPayTariffPlanState.Order? = null,
): TangemPayTariffPlanState = TangemPayTariffPlanState(tariff = tariff, order = order)

internal fun tangemPayCard(
    id: String = "card_1",
    lastDigits: String = "1234",
    frozenState: TangemPayCardFrozenState = TangemPayCardFrozenState.Unfrozen,
    state: TangemPayCardState = TangemPayCardState.Active,
): TangemPayCard = TangemPayCard(
    id = id,
    productInstanceId = "product_1",
    cardStatus = TangemPayCard.Status.ACTIVE,
    hasPinCode = true,
    displayName = null,
    limit = null,
    frozenState = frozenState,
    lastDigits = lastDigits,
    images = emptyList(),
    state = state,
)