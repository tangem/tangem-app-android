package com.tangem.common.ui.amountScreen.converters

import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldConverter
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.AmountSegmentedButtonsConfig
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter.formatCryptoAmount
import com.tangem.core.ui.utils.BigDecimalFormatter.formatFiatAmount
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import kotlinx.collections.immutable.persistentListOf

/**
 * Converts initial [String] to [AmountState]
 *
 * @property clickIntents amount screen clicks
 * @property appCurrencyProvider selected app currency provider
 * @property userWalletProvider selected user wallet provider
 * @property cryptoCurrencyStatusProvider current cryptocurrency status provider
 * @property iconStateConverter currency icon converter
 */
class AmountStateConverter(
    private val clickIntents: AmountScreenClickIntents,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val userWalletProvider: Provider<UserWallet>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val iconStateConverter: CryptoCurrencyToIconStateConverter,
) : Converter<String, AmountState> {

    private val amountFieldConverter by lazy(LazyThreadSafetyMode.NONE) {
        AmountFieldConverter(
            clickIntents = clickIntents,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
            appCurrencyProvider = appCurrencyProvider,
        )
    }

    override fun convert(value: String): AmountState {
        val userWallet = userWalletProvider()
        val appCurrency = appCurrencyProvider()
        val status = cryptoCurrencyStatusProvider()
        val fiat = formatFiatAmount(status.value.fiatAmount, appCurrency.code, appCurrency.symbol)
        val crypto = formatCryptoAmount(status.value.amount, status.currency.symbol, status.currency.decimals)
        val noFeeRate = status.value.fiatRate.isNullOrZero()

        return AmountState.Data(
            walletName = userWallet.name,
            walletBalance = resourceReference(R.string.send_wallet_balance_format, wrappedList(crypto, fiat)),
            tokenIconState = iconStateConverter.convert(status),
            amountTextField = amountFieldConverter.convert(value),
            isPrimaryButtonEnabled = false,
            appCurrencyCode = appCurrency.code,
            segmentedButtonConfig = persistentListOf(
                AmountSegmentedButtonsConfig(
                    title = stringReference(status.currency.symbol),
                    iconState = iconStateConverter.convertCustom(
                        value = status,
                        forceGrayscale = noFeeRate,
                        showCustomTokenBadge = false,
                    ),
                    isFiat = false,
                ),
                AmountSegmentedButtonsConfig(
                    title = stringReference(appCurrency.code),
                    iconUrl = appCurrency.iconSmallUrl,
                    isFiat = true,
                ),
            ),
            isSegmentedButtonsEnabled = !noFeeRate,
            selectedButton = 0,
        )
    }
}
