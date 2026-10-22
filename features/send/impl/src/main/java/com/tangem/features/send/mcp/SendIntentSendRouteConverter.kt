package com.tangem.features.send.mcp

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.common.routing.AppRoute
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.utils.converter.Converter
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Builds the Send route that carries out a [SendIntent] from the currency it was matched to. The recipient, the memo
 * and the amount are prefilled and stay editable, as for a QR scanned on the main screen: no `transactionId` is set,
 * since it switches Send to the locked sell-redirect mode. Send takes the amount as typed text, so it is written in the
 * locale's separators without changing its value. Non-positive amounts and amounts that need rounding to fit the
 * currency's decimals are rejected; insignificant trailing zeros are allowed.
 *
 * The memo is passed for any network: Send shows no memo field and builds no memo extras where the network has none.
 */
internal class SendIntentSendRouteConverter @Inject constructor() :
    Converter<SendIntentSendRouteConverter.Input, Either<Throwable, AppRoute.Send>> {

    data class Input(
        val intent: SendIntent,
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
    )

    override fun convert(value: Input): Either<Throwable, AppRoute.Send> = either {
        val amount = value.intent.amount
        val amountScale = Either.catch { amount.stripTrailingZeros().scale() }.bind()
        ensure(amount.signum() > 0 && amountScale <= value.currency.decimals) {
            IllegalArgumentException("Intent ${value.intent.id} amount cannot fit ${value.currency.decimals} decimals")
        }
        val formattedAmount = amount.parseBigDecimal(value.currency.decimals, roundingMode = RoundingMode.UNNECESSARY)
        ensure(formattedAmount.isNotBlank()) {
            IllegalArgumentException("Intent ${value.intent.id} amount cannot be formatted")
        }

        AppRoute.Send(
            userWalletId = value.userWalletId,
            currency = value.currency,
            amount = formattedAmount,
            tag = value.intent.memo,
            destinationAddress = value.intent.toAddress,
            intentId = value.intent.id,
        )
    }
}