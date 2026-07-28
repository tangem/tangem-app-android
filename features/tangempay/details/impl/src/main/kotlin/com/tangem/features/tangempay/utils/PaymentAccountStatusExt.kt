package com.tangem.features.tangempay.utils

import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.wallet.UserWalletId

internal val AccountStatus.Payment.userWalletId: UserWalletId
    get() = account.userWalletId

internal val AccountStatus.Payment.customerId: String?
    get() = when (val v = value) {
        is PaymentAccountStatusValue.Loaded -> v.customerId
        is PaymentAccountStatusValue.Deactivated -> v.customerId
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

internal val AccountStatus.Payment.tariffPlan: TangemPayCustomerTariffPlan?
    get() = when (val v = value) {
        is PaymentAccountStatusValue.Inactive -> v.tariffPlan.tariff
        is PaymentAccountStatusValue.AwaitingPlanSelection -> v.tariffPlan
        is PaymentAccountStatusValue.Loaded -> v.tariffPlan?.tariff
        is PaymentAccountStatusValue.Deactivated -> null
        else -> error("TangemPayDetails opened with unsupported status: $v")
    }

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
    else -> null
}

internal val PaymentAccountStatusValue.Balance.hasWithdrawableAmount: Boolean
    get() = availableForWithdrawal.signum() > 0