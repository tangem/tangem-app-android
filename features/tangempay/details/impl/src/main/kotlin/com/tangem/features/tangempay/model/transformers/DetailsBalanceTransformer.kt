package com.tangem.features.tangempay.model.transformers

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import java.util.Currency

internal class DetailsBalanceTransformer(
    private val balance: Either<UniversalError, TangemPayCardBalance>,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val balance = when (balance) {
            is Either.Left<UniversalError> -> {
                TangemPayDetailsBalanceBlockState.Error(
                    actionButtons = persistentListOf(),
                    cardsBlockState = prevState.balanceBlockState.cardsBlockState,
                )
            }
            is Either.Right<TangemPayCardBalance> -> {
                TangemPayDetailsBalanceBlockState.Content(
                    isBalanceFlickering = false,
                    fiatBalance = getFiatBalanceText(balance.value),
                    actionButtons = prevState.balanceBlockState.actionButtons,
                    cardsBlockState = prevState.balanceBlockState.cardsBlockState,
                )
            }
        }
        return prevState.copy(balanceBlockState = balance)
    }

    private fun getFiatBalanceText(balance: TangemPayCardBalance): String {
        val currency = Currency.getInstance(balance.currencyCode)
        return balance.fiatBalance.format {
            fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol)
        }
    }
}