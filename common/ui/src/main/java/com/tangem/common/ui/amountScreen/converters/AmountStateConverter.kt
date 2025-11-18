package com.tangem.common.ui.amountScreen.converters

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldConverter
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.utils.StringsSigns.DOT
import com.tangem.utils.converter.Converter

/**
 * Converts initial [String] to [AmountState]
 *
 * @property clickIntents amount screen clicks
 * @property appCurrency selected app currency
 * @property cryptoCurrencyStatus current cryptocurrency status
 * @property maxEnterAmount max enter amount data
 * @property iconStateConverter currency icon converter
 * @property isBalanceHidden is balance hidden status
 */
@Suppress("LongParameterList")
class AmountStateConverter(
    private val clickIntents: AmountScreenClickIntents,
    private val appCurrency: AppCurrency,
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val maxEnterAmount: EnterAmountBoundary,
    private val iconStateConverter: CryptoCurrencyToIconStateConverter,
    private val isBalanceHidden: Boolean,
    private val accountTitleUM: AccountTitleUM,
) : Converter<AmountParameters, AmountState> {

    private val amountFieldConverter by lazy(LazyThreadSafetyMode.NONE) {
        AmountFieldConverter(
            clickIntents = clickIntents,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            appCurrency = appCurrency,
        )
    }

    override fun convert(value: AmountParameters): AmountState {
        val fiat = maxEnterAmount.fiatAmount.format { fiat(appCurrency.code, appCurrency.symbol) }
        val crypto = maxEnterAmount.amount.format { crypto(cryptoCurrencyStatus.currency) }

        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Loading) {
            return AmountState.Empty
        }

        return AmountState.Data(
            accountTitleUM = accountTitleUM,
            availableBalanceCrypto = stringReference(crypto).orMaskWithStars(isBalanceHidden),
            availableBalanceFiat = if (isBalanceHidden) {
                TextReference.EMPTY
            } else {
                combinedReference(
                    stringReference(" $DOT "),
                    stringReference(fiat),
                )
            },
            tokenName = stringReference(cryptoCurrencyStatus.currency.name),
            tokenIconState = iconStateConverter.convert(cryptoCurrencyStatus.currency),
            amountTextField = amountFieldConverter.convert(value.value),
            isPrimaryButtonEnabled = false,
            appCurrency = appCurrency,
        )
    }
}