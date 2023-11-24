package com.tangem.features.send.impl.presentation.state.amount

import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter.formatCryptoAmount
import com.tangem.core.ui.utils.BigDecimalFormatter.formatFiatAmount
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldConverter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

internal class SendAmountStateConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val userWalletProvider: Provider<UserWallet>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val iconStateConverter: CryptoCurrencyToIconStateConverter,
    private val sendAmountFieldConverter: SendAmountFieldConverter,
) : Converter<Unit, SendStates.AmountState> {

    override fun convert(value: Unit): SendStates.AmountState {
        val userWallet = userWalletProvider()
        val appCurrency = appCurrencyProvider()
        val status = cryptoCurrencyStatusProvider()
        val fiat = formatFiatAmount(status.value.fiatAmount, appCurrency.code, appCurrency.symbol)
        val crypto = formatCryptoAmount(status.value.amount, status.currency.symbol, status.currency.decimals)

        return SendStates.AmountState(
            appCurrency = appCurrency,
            cryptoCurrencyStatus = status,
            walletName = userWallet.name,
            walletBalance = "$crypto ($fiat)",
            tokenIconState = iconStateConverter.convert(status),
            amountTextField = MutableStateFlow(sendAmountFieldConverter.convert(Unit)),
            isFiatValue = false,
            isPrimaryButtonEnabled = false,
            segmentedButtonConfig = persistentListOf(
                SendAmountSegmentedButtonsConfig(
                    title = stringReference(status.currency.symbol),
                    iconState = iconStateConverter.convert(status),
                    isFiat = false,
                ),
                SendAmountSegmentedButtonsConfig(
                    title = stringReference(appCurrency.code),
                    iconState = iconStateConverter.convert(status),
                    isFiat = true,
                ),
            ),
        )
    }
}