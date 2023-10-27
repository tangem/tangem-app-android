package com.tangem.features.send.impl.presentation.send.state

import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter.formatCryptoAmount
import com.tangem.core.ui.utils.BigDecimalFormatter.formatFiatAmount
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.presentation.send.state.fields.SendAmountFieldConverter
import com.tangem.features.send.impl.presentation.send.viewmodel.SendClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class SendAmountStateConverter(
    private val currentStateProvider: Provider<SendUiState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val userWalletProvider: Provider<UserWallet?>,
    private val clickIntents: SendClickIntents,
    private val iconStateConverter: CryptoCurrencyToIconStateConverter,
    private val sendAmountFieldConverter: SendAmountFieldConverter,
) : Converter<Either<CurrencyStatusError, CryptoCurrencyStatus>, SendUiState> {

    override fun convert(value: Either<CurrencyStatusError, CryptoCurrencyStatus>): SendUiState {
        val userWallet = userWalletProvider() ?: return currentStateProvider()
        val appCurrency = appCurrencyProvider()
        return value.fold(
            ifLeft = {
                // TODO add error handling
                currentStateProvider()
            },
            ifRight = {
                val fiat = formatFiatAmount(it.value.fiatAmount, appCurrency.code, appCurrency.symbol)
                val crypto = formatCryptoAmount(it.value.amount, it.currency.symbol, it.currency.decimals)
                SendUiState.Content.AmountState(
                    cryptoCurrencyStatus = it,
                    walletName = userWallet.name,
                    walletBalance = "$crypto ($fiat)",
                    tokenIconState = iconStateConverter.convert(it),
                    appCurrency = appCurrency,
                    amountTextField = sendAmountFieldConverter.convert(Unit),
                    isFiatValue = false,
                    clickIntents = clickIntents,
                    segmentedButtonConfig = persistentListOf(
                        SendAmountSegmentedButtonsConfig(
                            title = stringReference(it.currency.symbol),
                            iconState = iconStateConverter.convert(it),
                            isFiat = false,
                        ),
                        SendAmountSegmentedButtonsConfig(
                            title = stringReference(appCurrency.code),
                            iconState = iconStateConverter.convert(it),
                            isFiat = true,
                        ),
                    ),
                )
            },
        )
    }
}