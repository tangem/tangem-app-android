package com.tangem.features.tangempay.common

import com.tangem.domain.models.account.*
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

internal val AccountStatus.Payment.userWalletId: UserWalletId
    get() = account.userWalletId

internal val AccountStatus.Payment.customerId: String?
    get() = when (val v = value) {
        is PaymentAccountStatusValue.Loaded -> v.customerId
        is PaymentAccountStatusValue.Deactivated -> v.customerId
        is PaymentAccountStatusValue.Error.CardIssueFailed -> v.customerId
        else -> null
    }

internal val PaymentAccountStatusValue.typeName: String
    get() = when (this) {
        PaymentAccountStatusValue.Empty -> "Empty"
        PaymentAccountStatusValue.Loading -> "Loading"
        PaymentAccountStatusValue.NotCreated -> "NotCreated"
        is PaymentAccountStatusValue.UnderReview -> "UnderReview"
        is PaymentAccountStatusValue.IssuingCard -> "IssuingCard"
        is PaymentAccountStatusValue.AwaitingPlanSelection -> "AwaitingPlanSelection"
        is PaymentAccountStatusValue.Inactive -> "Inactive"
        is PaymentAccountStatusValue.Deactivated -> "Deactivated"
        is PaymentAccountStatusValue.Loaded -> "Loaded"
        PaymentAccountStatusValue.Error.ExposedDevice -> "Error.ExposedDevice"
        PaymentAccountStatusValue.Error.Unavailable -> "Error.Unavailable"
        PaymentAccountStatusValue.Error.NotSynced -> "Error.NotSynced"
        is PaymentAccountStatusValue.Error.CardIssueFailed -> "Error.CardIssueFailed"
    }

internal val AccountStatus.Payment.tariffPlanState: TangemPayTariffPlanState?
    get() = when (val v = value) {
        is PaymentAccountStatusValue.Inactive -> v.tariffPlan
        is PaymentAccountStatusValue.Loaded -> v.tariffPlan
        is PaymentAccountStatusValue.Error.CardIssueFailed -> v.tariffPlan
        else -> null
    }

internal val AccountStatus.Payment.tariffPlan: TangemPayCustomerTariffPlan?
    get() = value.tariffPlan

internal val AccountStatus.Payment.cardMainImageUrl: String?
    get() = tariffPlan?.plan?.mainImageUrl

internal val PaymentAccountStatusValue.Loaded.isFresh: Boolean
    get() = source.isActual() && error == null

internal inline fun <T> AccountStatus.Payment.ifLoadedOrNull(call: (PaymentAccountStatusValue.Loaded) -> T): T? {
    val value = value
    return if (value is PaymentAccountStatusValue.Loaded) {
        call(value)
    } else {
        null
    }
}

internal fun AccountStatus.Payment.balanceOrNull(): PaymentAccountStatusValue.Balance? = when (val v = value) {
    is PaymentAccountStatusValue.Loaded -> v.balance
    is PaymentAccountStatusValue.Deactivated -> v.balance
    is PaymentAccountStatusValue.Error.CardIssueFailed -> v.balance
    else -> null
}

internal fun AccountStatus.Payment.networksOrNull(): List<PaymentNetworkStatus>? = when (val v = value) {
    is PaymentAccountStatusValue.Loaded -> v.networks
    is PaymentAccountStatusValue.Deactivated -> v.networks
    is PaymentAccountStatusValue.Error.CardIssueFailed -> v.networks
    else -> null
}

internal val PaymentAccountStatusValue.availableForWithdrawalOrZero: BigDecimal
    get() = when (this) {
        is PaymentAccountStatusValue.Loaded -> availableForWithdrawal
        is PaymentAccountStatusValue.Deactivated -> availableForWithdrawal
        else -> BigDecimal.ZERO
    }

internal val PaymentAccountStatusValue.hasWithdrawableAmount: Boolean
    get() = availableForWithdrawalOrZero.signum() > 0

internal fun List<PaymentNetworkStatus>.firstDepositAddress(): String? =
    filterIsInstance<PaymentNetworkStatus.Available>().firstOrNull()?.depositAddress

internal fun PaymentAccountStatusValue.canAddFunds(): Boolean = when (this) {
    is PaymentAccountStatusValue.Error.CardIssueFailed -> networks
    is PaymentAccountStatusValue.Loaded -> networks
    is PaymentAccountStatusValue.Deactivated -> networks
    else -> emptyList()
}.any { it is PaymentNetworkStatus.Available }