package com.tangem.features.tangempay

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