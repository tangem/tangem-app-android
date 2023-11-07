package com.tangem.features.send.impl.presentation.send.state

import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.presentation.send.state.fields.SendAmountFieldChangeConverter
import com.tangem.features.send.impl.presentation.send.state.fields.SendAmountFieldConverter
import com.tangem.features.send.impl.presentation.send.viewmodel.SendClickIntents

internal class SendStateFactory(
    private val clickIntents: SendClickIntents,
    private val currentStateProvider: Provider<SendUiState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val userWalletProvider: Provider<UserWallet?>,
) {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    private val amountFieldConverter by lazy { SendAmountFieldConverter(clickIntents) }

    private val amountFieldChangeConverter by lazy { SendAmountFieldChangeConverter(currentStateProvider) }

    private val amountStateConverter by lazy {
        SendAmountStateConverter(
            currentStateProvider = currentStateProvider,
            appCurrencyProvider = appCurrencyProvider,
            clickIntents = clickIntents,
            iconStateConverter = iconStateConverter,
            userWalletProvider = userWalletProvider,
            sendAmountFieldConverter = amountFieldConverter,
        )
    }

    fun getInitialState(): SendUiState = SendUiState.Content.Initial(clickIntents = clickIntents)

    fun getAmountState(cryptoCurrencyStatus: Either<CurrencyStatusError, CryptoCurrencyStatus>): SendUiState {
        return amountStateConverter.convert(cryptoCurrencyStatus)
    }

    fun getOnReceiveState(): SendUiState = SendUiState.Content.Initial(clickIntents = clickIntents)

    fun getOnAmountValueChange(value: String) = amountFieldChangeConverter.convert(value)

    fun getOnCurrencyChangedState(isFiat: Boolean): SendUiState {
        val state = currentStateProvider()
        val amountState = state as? SendUiState.Content.AmountState ?: return state

        return if (amountState.isFiatValue == isFiat) {
            state
        } else {
            return state.copy(isFiatValue = isFiat)
        }
    }
}