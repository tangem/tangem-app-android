package com.tangem.common.ui.amountScreen.converters

import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldConverter
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldConverterV2
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.models.AmountSegmentedButtonsConfig
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns.DOT
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import kotlinx.collections.immutable.persistentListOf

/**
 * Converts initial [String] to [AmountState]
 *
 * @property clickIntents amount screen clicks
 * @property appCurrencyProvider selected app currency provider
 * @property maxEnterAmount max enter amount data
 * @property cryptoCurrencyStatusProvider current cryptocurrency status provider
 * @property iconStateConverter currency icon converter
 */
@Deprecated("Use AmountStateConverterV2")
class AmountStateConverter(
    private val clickIntents: AmountScreenClickIntents,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val maxEnterAmount: EnterAmountBoundary,
    private val iconStateConverter: CryptoCurrencyToIconStateConverter,
) : Converter<AmountParameters, AmountState> {

    private val amountFieldConverter by lazy(LazyThreadSafetyMode.NONE) {
        AmountFieldConverter(
            clickIntents = clickIntents,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
            appCurrencyProvider = appCurrencyProvider,
        )
    }

    override fun convert(value: AmountParameters): AmountState {
        val appCurrency = appCurrencyProvider()
        val status = cryptoCurrencyStatusProvider()
        val fiat = maxEnterAmount.fiatAmount.format { fiat(appCurrency.code, appCurrency.symbol) }
        val crypto = maxEnterAmount.amount.format { crypto(status.currency) }
        val noFeeRate = status.value.fiatRate.isNullOrZero()

        return AmountState.Data(
            title = value.title,
            availableBalance = resourceReference(R.string.common_crypto_fiat_format, wrappedList(crypto, fiat)),
            tokenName = stringReference(status.currency.name),
            tokenIconState = iconStateConverter.convert(status),
            amountTextField = amountFieldConverter.convert(value.value),
            isPrimaryButtonEnabled = false,
            appCurrency = appCurrency,
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
            isRedesignEnabled = false,
        )
    }
}

/**
 * Converts initial [String] to [AmountState]
 *
 * @property clickIntents amount screen clicks
 * @property appCurrency selected app currency
 * @property maxEnterAmount max enter amount data
 * @property cryptoCurrencyStatus current cryptocurrency status
 * @property iconStateConverter currency icon converter
 */
class AmountStateConverterV2(
    private val clickIntents: AmountScreenClickIntents,
    private val appCurrency: AppCurrency,
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val maxEnterAmount: EnterAmountBoundary,
    private val iconStateConverter: CryptoCurrencyToIconStateConverter,
    private val isRedesignEnabled: Boolean,
) : Converter<AmountParameters, AmountState> {

    private val amountFieldConverter by lazy(LazyThreadSafetyMode.NONE) {
        AmountFieldConverterV2(
            clickIntents = clickIntents,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            appCurrency = appCurrency,
        )
    }

    override fun convert(value: AmountParameters): AmountState {
        val fiat = maxEnterAmount.fiatAmount.format { fiat(appCurrency.code, appCurrency.symbol) }
        val crypto = maxEnterAmount.amount.format { crypto(cryptoCurrencyStatus.currency) }
        val noFeeRate = cryptoCurrencyStatus.value.fiatRate.isNullOrZero()

        return AmountState.Data(
            title = value.title,
            availableBalance = if (isRedesignEnabled) {
                combinedReference(
                    stringReference(crypto),
                    stringReference(" $DOT "),
                    stringReference(fiat),
                )
            } else {
                resourceReference(R.string.common_crypto_fiat_format, wrappedList(crypto, fiat))
            },
            tokenName = stringReference(cryptoCurrencyStatus.currency.name),
            tokenIconState = iconStateConverter.convert(cryptoCurrencyStatus.currency),
            amountTextField = amountFieldConverter.convert(value.value),
            isPrimaryButtonEnabled = false,
            appCurrency = appCurrency,
            segmentedButtonConfig = persistentListOf(
                AmountSegmentedButtonsConfig(
                    title = stringReference(cryptoCurrencyStatus.currency.symbol),
                    iconState = iconStateConverter.convertCustom(
                        value = cryptoCurrencyStatus,
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
            isRedesignEnabled = isRedesignEnabled,
        )
    }
}