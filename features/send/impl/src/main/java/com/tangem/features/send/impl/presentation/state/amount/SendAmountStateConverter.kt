package com.tangem.features.send.impl.presentation.state.amount

import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter.formatCryptoAmount
import com.tangem.core.ui.utils.BigDecimalFormatter.formatFiatAmount
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldConverter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import kotlinx.collections.immutable.persistentListOf

internal class SendAmountStateConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val userWalletProvider: Provider<UserWallet>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val iconStateConverter: CryptoCurrencyToIconStateConverter,
    private val sendAmountFieldConverter: SendAmountFieldConverter,
) : Converter<String, SendStates.AmountState> {

    override fun convert(value: String): SendStates.AmountState {
        val userWallet = userWalletProvider()
        val appCurrency = appCurrencyProvider()
        val status = cryptoCurrencyStatusProvider()
        val fiat = formatFiatAmount(status.value.fiatAmount, appCurrency.code, appCurrency.symbol)
        val crypto = formatCryptoAmount(status.value.amount, status.currency.symbol, status.currency.decimals)
        val noFeeRate = status.value.fiatRate.isNullOrZero()

        return SendStates.AmountState(
            walletName = userWallet.name,
            walletBalance = resourceReference(R.string.send_wallet_balance_format, wrappedList(crypto, fiat)),
            tokenIconState = iconStateConverter.convert(status),
            amountTextField = sendAmountFieldConverter.convert(value),
            isPrimaryButtonEnabled = false,
            appCurrencyCode = appCurrency.code,
            subtractedFee = null,
            segmentedButtonConfig = persistentListOf(
                SendAmountSegmentedButtonsConfig(
                    title = stringReference(status.currency.symbol),
                    iconState = iconStateConverter.convertCustom(
                        value = status,
                        forceGrayscale = noFeeRate,
                        showCustomTokenBadge = false,
                    ),
                    isFiat = false,
                ),
                SendAmountSegmentedButtonsConfig(
                    title = stringReference(appCurrency.code),
                    iconUrl = appCurrency.iconSmallUrl,
                    isFiat = true,
                ),
            ),
            isSegmentedButtonsEnabled = !noFeeRate,
        )
    }
}
