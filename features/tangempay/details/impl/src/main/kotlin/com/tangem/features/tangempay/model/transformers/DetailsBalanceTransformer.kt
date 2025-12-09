package com.tangem.features.tangempay.model.transformers

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.TangemPayCryptoCurrencyFactory
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.util.Currency

internal class DetailsBalanceTransformer(
    private val balance: Either<UniversalError, TangemPayCardBalance>,
    private val cryptoCurrencyFactory: TangemPayCryptoCurrencyFactory,
    private val userWallet: UserWallet?,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val balance = when (balance) {
            is Either.Left<UniversalError> -> {
                TangemPayDetailsBalanceBlockState.Error(actionButtons = persistentListOf())
            }
            is Either.Right<TangemPayCardBalance> -> {
                val cryptoCurrency = userWallet?.let {
                    cryptoCurrencyFactory.create(userWallet, balance.value.chainId).getOrNull()
                }
                if (cryptoCurrency == null) {
                    TangemPayDetailsBalanceBlockState.Error(actionButtons = persistentListOf())
                } else {
                    TangemPayDetailsBalanceBlockState.Content(
                        isBalanceFlickering = false,
                        fiatBalance = getFiatBalanceText(balance.value),
                        cryptoBalance = getCryptoBalanceText(balance.value.cryptoBalance, cryptoCurrency),
                        actionButtons = prevState.balanceBlockState.actionButtons,
                    )
                }
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

    private fun getCryptoBalanceText(cryptoBalance: BigDecimal, cryptoCurrency: CryptoCurrency): String {
        return cryptoBalance.format { crypto(cryptoCurrency = cryptoCurrency) }
    }
}