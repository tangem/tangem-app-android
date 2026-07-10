package com.tangem.features.tangempay.utils

import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

internal val AccountStatus.Payment.userWalletId: UserWalletId
    get() = account.userWalletId

internal val AccountStatus.Payment.cryptoCurrency: CryptoCurrency.Token
    get() = when (val v = value) {
        is PaymentAccountStatusValue.Loaded -> v.cryptoCurrency
        is PaymentAccountStatusValue.Deactivated -> v.cryptoCurrency
        else -> error("TangemPayDetails opened with unsupported status: $v")
    }

internal val AccountStatus.Payment.isDeactivated: Boolean
    get() = value is PaymentAccountStatusValue.Deactivated

internal val PaymentAccountStatusValue.Loaded.isFresh: Boolean
    get() = source.isActual() && error == null

internal fun AccountStatus.Payment.requireLoaded(): PaymentAccountStatusValue.Loaded =
    value as? PaymentAccountStatusValue.Loaded
        ?: error("Card-detail subflow requires Loaded status, got ${value::class.simpleName}")

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